近两年来工信部对于应用的隐私合规安全问题愈加重视，对 Android 平台的管控程度也要比 IOS 平台严格很多，很多不合规的应用也先后被下架要求整改

![](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/f01addaeea7c4865a77119108fd2c86c~tplv-k3u1fbpfcp-zoom-1.image)

要避免出现隐私合规安全问题，最主要的就是要做到以下两点：

- 在用户同意隐私协议之前，不能有收集用户隐私数据的行为。例如，在用户同意之前不能去获取 Android ID、Device ID、MAC 等隐私数据
- 在用户同意隐私协议之后，搜集用户隐私数据的行为不能超出实现服务场景所必需的最低频率。例如，某些应用会在每次网络请求时将当前设备的 Android ID 作为 header 一起上报，如果没有对 Android ID 进行缓存处理的话，搜集该数据的行为频率就会非常高，此时一样存在隐私合规问题

针对以上两点问题有不同的应对措施

- 对于第一点。需要统计出整个项目中所有涉及到隐私行为的相关代码，根据业务流程来判断该隐私行为是否合理、以及是否会在用户同意隐私协议之前被触发。这就需要对整个项目进行 **静态扫描** 了
- 对于第二点。需要在应用运行时动态记录每次触发隐私行为的时间点和调用链信息，根据触发时间来判断该隐私行为是否过量执行，根据调用链信息来辅助判断具体是哪一块业务在获取隐私数据。这就需要对应用进行 **动态记录** 了

以上应对措施如果单纯靠开发人员来肉眼识别代码和编码统计的话，工作量非常大而且也很不现实，因为一个大型项目往往都会引入多个依赖库和第三方 SDK，我们可以规范自有代码，但没法修改和有效约束外部依赖，也很难理清楚依赖库的内部逻辑和调用链关系。此外，当检测到隐私行为后，也要输出相对应的记录报告，以便开发人员能够在开发阶段排查问题

此时就可以依靠 ASM + Transform 来应对这种非业务侵入式的开发场景了，本文要介绍的就是如何通过 ASM + Transform 来助力通过隐私合规安全审核

# 静态扫描

要进行静态扫描，首先就需要先明确哪些操作是属于隐私行为，这里以获取 Device ID 和 Build.BRAND 为例。在 Activity 中通过点击按钮来模拟获取隐私数据的行为，后续实现 **动态记录** 时需要记录的调用链指的也就是从点击按钮到执行 `DeviceUtils.getDeviceId` 方法的整个过程

```kotlin
package github.leavesc.asm.privacy_sentry

class PrivacySentryActivity : AppCompatActivity() {

    private val btnGetDeviceId by lazy {
        findViewById<Button>(R.id.btnGetDeviceId)
    }

    private val btnGetDeviceBrand by lazy {
        findViewById<Button>(R.id.btnGetDeviceBrand)
    }

    private val tvLog by lazy {
        findViewById<TextView>(R.id.tvLog)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_privacy_sentry)
        btnGetDeviceId.setOnClickListener {
            tvLog.append("\n" + "deviceId: " + DeviceUtils.getDeviceId(this))
        }
        btnGetDeviceBrand.setOnClickListener {
            tvLog.append("\n" + "brand: " + DeviceUtils.getBrand())
        }
    }

}

object DeviceUtils {

    fun getDeviceId(context: Context): String {
        return try {
            val telephonyManager =
                context.getSystemService(Service.TELEPHONY_SERVICE) as? TelephonyManager
            telephonyManager?.deviceId ?: ""
        } catch (e: Throwable) {
            e.printStackTrace()
            ""
        }
    }

    fun getBrand(): String {
        return Build.BRAND
    }

}
```

以上两个隐私行为在字节码层面上属于不同的字节码指令：

- MethodInsnNode。属于调用方法的指令，对应 `TelephonyManager.getDeviceId()` 
- FieldInsnNode。属于调用成员变量的指令，对应 `Build.BRAND`

