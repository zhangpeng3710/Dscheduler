package com.roc.dscheduler.listener;

import org.quartz.JobExecutionContext;
import org.quartz.Trigger;
import org.quartz.TriggerListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class GlobalTriggerListener implements TriggerListener {

    private static final Logger log = LoggerFactory.getLogger(GlobalTriggerListener.class);
    private static final String LISTENER_NAME = "GlobalTriggerListener";

    @Override
    public String getName() {
        return LISTENER_NAME;
    }

    @Override
    public void triggerFired(Trigger trigger, JobExecutionContext context) {
        String triggerName = trigger.getKey().getName();
        String jobName = context.getJobDetail().getKey().getName();
        log.info("Trigger Listener: {} fired for job {}.", triggerName, jobName);
    }

    @Override
    public boolean vetoJobExecution(Trigger trigger, JobExecutionContext context) {
        String triggerName = trigger.getKey().getName();
        String jobName = context.getJobDetail().getKey().getName();
        log.info("Trigger Listener: {} is about to veto job execution for job {}.", triggerName, jobName);
        // Return true to veto, false to allow execution
        return false;
    }

    @Override
    public void triggerMisfired(Trigger trigger) {
        String triggerName = trigger.getKey().getName();
        log.warn("Trigger Listener: {} misfired.", triggerName);
    }

    @Override
    public void triggerComplete(Trigger trigger, JobExecutionContext context, Trigger.CompletedExecutionInstruction triggerInstructionCode) {
        String triggerName = trigger.getKey().getName();
        String jobName = context.getJobDetail().getKey().getName();
        log.info("Trigger Listener: {} completed for job {}. Instruction code: {}", triggerName, jobName, triggerInstructionCode);
    }
}