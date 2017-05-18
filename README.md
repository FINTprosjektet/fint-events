# FINT Events

[![Build Status](https://travis-ci.org/FINTlibs/fint-events.svg?branch=master)](https://travis-ci.org/FINTlibs/fint-events)
[![Coverage Status](https://coveralls.io/repos/github/FINTlibs/fint-events/badge.svg?branch=master)](https://coveralls.io/github/FINTlibs/fint-events?branch=master)

Event library built on top of [redisson](https://redisson.org/).

* [Installation](#installation)
* [Usage](#usage)
  * [Publish message on queue](#publish-message-on-queue)
  * [Register listener](#register-listener)
  * [Queue name configuration](#queue-name-configuration)
  * [Temporary queues](#temporary-queues)
  * [Health check](#health-check)
  * [Fint Events endpoints](#fint-events-endpoints)
  * [Reconnect](#reconnect)
  * [Configuration](#configuration)

---

# Installation

```groovy
repositories {
    maven {
        url  "http://dl.bintray.com/fint/maven" 
    }
}

compile('no.fint:fint-events:0.1.17')
```

# Usage

Add `@EnableFintEvents` to the main class

```java
@EnableFintEvents
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

## Publish message on queue

Custom queue name:
```java
@Autowired
private FintEvents fintEvents;

fintEvents.send("queue-name", messageObj);
```

Downstream/upstream queue:
```java
fintEvents.sendDownstream("orgId", messageObj);
fintEvents.sendUpstream("orgId", messageObj);
```

## Register listener

Create listener bean. This needs to be a bean registered in the Spring container.  
The method that will receive the message is annotated with `@FintEventListener`:
```java
@Component
public class TestListener {

    @FintEventListener
    public void receive(TestDto testDto) {
        ...
    }
}
```

Custom queue name:
```java
fintEvents.registerListener("queue-name", MyListener);
```

Downstream/upstream queue:
```java
fintEvents.registerDownstreamListener(MyListener.class, "orgId");
fintEvents.registerUpstreamListener(MyListener.class, "orgId");
```

Get registered listeners (Queue name + time registered):
```java
Map<String, Long> listeners = fintEvents.getListeners();
```

If orgId(s) are added to `fint.events.orgIds`, event listeners can be automatically registered.  
See [configuration](#configuration) for more details.

## Queue name configuration

If you need more control to customize the queue name than with the properties (`fint.events.env`/ `fint.events.component`)
it is possible to use the `QueueName` object.  

```java
QueueName.with(orgId);
QueueName.with(component, orgId);
QueueName.with(env, component, orgId);    
```

The object can be sent into methods that uses a queue, for example:
  
```java
fintEvents.getDownstream(queueName);
fintEvents.sendUpstream(queueName, value);
fintEvents.registerUpstreamListener(MyListener.class, queueName);
```
If the value is set in the QueueName object, it will be used instead of the configured properties.  
If a value is null in QueueName the configured values are used.

## Temporary queues

A temporary queue is a short-lived queue that will not be registered in the queue names list.  
It will also have a standard prefix (`temp-`) making it easy to find all temporary queues in redis.  

```java
fintEvents.getTempQueue("my-queue");
```

In this example the queue name in redis will be `temp-my-queue`.


## Health check

**Client:**
```java
@Autowired
private FintEventsHealth fintEventsHealth;

Health<TestDto> healthClient = fintEventsHealth.registerClient();
Health<TestDto> response = client.healthCheck(new TestDto());
```

The health check client can be deregistered, which can be useful if there is a problem with the redis instance:
```java
fintEventsHealth.deregisterClient();
```

**Server:**  

```java
@Component
public class TestHealth implements HealthCheck<TestDto> {
    @Override
    public TestDto check(TestDto value) {
        ...
        return value;
    }
}

```

Register listener in redisson:
```java
@Autowired
private FintEventsHealth fintEventsHealth;

fintEventsHealth.registerServer(TestHealth);
```

## Remote Service

We recommend publishing messages instead of using the remote service feature. This is a blocking call, where the client will wait for a response or a timeout happens.


**Client:**
```java
@Autowired
private FintEventsRemote fintEventsRemote;

RemoteEvent<TestDto> remoteEvent = fintEventsRemote.registerClient();
```

**Server:**
```java
@Component
public class TestListener {

    @FintEventListener
    public void receive(TestDto testDto) {
        ...
    }
}
```

Register listener in redisson:
```java
@Autowired
private FintEventsRemote fintEventsRemote;

fintEventsRemote.registerServer(TestListener);
```

### Run RemoteService integration tests

Add the system property: `remoteServiceTestsEnabled=true`

## Fint Events endpoints

Makes it possible to query the content of the queues.  
Enabled with the property `fint.events.queue-endpoint-enabled`.  

If use with [springfox-loader](https://github.com/jarlehansen/springfox-loader), add the `FintEventsController`:
```java
@EnableSpringfox(includeControllers = FintEventsController.class)
```

**GET all queue names**

`GET /fint-events/queues`

* *componentQueues*, the queues registered for the specific instance of the application
* *queues*, all queues created with fint-events that are sharing the same instances of redis

Response:
```json
{
  "componentQueues": [
    "mock.no.upstream",
    "mock.no.downstream"
  ],
  "queues": [
    "mock.no.upstream",
    "mock.no.downstream"]
}
```

**GET content of queue**

`GET /fint-events/queues/{queue}`

This will use a `peek()` method on the actual queue, meaning it will not be removed.  
The response contains size of the queue and the next value. The length of the shown next value in the queue will be max 300 characters.
```json
{
  "size": "3",
  "value": "Event{corrId='43ab45e1-ed06-404d-a093-3f92cf37fc3d', ...}"
}
```

Get the value in the queue on the specified index.

`GET /fint-events/queues/{queue}?index=0`


## Reconnect

When there is a need to reconnect the redisson client (when the default reconnection strategy from redisson does not work for some reason):
```java
fintEvents.reconnect();
```
This will shutdown the redisson client and recreate it.  

In these situations it is also important to remember to deregister and register listeners:
```java
fintEvents.reconnect();
fintEventsHealth.deregisterClient();
fintEvents.registerUpstreamListener(MyListener.class, "orgId");
fintEventsHealth.registerClient();
```

## Configuration

Redisson configuration is added in a file `redisson.yml` on classpath (`src/main/resources`).  
It also supports to separate config-files for the Spring profile used, for example `redisson-test.yml` when using the test profile.
If no config-file is found the default values are used: `Single server, 127.0.0.1:6379`  
If test-model is enabled, the default config will always be used.

* **[Redisson configuration](https://github.com/redisson/redisson/wiki/2.-Configuration)**
* [Single instance mode](https://github.com/redisson/redisson/wiki/2.-Configuration#26-single-instance-mode)
* [Cluster mode](https://github.com/redisson/redisson/wiki/2.-Configuration#24-cluster-mode)

| Key | Description | Default value |
|-----|-------------|---------------|
| fint.events.orgIds | The organisations that are included when generating the event listeners. The default listeners are only created if there is one event listener registered, and the listener has specified queue type `@FintEventListener(type = QueueType.DOWNSTREAM)`. Value can be a comma separated list of orgIds. | Empty array |
| fint.events.env | The environment that the system is running in, for example test / prod. Used to build the downstream/upstream queue name. | local |
| fint.events.component | The component name. Used to build the downstream/upstream queue name. | default |
| fint.events.default-downstream-queue | The format of the default downstream queue. {component}=`fint.events.component` {env}=`fint-events.env` | `downstream_{component}_{env}_{orgId}` |
| fint.events.default-upstream-queue | The format of the default upstream queue. {component}=`fint.events.component` {env}=`fint-events.env` | `upstream_{component}_{env}_{orgId}` |
| fint.events.test-mode | When test mode is enable, an embedded redis instance is initialized on startup. It will also use the default redisson config `single server, 127.0.0.1:6379`. | false |
| fint.events.queue-endpoint-enabled | Enable the rest endpoints `/fint-events/*` that make it possible to query the content of the queues. If the endpoint is disable a 404 response code is returned. | false |
| fint.events.task-scheduler-thread-pool-size | The number of threads in the task scheduler thread pool. This will be used by all event listeners and `@Scheduled` methods. | 50 |
| fint.events.healthcheck.timeout-in-seconds | The number of seconds the health check client will wait before timing out. | 120 |