静态扫描要做的，就是在编译阶段遍历所有的 Class 文件，只要判断到文件中包含这两个字节码指令，就将 **隐私操作所在的类路径、调用者的方法签名信息、隐私操作的签名信息** 记录到文本中

首先，通过对比签名信息来判断是否属于目标指令。例如，Build.BRAND 所在的类路径是 `"android.os.Build"`，该字段是 String 类型，也即 `Ljava/lang/String;`，字段名也即 `BRAND`，通过比对这三个属性来识别是否是目标指令

```kotlin
private fun AbstractInsnNode.isHookPoint(): Boolean {
    when (this) {
        is MethodInsnNode -> {
            if (owner == "android/telephony/TelephonyManager"
                && name == "getDeviceId"
                && desc == "()Ljava/lang/String;"
            ) {
                return true
            }
        }
        is FieldInsnNode -> {
            if (owner == "android/os/Build" 
                && name == "BRAND" 
                && desc == "Ljava/lang/String;") {
                return true
            }
        }
    }
    return false
}
```

找到目标指令后，就将 **隐私操作所在的类路径、调用者的方法签名信息、隐私操作的签名信息** 拼接在一起

```kotlin
private fun getLintLog(
    classNode: ClassNode,
    methodNode: MethodNode,
    hokeInstruction: AbstractInsnNode
): StringBuilder {
    val classPath = classNode.name
    val methodName = methodNode.name
    val methodDesc = methodNode.desc
    val owner: String
    val desc: String
    val name: String
    when (hokeInstruction) {
        is MethodInsnNode -> {
            owner = hokeInstruction.owner
            desc = hokeInstruction.desc
            name = hokeInstruction.name
        }
        is FieldInsnNode -> {
            owner = hokeInstruction.owner
            desc = hokeInstruction.desc
            name = hokeInstruction.name
        }
        else -> {
            throw RuntimeException("非法指令")
        }
    }
    val lintLog = StringBuilder()
    lintLog.append(classPath)
    lintLog.append("  ->  ")
    lintLog.append(methodName)
    lintLog.append("  ->  ")
    lintLog.append(methodDesc)
    lintLog.append("\n")
    lintLog.append(owner)
    lintLog.append("  ->  ")
    lintLog.append(name)
    lintLog.append("  ->  ")
    lintLog.append(desc)
    return lintLog
}
```

在 Transform 阶段，每当拿到一个 Class 文件后，就扫描当中是否包含有目标指令，有的话就将相关信息写入到文本文件中，并保存到桌面

```kotlin
class PrivacySentryTransform : BaseTransform() {

    private val allLintLog = StringBuffer()

    override fun modifyClass(byteArray: ByteArray): ByteArray {
        val classNode = ClassNode()
        val classReader = ClassReader(byteArray)
        classReader.accept(classNode, ClassReader.EXPAND_FRAMES)
        val methods = classNode.methods
        if (!methods.isNullOrEmpty()) {
            val tempLintLog = StringBuilder()
            for (methodNode in methods) {
                val instructions = methodNode.instructions
                val instructionIterator = instructions?.iterator()
                if (instructionIterator != null) {
                    while (instructionIterator.hasNext()) {
                        val instruction = instructionIterator.next()
                        //判断是否是目标指令
                        if (instruction.isHookPoint()) {
                            val lintLog = getLintLog(
                                classNode,
                                methodNode,
                                instruction
                            )
                            tempLintLog.append(lintLog)
                            tempLintLog.append("\n\n")
                        }
                    }
                }
            }
            if (tempLintLog.isNotBlank()) {
                allLintLog.append(tempLintLog)
            }
        }
        return byteArray
    }

    override fun onTransformEnd() {
        if (allLintLog.isNotEmpty()) {
            //将扫描结果保存到文本文件中
            FileUtils.write(generateLogFile(), allLintLog, Charset.defaultCharset())
        }
    }

    private fun generateLogFile(): File {
        val time = SimpleDateFormat(
            "yyyy_MM_dd_HH_mm_ss",
            Locale.CHINA
        ).format(Date(System.currentTimeMillis()))
        //将文件保存到桌面
        return File(
            FileSystemView.getFileSystemView().homeDirectory,
            "PrivacySentry_${time}.txt"
        )
    }
    
    ···

}
```

