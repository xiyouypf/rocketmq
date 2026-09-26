package org.apache.rocketmq.client.consumer;

import java.util.List;
import org.apache.rocketmq.common.message.MessageQueue;

/**
 * RocketMQ消息队列分配算法接口（消费者使用）
 * 消息队列分配遵循一个消费者可以分配多个消息队列，但同一个消息队列只会分配给一个消费者，故如果消费者个数大于消息队列数量，则有些消费者无法消费消息。
 * RocketMQ默认提供5种分配算法：
 * 1. AllocateMessageQueueAveragely：平均分配，推荐指数为5颗星。
 * 2. AllocateMessageQueueAveragelyByCircle：平均轮询分配，推荐指数为5颗星。
 * 3. AllocateMessageQueueConsistentHash：一致性hash。不推荐使用，因为消息队列负载信息不容易跟踪。
 * 4. AllocateMessageQueueByConfig：根据配置，为每一个消费者配置固定的消息队列。
 * 5. AllocateMessageQueueByMachineRoom：根据Broker部署机房名，每个消费者负责Broker上不同的队列。
 */
public interface AllocateMessageQueueStrategy {

    /**
     * Allocating by consumer id
     *
     * @param consumerGroup current consumer group
     * @param currentCID current consumer id
     * @param mqAll message queue set in current topic
     * @param cidAll consumer set in current consumer group
     * @return The allocate result of given strategy
     */
    List<MessageQueue> allocate(
        final String consumerGroup,
        final String currentCID,
        final List<MessageQueue> mqAll,
        final List<String> cidAll
    );

    /**
     * Algorithm name
     *
     * @return The strategy name
     */
    String getName();
}
