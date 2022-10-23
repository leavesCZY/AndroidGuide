> 公众号：[字节数组](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/adbc507fc3704fd8955aae739a433db2~tplv-k3u1fbpfcp-zoom-1.image)
>
> 希望对你有所帮助 🤣🤣

> 对于 Android Developer 来说，很多开源库都是属于**开发必备**的知识点，从使用方式到实现原理再到源码解析，这些都需要我们有一定程度的了解和运用能力。所以我打算来写一系列关于开源库**源码解析**和**实战演练**的文章，初定的目标是 **EventBus、ARouter、LeakCanary、Retrofit、Glide、OkHttp、Coil** 等七个知名开源库，希望对你有所帮助 🤣🤣

我们知道，EventBus 在发送了消息后，就会直接回调该消息类型的所有监听方法，回调操作是通过反射 `method.invoke` 来实现的，那么在回调之前也必须先拿到应用内所有的监听方法才行。EventBus 获取监听方法的方式有两种：

- 不配置注解处理器。在 subscriber 进行 register 时通过反射获取到 subscriber 的所有监听方法，这种方式是在运行时实现的
- 配置注解处理器。预先将所有的监听方法的方法签名信息保存到辅助文件中，在运行时就可以直接拿到所有的解析结果而不必依靠反射来获取，这种方式是在编译阶段实现的，相比第一种方式性能上会高很多

这里先介绍第一种方式，这种方式只需要导入如下依赖即可

```groovy
implementation "org.greenrobot:eventbus:3.2.0"
```

# 一、注册

EventBus 通过 `EventBus.register(Object)`方法来进行注册的。该方法会对 subscriber 进行解析，通过 SubscriberMethodFinder 的 `findSubscriberMethods` 方法将 subscriber 包含的所有声明了`@Subscribe`注解的方法的签名信息保存到内存中，当有消息被 Post 时就可以直接在内存中查找到目标方法了

```java
public void register(Object subscriber) {
    Class<?> subscriberClass = subscriber.getClass();
    List<SubscriberMethod> subscriberMethods = subscriberMethodFinder.findSubscriberMethods(subscriberClass);
    synchronized (this) {
        for (SubscriberMethod subscriberMethod : subscriberMethods) {
            subscribe(subscriber, subscriberMethod);
        }
    }
}
```

从 SubscriberMethod 包含的所有参数可以看出来，它包含了 `@Subscribe` 的参数信息以及对应的方法签名信息

```java
public class SubscriberMethod {
    final Method method;
    final ThreadMode threadMode;
    //消息类型
    final Class<?> eventType;
    //消息优先级
    final int priority;
    //是否属于黏性消息
    final boolean sticky;
    /** Used for efficient comparison */
    String methodString;
	
    ···

}
```

SubscriberMethodFinder 会将每次的查找结果缓存到 `METHOD_CACHE`中，这对某些会先后经历**多次注册和反注册**操作的 subscriber 来说比较有用，因为每次查找可能需要依靠多次循环遍历和反射操作，会稍微有点消耗性能，但缓存也会占用一部分内存空间

```java
private static final Map<Class<?>, List<SubscriberMethod>> METHOD_CACHE = new ConcurrentHashMap<>();

List<SubscriberMethod> findSubscriberMethods(Class<?> subscriberClass) {
    List<SubscriberMethod> subscriberMethods = METHOD_CACHE.get(subscriberClass);
    if (subscriberMethods != null) {
        return subscriberMethods;
    }

    if (ignoreGeneratedIndex) {
        subscriberMethods = findUsingReflection(subscriberClass);
    } else {
        subscriberMethods = findUsingInfo(subscriberClass);
    }
    if (subscriberMethods.isEmpty()) {
        //如果为空，说明没找到使用 @Subscribe 方法，那么 register 操作就是没有意义的，直接抛出异常
        throw new EventBusException("Subscriber " + subscriberClass
                + " and its super classes have no public methods with the @Subscribe annotation");
    } else {
        METHOD_CACHE.put(subscriberClass, subscriberMethods);
        return subscriberMethods;
    }
}
```

因为`ignoreGeneratedIndex`默认是 false，所以这里直接看 `findUsingInfo(subscriberClass)` 方法

其主要逻辑是：

1. 通过 `prepareFindState()` 方法从对象池 `FIND_STATE_POOL` 中获取空闲的 FindState 对象，如果不存在则初始化一个新的，并在使用过后通过 `getMethodsAndRelease` 方法将对象还给对象池。通过对象池来避免无限制地创建 `FindState` 对象，这也算做是一个优化点。FindState 用于在反射遍历过程中保存各种中间状态值
2. 在不使用注解处理器的情况下 `findState.subscriberInfo` 和 `subscriberInfoIndexes`默认都是等于 null，所以主要看 `findUsingReflectionInSingleClass` 方法即可，从该方法名可知是通过反射操作来进行解析的，解析结果会被存到 `findState`中
3. 因为父类声明的监听方法会被子类所继承，而解析过程是会从子类向其父类依次遍历的，所以在解析完子类后需要通过 `findState.moveToSuperclass()` 方法将下一个查找的 class 对象指向父类

