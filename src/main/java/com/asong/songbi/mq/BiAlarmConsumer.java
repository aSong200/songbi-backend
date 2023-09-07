package com.asong.songbi.mq;

import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * @author xys
 */
@Component
@Slf4j
public class BiAlarmConsumer {

    @RabbitListener(queues = {BiMqConstant.BI_ALARM_QUEUE},ackMode = "MANUAL")
    public void alarmQueue(String message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        log.error("正常队列不可路由，报警队列收到消息，消息为:{}，delevery-tag:{}",message,deliveryTag);
        channel.basicAck(deliveryTag,false);
    }
}
