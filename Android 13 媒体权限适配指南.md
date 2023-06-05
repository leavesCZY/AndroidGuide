> 公众号：[字节数组](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/adbc507fc3704fd8955aae739a433db2~tplv-k3u1fbpfcp-zoom-1.image)
>
> 希望对你有所帮助 🤣🤣

在 Android 系统最近的几个大版本里，更新方向有很大一部分都集中在了隐私安全这一方面，每个版本都会新增隐私安全限制，或者是对之前的隐私项进行进一步的升级

- Android 10。分区存储、限制访问不可重置的硬件标识符、限制对剪贴板数据的访问权限
- Android 11。强制执行分区存储、单次授权、自动重置权限、软件包可见性
- Android 12。授予大致位置信息权限、剪贴板访问通知、更安全的组件导出
- Android 13。细化的媒体权限、内置图片选择器、隐藏剪贴板中的敏感内容、屏蔽不匹配的 Intent、针对 Wifi 设备的新运行时权限、广告 ID 权限

Android 13 在最近也发布了正式版，此次版本中新增的隐私安全限制也终于能够解决众多应用长久以来的两个问题了

在很多 Android 应用中，都会通过内置一个 **图片选择器** 来向用户展示系统相册内的所有图片，常见于 “上传用户头像、发送图片” 等业务场景，这就需要通过获得 READ_EXTERNAL_STORAGE 权限来实现了。而这个权限也存在极大的隐私风险，应用也许会向用户说明该权限仅仅只会在选择图片时使用，但除了应用开发者外，谁又能确保应用不会依靠该权限在后台偷偷做些什么呢？而对于开发者来说也属于无奈之举，应用也许仅仅只是想拿到系统相册内的图片而已，却由于 Android 系统的机制被迫要去申请一个应用范围更高的权限

平心而论，我觉得在应用中内置一个图片选择器的确算作是一个比较能提升用户体验的点。因为 Android 系统内置的图片选择器的功能长久以来一直很弱，应用的业务场景往往都需要限制图片的类型、允许选择多张图片并限制最大选择数，但系统却无法满足这些需求，如果我们等到用户选择图片返回后再提示用户不支持该图片格式，或者是让用户多次往返选择图片的话，那的确是挺让人反感的

以上两个问题，依靠在 Android 13 中新增的两个隐私安全项：**细化的媒体权限** 和 **内置图片选择器**，也终于能够得到解决了，后面来一一进行讲解

# 细化的媒体权限

在 Android 13 之前，应用如果想要访问设备中的媒体资源的话，都必须通过 READ_EXTERNAL_STORAGE 权限才能实现。从 Android 13 开始，系统将 READ_EXTERNAL_STORAGE 细分为了三个更加明确的权限，分别用于访问用户的三类媒体资源：Image、Video、Audio，从而让用户能够按需授权，避免隐私风险无序扩大

| 媒体类型 | 请求权限          |
| :------- | :---------------- |
| 图片     | READ_MEDIA_IMAGES |
| 视频     | READ_MEDIA_VIDEO  |
| 音频     | READ_MEDIA_AUDIO  |

我们需要同时通过 **应用的 targetSdkVersion** 和 **设备的系统版本** 来适配这三个权限

- 如果应用还未适配 Android 13，也即 targetSdkVersion 小于 33
  - 此时不管系统版本是多少，依然还是通过 READ_EXTERNAL_STORAGE 权限来访问媒体资源
- 如果应用已适配 Android 13，也即 targetSdkVersion 大于等于 33
  - 如果系统版本小于 33，此时依然要通过 READ_EXTERNAL_STORAGE 才能访问媒体资源
  - 如果系统版本大于等于 33，此时必须通过这三个细分权限才能访问媒体资源，READ_EXTERNAL_STORAGE 权限已失效

简单来说，如果系统版本和 targetSdkVersion 都大于等于 33 的话，此时就必须通过这三个细分权限才能访问媒体资源，其它情况还是需要依赖于 READ_EXTERNAL_STORAGE 权限

看个实际的例子