```java
private static final int POOL_SIZE = 4;

private static final FindState[] FIND_STATE_POOL = new FindState[POOL_SIZE];

private List<SubscriberMethod> findUsingInfo(Class<?> subscriberClass) {
    //步骤1
    FindState findState = prepareFindState();
    findState.initForSubscriber(subscriberClass);
    while (findState.clazz != null) {
        findState.subscriberInfo = getSubscriberInfo(findState);
        if (findState.subscriberInfo != null) {
            SubscriberMethod[] array = findState.subscriberInfo.getSubscriberMethods();
            for (SubscriberMethod subscriberMethod : array) {
                if (findState.checkAdd(subscriberMethod.method, subscriberMethod.eventType)) {
                    findState.subscriberMethods.add(subscriberMethod);
                }
            }
        } else {
            //步骤2
            findUsingReflectionInSingleClass(findState);
        }
        //步骤3
        findState.moveToSuperclass();
    }
    return getMethodsAndRelease(findState);
}

private List<SubscriberMethod> getMethodsAndRelease(FindState findState) {
    List<SubscriberMethod> subscriberMethods = new ArrayList<>(findState.subscriberMethods);
    findState.recycle();
    synchronized (FIND_STATE_POOL) {
        //回收 findState，尝试将之存到对象池中
        for (int i = 0; i < POOL_SIZE; i++) {
            if (FIND_STATE_POOL[i] == null) {
                FIND_STATE_POOL[i] = findState;
                break;
            }
        }
    }
    return subscriberMethods;
}

//如果对象池中有可用的对象则取出来使用，否则的话就构建一个新的
private FindState prepareFindState() {
    synchronized (FIND_STATE_POOL) {
        for (int i = 0; i < POOL_SIZE; i++) {
            FindState state = FIND_STATE_POOL[i];
            if (state != null) {
                FIND_STATE_POOL[i] = null;
                return state;
            }
        }
    }
    return new FindState();
}
```

这里来主要看下 `findUsingReflectionInSingleClass` 方法是如何完成反射操作的。如果解析到的方法签名不符合要求，则在开启了**严格检查**的情况下直接抛出异常；如果方法签名符合要求，则会将方法签名保存到`subscriberMethods`中

```java
private void findUsingReflectionInSingleClass(FindState findState) {
    Method[] methods;
    try {
        // This is faster than getMethods, especially when subscribers are fat classes like Activities
        //获取 clazz 包含的所有方法，不包含继承得来的方法
        methods = findState.clazz.getDeclaredMethods();
    } catch (Throwable th) {
        // Workaround for java.lang.NoClassDefFoundError, see https://github.com/greenrobot/EventBus/issues/149
        try {
            //获取 clazz 以及其父类的所有 public 方法
            methods = findState.clazz.getMethods();
        } catch (LinkageError error) { // super class of NoClassDefFoundError to be a bit more broad...
            String msg = "Could not inspect methods of " + findState.clazz.getName();
            if (ignoreGeneratedIndex) {
                msg += ". Please consider using EventBus annotation processor to avoid reflection.";
            } else {
                msg += ". Please make this class visible to EventBus annotation processor to avoid reflection.";
            }
            throw new EventBusException(msg, error);
        }
        //由于 getDeclaredMethods() 都抛出异常了，就不再继续向下循环了，所以指定下次循环时忽略父类
        findState.skipSuperClasses = true;
    }
    for (Method method : methods) {
        int modifiers = method.getModifiers();
        if ((modifiers & Modifier.PUBLIC) != 0 && (modifiers & MODIFIERS_IGNORE) == 0) {
            //method 是 public 的，且不是 ABSTRACT、STATIC、BRIDGE、SYNTHETIC

            Class<?>[] parameterTypes = method.getParameterTypes();
            if (parameterTypes.length == 1) {  //方法包含的参数个数是一
                Subscribe subscribeAnnotation = method.getAnnotation(Subscribe.class);
                if (subscribeAnnotation != null) { //方法签名包含 Subscribe 注解
                    Class<?> eventType = parameterTypes[0];
                    if (findState.checkAdd(method, eventType)) {
                        //校验通过后，就将 Subscribe 注解的配置信息及 method 方法签名保存起来
                        ThreadMode threadMode = subscribeAnnotation.threadMode();
                        findState.subscriberMethods.add(new SubscriberMethod(method, eventType, threadMode,
                                subscribeAnnotation.priority(), subscribeAnnotation.sticky()));
                    }
                }
            } else if (strictMethodVerification && method.isAnnotationPresent(Subscribe.class)) {
                //因为 EventBus 只支持包含一个入参参数的注解函数，所以如果开启了严格的方法校验那么就抛出异常
                String methodName = method.getDeclaringClass().getName() + "." + method.getName();
                throw new EventBusException("@Subscribe method " + methodName +
                        "must have exactly 1 parameter but has " + parameterTypes.length);
            }
        } else if (strictMethodVerification && method.isAnnotationPresent(Subscribe.class)) {
            //如果 method 的方法签名不符合要求且开启了严格的方法校验那么就抛出异常
            String methodName = method.getDeclaringClass().getName() + "." + method.getName();
            throw new EventBusException(methodName +
                    " is a illegal @Subscribe method: must be public, non-static, and non-abstract");
        }
    }
}
```

`findUsingReflectionInSingleClass`方法的一个重点是 `findState.checkAdd`方法。如果往简单了想，只要把 subscriber 每个声明了 Subscribe 注解的方法都给保存起来就可以了，可是还需要考虑一些特殊情况：

1. Java 中的类是具有继承关系的，如果父类声明了 Subscribe 方法，那么就相当于子类也持有了该监听方法，那么子类在 register 后就需要拿到父类的所有 Subscribe 方法
2. 如果子类继承并重写了父类的 Subscribe 方法，那么子类在 register 后就需要以自己重写后的方法为准，忽略父类的相应方法

`checkAdd` 方法就用于进行上述判断

