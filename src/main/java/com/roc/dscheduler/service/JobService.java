package com.roc.dscheduler.service;

import com.roc.dscheduler.entity.JobInfo;
import org.quartz.*;
import org.quartz.impl.matchers.GroupMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

@Service
public class JobService {

    private static final Logger log = LoggerFactory.getLogger(JobService.class);
    private final Scheduler scheduler;

    @Autowired
    public JobService(Scheduler scheduler) throws SchedulerException {
        this.scheduler = scheduler;
    }

    /**
     * Schedules a new job.
     *
     * @param jobInfo containing job details.
     * @throws SchedulerException     if scheduling fails.
     * @throws ClassNotFoundException if the job class is not found.
     */
    public void scheduleJob(JobInfo jobInfo) throws SchedulerException, ClassNotFoundException {
        JobKey jobKey = JobKey.jobKey(jobInfo.getJobName(), jobInfo.getJobGroup());

        if (scheduler.checkExists(jobKey)) {
            log.warn("Job {} in group {} already exists. It will be updated.", jobInfo.getJobName(), jobInfo.getJobGroup());
            // Optionally delete and recreate, or update. For simplicity, we'll allow Quartz to update if overwriteExistingJobs is true.
            // scheduler.deleteJob(jobKey);
        }

        @SuppressWarnings("unchecked")
        Class<? extends Job> jobClass = (Class<? extends Job>) Class.forName(jobInfo.getJobClass());

        JobDetail jobDetail = JobBuilder.newJob(jobClass)
                .withIdentity(jobKey)
                .withDescription(jobInfo.getDescription())
                .storeDurably() // Important if the job is to exist without triggers
                .build();

        Trigger trigger = TriggerBuilder.newTrigger()
                .forJob(jobDetail)
                .withIdentity(jobInfo.getJobName() + "_trigger", jobInfo.getJobGroup())
                .withDescription(jobInfo.getDescription())
                .withSchedule(CronScheduleBuilder.cronSchedule(jobInfo.getCronExpression())
                        .withMisfireHandlingInstructionFireAndProceed()) // Example misfire instruction
                .build();

        scheduler.scheduleJob(jobDetail, trigger);
        log.info("Scheduled job: {} in group: {} with cron: {}", jobInfo.getJobName(), jobInfo.getJobGroup(), jobInfo.getCronExpression());
    }

    /**
     * Pauses a job.
     *
     * @param jobName  Name of the job.
     * @param jobGroup Group of the job.
     * @throws SchedulerException if pausing fails.
     */
    public void pauseJob(String jobName, String jobGroup) throws SchedulerException {
        scheduler.pauseJob(JobKey.jobKey(jobName, jobGroup));
        log.info("Paused job: {} in group: {}", jobName, jobGroup);
    }

    /**
     * Resumes a paused job.
     *
     * @param jobName  Name of the job.
     * @param jobGroup Group of the job.
     * @throws SchedulerException if resuming fails.
     */
    public void resumeJob(String jobName, String jobGroup) throws SchedulerException {
        scheduler.resumeJob(JobKey.jobKey(jobName, jobGroup));
        log.info("Resumed job: {} in group: {}", jobName, jobGroup);
    }

    /**
     * Deletes a job.
     *
     * @param jobName  Name of the job.
     * @param jobGroup Group of the job.
     * @throws SchedulerException if deletion fails.
     */
    public void deleteJob(String jobName, String jobGroup) throws SchedulerException {
        scheduler.deleteJob(JobKey.jobKey(jobName, jobGroup));
        log.info("Deleted job: {} in group: {}", jobName, jobGroup);
    }

    /**
     * Retrieves all scheduled jobs.
     *
     * @return List of jobInfos.
     * @throws SchedulerException if retrieval fails.
     */
    public List<JobInfo> getAllJobs() throws SchedulerException {
        List<JobInfo> jobInfos = new ArrayList<>();
        Set<JobKey> jobKeys = scheduler.getJobKeys(GroupMatcher.anyJobGroup());

        for (JobKey jobKey : jobKeys) {
            JobDetail jobDetail = scheduler.getJobDetail(jobKey);
            List<? extends Trigger> triggers = scheduler.getTriggersOfJob(jobKey);

            JobInfo jobInfo = new JobInfo();
            jobInfo.setJobName(jobKey.getName());
            jobInfo.setJobGroup(jobKey.getGroup());
            jobInfo.setJobClass(jobDetail.getJobClass().getName());
            jobInfo.setDescription(jobDetail.getDescription());

            if (!triggers.isEmpty()) {
                Trigger trigger = triggers.get(0); // Assuming one trigger per job for simplicity
                jobInfo.setTriggerState(scheduler.getTriggerState(trigger.getKey()).name());
                jobInfo.setPreviousFireTime(toLocalDateTime(trigger.getPreviousFireTime()));
                jobInfo.setNextFireTime(toLocalDateTime(trigger.getNextFireTime()));
                if (trigger instanceof CronTrigger) {
                    jobInfo.setCronExpression(((CronTrigger) trigger).getCronExpression());
                }
            } else {
                jobInfo.setTriggerState("NO_TRIGGER"); // Job exists but has no trigger
            }
            jobInfos.add(jobInfo);
        }
        return jobInfos;
    }

    private LocalDateTime toLocalDateTime(Date date) {
        return (date == null) ? null : date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }
}