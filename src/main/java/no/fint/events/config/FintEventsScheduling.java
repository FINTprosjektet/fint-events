package no.fint.events.config;

import no.fint.events.listener.Listener;
import org.springframework.scheduling.config.IntervalTask;
import org.springframework.scheduling.config.ScheduledTask;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class FintEventsScheduling {
    private ScheduledTaskRegistrar registrar;

    private List<Listener> listeners = new ArrayList<>();
    private List<ScheduledTask> scheduledTasks = new ArrayList<>();

    public void setRegistrar(ScheduledTaskRegistrar registrar) {
        this.registrar = registrar;
        listeners.forEach(this::register);
        listeners.clear();
    }

    public void register(Listener listener) {
        if (registrar == null) {
            listeners.add(listener);
        } else {
            IntervalTask intervalTask = new IntervalTask(listener, 10);
            ScheduledTask scheduledTask = registrar.scheduleFixedDelayTask(intervalTask);
            scheduledTasks.add(scheduledTask);
        }
    }

    public void removeAllListeners() {
        scheduledTasks.forEach(ScheduledTask::cancel);
        scheduledTasks.clear();
    }

}
