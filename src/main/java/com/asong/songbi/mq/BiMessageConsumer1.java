package com.asong.songbi.mq;

import com.asong.songbi.common.ErrorCode;
import com.asong.songbi.constant.CommonConstant;
import com.asong.songbi.exception.BusinessException;
import com.asong.songbi.manager.AiManager;
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
 * 消息队列消费者 1
 * 
 * @author xys
 */
@Slf4j
@Component
public class BiMessageConsumer1 {
    
    @Resource
    private ChartService chartService;
    
    @Resource
    private AiManager aiManager;
    
    @RabbitListener(queues = {BiMqConstant.BI_QUEUE_NAME},ackMode = "MANUAL")
    private void receiveMessage(String message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        log.info("receiveMessage message = {}", message);
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
        // 如果为succeed状态，直接确认，防止重复消费
        if(chart.getStatus().equals(ChartStatusEnum.FAILED.getStatus())){
            channel.basicAck(deliveryTag,false);
        }
        // 先修改图表任务状态为 “running执行中”。等执行AI服务成功后，修改为 “已完成”、保存执行结果；执行失败后，状态修改为 “失败”，记录任务失败信息。
        Chart updateChart = new Chart();
        updateChart.setId(chart.getId());
        updateChart.setStatus(ChartStatusEnum.RUNNING.getStatus());
        boolean b = chartService.updateById(updateChart);
        if (!b) {
            channel.basicNack(deliveryTag, false, false);
            handleChartUpdateError(chart.getId(), "更新图表执行中状态失败");
            return;
        }
        
        //调用AI
        String result = aiManager.doAiChat(CommonConstant.BI_MODEL_ID, buildUserInput(chart));
        String[] splits = result.split("@@@");
        if (splits.length < 3) {
            channel.basicNack(deliveryTag, false, false);
            handleChartUpdateError(chart.getId(), "AI 生成错误");
            return;
        }
        String genChart = splits[1].trim();
        String genResult = splits[2].trim();
        Chart updateChartResult = new Chart();
        updateChartResult.setId(chart.getId());
        updateChartResult.setGenChart(genChart);
        updateChartResult.setGenResult(genResult);
        //更新图标状态为成功
        updateChartResult.setStatus(ChartStatusEnum.SUCCEED.getStatus());
        boolean updateResult = chartService.updateById(updateChartResult);
        if (!updateResult) {
            channel.basicNack(deliveryTag, false, false);
            handleChartUpdateError(chart.getId(), "更新图表成功状态失败");
        }
        // 消息确认
        channel.basicAck(deliveryTag, false);
    }

    /**
     * 构建用户输入
     * @param chart 图表
     * @return
     */
    private String buildUserInput(Chart chart) {
        String goal = chart.getGoal();
        String chartType = chart.getChartType();
        String csvData = chart.getChartData();

        //构造用户输入的需求
        StringBuilder userInput = new StringBuilder();
        userInput.append("分析需求：").append("\n");
        //拼接分析目标、图表类型
        String biGoal = goal;
        userInput.append(biGoal);
        if (StringUtils.isNotBlank(chartType)) {
            userInput.append(" 请使用").append(chartType);
        }
        userInput.append("\n").append("原始数据：").append("\n");
        userInput.append(csvData).append("\n");
        return userInput.toString();
    }
    
    private void handleChartUpdateError(long chartId, String execMessage) {
        Chart updateChartResult = new Chart();
        updateChartResult.setId(chartId);
        updateChartResult.setStatus("failed");
        updateChartResult.setExecMessage("execMessage");
        boolean updateResult = chartService.updateById(updateChartResult);
        if (!updateResult) {
            log.error("更新图表失败状态失败" + chartId + "," + execMessage);
        }
    }
    
}
