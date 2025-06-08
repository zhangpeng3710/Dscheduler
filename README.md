# Dscheduler - 分布式任务调度系统

[![Java Version](https://img.shields.io/badge/Java-8%2B-blue.svg)](https://www.java.com/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.7.18-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Quartz](https://img.shields.io/badge/Quartz-2.3.2-orange.svg)](http://www.quartz-scheduler.org/)

## 项目介绍

Dscheduler 是一个基于 Spring Boot 和 Quartz 的分布式任务调度系统，提供了简单易用的 Web 界面来管理定时任务。系统支持动态添加、修改、暂停、恢复和删除任务，并提供了任务执行日志查询功能。

## 功能特点

- 🚀 基于 Spring Boot 2 和 Quartz 构建
- 🌐 响应式 Web 界面，支持移动设备
- 🔄 支持 Cron 表达式配置任务执行时间
- ⏯️ 任务动态管理：添加、修改、暂停、恢复、删除
- 📊 任务执行日志记录与查询
- 🔒 基于 Spring Security 的安全认证
- 📱 响应式设计，适配各种屏幕尺寸

## 技术栈

- **后端**
  - Java 8
  - Spring Boot 2.7.18
  - Spring Security
  - Quartz

- **前端**
  - Thymeleaf 模板引擎
  - Bootstrap 5
  - jQuery
  - Font Awesome 图标库

## 快速开始

### 安装步骤

1. 克隆项目
2. 添加自己的任务类
3. 配置数据库、Quartz、Spring Security

## 使用指南

### 登录系统访问应用, 默认配置下
- 打开浏览器访问：http://localhost:8080
- 默认用户名：admin
- 默认密码：password

### 创建新任务

1. 点击导航栏中的"添加定时任务"
2. 填写任务信息：
   - 任务名称
   - 任务组
   - 任务类名（实现 `org.quartz.Job` 接口）
   - Cron 表达式
   - 任务描述（可选）
3. 点击"保存"按钮

### 管理任务

- **启动/暂停任务**：点击任务列表中的开关按钮
- **编辑任务**：点击编辑图标
- **删除任务**：点击删除图标
- **立即执行**：点击立即执行按钮

### 查看执行日志

在任务列表中，点击"日志"按钮查看任务的执行历史记录。

## 开发指南

### 自定义任务开发

1. 可复制SampleJob类作为模板，创建自定义任务类。
2. 在 `execute` 方法中实现任务逻辑
3. 将类放在 `com.roc.dscheduler.job` 包下（或配置的扫描路径）

示例任务类：

```java
@Component
@DisallowConcurrentExecution
public class MyCustomJob implements Job {
    
    private static final Logger logger = LoggerFactory.getLogger(MyCustomJob.class);
    
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        logger.info("执行自定义任务: {}", new Date());
        // 在这里实现你的业务逻辑
    }
}
```

## 生产环境

可根据安全规范，支持关闭页面访问
在生产环境中，建议关闭页面访问，以防止用户直接访问系统。您可以在 `application.properties` 文件中添加以下配置来禁用页面访问：
```yml
spring:
    main:
        web-application-type: none
```
或在运行项目时，您可以使用以下命令启动应用程序：
```bash
java -jar your-app.jar -Dspring.main.web-application-type=none
```
