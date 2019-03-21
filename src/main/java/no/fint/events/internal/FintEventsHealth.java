package no.fint.events.internal;

import com.hazelcast.core.ItemEvent;
import com.hazelcast.core.ItemListener;
import lombok.extern.slf4j.Slf4j;
import no.fint.event.model.Event;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.SynchronousQueue;

@Slf4j
@Component
public class FintEventsHealth extends FintEventsAbstract {


    @Override
    public void itemAdded(ItemEvent<Event> item) {
        Event event = item.getItem();
        if (event.isHealthCheck()) {
            offer(event);
        }
    }

    @Override
    public void itemRemoved(ItemEvent<Event> item) {
    }

}
