package no.fint.events;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import no.fint.events.annotations.FintEventListener;
import no.fint.events.config.FintEventsProps;
import no.fint.events.config.FintEventsScheduling;
import no.fint.events.listener.Listener;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;

@Slf4j
public class FintEvents implements ApplicationContextAware {
    private RedissonClient client;
    private ApplicationContext applicationContext;

    @Autowired
    private FintEventsScheduling scheduling;

    @Autowired
    private FintEventsProps props;

    @Getter
    private Map<String, Long> listeners = new HashMap<>();

    @Getter
    private Set<String> queues = new HashSet<>();

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @PostConstruct
    public void init() {
        Config config = props.getRedissonConfig();
        client = Redisson.create(config);
    }

    @PreDestroy
    public void shutdown() {
        client.shutdown();
    }

    public RedissonClient getClient() {
        return client;
    }

    public <V> BlockingQueue<V> getQueue(String queue) {
        queues.add(queue);
        return client.getBlockingQueue(queue);
    }

    public <V> BlockingQueue<V> getDownstream(String orgId) {
        String downstream = props.getDefaultDownstreamQueue();
        return getQueue(String.format(downstream, orgId));
    }

    public <V> BlockingQueue<V> getUpstream(String orgId) {
        String upstream = props.getDefaultUpstreamQueue();
        return getQueue(String.format(upstream, orgId));
    }

    public void send(String queue, Object value) {
        getQueue(queue).offer(value);
    }

    public void sendDownstream(String orgId, Object value) {
        getDownstream(orgId).offer(value);
    }

    public void sendUpstream(String orgId, Object value) {
        getUpstream(orgId).offer(value);
    }

    public void registerDownstreamListener(Class<?> listener, String... orgIds) {
        String downstream = props.getDefaultDownstreamQueue();
        for (String orgId : orgIds) {
            log.info("Registering downstream listener ({}) for {}", listener.getSimpleName(), orgId);
            registerListener(String.format(downstream, orgId), listener);
        }
    }

    public void registerUpstreamListener(Class<?> listener, String... orgIds) {
        String upstream = props.getDefaultUpstreamQueue();
        for (String orgId : orgIds) {
            log.info("Registering upstream listener ({}) for {}", listener.getSimpleName(), orgId);
            registerListener(String.format(upstream, orgId), listener);
        }
    }

    public void registerListener(String queue, Class<?> listener) {
        Object bean = applicationContext.getBean(listener);
        Method[] methods = bean.getClass().getMethods();
        for (Method method : methods) {
            FintEventListener annotation = method.getAnnotation(FintEventListener.class);
            if (annotation != null) {
                Listener listenerInstance = new Listener(bean, method, getQueue(queue));
                scheduling.register(listenerInstance);
                listeners.put(queue, System.currentTimeMillis());
            }
        }
    }
}
