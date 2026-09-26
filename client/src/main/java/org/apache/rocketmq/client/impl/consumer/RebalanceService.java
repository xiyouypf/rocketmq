package org.apache.rocketmq.client.impl.consumer;

import org.apache.rocketmq.client.impl.factory.MQClientInstance;
import org.apache.rocketmq.client.log.ClientLogger;
import org.apache.rocketmq.common.ServiceThread;
import org.apache.rocketmq.logging.InternalLogger;

/**
 * 消息队列负载均衡服务
 * 一个MQClientInstance持有一个RebalanceService实现，并随着MQClientInstance的启动而启动。
 */
public class RebalanceService extends ServiceThread {
    // RebalanceService线程默认每隔20s执行一次mqClientFactory.doRebalance（）方法，
    private static long waitInterval =
        Long.parseLong(System.getProperty(
            "rocketmq.client.rebalance.waitInterval", "20000"));
    private final InternalLogger log = ClientLogger.getLog();
    private final MQClientInstance mqClientFactory;

    public RebalanceService(MQClientInstance mqClientFactory) {
        this.mqClientFactory = mqClientFactory;
    }

    /**
     * RebalanceService线程默认每隔20s执行一次mqClientFactory.doRebalance（）方法，
     * 可以使用-Drocketmq.client.rebalance.waitInterval=interval来改变默认值。
     */
    @Override
    public void run() {
        log.info(this.getServiceName() + " service started");

        while (!this.isStopped()) {
            this.waitForRunning(waitInterval);
            this.mqClientFactory.doRebalance();
        }

        log.info(this.getServiceName() + " service end");
    }

    @Override
    public String getServiceName() {
        return RebalanceService.class.getSimpleName();
    }
}
