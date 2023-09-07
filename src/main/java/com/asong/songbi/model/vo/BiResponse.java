package com.asong.songbi.model.vo;

import lombok.Data;

/**
 * Bi 的返回结果
 * 
 * @author xys
 */
@Data
public class BiResponse {

    /**
     * 生成的图表
     */
    private String genChart;

    /**
     * 生成的分析结果
     */
    private String genResult;

    /**
     * 图表id
     */
    private Long chartId;
}
