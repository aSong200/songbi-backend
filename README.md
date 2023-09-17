# 松鼠智能 BI
一个智能数据分析平台，可自动生成数据的可视化图表及分析结论，实现数据分析的降本增效，降低数据分析的门槛。

> 项目访问地址  [松鼠智能 BI](http://101.34.64.216/)    (http://101.34.64.216/)
> 用户名：root 
> 密码：12345678  

## 项目背景 📚

 1. 传统数据分析工作需要经历繁琐的数据处理和可视化工作，复杂且耗时。
 2. 传统数据分析需要数据分析者具备一定的专业知识和技术，限制了非专业人士的参与。
 3. AI 快速发展，可利用 AI 的技术，导入需要分析的数据，告知 AI 需要的分析目标，即可自动生成符合要求的图表和分析结论。
 4. 智能分析可降低数据分析的门槛，让大家都可以对需要分析的数据进行可视化和生成分析结论。

## 项目结构图 
**未优化的基础架构：**
![image](https://github.com/aSong200/songbi-backend/assets/140307645/6618e826-8fd0-4050-a21b-b1f27f46cf0c)


**优化后的架构（异步化）：**
![image](https://github.com/aSong200/songbi-backend/assets/140307645/f078c989-f0bf-47d6-ba37-30d72efd6649)


## 项目展示
测试文件在文件夹中
### 1. 智能分析（同步）
**1.1 提交智能分析请求后，只能阻塞等待**
![a8f645413802a6e31c07c58312da13e](https://github.com/aSong200/songbi-backend/assets/140307645/72055c91-1716-4a4c-9966-c184b502c20c)

**1.2 分析成功后**
![a373381b05dd46d5ff6d7e417eec5d0](https://github.com/aSong200/songbi-backend/assets/140307645/816a6f33-867c-4caf-9fda-472d531a740b)

### 2.智能分析（异步）
**1.1 提交智能分析请求后，立刻响应前端提交任务成功，可向 我的图表 中查看图表生成状况**
![image](https://github.com/aSong200/songbi-backend/assets/140307645/5ed6aa93-22a6-4059-b4cf-9781e2538c2e)

![e876562754f105413ece2dc835c38a1](https://github.com/aSong200/songbi-backend/assets/140307645/dd5202d5-59d2-47c3-90cc-7ce7d3fbebca)

**1.2 去 我的图表 中查看图表生成状况**
![81c6806da25a8add54ef457bd636c51](https://github.com/aSong200/songbi-backend/assets/140307645/60647c93-d73d-4cdb-a44b-d93893b3014b)
