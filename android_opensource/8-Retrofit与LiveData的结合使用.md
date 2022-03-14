> 公众号：[字节数组](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/adbc507fc3704fd8955aae739a433db2~tplv-k3u1fbpfcp-zoom-1.image)
>
> 希望对你有所帮助 🤣🤣

> 对于 Android Developer 来说，很多开源库都是属于**开发必备**的知识点，从使用方式到实现原理再到源码解析，这些都需要我们有一定程度的了解和运用能力。所以我打算来写一系列关于开源库**源码解析**和**实战演练**的文章，初定的目标是 **EventBus、ARouter、LeakCanary、Retrofit、Glide、OkHttp、Coil** 等七个知名开源库，希望对你有所帮助 🤣🤣

在上篇文章中我讲解了 Retrofit 是如何实现支持不同的 API 返回值的。例如，对于同一个 API 接口，我们既可以使用 Retrofit 原生的 `Call<ResponseBody>`方式来作为返回值，也可以使用 `Observable<ResponseBody>`这种 RxJava 的方式来发起网络请求

```kotlin
/**
 * @Author: leavesCZY
 * @Github：https://github.com/leavesCZY
 */
interface ApiService {

    //Retrofit 原始请求方式
    @GET("getUserData")
    fun getUserDataA(): Call<ResponseBody>

    //RxJava 的请求方式
    @GET("getUserData")
    fun getUserDataB(): Observable<ResponseBody>

}
```

