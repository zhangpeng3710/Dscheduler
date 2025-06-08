package com.roc.dscheduler.entity;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

public class JobInfo {


    @NotBlank(message = "Job name cannot be blank")
    @Size(max = 200, message = "Job name must be less than 200 characters")
    private String jobName;

    @NotBlank(message = "Job group cannot be blank")
    @Size(max = 200, message = "Job group must be less than 200 characters")
    private String jobGroup;

    @NotBlank(message = "Job class cannot be blank")
    @Size(max = 255, message = "Job class name must be less than 255 characters")
    private String jobClass; // Fully qualified class name of the job

    @NotBlank(message = "Cron expression cannot be blank")
    private String cronExpression;

    @Size(max = 250, message = "Description must be less than 250 characters")
    private String description;

    private String triggerState; // e.g., NORMAL, PAUSED, ERROR
    private LocalDateTime previousFireTime;
    private LocalDateTime nextFireTime;

    // Getters and Setters
    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public String getJobGroup() {
        return jobGroup;
    }

    public void setJobGroup(String jobGroup) {
        this.jobGroup = jobGroup;
    }

    public String getJobClass() {
        return jobClass;
    }

    public void setJobClass(String jobClass) {
        this.jobClass = jobClass;
    }

    public String getCronExpression() {
        return cronExpression;
    }

    public void setCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTriggerState() {
        return triggerState;
    }

    public void setTriggerState(String triggerState) {
        this.triggerState = triggerState;
    }

    public LocalDateTime getPreviousFireTime() {
        return previousFireTime;
    }

    public void setPreviousFireTime(LocalDateTime previousFireTime) {
        this.previousFireTime = previousFireTime;
    }

    public LocalDateTime getNextFireTime() {
        return nextFireTime;
    }

    public void setNextFireTime(LocalDateTime nextFireTime) {
        this.nextFireTime = nextFireTime;
    }

    // Constructors
    public JobInfo() {
    }

    public JobInfo(String jobName, String jobGroup, String jobClass, String cronExpression,
                   String description, String triggerState,
                   LocalDateTime previousFireTime, LocalDateTime nextFireTime) {
        this.jobName = jobName;
        this.jobGroup = jobGroup;
        this.jobClass = jobClass;
        this.cronExpression = cronExpression;
        this.description = description;
        this.triggerState = triggerState;
        this.previousFireTime = previousFireTime;
        this.nextFireTime = nextFireTime;
    }

}