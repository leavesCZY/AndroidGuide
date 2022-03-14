> 公众号：[字节数组](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/adbc507fc3704fd8955aae739a433db2~tplv-k3u1fbpfcp-zoom-1.image)
>
> 希望对你有所帮助 🤣🤣

> 对于 Android Developer 来说，很多开源库都是属于**开发必备**的知识点，从使用方式到实现原理再到源码解析，这些都需要我们有一定程度的了解和运用能力。所以我打算来写一系列关于开源库**源码解析**和**实战演练**的文章，初定的目标是 **EventBus、ARouter、LeakCanary、Retrofit、Glide、OkHttp、Coil** 等七个知名开源库，希望对你有所帮助 🤣🤣

# 一、前言

Retrofit 也是现在 Android 应用开发中的标配之一了吧？笔者使用 Retrofit 蛮久的了，一直以来用着也挺舒心的，没遇到啥大的坑。总这样用着不来了解下其底层实现好像也不太好，趁着动笔写 [**三方库源码笔记**](https://juejin.cn/user/923245496518439/posts) 系列文章就来对 Retrofit 进行一次（~~自我感觉的~~）全面的源码解析吧 ~

Retrofit 是这么自我介绍的：**A type-safe HTTP client for Android and Java.**  这说明 Retrofit 的内部实现并不需要依赖于 Android 平台，而是可以用于任意的 Java 客户端，Retrofit 只是对 Android 平台进行了特殊实现而已。此外，现在 Android 平台的主流开发语言早已是 Kotlin 了，所以本篇文章所写的例子都采用了 Kotlin ~

对 Kotlin 语言不熟悉的同学可以看我的这篇文章来入门：[两万六千字带你 Kotlin 入门](https://juejin.im/post/6880602489297895438)

Retrofit 的源码并不算太复杂，但由于应用了很多种设计模式，所以在流程上会比较绕。笔者陆陆续续看了几天源码后就开始动笔，但总感觉没法阐述得特别清晰，写着写着就成了目前的样子。读者如果觉得我有哪里写得不太好的地方也欢迎给下建议 😂😂

# 二、小例子

先来看几个简单的小例子，后续的讲解都会围绕这几个例子来展开

```groovy
dependencies {
    implementation 'com.squareup.retrofit2:retrofit:2.9.0'
}
```

在除了 Retrofit 外不引入其它任何依赖库的情况下，我们发起一个网络请求的流程大致如下所示：

```kotlin
/**
 * @Author: leavesCZY
 * @Github：https://github.com/leavesCZY
 */
interface ApiService {

    @GET("getUserData")
    fun getUserData(): Call<ResponseBody>

}

fun main() {
    val retrofit = Retrofit.Builder()
        .baseUrl("https://mockapi.eolinker.com/9IiwI82f58c23411240ed608ceca204b2f185014507cbe3/")
        .build()
    val service = retrofit.create(ApiService::class.java)
    val call: Call<ResponseBody> = service.getUserData()
    call.enqueue(object : Callback<ResponseBody> {
        override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
            val userBean = response.body()?.string()
            println("userBean: $userBean")
        }

        override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
            println("onFailure: $t")
        }
    })
}
```

输出结果：

```kotlin
userBean: {"userName":"JBl","userAge":7816977017260632}
```

Retrofit 是建立在 OkHttp 之上的一个网络请求封装库，内部依靠 OkHttp 来完成实际的网络请求。Retrofit 在使用上很简洁，API 是通过 interface 来声明的。不知道读者第一次使用 Retrofit 的时候是什么感受，我第一次使用的时候就觉得 Retrofit 好神奇，我只需要通过 interface 来声明 **API 路径、请求方式、请求参数、返回值类型**等各个配置项，然后调用方法就可以发起网络请求了，相比 OkHttp 和 Volley 这些网络请求库真的是简洁到没朋友

可以看到，`getUserData()`方法的请求结果是一个 Json 格式的字符串，其返回值类型被定义为 `Call<ResponseBody>`，此处的 ResponseBody 即 `okhttp3.ResponseBody`，是 OkHttp 提供的对网络请求结果的包装类，Call 即`retrofit2.Call`，是 Retrofit 对 `okhttp3.Call`做的一层包装，OkHttp 在实际发起请求的时候使用的回调是`okhttp3.Call`，回调内部会中转调用 `retrofit2.Call`，以便将请求结果转交给外部

## 1、converter-gson

上述请求虽然简单，但还不够方便，因为既然 API 的返回值我们已知就是 Json 格式的了，那么我们自然就希望 `getUserData()` 方法的返回值直接就是一个 Bean 对象，而不是拿到一个 String 后还需要自己再去进行反序列化，这可以通过引入`converter-gson`这个库来达到这个效果

```groovy
dependencies {
    implementation 'com.squareup.retrofit2:retrofit:2.9.0'
    implementation 'com.squareup.retrofit2:converter-gson:2.5.0'
}
```

代码再做点小改动，之后就可以直接在 Callback 中拿到 UserBean 对象了

```kotlin
/**
 * @Author: leavesCZY
 * @Github：https://github.com/leavesCZY
 */
interface ApiService {

    @GET("getUserData")
    fun getUserData(): Call<UserBean>

}

data class UserBean(val userName: String, val userAge: Long)

fun main() {
    val retrofit = Retrofit.Builder()
        .baseUrl("https://mockapi.eolinker.com/9IiwI82f58c23411240ed608ceca204b2f185014507cbe3/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    val service = retrofit.create(ApiService::class.java)
    val call: Call<UserBean> = service.getUserData()
    call.enqueue(object : Callback<UserBean> {
        override fun onResponse(call: Call<UserBean>, response: Response<UserBean>) {
            val userBean = response.body()
            println("userBean: $userBean")
        }

        override fun onFailure(call: Call<UserBean>, t: Throwable) {
            println("onFailure: $t")
        }
    })
}
```

## 2、adapter-rxjava2

再然后，如果也看 `Call<UserBean>`不爽，想要通过 RxJava 的方式来进行网络请求可不可以？也行，此时就需要再引入`adapter-rxjava2`这个库了

```kotlin
dependencies {
    implementation 'com.squareup.retrofit2:retrofit:2.9.0'
    implementation 'com.squareup.retrofit2:converter-gson:2.5.0'
    implementation 'com.squareup.retrofit2:adapter-rxjava2:2.9.0'
}
```

代码再来做点小改动，此时就完全不用使用到`Call.enqueue`来显式发起网络请求了，在进行 subscribe 的时候就会自动发起网络请求

```kotlin
/**
 * @Author: leavesCZY
 * @Github：https://github.com/leavesCZY
 */
interface ApiService {

    @GET("getUserData")
    fun getUserData(): Observable<UserBean>

}

data class UserBean(val userName: String, val userAge: Long)

fun main() {
    val retrofit = Retrofit.Builder()
        .baseUrl("https://mockapi.eolinker.com/9IiwI82f58c23411240ed608ceca204b2f185014507cbe3/")
        .addConverterFactory(GsonConverterFactory.create())
        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
        .build()
    val service = retrofit.create(ApiService::class.java)
    val call: Observable<UserBean> = service.getUserData()
    call.subscribe(object : Consumer<UserBean> {
        override fun accept(userBean: UserBean?) {
            println("userBean: $userBean")
        }

    }, object : Consumer<Throwable> {
        override fun accept(t: Throwable?) {
            println("onFailure: $t")
        }
    })
}
```

## 3、提出疑问

可以看到，Retrofit 在抽象程度上是很高的。不管是需要 Call 类还是 Observable 类型的包装类，只需要添加不同的`CallAdapterFactory`即可，就算想返回 LiveData 类型都是可以实现的。也不管是需要 ResponseBody 还是具体的 Bean 对象，也只需要添加不同的 `ConverterFactory` 即可，就算网络请求返回值是 XML 格式也可以进行映射解析

之后，我们就带着几个问题来逐步看 Retrofit 的源码：

1. Retrofit 是如何将 interface 内部的方法转化为一个个实际的 GET、POST、DELETE 等各式各样的网络请求的呢？例如，Retrofit 是如何将 getUserData() 方法转换为一个 OkHttp 的 GET 请求的呢？
2. Retrofit 是如何将 API 的返回值映射为具体的 Bean 对象的呢？例如，ResponseBody 是如何映射为 UserBean 的呢？
3. Retrofit 是如何抽象不同的接口方法的返回值包装类的呢？例如，Call 是如何替换为 Observable 的呢？

# 三、Retrofit.create() 

先来看下`retrofit.create`方法做了什么

```java
public <T> T create(final Class<T> service) {
    validateServiceInterface(service);
    return (T)
        Proxy.newProxyInstance(
            service.getClassLoader(),
            new Class<?>[] {service},
            new InvocationHandler() {
              private final Platform platform = Platform.get();
              private final Object[] emptyArgs = new Object[0];

              @Override
              public @Nullable Object invoke(Object proxy, Method method, @Nullable Object[] args)
                  throws Throwable {
                // If the method is a method from Object then defer to normal invocation.
                if (method.getDeclaringClass() == Object.class) {
                //如果外部调用的是 Object 中声明的方法的话则直接调用
                //例如 toString()、hashCode() 等方法
                  return method.invoke(this, args);
                }
                args = args != null ? args : emptyArgs;
                //根据 method 是否默认方法来决定如何调用
                return platform.isDefaultMethod(method)
                    ? platform.invokeDefaultMethod(method, service, proxy, args)
                    : loadServiceMethod(method).invoke(args);
              }
            });
  }
```

这里的重点就是`Proxy.newProxyInstance`所实现的**动态代理模式**了。通过动态代理，Retrofit 会将我们对 ApiService 的调用操作转发给 InvocationHandler 来完成。Retrofit 在后续会通过反射拿到我们在声明 `getUserData()`时标注的各个配置项，例如 **API 路径、请求方式、请求参数、返回值类型**等各个信息，然后将这些配置项拼接为 OkHttp 的一个网络请求。当我们调用了`call.enqueue`方法时，这个操作就会触发 InvocationHandler 去发起 OkHttp 网络请求了

Retrofit 会根据 method 是否是**默认方法**来决定如何调用，这里主要看`loadServiceMethod(method)`方法，该方法的主要逻辑是：

1. 将每个代表接口方法的 method 对象转换为 ServiceMethod 对象，该对象中就包含了接口方法的具体信息
2. 因为单个接口方法可能会先后被调用多次，所以将构造出来的 ServiceMethod 对象缓存到 serviceMethodCache 中以实现复用

```java
private final Map<Method, ServiceMethod<?>> serviceMethodCache = new ConcurrentHashMap<>();  

ServiceMethod<?> loadServiceMethod(Method method) {
	ServiceMethod<?> result = serviceMethodCache.get(method);
	if (result != null) return result;
	synchronized (serviceMethodCache) {
  		result = serviceMethodCache.get(method);
  		if (result == null) {
    		//重点
    		result = ServiceMethod.parseAnnotations(this, method);
    		serviceMethodCache.put(method, result);
  		}
	}
	return result;
}
```

# 四、ServiceMethod

从上面可知，`loadServiceMethod(method)`方法返回的是一个 ServiceMethod 对象，从名字可以猜出来每个 ServiceMethod 对象就对应一个接口方法，其内部就包含了对接口方法的解析结果。`loadServiceMethod(method).invoke(args)` 这个操作就对应**调用接口方法并传递网络请求参数**这个过程，即对应`service.getUserData()` 这个过程

ServiceMethod 是一个抽象类，仅包含一个抽象的 `invoke(Object[] args)`方法。ServiceMethod 使用到了**工厂模式**，由于网络请求最终的请求方式可能是多样化的，既可能是通过线程池来执行，也可能是通过 Kotlin 协程来执行，使用工厂模式的意义就在于可以将这种差异都隐藏在不同的 ServiceMethod 实现类中，而外部统一都是通过 `parseAnnotations` 方法来获取 ServiceMethod 的实现类

`parseAnnotations`方法返回的 ServiceMethod 实际上是 HttpServiceMethod，所以重点就要来看 `HttpServiceMethod.parseAnnotations`方法返回的 HttpServiceMethod 具体是如何实现的，并是如何拼接出一个完整的 OkHttp 请求调用链

```java
abstract class ServiceMethod<T> {
    
  static <T> ServiceMethod<T> parseAnnotations(Retrofit retrofit, Method method) {
    //requestFactory 包含了对 API 的注解信息进行解析后的结果
    RequestFactory requestFactory = RequestFactory.parseAnnotations(retrofit, method);

    Type returnType = method.getGenericReturnType();
    //如果返回值包含未确定的泛型类型或者是包含通配符的话，那么就抛出异常
    //因为 Retrofit 无法构造出一个不具有确定类型的对象作为返回值
    if (Utils.hasUnresolvableType(returnType)) {
      throw methodError(
          method,
          "Method return type must not include a type variable or wildcard: %s",
          returnType);
    }
    //返回值类型不能是 void
    if (returnType == void.class) {
      throw methodError(method, "Service methods cannot return void.");
    }
	
    //重点
    return HttpServiceMethod.parseAnnotations(retrofit, method, requestFactory);
  }

  abstract @Nullable T invoke(Object[] args);
    
}
```

# 五、HttpServiceMethod

ServiceMethod 这个抽象类的直接子类只有一个，即 HttpServiceMethod。HttpServiceMethod 也是一个抽象类，其包含两个泛型声明，ResponseT 表示的是**接口方法返回值的外层包装类型**，ReturnT 表示的是我们**实际需要的数据类型**。例如，对于 `fun getUserData(): Call<UserBean>` 方法，ResponseT 对应的是 Call，ReturnT 对应的是 UserBean

HttpServiceMethod 实现了父类的 `invoke` 方法，并将操作转交给了另一个抽象方法 `adapt` 来完成。可以看到，即使我们为接口方法声明的返回值类型是 `Observable<UserBean>`，`invoke` 方法内部其实还是需要创建出一个 Call 对象的，HttpServiceMethod 只是把 Call 转换为 Observable 的这个过程交由了 `adapt` 方法来完成

```java
abstract class HttpServiceMethod<ResponseT, ReturnT> extends ServiceMethod<ReturnT> {
 
  @Override
  final @Nullable ReturnT invoke(Object[] args) {
    Call<ResponseT> call = new OkHttpCall<>(requestFactory, args, callFactory, responseConverter);
    return adapt(call, args);
  }

  protected abstract @Nullable ReturnT adapt(Call<ResponseT> call, Object[] args);
    
  ···

}
```

再来看`HttpServiceMethod.parseAnnotations()`方法是如何构建出一个 HttpServiceMethod 对象的，并且该对象的`adapt`方法是如何实现的

```java
abstract class HttpServiceMethod<ResponseT, ReturnT> extends ServiceMethod<ReturnT> {

  static <ResponseT, ReturnT> HttpServiceMethod<ResponseT, ReturnT> parseAnnotations(
      Retrofit retrofit, Method method, RequestFactory requestFactory) {
    //是否是 Suspend 函数，即是否以 Kotlin 协程的方式来进行请求
    boolean isKotlinSuspendFunction = requestFactory.isKotlinSuspendFunction;
    boolean continuationWantsResponse = false;
    boolean continuationBodyNullable = false;

    Annotation[] annotations = method.getAnnotations();
    Type adapterType;
    if (isKotlinSuspendFunction) {
        //省略 Kotlin 协程的一些处理逻辑
    } else {
      adapterType = method.getGenericReturnType();
    }
	
    //重点1
    CallAdapter<ResponseT, ReturnT> callAdapter =
        createCallAdapter(retrofit, method, adapterType, annotations);
      
    //拿到包装类内部的具体类型，例如，Observable<UserBean> 内部的 UserBean
    //responseType 不能是 okhttp3.Response 或者是不包含具体泛型类型的 Response
    Type responseType = callAdapter.responseType();
    if (responseType == okhttp3.Response.class) {
      throw methodError(
          method,
          "'"
              + getRawType(responseType).getName()
              + "' is not a valid response body type. Did you mean ResponseBody?");
    }
    if (responseType == Response.class) {
      throw methodError(method, "Response must include generic type (e.g., Response<String>)");
    }
    // TODO support Unit for Kotlin?
    if (requestFactory.httpMethod.equals("HEAD") && !Void.class.equals(responseType)) {
      throw methodError(method, "HEAD method must use Void as response type.");
    }
	
    //重点2
    Converter<ResponseBody, ResponseT> responseConverter =
        createResponseConverter(retrofit, method, responseType);

    okhttp3.Call.Factory callFactory = retrofit.callFactory;
    if (!isKotlinSuspendFunction) {
      //重点3
      return new CallAdapted<>(requestFactory, callFactory, responseConverter, callAdapter);
    } 
    
    //省略 Kotlin 协程的一些处理逻辑
	···
        
  }

  ···
    
}
```

Retrofit 目前已经支持以 Kotlin 协程的方式来进行调用了，但本例子和协程无关，所以此处先忽略协程相关的处理逻辑，后面会再讲解，`parseAnnotations` 方法的主要逻辑是：

1. 先通过`createCallAdapter(retrofit, method, adapterType, annotations` 方法拿到 CallAdapter 对象，CallAdapter 就用于实现接口方法的**返回值包装类**处理逻辑。例如，`getUserData()`方法的返回值**包装类**类型如果是 `Call` ，那么返回的 CallAdapter 对象就对应 DefaultCallAdapterFactory 包含的 Adapter；如果是 Observable，那么返回的就是 RxJava2CallAdapterFactory 包含的 Adapter
2. 再通过 `createResponseConverter(retrofit, method, responseType)`方法拿到 Converter 对象，Converter 就用于实现接口方法的**返回值**处理逻辑。例如，`getUserData()`方法的目标返回值类型如果是 ResponseBody，那么 Converter 对象就对应 BuiltInConverters；如果是 UserBean，那么就对应 GsonConverterFactory
3. 根据前两个步骤拿到的值，构造出一个 CallAdapted 对象并返回

CallAdapted 正是 HttpServiceMethod 的子类，在以上步骤中已经找到了可以实现将 Call 转换为 Observable 的 CallAdapter 了，所以对于 CallAdapted 来说，其 `adapt` 方法会直接将 Call 提交给 CallAdapter，由其去实现这种转换过程

```java
abstract class HttpServiceMethod<ResponseT, ReturnT> extends ServiceMethod<ReturnT> {
    
    @Override
  	final @Nullable ReturnT invoke(Object[] args) {
    	Call<ResponseT> call = new OkHttpCall<>(requestFactory, args, callFactory, responseConverter);
    	return adapt(call, args);
  	}
    
}

static final class CallAdapted<ResponseT, ReturnT> extends HttpServiceMethod<ResponseT, ReturnT> {
    private final CallAdapter<ResponseT, ReturnT> callAdapter;

    CallAdapted(
        RequestFactory requestFactory,
        okhttp3.Call.Factory callFactory,
        Converter<ResponseBody, ResponseT> responseConverter,
        CallAdapter<ResponseT, ReturnT> callAdapter) {
      super(requestFactory, callFactory, responseConverter);
      this.callAdapter = callAdapter;
    }

    @Override
    protected ReturnT adapt(Call<ResponseT> call, Object[] args) {
      return callAdapter.adapt(call);
    }
  }
```

# 六、OkHttpCall

OkHttpCall 是实际发起 OkHttp 请求的地方。当我们调用 `fun getUserData(): Call<ResponseBody>` 方法时，返回的 Call 对象实际上是 OkHttpCall 类型，而当我们调用 `call.enqueue(Callback)`方法时，`enqueue` 方法就会发起一个 OkHttp 请求，传入的 `retrofit2.Callback` 对象就会由 `okhttp3.Callback`本身收到回调时再进行中转调用

```java
final class OkHttpCall<T> implements Call<T> {
  private final RequestFactory requestFactory;
  private final Object[] args;
  private final okhttp3.Call.Factory callFactory;
  private final Converter<ResponseBody, T> responseConverter;

  private volatile boolean canceled;

  @GuardedBy("this")
  private @Nullable okhttp3.Call rawCall;

  @GuardedBy("this") // Either a RuntimeException, non-fatal Error, or IOException.
  private @Nullable Throwable creationFailure;

  @GuardedBy("this")
  private boolean executed;
    
  @Override
  public void enqueue(final Callback<T> callback) { 
    ···
    okhttp3.Call call;
    ··· 
    call.enqueue( new okhttp3.Callback() {
          @Override
          public void onResponse(okhttp3.Call call, okhttp3.Response rawResponse) {
            Response<T> response;
            try {
              response = parseResponse(rawResponse);
            } catch (Throwable e) {
              throwIfFatal(e);
              callFailure(e);
              return;
            }

            try {
              callback.onResponse(OkHttpCall.this, response);
            } catch (Throwable t) {
              throwIfFatal(t);
              t.printStackTrace(); // TODO this is not great
            }
          }

          @Override
          public void onFailure(okhttp3.Call call, IOException e) {
            callFailure(e);
          }

          private void callFailure(Throwable e) {
            try {
              callback.onFailure(OkHttpCall.this, e);
            } catch (Throwable t) {
              throwIfFatal(t);
              t.printStackTrace(); // TODO this is not great
            }
          }
        });
  }
 
   ···
    
}
```

# 七、做个总结

以上几个小节的内容讲了在发起如下请求的过程中涉及到的所有流程，但单纯这样看的话其实有点难把握各个小点，我自己看着都有点绕，所以这里就再来回顾下以上内容，把所有知识点给串联起来

- 首先，我们通过 `retrofit.create(ApiService::class.java)`得到一个 ApiService 的**动态实现类**，这是通过 Java 原生提供的`Proxy.newProxyInstance` 代表的动态代理功能来实现的。在拿到 ApiService 的实现类后，我们就可以直接调用 ApiService 中声明的所有方法了
- 当我们调用了`service.getUserData()`方法时，Retrofit 会将每一个接口方法都抽象封装为一个 ServiceMethod 对象并缓存起来，我们的操作会转交给 ServiceMethod 来完成，由 ServiceMethod 来负责返回我们的目标类型，对应的是 `serviceMethod.invoke(Object[] args)`方法，args 代表的是我们调用接口方法时需要传递的参数，对应本例子就是一个空数组
- ServiceMethod 使用到了**工厂模式**，由于网络请求最终的请求方式可能是多样化的，既可能是通过线程池来执行，也可能是通过 Kotlin 协程来执行，使用工厂模式的意义就在于可以将这种差异都隐藏在不同的 ServiceMethod 实现类中，而外部统一都是通过 `parseAnnotations` 方法来获取 ServiceMethod 的实现类
- ServiceMethod 具有一个唯一的直接子类，即 HttpServiceMethod。HttpServiceMethod 自身已经找到了可以将 Call 转换为 Observable，ResponseBody 转换为 UserBean 的转换器，其`invoke`方法会构建出一个 OkHttpCall 对象，然后转发给抽象方法`adapt`，由`adapt`来发起实际的网络请求
- 而不管外部的接口方法返回值类型是不是 `Observable<UserBean>`，最终的网络请求都是需要通过 OkHttpCall 来发起，HttpServiceMethod 依靠找到的转换器将 OkHttpCall 给隐藏在了内部

# 八、接口方法是如何解析的？

Retrofit 是如何将 interface 内部的方法转化为一个个实际的 GET、POST、DELETE 等各式各样的网络请求的呢？例如，Retrofit 是如何将 getUserData() 转换为一个 OkHttp 的 GET 请求的呢？

这个过程在 ServiceMethod 的 `parseAnnotations` 方法中就已经完成的了，对应的是 `RequestFactory.parseAnnotations(retrofit, method)`方法

```java
abstract class ServiceMethod<T> {
  static <T> ServiceMethod<T> parseAnnotations(Retrofit retrofit, Method method) {
    //重点
    RequestFactory requestFactory = RequestFactory.parseAnnotations(retrofit, method);
    ···
    return HttpServiceMethod.parseAnnotations(retrofit, method, requestFactory);
  }

  abstract @Nullable T invoke(Object[] args);
}
```

前文说了，Retrofit 是建立在 OkHttp 之上的一个网络请求封装库，内部依靠 OkHttp 来完成实际的网络请求。而 OkHttp 的一般请求方式如下所示

```kotlin
fun run(url: String): String {
    val request: Request = Request.Builder()
        .url(url)
        .build()
    OkHttpClient().newCall(request).execute().use { response ->
        return response.body!!.string()
    }
}
```

OkHttp 需要构建一个 Request 对象来配置请求方式和请求参数，以此来发起网络请求。所以，Retrofit 也需要一个构建 Request 对象的过程，这个过程就隐藏在 RequestFactory 中

RequestFactory 采用了 Builder 模式，这里无需过多理会其构建过程，我们只要知道 RequestFactory 中包含了对 API 方法的各项解析结果即可。其 `create(Object[] args)`方法就会根据各项解析结果，最终返回一个 `okhttp3.Request` 对象

```java
final class RequestFactory {
  static RequestFactory parseAnnotations(Retrofit retrofit, Method method) {
    return new Builder(retrofit, method).build();
  }

  private final Method method;
  private final HttpUrl baseUrl;
  final String httpMethod;
  private final @Nullable String relativeUrl;
  private final @Nullable Headers headers;
  private final @Nullable MediaType contentType;
  private final boolean hasBody;
  private final boolean isFormEncoded;
  private final boolean isMultipart;
  private final ParameterHandler<?>[] parameterHandlers;
  final boolean isKotlinSuspendFunction;

  okhttp3.Request create(Object[] args) throws IOException {
    @SuppressWarnings("unchecked") // It is an error to invoke a method with the wrong arg types.
    ParameterHandler<Object>[] handlers = (ParameterHandler<Object>[]) parameterHandlers;

    int argumentCount = args.length;
    if (argumentCount != handlers.length) {
      throw new IllegalArgumentException(
          "Argument count ("
              + argumentCount
              + ") doesn't match expected count ("
              + handlers.length
              + ")");
    }

    RequestBuilder requestBuilder =
        new RequestBuilder(
            httpMethod,
            baseUrl,
            relativeUrl,
            headers,
            contentType,
            hasBody,
            isFormEncoded,
            isMultipart);

    if (isKotlinSuspendFunction) {
      // The Continuation is the last parameter and the handlers array contains null at that index.
      argumentCount--;
    }

    List<Object> argumentList = new ArrayList<>(argumentCount);
    for (int p = 0; p < argumentCount; p++) {
      argumentList.add(args[p]);
      handlers[p].apply(requestBuilder, args[p]);
    }

    return requestBuilder.get().tag(Invocation.class, new Invocation(method, argumentList)).build();
  }
    
}
```

我们现在知道，OkHttpCall 是实际上发起网络请求的地方，所以最终 RequestFactory 的 `create` 方法会由 OkHttpCall 的 `createRawCall()` 方法来调用

```java
final class OkHttpCall<T> implements Call<T> {
     
  	private okhttp3.Call createRawCall() throws IOException {
    	okhttp3.Call call = callFactory.newCall(requestFactory.create(args));
    	if (call == null) {
      		throw new NullPointerException("Call.Factory returned null.");
    	}
    	return call;
  	}
     
}
```

# 九、ResponseBody 如何映射为 UserBean

Retrofit 是如何将 API 的返回值映射为具体的 Bean 对象的呢？例如，ResponseBody 是如何映射为 UserBean 的呢？

OkHttp 默认的接口返回值对象是 ResponseBody，如果不引入`converter-gson`，我们只能将接口请求结果都定义为 ResponseBody，而不能是具体的 Bean 对象，因为 Retrofit 无法自动地完成 ResponseBody 到 UserBean 之间的转换操作，需要我们将这种转换规则告知 Retrofit。这种转换规则被 Retrofit 定义为 Converter 接口，对应它的 `responseBodyConverter`方法

```java
public interface Converter<F, T> {
    
  @Nullable
  T convert(F value) throws IOException;

  abstract class Factory {
	
    //将 ResponseBody 转换为目标类型 type
    public @Nullable Converter<ResponseBody, ?> responseBodyConverter(
        Type type, Annotation[] annotations, Retrofit retrofit) {
      return null;
    }

    ···
  }
}
```

为了能直接获取到 UserBean 对象，我们需要在构建 Retrofit 对象的时候添加 GsonConverterFactory。GsonConverterFactory 会根据目标类型 type，通过 Gson 来进行反序列化出 UserBean 对象

```java
public final class GsonConverterFactory extends Converter.Factory {

  @Override
  public Converter<ResponseBody, ?> responseBodyConverter(Type type, Annotation[] annotations,
      Retrofit retrofit) {
    TypeAdapter<?> adapter = gson.getAdapter(TypeToken.get(type));
    return new GsonResponseBodyConverter<>(gson, adapter);
  }
    
  ···
      
}

final class GsonResponseBodyConverter<T> implements Converter<ResponseBody, T> {
  private final Gson gson;
  private final TypeAdapter<T> adapter;

  GsonResponseBodyConverter(Gson gson, TypeAdapter<T> adapter) {
    this.gson = gson;
    this.adapter = adapter;
  }

  @Override public T convert(ResponseBody value) throws IOException {
    JsonReader jsonReader = gson.newJsonReader(value.charStream());
    try {
      T result = adapter.read(jsonReader);
      if (jsonReader.peek() != JsonToken.END_DOCUMENT) {
        throw new JsonIOException("JSON document was not fully consumed.");
      }
      return result;
    } finally {
      value.close();
    }
  }
}
```

那么，问题又来了，Retrofit 是如何知道什么类型才可以交由 GsonConverterFactory 来进行处理的呢？至少 ResponseBody 就不应该交由 GsonConverterFactory 来处理，Retrofit 如何进行选择呢？

首先，当我们在构建 Retrofit 对象时传入了 GsonConverterFactory，最终 Retrofit 会对所有 Converter.Factory 进行排序，converterFactories 中 BuiltInConverters 会被默认排在第一位，BuiltInConverters 是 Retrofit 自带的对 ResponseBody 进行默认解析的 Converter.Factory 实现类

```java
public final class Retrofit {
    
   public static final class Builder {
          public Retrofit build() {
      
      ···
              
      // Make a defensive copy of the converters.
      List<Converter.Factory> converterFactories =
          new ArrayList<>(
              1 + this.converterFactories.size() + platform.defaultConverterFactoriesSize());

      // Add the built-in converter factory first. This prevents overriding its behavior but also
      // ensures correct behavior when using converters that consume all types.
      converterFactories.add(new BuiltInConverters());
      converterFactories.addAll(this.converterFactories);
      converterFactories.addAll(platform.defaultConverterFactories());

      ···
   }
   
}
```

而 BuiltInConverters 的 `responseBodyConverter` 方法在目标类型并非 ResponseBody、Void、Unit 等三种类型的情况下会返回 null 

```java
final class BuiltInConverters extends Converter.Factory {
    
@Override
public @Nullable Converter<ResponseBody, ?> responseBodyConverter(
      Type type, Annotation[] annotations, Retrofit retrofit) {
    if (type == ResponseBody.class) {
      return Utils.isAnnotationPresent(annotations, Streaming.class)
          ? StreamingResponseBodyConverter.INSTANCE
          : BufferingResponseBodyConverter.INSTANCE;
    }
    if (type == Void.class) {
      return VoidResponseBodyConverter.INSTANCE;
    }
    if (checkForKotlinUnit) {
      try {
        if (type == Unit.class) {
          return UnitResponseBodyConverter.INSTANCE;
        }
      } catch (NoClassDefFoundError ignored) {
        checkForKotlinUnit = false;
      }
    }
    return null;
  }

  ···

}
```

而 Retrofit 类的 `nextResponseBodyConverter` 方法就是为每一个接口方法选择 Converter 进行返回值数据类型转换的方法。该方法会先遍历到 BuiltInConverters，发现其返回了 null，就会最终选择到 GsonResponseBodyConverter，从而完成数据解析。如果最终没有找到一个合适的处理器的话，就会抛出 IllegalArgumentException

```java
public <T> Converter<ResponseBody, T> nextResponseBodyConverter(
      @Nullable Converter.Factory skipPast, Type type, Annotation[] annotations) {
    Objects.requireNonNull(type, "type == null");
    Objects.requireNonNull(annotations, "annotations == null");

    int start = converterFactories.indexOf(skipPast) + 1;
    for (int i = start, count = converterFactories.size(); i < count; i++) {
      Converter<ResponseBody, ?> converter =
          converterFactories.get(i).responseBodyConverter(type, annotations, this);
      if (converter != null) {
        //noinspection unchecked
        return (Converter<ResponseBody, T>) converter;
      }
    }
	···
    throw new IllegalArgumentException(builder.toString());
}
```

# 十、Call 如何替换为 Observable

Retrofit 是如何抽象不同的接口返回值包装类的呢？例如，Call 是如何替换为 Observable 的？

与上一节内容相类似，Retrofit 在默认情况下也只支持将 `retrofit2.Call` 作为接口方法的返回值包装类，为了支持返回 `Observable` 类型，我们需要在构建 Retrofit 的时候添加 RxJava2CallAdapterFactory

Retrofit 将`retrofit2.Call`转换为`Observable`的这种规则抽象为了 CallAdapter 接口

```java
public interface CallAdapter<R, T> {

  //返回具体的内部类型，即 UserBean
  Type responseType();

  //用于将 Call 转换为 Observable
  T adapt(Call<R> call);

  abstract class Factory {

    //用于提供将 Call<UserBean> 转换为 Observable<UserBean> 的 CallAdapter 对象
    //此处的 returnType 即 Observable<UserBean>
    //如果此 CallAdapter 无法完成这种数据类型的转换，那么就返回 null
    public abstract @Nullable CallAdapter<?, ?> get(
        Type returnType, Annotation[] annotations, Retrofit retrofit);

    ···
        
  }
}
```

对于 RxJava2CallAdapterFactory 的 `get` 方法而言，如何返回值类型并非 Completable、Flowable、Single、Maybe 等类型的话就会返回 null，否则就返回 RxJava2CallAdapter 对象

```java
public final class RxJava2CallAdapterFactory extends CallAdapter.Factory {
    
  ···
  
  @Override
  public @Nullable CallAdapter<?, ?> get(Type returnType, Annotation[] annotations, Retrofit retrofit) {
    
        Class<?> rawType = getRawType(returnType);
    	if (rawType == Completable.class) {
      		// Completable is not parameterized (which is what the rest of this method deals with) so it
      		// can only be created with a single configuration.
      		return new RxJava2CallAdapter(
          	Void.class, scheduler, isAsync, false, true, false, false, false, true);
    	}

    	boolean isFlowable = rawType == Flowable.class;
    	boolean isSingle = rawType == Single.class;
    	boolean isMaybe = rawType == Maybe.class;
    	if (rawType != Observable.class && !isFlowable && !isSingle && !isMaybe) {
      		return null;
    	}
    	···
      
    	return new RxJava2CallAdapter(responseType, scheduler, isAsync, isResult, isBody, isFlowable, isSingle, isMaybe, false);
  }
}
```

对于本例子而言，最终 RxJava2CallAdapter 又会返回 CallExecuteObservable，CallExecuteObservable 又会在外部进行 subscribe 的时候调用 `call.execute()` 方法来发起网络请求，所以在上面的例子中我们并不需要显式地发起网络请求，而是在进行 subscribe 的时候就自动触发请求了，Observer 只需要等待网络请求结果自动回调出来即可

```java
final class RxJava2CallAdapter<R> implements CallAdapter<R, Object> {
  
  ···
        
  @Override
  public Type responseType() {
    return responseType;
  }

  @Override
  public Object adapt(Call<R> call) {
    Observable<Response<R>> responseObservable =
        isAsync ? new CallEnqueueObservable<>(call) : new CallExecuteObservable<>(call);

    ···
    return RxJavaPlugins.onAssembly(observable);
  }
}

final class CallExecuteObservable<T> extends Observable<Response<T>> {
  private final Call<T> originalCall;

  CallExecuteObservable(Call<T> originalCall) {
    this.originalCall = originalCall;
  }

  @Override
  protected void subscribeActual(Observer<? super Response<T>> observer) {
    // Since Call is a one-shot type, clone it for each new observer.
    Call<T> call = originalCall.clone();
    CallDisposable disposable = new CallDisposable(call);
    observer.onSubscribe(disposable);
    if (disposable.isDisposed()) {
      return;
    }

    boolean terminated = false;
    try {
      //发起网络请求
      Response<T> response = call.execute();
      if (!disposable.isDisposed()) {
        //将请求结果传给外部
        observer.onNext(response);
      }
      if (!disposable.isDisposed()) {
        terminated = true;
        observer.onComplete();
      }
    } catch (Throwable t) {
      Exceptions.throwIfFatal(t);
      if (terminated) {
        RxJavaPlugins.onError(t);
      } else if (!disposable.isDisposed()) {
        try {
          observer.onError(t);
        } catch (Throwable inner) {
          Exceptions.throwIfFatal(inner);
          RxJavaPlugins.onError(new CompositeException(t, inner));
        }
      }
    }
  }
	
  ···

}
```

那么，问题又来了，Retrofit 是如何知道什么类型才可以交由 RxJava2CallAdapterFactory 来进行处理的呢？

首先，当我们在构建 Retrofit 对象时传入了 RxJava2CallAdapterFactory，最终 Retrofit 会按照添加顺序对所有 CallAdapter.Factory 进行保存，且默认会在队尾添加一个 DefaultCallAdapterFactory，用于对包装类型为 `retrofit2.Call`的情况进行解析

而 Retrofit 类的 `nextCallAdapter` 方法就是为每一个 API 方法选择 CallAdapter 进行返回值数据类型转换的方法。该方法会先遍历到 RxJava2CallAdapter ，发现其返回了非 null 值，之后就交由其进行处理

```java
public CallAdapter<?, ?> nextCallAdapter(
      @Nullable CallAdapter.Factory skipPast, Type returnType, Annotation[] annotations) {
    Objects.requireNonNull(returnType, "returnType == null");
    Objects.requireNonNull(annotations, "annotations == null");

    int start = callAdapterFactories.indexOf(skipPast) + 1;
    for (int i = start, count = callAdapterFactories.size(); i < count; i++) {
      CallAdapter<?, ?> adapter = callAdapterFactories.get(i).get(returnType, annotations, this);
      if (adapter != null) {
        return adapter;
      }
    }

    ···
        
    throw new IllegalArgumentException(builder.toString());
  }
```

# 十一、再总结下

这里再来总结下上面两节关于 Retrofit 整个数据转换的流程的内容

在默认情况下，我们从回调 Callback 中取到的最原始的返回值类型是 `Response<ResponseBody>`，而在引入了 `converter-gson` 和`adapter-rxjava2` 之后，我们可以直接拿到目标类型 UserBean

Retrofit 为了达到这种转换效果，就要先后进行两个步骤：

1. 将 ResponseBody 转换为 UserBean，从而可以得到接口方法返回值 `Response<UserBean>`
2. 将 Call 转换为 Observable，Observable 直接从 `Response<UserBean>` 中把 UserBean 取出来作为返回值来返回，从而直接得到目标类型 UserBean

第一个步骤即第九节所讲的内容，ResponseBody 转为 UserBean 的转换规则是通过 Converter 接口来定义的

```java
public interface Converter<F, T> {
   
  //用于将 F 类型转换为 T 类型
  @Nullable
  T convert(F value) throws IOException;

  ···
      
}
```

这个过程的转换就发生在 OkHttpCall 中，`enqueue` 方法在拿到 OkHttp 返回的 `okhttp3.Response` 对象后，就通过调用 `parseResponse`方法来完成转化为 `Response<T>`的逻辑，当中就调用了 Converter 接口的 `convert` 方法，从而得到返回值 `Response<UserBean>`

```java
  final class OkHttpCall<T> implements Call<T> {
 
  @Override
  public void enqueue(final Callback<T> callback) {
    call.enqueue(
        new okhttp3.Callback() {
          @Override
          public void onResponse(okhttp3.Call call, okhttp3.Response rawResponse) {
            Response<T> response;
            try {
              //重点
              response = parseResponse(rawResponse);
            } catch (Throwable e) {
              throwIfFatal(e);
              callFailure(e);
              return;
            }

            try {
              callback.onResponse(OkHttpCall.this, response);
            } catch (Throwable t) {
              throwIfFatal(t);
              t.printStackTrace(); // TODO this is not great
            }
          }
        });
  }
  
 private final Converter<ResponseBody, T> responseConverter;
    
 Response<T> parseResponse(okhttp3.Response rawResponse) throws IOException {
    ···
    ExceptionCatchingResponseBody catchingBody = new ExceptionCatchingResponseBody(rawBody);
    try {
      //catchingBody 就是 ResponseBody 类型，将其转换为 T 类型
      T body = responseConverter.convert(catchingBody);
      //然后再将其包装为 Response<T> 类型
      return Response.success(body, rawResponse);
    } catch (RuntimeException e) {
      // If the underlying source threw an exception, propagate that rather than indicating it was
      // a runtime exception.
      catchingBody.throwIfCaught();
      throw e;
    }
  }
    
}
```

第二个步骤即第十节所讲的内容， Call 转换为 Observable 的转换规则是通过 CallAdapter 接口来定义的

```java
public interface CallAdapter<R, T> {

  Type responseType();

  //此方法就用于将 Call<R> 转为你希望的目标类型 T，例如：Observable<UserBean>
  T adapt(Call<R> call);

  ··· 
   
}
```

在 CallEnqueueObservable 这个类中，通过自定义回调接口 CallCallback 来发起网络请求，从而拿到在第一个步骤解析完成后的数据，即 `Response<UserBean>` 对象

```java
final class CallEnqueueObservable<T> extends Observable<Response<T>> {
  private final Call<T> originalCall;

  CallEnqueueObservable(Call<T> originalCall) {
    this.originalCall = originalCall;
  }

  @Override
  protected void subscribeActual(Observer<? super Response<T>> observer) {
    // Since Call is a one-shot type, clone it for each new observer.
    Call<T> call = originalCall.clone();
    CallCallback<T> callback = new CallCallback<>(call, observer);
    observer.onSubscribe(callback);
    if (!callback.isDisposed()) {
      //自定义回调，发起请求
      call.enqueue(callback);
    }
  }

  private static final class CallCallback<T> implements Disposable, Callback<T> {
    private final Call<?> call;
    private final Observer<? super Response<T>> observer;
    private volatile boolean disposed;
    boolean terminated = false;

    CallCallback(Call<?> call, Observer<? super Response<T>> observer) {
      this.call = call;
      this.observer = observer;
    }

    @Override
    public void onResponse(Call<T> call, Response<T> response) {
        ···
        //直接将Response<T>传递出去，即 Response<UserBean> 对象
     	observer.onNext(response);
        ···
    }

    ···
  }
}
```

CallCallback 类同时持有着一个 observer 对象，该 observer 对象实际上又属于 BodyObservable 类。BodyObservable 在拿到了 `Response<UserBean>` 对象后，如果判断到此次网络请求属于成功状态的话，那么就直接取出 body （即 UserBean）传递出去。因此我们才可以直接拿到目标类型，而不包含任何包装类

```java
final class BodyObservable<T> extends Observable<T> {
  private final Observable<Response<T>> upstream;

  BodyObservable(Observable<Response<T>> upstream) {
    this.upstream = upstream;
  }

  @Override
  protected void subscribeActual(Observer<? super T> observer) {
    upstream.subscribe(new BodyObserver<T>(observer));
  }

  private static class BodyObserver<R> implements Observer<Response<R>> {
    private final Observer<? super R> observer;
    private boolean terminated;

    BodyObserver(Observer<? super R> observer) {
      this.observer = observer;
    }

    @Override
    public void onSubscribe(Disposable disposable) {
      observer.onSubscribe(disposable);
    }

    @Override
    public void onNext(Response<R> response) {
      if (response.isSuccessful()) {
        //如果本次网络请求成功，那么就直接取出 body 传递出去
        observer.onNext(response.body());
      } else {
        terminated = true;
        Throwable t = new HttpException(response);
        try {
          observer.onError(t);
        } catch (Throwable inner) {
          Exceptions.throwIfFatal(inner);
          RxJavaPlugins.onError(new CompositeException(t, inner));
        }
      }
    }

    ···
  }
}

```

# 十二、使用 Kotlin 协程

Retrofit 的当前版本已经支持以 Kotlin 协程的方式来调用了，这里就来看下 Retrofit 是如何支持协程调用的

先导入所有需要使用到的依赖，因为本例子是纯 Kotlin 项目，所以就不导入 Android 平台的 Kotlin 协程支持库了

```groovy
dependencies {
    implementation 'com.squareup.retrofit2:retrofit:2.9.0'
    implementation 'com.squareup.retrofit2:converter-gson:2.5.0'
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.9'
}
```

本例子通过 runBlocking 来启动一个协程，避免网络请求还未结束 main 线程就停止了。**需要注意的是，在实际开发中应该避免这样来使用协程，否则使用协程就没有多少意义了**

```kotlin
/**
 * @Author: leavesCZY
 * @Github：https://github.com/leavesCZY
 */
interface ApiService {

    @GET("getUserData")
    suspend fun getUserData(): UserBean

}

data class UserBean(val userName: String, val userAge: Long)

fun main() {
    val retrofit = Retrofit.Builder()
        .baseUrl("https://mockapi.eolinker.com/9IiwI82f58c23411240ed608ceca204b2f185014507cbe3/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    val service = retrofit.create(ApiService::class.java)
    runBlocking {
        val job: Job = launch {
            try {
                val userBean: UserBean = service.getUserData()
                println("userBean: $userBean")
            } catch (e: Throwable) {
                println("onFailure: $e")
            }
        }
    }
}
```

在本例子中，`getUserData()`方法的返回值就不需要任何包装类了，我们直接声明目标数据类型就可以了，在使用上会比之前更加简洁方便

好了，开始来分析下流程

我们先为 ApiService 多声明几个方法，方便来分析规律。每个方法都使用 `suspend`关键字进行修饰，标明其只能用于在协程中来调用

```kotlin
interface ApiService {

    @GET("getUserData")
    fun getUserData(): UserBean

    @GET("getUserData")
    suspend fun getUserData1(): UserBean

    @GET("getUserData")
    suspend fun getUserData2(id: String): UserBean

    @GET("getUserData")
    suspend fun getUserData3(id: String, limit: Int): UserBean

}
```

Retrofit 是以 Java 语言实现的，但 suspend 关键字只能用于 Kotlin，两者就存在着“沟通障碍”，但只要调用方也属于 JVM 语言的话，那么按道理来说 Retrofit 就都是可以使用的，此处就通过 IDEA 将 ApiService 反编译为了以下的 Java 类，看下 suspend 函数在 Retrofit 的角度来看是怎么实现的

```java
public interface ApiService {
   @GET("getUserData")
   @NotNull
   UserBean getUserData();

   @GET("getUserData")
   @Nullable
   Object getUserData1(@NotNull Continuation var1);

   @GET("getUserData")
   @Nullable
   Object getUserData2(@NotNull String var1, @NotNull Continuation var2);

   @GET("getUserData")
   @Nullable
   Object getUserData3(@NotNull String var1, int var2, @NotNull Continuation var3);
}
```

可以看到，非 suspend 函数的转换结果还符合我们的心理预期，但是 suspend 函数就相差得比较大了，方法返回值类型都变为 Object，且在方法的参数列表的最后都被添加了一个 `kotlin.coroutines.Continuation` 参数。**这个参数是重点，后面会使用到**

在 RequestFactory 类中包含一个 `isKotlinSuspendFunction` 的布尔变量，就用来标记当前解析到的 Method 是否是 suspend 函数。在 RequestFactory 的 `build()`方法中，会对 API 方法的每一个参数进行解析，当中就包含了检测当前解析的参数是否是属于最后一个参数的逻辑

```java
RequestFactory build() {
      ···
      int parameterCount = parameterAnnotationsArray.length;
      parameterHandlers = new ParameterHandler<?>[parameterCount];
      for (int p = 0, lastParameter = parameterCount - 1; p < parameterCount; p++) {
        parameterHandlers[p] =
            //p == lastParameter 如果为 true 就说明当前解析的 parameterTypes[p] 是 API 方法的最后一个参数
            parseParameter(p, parameterTypes[p], parameterAnnotationsArray[p], p == lastParameter);
      }
      ···
      return new RequestFactory(this);
}
```

如果检测到最后一个参数类型就是 `Continuation.class`的话，那么 `isKotlinSuspendFunction` 就会变成 true。这个检测逻辑就符合了上面所介绍的 Kotlin 类型的 ApiService 代码转换为 Java 形式后的变化规则 

```java
private @Nullable ParameterHandler<?> parseParameter(
        int p, Type parameterType, @Nullable Annotation[] annotations, boolean allowContinuation) {
      ···
      if (result == null) {
        if (allowContinuation) {
          try {
            if (Utils.getRawType(parameterType) == Continuation.class) {
              isKotlinSuspendFunction = true;
              return null;
            }
          } catch (NoClassDefFoundError ignored) {
          }
        }
        throw parameterError(method, p, "No Retrofit annotation found.");
      }

      return result;
    }
```

然后，在 HttpServiceMethod 的 `parseAnnotations`方法中我们就会用到`isKotlinSuspendFunction`这个变量

```java
static <ResponseT, ReturnT> HttpServiceMethod<ResponseT, ReturnT> parseAnnotations(
      Retrofit retrofit, Method method, RequestFactory requestFactory) {
    
    boolean isKotlinSuspendFunction = requestFactory.isKotlinSuspendFunction;
    boolean continuationWantsResponse = false;
    boolean continuationBodyNullable = false;

    Annotation[] annotations = method.getAnnotations();
    Type adapterType;
    if (isKotlinSuspendFunction) {
      ···
      //虽然 getUserData() 方法我们直接定义返回类型为 UserBean
      //但实际上 Retrofit 还是需要将返回类型转为 Call<UserBean>，使之符合我们上述的数据解析流程
      //所以，此处的 responseType 为 UserBean，adapterType 确是 Call<UserBean>
      adapterType = new Utils.ParameterizedTypeImpl(null, Call.class, responseType);
      annotations = SkipCallbackExecutorImpl.ensurePresent(annotations);
    } else {
      adapterType = method.getGenericReturnType();
    }

    CallAdapter<ResponseT, ReturnT> callAdapter =
        createCallAdapter(retrofit, method, adapterType, annotations);
    Type responseType = callAdapter.responseType();
    
    ···
        
    //重点
      return (HttpServiceMethod<ResponseT, ReturnT>)
          new SuspendForBody<>(
              requestFactory,
              callFactory,
              responseConverter,
              (CallAdapter<ResponseT, Call<ResponseT>>) callAdapter,
              continuationBodyNullable);
  }
```

最终，对于本例子来说，`parseAnnotations` 方法最终的返回值是 SuspendForBody，它也是 HttpServiceMethod 的子类。其主要逻辑是：

1. 将接口方法的最后一个参数强转为 `Continuation<ResponseT>` 类型，这符合上述的分析
2. 因为 isNullable 固定为 false，所以最终会调用 `KotlinExtensions.await(call, continuation)` 这个 Kotlin 的扩展函数

```java
static final class SuspendForBody<ResponseT> extends HttpServiceMethod<ResponseT, Object> {
    private final CallAdapter<ResponseT, Call<ResponseT>> callAdapter;
    private final boolean isNullable;

    SuspendForBody(
        RequestFactory requestFactory,
        okhttp3.Call.Factory callFactory,
        Converter<ResponseBody, ResponseT> responseConverter,
        CallAdapter<ResponseT, Call<ResponseT>> callAdapter,
        boolean isNullable) {
      super(requestFactory, callFactory, responseConverter);
      this.callAdapter = callAdapter;
      this.isNullable = isNullable;
    }

    @Override
    protected Object adapt(Call<ResponseT> call, Object[] args) {
      call = callAdapter.adapt(call);
      //noinspection unchecked Checked by reflection inside RequestFactory.
      Continuation<ResponseT> continuation = (Continuation<ResponseT>) args[args.length - 1];
      try {
        return isNullable
            ? KotlinExtensions.awaitNullable(call, continuation)
            : KotlinExtensions.await(call, continuation);
      } catch (Exception e) {
        return KotlinExtensions.suspendAndThrow(e, continuation);
      }
    }
  }
```

`await()`方法就会以 `suspendCancellableCoroutine` 这个支持 cancel 的 CoroutineScope 作为作用域，依旧以 `Call.enqueue`的方式来发起 OkHttp 请求，拿到 responseBody 后就透传出来，至此就完成了整个调用流程了

```kotlin
suspend fun <T : Any> Call<T>.await(): T {
  return suspendCancellableCoroutine { continuation ->
    continuation.invokeOnCancellation {
      cancel()
    }
    enqueue(object : Callback<T> {
      override fun onResponse(call: Call<T>, response: Response<T>) {
        if (response.isSuccessful) {
          val body = response.body()
          if (body == null) {
            val invocation = call.request().tag(Invocation::class.java)!!
            val method = invocation.method()
            val e = KotlinNullPointerException("Response from " +
                method.declaringClass.name +
                '.' +
                method.name +
                " was null but response body type was declared as non-null")
            continuation.resumeWithException(e)
          } else {
            continuation.resume(body)
          }
        } else {
          continuation.resumeWithException(HttpException(response))
        }
      }

      override fun onFailure(call: Call<T>, t: Throwable) {
        continuation.resumeWithException(t)
      }
    })
  }
}
```

# 十三、Retrofit  &  Android

上文有讲到，Retrofit 的内部实现并不需要依赖于 Android 平台，而是可以用于任意的 Java 客户端，Retrofit 只是对 Android 平台进行了特殊实现而已。那么，Retrofit 具体是对 Android 平台做了什么特殊支持呢？

在构建 Retrofit 对象的时候，我们可以选择传递一个 Platform 对象用于标记调用方所处的平台

```java
public static final class Builder {
    private final Platform platform;
    private @Nullable okhttp3.Call.Factory callFactory;
    private @Nullable HttpUrl baseUrl;
    private final List<Converter.Factory> converterFactories = new ArrayList<>();
    private final List<CallAdapter.Factory> callAdapterFactories = new ArrayList<>();
    private @Nullable Executor callbackExecutor;
    private boolean validateEagerly;

    Builder(Platform platform) {
      this.platform = platform;
    }

    public Builder() {
      this(Platform.get());
    }

	···
}
```

Platform 类有两个作用：

1. 判断是否支持 Java 8。这在判断是否支持调用 interface 的默认方法，以及判断是否支持 Optional 和 CompletableFuture 时需要用到。因为 Android 应用如果想要支持 Java 8 的话，是需要在 Gradle 文件中进行主动配置的，且 Java 8 在 Android 平台上目前也支持得并不彻底，所以需要判断是否支持 Java 8 来决定是否启用特定功能
2. 实现 main 线程回调的 Executor。众所周知，Android 平台是不允许在 main 线程上执行耗时任务的，且 UI 操作都需要切换到 main 线程来完成。所以，对于 Android 平台来说，Retrofit 在回调网络请求结果时，都会通过 main 线程执行的 Executor 来进行线程切换

```java
class Platform {
  private static final Platform PLATFORM = findPlatform();

  static Platform get() {
    return PLATFORM;
  }

  private static Platform findPlatform() {
    //根据 JVM 名字来判断使用方是否是 Android 平台
    return "Dalvik".equals(System.getProperty("java.vm.name"))
        ? new Android() //
        : new Platform(true);
  }
  
  //是否支持 Java 8
  private final boolean hasJava8Types;
  private final @Nullable Constructor<Lookup> lookupConstructor;

  Platform(boolean hasJava8Types) {
    this.hasJava8Types = hasJava8Types;

    Constructor<Lookup> lookupConstructor = null;
    if (hasJava8Types) {
      try {
        // Because the service interface might not be public, we need to use a MethodHandle lookup
        // that ignores the visibility of the declaringClass.
        lookupConstructor = Lookup.class.getDeclaredConstructor(Class.class, int.class);
        lookupConstructor.setAccessible(true);
      } catch (NoClassDefFoundError ignored) {
        // Android API 24 or 25 where Lookup doesn't exist. Calling default methods on non-public
        // interfaces will fail, but there's nothing we can do about it.
      } catch (NoSuchMethodException ignored) {
        // Assume JDK 14+ which contains a fix that allows a regular lookup to succeed.
        // See https://bugs.openjdk.java.net/browse/JDK-8209005.
      }
    }
    this.lookupConstructor = lookupConstructor;
  }
    
  //获取默认的 Executor 实现，用于 Android 平台
  @Nullable
  Executor defaultCallbackExecutor() {
    return null;
  }

  List<? extends CallAdapter.Factory> defaultCallAdapterFactories(
      @Nullable Executor callbackExecutor) {
    DefaultCallAdapterFactory executorFactory = new DefaultCallAdapterFactory(callbackExecutor);
    return hasJava8Types
        ? asList(CompletableFutureCallAdapterFactory.INSTANCE, executorFactory)
        : singletonList(executorFactory);
  }

  int defaultCallAdapterFactoriesSize() {
    return hasJava8Types ? 2 : 1;
  }

  List<? extends Converter.Factory> defaultConverterFactories() {
    return hasJava8Types ? singletonList(OptionalConverterFactory.INSTANCE) : emptyList();
  }

  int defaultConverterFactoriesSize() {
    return hasJava8Types ? 1 : 0;
  }

  @IgnoreJRERequirement // Only called on API 24+.
  boolean isDefaultMethod(Method method) {
    return hasJava8Types && method.isDefault();
  }

  @IgnoreJRERequirement // Only called on API 26+.
  @Nullable
  Object invokeDefaultMethod(Method method, Class<?> declaringClass, Object object, Object... args)
      throws Throwable {
    Lookup lookup =
        lookupConstructor != null
            ? lookupConstructor.newInstance(declaringClass, -1 /* trusted */)
            : MethodHandles.lookup();
    return lookup.unreflectSpecial(method, declaringClass).bindTo(object).invokeWithArguments(args);
  }

}
```

Platform 类只具有一个唯一子类，即 Android 类。其主要逻辑就是重写了父类的 `defaultCallbackExecutor()`方法，通过 Handler 来实现在 main 线程执行特定的 Runnable，以此来实现网络请求结果都在 main 线程进行回调

```java
static final class Android extends Platform {
    Android() {
      super(Build.VERSION.SDK_INT >= 24);
    }

    @Override
    public Executor defaultCallbackExecutor() {
      return new MainThreadExecutor();
    }

    @Nullable
    @Override
    Object invokeDefaultMethod(
        Method method, Class<?> declaringClass, Object object, Object... args) throws Throwable {
      if (Build.VERSION.SDK_INT < 26) {
        throw new UnsupportedOperationException(
            "Calling default methods on API 24 and 25 is not supported");
      }
      return super.invokeDefaultMethod(method, declaringClass, object, args);
    }

    static final class MainThreadExecutor implements Executor {
      private final Handler handler = new Handler(Looper.getMainLooper());

      @Override
      public void execute(Runnable r) {
        handler.post(r);
      }
    }
  }
```

前文也有讲到，Retrofit 有个默认的 `CallAdapter.Factory`接口实现类，用于对接口方法返回值包装类型是 `Call` 的情形进行处理。DefaultCallAdapterFactory 会拿到 Platform 返回的 Executor 对象，如果 Executor 对象不为 null 且接口方法没有标注 SkipCallbackExecutor 注解的话，就使用该 Executor 对象作为一个代理来中转所有的回调操作，以此实现线程切换。这里使用到了**装饰器模式**

```java
final class DefaultCallAdapterFactory extends CallAdapter.Factory {
  private final @Nullable Executor callbackExecutor;

  DefaultCallAdapterFactory(@Nullable Executor callbackExecutor) {
    this.callbackExecutor = callbackExecutor;
  }

  @Override
  public @Nullable CallAdapter<?, ?> get(
      Type returnType, Annotation[] annotations, Retrofit retrofit) {
      
    ···

    final Executor executor =
        //判断 annotations 是否包含 SkipCallbackExecutor 注解
        //有的话说明希望直接在原来的线程进行方法调用，不需要进行线程切换
        Utils.isAnnotationPresent(annotations, SkipCallbackExecutor.class)
            ? null
            : callbackExecutor;

    return new CallAdapter<Object, Call<?>>() {
      @Override
      public Type responseType() {
        return responseType;
      }

      @Override
      public Call<Object> adapt(Call<Object> call) {
        //executor 不为 null 的话就将其作为一个中间代理
        //交由 ExecutorCallbackCall 来完成实际的回调操作
        return executor == null ? call : new ExecutorCallbackCall<>(executor, call);
      }
    };
  }

  static final class ExecutorCallbackCall<T> implements Call<T> {
    final Executor callbackExecutor;
    final Call<T> delegate;

    ExecutorCallbackCall(Executor callbackExecutor, Call<T> delegate) {
      this.callbackExecutor = callbackExecutor;
      this.delegate = delegate;
    }

    @Override
    public void enqueue(final Callback<T> callback) {
      Objects.requireNonNull(callback, "callback == null");

      delegate.enqueue(
          new Callback<T>() {
            @Override
            public void onResponse(Call<T> call, final Response<T> response) {
              callbackExecutor.execute(
                  () -> {
                    if (delegate.isCanceled()) {
                      // Emulate OkHttp's behavior of throwing/delivering an IOException on
                      // cancellation.
                      callback.onFailure(ExecutorCallbackCall.this, new IOException("Canceled"));
                    } else {
                      callback.onResponse(ExecutorCallbackCall.this, response);
                    }
                  });
            }

            @Override
            public void onFailure(Call<T> call, final Throwable t) {
              callbackExecutor.execute(() -> callback.onFailure(ExecutorCallbackCall.this, t));
            }
          });
    }

    ···
        
  }
}
```

# 十四、动态代理模式

在讲`retrofit.create`这一节内容的时候有提到**动态代理**。动态代理模式是 Retrofit 能够做到网络请求如此简洁方便的主要原因。有时候，对于某个既定的 interface，我们不希望直接声明并使用其实现类，而是希望实现类可以动态生成，并且提供实现 AOP 编程的机会，此时就可以通过 `Proxy.newProxyInstance`来实现这个目的

```kotlin
/**
 * @Author: leavesCZY
 * @Github：https://github.com/leavesCZY
 */
data class UserBean(val userName: String, val userAge: Long)

interface ApiService {

    fun getUserData(id: String): UserBean

}

fun main() {
    val apiService = ApiService::class.java
    var newProxyInstance = Proxy.newProxyInstance(
        apiService.classLoader,
        arrayOf<Class<*>>(apiService), object : InvocationHandler {
            override fun invoke(proxy: Any, method: Method, args: Array<out Any>?): Any {
                println("methodName: " + method.name)
                args?.forEach {
                    println("args: " + it)
                }
                return UserBean("leavesC", 26)
            }

        })
    newProxyInstance = newProxyInstance as ApiService
    val userBean = newProxyInstance.getUserData("100")
    println("userBean: $userBean")
}
```

在上述例子中，虽然我们并没有声明`ApiService`的任何实现类，但是在 `invoke`方法中我们拿到了`getUserData`方法所代表的 `method` 对象以及请求参数 `args`，最终外部也获得了返回值。这就是代理模式给我们带来的便利之处

```kotlin
methodName: getUserData
args: 100
userBean: UserBean(userName=leavesC, userAge=26)
```

# 十五、结尾

Retrofit 的源码就讲到这里了，自我感觉还是讲得挺全面的，虽然可能讲得没那么易于理解。从开始看源码到写完文章花了要十天出一些，断断续续地看源码，断断续续地写文章，总算写完了。觉得对你有所帮助就请点个赞吧 😂😂

下篇文章就再来写关于 Retrofit 的扩展知识吧 ~~