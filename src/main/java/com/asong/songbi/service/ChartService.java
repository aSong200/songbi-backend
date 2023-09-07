package com.asong.songbi.service;

import com.asong.songbi.model.dto.chart.GenChartByAiRequest;
import com.asong.songbi.model.dto.chart.ReGenRequest;
import com.asong.songbi.model.entity.Chart;
import com.asong.songbi.model.vo.BiResponse;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;

/**
 * 图表信息表
 * 
 * @author xys
*/
public interface ChartService extends IService<Chart> {

    /**
     * 校验
     * 
     * @param chart 图表
     * @param add 是否为新增创建
     */
    void validChart(Chart chart, boolean add);

    /**
     * AI智能分析生成图表 (异步---RabbitMQ消息队列)
     * 
     * @param multipartFile
     * @param genChartByAiRequest
     * @param request
     * @return
     */
    BiResponse genChartByAiAsyncMq(MultipartFile multipartFile, GenChartByAiRequest genChartByAiRequest, HttpServletRequest request);

    /**
     * 重新生成图表
     * 
     * @param reGenRequest
     * @param request
     * @return
     */
    BiResponse reGenChartByAi(ReGenRequest reGenRequest, HttpServletRequest request);

    /**
     * AI智能分析生成图表 (同步)
     * 
     * @param multipartFile
     * @param genChartByAiRequest
     * @param request
     * @return
     */
    BiResponse genChartByAi(MultipartFile multipartFile, GenChartByAiRequest genChartByAiRequest, HttpServletRequest request);

    /**
     * AI智能分析生成图表 (异步--线程池)
     * @param multipartFile
     * @param genChartByAiRequest
     * @param request
     * @return
     */
    BiResponse genChartByAiAsync(MultipartFile multipartFile, GenChartByAiRequest genChartByAiRequest, HttpServletRequest request);
}
