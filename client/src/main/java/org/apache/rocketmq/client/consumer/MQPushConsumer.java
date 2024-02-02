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
package org.apache.rocketmq.client.consumer;

import org.apache.rocketmq.client.consumer.listener.MessageListener;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.consumer.listener.MessageListenerOrderly;
import org.apache.rocketmq.client.exception.MQClientException;

/**
 * 推模式消费者
 */
public interface MQPushConsumer extends MQConsumer {
    /**
     * 启动消费者
     */
    void start() throws MQClientException;

    /**
     * 关闭消费者
     */
    void shutdown();

    /**
     * 注册消息事件监听器。
     */
    @Deprecated
    void registerMessageListener(MessageListener messageListener);

    /**
     * 注册并发消息事件监听器。
     */
    void registerMessageListener(final MessageListenerConcurrently messageListener);

    /**
     * 注册顺序消费事件监听器。
     */
    void registerMessageListener(final MessageListenerOrderly messageListener);

    /**
     * 基于主题订阅消息。
     * topic：消息主题。
     * subExpression：消息过滤表达式，TAG或SQL92表达式。
     */
    void subscribe(final String topic, final String subExpression) throws MQClientException;

    /**
     * 基于主题订阅消息，消息过滤方式使用类模式。
     * topic：消息主题。
     * fullClassName：过滤类全路径名。
     * filterClassSource：过滤类代码。
     */
    @Deprecated
    void subscribe(final String topic, final String fullClassName,
        final String filterClassSource) throws MQClientException;

    /**
     * Subscribe some topic with selector.
     * <p>
     * This interface also has the ability of {@link #subscribe(String, String)},
     * and, support other message selection, such as {@link org.apache.rocketmq.common.filter.ExpressionType#SQL92}.
     * </p>
     * <p/>
     * <p>
     * Choose Tag: {@link MessageSelector#byTag(java.lang.String)}
     * </p>
     * <p/>
     * <p>
     * Choose SQL92: {@link MessageSelector#bySql(java.lang.String)}
     * </p>
     *
     * @param selector message selector({@link MessageSelector}), can be null.
     */
    void subscribe(final String topic, final MessageSelector selector) throws MQClientException;

    /**
     * 取消消息订阅。
     */
    void unsubscribe(final String topic);

    /**
     * Update the consumer thread pool size Dynamically
     */
    void updateCorePoolSize(int corePoolSize);

    /**
     * Suspend the consumption
     */
    void suspend();

    /**
     * Resume the consumption
     */
    void resume();
}