```java
//以 eventType 作为 key，method 或者 FindState 作为 value
final Map<Class, Object> anyMethodByEventType = new HashMap<>();
//以 methodKey 作为 key，methodClass 作为 value
final Map<String, Class> subscriberClassByMethodKey = new HashMap<>();

boolean checkAdd(Method method, Class<?> eventType) {
    // 2 level check: 1st level with event type only (fast), 2nd level with complete signature when required.
    // Usually a subscriber doesn't have methods listening to the same event type.

    Object existing = anyMethodByEventType.put(eventType, method);
    if (existing == null) {
        //existing 等于 null 说明之前未解析到监听相同事件的方法，检查通过
        //因为大部分情况下监听者不会声明多个监听相同事件的方法，所以先进行这步检查效率上会比较高
        return true;
    } else { //existing 不等于 null 说明之前已经解析到同样监听这个事件的方法了

        if (existing instanceof Method) {
            if (!checkAddWithMethodSignature((Method) existing, eventType)) {
                // Paranoia check
                throw new IllegalStateException();
            }
            // Put any non-Method object to "consume" the existing Method
            //会执行到这里，说明存在多个方法监听同个 Event，那么将将 eventType 对应的 value 置为 this
            //避免多次检查，让其直接去执行 checkAddWithMethodSignature 方法
            anyMethodByEventType.put(eventType, this);
        }
        return checkAddWithMethodSignature(method, eventType);
    }
}

private boolean checkAddWithMethodSignature(Method method, Class<?> eventType) {
    methodKeyBuilder.setLength(0);
    methodKeyBuilder.append(method.getName());
    methodKeyBuilder.append('>').append(eventType.getName());

    //以 methodName>eventTypeName 字符串作为 key
    //通过这个 key 来判断是否存在子类重写了父类方法的情况
    String methodKey = methodKeyBuilder.toString();
    //获取声明了 method 的类对应的 class 对象
    Class<?> methodClass = method.getDeclaringClass();

    Class<?> methodClassOld = subscriberClassByMethodKey.put(methodKey, methodClass);
    //1. 如果 methodClassOld == null 为 true，说明 method 是第一次解析到，允许添加
    //2. 如果 methodClassOld.isAssignableFrom(methodClass) 为 true
    //2.1、说明 methodClassOld 是 methodClass 的父类，需要以子类重写的方法 method 为准，允许添加
    //     实际上应该不存在这种情况，因为 EventBus 是从子类开始向父类进行遍历的
    //2.2、说明 methodClassOld 是 methodClass 是同个类，即 methodClass 声明了多个方法对同个事件进行监听 ，也允许添加
    if (methodClassOld == null || methodClassOld.isAssignableFrom(methodClass)) {
        // Only add if not already found in a sub class
        return true;
    } else {
        // Revert the put, old class is further down the class hierarchy
        //由于 EventBus 是从子类向父类进行解析
        //会执行到这里就说明之前已经解析到了相同 key 的方法，对应子类重写了父类方法的情况
        //此时需要以子类重写的方法 method 为准，所以又将 methodClassOld 重新设回去
        subscriberClassByMethodKey.put(methodKey, methodClassOld);
        return false;
    }
}
```

进行上述操作后，就找到了 subscriber 包含的所有监听方法了，这些方法都会保存到 `List<SubscriberMethod>` 中。拿到所有方法后，`register` 方法就需要对 subscriber 及其所有监听方法进行归类了

归类的目的是既是为了方便后续操作也是为了提高效率。 因为可能同时存在多个 subscriber 声明了多个对同种类型消息的监听方法，那么就需要将每种消息类型和其当前的所有监听方法对应起来，提高消息的发送效率。而且在 subscriber 解除注册时，也需要将 subscriber 包含的所有监听方法都给移除掉，那么也需要预先进行归类。监听方法也可以设定自己对消息处理的优先级顺序，所以需要预先对监听方法进行排序

```java
public void register(Object subscriber) {
    Class<?> subscriberClass = subscriber.getClass();
    List<SubscriberMethod> subscriberMethods = subscriberMethodFinder.findSubscriberMethods(subscriberClass);
    synchronized (this) {
        for (SubscriberMethod subscriberMethod : subscriberMethods) {
            subscribe(subscriber, subscriberMethod);
        }
    }
}

private final Map<Class<?>, CopyOnWriteArrayList<Subscription>> subscriptionsByEventType;

private final Map<Object, List<Class<?>>> typesBySubscriber;

// Must be called in synchronized block
private void subscribe(Object subscriber, SubscriberMethod subscriberMethod) {
    Class<?> eventType = subscriberMethod.eventType;
    Subscription newSubscription = new Subscription(subscriber, subscriberMethod);
    //subscriptionsByEventType 以消息类型 eventType 作为 key，value 存储了所有对该 eventType 的订阅者，提高后续在发送消息时的效率
    CopyOnWriteArrayList<Subscription> subscriptions = subscriptionsByEventType.get(eventType);
    if (subscriptions == null) {
        subscriptions = new CopyOnWriteArrayList<>();
        subscriptionsByEventType.put(eventType, subscriptions);
    } else {
        if (subscriptions.contains(newSubscription)) {
            //说明某个 Subscriber 重复注册了
            throw new EventBusException("Subscriber " + subscriber.getClass() + " already registered to event "
                    + eventType);
        }
    }

    //将订阅者根据消息优先级高低进行排序
    int size = subscriptions.size();
    for (int i = 0; i <= size; i++) {
        if (i == size || subscriberMethod.priority > subscriptions.get(i).subscriberMethod.priority) {
            subscriptions.add(i, newSubscription);
            break;
        }
    }

    //typesBySubscriber 以订阅者 subscriber 作为 key，value 存储了其订阅的所有 eventType
    //用于向外提供某个类是否已注册的功能，也方便后续在 unregister 时移除 subscriber 下的所有监听方法
    List<Class<?>> subscribedEvents = typesBySubscriber.get(subscriber);
    if (subscribedEvents == null) {
        subscribedEvents = new ArrayList<>();
        typesBySubscriber.put(subscriber, subscribedEvents);
    }
    subscribedEvents.add(eventType);

    //下面是关于粘性事件的处理，后续再进行介绍
    if (subscriberMethod.sticky) {
        if (eventInheritance) {
            // Existing sticky events of all subclasses of eventType have to be considered.
            // Note: Iterating over all events may be inefficient with lots of sticky events,
            // thus data structure should be changed to allow a more efficient lookup
            // (e.g. an additional map storing sub classes of super classes: Class -> List<Class>).
            Set<Map.Entry<Class<?>, Object>> entries = stickyEvents.entrySet();
            for (Map.Entry<Class<?>, Object> entry : entries) {
                Class<?> candidateEventType = entry.getKey();
                if (eventType.isAssignableFrom(candidateEventType)) {
                    Object stickyEvent = entry.getValue();
                    checkPostStickyEventToSubscription(newSubscription, stickyEvent);
                }
            }
        } else {
            Object stickyEvent = stickyEvents.get(eventType);
            checkPostStickyEventToSubscription(newSubscription, stickyEvent);
        }
    }
}
```

