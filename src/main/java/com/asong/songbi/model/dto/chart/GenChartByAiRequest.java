package com.asong.songbi.model.dto.chart;

import lombok.Data;

import java.io.Serializable;

/**
 * 上传excel文件进行智能分析
 * 
 * @author xys
 */
@Data
public class GenChartByAiRequest implements Serializable {
    /**
     * 图表名称
     */
    private String name;

    /**
     * 分析目标
     */
    private String goal;

    /**
     * 图表类型
     */
    private String chartType;

    private static final long serialVersionUID = 1L;
}
