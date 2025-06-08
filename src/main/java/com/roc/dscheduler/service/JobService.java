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
import java.util.*;
import java.util.stream.Collectors;

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
     * Retrieves paginated list of jobs with their details and triggers.
     *
     * @param page The page number (1-based)
     * @param size The number of items per page
     * @param sort The field to sort by (jobName, jobGroup, triggerState)
     * @param order The sort order (asc/desc)
     * @return List of JobInfo objects for the requested page
     * @throws SchedulerException if retrieval fails
     */
    public List<JobInfo> getAllJobs(int page, int size, String sort, String order) throws SchedulerException {
        // Get all job keys first (this is lightweight)
        List<JobKey> jobKeys = new ArrayList<>(scheduler.getJobKeys(GroupMatcher.anyJobGroup()));
        int totalJobs = jobKeys.size();

        // Calculate pagination bounds
        int totalPages = (int) Math.ceil((double) totalJobs / size);
        page = Math.max(1, Math.min(page, totalPages));
        int fromIndex = (page - 1) * size;
        int toIndex = Math.min(fromIndex + size, totalJobs);

        // Sort job keys based on the requested field and order
        Comparator<JobKey> keyComparator;
        switch (sort) {
            case "jobGroup":
                keyComparator = Comparator.comparing(JobKey::getGroup, String.CASE_INSENSITIVE_ORDER);
                break;
            case "triggerState":
                // For trigger state, we'll need to sort after loading job details
                keyComparator = Comparator.comparing(JobKey::getName);
                break;
            default: // jobName
                keyComparator = Comparator.comparing(JobKey::getName, String.CASE_INSENSITIVE_ORDER);
                break;
        }

        if ("desc".equalsIgnoreCase(order)) {
            keyComparator = keyComparator.reversed();
        }

        // Sort the keys
        jobKeys.sort(keyComparator);

        // Get the sublist for the current page
        List<JobKey> pagedJobKeys = jobKeys.subList(fromIndex, toIndex);

        // Now fetch details only for the jobs on the current page
        List<JobInfo> jobInfos = pagedJobKeys.stream().map(jobKey -> {
            try {
                JobDetail jobDetail = scheduler.getJobDetail(jobKey);
                List<? extends Trigger> triggers = scheduler.getTriggersOfJob(jobKey);

                JobInfo jobInfo = new JobInfo();
                jobInfo.setJobName(jobKey.getName());
                jobInfo.setJobGroup(jobKey.getGroup());
                jobInfo.setJobClass(jobDetail.getJobClass().getName());
                jobInfo.setDescription(jobDetail.getDescription());

                if (!triggers.isEmpty()) {
                    Trigger trigger = triggers.get(0);
                    jobInfo.setTriggerState(scheduler.getTriggerState(trigger.getKey()).name());
                    jobInfo.setPreviousFireTime(toLocalDateTime(trigger.getPreviousFireTime()));
                    jobInfo.setNextFireTime(toLocalDateTime(trigger.getNextFireTime()));
                    if (trigger instanceof CronTrigger) {
                        jobInfo.setCronExpression(((CronTrigger) trigger).getCronExpression());
                    }
                } else {
                    jobInfo.setTriggerState("NO_TRIGGER");
                }
                return jobInfo;
            } catch (SchedulerException e) {
                log.error("Error fetching job details for {}:{}", jobKey.getGroup(), jobKey.getName(), e);
                return null;
            }
        }).filter(Objects::nonNull).collect(Collectors.toList());

        // If sorting by triggerState, we need to sort after loading the state
        if ("triggerState".equals(sort)) {
            Comparator<JobInfo> stateComparator = Comparator.comparing(
                    JobInfo::getTriggerState,
                    String.CASE_INSENSITIVE_ORDER);

            if ("desc".equalsIgnoreCase(order)) {
                stateComparator = stateComparator.reversed();
            }

            jobInfos.sort(stateComparator);
        }

        return jobInfos;
    }

    /**
     * Gets the total count of all jobs.
     *
     * @return total number of jobs
     * @throws SchedulerException if retrieval fails
     */
    public int getTotalJobCount() throws SchedulerException {
        return scheduler.getJobKeys(GroupMatcher.anyJobGroup()).size();
    }

    private LocalDateTime toLocalDateTime(Date date) {
        return (date == null) ? null : date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }
}