# 二、消息

## 1、消息的执行策略

在介绍消息的具体发送步骤前，先来了解下 EventBus 几种不同的消息执行策略。执行策略由枚举 ThreadMode 来定义，在 `@Subscribe` 注解中进行声明。执行策略决定了消息接收方是在哪一个线程接收到消息的

| ThreadMode   | 执行线程                 |                                                              |
| ------------ | ------------------------ | ------------------------------------------------------------ |
| POSTING      | 在发送事件的线程中执行   | 直接调用消息接收方                                           |
| MAIN         | 在主线程中执行           | 如果事件就是在主线程发送的，则直接调用消息接收方，否则通过 mainThreadPoster 进行处理 |
| MAIN_ORDERED | 在主线程中按顺序执行     | 通过 mainThreadPoster 进行处理，以此保证消息处理的有序性     |
| BACKGROUND   | 在后台线程中按顺序执行   | 如果事件是在主线程发送的，则提交给 backgroundPoster 处理，否则直接调用消息接收方 |
| ASYNC        | 提交给空闲的后台线程执行 | 将消息提交到 asyncPoster 进行处理                            |

执行策略的具体细分逻辑是在 EventBus 类的`postToSubscription` 方法完成的

```java
private void postToSubscription(Subscription subscription, Object event, boolean isMainThread) {
    switch (subscription.subscriberMethod.threadMode) {
        case POSTING:
            invokeSubscriber(subscription, event);
            break;
        case MAIN:
            if (isMainThread) {
                invokeSubscriber(subscription, event);
            } else {
                mainThreadPoster.enqueue(subscription, event);
            }
            break;
        case MAIN_ORDERED:
            if (mainThreadPoster != null) {
                mainThreadPoster.enqueue(subscription, event);
            } else {
                // temporary: technically not correct as poster not decoupled from subscriber
                invokeSubscriber(subscription, event);
            }
            break;
        case BACKGROUND:
            if (isMainThread) {
                backgroundPoster.enqueue(subscription, event);
            } else {
                invokeSubscriber(subscription, event);
            }
            break;
        case ASYNC:
            asyncPoster.enqueue(subscription, event);
            break;
        default:
            throw new IllegalStateException("Unknown thread mode: " + subscription.subscriberMethod.threadMode);
    }
}
```

例如，对于 AsyncPoster 来说，其每接收到一个消息，都会直接在 `enqueue` 方法中将自己（Runnable）提交给线程池进行处理，而使用的线程池默认是 `Executors.newCachedThreadPool()`，该线程池每接收到一个任务都会马上交由线程进行处理，所以 AsyncPoster 并不保证消息处理的有序性，但在消息处理的及时性方面会比较高，且每次提交给 AsyncPoster 的消息可能都是由不同的线程来处理

```java
class AsyncPoster implements Runnable, Poster {

    private final PendingPostQueue queue;
    private final EventBus eventBus;

    AsyncPoster(EventBus eventBus) {
        this.eventBus = eventBus;
        queue = new PendingPostQueue();
    }

    public void enqueue(Subscription subscription, Object event) {
        PendingPost pendingPost = PendingPost.obtainPendingPost(subscription, event);
        queue.enqueue(pendingPost);
        eventBus.getExecutorService().execute(this);
    }

    @Override
    public void run() {
        PendingPost pendingPost = queue.poll();
        if(pendingPost == null) {
            throw new IllegalStateException("No pending post available");
        }
        eventBus.invokeSubscriber(pendingPost);
    }

}
```

而 BackgroundPoster 会将任务依次缓存到 PendingPostQueue 中，每次只取出一个任务交由线程池来执行，所以 BackgroundPoster 会保证消息队列在处理时的有序性，但在消息处理的及时性方面相比 AsyncPoster 要低一些

```java
final class BackgroundPoster implements Runnable, Poster {

    private final PendingPostQueue queue;
    private final EventBus eventBus;

    private volatile boolean executorRunning;

    BackgroundPoster(EventBus eventBus) {
        this.eventBus = eventBus;
        queue = new PendingPostQueue();
    }

    public void enqueue(Subscription subscription, Object event) {
        PendingPost pendingPost = PendingPost.obtainPendingPost(subscription, event);
        synchronized (this) {
            queue.enqueue(pendingPost);
            if (!executorRunning) {
                executorRunning = true;
                eventBus.getExecutorService().execute(this);
            }
        }
    }

    ···
}
```

而不管是使用什么消息处理策略，最终都是通过调用以下方法来反射调用监听方法

```java
void invokeSubscriber(PendingPost pendingPost) {
    Object event = pendingPost.event;
    Subscription subscription = pendingPost.subscription;
    PendingPost.releasePendingPost(pendingPost);
    if (subscription.active) {
        invokeSubscriber(subscription, event);
    }
}

void invokeSubscriber(Subscription subscription, Object event) {
    try {
        subscription.subscriberMethod.method.invoke(subscription.subscriber, event);
    } catch (InvocationTargetException e) {
        handleSubscriberException(subscription, event, e.getCause());
    } catch (IllegalAccessException e) {
        throw new IllegalStateException("Unexpected exception", e);
    }
}
```

