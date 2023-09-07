package com.asong.songbi.service.impl;

import cn.hutool.core.io.FileUtil;
import com.asong.songbi.common.ErrorCode;
import com.asong.songbi.constant.CommonConstant;
import com.asong.songbi.exception.BusinessException;
import com.asong.songbi.exception.ThrowUtils;
import com.asong.songbi.manager.AiManager;
import com.asong.songbi.manager.RedisLimiterManager;
import com.asong.songbi.model.dto.chart.GenChartByAiRequest;
import com.asong.songbi.model.dto.chart.ReGenRequest;
import com.asong.songbi.model.entity.User;
import com.asong.songbi.model.enums.ChartStatusEnum;
import com.asong.songbi.model.vo.BiResponse;
import com.asong.songbi.mq.BiMessageProducer;
import com.asong.songbi.service.UserService;
import com.asong.songbi.utils.ExcelUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.asong.songbi.model.entity.Chart;
import com.asong.songbi.service.ChartService;
import com.asong.songbi.mapper.ChartMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 图表信息表
 * 
 * @author xys
*/
@Service
public class ChartServiceImpl extends ServiceImpl<ChartMapper, Chart>
    implements ChartService{

    @Resource
    UserService userService;
    
    @Resource
    RedisLimiterManager redisLimiterManager;

    @Resource
    private BiMessageProducer biMessageProducer;

    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    @Resource
    private AiManager aiManager;
    
    @Override
    public BiResponse genChartByAiAsyncMq(MultipartFile multipartFile, GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) {
        String name = genChartByAiRequest.getName();
        String goal = genChartByAiRequest.getGoal();
        String chartType = genChartByAiRequest.getChartType();
        //校验参数
        ThrowUtils.throwIf(StringUtils.isBlank(goal), ErrorCode.PARAMS_ERROR, "图表分析目标为空");
        ThrowUtils.throwIf(StringUtils.isBlank(chartType), ErrorCode.PARAMS_ERROR, "图表类型为空");
        ThrowUtils.throwIf(StringUtils.isNotBlank(name) && name.length() > 64, ErrorCode.PARAMS_ERROR, "图表名称过长");
        //校验文件大小和类型
        long size = multipartFile.getSize();
        String originalFileName = multipartFile.getOriginalFilename();
        //校验文件大小
        final long ONE_MB = 1024 * 1024L;
        ThrowUtils.throwIf(size > ONE_MB, ErrorCode.PARAMS_ERROR, "文件过大，超过 1M");
        //校验文件后缀 xlsx xls  只允许上传excel文件
        String suffix = FileUtil.getSuffix(originalFileName);
        final List<String> validFileSuffixList = Arrays.asList("xlsx", "xls");
        ThrowUtils.throwIf(!validFileSuffixList.contains(suffix), ErrorCode.PARAMS_ERROR, "非法文件后缀");
        //获取当前登录用户 做限流操作
        User loginUser = userService.getLoginUser(request);
        //限流判断 每个用户一个限流器
        redisLimiterManager.doRateLimit("genChartByAi_"+loginUser.getId());

        long biModelId = CommonConstant.BI_MODEL_ID;
        /*
          例子
          分析需求：
          分析网站用户的增长情况以及未来趋势 请使用折线图
          原始数据：
          日期,用户数
          1号,10
          2号,30
          3号,40
          4号,30
          5号,35
         */

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
        //压缩转换成csv后的数据
        String csvData = ExcelUtils.excelToCsv(multipartFile);
        userInput.append(csvData).append("\n");

        // 1.插入到数据库
        Chart chart = new Chart();
        chart.setName(name);
        chart.setGoal(goal);
        chart.setChartData(csvData);
        chart.setChartType(chartType);
        // 图表为wait待生成状态
        chart.setStatus(ChartStatusEnum.WAIT.getStatus());
        chart.setUserId(loginUser.getId());
        boolean saveResult = this.save(chart);
        ThrowUtils.throwIf(!saveResult, ErrorCode.SYSTEM_ERROR, "图表保存失败");

        // 2.将图表id发送到消息队列
        long newChartId = chart.getId();
        biMessageProducer.sendMessage(String.valueOf(newChartId));

        BiResponse biResponse = new BiResponse();
        biResponse.setChartId(chart.getId());
        return biResponse;
    }

    @Override
    public BiResponse reGenChartByAi(ReGenRequest reGenRequest, HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        long chartId = reGenRequest.getId();
        // 判断是否存在
        Chart chart = this.getById(chartId);
        ThrowUtils.throwIf(chart == null ,ErrorCode.NOT_FOUND_ERROR);
        // 仅限本人或管理员可重新生成
        if(!chart.getUserId().equals(user.getId()) && !userService.isAdmin(request)){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 将图表id发送到消息队列
        biMessageProducer.sendMessage(String.valueOf(chartId));
        BiResponse biResponse = new BiResponse();
        biResponse.setChartId(chart.getId());
        return biResponse;
    }

    @Override
    public BiResponse genChartByAi(MultipartFile multipartFile, GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) {
        String name = genChartByAiRequest.getName();
        String goal = genChartByAiRequest.getGoal();
        String chartType = genChartByAiRequest.getChartType();
        //校验请求参数
        ThrowUtils.throwIf(StringUtils.isBlank(goal), ErrorCode.PARAMS_ERROR, "图表分析目标为空");
        ThrowUtils.throwIf(StringUtils.isBlank(chartType), ErrorCode.PARAMS_ERROR, "图表类型为空");
        ThrowUtils.throwIf(StringUtils.isNotBlank(name) && name.length() > 64, ErrorCode.PARAMS_ERROR, "图表名称过长");
        //校验文件大小和类型
        long size = multipartFile.getSize();
        String originalFileName = multipartFile.getOriginalFilename();
        //校验文件大小
        final long ONE_MB = 1024 * 1024L;
        ThrowUtils.throwIf(size > ONE_MB, ErrorCode.PARAMS_ERROR, "文件过大，超过 1M");
        //校验文件类型 只允许上传excel文件
        String suffix = FileUtil.getSuffix(originalFileName);
        final List<String> validFileSuffixList = Arrays.asList("xlsx", "xls");
        ThrowUtils.throwIf(!validFileSuffixList.contains(suffix), ErrorCode.PARAMS_ERROR, "非法文件后缀");

        //获取当前登录用户 做限流操作
        User loginUser = userService.getLoginUser(request);
        //限流判断 每个用户一个限流器
        redisLimiterManager.doRateLimit("genChartByAi_"+loginUser.getId());
        long biModelId = CommonConstant.BI_MODEL_ID;
        /*
          分析需求：
          分析网站用户的增长情况以及未来趋势
          原始数据：
          日期,用户数
          1号,10
          2号,30
          3号,40
          4号,30
          5号,35
         */

        //构造用户输入的需求
        StringBuilder userInput = new StringBuilder();
        userInput.append("分析需求：").append("\n");
        //拼接分析目标 图表类型
        String biGoal = goal;
        userInput.append(biGoal);
        if (StringUtils.isNotBlank(chartType)) {
            userInput.append(" 请使用").append(chartType);
        }
        userInput.append("\n").append("原始数据：").append("\n");
        //压缩转换成csv后的数据
        String csvData = ExcelUtils.excelToCsv(multipartFile);
        userInput.append(csvData).append("\n");

        //提问 AI 模型
        String result = aiManager.doAiChat(biModelId, userInput.toString());
        //设置了以 @@@ 分隔
        String[] splits = result.split("@@@");
        if (splits.length < 3) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI 生成错误");
        }
        //生成的图表js 和 分析结果
        String genChart = splits[1].trim();
        String genResult = splits[2].trim();
        //插入到如表数据库中
        Chart chart = new Chart();
        chart.setName(name);
        chart.setGoal(goal);
        chart.setChartData(csvData);
        chart.setChartType(chartType);
        chart.setGenChart(genChart);
        chart.setGenResult(genResult);
        chart.setUserId(loginUser.getId());
        //设置图表状态为succeed成功
        chart.setStatus(ChartStatusEnum.SUCCEED.getStatus());
        boolean isSave = this.save(chart);
        ThrowUtils.throwIf(!isSave, ErrorCode.SYSTEM_ERROR, "图表保存失败");
        BiResponse biResponse = new BiResponse();
        biResponse.setGenChart(genChart);
        biResponse.setGenResult(genResult);
        biResponse.setChartId(chart.getId());
        return biResponse;
    }

    @Override
    public BiResponse genChartByAiAsync(MultipartFile multipartFile, GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) {
        String name = genChartByAiRequest.getName();
        String goal = genChartByAiRequest.getGoal();
        String chartType = genChartByAiRequest.getChartType();
        //校验请求参数
        ThrowUtils.throwIf(StringUtils.isBlank(goal), ErrorCode.PARAMS_ERROR, "图表分析目标为空");
        ThrowUtils.throwIf(StringUtils.isBlank(chartType), ErrorCode.PARAMS_ERROR, "图表类型为空");
        ThrowUtils.throwIf(StringUtils.isNotBlank(name) && name.length() > 64, ErrorCode.PARAMS_ERROR, "图表名称过长");
        //校验文件大小和类型
        long size = multipartFile.getSize();
        String originalFileName = multipartFile.getOriginalFilename();
        //校验文件大小
        final long ONE_MB = 1024 * 1024L;
        ThrowUtils.throwIf(size > ONE_MB, ErrorCode.PARAMS_ERROR, "文件过大，超过 1M");
        //校验文件后缀 xlsx xls  只允许上传excel文件
        String suffix = FileUtil.getSuffix(originalFileName);
        final List<String> validFileSuffixList = Arrays.asList("xlsx", "xls");
        ThrowUtils.throwIf(!validFileSuffixList.contains(suffix), ErrorCode.PARAMS_ERROR, "非法文件后缀");

        //获取当前登录用户 做限流操作
        User loginUser = userService.getLoginUser(request);
        //限流判断 每个用户一个限流器
        redisLimiterManager.doRateLimit("genChartByAi_"+loginUser.getId());

        long biModelId = CommonConstant.BI_MODEL_ID;
        /*
          例子
          分析需求：
          分析网站用户的增长情况以及未来趋势 请使用折线图
          原始数据：
          日期,用户数
          1号,10
          2号,30
          3号,40
          4号,30
          5号,35
         */

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
        //压缩转换成csv后的数据
        String csvData = ExcelUtils.excelToCsv(multipartFile);
        userInput.append(csvData).append("\n");

        // 1.插入到数据库
        Chart chart = new Chart();
        chart.setName(name);
        chart.setGoal(goal);
        chart.setChartData(csvData);
        chart.setChartType(chartType);
        // 图表为wait待生成状态
        chart.setStatus(ChartStatusEnum.WAIT.getStatus());
        chart.setUserId(loginUser.getId());
        boolean saveResult = this.save(chart);
        ThrowUtils.throwIf(!saveResult, ErrorCode.SYSTEM_ERROR, "图表保存失败");

        // 2.异步调用AI模型，生成图表
        CompletableFuture.runAsync(() -> {
            // 先修改图表任务状态为 “running 执行中”。等执行成功后，修改为 “已完成”、保存执行结果；执行失败后，状态修改为 “失败”，记录任务失败信息。
            Chart updateChart = new Chart();
            updateChart.setId(chart.getId());
            updateChart.setStatus(ChartStatusEnum.RUNNING.getStatus());
            boolean b = this.updateById(updateChart);
            if (!b) {
                handleChartUpdateError(chart.getId(), "更新图表执行中状态失败");
                return;
            }
            // 调用 AI 生成图表
            String result = aiManager.doAiChat(biModelId, userInput.toString());
            String[] splits = result.split("@@@");
            if (splits.length < 3) {
                handleChartUpdateError(chart.getId(), "AI 生成错误");
                return;
            }
            String genChart = splits[1].trim();
            String genResult = splits[2].trim();
            Chart updateChartResult = new Chart();
            updateChartResult.setId(chart.getId());
            updateChartResult.setGenChart(genChart);
            updateChartResult.setGenResult(genResult);
            //图表生成成功，更新图表状态为“succeed 成功”
            updateChartResult.setStatus(ChartStatusEnum.SUCCEED.getStatus());
            boolean updateResult = this.updateById(updateChartResult);
            if (!updateResult) {
                handleChartUpdateError(chart.getId(), "更新图表成功状态失败");
            }
        }, threadPoolExecutor);

        BiResponse biResponse = new BiResponse();
        biResponse.setChartId(chart.getId());
        return biResponse;
    }

    @Override
    public void validChart(Chart chart, boolean add) {
        if(chart == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String goal = chart.getGoal();
        String chartData = chart.getChartData();
        //创建时，参数不能为空
        if(add){
            ThrowUtils.throwIf(StringUtils.isAnyBlank(goal,chartData),ErrorCode.PARAMS_ERROR);
        }
    }

    private void handleChartUpdateError(long chartId, String execMessage) {
        Chart updateChartResult = new Chart();
        updateChartResult.setId(chartId);
        //更新图表状态为“failed 失败”
        updateChartResult.setStatus(ChartStatusEnum.FAILED.getStatus());
        updateChartResult.setExecMessage(execMessage);
        boolean updateResult = this.updateById(updateChartResult);
        if (!updateResult) {
            log.error("更新图表失败状态失败" + chartId + "," + execMessage);
        }
    }
}




