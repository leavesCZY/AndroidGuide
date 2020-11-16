> 前阵子定了个小目标，打算来深入了解下几个常用的开源库，看下其源码和实现原理，进行总结并输出成文章。初定的目标是 EventBus、ARouter、LeakCanary、Retrofit、Glide、OkHttp、Coil 等七个。**目前已经完成了十一篇关于 EventBus、ARouter、LeakCanary、Retrofit、Glide、OkHttp 的文章**，本篇是第十二篇，是关于 OkHttp 的实战内容，来实现一个 OkHttp 的可视化抓包工具，希望对你有所帮助😎😎

在使用 OkHttp 或者 Retrofit 的时候，我觉得大部分开发者会做得最多的自定义实现就是**拦截器**了。因为 OkHttp 的拦截器真的是太有用了，我们的很多需求：**添加 Header、计算并添加签名信息、网络请求记录**等都可以通过拦截器来自动完成，只要定义好规则，就可以覆盖到全局的 OkHttp 网络请求

按照我写 **[三方库源码笔记]** 这系列文章的习惯，我是每写一篇关于源码讲解的文章，就会接着写一篇关于该三方库的自定义实现或者是扩展阅读。所以，承接上一篇文章：[三方库源码笔记（11）-OkHttp 源码详解](https://juejin.im/post/6895369745445748749)  本篇文章就来写关于 OkHttp 的实战内容，来实现一个在移动端的可视化抓包工具：[Monitor](https://github.com/leavesC/Monitor)

Monitor 适用于使用了 **OkHttp/Retrofit** 作为网络请求框架的项目，只要添加了 **MonitorInterceptor** 拦截器，Monitor 就会自动记录并保存所有的**网络请求信息**且自动弹窗展示

最后实现的效果如下所示：

![](https://s1.ax1x.com/2020/10/21/BCJpz6.gif)

代码我已经发布到了 jitpack，方便大家直接远程依赖使用。同时引入 debug 和 release 版本的依赖，**release 版本的 MonitorInterceptor 不会做任何操作，避免了信息泄露，也不会增加 Apk 体积大小**

```groovy
        allprojects {
            repositories {
                maven { url 'https://jitpack.io' }
            }
        }

        dependencies {
           debugImplementation 'com.github.leavesC.Monitor:monitor:1.1.3'
           releaseImplementation 'com.github.leavesC.Monitor:monitor-no-op:1.1.3'
        }
```

只要向 OkHttpClient 添加了 MonitorInterceptor，之后的操作就都会自动完成

```kotlin
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(MonitorInterceptor(Context))
            .build()
```

由于代码都已开源，所以我这里也不做过多介绍了，读者直接看源码即可，其核心思路就是通过 Interceptor 拿到 Request 和 Response，记录各项请求信息，存到数据库中进行持久化保存，在实现思路上类似于 squareup 官方的`ogging-interceptor`，只是说 Monitor 会更加直接和方便😋😋

其实 Monitor 是我蛮久前写的一个开源库了，刚好和我现在要写的文章主题相符，就趁着这机会做了一次整体重构，欢迎 star 和 issue

GitHub 链接点击这里：[Monitor](https://github.com/leavesC/Monitor)