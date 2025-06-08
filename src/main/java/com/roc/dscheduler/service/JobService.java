package com.roc.dscheduler.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.roc.dscheduler.entity.JobInfo;
import com.roc.dscheduler.job.SampleJob;
import org.quartz.*;
import org.quartz.impl.matchers.GroupMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class JobService {

    private static final Logger log = LoggerFactory.getLogger(JobService.class);
    private static final String JOB_KEYS_CACHE_KEY = "allJobKeys";
    private final Scheduler scheduler;
    private final Cache<String, List<? extends Trigger>> triggerCache;
    private final Cache<String, Boolean> jobKeysCache;
    private final Cache<String, JobDetail> jobDetailCache;

    @Autowired
    public JobService(Scheduler scheduler,
                     @Qualifier("triggerCache") Cache<String, List<? extends Trigger>> triggerCache,
                     @Qualifier("jobKeysCache") Cache<String, Boolean> jobKeysCache,
                     @Qualifier("jobDetailCache") Cache<String, JobDetail> jobDetailCache) {
        this.scheduler = scheduler;
        this.triggerCache = triggerCache;
        this.jobKeysCache = jobKeysCache;
        this.jobDetailCache = jobDetailCache;
    }
    
    private void createTestTasks() {
        String[] groups = {"g2", "g3", "g4", "g5", "g6"};
        int tasksPerGroup = 100;
        int startTaskNumber = 200;  // Starting from test200
        
        for (String group : groups) {
            for (int i = 0; i < tasksPerGroup; i++) {
                String jobName = "test" + (startTaskNumber + i);
                JobInfo jobInfo = new JobInfo();
                jobInfo.setJobName(jobName);
                jobInfo.setJobGroup(group);
                jobInfo.setJobClass(SampleJob.class.getName());
                jobInfo.setDescription("Test job " + jobName + " in group " + group);
                jobInfo.setCronExpression("0 0 0/1 * * ?");
                
                try {
                    if (!scheduler.checkExists(JobKey.jobKey(jobName, group))) {
                        scheduleJob(jobInfo);
                        log.info("Created test job: {}.{}", group, jobName);
                    }
                } catch (Exception e) {
                    log.error("Failed to create test job: {}.{}", group, jobName, e);
                }
            }
        }
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

        // Update cache after scheduling
        updateJobKeysCache(scheduler.getJobKeys(GroupMatcher.anyJobGroup()));
    }

    /**
     * Pauses a job.
     *
     * @param jobName  Name of the job.
     * @param jobGroup Group of the job.
     * @throws SchedulerException if pausing fails.
     */
    public void pauseJob(String jobName, String jobGroup) throws SchedulerException {
        JobKey jobKey = JobKey.jobKey(jobName, jobGroup);
        scheduler.pauseJob(jobKey);
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
        JobKey jobKey = JobKey.jobKey(jobName, jobGroup);
        scheduler.resumeJob(jobKey);
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
        JobKey jobKey = JobKey.jobKey(jobName, jobGroup);
        scheduler.deleteJob(jobKey);
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
        Set<JobKey> currentJobKeys = scheduler.getJobKeys(GroupMatcher.anyJobGroup());

        // Get cached job keys
        Set<JobKey> cachedJobKeys = getCachedJobKeys();
        boolean jobKeysChanged = !currentJobKeys.equals(cachedJobKeys);

        if (jobKeysChanged) {
            // Update job keys cache
            updateJobKeysCache(currentJobKeys);
            // Invalidate all trigger caches if job keys changed
            triggerCache.invalidateAll();
        }

        for (JobKey jobKey : currentJobKeys) {
            String cacheKey = jobKey.getGroup() + "." + jobKey.getName();
            
            // Get job details and triggers from cache or scheduler
            JobDetail jobDetail = getOrLoadJobDetail(jobKey, cacheKey, jobKeysChanged);
            List<? extends Trigger> triggers = getOrLoadTriggers(jobKey, cacheKey, jobKeysChanged);
            
            if (jobDetail != null) {
                JobInfo jobInfo = createJobInfo(jobKey, jobDetail, triggers);
                jobInfos.add(jobInfo);
            }
        }

        log.debug("Job cache stats: {}", triggerCache.stats());
        return jobInfos;
    }

    private Set<JobKey> getCachedJobKeys() {
        return jobKeysCache.asMap().keySet().stream()
                .filter(key -> !key.equals(JOB_KEYS_CACHE_KEY))
                .map(key -> {
                    String[] parts = key.split("\\.", 2);
                    return JobKey.jobKey(parts[1], parts[0]);
                })
                .collect(Collectors.toSet());
    }

    private void updateJobKeysCache(Set<JobKey> jobKeys) {
        jobKeysCache.invalidateAll();
        jobKeys.forEach(jobKey ->
                jobKeysCache.put(jobKey.getGroup() + "." + jobKey.getName(), true)
        );
    }

    @SuppressWarnings("unchecked")
    private List<? extends Trigger> getOrLoadTriggers(JobKey jobKey, String cacheKey, boolean forceReload) throws SchedulerException {
        if (forceReload) {
            List<? extends Trigger> triggers = scheduler.getTriggersOfJob(jobKey);
            triggerCache.put(cacheKey, triggers);
            return triggers;
        }

        return triggerCache.get(cacheKey, key -> {
            try {
                return scheduler.getTriggersOfJob(jobKey);
            } catch (SchedulerException e) {
                log.error("Error loading triggers for job: {}", key, e);
                return Collections.emptyList();
            }
        });
    }
    
    /**
     * Get job details from cache or load from scheduler if not in cache
     */
    private JobDetail getOrLoadJobDetail(JobKey jobKey, String cacheKey, boolean forceReload) throws SchedulerException {
        if (forceReload) {
            JobDetail jobDetail = scheduler.getJobDetail(jobKey);
            if (jobDetail != null) {
                jobDetailCache.put(cacheKey, jobDetail);
            }
            return jobDetail;
        }

        return jobDetailCache.get(cacheKey, key -> {
            try {
                return scheduler.getJobDetail(jobKey);
            } catch (SchedulerException e) {
                log.error("Error loading job detail for job: {}", key, e);
                return null;
            }
        });
    }

    private JobInfo createJobInfo(JobKey jobKey, JobDetail jobDetail, List<? extends Trigger> triggers) throws SchedulerException {
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
            jobInfo.setTriggerState("NO_TRIGGER");
        }
        return jobInfo;
    }

    private LocalDateTime toLocalDateTime(Date date) {
        return (date == null) ? null : date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    /**
     * Manually invalidate cache for a specific job
     * @param jobKey The job key to invalidate cache for
     */
    public void invalidateJobCache(JobKey jobKey) {
        if (jobKey != null) {
            String cacheKey = jobKey.getGroup() + "." + jobKey.getName();
            triggerCache.invalidate(cacheKey);
            jobDetailCache.invalidate(cacheKey);
            // Also update the job keys cache
            try {
                updateJobKeysCache(scheduler.getJobKeys(GroupMatcher.anyJobGroup()));
            } catch (SchedulerException e) {
                log.error("Failed to update job keys cache after invalidation", e);
            }
        }
    }
}
