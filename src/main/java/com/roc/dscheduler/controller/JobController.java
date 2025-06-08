package com.roc.dscheduler.controller;

import com.roc.dscheduler.entity.JobInfo;
import com.roc.dscheduler.entity.Page;
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
import java.util.stream.Collectors;

@Controller
@RequestMapping("/jobs")
public class JobController {

    private static final Logger log = LoggerFactory.getLogger(JobController.class);

    private final JobService jobService;

    @Autowired
    public JobController(JobService jobService) {
        this.jobService = jobService;
    }


    @GetMapping
    public String listJobs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "jobName") String sort,
            @RequestParam(defaultValue = "asc") String order,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String searchType,
            Model model) {
        try {
            // Get all jobs first (for filtering)
            List<JobInfo> allJobs = jobService.getAllJobs();

            // Apply search filter if search term is provided
            if (search != null && !search.trim().isEmpty() && searchType != null) {
                String searchTerm = search.toLowerCase().trim();
                allJobs = allJobs.stream().filter(job -> {
                    switch (searchType) {
                        case "name":
                            return job.getJobName().toLowerCase().contains(searchTerm);
                        case "group":
                            return job.getJobGroup().toLowerCase().contains(searchTerm);
                        case "cron":
                            return job.getCronExpression().toLowerCase().contains(searchTerm);
                        case "status":
                            return job.getTriggerState().toLowerCase().contains(searchTerm);
                        default:
                            return true;
                    }
                }).collect(Collectors.toList());
            }

            // Sort the jobs
            Comparator<JobInfo> comparator;
            switch (sort) {
                case "jobGroup":
                    comparator = Comparator.comparing(JobInfo::getJobGroup);
                    break;
                case "triggerState":
                    comparator = Comparator.comparing(JobInfo::getTriggerState);
                    break;
                default:
                    comparator = Comparator.comparing(JobInfo::getJobName);
                    break;
            }

            if ("desc".equalsIgnoreCase(order)) {
                comparator = comparator.reversed();
            }

            allJobs.sort(comparator);

            // Apply pagination
            int totalItems = allJobs.size();
            int totalPages = (int) Math.ceil((double) totalItems / size);
            page = Math.max(1, Math.min(page, totalPages));

            int fromIndex = (page - 1) * size;
            int toIndex = Math.min(fromIndex + size, totalItems);
            List<JobInfo> pageJobs = allJobs.subList(fromIndex, toIndex);

            // Create page object
            Page<JobInfo> jobPage = new Page<>();
            jobPage.setContent(pageJobs);
            jobPage.setCurrentPage(page);
            jobPage.setPageSize(size);
            jobPage.setTotalItems(totalItems);
            jobPage.setTotalPages(totalPages == 0 ? 1 : totalPages);

            model.addAttribute("page", jobPage);
            model.addAttribute("sortField", sort);
            model.addAttribute("sortOrder", order);
            model.addAttribute("search", search);
            model.addAttribute("searchType", searchType != null ? searchType : "name");
            List<Integer> attributeValue = new java.util.ArrayList<>();
            attributeValue.add(10);
            attributeValue.add(20);
            attributeValue.add(50);
            model.addAttribute("pageSizes", attributeValue);

        } catch (SchedulerException e) {
            log.error("Error fetching job list: {}", e.getMessage(), e);
            model.addAttribute("error", "Could not retrieve job list: " + e.getMessage());
        }
        return "jobs/list";
    }

    @GetMapping("/new")
    public String showCreateJobForm(Model model) {
        model.addAttribute("jobInfo", new JobInfo());
        return "jobs/form"; // Thymeleaf template: src/main/resources/templates/jobs/form.html
    }

    @PostMapping("/save")
    public String saveJob(@Valid @ModelAttribute("jobInfo") JobInfo jobInfo,
                          BindingResult bindingResult,
                          RedirectAttributes redirectAttributes,
                          Model model) {
        if (bindingResult.hasErrors()) {
            // If using @Valid, ensure your jobInfo has validation annotations (e.g., @NotEmpty, @Pattern for cron)
            // For simplicity, basic error handling is shown here.
            model.addAttribute("jobInfo", jobInfo); // Keep user input
            return "jobs/form";
        }
        try {
            jobService.scheduleJob(jobInfo);
            redirectAttributes.addFlashAttribute("successMessage", "Job '" + jobInfo.getJobName() + "' scheduled successfully!");
        } catch (ClassNotFoundException e) {
            log.error("Error scheduling job {}: Job class not found - {}", jobInfo.getJobName(), jobInfo.getJobClass(), e);
            model.addAttribute("jobInfo", jobInfo);
            model.addAttribute("errorMessage", "Job class '" + jobInfo.getJobClass() + "' not found.");
            return "jobs/form";
        } catch (SchedulerException e) {
            log.error("Error scheduling job {}: {}", jobInfo.getJobName(), e.getMessage(), e);
            model.addAttribute("jobInfo", jobInfo);
            model.addAttribute("errorMessage", "Could not schedule job: " + e.getMessage());
            return "jobs/form";
        } catch (Exception e) {
            log.error("Unexpected error scheduling job {}: {}", jobInfo.getJobName(), e.getMessage(), e);
            model.addAttribute("jobInfo", jobInfo);
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