/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.rocketmq.store.index;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.util.List;
import org.apache.rocketmq.common.constant.LoggerName;
import org.apache.rocketmq.logging.InternalLogger;
import org.apache.rocketmq.logging.InternalLoggerFactory;
import org.apache.rocketmq.store.MappedFile;

/**
 * 消息索引文件
 * 主要存储消息Key与Offset的对应关系
 * Hash索引机制为消息建立索引
 * 通过特定的哈希算法将Message的key或者Message的hash进行映射，
 * 然后通过二分查找的方式快速定位到Message的物理位置。
 */
public class IndexFile {
    private static final InternalLogger log = InternalLoggerFactory.getLogger(LoggerName.STORE_LOGGER_NAME);
    // hash槽数量大小
    private static int hashSlotSize = 4;
    // 单个Index条目大小
    private static int indexSize = 20;
    private static int invalidIndex = 0;
    // hash槽数量
    private final int hashSlotNum;
    // 最大条目数
    private final int indexNum;
    private final MappedFile mappedFile;
    private final MappedByteBuffer mappedByteBuffer;
    // IndexHeader头部，包含40个字节，记录该IndexFile的统计信息
    private final IndexHeader indexHeader;

    public IndexFile(final String fileName, final int hashSlotNum, final int indexNum,
        final long endPhyOffset, final long endTimestamp) throws IOException {
        int fileTotalSize =
            IndexHeader.INDEX_HEADER_SIZE + (hashSlotNum * hashSlotSize) + (indexNum * indexSize);
        this.mappedFile = new MappedFile(fileName, fileTotalSize);
        this.mappedByteBuffer = this.mappedFile.getMappedByteBuffer();
        this.hashSlotNum = hashSlotNum;
        this.indexNum = indexNum;

        ByteBuffer byteBuffer = this.mappedByteBuffer.slice();
        this.indexHeader = new IndexHeader(byteBuffer);

        if (endPhyOffset > 0) {
            this.indexHeader.setBeginPhyOffset(endPhyOffset);
            this.indexHeader.setEndPhyOffset(endPhyOffset);
        }

        if (endTimestamp > 0) {
            this.indexHeader.setBeginTimestamp(endTimestamp);
            this.indexHeader.setEndTimestamp(endTimestamp);
        }
    }

    public String getFileName() {
        return this.mappedFile.getFileName();
    }

    public void load() {
        this.indexHeader.load();
    }

    public void flush() {
        long beginTime = System.currentTimeMillis();
        if (this.mappedFile.hold()) {
            this.indexHeader.updateByteBuffer();
            this.mappedByteBuffer.force();
            this.mappedFile.release();
            log.info("flush index file elapsed time(ms) " + (System.currentTimeMillis() - beginTime));
        }
    }

    public boolean isWriteFull() {
        return this.indexHeader.getIndexCount() >= this.indexNum;
    }

    public boolean destroy(final long intervalForcibly) {
        return this.mappedFile.destroy(intervalForcibly);
    }

    /**
     * 将消息索引键与消息偏移量映射关系写入到IndexFile
     *  key：消息索引
     *  phyOffset：消息物理偏移量
     *  storeTimestamp：消息存储时间
     */
    public boolean putKey(final String key, final long phyOffset, final long storeTimestamp) {
        // Step1：当前索引文件未写满
        if (this.indexHeader.getIndexCount() < this.indexNum) {
            // 根据key算出key的hashcode
            int keyHash = indexKeyHashMethod(key);
            // 定位到hashcode对应的hash槽下标
            int slotPos = keyHash % this.hashSlotNum;
            // hashcode对应的hash槽的物理地址
            int absSlotPos = IndexHeader.INDEX_HEADER_SIZE + slotPos * hashSlotSize;

            try {

                // Step2：读取hash槽中存储的数据
                int slotValue = this.mappedByteBuffer.getInt(absSlotPos);
                if (slotValue <= invalidIndex || slotValue > this.indexHeader.getIndexCount()) {
                    // 将slotValue设置为0
                    slotValue = invalidIndex;
                }

                // Step3：计算待存储消息的时间戳与第一条消息时间戳的差值，并转换成秒。
                long timeDiff = storeTimestamp - this.indexHeader.getBeginTimestamp();
                timeDiff = timeDiff / 1000;
                if (this.indexHeader.getBeginTimestamp() <= 0) {
                    timeDiff = 0;
                } else if (timeDiff > Integer.MAX_VALUE) {
                    timeDiff = Integer.MAX_VALUE;
                } else if (timeDiff < 0) {
                    timeDiff = 0;
                }

                // Step4：将条目信息存储在IndexFile中
                // 1）计算新添加条目的起始物理偏移量，等于头部字节长度 +hash槽数量 * 单个hash槽大小（4个字节）
                // + 当前Index条目个数 * 单个Index条目大小（20个字节）。
                int absIndexPos =
                    IndexHeader.INDEX_HEADER_SIZE + this.hashSlotNum * hashSlotSize
                        + this.indexHeader.getIndexCount() * indexSize;

                // 2）依次将hashcode、消息物理偏移量、消息存储时间戳与索引文件时间戳、当前Hash槽的值存入MappedByteBuffer中。
                this.mappedByteBuffer.putInt(absIndexPos, keyHash);
                this.mappedByteBuffer.putLong(absIndexPos + 4, phyOffset);
                this.mappedByteBuffer.putInt(absIndexPos + 4 + 8, (int) timeDiff);
                this.mappedByteBuffer.putInt(absIndexPos + 4 + 8 + 4, slotValue);

                // 3）将当前Index中包含的条目数量存入Hash槽中，将覆盖原先Hash槽的值。
                this.mappedByteBuffer.putInt(absSlotPos, this.indexHeader.getIndexCount());

                // Step5：更新文件索引头信息
                if (this.indexHeader.getIndexCount() <= 1) {
                    this.indexHeader.setBeginPhyOffset(phyOffset);
                    this.indexHeader.setBeginTimestamp(storeTimestamp);
                }
                if (invalidIndex == slotValue) {
                    this.indexHeader.incHashSlotCount();
                }
                this.indexHeader.incIndexCount();
                this.indexHeader.setEndPhyOffset(phyOffset);
                this.indexHeader.setEndTimestamp(storeTimestamp);

                return true;
            } catch (Exception e) {
                log.error("putKey exception, Key: " + key + " KeyHashCode: " + key.hashCode(), e);
            }
        } else {
            log.warn("Over index file capacity: index count = " + this.indexHeader.getIndexCount()
                + "; index max num = " + this.indexNum);
        }

        return false;
    }

