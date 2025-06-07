package com.roc.dscheduler.job;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * A sample job that logs its execution time.
 * Implements {@link Job} for Quartz execution.
 * {@link DisallowConcurrentExecution} prevents multiple instances of this job
 * from running concurrently if the previous run hasn't finished.
 * {@link Component} allows Spring to manage this bean if needed, though Quartz
 * will instantiate it via the JobFactory. If you need to inject Spring beans
 * into this job, ensure AutowiringSpringBeanJobFactory is correctly configured.
 */
@Component // Optional: if you want Spring to be aware of it for other purposes, or if it has Spring dependencies.
@DisallowConcurrentExecution // Prevents concurrent execution of the same job definition
public class SampleJob implements Job {

    private static final Logger log = LoggerFactory.getLogger(SampleJob.class);
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Example of a field that could be injected by Spring if AutowiringSpringBeanJobFactory is used
    // @Autowired
    // private MyService myService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        String jobName = context.getJobDetail().getKey().getName();
        String jobGroup = context.getJobDetail().getKey().getGroup();
        LocalDateTime now = LocalDateTime.now();

        log.info("SampleJob (Name: {}, Group: {}) is executing at: {}. Trigger: {}",
                jobName,
                jobGroup,
                now.format(formatter),
                context.getTrigger().getKey());

        // Example: Accessing JobDataMap
        // String myData = context.getJobDetail().getJobDataMap().getString("myDataKey");
        // log.info("JobDataMap value for 'myDataKey': {}", myData);

        // Simulate some work
        try {
            Thread.sleep(5000); // Sleep for 5 seconds
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("SampleJob execution was interrupted.", e);
            throw new JobExecutionException("SampleJob execution was interrupted.", e, false); // false = don't refire immediately
        }

        log.info("SampleJob (Name: {}, Group: {}) finished execution at: {}.", jobName, jobGroup, LocalDateTime.now().format(formatter));
    }
}