当 Transform 结束后，就会在桌面上生成一个文本文件了，当中就记录了 DeviceUtils 中的两个隐私操作行为。此外，由于项目中我同时引入了 `androidx.core:core:1.7.0`，所以也会把 TelephonyManagerCompat 给扫描出来，可以看到该类当中包含的 `String getImei(TelephonyManager)` 方法也会去获取 Device ID

![](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/aec2f676c0a241f19f0e21cf33aa85f5~tplv-k3u1fbpfcp-zoom-1.image)

需要注意，由于 PrivacySentryTransform 支持增量编译，所以并不是每次编译都会输出日志文件，且只有当全量编译时才能够输出完整的记录报告

# 动态记录

动态记录要做的是：在应用运行时记录每次触发隐私行为的时间点和调用链信息，根据触发时间来判断该隐私行为是否过量执行，根据调用链信息来辅助判断具体是哪一块业务需要来获取隐私数据。这就要求我们除了要读取 Class 文件外，还要对其进行修改，插入一些日志信息和逻辑代码

以 `getBrand()` 方法为例，动态记录要做到的就是：在方法被调用时，自动生成日志信息并将日志写入到应用内部的某个文件中，日志中要包含调用的时间点、 `Build.BRAND`的签名信息、`getBrand()` 方法的调用链

```kotlin
object DeviceUtils {

    //插桩前
    fun getBrand(): String {
        return Build.BRAND
    }

    //插桩后
    fun getBrand(): String {
        val invokeTime = "xxx"
        val methodDesc = "xxx"
        val stackTrace = "xxx"
        writeToFile(invokeTime + methodDesc + stackTrace)
        return Build.BRAND
    }

}
```

`Build.BRAND` 的签名信息可以复用静态扫描时得到的数据，方法调用的时间点则可以在将信息写入到文件时再来确定，相对麻烦一点的是要来获取 `getBrand()` 方法的调用链

在本文给出的示例代码中，`getBrand()` 方法是由 Activity 中的一个 Button 来直接触发执行的，这个调用链层级还比较少，而现实中调用关系往往没那么简单，A 调用了 B、B 调用了 C、C 调用了 D，D 再来调用 DeviceUtils，这样连锁嵌套 N 个文件也很常见。因此为 `getBrand()` 方法生成调用链信息是很有必要的

我们可以依靠 Throwable 来实现该目的。我们知道，当应用抛出异常时，异常信息中就会包含具体的异常点，以及导致该异常的连锁调用链。我们可以根据该思路，在 `getBrand()` 方法中主动生成一个 Throwable 对象并获取其堆栈信息，从而来间接获取方法的调用链

大致效果就如下所示，在 Transform 阶段，自动为 DeviceUtils 生成一个 `writeToFile()`方法，并在 Build.BRAND 被调用前执行 `writeToFile()` 方法，以此来实现动态记录

```kotlin
object DeviceUtils {

    //插桩前
    fun getBrand(): String {
        return Build.BRAND
    }

}


object DeviceUtils {

    //插桩后
    fun getBrand(): String {
        val methodDesc = "xxx"
        writeToFile(methodDesc, Throwable())
        return Build.BRAND
    }

    private fun writeToFile(log: String, throwable: Throwable) {
        val byteArrayOutputStream = ByteArrayOutputStream()
        throwable.printStackTrace(PrintStream(byteArrayOutputStream))
        val stackTrace = byteArrayOutputStream.toString()
        val realLog = log + stackTrace
        //将 realLog 写入到文本中
        PrivacySentryRecord.writeToFile(realLog)
    }

}
```

有了实现思路后，后续的编码就很简单了，分为两步：

- 为每个包含隐私行为的类自动生成 `writeToFile(log: String, throwable: Throwable)` 方法
- 在每个代表隐私行为的字节码指令之前插入一个方法调用指令，该指令就负责去调用 `writeToFile`方法

