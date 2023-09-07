package com.asong.songbi.model.dto.chart;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.io.Serializable;

/**
 * 创建请求
 *
 */
@Data
public class ChartAddRequest implements Serializable {
    /**
     * 图表名称
     */
    private String name;
    
    /**
     * 分析目标
     */
    private String goal;
    
    /**
     * 图表数据
     */
    private String chartData;

    /**
     * 图表类型
     */
    private String chartType;
    

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}