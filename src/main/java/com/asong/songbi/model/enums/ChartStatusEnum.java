package com.asong.songbi.model.enums;



/**
 * 图表任务状态枚举
 * 
 * @author xys
 */

public enum ChartStatusEnum {
    
    WAIT("wait", "待生成"),
    RUNNING("running", "生成中"),
    SUCCEED("succeed", "生成成功"),
    FAILED("failed", "生成失败");
    
    private final String  status;
    
    private final String text;

    ChartStatusEnum(String status, String text) {
        this.status = status;
        this.text = text;
    }

    public String getStatus() {
        return status;
    }

    public String getText() {
        return text;
    }
}