在静态扫描的基础上，过 `generateWriteToFileMethod` 方法来生成 `writeToFile` 方法，每当扫描到一个目标指令后，再通过 `insertRuntimeLog` 方法来向其插入调用 `writeToFile` 方法的指令

```kotlin
class PrivacySentryTransform(private val config: PrivacySentryConfig) : BaseTransform() {

    companion object {

        private const val writeToFileMethodName = "writeToFile"

        private const val writeToFileMethodDesc = "(Ljava/lang/String;Ljava/lang/Throwable;)V"

    }

    private val allLintLog = StringBuffer()

    override fun modifyClass(byteArray: ByteArray): ByteArray {
        val classNode = ClassNode()
        val classReader = ClassReader(byteArray)
        classReader.accept(classNode, ClassReader.EXPAND_FRAMES)
        val methods = classNode.methods
        if (!methods.isNullOrEmpty()) {
            val taskList = mutableListOf<() -> Unit>()
            val tempLintLog = StringBuilder()
            for (methodNode in methods) {
                val instructions = methodNode.instructions
                val instructionIterator = instructions?.iterator()
                if (instructionIterator != null) {
                    while (instructionIterator.hasNext()) {
                        val instruction = instructionIterator.next()
                        if (instruction.isHookPoint()) {
                            val lintLog = getLintLog(
                                classNode,
                                methodNode,
                                instruction
                            )
                            tempLintLog.append(lintLog)
                            tempLintLog.append("\n\n")

                            if (mRuntimeRecord != null) {
                                taskList.add {
                                    //在 instruction 指令之前，插入调用 writeToFile 的指令
                                    insertRuntimeLog(
                                        classNode,
                                        methodNode,
                                        instruction
                                    )
                                }
                            }
                            Log.log(
                                "PrivacySentryTransform $lintLog"
                            )
                        }
                    }
                }
            }
            if (tempLintLog.isNotBlank()) {
                allLintLog.append(tempLintLog)
            }
            if (taskList.isNotEmpty() && mRuntimeRecord != null) {
                taskList.forEach {
                    it.invoke()
                }
                val classWriter = ClassWriter(ClassWriter.COMPUTE_MAXS)
                classNode.accept(classWriter)
                //生成 writeToFile 方法
                generateWriteToFileMethod(classWriter, mRuntimeRecord)
                return classWriter.toByteArray()
            }
        }
        return byteArray
    }
    
    ···

}
```

最终，每当 DeviceUtils 中的两个方法被执行时，在 `externalCacheDir`目录下就会自动生成一个 PrivacySentry.txt 文件，当中就记录了隐私方法的具体调用时间和调用链。根据该调用链，我们就可以快速定位是哪一块业务执行了敏感操作

![](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/51f3920174ed49049874b9684361f262~tplv-k3u1fbpfcp-zoom-1.image)

# TODO

静态扫描 + 动态记录 已经能够排查出大部分的隐私合规问题了，但并不是很保险，因为随着项目更迭，随时可能有新的隐私安全问题被引入进来，而如果每次发版前都要重新走一遍上述流程来排查是否存在问题的话，也是很麻烦

最保险的做法就是通过 hook 来动态代理隐私行为。我们的应用一般都是会通过一个标记位来记录用户是否已经同意过隐私协议，我们可以在每次获取敏感数据前均先判断该标记位，如果用户还未同意隐私协议的话就直接返回空数据，否则才去真正执行操作。这样才能保证隐私合规的绝对安全

```kotlin
object DeviceUtils {

    //插桩前
    fun getBrand(): String {
        return Build.BRAND
    }

    //插桩后
    fun getBrand(): String {
        if (!UserCache.serviceAgree){
            return ""
        }
        return Build.BRAND
    }

}
```

# 源码

最后也给出完整的源码：[asm-samples](https://github.com/leavesCZY/asm-samples)

ASM 的更多实践场景看这里：

- [ASM 字节码插桩：实现双击防抖](https://juejin.cn/post/7042328862872567838)
- [ASM 字节码插桩：进行线程整治](https://juejin.cn/post/7044339202997092383)