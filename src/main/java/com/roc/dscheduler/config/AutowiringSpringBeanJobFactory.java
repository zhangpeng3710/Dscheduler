package com.roc.dscheduler.config; // Or your current package for this file

import org.quartz.spi.TriggerFiredBundle;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.scheduling.quartz.SpringBeanJobFactory;

/**
 * A Spring Bean Job Factory that enables auto-wiring of Spring beans into Quartz Jobs.
 * This allows Quartz jobs to use Spring's dependency injection.
 */
// No @Component needed here as it's instantiated directly in QuartzConfig
public class AutowiringSpringBeanJobFactory extends SpringBeanJobFactory implements ApplicationContextAware {

    private transient AutowireCapableBeanFactory beanFactory;

    @Override
    public void setApplicationContext(final ApplicationContext context) {
        beanFactory = context.getAutowireCapableBeanFactory();
    }

    /**
     * Creates the job instance, and then autowires dependencies into it.
     */
    @Override
    protected Object createJobInstance(final TriggerFiredBundle bundle) throws Exception {
        final Object job = super.createJobInstance(bundle);
        beanFactory.autowireBean(job); // Autowire the job instance
        return job;
    }
}