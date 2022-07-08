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

import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.*;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.remoting.common.RemotingHelper;
import org.apache.rocketmq.remoting.exception.RemotingException;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * This class demonstrates how to send messages to brokers using provided {@link DefaultMQProducer}.
 */
public class Producer {

    public static final String PRODUCER_GROUP = "mq_producer_group";
    public static final String DEFAULT_NAMESRVADDR_ADDR = "127.0.0.1:9876";
    public static final String TOPIC = "topic001";
    //用于客户端过滤，消息消费掉了，但是被过滤掉了
    public static final String TAG = "TagA";

    private static volatile Producer INSTANCE;

    private Producer() {

    }

    public static Producer getInstance() {
        if (INSTANCE == null) {
            synchronized (Producer.class) {
                if (INSTANCE == null) {
                    INSTANCE = new Producer();
                }
            }
        }
        return INSTANCE;
    }

    public static void main(String[] args) throws MQClientException, InterruptedException, RemotingException, MQBrokerException {
//        getInstance().sendOneSyncMessage();
//        getInstance().sendOneASyncMessage(producer);
//        getInstance().sendOneWayMessage(producer);
//        getInstance().sendBatchMessage(producer);
//        getInstance().sendFilterMessage(producer);

//        getInstance().sendTransactionMessage();
        getInstance().sendToSelectQueue();
    }

    private DefaultMQProducer createAndStartMQProducer() throws MQClientException, InterruptedException {
        DefaultMQProducer producer = new DefaultMQProducer(PRODUCER_GROUP);
        producer.setNamesrvAddr(DEFAULT_NAMESRVADDR_ADDR);
        producer.start();
        return producer;
    }

    public void sendOneSyncMessage() throws InterruptedException, MQClientException {
        //可以设置线程池
        DefaultMQProducer producer = createAndStartMQProducer();
        Message message = new Message(TOPIC, TAG, "send one sync message".getBytes(StandardCharsets.UTF_8));
        SendResult sendResult = null;
        try {
            //阻塞
            sendResult = producer.send(message);
        } catch (Exception e) {
            System.out.println("e = " + e);
            Thread.sleep(1000);
        }
        System.out.println("sendResult.getSendStatus() = " + sendResult.getSendStatus());
        producer.shutdown();
    }

    public void sendOneASyncMessage() throws InterruptedException, RemotingException, MQClientException {
        DefaultMQProducer producer = createAndStartMQProducer();
        Message message = new Message(TOPIC, TAG, "send one async message".getBytes(StandardCharsets.UTF_8));
        producer.setRetryTimesWhenSendAsyncFailed(2);
        producer.send(message, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                System.out.println("sendResult.getSendStatus() = " + sendResult.getSendStatus());
                producer.shutdown();
            }

            @Override
            public void onException(Throwable e) {
                System.out.println("e = " + e);
                producer.shutdown();
            }
        });
    }

    public void sendOneWayMessage() throws RemotingException, InterruptedException, MQClientException {
        DefaultMQProducer producer = createAndStartMQProducer();
        Message message = new Message(TOPIC, TAG, "send one way message".getBytes(StandardCharsets.UTF_8));
        producer.sendOneway(message);
        producer.shutdown();
    }

    public void sendBatchMessage() throws InterruptedException, MQClientException {
        DefaultMQProducer producer = createAndStartMQProducer();
        List<Message> list = new ArrayList<>();
        Message message1 = new Message(TOPIC, TAG, "send batch message 01".getBytes(StandardCharsets.UTF_8));
        Message message2 = new Message(TOPIC, TAG, "send batch message 02".getBytes(StandardCharsets.UTF_8));
        Message message3 = new Message(TOPIC, TAG, "send batch message 03".getBytes(StandardCharsets.UTF_8));
        list.add(message1);
        list.add(message2);
        list.add(message3);
        SendResult sendResult = null;
        try {
            //阻塞
            sendResult = producer.send(list);
        } catch (Exception e) {
            e.printStackTrace();
            Thread.sleep(1000);
        }
        System.out.println("sendResult.getSendStatus() = " + sendResult.getSendStatus());
        producer.shutdown();
    }

    public void sendFilterMessage() throws MQBrokerException, RemotingException, InterruptedException, MQClientException {
        DefaultMQProducer producer = createAndStartMQProducer();
        for (int i = 1; i <= 100; i++) {
            Message message = new Message(TOPIC, TAG, "sendFilterMessage", "send Filter Message".getBytes(StandardCharsets.UTF_8));
            message.putUserProperty("age", "" + i);
            producer.send(message);
        }
        producer.shutdown();
    }

    public void sendTransactionMessage() throws MQClientException {
        TransactionMQProducer producer = new TransactionMQProducer(PRODUCER_GROUP);
        producer.setNamesrvAddr(DEFAULT_NAMESRVADDR_ADDR);
        producer.setTransactionListener(new TransactionListener() {
            @Override
            public LocalTransactionState executeLocalTransaction(Message msg, Object arg) {
                try {
                    //执行本地事务
                    System.out.println("msg = " + msg);
                } catch (Exception e) {
                    System.out.println("e = " + e);
                    return LocalTransactionState.ROLLBACK_MESSAGE;
                }
                return LocalTransactionState.COMMIT_MESSAGE;
            }

            @Override
            public LocalTransactionState checkLocalTransaction(MessageExt msg) {
                try {
                    System.out.println("msg = " + msg);
                } catch (Exception e) {
                    return LocalTransactionState.UNKNOW;
                }
                return LocalTransactionState.COMMIT_MESSAGE;
            }
        });
        producer.start();
        Message message = new Message(TOPIC, "事务消息".getBytes(StandardCharsets.UTF_8));
        TransactionSendResult sendResult = producer.sendMessageInTransaction(message, null);
        System.out.println("sendResult = " + sendResult);
    }

    /**
     * 保证消息的顺序性
     * 同一个topic
     * 同一个queue
     * 同一个发送线程
     */
    public void sendToSelectQueue() throws InterruptedException, MQClientException, MQBrokerException, RemotingException {
        DefaultMQProducer producer = createAndStartMQProducer();
        Message message = new Message(TOPIC, "select message queue".getBytes(StandardCharsets.UTF_8));
        SendResult sendResult = producer.send(message, new MessageQueueSelector() {
            @Override
            public MessageQueue select(List<MessageQueue> mqs, Message msg, Object arg) {
                MessageQueue messageQueue = mqs.get((Integer) arg);
                return messageQueue;
            }
        }, 1, 3000L);
    }
}