我们在搭建项目的网络请求框架的时候，一个重要的设计环节就是要**避免由于网络请求结果的异步延时回调导致内存泄漏情况的发生**，所以在使用 RxJava 的时候我们往往是会搭配 RxLifecycle 来一起使用。而 Google 推出的 Jetpack 组件一个很大的亮点就是提供了生命周期安全保障的 LiveData：[从源码看 Jetpack（3）-LiveData 源码解析](https://juejin.im/post/6847902222345633806)

LiveData 是基于观察者模式来实现的，也完全符合我们在进行网络请求时的使用习惯。所以，本篇文章就来动手实现一个 **LiveDataCallAdapter**，即实现以下方式的网络请求回调

```kotlin
interface ApiService {

    @GET("getUserData")
    fun getUserData(): LiveData<HttpWrapBean<UserBean>>

}

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        RetrofitManager.apiService.getUserData().observe(this, Observer {
                val userBean = it.data
        })
    }

}
```

# 一、基础定义

假设我们的项目中 API 接口的返回值的数据格式都是如下所示。通过 status 来标明本次网络请求结果是否成功，在 data 里面存放具体的目标数据

```json
{
	"status": 200,
	"msg": "success",
	"data": {
		
	}
}
```

对应我们项目中的实际代码就是一个泛型类

```kotlin
data class HttpWrapBean<T>(val status: Int, val msg: String, val data: T) {

    val isSuccess: Boolean
        get() = status == 200

}
```

所以，ApiService 就可以如下定义，用 LiveData 作为目标数据的包装类

```kotlin
data class UserBean(val userName: String, val userAge: Int)

interface ApiService {

    @GET("getUserData")
    fun getUserData(): LiveData<HttpWrapBean<UserBean>>

}
```

而网络请求不可避免会有异常发生，我们还需要预定义几个 Exception，对常见的异常类型：**无网络** 或者 **status!=200** 的情况进行封装

```kotlin
sealed class BaseHttpException(
    val errorCode: Int,
    val errorMessage: String,
    val realException: Throwable?
) : Exception(errorMessage) {

    companion object {

        const val CODE_UNKNOWN = -1024

        const val CODE_NETWORK_BAD = -1025

        fun generateException(throwable: Throwable?): BaseHttpException {
            return when (throwable) {
                is BaseHttpException -> {
                    throwable
                }
                is SocketException, is IOException -> {
                    NetworkBadException("网络请求失败", throwable)
                }
                else -> {
                    UnknownException("未知错误", throwable)
                }
            }
        }

    }

}

/**
 * 由于网络原因导致 API 请求失败
 * @param errorMessage
 * @param realException
 */
class NetworkBadException(errorMessage: String, realException: Throwable) :
    BaseHttpException(CODE_NETWORK_BAD, errorMessage, realException)

/**
 * API 请求成功了，但 code != successCode
 * @param bean
 */
class ServerCodeNoSuccessException(bean: HttpWrapBean<*>) :
    BaseHttpException(bean.status, bean.msg, null)

/**
 * 未知错误
 * @param errorMessage
 * @param realException
 */
class UnknownException(errorMessage: String, realException: Throwable?) :
    BaseHttpException(CODE_UNKNOWN, errorMessage, realException)
```

而在网络请求失败的时候，我们往往是需要向用户 Toast 失败原因的，所以此时一样需要向 LiveData postValue，以此将异常情况回调出去。因为还需要一个可以根据 Throwable 来生成对应的 HttpWrapBean 对象的方法

```kotlin
data class HttpWrapBean<T>(val status: Int, val msg: String, val data: T) {

    companion object {

        fun error(throwable: Throwable): HttpWrapBean<*> {
            val exception = BaseHttpException.generateException(throwable)
            return HttpWrapBean(exception.errorCode, exception.errorMessage, null)
        }

    }

    val isSuccess: Boolean
        get() = status == 200

}
```

# 二、LiveDataCallAdapter

首先需要继承 CallAdapter.Factory 类，在 LiveDataCallAdapterFactory 类中判断是否支持特定的 API 方法，在类型校验通过后返回 LiveDataCallAdapter

```kotlin
class LiveDataCallAdapterFactory private constructor() : CallAdapter.Factory() {

    companion object {

        fun create(): LiveDataCallAdapterFactory {
            return LiveDataCallAdapterFactory()
        }

    }

    override fun get(
        returnType: Type,
        annotations: Array<Annotation>,
        retrofit: Retrofit
    ): CallAdapter<*, *>? {
        if (getRawType(returnType) != LiveData::class.java) {
            //并非目标类型的话就直接返回 null
            return null
        }
        //拿到 LiveData 包含的内部泛型类型
        val responseType = getParameterUpperBound(0, returnType as ParameterizedType)
        require(getRawType(responseType) == HttpWrapBean::class.java) {
            "LiveData 包含的泛型类型必须是 HttpWrapBean"
        }
        return LiveDataCallAdapter<Any>(responseType)
    }

}
```

LiveDataCallAdapter 的逻辑也比较简单，如果**网络请求成功且状态码等于 200 **则直接返回接口值，否则就需要根据不同的失败原因构建出不同的 HttpWrapBean 对象

```kotlin
/**
 * @Author: leavesCZY
 * @Github：https://github.com/leavesCZY
 */
class LiveDataCallAdapter<R>(private val responseType: Type) : CallAdapter<R, LiveData<R>> {

    override fun responseType(): Type {
        return responseType
    }

    override fun adapt(call: Call<R>): LiveData<R> {
        return object : LiveData<R>() {

            private val started = AtomicBoolean(false)

            override fun onActive() {
                //避免重复请求
                if (started.compareAndSet(false, true)) {
                    call.enqueue(object : Callback<R> {
                        override fun onResponse(call: Call<R>, response: Response<R>) {
                            val body = response.body() as HttpWrapBean<*>
                            if (body.isSuccess) {
                                //成功状态，直接返回 body
                                postValue(response.body())
                            } else {
                                //失败状态，返回格式化好的 HttpWrapBean 对象
                                postValue(HttpWrapBean.error(ServerCodeNoSuccessException(body)) as R)
                            }
                        }

                        override fun onFailure(call: Call<R>, t: Throwable) {
                            //网络请求失败，根据 Throwable 类型来构建 HttpWrapBean
                            postValue(HttpWrapBean.error(t) as R)
                        }
                    })
                }
            }

        }
    }

}
```

然后在构建 Retrofit 的时候添加 LiveDataCallAdapterFactory

```kotlin
object RetrofitManager { 

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://getman.cn/mock/")
        .addConverterFactory(GsonConverterFactory.create())
        .addCallAdapterFactory(LiveDataCallAdapterFactory.create())
        .build()

    val apiService = retrofit.create(ApiService::class.java)

}
```

然后就可以直接在 Activity 中发起网络请求了。当 Activity 处于后台时 LiveData 不会回调任何数据，避免了常见的内存泄漏和 NPE 问题

```kotlin
/**
 * @Author: leavesCZY
 * @Github：https://github.com/leavesCZY
 */
@Router(EasyRouterPath.PATH_RETROFIT)
class LiveDataCallAdapterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_live_data_call_adapter)
        btn_success.setOnClickListener {
            RetrofitManager.apiService.getUserDataSuccess().observe(this, Observer {
                if (it.isSuccess) {
                    showToast(it.toString())
                } else {
                    showToast("failed: " + it.msg)
                }
            })
        }
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

}
```

# 三、GitHub

LiveDataCallAdapter 的实现逻辑挺简单的，在使用上也很简单。本篇文章也算作是在了解了 Retrofit 源码后所做的一个实战 😁😁 这里也提供上述代码的 GitHub 链接：[AndroidOpenSourceDemo](https://github.com/leavesCZY/AndroidOpenSourceDemo)