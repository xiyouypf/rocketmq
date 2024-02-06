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
package org.apache.rocketmq.example.quickstart;

import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.*;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.common.protocol.heartbeat.MessageModel;

import java.util.List;

/**
 * This example shows how to subscribe and consume messages using providing {@link DefaultMQPushConsumer}.
 */
public class Consumer {

    public static final String CONSUMER_GROUP = "mq_consumer_group";
    public static final String DEFAULT_NAMESRVADDR = "127.0.0.1:9876";
    public static final String TOPIC = "topic001";
    public static final String TAG = "TagA";

    private Consumer() {

    }

    public static volatile Consumer INSTANCE;

    public static Consumer getInstance() {
        if (INSTANCE == null) {
            synchronized (Consumer.class) {
                if (INSTANCE == null) {
                    INSTANCE = new Consumer();
                }
            }
        }
        return INSTANCE;
    }

    public static void main(String[] args) throws InterruptedException, MQClientException {
        getInstance().pushConsumer();
    }

    public void pushConsumer() throws MQClientException {
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer(CONSUMER_GROUP);

        consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET);
        consumer.setNamesrvAddr(DEFAULT_NAMESRVADDR);

//        consumer.subscribe(TOPIC, MessageSelector.byTag(TAG));
//        consumer.subscribe(TOPIC, MessageSelector.bySql("age >= 80 and age<=90"));
        consumer.subscribe(TOPIC, "*");
        consumer.setConsumeThreadMax(1); //最大开启的线程数
        consumer.setConsumeThreadMin(1); //最小开启的线程数
        consumer.registerMessageListener(new MessageListenerConcurrently() {
            @Override
            public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
                try {
                    System.out.println(msgs);
                } catch (Exception e) {
                    return ConsumeConcurrentlyStatus.RECONSUME_LATER;
                }
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            }
        });

        //集群：一组consumer，保证每个集群消费一次
        consumer.setMessageModel(MessageModel.CLUSTERING);
        consumer.start();

        System.out.printf("Consumer Started.%n");
    }
}
