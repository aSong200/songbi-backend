package com.asong.songbi.mq;

import com.asong.songbi.common.ErrorCode;
import com.asong.songbi.exception.BusinessException;
import com.asong.songbi.model.entity.Chart;
import com.asong.songbi.model.enums.ChartStatusEnum;
import com.asong.songbi.service.ChartService;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;

/**
 * 死信队列消费者
 * 
 * @author xys
 */
@Component
@Slf4j
public class BiMessageDeadConsumer {

    @Resource
    private ChartService chartService;

    @RabbitListener(queues = {BiMqConstant.DEAD_QUEUE},ackMode = "MANUAL")
    private void handleDeadMessage(String message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        log.info("receive dead message = {}", message);
        if (StringUtils.isBlank(message)) {
            // 图表id为空，失败，消息拒绝
            channel.basicNack(deliveryTag, false, false);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "消息为空");
        }
        long chartId = Long.parseLong(message);
        Chart chart = chartService.getById(chartId);
        if (chart == null) {
            channel.basicNack(deliveryTag, false, false);
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图表未找到");
        }
        // 将图表状态设置为 failed 生成失败
        Chart updateChart = new Chart();
        updateChart.setId(chart.getId());
        updateChart.setStatus(ChartStatusEnum.FAILED.getStatus());
        chartService.updateById(updateChart);
        // 消息确认
        channel.basicAck(deliveryTag, false);
    }
}
