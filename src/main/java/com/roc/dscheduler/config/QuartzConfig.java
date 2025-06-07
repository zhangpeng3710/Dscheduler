package com.roc.dscheduler.config;

import com.roc.dscheduler.listener.GlobalJobListener;
import com.roc.dscheduler.listener.GlobalTriggerListener;
import org.quartz.spi.JobFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.quartz.QuartzProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

import javax.sql.DataSource;
import java.util.Properties;

@Configuration
public class QuartzConfig {

    @Autowired
    private DataSource dataSource; // Spring Boot auto-configured DataSource

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private QuartzProperties quartzProperties; // Spring Boot's parsed quartz properties from application.yml

    @Autowired
    private GlobalJobListener globalJobListener; // Spring-managed listener

    @Autowired
    private GlobalTriggerListener globalTriggerListener; // Spring-managed listener

    /**
     * Custom JobFactory to enable Spring dependency injection in Quartz Jobs.
     */
    @Bean
    public JobFactory jobFactory() {
        AutowiringSpringBeanJobFactory jobFactory = new AutowiringSpringBeanJobFactory();
        jobFactory.setApplicationContext(applicationContext);
        return jobFactory;
    }

    /**
     * Configure the Quartz Scheduler.
     * It uses the Spring-managed DataSource, our custom JobFactory,
     * and properties defined in application.yml.
     * It also registers the Spring-managed listeners.
     */
    @Bean
    public SchedulerFactoryBean schedulerFactoryBean(JobFactory jobFactory) {
        SchedulerFactoryBean factory = new SchedulerFactoryBean();

        // Set the custom JobFactory
        factory.setJobFactory(jobFactory);

        // Set the DataSource for JDBC JobStore
        factory.setDataSource(dataSource);

        // Set Quartz properties from application.yml
        // Spring Boot's QuartzProperties provides a convenient way to pass these.
        Properties props = new Properties();
        props.putAll(quartzProperties.getProperties());
        factory.setQuartzProperties(props);

        // Register Spring-managed listeners
        factory.setGlobalJobListeners(globalJobListener);
        factory.setGlobalTriggerListeners(globalTriggerListener);
        // If you have more specific (non-global) listeners, you can register them too:
        // factory.setJobListeners(jobListener1, jobListener2);
        // factory.setTriggerListeners(triggerListener1, triggerListener2);


        // Other important settings
        factory.setOverwriteExistingJobs(quartzProperties.isOverwriteExistingJobs());
        factory.setStartupDelay((int) quartzProperties.getStartupDelay().getSeconds());
        factory.setAutoStartup(quartzProperties.isAutoStartup());
        factory.setWaitForJobsToCompleteOnShutdown(quartzProperties.isWaitForJobsToCompleteOnShutdown());

        return factory;
    }
}