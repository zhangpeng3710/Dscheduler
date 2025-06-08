package com.roc.dscheduler.controller;

import com.roc.dscheduler.dto.JobDTO;
import com.roc.dscheduler.service.JobService;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;
import java.util.Comparator;
import java.util.List;

@Controller
@RequestMapping("/jobs")
public class JobController {

    private static final Logger log = LoggerFactory.getLogger(JobController.class);

    @Autowired
    private JobService jobService;

    @GetMapping
    public String listJobs(
            @RequestParam(defaultValue = "jobName") String sort,
            @RequestParam(defaultValue = "asc") String order,
            Model model) {
        try {
            // Get all jobs
            List<JobDTO> jobs = jobService.getAllJobs();

            // Sort the jobs based on sort and order parameters
            Comparator<JobDTO> comparator;
            switch (sort) {
                case "jobGroup":
                    comparator = Comparator.comparing(JobDTO::getJobGroup);
                    break;
                case "triggerState":
                    comparator = Comparator.comparing(JobDTO::getTriggerState);
                    break;
                default:
                    comparator = Comparator.comparing(JobDTO::getJobName);
                    sort = "jobName"; // Ensure sort is set to a valid value
            }

            if ("desc".equalsIgnoreCase(order)) {
                comparator = comparator.reversed();
            }

            jobs.sort(comparator);

            model.addAttribute("jobs", jobs);
            model.addAttribute("sortField", sort);
            model.addAttribute("sortOrder", order);
            
        } catch (SchedulerException e) {
            log.error("Error fetching job list: {}", e.getMessage(), e);
            model.addAttribute("error", "Could not retrieve job list: " + e.getMessage());
        }
        return "jobs/list";
    }

    @GetMapping("/new")
    public String showCreateJobForm(Model model) {
        model.addAttribute("jobDTO", new JobDTO());
        return "jobs/form"; // Thymeleaf template: src/main/resources/templates/jobs/form.html
    }

    @PostMapping("/save")
    public String saveJob(@Valid @ModelAttribute("jobDTO") JobDTO jobDTO,
                          BindingResult bindingResult,
                          RedirectAttributes redirectAttributes,
                          Model model) {
        if (bindingResult.hasErrors()) {
            // If using @Valid, ensure your JobDTO has validation annotations (e.g., @NotEmpty, @Pattern for cron)
            // For simplicity, basic error handling is shown here.
            model.addAttribute("jobDTO", jobDTO); // Keep user input
            return "jobs/form";
        }
        try {
            jobService.scheduleJob(jobDTO);
            redirectAttributes.addFlashAttribute("successMessage", "Job '" + jobDTO.getJobName() + "' scheduled successfully!");
        } catch (ClassNotFoundException e) {
            log.error("Error scheduling job {}: Job class not found - {}", jobDTO.getJobName(), jobDTO.getJobClass(), e);
            model.addAttribute("jobDTO", jobDTO);
            model.addAttribute("errorMessage", "Job class '" + jobDTO.getJobClass() + "' not found.");
            return "jobs/form";
        } catch (SchedulerException e) {
            log.error("Error scheduling job {}: {}", jobDTO.getJobName(), e.getMessage(), e);
            model.addAttribute("jobDTO", jobDTO);
            model.addAttribute("errorMessage", "Could not schedule job: " + e.getMessage());
            return "jobs/form";
        } catch (Exception e) {
            log.error("Unexpected error scheduling job {}: {}", jobDTO.getJobName(), e.getMessage(), e);
            model.addAttribute("jobDTO", jobDTO);
            model.addAttribute("errorMessage", "An unexpected error occurred: " + e.getMessage());
            return "jobs/form";
        }
        return "redirect:/jobs";
    }

    @PostMapping("/pause")
    public String pauseJob(@RequestParam String jobName, @RequestParam String jobGroup, RedirectAttributes redirectAttributes) {
        try {
            jobService.pauseJob(jobName, jobGroup);
            redirectAttributes.addFlashAttribute("successMessage", "Job '" + jobName + "' paused successfully.");
        } catch (SchedulerException e) {
            log.error("Error pausing job {} in group {}: {}", jobName, jobGroup, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Could not pause job: " + e.getMessage());
        }
        return "redirect:/jobs";
    }

    @PostMapping("/resume")
    public String resumeJob(@RequestParam String jobName, @RequestParam String jobGroup, RedirectAttributes redirectAttributes) {
        try {
            jobService.resumeJob(jobName, jobGroup);
            redirectAttributes.addFlashAttribute("successMessage", "Job '" + jobName + "' resumed successfully.");
        } catch (SchedulerException e) {
            log.error("Error resuming job {} in group {}: {}", jobName, jobGroup, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Could not resume job: " + e.getMessage());
        }
        return "redirect:/jobs";
    }

    @PostMapping("/delete")
    public String deleteJob(@RequestParam String jobName, @RequestParam String jobGroup, RedirectAttributes redirectAttributes) {
        try {
            jobService.deleteJob(jobName, jobGroup);
            redirectAttributes.addFlashAttribute("successMessage", "Job '" + jobName + "' deleted successfully.");
        } catch (SchedulerException e) {
            log.error("Error deleting job {} in group {}: {}", jobName, jobGroup, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Could not delete job: " + e.getMessage());
        }
        return "redirect:/jobs";
    }
}