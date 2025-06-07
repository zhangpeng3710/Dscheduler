package com.roc.dscheduler.service;

import com.roc.dscheduler.dto.JobDTO;
import org.quartz.*;
import org.quartz.impl.matchers.GroupMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
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

    @Autowired
    private SchedulerFactoryBean schedulerFactoryBean;

    /**
     * Schedules a new job.
     *
     * @param jobDTO DTO containing job details.
     * @throws SchedulerException if scheduling fails.
     * @throws ClassNotFoundException if the job class is not found.
     */
    public void scheduleJob(JobDTO jobDTO) throws SchedulerException, ClassNotFoundException {
        Scheduler scheduler = schedulerFactoryBean.getScheduler();
        JobKey jobKey = JobKey.jobKey(jobDTO.getJobName(), jobDTO.getJobGroup());

        if (scheduler.checkExists(jobKey)) {
            log.warn("Job {} in group {} already exists. It will be updated.", jobDTO.getJobName(), jobDTO.getJobGroup());
            // Optionally delete and recreate, or update. For simplicity, we'll allow Quartz to update if overwriteExistingJobs is true.
            // scheduler.deleteJob(jobKey);
        }

        @SuppressWarnings("unchecked")
        Class<? extends Job> jobClass = (Class<? extends Job>) Class.forName(jobDTO.getJobClass());

        JobDetail jobDetail = JobBuilder.newJob(jobClass)
                .withIdentity(jobKey)
                .withDescription(jobDTO.getDescription())
                .storeDurably() // Important if the job is to exist without triggers
                .build();

        Trigger trigger = TriggerBuilder.newTrigger()
                .forJob(jobDetail)
                .withIdentity(jobDTO.getJobName() + "_trigger", jobDTO.getJobGroup())
                .withDescription(jobDTO.getDescription())
                .withSchedule(CronScheduleBuilder.cronSchedule(jobDTO.getCronExpression())
                        .withMisfireHandlingInstructionFireAndProceed()) // Example misfire instruction
                .build();

        scheduler.scheduleJob(jobDetail, trigger);
        log.info("Scheduled job: {} in group: {} with cron: {}", jobDTO.getJobName(), jobDTO.getJobGroup(), jobDTO.getCronExpression());
    }

    /**
     * Pauses a job.
     *
     * @param jobName  Name of the job.
     * @param jobGroup Group of the job.
     * @throws SchedulerException if pausing fails.
     */
    public void pauseJob(String jobName, String jobGroup) throws SchedulerException {
        Scheduler scheduler = schedulerFactoryBean.getScheduler();
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
        Scheduler scheduler = schedulerFactoryBean.getScheduler();
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
        Scheduler scheduler = schedulerFactoryBean.getScheduler();
        scheduler.deleteJob(JobKey.jobKey(jobName, jobGroup));
        log.info("Deleted job: {} in group: {}", jobName, jobGroup);
    }

    /**
     * Retrieves all scheduled jobs.
     *
     * @return List of JobDTOs.
     * @throws SchedulerException if retrieval fails.
     */
    public List<JobDTO> getAllJobs() throws SchedulerException {
        Scheduler scheduler = schedulerFactoryBean.getScheduler();
        List<JobDTO> jobDTOs = new ArrayList<>();
        Set<JobKey> jobKeys = scheduler.getJobKeys(GroupMatcher.anyJobGroup());

        for (JobKey jobKey : jobKeys) {
            JobDetail jobDetail = scheduler.getJobDetail(jobKey);
            List<? extends Trigger> triggers = scheduler.getTriggersOfJob(jobKey);

            JobDTO jobDTO = new JobDTO();
            jobDTO.setJobName(jobKey.getName());
            jobDTO.setJobGroup(jobKey.getGroup());
            jobDTO.setJobClass(jobDetail.getJobClass().getName());
            jobDTO.setDescription(jobDetail.getDescription());

            if (!triggers.isEmpty()) {
                Trigger trigger = triggers.get(0); // Assuming one trigger per job for simplicity
                jobDTO.setTriggerState(scheduler.getTriggerState(trigger.getKey()).name());
                jobDTO.setPreviousFireTime(toLocalDateTime(trigger.getPreviousFireTime()));
                jobDTO.setNextFireTime(toLocalDateTime(trigger.getNextFireTime()));
                if (trigger instanceof CronTrigger) {
                    jobDTO.setCronExpression(((CronTrigger) trigger).getCronExpression());
                }
            } else {
                jobDTO.setTriggerState("NO_TRIGGER"); // Job exists but has no trigger
            }
            jobDTOs.add(jobDTO);
        }
        return jobDTOs;
    }

    private LocalDateTime toLocalDateTime(Date date) {
        return (date == null) ? null : date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }
}