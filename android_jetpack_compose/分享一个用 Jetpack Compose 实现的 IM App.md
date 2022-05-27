> 公众号：[字节数组](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/adbc507fc3704fd8955aae739a433db2~tplv-k3u1fbpfcp-zoom-1.image)
>
> 希望对你有所帮助 🤣🤣

在 Google 刚正式发布 Jetpack Compose 1.0 release 版本的时候，我花了一个多月的时间进行了一次实战演练，用 Jetpack Compose 实现了一个 IM App，提供了完备的聊天功能，并在 Github 开源了：[compose_chat](https://github.com/leavesCZY/compose_chat)

![](https://p1-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/7085f204d3b8487ca7d6aedc50ab66c5~tplv-k3u1fbpfcp-watermark.image?)

在完成初始版本的开发后，我以 compose_chat 为例子，先后写了两篇文章来详细介绍 Jetpack Compose 的种种特性，自认算作是比较不错的入门学习资料了：

- [学不动也要学，Jetpack Compose 写一个 IM APP（一）](https://juejin.cn/post/6991429231821684773)
- [学不动也要学，Jetpack Compose 写一个 IM APP（二）](https://juejin.cn/post/7028397244894330917)

此外，我也写了几篇文章来介绍 Jetpack Compose 的一些进阶知识：

- [随便嵌套？Jetpack Compose 到底优秀在哪里](https://juejin.cn/post/7070158120831418381)
- [学不动也要学，Jetpack Compose 实现自定义绘制](https://juejin.cn/post/6996568363581308959)
- [学不动也要学，Jetpack Compose 玩一把俄罗斯方块](https://juejin.cn/post/6974585048762679310)

Jetpack Compose 在 Android 开发者的世界里也逐渐普及开了，甚至连适用于多个平台的声明式 UI 开发框架 Compose Multiplatform 也发布了正式版本，我也对 Compose Multiplatform 做了一次小小的实践：

- [不止 Android，Compose Multiplatform 初探](https://juejin.cn/post/7062533562460799013)

Jetpack Compose 应该会成为以后 Android 原生 UI 开发时的首选方案。我公司的项目由于一些问题所限，所以还没引入 Jetpack Compose，但我对其一直保持着很大的兴趣，希望能够将其应用到尽量真实的需求开发中。因此，近一年来我也一直在维护 compose_chat，不断为其增添一些新功能：从一开始只能私聊，到现在支持群聊；从只支持发送文本消息，到现在支持发送 emojo 表情和图片消息；Material Design 3 刚推出的时候我也立马就引入进来了

历史更新记录如下所示：

### v1.5.0

- 支持 Gif 类型的图片消息和用户头像
- 支持保存 Gif 类型的图片到本地相册

### v1.4.9

- 修复 bug
- 规整代码

### v1.4.8

- 优化交互体验
- 规整代码

### v1.4.7

- 升级 targetSdkVersion 到 31
- 发送图片消息前先检测是否需要对图片进行压缩
- 优化图片消息的显示比例
- 统一消息的发送逻辑
- 修复 bug

### v1.4.6

- 修复 bug

### v1.4.5

- 升级依赖库
- 优化交互体验

### v1.4.4

- 支持保存图片到本地相册
- 支持修改个人资料时进行效果预览
- 为侧滑栏添加拖拽动画
- 升级依赖库

### v1.4.3

- 引入 Material Design 3

### v1.4.2

- 支持点击查看大图
- 支持选择本地图片作为头像
- 优化交互体验

### v1.4.0

- 支持发送图片消息
- 新增应用全局黑白化的主题

### v1.2.1

- 群主能够修改群头像

### v1.2.0

- 支持群聊
- 支持发送 emoji 表情

### v1.0.0

- 支持私聊
- 支持发送文本消息



欢迎读者去 Github 下载体验，也欢迎给个 star 和建议：[compose_chat](https://github.com/leavesCZY/compose_chat)