## 2、发送非黏性消息

`EventBus.getDefault().post(Any)`方法用于发送非黏性消息。EventBus 会通过 ThreadLocal 为每个发送消息的线程维护一个 PostingThreadState 对象，用于为每个线程维护一个消息队列及其它辅助参数

```java
/**
 * For ThreadLocal, much faster to set (and get multiple values).
 */
final static class PostingThreadState {
    final List<Object> eventQueue = new ArrayList<>();
    boolean isPosting;
    boolean isMainThread;
    Subscription subscription;
    Object event;
    boolean canceled;
}

private final ThreadLocal<PostingThreadState> currentPostingThreadState = new ThreadLocal<PostingThreadState>() {
    @Override
    protected PostingThreadState initialValue() {
        return new PostingThreadState();
    }
};

/**
 * Posts the given event to the event bus.
 */
public void post(Object event) {
    PostingThreadState postingState = currentPostingThreadState.get();
    List<Object> eventQueue = postingState.eventQueue;
    //将消息添加到消息队列
    eventQueue.add(event);

    if (!postingState.isPosting) {
        //是否在主线程发送的消息
        postingState.isMainThread = isMainThread();
        //标记当前正在发送消息中
        postingState.isPosting = true;
        if (postingState.canceled) {
            throw new EventBusException("Internal error. Abort state was not reset");
        }
        try {
            while (!eventQueue.isEmpty()) {
                postSingleEvent(eventQueue.remove(0), postingState);
            }
        } finally {
            postingState.isPosting = false;
            postingState.isMainThread = false;
        }
    }
}

```

每次 post 进来的消息都会先存到消息队列 `eventQueue`中，然后通过 while 循环进行处理，消息处理逻辑是通过 `postSingleEvent`方法来完成的

其主要逻辑是：

1. 假设 EventA 继承于 EventB，那么当发送的消息类型是 EventA 时，就需要考虑 EventB 的监听方法是否可以接收到 EventA，即需要考虑消息类型是否具有继承关系
2. 具有继承关系。此时就需要拿到 EventA 的所有父类型，然后根据 EventA 本身和其父类型关联到的所有监听方法依次进行消息发送
3. 不具有继承关系。此时只需要向 EventA 的监听方法进行消息发送即可
4. 如果发送的消息最终没有找到任何接收者，且 `sendNoSubscriberEvent` 为 true，那么就主动发送一个 NoSubscriberEvent 事件，用于向外通知消息没有找到任何接收者
5. 监听方法之间可以设定消息处理的优先级高低，高优先级的方法可以通过调用 `cancelEventDelivery` 方法来拦截事件，不再继续向下发送。但只有在 `POSTING` 模式下才能拦截事件，因为只有在这个模式下才能保证监听方法是按照严格的先后顺序被执行的

最终，发送的消息都会通过 `postToSubscription`方法来完成，根据接收者方法不同的处理策略进行处理

```java
private void postSingleEvent(Object event, PostingThreadState postingState) throws Error {
    Class<?> eventClass = event.getClass();
    //用于标记是否有找到消息的接收者
    boolean subscriptionFound = false;
    if (eventInheritance) {
        //步骤2
        List<Class<?>> eventTypes = lookupAllEventTypes(eventClass);
        int countTypes = eventTypes.size();
        for (int h = 0; h < countTypes; h++) {
            Class<?> clazz = eventTypes.get(h);
            subscriptionFound |= postSingleEventForEventType(event, postingState, clazz);
        }
    } else {
        //步骤3
        subscriptionFound = postSingleEventForEventType(event, postingState, eventClass);
    }
    if (!subscriptionFound) {
        if (logNoSubscriberMessages) {
            logger.log(Level.FINE, "No subscribers registered for event " + eventClass);
        }
        if (sendNoSubscriberEvent && eventClass != NoSubscriberEvent.class &&
                eventClass != SubscriberExceptionEvent.class) {
            //步骤4
            post(new NoSubscriberEvent(this, event));
        }
    }
}

private boolean postSingleEventForEventType(Object event, PostingThreadState postingState, Class<?> eventClass) {
    CopyOnWriteArrayList<Subscription> subscriptions;
    synchronized (this) {
        //找到所有监听者
        subscriptions = subscriptionsByEventType.get(eventClass);
    }
    if (subscriptions != null && !subscriptions.isEmpty()) {
        for (Subscription subscription : subscriptions) {
            postingState.event = event;
            postingState.subscription = subscription;
            boolean aborted;
            try {
                postToSubscription(subscription, event, postingState.isMainThread);
                aborted = postingState.canceled;
            } finally {
                postingState.event = null;
                postingState.subscription = null;
                postingState.canceled = false;
            }
            //步骤5
            if (aborted) {
                break;
            }
        }
        return true;
    }
    return false;
}

```

## 3、发送黏性消息

黏性消息的意义是为了使得在消息发出来后，即使是后续再进行 `register` 的 subscriber 也可以收到之前发送的消息，这需要将 `@Subscribe` 注解的 `sticky` 属性设为 true，即表明消息接收方希望接收黏性消息

`EventBus.getDefault().postSticky(Any)`方法就用于发送黏性消息。黏性事件会被保存到 `stickyEvents` 这个 Map 中，key 是 event 的 Class 对象，value 是 event 本身，这也说明对于同一类型的黏性消息来说，只会保存其最后一个消息

