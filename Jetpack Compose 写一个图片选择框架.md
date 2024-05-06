知乎的 [Matisse](https://github.com/zhihu/Matisse) 应该有蛮多的 Android 开发者有了解过或者是曾经使用过，这是知乎在 2017 年开源的一个 Android 端图片选择框架，其颜值在现在看来也还是挺不错的

![](https://upload-images.jianshu.io/upload_images/2552605-9504d5d974f9c21e.png#id=FnSsV&originHeight=507&originWidth=826&originalType=binary&ratio=1&rotation=0&showTitle=false&status=done&style=none&title=)

可惜近几年知乎官方已经不再对 Matisse 进行维护更新了，上一次提交记录还停留在 2019 年，累积了四百多个 issues 一直没人解答，很多高版本系统的兼容性问题和内部 bug 也一直得不到解决。我反编译了知乎 App，发现其内部还保留着 Matisse 的相关代码，所以知乎应该不是完全废弃了 Matisse，而只是不再开源后续更新了

我公司的项目也使用到了 Matisse，随着 Android 系统的更新，时不时地就会有用户来反馈问题，无奈我也只能 fork 了源码自己来维护。一直这么小修小补终究不太合适，而且如果不进行完全重写的话，Matisse 的一些交互体验问题也没法得到彻底解决，而这些问题在目前的知乎 App 上也一样存在，以修改个人头像时打开的图片选择页面为例：

![](https://upload-images.jianshu.io/upload_images/2552605-69332d4499ae215b.gif#id=GMNgn&originHeight=893&originWidth=427&originalType=binary&ratio=1&rotation=0&showTitle=false&status=done&style=none&title=)

我发现的问题有三个：

- 知乎的用户头像不支持 Gif 格式，当用户点击 Gif 图片时会提示 “不支持的文件类型”。按我的想法，既然不支持 Gif 格式，那么一开始展示的时候就应该过滤掉才对，而知乎目前的筛选逻辑应该就是来源自 Matisse ，因为 Matisse 也不支持 **只展示静态图**，但又可以 **只展示 Gif**，这筛选逻辑我觉得十分奇怪
- 当取消勾选静态图时，可以看到 Gif 图片会很明显地闪烁了一下，此问题在 Matisse 中也存在。而如果从知乎的编辑器进入图片选择页面的话，就不单单是 Gif 图片会闪烁了，而是整个页面都会闪烁一下…
- 当点击下拉菜单时，可以看到 Pictures 目录中有三张图片，但打开目录又发现是空的。这是由于知乎没有过滤掉一些脏数据导致的，后面会讲到具体原因

由于以上问题，也让我有了彻底放弃 Matisse，自己来实现一个新的图片选择框架的打算，也实现得差不多了，最终的效果如下所示

![](https://upload-images.jianshu.io/upload_images/2552605-8772ec10193468d5.png#id=oM7Nh&originHeight=598&originWidth=882&originalType=binary&ratio=1&rotation=0&showTitle=false&status=done&style=none&title=)

此框架的特点 & 优势有：

- 适配到 Android 13
- 解决了多个系统兼容性问题，下文会提到
- 完全用 Kotlin & Jetpack Compose 实现
- 支持精准筛选图片类型并获取图片的详细信息
- 支持精细自定义主题，默认提供日夜间两套主题
- 支持直接启动拍照流程，一共包含四种拍照策略，可以自由选择是否要申请权限

此框架也有一些劣势：

- 预览图片时不支持手势缩放。一开始我有尝试用 Jetpack Compose 来实现图片手势缩放，但效果不太理想，而我又不想引入 View 体系中的三方库，所以此版本暂不支持图片手势缩放
- 框架内部采用的图片加载库是 Coil，且不支持替换。由于目前支持 Jetpack Compose 的图片加载库基本只能选择 Coil 了，因此没有提供替换图片加载库的入口
- 图片列表的滑动性能要低于原生的 RecyclerView，debug 版本尤为明显。此问题目前无解，只能等 Google 官方后续的优化了

代码我也开源到了 Github，懒得想名字，再加上一开始的设计思路也来自于 Matisse，因此就取了一样的名字，也叫 [Matisse](https://github.com/leavesCZY/Matisse)。下文如果没有特别说明，Matisse 指的就是此 Jetpack Compose 版本的图片选择框架了

在 UI 层面上，用 Jetpack Compose 来实现相比原生的 View 体系实在要简单很多，在这一块除了滑动性能之外我也没遇到其它问题。因此，本文的内容和 Jetpack Compose 无关，主要是讲 Matisse 的一些实现细节和遇到的系统兼容性问题

# 获取图片

实现一个图片选择框架的第一步自然就是要获取到相册内的所有图片了，因此需要申请 READ_EXTERNAL_STORAGE 或者 READ_MEDIA_IMAGES 权限，此外还需要依赖系统的 MediaStore API

MediaStore 相当于一个文件系统数据库，记录了当前设备中所有文件的索引，我们可以通过它来快速查找设备中特定类型的文件。Matisse 使用的是 MediaStore.Image，在操作上就类似于查询数据库，通过声明需要的数据库字段 projection 和排序规则 sortOrder，得到相应的数据库游标 cursor，通过 cursor 遍历查询出每一个字段值

```kotlin
val projection = arrayOf(
    MediaStore.Images.Media._ID,
    MediaStore.Images.Media.DISPLAY_NAME,
    MediaStore.Images.Media.MIME_TYPE,
    MediaStore.Images.Media.WIDTH,
    MediaStore.Images.Media.HEIGHT,
    MediaStore.Images.Media.SIZE,
    MediaStore.Images.Media.ORIENTATION,
    MediaStore.Images.Media.DATA,
    MediaStore.Images.Media.BUCKET_ID,
    MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
)
val sortOrder = "${MediaStore.Images.Media.DATE_MODIFIED} DESC"
val mediaResourceList = mutableListOf<MediaResource>()
try {
    val mediaCursor = context.contentResolver.query(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        projection,
        selection,
        selectionArgs,
        sortOrder,
    ) ?: return@withContext null
    mediaCursor.use { cursor ->
        while (cursor.moveToNext()) {
            val data = cursor.getString(MediaStore.Images.Media.DATA)
            if (data.isBlank() || !File(data).exists()) {
                continue
            }
            val id = cursor.getLong(MediaStore.Images.Media._ID)
            val displayName = cursor.getString(MediaStore.Images.Media.DISPLAY_NAME)
            val mimeType = cursor.getString(MediaStore.Images.Media.MIME_TYPE)
            val width = cursor.getInt(MediaStore.Images.Media.WIDTH)
            val height = cursor.getInt(MediaStore.Images.Media.HEIGHT)
            val size = cursor.getLong(MediaStore.Images.Media.SIZE)
            val orientation = cursor.getInt(MediaStore.Images.Media.ORIENTATION)
            val bucketId = cursor.getString(MediaStore.Images.Media.BUCKET_ID)
            val bucketDisplayName =
                cursor.getString(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            val contentUri = ContentUris.withAppendedId(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                id
            )
            val mediaResource = MediaResource(
                id = id,
                uri = contentUri,
                displayName = displayName,
                mimeType = mimeType,
                width = width,
                height = height,
                orientation = orientation,
                path = data,
                size = size,
                bucketId = bucketId,
                bucketDisplayName = bucketDisplayName,
            )
            mediaResourceList.add(mediaResource)
        }
    }
} catch (e: Throwable) {
    e.printStackTrace()
}
return@withContext mediaResourceList
```

每一张图片都存放于特定的相册文件夹内，因此可以通过 bucketId 来对每一张图片进行归类，从而得到 Matisse 中的下拉菜单

```kotlin
suspend fun groupByBucket(resources: List<MediaResource>): List<MediaBucket> {
    return withContext(context = Dispatchers.IO) {
        val resourcesMap = linkedMapOf<String, MutableList<MediaResource>>()
        resources.forEach { res ->
            val bucketId = res.bucketId
            val list = resourcesMap[bucketId]
            if (list == null) {
                resourcesMap[bucketId] = mutableListOf(res)
            } else {
                list.add(res)
            }
        }
        val allMediaBucketResource = mutableListOf<MediaBucket>()
        resourcesMap.forEach {
            val resourcesList = it.value
            if (resourcesList.isNotEmpty()) {
                val bucketId = it.key
                val bucketDisplayName = resourcesList[0].bucketDisplayName
                allMediaBucketResource.add(
                    MediaBucket(
                        id = bucketId,
                        displayName = bucketDisplayName,
                        displayIcon = resourcesList[0].uri,
                        resources = resourcesList,
                        supportCapture = false
                    )
                )
            }
        }
        return@withContext allMediaBucketResource
    }
}
```

# 拍照策略

一般情况下，应用对于拍照功能都不会有太多的自定义需求，因此大多是通过直接调起系统相机来实现拍照，优点是实现简单，且不用申请 CAMERA 权限

实现代码大致如下所示，最终图片就会保存在 imageUri 指向的文件中

```kotlin
class MatisseActivity : ComponentActivity() {

    private var tempImageUri: Uri? = null

    private fun takePicture(imageUri: Uri) {
        tempImageUri = imageUri
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        startActivityForResult(intent, 1)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == Activity.RESULT_OK) {
            val mTempImageUri = tempImageUri
            if (mTempImageUri != null) {
                //TODO
            }
        }
    }

}
```

以上代码属于通用流程，当判断到完成拍照后，将以上的 imageUri 返回即可

但生成 imageUri 却有着很多学问：不同的生成规则对应着不同的权限，甚至同种方式在不同系统版本上对权限的要求也不一样，对用户的感知也不一样。此外，如果用户又取消了拍照的话，此时 imageUri 指向的图片文件就没有用了，我们还需要主动删除该文件

Matisse 通过 CaptureStrategy 接口来抽象以上逻辑

```kotlin
/**
 * 拍照策略
 */
interface CaptureStrategy : Parcelable {

    /**
     * 是否启用拍照功能
     */
    fun isEnabled(): Boolean

    /**
     * 是否需要申请读取存储卡的权限
     */
    fun shouldRequestWriteExternalStoragePermission(context: Context): Boolean

    /**
     * 获取用于存储拍照结果的 Uri
     */
    suspend fun createImageUri(context: Context): Uri?

    /**
     * 获取拍照结果
     */
    suspend fun loadResource(context: Context, imageUri: Uri): MediaResource?

    /**
     * 当用户取消拍照时调用
     */
    suspend fun onTakePictureCanceled(context: Context, imageUri: Uri)

    /**
     * 生成图片文件名
     */
    fun createImageName(): String {
        val uuid = UUID.randomUUID().toString()
        val randomName = uuid.substring(0, 8)
        return "$randomName.jpg"
    }

}
```

Matisse 实现了四种拍照策略供引用方使用：

- NothingCaptureStrategy
- FileProviderCaptureStrategy
- MediaStoreCaptureStrategy
- SmartCaptureStrategy

## NothingCaptureStrategy

NothingCaptureStrategy 代表的是不开启拍照功能，也是 Matisse 的默认策略

```kotlin
/**
 *  不开启拍照功能
 */
@Parcelize
object NothingCaptureStrategy : CaptureStrategy {

    override fun isEnabled(): Boolean {
        return false
    }

    override fun shouldRequestWriteExternalStoragePermission(context: Context): Boolean {
        return false
    }

    override suspend fun createImageUri(context: Context): Uri? {
        return null
    }

    override suspend fun loadResource(context: Context, imageUri: Uri): MediaResource? {
        return null
    }

    override suspend fun onTakePictureCanceled(context: Context, imageUri: Uri) {

    }

}
```

## FileProviderCaptureStrategy

顾名思义，此策略通过 FileProvider 来生成所需要的 imageUri

从 Android 7.0 开始，系统禁止应用通过  `file://URI` 来访问其他应用的私有目录文件，要在应用间共享私有文件，必须通过 `content://URI` 并授予 URI 临时访问权限来实现，否则将直接抛出异常。而将 File 转换为 `content://URI` 的操作就需要依靠 FileProvider 来实现了。Matisse 传递给系统相机的 imageUri 也需要满足此要求

FileProviderCaptureStrategy 采用的策略就是：

- 在 ExternalFilesDir 的 Pictures 目录中创建一个图片临时文件用于存储拍照结果，通过 FileProvider 得到该文件对应的 `content://URI` ，从而得到待写入的 imageUri
- 假如用户最终完成拍照，则通过 BitmapFactory 获取图片的详细信息
- 假如用户最终取消拍照，则直接删除创建的临时文件
- 由于图片是保存在应用自身的私有目录中，因此不需要申请任何权限，也正因为是私有目录，所以图片不会出现在系统相册中

```kotlin
/**
 *  通过 FileProvider 来生成拍照所需要的 ImageUri
 *  无需申请权限，所拍的照片不会保存在系统相册里
 *  外部必须配置 FileProvider，并在此处传入 authority
 */
@Parcelize
class FileProviderCaptureStrategy(private val authority: String) : CaptureStrategy {

    @IgnoredOnParcel
    private val uriFileMap = mutableMapOf<Uri, File>()

    override fun isEnabled(): Boolean {
        return true
    }

    override fun shouldRequestWriteExternalStoragePermission(context: Context): Boolean {
        return false
    }

    override suspend fun createImageUri(context: Context): Uri? {
        return withContext(context = Dispatchers.IO) {
            try {
                val tempFile = createTempFile(context = context)
                if (tempFile != null) {
                    val uri = FileProvider.getUriForFile(context, authority, tempFile)
                    uriFileMap[uri] = tempFile
                    return@withContext uri
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            }
            return@withContext null
        }
    }

    private fun createTempFile(context: Context): File? {
        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val file = File(storageDir, createImageName())
        if (file.createNewFile()) {
            return file
        }
        return null
    }

    override suspend fun loadResource(context: Context, imageUri: Uri): MediaResource {
        return withContext(context = Dispatchers.IO) {
            val imageFile = uriFileMap[imageUri]!!
            uriFileMap.remove(key = imageUri)
            val imageFilePath = imageFile.absolutePath
            val option = BitmapFactory.Options()
            option.inJustDecodeBounds = true
            BitmapFactory.decodeFile(imageFilePath, option)
            return@withContext MediaResource(
                id = 0,
                uri = imageUri,
                displayName = imageFile.name,
                mimeType = option.outMimeType ?: "",
                width = max(option.outWidth, 0),
                height = max(option.outHeight, 0),
                orientation = 0,
                size = imageFile.length(),
                path = imageFile.absolutePath,
                bucketId = "",
                bucketDisplayName = ""
            )
        }
    }

    override suspend fun onTakePictureCanceled(context: Context, imageUri: Uri) {
        withContext(context = Dispatchers.IO) {
            val imageFile = uriFileMap[imageUri]
            if (imageFile != null && imageFile.exists()) {
                imageFile.delete()
            }
            uriFileMap.remove(key = imageUri)
        }
    }

}
```

外部需要在自身项目中声明 FileProvider，authorities 视自身情况而定，通过 authorities 来实例化 FileProviderCaptureStrategy

```kotlin
<provider
    android:name="androidx.core.content.FileProvider"
    android:authorities="github.leavesczy.matisse.samples.FileProvider"
    android:exported="false"
    android:grantUriPermissions="true">
    <meta-data
        android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/file_paths" />
</provider>
```

`file_paths.xml` 中需要配置 `external-files-path` 路径的 Pictures 文件夹，name 可以随意命名

```kotlin
<?xml version="1.0" encoding="utf-8"?>
<paths>
    <external-files-path
        name="Capture"
        path="Pictures" />
</paths>
```

## MediaStoreCaptureStrategy

顾名思义，此策略通过 MediaStore 来生成所需要的 imageUri

在 Android 10 系统之前，应用需要获取到 WRITE_EXTERNAL_STORAGE 权限后才可以向共享存储空间中写入文件。从 Android 10 开始，应用通过 MediaStore 向共享存储空间中写入文件无需任何权限，且对于应用自身创建的文件，无需 READ_EXTERNAL_STORAGE 权限就可以直接访问和删除

MediaStoreCaptureStrategy 采用的策略就是：

- 在大于等于 10 的系统版本中，不申请 WRITE_EXTERNAL_STORAGE 权限，其它系统版本则进行申请
- 通过 MediaStore 在系统相册中预创建一张图片，从而得到待写入的 imageUri
- 假如用户最终完成拍照，则通过 MediaStore 去查询 imageUri 对应图片的详细信息
- 假如用户最终取消拍照，则通过 MediaStore 删除 imageUri 指向的脏数据
- 由于图片一开始就保存在 MediaStore 中，因此图片会显示在系统相册中

```kotlin
/**
 *  通过 MediaStore 来生成拍照所需要的 ImageUri
 *  根据系统版本决定是否需要申请 WRITE_EXTERNAL_STORAGE 权限
 *  所拍的照片会保存在系统相册里
 */
@Parcelize
class MediaStoreCaptureStrategy : CaptureStrategy {

    override fun isEnabled(): Boolean {
        return true
    }

    override fun shouldRequestWriteExternalStoragePermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return false
        }
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_DENIED
    }

    override suspend fun createImageUri(context: Context): Uri? {
        return MediaProvider.createImage(context = context, fileName = createImageName())
    }

    override suspend fun loadResource(context: Context, imageUri: Uri): MediaResource? {
        return MediaProvider.loadResources(context = context, uri = imageUri)
    }

    override suspend fun onTakePictureCanceled(context: Context, imageUri: Uri) {
        MediaProvider.deleteImage(context = context, imageUri = imageUri)
    }

}
```

## SmartCaptureStrategy

此策略同时包含 FileProviderCaptureStrategy 和 MediaStoreCaptureStrategy，因此外部也需要像 FileProviderCaptureStrategy 一样配置 FileProvider，通过 authorities 来实例化 SmartCaptureStrategy

- 当系统版本小于 Android 10 时，执行 FileProviderCaptureStrategy 策略
- 当系统版本大于等于 Android 10 时，执行 MediaStoreCaptureStrategy 策略

此策略既无需申请权限，又可以在 Android 10 开始之后的系统版本将拍照所得照片存入到相册中，同时平衡了 “权限” 和 “用户体验”

```kotlin
/**
 * 根据系统版本智能选择拍照策略
 * 既避免需要申请权限，又可以在系统允许的情况下将拍照所得照片存入到系统相册中
 * 当系统版本小于 Android 10 时，执行 FileProviderCaptureStrategy 策略
 * 当系统版本大于等于 Android 10 时，执行 MediaStoreCaptureStrategy 策略
 */
@Parcelize
@Suppress("CanBeParameter")
class SmartCaptureStrategy(private val authority: String) : CaptureStrategy {

    @IgnoredOnParcel
    private val proxy = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStoreCaptureStrategy()
    } else {
        FileProviderCaptureStrategy(authority = authority)
    }

    override fun isEnabled(): Boolean {
        return proxy.isEnabled()
    }

    override fun shouldRequestWriteExternalStoragePermission(context: Context): Boolean {
        return proxy.shouldRequestWriteExternalStoragePermission(context = context)
    }

    override suspend fun createImageUri(context: Context): Uri? {
        return proxy.createImageUri(context = context)
    }

    override suspend fun loadResource(context: Context, imageUri: Uri): MediaResource? {
        return proxy.loadResource(context = context, imageUri = imageUri)
    }

    override suspend fun onTakePictureCanceled(context: Context, imageUri: Uri) {
        proxy.onTakePictureCanceled(context = context, imageUri = imageUri)
    }

}
```

## 总结

所以说，除了 NothingCaptureStrategy 代表不开启拍照功能外，其他三种策略所需要的权限和图片存储的位置都不一样，对用户的感知也不一样

| 拍照策略 | 需要的权限 | 配置项 | 图片对用户是否可见 |
| --- | --- | --- | --- |
| NothingCaptureStrategy |  |  |  |
| FileProviderCaptureStrategy | 无 | 需要配置 FileProvider | 否，图片存储在应用私有目录内，对用户不可见 |
| MediaStoreCaptureStrategy | Android 10 之前需要 WRITE_EXTERNAL_STORAGE 权限，Android 10 开始不需要权限 | 无 | 是，图片存储在系统相册内，对用户可见 |
| SmartCaptureStrategy | 无 | 需要配置 FileProvider | 在 Android 10 之前的系统版本不可见，和 FileProviderCaptureStrategy 一样。在 Android 10 开始之后的系统版本可见，和 MediaStoreCaptureStrategy 一样 |


开发者根据自己的实际情况来选择拍照策略：

- 如果应用本身就需要申请 WRITE_EXTERNAL_STORAGE 权限的话，选 MediaStoreCaptureStrategy，拍摄的图片会保存在系统相册中也比较符合用户的认知
- 如果应用本身就不需要申请 WRITE_EXTERNAL_STORAGE 权限的话，选 FileProviderCaptureStrategy 或者 SmartCaptureStrategy，为了相册问题而多申请一个敏感权限得不偿失

# 拍照权限

Android 系统的 CAMERA 权限用于自定义实现相机功能的业务场景，也即如果使用到了 Camera API 的话，应用就必须声明和申请 CAMERA 权限

而调起系统相机进行拍照不属于自定义实现，因此该操作本身是不要求 CAMERA 权限的，但是否真的不需要申请权限要根据实际情况而定

Android 系统对于 CAMERA 权限有着比较奇怪的要求：

- 应用如果没有声明 CAMERA 权限，此时调起系统相机不需要申请任何权限
- 应用如果有声明 CAMERA 权限，就必须等到用户同意了 CAMERA 权限后才能调起系统相机，否则将直接抛出 SecurityException

因此，虽然 Matisse 本身是通过调起系统相机来实现拍照的，但如果引用方声明了 CAMERA 权限的话，将连锁导致 Matisse 也必须申请 CAMERA 权限

为了解决这个问题，Matisse 通过检查应用的 Manifest 文件中是否包含 CAMERA 权限来决定是否需要进行申请，避免由于意外而奔溃

```kotlin
private fun requestCameraPermissionIfNeed() {
    val cameraPermission = Manifest.permission.CAMERA
    if (PermissionUtils.containsPermission(
            context = this,
            permission = cameraPermission
        ) && !PermissionUtils.checkSelfPermission(context = this, permission = cameraPermission)
    ) {
        requestCameraPermissionLauncher.launch(cameraPermission)
    } else {
        takePicture()
    }
}

internal object PermissionUtils {

    /**
     * 判断是否拥有指定权限
     */
    fun checkSelfPermission(context: Context, permission: String): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * 检查应用是否声明了指定权限
     */
    fun containsPermission(context: Context, permission: String): Boolean {
        try {
            val packageManager: PackageManager = context.packageManager
            val packageInfo = packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_PERMISSIONS
            )
            val permissions = packageInfo.requestedPermissions
            if (!permissions.isNullOrEmpty()) {
                return permissions.contains(permission)
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        return false
    }

}
```

# 取消拍照导致的脏数据

在文章开头给出来的知乎 App 示例图片中可以看到，Pictures 目录明明显示有三张图片，但点击进去又发现目录是空的。这是由于 MediaStore 中存在脏数据导致的

当应用通过 MediaStoreCaptureStrategy 来启动相机时，已经先向 MediaStore 插入一条图片数据了，但如果用户此时又取消了拍照，就会导致 MediaStore 中存在一条脏数据：该数据有 id、uri、path、displayName 等信息，但对应的图片文件实际上并不存在。知乎 App 应该是一开始在归类图片目录的时候没有检查图片是否真的存在，等到要加载图片的时候才发现图片不可用

虽然 MediaStoreCaptureStrategy 会主动删除自己生成的脏数据，但我们没法确保其它应用就不会向 MediaStore 插入脏数据。因此，Matisse 会在遍历查询所有图片的过程中，同时判断该图片指向的文件是否真的存在，有的话才进行展示

```kotlin
mediaCursor.use { cursor ->
    while (cursor.moveToNext()) {
        val data = cursor.getString(MediaStore.Images.Media.DATA)
        if (data.isBlank() || !File(data).exists()) {
            continue
        }
        //TODO
    }
}
```

# resolveActivity API 的兼容性

当我们要隐式启动一个 Activity 的时候，为了避免由于目标 Activity 不存在而导致应用崩溃，我们就需要在 startActivity 前先判断该隐式启动是否有接收者，有的话才去调用 startActivity

Matisse 在启动系统相机的时候也是如此，会先通过 `resolveActivity` 方法查询系统中是否有应用可以处理拍照请求，有的话才去启动相机，避免由于设备没有摄像头而导致应用崩溃

```kotlin
private fun takePicture() {
    lifecycleScope.launch(context = Dispatchers.Main.immediate) {
        tempImageUriForTakePicture = null
        val captureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (captureIntent.resolveActivity(packageManager) != null) {
            val imageUri = captureStrategy.createImageUri(context = applicationContext)
            if (imageUri != null) {
                tempImageUriForTakePicture = imageUri
                takePictureLauncher.launch(imageUri)
                return@launch
            }
        }
        canceled()
    }
}
```

但 `resolveActivity` 方法在 Android 11 和更高的系统上也有着一个兼容性问题：软件包可见性过滤

如果应用的目标平台是 Android 11 或更高版本，那么当应用通过 `queryIntentActivities()、getPackageInfo()、getInstalledApplications()` 等方法查询设备上已安装的其它应用相关信息时，系统会默认对返回结果进行过滤。也就是说，通过这些方法查询到的应用信息会少于设备上真实安装的应用数。`resolveActivity` 方法也受到此影响，经测试，在 Android 11 和 Android 12 的模拟器上，resolveActivity 方法均会返回 null，但在一台 Android 12 的真机上返回值则不为 null，因为不同设备会根据自己的实际情况来决定哪些实现 Android 核心功能的系统服务对所有应用均可见

Matisse 的解决方案是：在 Manifest 文件中通过 `queries` 主动声明 IMAGE_CAPTURE，从而提高对此 action 的可见性

```kotlin
<queries>
    <intent>
        <action android:name="android.media.action.IMAGE_CAPTURE" />
    </intent>
</queries>
```

# File API 的兼容性

严格来说，File API 的兼容性并不属于 Matisse 遇到的问题，而是外部使用者会遇到的问题

从 Android 10 开始，系统推出了分区存储的特性，限制了应用读写共享文件的方式。当应用开启分区存储特性后，对共享文件的读写需要通过 MediaStore 来实现，而不能使用以前常用的 File API，否则将直接抛出异常：`FileNotFoundException open failed: EACCES (Permission denied)`

例如，像 Glide、Coil 等图片框架均支持通过 ByteArray 来加载图片，对于开启了分区存储特性的应用，在 Android 10 系统之前，以下方式是完全可用的，但在 Android 10 系统上就会直接崩溃

```kotlin
val filePath: String = xxx
imageView.load(File(filePath).readBytes())
```

而到了 Android 11 后，Google 可能觉得这种限制对于应用来说过于严格，因此又取消了限制，允许应用继续通过 File API 来读写共享文件，系统会自动将 File API 重定向为 MediaStore API，稍稍有点无语……

因此，虽然 Matisse 的返回值中包含了图片的绝对路径 path，但如果外部开启了分区存储特性的话，在 Android 10 设备上是不能直接通过 File API 来读写共享文件的，在其它系统版本上则可以继续使用

# Github

以上就是 Matisse 的一些实现细节和遇到的系统兼容性问题，更多实现细节请看 Github：[Matisse](https://github.com/leavesCZY/Matisse)

Matisse 同时也发布到了 Jitpack，方便开发者直接远程依赖使用：

```kotlin
allprojects {
    repositories {
        maven { url "https://jitpack.io" }
    }
}

dependencies {
    implementation 'com.github.leavesCZY:Matisse:latestVersion'
}
```
