package io.github.rutwoklabs.jbuildpilot.tasks;

import java.util.ArrayList;
import java.util.List;

public class MaintenancePlan {
    private final String repository;
    private final List<MaintenanceTask> tasks;

    public MaintenancePlan(String repository) {
        this.repository = repository;
        this.tasks = new ArrayList<>();
    }

    public String getRepository() {
        return repository;
    }

    public void addTask(MaintenanceTask task) {
        this.tasks.add(task);
    }

    public List<MaintenanceTask> getTasks() {
        return tasks;
    }
    
    public List<MaintenanceTask> getApprovedTasks() {
        List<MaintenanceTask> approved = new ArrayList<>();
        for (MaintenanceTask task : tasks) {
            if (task.isApproved() || !task.isRequiresApproval()) {
                approved.add(task);
            }
        }
        return approved;
    }
}