```java
private final Map<Class<?>, Object> stickyEvents;

/**
 * Posts the given event to the event bus and holds on to the event (because it is sticky). The most recent sticky
 * event of an event's type is kept in memory for future access by subscribers using {@link Subscribe#sticky()}.
 */
public void postSticky(Object event) {
    synchronized (stickyEvents) {
        stickyEvents.put(event.getClass(), event);
    }
    // Should be posted after it is putted, in case the subscriber wants to remove immediately
    post(event);
}
```

对于一个黏性消息，会有两种不同的时机被 subscriber 接收到

1. 调用 postSticky 方法时，被其现有的 subscriber 直接接收到，这种方式通过在 postSticky 方法里调用 post 方法来实现
2. 调用 register 方法时，新添加的 subscriber 会判断 stickyEvents 中是否存在关联的 event 需要进行分发

这里主要看第二种情况。register 操作会在 subscribe 方法里完成黏性事件的分发。和 post 操作一样，发送黏性事件时也需要考虑 event 的继承关系

```java
private void subscribe(Object subscriber, SubscriberMethod subscriberMethod) {

    ···

    if (subscriberMethod.sticky) {
        if (eventInheritance) {
            // Existing sticky events of all subclasses of eventType have to be considered.
            // Note: Iterating over all events may be inefficient with lots of sticky events,
            // thus data structure should be changed to allow a more efficient lookup
            // (e.g. an additional map storing sub classes of super classes: Class -> List<Class>).

            //事件类型需要考虑其继承关系
            //因此需要判断每一个 stickyEvent 的父类型是否存在监听者，有的话就需要都进行回调
            Set<Map.Entry<Class<?>, Object>> entries = stickyEvents.entrySet();
            for (Map.Entry<Class<?>, Object> entry : entries) {
                Class<?> candidateEventType = entry.getKey();
                if (eventType.isAssignableFrom(candidateEventType)) {
                    Object stickyEvent = entry.getValue();
                    checkPostStickyEventToSubscription(newSubscription, stickyEvent);
                }
            }
        } else {
            //事件类型不需要考虑其继承关系
            Object stickyEvent = stickyEvents.get(eventType);
            checkPostStickyEventToSubscription(newSubscription, stickyEvent);
        }
    }
}

private void checkPostStickyEventToSubscription(Subscription newSubscription, Object stickyEvent) {
    if (stickyEvent != null) {
        // If the subscriber is trying to abort the event, it will fail (event is not tracked in posting state)
        // --> Strange corner case, which we don't take care of here.
        postToSubscription(newSubscription, stickyEvent, isMainThread());
    }
}
```

## 4、移除黏性事件

移除指定的黏性事件可以通过以下方法来实现，都是用于将指定事件从 `stickyEvents` 中移除

```java
/**
 * Remove and gets the recent sticky event for the given event type.
 *
 * @see #postSticky(Object)
 */
public <T> T removeStickyEvent(Class<T> eventType) {
    synchronized (stickyEvents) {
        return eventType.cast(stickyEvents.remove(eventType));
    }
}

/**
 * Removes the sticky event if it equals to the given event.
 *
 * @return true if the events matched and the sticky event was removed.
 */
public boolean removeStickyEvent(Object event) {
    synchronized (stickyEvents) {
        Class<?> eventType = event.getClass();
        Object existingEvent = stickyEvents.get(eventType);
        if (event.equals(existingEvent)) {
            stickyEvents.remove(eventType);
            return true;
        } else {
            return false;
        }
    }
}

```

# 三、解除注册

解除注册的目的是为了避免内存泄露，EventBus 使用了单例模式，如果不主动解除注册的话，EventBus 就会一直持有 subscriber。解除注册是通过 `unregister`方法来实现的，该方法逻辑也比较简单，只是将 subscriber 以及其关联的所有 method 对象从集合中移除而已

而此处虽然会将关于 subscriber 的信息均给移除掉，但是在 SubscriberMethodFinder 中的静态成员变量 `METHOD_CACHE` 依然会缓存着已经注册过的 subscriber 的信息，这也是为了在某些 subscriber 会先后多次注册 EventBus 时可以做到信息复用，避免多次循环反射

```java
/**
 * Unregisters the given subscriber from all event classes.
 */
public synchronized void unregister(Object subscriber) {
    List<Class<?>> subscribedTypes = typesBySubscriber.get(subscriber);
    if (subscribedTypes != null) {
        for (Class<?> eventType : subscribedTypes) {
            unsubscribeByEventType(subscriber, eventType);
        }
        typesBySubscriber.remove(subscriber);
    } else {
        logger.log(Level.WARNING, "Subscriber to unregister was not registered before: " + subscriber.getClass());
    }
}

/**
 * Only updates subscriptionsByEventType, not typesBySubscriber! Caller must update typesBySubscriber.
 */
private void unsubscribeByEventType(Object subscriber, Class<?> eventType) {
    List<Subscription> subscriptions = subscriptionsByEventType.get(eventType);
    if (subscriptions != null) {
        int size = subscriptions.size();
        for (int i = 0; i < size; i++) {
            Subscription subscription = subscriptions.get(i);
            if (subscription.subscriber == subscriber) {
                subscription.active = false;
                subscriptions.remove(i);
                i--;
                size--;
            }
        }
    }
}
```

# 四、注解处理器

使用注解处理器（Annotation Processing Tool）可以避免 subscriber 进行注册时的多次循环反射操作，极大提升了 EventBus 的运行效率。注解处理器是一种注解处理工具，用来在编译期扫描和处理注解，通过注解来生成 Java 文件。即以注解作为桥梁，通过预先规定好的代码生成规则来自动生成 Java 文件。此类注解框架的代表有 ButterKnife、Dragger2、EventBus 等

