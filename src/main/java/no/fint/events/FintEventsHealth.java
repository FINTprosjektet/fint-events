package no.fint.events;

import lombok.extern.slf4j.Slf4j;
import no.fint.events.config.FintEventsProps;
import org.redisson.api.RRemoteService;
import org.redisson.api.RemoteInvocationOptions;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
public class FintEventsHealth implements ApplicationContextAware {
    @Autowired
    private FintEvents fintEvents;

    @Autowired
    private FintEventsProps props;

    private ApplicationContext applicationContext;
    private RemoteInvocationOptions options;

    private AtomicBoolean clientRegistered = new AtomicBoolean(false);

    @PostConstruct
    public void init() {
        options = RemoteInvocationOptions.defaults().expectAckWithin(10, TimeUnit.SECONDS).expectResultWithin(props.getHealthCheckTimeout(), TimeUnit.SECONDS);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public void registerServer(Class<? extends HealthCheck> type) {
        HealthCheck bean = applicationContext.getBean(type);
        RRemoteService remoteService = fintEvents.getClient().getRemoteService();
        remoteService.register(HealthCheck.class, bean);
    }

    @SuppressWarnings("unchecked")
    public <V> HealthCheck<V> registerClient() {
        clientRegistered.set(true);
        RRemoteService remoteService = fintEvents.getClient().getRemoteService();
        return remoteService.get(HealthCheck.class, options);
    }

    public void deregisterClient() {
        if (clientRegistered.get()) {
            log.info("Removing registered HealthCheck clients");
            fintEvents.getClient().getRemoteService().deregister(HealthCheck.class);
            clientRegistered.set(false);
        }
    }
}
