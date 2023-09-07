package com.asong.songbi.manager;


import com.asong.songbi.common.ErrorCode;
import com.asong.songbi.exception.BusinessException;
import com.yupi.yucongming.dev.client.YuCongMingClient;
import com.yupi.yucongming.dev.common.BaseResponse;
import com.yupi.yucongming.dev.model.DevChatRequest;
import com.yupi.yucongming.dev.model.DevChatResponse;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 对接 AI 平台
 * 
 * @author xys
 */
@Service
public class AiManager {
    @Resource
    private YuCongMingClient aiClient;

    /**
     * 向 AI 发起对话
     * @param modelId AI 模型id
     * @param message 向 AI 发起的对话消息
     * @return AI响应
     */
    public String doAiChat(long modelId,String message){
        DevChatRequest devChatRequest = new DevChatRequest();
        devChatRequest.setModelId(modelId);
        devChatRequest.setMessage(message);
        BaseResponse<DevChatResponse> response = aiClient.doChat(devChatRequest);
        if(response == null){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"AI 响应错误");
        }
        return response.getData().getContent();
    }
}