    /**
     * 根据key算出key的hashcode
     */
    public int indexKeyHashMethod(final String key) {
        int keyHash = key.hashCode();
        int keyHashPositive = Math.abs(keyHash);
        if (keyHashPositive < 0)
            keyHashPositive = 0;
        return keyHashPositive;
    }

    public long getBeginTimestamp() {
        return this.indexHeader.getBeginTimestamp();
    }

    public long getEndTimestamp() {
        return this.indexHeader.getEndTimestamp();
    }

    public long getEndPhyOffset() {
        return this.indexHeader.getEndPhyOffset();
    }

    public boolean isTimeMatched(final long begin, final long end) {
        boolean result = begin < this.indexHeader.getBeginTimestamp() && end > this.indexHeader.getEndTimestamp();
        result = result || (begin >= this.indexHeader.getBeginTimestamp() && begin <= this.indexHeader.getEndTimestamp());
        result = result || (end >= this.indexHeader.getBeginTimestamp() && end <= this.indexHeader.getEndTimestamp());
        return result;
    }

    /**
     * 根据索引key查找消息
     *  phyOffsets：查找到的消息物理偏移量
     *  key：索引key
     *  maxNum：本次查找最大消息条数
     *  begin：开始时间戳
     *  end：结束时间戳
     */
    public void selectPhyOffset(final List<Long> phyOffsets, final String key, final int maxNum,
                                final long begin, final long end) {
        if (this.mappedFile.hold()) {
            // Step1：根据key算出key的hashcode
            int keyHash = indexKeyHashMethod(key);
            // 定位到hashcode对应的hash槽下标
            int slotPos = keyHash % this.hashSlotNum;
            // hashcode对应的hash槽的物理地址
            int absSlotPos = IndexHeader.INDEX_HEADER_SIZE + slotPos * hashSlotSize;

            try {
                int slotValue = this.mappedByteBuffer.getInt(absSlotPos);
                // Step2：如果对应的Hash槽中存储的数据小于1或大于当前索引条目个数则表示该HashCode没有对应的条目，直接返回
                if (slotValue <= invalidIndex || slotValue > this.indexHeader.getIndexCount()
                    || this.indexHeader.getIndexCount() <= 1) {
                } else {
                    for (int nextIndexToRead = slotValue; ; ) {
                        if (phyOffsets.size() >= maxNum) {
                            break;
                        }

                        // Step4：根据Index下标定位到条目的起始物理偏移量
                        int absIndexPos =
                            IndexHeader.INDEX_HEADER_SIZE + this.hashSlotNum * hashSlotSize
                                + nextIndexToRead * indexSize;
                        // 依次读取hashcode、物理偏移量、时间差、上一个条目的Index下标
                        int keyHashRead = this.mappedByteBuffer.getInt(absIndexPos);
                        long phyOffsetRead = this.mappedByteBuffer.getLong(absIndexPos + 4);
                        long timeDiff = this.mappedByteBuffer.getInt(absIndexPos + 4 + 8);
                        int prevIndexRead = this.mappedByteBuffer.getInt(absIndexPos + 4 + 8 + 4);

                        // Step5：如果存储的时间差小于0，则直接结束
                        if (timeDiff < 0) {
                            break;
                        }
                        timeDiff *= 1000L;

                        // 如果hashcode匹配并且消息存储时间介于待查找时间start、end之间则将消息物理偏移量加入到phyOffsets
                        long timeRead = this.indexHeader.getBeginTimestamp() + timeDiff;
                        boolean timeMatched = (timeRead >= begin) && (timeRead <= end);
                        if (keyHash == keyHashRead && timeMatched) {
                            phyOffsets.add(phyOffsetRead);
                        }

                        // 验证条目的前一个Index索引，如果索引大于等于1并且小于Index条目数，则继续查找，否则结束整个查找
                        if (prevIndexRead <= invalidIndex
                            || prevIndexRead > this.indexHeader.getIndexCount()
                            || prevIndexRead == nextIndexToRead || timeRead < begin) {
                            break;
                        }

                        nextIndexToRead = prevIndexRead;
                    }
                }
            } catch (Exception e) {
                log.error("selectPhyOffset exception ", e);
            } finally {
                this.mappedFile.release();
            }
        }
    }
}
