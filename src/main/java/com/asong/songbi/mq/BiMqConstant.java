package com.asong.songbi.mq;

public interface BiMqConstant {

    String BI_EXCHANGE_NAME = "bi_exchange";

    String BI_QUEUE_NAME = "bi_queue";

    String BI_ROUTING_KEY = "bi_routingKey";

    /**
     * 死信队列
     */
    String DEAD_EXCHANGE = "dead_exchange";

    String DEAD_QUEUE = "dead_queue";

    String DEAD_ROUTING_KEY = "dead_rout";

    /**
     * 备用队列 报警队列
     */
    String BI_STANDBY_QUEUE = "bi_standby_queue";

    String BI_ALARM_QUEUE = "bi_alarm_queue";
    
    String BI_STANDBY_EXCHANGE = "bi_standby_exchange";
}
