package com.asong.songbi.mq;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;

/**
 * @author xys
 */
@Configuration
@Slf4j
public class RabbitConfig {
    
    @Resource
    ConnectionFactory connectionFactory;
    
    @Bean
    public DirectExchange directExchange() {
        // 创建一个直接交换机
        //return ExchangeBuilder.directExchange(BiMqConstant.BI_EXCHANGE_NAME).build();
        // alternate-exchange 参数：设置备用交换机，当消息不可路由的时候就会把消息推送到该交换机上
        return ExchangeBuilder.directExchange(BiMqConstant.BI_EXCHANGE_NAME).withArgument("alternate-exchange",BiMqConstant.BI_STANDBY_EXCHANGE).build();

    }

    @Bean
    public Queue queue() {
        return QueueBuilder.durable(BiMqConstant.BI_QUEUE_NAME)
                .deadLetterExchange(BiMqConstant.DEAD_EXCHANGE)
                .deadLetterRoutingKey(BiMqConstant.DEAD_ROUTING_KEY)
                .build();
    }

    @Bean
    public Binding binding(DirectExchange directExchange, Queue queue) {
        return BindingBuilder.bind(queue).to(directExchange).with(BiMqConstant.BI_ROUTING_KEY);
    }
    
    @Bean
    public Exchange deadExchange() {
        return ExchangeBuilder.directExchange(BiMqConstant.DEAD_EXCHANGE).build();
    }

    @Bean
    public Queue deadQueue() {
        return QueueBuilder.durable(BiMqConstant.DEAD_QUEUE).build();
    }

    @Bean
    public Binding deadBind(Exchange deadExchange, Queue deadQueue) {
        return BindingBuilder.bind(deadQueue).to(deadExchange).with(BiMqConstant.DEAD_ROUTING_KEY).noargs();
    }

    @Bean
    public RabbitTemplate createRabbitTemplate(){
        RabbitTemplate rabbitTemplate = new RabbitTemplate();
        rabbitTemplate.setConnectionFactory(connectionFactory);
        // 设置Mandatory
        rabbitTemplate.setMandatory(true);
        // 消息发送到交换机确认
        rabbitTemplate.setConfirmCallback(new RabbitTemplate.ConfirmCallback() {
            @Override
            public void confirm(CorrelationData correlationData, boolean ack, String cause) {
                if(ack){
                    if (correlationData != null) {
                        log.info("交换机收到消息成功，消息为:{}", correlationData.toString());
                    } else {
                        log.info("交换机收到消息成功，但未提供 correlationData。");
                    }
                }else {
                    if (correlationData != null) {
                        log.info("交换机收到消息失败，消息为:{}", correlationData.toString());
                    } else {
                        log.info("交换机收到消息失败，但未提供 correlationData，原因为:{}",cause);
                    }
                }
            }
        });

        // 消息从交换机推送到队列确认
        rabbitTemplate.setReturnsCallback(new RabbitTemplate.ReturnsCallback() {
            @Override
            public void returnedMessage(ReturnedMessage returnedMessage) {
                log.info("交换机:{}推送消息到队列失败，推送的消息是:{}，路由键是:{}，错误码是:{}，错误原因是:{}",
                        returnedMessage.getExchange(),returnedMessage.getMessage().getBody(),returnedMessage.getRoutingKey(),
                        returnedMessage.getReplyCode(),returnedMessage.getReplyText());
            }
        });
        return rabbitTemplate;
    }
    
    
    @Bean
    public FanoutExchange standbyFanoutExchange(){
        // 备用交换机
        return new FanoutExchange(BiMqConstant.BI_STANDBY_EXCHANGE);
    }

    @Bean
    public Queue getStandbyQueue(){
        // 备用队列
        return QueueBuilder.durable(BiMqConstant.BI_STANDBY_QUEUE).build();
    }

    @Bean
    public Binding standbyExchagneBinding()
    {
        // 设置备用队列和备用交换机的绑定关系
        return BindingBuilder.bind(getStandbyQueue()).to(standbyFanoutExchange());
    }

    @Bean
    public Queue getAlarmQueue()
    {
        // 报警队列
        return new Queue(BiMqConstant.BI_ALARM_QUEUE);
    }

    // 设置报警队列和备用交换机的绑定关系
    @Bean
    public Binding alarmExchagneBinding()
    {
        return BindingBuilder.bind(getAlarmQueue()).to(standbyFanoutExchange());
    }

    
}