Java API 已经提供了扫描源码并解析注解的框架，开发者可以通过继承 AbstractProcessor 类来实现自己的注解解析逻辑。APT 的原理就是在注解了某些代码元素（如字段、函数、类等）后，在编译时编译器会检查 AbstractProcessor 的子类，并且自动调用其 `process()` 方法，然后将添加了指定注解的所有代码元素作为参数传递给该方法，开发者再根据注解元素在编译期输出对应的 Java 代码

关于 APT 技术的原理和应用可以看这篇文章：[Android APT 实例讲解](https://juejin.im/post/6844903753108160525)

在 Kotlin 环境引入注解处理器的方法如下所示：

```groovy
apply plugin: 'kotlin-kapt'

kapt {
    arguments {
        arg('eventBusIndex', 'github.leavesc.demo.MyEventBusIndex')
    }
}

dependencies {
    implementation "org.greenrobot:eventbus:3.2.0"
    kapt "org.greenrobot:eventbus-annotation-processor:3.2.0"
}
```

当中，MyEventBusIndex 就是在编译阶段将生成的辅助文件，`github.leavesc.demo.MyEventBusIndex` 就是生成的辅助文件的包名路径，可以由我们自己定义

原始文件：

```kotlin
/**
 * @Author: leavesCZY
 * @Github：https://github.com/leavesCZY
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    @Subscribe
    fun fun1(msg: String) {

    }

    @Subscribe(threadMode = ThreadMode.MAIN, priority = 100)
    fun fun2(msg: String) {

    }

}
```

编译过后生成的辅助文件如下所示。可以看出，MyEventBusIndex 文件中封装了 subscriber 和其所有监听方法的签名信息，这样我们就无需在运行时再来进行解析了，而是直接在编译阶段就生成好了

```java
/** This class is generated by EventBus, do not edit. */
public class MyEventBusIndex implements SubscriberInfoIndex {
    private static final Map<Class<?>, SubscriberInfo> SUBSCRIBER_INDEX;

    static {
        SUBSCRIBER_INDEX = new HashMap<Class<?>, SubscriberInfo>();

        putIndex(new SimpleSubscriberInfo(MainActivity.class, true, new SubscriberMethodInfo[] {
            new SubscriberMethodInfo("fun1", String.class),
            new SubscriberMethodInfo("fun2", String.class, ThreadMode.MAIN, 100, false),
        }));

    }

    private static void putIndex(SubscriberInfo info) {
        SUBSCRIBER_INDEX.put(info.getSubscriberClass(), info);
    }

    @Override
    public SubscriberInfo getSubscriberInfo(Class<?> subscriberClass) {
        SubscriberInfo info = SUBSCRIBER_INDEX.get(subscriberClass);
        if (info != null) {
            return info;
        } else {
            return null;
        }
    }
}
```

需要注意的是，在生成了辅助文件后，还需要通过这些类文件来初始化 EventBus

```kotlin
EventBus.builder().addIndex(MyEventBusIndex()).installDefaultEventBus()
```

注入的辅助文件会被保存到 SubscriberMethodFinder 类的成员变量 `subscriberInfoIndexes` 中，`findUsingInfo` 方法会先尝试从辅助文件中获取 SubscriberMethod，只有在获取不到的时候才会通过性能较低的反射操作来完成

```java
private List<SubscriberMethod> findUsingInfo(Class<?> subscriberClass) {
    FindState findState = prepareFindState();
    findState.initForSubscriber(subscriberClass);
    while (findState.clazz != null) {
        //在没有使用注解处理器的情况下，findState.subscriberInfo 和 subscriberInfoIndexes 的默认值都是为 null，所以 getSubscriberInfo 会返回 null
        //此时就需要通过 findUsingReflectionInSingleClass 方法来进行反射获取

        //而在有使用注解处理器的情况下，subscriberInfoIndexes 就存储了自动生成的辅助文件，此时 getSubscriberInfo 就可以从辅助文件中拿到目标信息
        //从而避免了反射操作

        findState.subscriberInfo = getSubscriberInfo(findState);
        if (findState.subscriberInfo != null) {
            SubscriberMethod[] array = findState.subscriberInfo.getSubscriberMethods();
            for (SubscriberMethod subscriberMethod : array) {
                if (findState.checkAdd(subscriberMethod.method, subscriberMethod.eventType)) {
                    findState.subscriberMethods.add(subscriberMethod);
                }
            }
        } else {
            findUsingReflectionInSingleClass(findState);
        }
        findState.moveToSuperclass();
    }
    return getMethodsAndRelease(findState);
}

private SubscriberInfo getSubscriberInfo(FindState findState) {
    if (findState.subscriberInfo != null && findState.subscriberInfo.getSuperSubscriberInfo() != null) {
        SubscriberInfo superclassInfo = findState.subscriberInfo.getSuperSubscriberInfo();
        if (findState.clazz == superclassInfo.getSubscriberClass()) {
            return superclassInfo;
        }
    }
    if (subscriberInfoIndexes != null) {
        for (SubscriberInfoIndex index : subscriberInfoIndexes) {
            SubscriberInfo info = index.getSubscriberInfo(findState.clazz);
            if (info != null) {
                return info;
            }
        }
    }
    return null;
}
```

使用了注解处理器后也有一定的弊端。由于 MyEventBusIndex 是通过静态常量类型的 Map 来保存所有的方法签名信息，当在初始化 EventBus 时该 Map 就同时被初始化了，这就相当于在一开始就进行了全量加载，而某些 subscriber 我们可能不会使用到，这就造成了内存浪费。而如果是通过反射来获取，那就相当于在按需加载，只有 subscriber 进行注册了才会去缓存 subscriber 带有的监听方法

# 五、一些坑

## 1、奇怪的继承关系

上文有介绍到，子类可以继承父类的 Subscribe 方法。但有一个比较奇怪的地方是：如果子类重写了父类多个 Subscribe 方法的话，就会抛出 IllegalStateException。例如，在下面的例子中。父类 BaseActivity 声明了两个 Subscribe 方法，子类 MainActivity 重写了这两个方法，此时运行后就会抛出 IllegalStateException。而如果 MainActivity 不重写或者只重写一个方法的话，就可以正常运行

```java
/**
 * @Author: leavesCZY
 * @Github：https://github.com/leavesCZY
 */
open class BaseActivity : AppCompatActivity() {

    @Subscribe
    open fun fun1(msg: String) {

    }

    @Subscribe
    open fun fun2(msg: String) {

    }

}

class MainActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        EventBus.getDefault().register(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
    }

    @Subscribe
    override fun fun1(msg: String) {

    }

    @Subscribe
    override fun fun2(msg: String) {

    }

}
```

按道理来说，如果子类重写了父类一个 Subscribe 方法都可以正常使用的话，那么重写两个也应该可以正常使用才对。可是上述例子就表现得 EventBus 好像有 bug 似的。通过定位堆栈信息，可以发现是在 `FindState` 的 `checkAdd` 方法抛出了异常

其抛出异常的步骤是这样的：

1. EventBus 对 Subscribe 方法的解析方向是子类向父类进行的，同个类下的 Subscribe 方法按照声明顺序进行解析
2. 当 `checkAdd` 方法开始解析 `BaseActivity` 的 `fun2` 方法时，`existing` 对象就是 `BaseActivity.fun1`，此时就会执行到操作1，而由于子类已经重写了 `fun1` 方法，此时 `checkAddWithMethodSignature` 方法就会返回 false，最终导致抛出异常

```java
boolean checkAdd(Method method, Class<?> eventType) {
    // 2 level check: 1st level with event type only (fast), 2nd level with complete signature when required.
    // Usually a subscriber doesn't have methods listening to the same event type.
    Object existing = anyMethodByEventType.put(eventType, method);
    if (existing == null) {
        return true;
    } else {
        if (existing instanceof Method) {
            //操作1
            if (!checkAddWithMethodSignature((Method) existing, eventType)) {
                // Paranoia check
                throw new IllegalStateException();
            }
            // Put any non-Method object to "consume" the existing Method
            anyMethodByEventType.put(eventType, this);
        }
        return checkAddWithMethodSignature(method, eventType);
    }
}
```

EventBus 有一个 issues 也反馈了这个问题：[issues](https://github.com/greenrobot/EventBus/issues/539)，该问题在 2018 年时就已经存在了，EeventBus 的作者也只是回复说：**只在子类进行方法监听**

## 2、移除黏性消息

`removeStickyEvent` 方法会有一个比较让人误解的点：对于通过 `EventBus.getDefault().postSticky(XXX)`方法发送的黏性消息无法通过 `removeStickyEvent` 方法来使现有的监听者拦截该事件

例如，假设下面的两个方法都已经处于注册状态了，`postSticky` 后，即使在 `fun1` 方法中移除了黏性消息，`fun2` 方法也可以接收到消息。这是因为 `postSticky` 方法最终也是要靠调用 `post` 方法来完成消息发送，而 `post` 方法并不受 `stickyEvents` 的影响

```java
@Subscribe(sticky = true)
fun fun1(msg: String) {
    EventBus.getDefault().removeStickyEvent(msg)
}

@Subscribe(sticky = true)
fun fun2(msg: String) {

}
```

而如果 EventBus 中已经存储了黏性事件，那么在上述两个方法刚 register 时，`fun1` 方法就可以拦截住消息使 `fun2` 方法接收不到消息。这是因为 `register` 方法是在 for 循环中遍历 method，如果之前的方法已经移除了黏性消息的话，那么后续方法就没有黏性消息需要处理了

```java
public void register(Object subscriber) {
    Class<?> subscriberClass = subscriber.getClass();
    List<SubscriberMethod> subscriberMethods = subscriberMethodFinder.findSubscriberMethods(subscriberClass);
    synchronized (this) {
        //在 for 循环中遍历 method 
        for (SubscriberMethod subscriberMethod : subscriberMethods) {
            subscribe(subscriber, subscriberMethod);
        }
    }
}
```

# 六、总结

EventBus 的源码解析到这里就结束了，本文所讲的内容应该也已经涵盖了大部分内容了。这里再来为 EventBus 的实现流程做一个总结

1. EventBus 包含 register 和 unregister 两个方法用于标记当前 subscriber 是否需要接收消息，内部对应向 CopyOnWriteArrayList 添加和移除元素这两个操作
2. 每当有 event 被 post 出来时，就需要根据 eventClass 对象找到所有所有声明了 @Subscribe 注解且对这种消息类型进行监听的方法，这些方法都是在 subscriber 进行 register 的时候，从 subscriber 中获取到的
3. 从 subscriber 中获取所有监听方法的方式有两种。第一种是在运行阶段通过反射来拿到，对应的是没有配置注解处理器的情况。第二种对应的是有配置注解处理器的情况，通过在编译阶段全局扫描  @Subscribe 注解并生成辅助文件，从而在 register 的时候省去效率低下的反射操作。不管是通过什么方式进行获取，拿到所有方法后都会将 methods 按照消息类型 eventType 进行归类，方便后续遍历
4. 每当有消息被发送出来时，就根据 event 对应的 Class 对象找到相应的监听方法，然后通过反射的方式来回调方法。外部可以在初始化 EventBus 的时候选择是否要考虑 event 的继承关系，即在 event 被 Post 出来时，对 event 的父类型进行监听的方法是否需要被回调

EventBus 的实现思路并不算多难，难的是在实现的时候可以方方面面都考虑周全，做到稳定高效，从 2018 年到 2020 年也才发布了两个版本（也许是作者懒得更新？）原理懂了，那么下一篇就进入实战篇，自己来动手实现一个 EventBus 😇😇