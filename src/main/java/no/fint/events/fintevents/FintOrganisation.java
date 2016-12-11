package no.fint.events.fintevents;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;

public class FintOrganisation {
    private final String name;

    private String exchange;
    private String downstreamQueue;
    private String upstreamQueue;
    private String undeliveredQueue;

    public FintOrganisation(String name, String defaultDownstreamQueue, String defaultUpstreamQueue, String defaultUndeliveredQueue) {
        this.name = name;
        this.exchange = name;
        this.downstreamQueue = String.format(defaultDownstreamQueue, name);
        this.upstreamQueue = String.format(defaultUpstreamQueue, name);
        this.undeliveredQueue = String.format(defaultUndeliveredQueue, name);
    }

    public String getName() {
        return name;
    }

    public String getExchangeName() {
        return exchange;
    }

    public TopicExchange getExchange() {
        return new TopicExchange(exchange);
    }

    public String getDownstreamQueueName() {
        return downstreamQueue;
    }

    public Queue getDownstreamQueue() {
        return new Queue(downstreamQueue);
    }

    public String getUpstreamQueueName() {
        return upstreamQueue;
    }

    public Queue getUpstreamQueue() {
        return new Queue(upstreamQueue);
    }

    public String getUndeliveredQueueName() {
        return undeliveredQueue;
    }

    public Queue getUndeliveredQueue() {
        return new Queue(undeliveredQueue);
    }

    public Queue getQueue(EventType type) {
        if (type == EventType.DOWNSTREAM) {
            return getDownstreamQueue();
        } else if (type == EventType.UPSTREAM) {
            return getUpstreamQueue();
        } else {
            return getUndeliveredQueue();
        }
    }
}