package org.apache.rocketmq.common.protocol.header;

import org.apache.rocketmq.remoting.CommandCustomHeader;
import org.apache.rocketmq.remoting.annotation.CFNotNull;
import org.apache.rocketmq.remoting.annotation.CFNullable;
import org.apache.rocketmq.remoting.exception.RemotingCommandException;

/**
 * 消息消费返回结果的头部
 */
public class ConsumerSendMsgBackRequestHeader implements CommandCustomHeader {
    // 消息物理偏移量
    @CFNotNull
    private Long offset;
    // 消费组名
    @CFNotNull
    private String group;
    // 延迟级别，RcketMQ不支持精确的定时消息调度，而是提供几个延时级别，
    // MessageStoreConfig#messageDelayLevel = "1s 5s 10s 30s 1m 2m 3m 4m 5m 6m 7m 8m 9m 10m 20m 30m 1h 2h"，
    // 如果delayLevel=1表示延迟5s, delayLevel=2则表示延迟10s。
    @CFNotNull
    private Integer delayLevel;
    // 消息ID
    private String originMsgId;
    // 消息主题
    private String originTopic;
    @CFNullable
    private boolean unitMode = false;
    // 最大重新消费次数，默认为16次。
    private Integer maxReconsumeTimes;

    @Override
    public void checkFields() throws RemotingCommandException {

    }

    public Long getOffset() {
        return offset;
    }

    public void setOffset(Long offset) {
        this.offset = offset;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public Integer getDelayLevel() {
        return delayLevel;
    }

    public void setDelayLevel(Integer delayLevel) {
        this.delayLevel = delayLevel;
    }

    public String getOriginMsgId() {
        return originMsgId;
    }

    public void setOriginMsgId(String originMsgId) {
        this.originMsgId = originMsgId;
    }

    public String getOriginTopic() {
        return originTopic;
    }

    public void setOriginTopic(String originTopic) {
        this.originTopic = originTopic;
    }

    public boolean isUnitMode() {
        return unitMode;
    }

    public void setUnitMode(boolean unitMode) {
        this.unitMode = unitMode;
    }

    public Integer getMaxReconsumeTimes() {
        return maxReconsumeTimes;
    }

    public void setMaxReconsumeTimes(final Integer maxReconsumeTimes) {
        this.maxReconsumeTimes = maxReconsumeTimes;
    }

    @Override
    public String toString() {
        return "ConsumerSendMsgBackRequestHeader [group=" + group + ", originTopic=" + originTopic + ", originMsgId=" + originMsgId
            + ", delayLevel=" + delayLevel + ", unitMode=" + unitMode + ", maxReconsumeTimes=" + maxReconsumeTimes + "]";
    }
}
