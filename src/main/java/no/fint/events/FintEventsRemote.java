package no.fint.events;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RRemoteService;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

@Slf4j
public class FintEventsRemote implements ApplicationContextAware {
    @Autowired
    private FintEvents fintEvents;
    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public void registerServer(Class<? extends FintRemoteEvent> type) {
        FintRemoteEvent bean = applicationContext.getBean(type);
        RRemoteService remoteService = fintEvents.getClient().getRemoteService();
        remoteService.register(FintRemoteEvent.class, bean);
    }

    @SuppressWarnings("unchecked")
    public <V> FintRemoteEvent<V> registerClient() {
        RRemoteService remoteService = fintEvents.getClient().getRemoteService();
        return remoteService.get(FintRemoteEvent.class);
    }

}