几个月前我发布了一个开源库， 一个用 Jetpack Compose 实现的 Android 图片选择框架：[Matisse](https://github.com/leavesCZY/Matisse)

也发表了一篇文章进行介绍：[Jetpack Compose 实现一个图片选择框架](https://juejin.cn/post/7108420791502372895)

![](https://upload-images.jianshu.io/upload_images/2552605-8772ec10193468d5.png)

Matisse 的特点 & 优势：

- 完全用 Kotlin & Jetpack Compose 实现
- 适配到 Android 13，解决了多个系统兼容性问题
- 支持精准筛选图片类型，获取图片的详细信息
- 支持精细自定义主题，默认提供日夜间两套主题
- 支持直接启动拍照流程，一共包含四种拍照策略，可以自由选择是否要申请权限

Matisse 刚发布时我是适配到 Android 12，目前 Android 13 已发布正式版，这阵子我也就将其适配到了 Android 13

Matisse 作为一个开源的图片选择框架，自然需要同时顾及 **引用方的 targetSdkVersion** 和 **设备的系统版本** 这两个变量，针对 Android 13 的 READ_MEDIA_IMAGES 权限，其实也仅需要将以前固定申请 READ_EXTERNAL_STORAGE 权限的方式，改为选择性申请即可

```kotlin
private fun requestReadImagesPermission() {
    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        applicationInfo.targetSdkVersion >= Build.VERSION_CODES.TIRAMISU
    ) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    if (PermissionUtils.checkSelfPermission(context = this, permission = permission)) {
        //已获得必要权限，可以去加载系统相册图片了
    } else {
        //去申请必要权限
    }
}
```

此外，我们也要根据实际情况来声明最合适且最少的权限

如果应用的 targetSdkVersion 小于 33，则还是继续声明 READ_EXTERNAL_STORAGE 权限即可

```kotlin
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
```

如果应用的 targetSdkVersion 大于等于 33，则需要同时声明 READ_EXTERNAL_STORAGE 和 READ_MEDIA_IMAGES 两个权限。此外，在这种情况下，READ_EXTERNAL_STORAGE 权限已无法用于 Android 13 开始之后的系统版本了，所以可以将此权限的 maxSdkVersion 设为 32，从而不会出现在 Android 13 开始之后的系统版本中

```kotlin
<uses-permission
    android:name="android.permission.READ_EXTERNAL_STORAGE"
    android:maxSdkVersion="32" />
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
```

# 内置图片选择器

从 Android 13 开始，Android 系统内置了一个功能更为强大的图片选择器，可以让应用更加灵活地访问设备中的媒体资源，且无需拥有查看设备上所有媒体文件的权限。此外，虽然官方将其命名为图片选择器，但实际上也支持选择设备中的视频文件

引用 Google 官方的描述：

照片选择器提供了一个可浏览、可搜索的界面，其中按日期从最近到最早的顺序向用户呈现其媒体库中的文件。此工具为用户提供了一种安全的内置图片和视频选择方式，让其无需向应用授予对整个媒体库的访问权限

如果您允许系统配置照片选择器，则该工具适用于满足以下条件的设备（Android Go 设备除外）：

- 搭载 Android 11（API 级别 30）或更高版本
- 通过 Google 系统更新接收对模块化系统组件的更改

此外，如果您允许系统配置照片选择器，该工具会自动更新，并随着时间推移为应用的用户提供扩展的功能，而无需对代码进行任何更改

![](https://upload-images.jianshu.io/upload_images/2552605-b1dabff57b2f4934.gif)

再来看下应用如何来使用该图片选择器

首先，由于 Google 会通过 Google Play 将该新型的图片选择器推送给 Android 11 及以上系统的设备 (不包括 Go 设备)，所以该功能不仅仅只能在 Android 13 开始后的系统可以使用，我们可以通过如下方法来检查当前设备是否支持该功能

```kotlin
//compileSdkVersion 需要至少为 33 才可以调用此方法
fun isPhotoPickerAvailable(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        true
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        getExtensionVersion(Build.VERSION_CODES.R) >= 2
    } else {
        false
    }
}
```

我们可以通过以下两种 ActivityResultContract 来启动图片选择器：

- PickVisualMedia。用于选择单张图片或单个视频
- PickMultipleVisualMedia。用于选择多张图片或多个视频

例如，在只需要选择单张图片或者单个视频的情况下，可以这么使用：

```kotlin
private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
    if (uri != null) {
        //TODO
    }
}

//选择图片或视频
pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))

//仅选择图片
pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))

//仅选择视频
pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))

//仅选择 Gif 图片
val mimeType = "image/gif"
pickMedia.launch(
    PickVisualMediaRequest(
        ActivityResultContracts.PickVisualMedia.SingleMimeType(
            mimeType
        )
    )
)
```

在这种情况下，图片选择器会以半屏模式打开

![](https://upload-images.jianshu.io/upload_images/2552605-f80611ac84b7674e.png)

类似的，如果想要选择多张图片或者是多个视频，可以通过 PickMultipleVisualMedia 来限定最大的选取数量

```kotlin
private val pickMultipleMedia = registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(5)) { uris ->
    if (uris.isNotEmpty()) {
        //TODO
    }
}
```

此时，图片选择器会以全屏展开的形式进行展示，当资源数量超限时也会对用户进行提示

![](https://upload-images.jianshu.io/upload_images/2552605-6b712682fcd9a8b6.png)

需要注意，以上代码需要添加 1.6.0 或更高版本的 `androidx.activity` 库后才可以使用。此外，从 PickVisualMedia 和 PickMultipleVisualMedia 的源码可以看到，Android 13 内置的图片选择器对应的是 `MediaStore.ACTION_PICK_IMAGES` 这个新增的 Intent，而如果当前设备不支持媒体选择器功能的话，就会改为通过调用 `Intent.ACTION_OPEN_DOCUMENT` 来选择媒体资源，这种情况下 PickMultipleVisualMedia 设定的数量上限自然也就失效了

# 结尾

Android 13 细化的媒体权限解决了 READ_EXTERNAL_STORAGE 带来的隐私风险无序扩大的问题，让用户可以按需授权。而内置的图片选择器又能够让应用在无需用户授权的情况下就可以灵活地选取媒体资源，应用也就完全没有必要自己内置一个图片选择器了

所以说，平衡用户体验和用户隐私安全最好的做法应该是：

- 在 Android 13 之前依然还是通过应用内置的图片选择器来实现业务功能
- 在 Android 13 开始之后的版本仅使用系统内置的图片选择器，完全弃用 READ_EXTERNAL_STORAGE 权限

此次 Android 13 版本还是有点东西的 ~ 