package com.roc.dscheduler.listener;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class GlobalJobListener implements JobListener {

    private static final Logger log = LoggerFactory.getLogger(GlobalJobListener.class);
    private static final String LISTENER_NAME = "GlobalJobListener";

    @Override
    public String getName() {
        return LISTENER_NAME;
    }

    @Override
    public void jobToBeExecuted(JobExecutionContext context) {
        String jobName = context.getJobDetail().getKey().getName();
        log.info("Job Listener: {} is about to be executed.", jobName);
    }

    @Override
    public void jobExecutionVetoed(JobExecutionContext context) {
        String jobName = context.getJobDetail().getKey().getName();
        log.info("Job Listener: {} was vetoed and will not be executed.", jobName);
    }

    @Override
    public void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException) {
        String jobName = context.getJobDetail().getKey().getName();
        if (jobException != null) {
            log.error("Job Listener: {} execution failed with exception: {}", jobName, jobException.getMessage(), jobException);
        } else {
            log.info("Job Listener: {} was executed successfully.", jobName);
        }
    }
}