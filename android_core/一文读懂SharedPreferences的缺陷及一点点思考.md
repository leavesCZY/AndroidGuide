> 公众号：[字节数组](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/adbc507fc3704fd8955aae739a433db2~tplv-k3u1fbpfcp-zoom-1.image)
>
> 希望对你有所帮助 🤣🤣

SharedPreferences 是系统提供的一个适合用于存储少量键值对数据的持久化存储方案，结构简单，使用方便，很多应用都会使用到。另一方面，SharedPreferences 存在的问题也挺多的，当中 ANR 问题就屡见不鲜，字节跳动技术团队就曾经发布过一篇文章专门来阐述该问题：[剖析 SharedPreference apply 引起的 ANR 问题](https://mp.weixin.qq.com/s?__biz=MzI1MzYzMjE0MQ==&mid=2247484387&idx=1&sn=e3c8d6ef52520c51b5e07306d9750e70&scene=21#wechat_redirect)。到了现在，Google Jetpack 也推出了一套新的持久化存储方案：DataStore，大有取代 SharedPreferences 的趋势

本文就结合源码来剖析 SharedPreferences 存在的缺陷以及背后的具体原因，基于 SDK 30 进行分析，让读者做到知其然也知其所以然，并在最后介绍下我个人的一种存储机制设计方案，希望对你有所帮助 🤣🤣

# 不得不说的坑

## 会一直占用内存

SharedPreferences 本身是一个接口，具体的实现类是 SharedPreferencesImpl，Context 中各个和 SP 相关的方法都是由 ContextImpl 来实现的。我们项目中的每个 SP 或多或少都是保存着一些键值对，而每当我们获取到一个 SP 对象时，其对应的数据就会一直被保留在内存中，直到应用进程被终结，因为每个 SP 对象都被系统作为静态变量缓存起来了，对应 ContextImpl 中的静态变量 `sSharedPrefsCache`

```java
class ContextImpl extends Context {
    
    //先根据应用包名缓存所有 SharedPreferences
    //再根据 xmlFile 和具体的 SharedPreferencesImpl 对应上
    private static ArrayMap<String, ArrayMap<File, SharedPreferencesImpl>> sSharedPrefsCache;

    //根据 fileName 拿到对应的 xmlFile
    private ArrayMap<String, File> mSharedPrefsPaths;

}
```

每个 SP 都对应一个本地磁盘中的 xmlFile，fileName 则是由开发者来显式指定的，每个 xmlFile 都对应一个 SharedPreferencesImpl。所以 ContextImpl 的逻辑是先根据 fileName 拿到 xmlFile，再根据 xmlFile 拿到 SharedPreferencesImpl，最终应用内所有的 SharedPreferencesImpl 就都会被缓存在 `sSharedPrefsCache` 这个静态变量中。此外，由于 SharedPreferencesImpl 在初始化后就会自动去加载 xmlFile 中的所有键值对数据，而 ContextImpl 内部并没有看到有清理 `sSharedPrefsCache` 缓存的逻辑，所以 `sSharedPrefsCache` 会被一直保留在内存中直到进程终结，其内存大小会随着我们引用到的 SP 增多而加大，这就可能会持续占用很大一块内存空间

```java
@Override
public SharedPreferences getSharedPreferences(String name, int mode) {
    ···
    File file;
    synchronized (ContextImpl.class) {
        if (mSharedPrefsPaths == null) {
            mSharedPrefsPaths = new ArrayMap<>();
        }
        file = mSharedPrefsPaths.get(name);
        if (file == null) {
            file = getSharedPreferencesPath(name);
            mSharedPrefsPaths.put(name, file);
        }
    }
    return getSharedPreferences(file, mode);
}

@Override
public SharedPreferences getSharedPreferences(File file, int mode) {
    SharedPreferencesImpl sp;
    synchronized (ContextImpl.class) {
        final ArrayMap<File, SharedPreferencesImpl> cache = getSharedPreferencesCacheLocked();
        sp = cache.get(file);
        if (sp == null) {
            ···
            sp = new SharedPreferencesImpl(file, mode);
            cache.put(file, sp);
            return sp;
        }
    }
    ···
    return sp;
}

@GuardedBy("ContextImpl.class")
private ArrayMap<File, SharedPreferencesImpl> getSharedPreferencesCacheLocked() {
    if (sSharedPrefsCache == null) {
        sSharedPrefsCache = new ArrayMap<>();
    }
    final String packageName = getPackageName();
    ArrayMap<File, SharedPreferencesImpl> packagePrefs = sSharedPrefsCache.get(packageName);
    if (packagePrefs == null) {
        packagePrefs = new ArrayMap<>();
        sSharedPrefsCache.put(packageName, packagePrefs);
    }
    return packagePrefs;
}
```

## getValue 可能导致线程阻塞

SharedPreferencesImpl 在构造函数中直接就启动了一个子线程去加载磁盘文件，这意味着该操作是一个异步操作（~~我好像在说废话~~），如果文件很大或者线程调度系统没有马上启动该线程的话，那么该操作就需要一小段时间后才能执行完毕

```java
final class SharedPreferencesImpl implements SharedPreferences {
    
    @UnsupportedAppUsage
    SharedPreferencesImpl(File file, int mode) {
        mFile = file;
        mBackupFile = makeBackupFile(file);
        mMode = mode;
        mLoaded = false;
        mMap = null;
        mThrowable = null;
        startLoadFromDisk();
    }
    
    @UnsupportedAppUsage
    private void startLoadFromDisk() {
        synchronized (mLock) {
            mLoaded = false;
        }
        new Thread("SharedPreferencesImpl-load") {
            public void run() {
                //加载磁盘文件
                loadFromDisk();
            }
        }.start();
    }
    
}
```

而如果我们在初始化 SharedPreferencesImpl 后紧接着就去 getValue 的话，势必也需要确保子线程已经加载完成后才去进行取值操作，所以 SharedPreferencesImpl 就通过在每个 getValue 方法中调用 `awaitLoadedLocked()`方法来判断是否需要阻塞外部线程，确保取值操作一定会在子线程执行完毕后才执行。`loadFromDisk()`方法会在任务执行完毕后调用 `mLock.notifyAll()`唤醒所有被阻塞的线程

```java
@Override
@Nullable
public String getString(String key, @Nullable String defValue) {
    synchronized (mLock) {
        //判断是否需要让外部线程等待
        awaitLoadedLocked();
        String v = (String)mMap.get(key);
        return v != null ? v : defValue;
    }
}

@GuardedBy("mLock")
private void awaitLoadedLocked() {
    if (!mLoaded) {
        BlockGuard.getThreadPolicy().onReadFromDisk();
    }
    while (!mLoaded) {
        try {
            //还未加载线程，让外部线程暂停等待
            mLock.wait();
        } catch (InterruptedException unused) {
        }
    }
    if (mThrowable != null) {
        throw new IllegalStateException(mThrowable);
    }
}

private void loadFromDisk() {
    ···
    synchronized (mLock) {
        mLoaded = true;
        mThrowable = thrown;
        try {
            if (thrown == null) {
                if (map != null) {
                    mMap = map;
                    mStatTimestamp = stat.st_mtim;
                    mStatSize = stat.st_size;
                } else {
                    mMap = new HashMap<>();
                }
            }
        } catch (Throwable t) {
            mThrowable = t;
        } finally {
            //唤醒所有被阻塞的线程
            mLock.notifyAll();
        }
    }
}
```

所以说，如果 SP 存储的数据量很大的话，那么就有可能导致外部的调用者线程被阻塞，严重时甚至可能导致 ANR。当然，这种可能性也只是发生在加载磁盘文件完成之前，当加载完成后 `awaitLoadedLocked()`方法自然不会阻塞线程

## getValue 不保证数据类型安全

以下代码在编译阶段是完全正常的，但在运行时就会抛出异常：`java.lang.ClassCastException: java.lang.Integer cannot be cast to java.lang.String`。很明显，这是由于同个 key 先后对应了不同数据类型导致的，SharedPreferences 没有办法对这种操作做出限制，完全需要依赖于开发者自己的代码规范来进行限制

```kotlin
val sharedPreferences: SharedPreferences = getSharedPreferences("UserInfo", Context.MODE_PRIVATE)
val key = "userName"
val edit = sharedPreferences.edit()
edit.putInt(key, 11)
edit.apply()
val name = sharedPreferences.getString(key, "")
```

## 不支持多进程数据共享

在获取 SP 实例的时候需要传入一个 int 类型的 mode 标记位参数，存在一个和多进程相关的标记位 MODE_MULTI_PROCESS，该标记位能起到一定程度的多进程数据同步的保障，但作用不大，且并不保证多进程并发安全性

```kotlin
val sharedPreferences: SharedPreferences = getSharedPreferences("UserInfo", Context.MODE_MULTI_PROCESS)
```

上文有讲到，SharedPreferencesImpl 在被加载后就会一直保留在内存中，之后每次获取都是直接使用缓存数据，通常情况下也不会再次去加载磁盘文件。而 MODE_MULTI_PROCESS 起到的作用就是每当再一次去获取 SP 实例时，会判断当前磁盘文件相对最后一次内存修改是否被改动过了，如果是的话就主动去重新加载磁盘文件，从而可以做到在多进程环境下一定的数据同步

但是，这种同步本身作用不大，因为即使此时重新加载磁盘文件了，后续修改 SP 值时不同进程中的内存数据也不会实时同步，且多进程同时修改 SP 值也存在数据丢失和数据覆盖的可能。所以说，SP 并不支持多进程数据共享，MODE_MULTI_PROCESS 也已经被废弃了，其注释也推荐使用 ContentProvider 来实现跨进程通信

```java
class ContextImpl extends Context {
    
    @Override
    public SharedPreferences getSharedPreferences(File file, int mode) {
        SharedPreferencesImpl sp;
        synchronized (ContextImpl.class) {
            ···
        }
        if ((mode & Context.MODE_MULTI_PROCESS) != 0 ||
            getApplicationInfo().targetSdkVersion < android.os.Build.VERSION_CODES.HONEYCOMB) {
            //重新去加载磁盘文件
            sp.startReloadIfChangedUnexpectedly();
        }
        return sp;
    }
    
}
```

## 不支持增量更新

我们知道，SP 提交数据的方法有两个：`commit()` 和 `apply()`，分别对应着同步修改和异步修改，而这两种方式对应的都是全量更新，SP 以文件为最小单位进行修改，即使我们只修改了一个键值对，这两个方法也会将所有键值对数据重新写入到磁盘文件中，即 SP 只支持全量更新

我们平时获取到的 Editor 对象，对应的都是 SharedPreferencesImpl 的内部类 EditorImpl。EditorImpl 的每个 putValue 方法都会将传进来的 key-value 保存在 `mModified` 中，暂时还没有涉及任何文件改动。比较特殊的是 `remove` 和 `clear` 两个方法，`remove` 方法会将 this 作为键值对的 value，后续就通过对比 value 的相等性来知道是要移除键值对还是修改键值对，`clear` 方法则只是将 mClear 标记位置为 true

```java
public final class EditorImpl implements Editor {

    private final Object mEditorLock = new Object();

    @GuardedBy("mEditorLock")
    private final Map<String, Object> mModified = new HashMap<>();

    @GuardedBy("mEditorLock")
    private boolean mClear = false;

    @Override
    public Editor putString(String key, @Nullable String value) {
        synchronized (mEditorLock) {
            mModified.put(key, value);
            return this;
        }
    }

    @Override
    public Editor remove(String key) {
        synchronized (mEditorLock) {
            //存入当前的 EditorImpl 对象
            mModified.put(key, this);
            return this;
        }
    }

    @Override
    public Editor clear() {
        synchronized (mEditorLock) {
            mClear = true;
            return this;
        }
    }

}
```

`commit()` 和`apply()`两个方法都会通过调用 `commitToMemory()`方法拿到修改后的全量数据

`commitToMemory()`采用了 diff 算法，SP 包含的所有键值对数据都存储在 mapToWriteToDisk 中，Editor 改动到的所有键值对数据都存储在 mModified 中。如果  mClear 为 true，则会先清空 mapToWriteToDisk，然后再遍历 mModified，将 mModified 中的所有改动都同步给 mapToWriteToDisk。最终 mapToWriteToDisk 就保存了要重新写入到磁盘文件中的全量数据，SP 会根据 mapToWriteToDisk 完全覆盖掉旧的 xml 文件

```java
// Returns true if any changes were made
private MemoryCommitResult commitToMemory() {
    long memoryStateGeneration;
    boolean keysCleared = false;
    List<String> keysModified = null;
    Set<OnSharedPreferenceChangeListener> listeners = null;
    Map<String, Object> mapToWriteToDisk;
    synchronized (SharedPreferencesImpl.this.mLock) {
        // We optimistically don't make a deep copy until
        // a memory commit comes in when we're already
        // writing to disk.
        if (mDiskWritesInFlight > 0) {
            // We can't modify our mMap as a currently
            // in-flight write owns it.  Clone it before
            // modifying it.
            // noinspection unchecked
            mMap = new HashMap<String, Object>(mMap);
        }
        //拿到内存中的全量数据
        mapToWriteToDisk = mMap;
        mDiskWritesInFlight++;
        boolean hasListeners = mListeners.size() > 0;
        if (hasListeners) {
            keysModified = new ArrayList<String>();
            listeners = new HashSet<OnSharedPreferenceChangeListener>(mListeners.keySet());
        }
        synchronized (mEditorLock) {
            //用于标记最终是否改动到了 mapToWriteToDisk
            boolean changesMade = false;
            if (mClear) {
                if (!mapToWriteToDisk.isEmpty()) {
                    changesMade = true;
                    //清空所有在内存中的数据
                    mapToWriteToDisk.clear();
                }
                keysCleared = true;
                //恢复状态，避免二次修改时状态错位
                mClear = false;
            }
            for (Map.Entry<String, Object> e : mModified.entrySet()) {
                String k = e.getKey();
                Object v = e.getValue();
                // "this" is the magic value for a removal mutation. In addition,
                // setting a value to "null" for a given key is specified to be
                // equivalent to calling remove on that key.
                if (v == this || v == null) { //意味着要移除该键值对
                    if (!mapToWriteToDisk.containsKey(k)) {
                        continue;
                    }
                    mapToWriteToDisk.remove(k);
                } else { //对应修改键值对值的情况
                    if (mapToWriteToDisk.containsKey(k)) {
                        Object existingValue = mapToWriteToDisk.get(k);
                        if (existingValue != null && existingValue.equals(v)) {
                            continue;
                        }
                    }
                    //只有在的确是修改了或新插入键值对的情况才需要保存值
                    mapToWriteToDisk.put(k, v);
                }
                changesMade = true;
                if (hasListeners) {
                    keysModified.add(k);
                }
            }
            //恢复状态，避免二次修改时状态错位
            mModified.clear();
            if (changesMade) {
                mCurrentMemoryStateGeneration++;
            }
            memoryStateGeneration = mCurrentMemoryStateGeneration;
        }
    }
    return new MemoryCommitResult(memoryStateGeneration, keysCleared, keysModified,
            listeners, mapToWriteToDisk);
}
```

## clear 的反直觉用法

看以下例子。按照语义分析的话，最终 SP 中应该是只剩下 blog 一个键值对才符合直觉，而实际上最终两个键值对都会被保留，且只有这两个键值对被保留下来

```kotlin
val sharedPreferences: SharedPreferences = getSharedPreferences("UserInfo", Context.MODE_PRIVATE)
val edit = sharedPreferences.edit()
edit.putString("name", "业志陈").clear().putString("blog", "https://juejin.cn/user/923245496518439")
edit.apply()
```

造成该问题的原因还需要看`commitToMemory()`方法。`clear()`会将 mClear 置为 true，所以在执行到第一步的时候就会将内存中的所有键值对数据 mapToWriteToDisk 清空。当执行到第二步的时候，mModified 中的所有数据就都会同步到 mapToWriteToDisk 中，从而导致最终 name 和 blog 两个键值对都会被保留下来，其它键值对都被移除了

所以说，`Editor.clear()` 之前不应该连贯调用 putValue 语句，这会造成理解和实际效果之间的偏差

```java
// Returns true if any changes were made
private MemoryCommitResult commitToMemory() {
    long memoryStateGeneration;
    boolean keysCleared = false;
    List<String> keysModified = null;
    Set<OnSharedPreferenceChangeListener> listeners = null;
    Map<String, Object> mapToWriteToDisk;
    synchronized (SharedPreferencesImpl.this.mLock) {
        // We optimistically don't make a deep copy until
        // a memory commit comes in when we're already
        // writing to disk.
        if (mDiskWritesInFlight > 0) {
            // We can't modify our mMap as a currently
            // in-flight write owns it.  Clone it before
            // modifying it.
            // noinspection unchecked
            mMap = new HashMap<String, Object>(mMap);
        }
        //拿到内存中的全量数据
        mapToWriteToDisk = mMap;
        mDiskWritesInFlight++;
        boolean hasListeners = mListeners.size() > 0;
        if (hasListeners) {
            keysModified = new ArrayList<String>();
            listeners = new HashSet<OnSharedPreferenceChangeListener>(mListeners.keySet());
        }
        synchronized (mEditorLock) {
            boolean changesMade = false;
            if (mClear) { //第一步
                if (!mapToWriteToDisk.isEmpty()) {
                    changesMade = true;
                    //清空所有在内存中的数据
                    mapToWriteToDisk.clear();
                }
                keysCleared = true;
                //恢复状态，避免二次修改时状态错位
                mClear = false;
            }
            for (Map.Entry<String, Object> e : mModified.entrySet()) { //第二步
                String k = e.getKey();
                Object v = e.getValue();
                // "this" is the magic value for a removal mutation. In addition,
                // setting a value to "null" for a given key is specified to be
                // equivalent to calling remove on that key.
                if (v == this || v == null) { //意味着要移除该键值对
                    if (!mapToWriteToDisk.containsKey(k)) {
                        continue;
                    }
                    mapToWriteToDisk.remove(k);
                } else { //对应修改键值对值的情况
                    if (mapToWriteToDisk.containsKey(k)) {
                        Object existingValue = mapToWriteToDisk.get(k);
                        if (existingValue != null && existingValue.equals(v)) {
                            continue;
                        }
                    }
                    //只有在的确是修改了或新插入键值对的情况才需要保存值
                    mapToWriteToDisk.put(k, v);
                }
                changesMade = true;
                if (hasListeners) {
                    keysModified.add(k);
                }
            }
            //恢复状态，避免二次修改时状态错位
            mModified.clear();
            if (changesMade) {
                mCurrentMemoryStateGeneration++;
            }
            memoryStateGeneration = mCurrentMemoryStateGeneration;
        }
    }
    return new MemoryCommitResult(memoryStateGeneration, keysCleared, keysModified,
            listeners, mapToWriteToDisk);
}
```

## commit、applay 可能导致 ANR

`commit()` 方法会通过 `commitToMemory()` 方法拿到本次修改后的全量数据，即 MemoryCommitResult，然后向 `enqueueDiskWrite` 方法提交将全量数据写入磁盘文件的任务，在写入完成前调用者线程都会由于 CountDownLatch 一直阻塞等待着，方法返回值即本次修改操作的成功状态

```java
@Override
public boolean commit() {
    long startTime = 0;
    if (DEBUG) {
        startTime = System.currentTimeMillis();
    }
   //拿到修改后的全量数据
    MemoryCommitResult mcr = commitToMemory();
   //提交写入磁盘文件的任务
    SharedPreferencesImpl.this.enqueueDiskWrite(
        mcr, null /* sync write on this thread okay */);
    try {
        //阻塞等待，直到 xml 文件写入完成（不管成功与否）
        mcr.writtenToDiskLatch.await();
    } catch (InterruptedException e) {
        return false;
    } finally {
        if (DEBUG) {
            Log.d(TAG, mFile.getName() + ":" + mcr.memoryStateGeneration
                    + " committed after " + (System.currentTimeMillis() - startTime)
                    + " ms");
        }
    }
    notifyListeners(mcr);
    return mcr.writeToDiskResult;
}
```

`enqueueDiskWrite` 方法就是包含了具体的磁盘写入逻辑的地方了，由于外部可能存在多个线程在同时执行 `apply()` 和 `commit()` 两个方法，而对应的磁盘文件只有一个，所以 `enqueueDiskWrite` 方法就必须保证写入操作的有序性，避免数据丢失或者覆盖，甚至是文件损坏

`enqueueDiskWrite` 方法的具体逻辑：

1. writeToDiskRunnable 使用到了内部锁 mWritingToDiskLock 来保证 writeToFile 操作的有序性，避免多线程竞争
2. 对于 commit 操作，如果当前只有一个线程在执行提交修改的操作的话，那么直接在该线程上执行 writeToDiskRunnable，流程结束
3. 对于其他情况（apply 操作、多线程同时 commit 或者 apply），都会将 writeToDiskRunnable 提交给 QueuedWork 执行
4. QueuedWork 内部使用到了 HandlerThread 来执行 writeToDiskRunnable，HandlerThread 本身也可以保证多个任务执行时的有序性

```java
private void enqueueDiskWrite(final MemoryCommitResult mcr,
                              final Runnable postWriteRunnable) {
    final boolean isFromSyncCommit = (postWriteRunnable == null);
    final Runnable writeToDiskRunnable = new Runnable() {
            @Override
            public void run() {
                synchronized (mWritingToDiskLock) {
                    //写入磁盘文件
                    writeToFile(mcr, isFromSyncCommit);
                }
                synchronized (mLock) {
                    mDiskWritesInFlight--;
                }
                if (postWriteRunnable != null) {
                    postWriteRunnable.run();
                }
            }
        };
    // Typical #commit() path with fewer allocations, doing a write on
    // the current thread.
    if (isFromSyncCommit) { //commit() 方法会走进这里面
        boolean wasEmpty = false;
        synchronized (mLock) {
            wasEmpty = mDiskWritesInFlight == 1;
        }
        if (wasEmpty) {
            //wasEmpty 为 true 说明当前只有一个线程在执行提交操作，那么就直接在此线程上完成任务
            writeToDiskRunnable.run();
            return;
        }
    }
    QueuedWork.queue(writeToDiskRunnable, !isFromSyncCommit);
}
```

此外，还有一个比较重要的知识点需要注意下。在 writeToFile 方法中会对本次任务进行校验，避免连续多次执行无效的磁盘任务。当中，mDiskStateGeneration 代表的是最后一次成功写入磁盘文件时的任务版本号，mCurrentMemoryStateGeneration 是当前内存中最新的修改记录版本号，mcr.memoryStateGeneration 是本次要执行的任务的版本号。通过两次版本号的对比，就避免了在连续多次 commit 或者 apply 时造成重复执行 I/O 操作的情况，而是只会执行最后一次，避免了无效的 I/O 任务

```java
@GuardedBy("mWritingToDiskLock")
private void writeToFile(MemoryCommitResult mcr, boolean isFromSyncCommit) {
    ···
    if (fileExists) {
        boolean needsWrite = false;

        // Only need to write if the disk state is older than this commit
        //判断版本号
        if (mDiskStateGeneration < mcr.memoryStateGeneration) {
            if (isFromSyncCommit) {
                needsWrite = true;
            } else {
                synchronized (mLock) {
                    // No need to persist intermediate states. Just wait for the latest state to
                    // be persisted.
                    //判断版本号
                    if (mCurrentMemoryStateGeneration == mcr.memoryStateGeneration) {
                        needsWrite = true;
                    }
                }
            }
        }

        if (!needsWrite) {
            //当前版本号并非最新，无需执行，直接返回即可
            mcr.setDiskWriteResult(false, true);
            return;
        }
    ···
}
```

再回过头看 `commit()` 方法。不管该方法关联的 writeToDiskRunnable 最终是在本线程还是 HandlerThread 中执行，`await()`方法都会使得本线程阻塞等待直到 writeToDiskRunnable 执行完毕，从而实现了 `commit()`同步提交的效果

```java
@Override
public boolean commit() {
    long startTime = 0;
    if (DEBUG) {
        startTime = System.currentTimeMillis();
    }
   //拿到修改后的全量数据
    MemoryCommitResult mcr = commitToMemory();
   //提交写入磁盘文件的任务
    SharedPreferencesImpl.this.enqueueDiskWrite(
        mcr, null /* sync write on this thread okay */);
    try {
        //阻塞等待，直到 xml 文件写入完成（不管成功与否）
        mcr.writtenToDiskLatch.await();
    } catch (InterruptedException e) {
        return false;
    } finally {
        if (DEBUG) {
            Log.d(TAG, mFile.getName() + ":" + mcr.memoryStateGeneration
                    + " committed after " + (System.currentTimeMillis() - startTime)
                    + " ms");
        }
    }
    notifyListeners(mcr);
    return mcr.writeToDiskResult;
}
```

而对于 `apply()` 方法，其本身具有异步提交的含义，I/O 操作应该都是交由给了子线程来执行才对，按道理来说只需要调用 `enqueueDiskWrite` 方法提交任务且不等待任务完成即可，可实际上`apply()`方法反而要比`commit()`方法复杂得多

`apply()`方法包含一个 awaitCommit 任务，用于阻塞其执行线程直到磁盘任务执行完毕，而 awaitCommit 又被包裹在 postWriteRunnable 中一起提交给了 `enqueueDiskWrite` 方法，`enqueueDiskWrite` 方法又会在 writeToDiskRunnable 执行完毕后执行 enqueueDiskWrite

```java
@Override
public void apply() {
    final long startTime = System.currentTimeMillis();

    final MemoryCommitResult mcr = commitToMemory();
    final Runnable awaitCommit = new Runnable() {
            @Override
            public void run() {
                try {
                    //阻塞线程直到磁盘任务执行完毕
                    mcr.writtenToDiskLatch.await();
                } catch (InterruptedException ignored) {
                }

                if (DEBUG && mcr.wasWritten) {
                    Log.d(TAG, mFile.getName() + ":" + mcr.memoryStateGeneration
                            + " applied after " + (System.currentTimeMillis() - startTime)
                            + " ms");
                }
            }
        };

    QueuedWork.addFinisher(awaitCommit);

    Runnable postWriteRunnable = new Runnable() {
            @Override
            public void run() {
                awaitCommit.run();
                QueuedWork.removeFinisher(awaitCommit);
            }
        };

    //提交任务
    SharedPreferencesImpl.this.enqueueDiskWrite(mcr, postWriteRunnable);

    // Okay to notify the listeners before it's hit disk
    // because the listeners should always get the same
    // SharedPreferences instance back, which has the
    // changes reflected in memory.
    notifyListeners(mcr);
}
```

单独看以上逻辑会显得十分奇怪，从上文就可以得知 writeToDiskRunnable 最终是会交由 HandlerThread 来执行的，那按照流程看 awaitCommit 最终也是会由 HandlerThread 调用，那么 awaitCommit 的等待操作就显得十分奇怪了，因为 awaitCommit 肯定是会在磁盘任务执行完毕才被调用，就相当于 HandlerThread  在自己等待自己执行完毕。此外，HandlerThread 属于子线程，按道理来说子线程即使执行了耗时操作也不会导致主线程 ANR 才对

要理解以上操作，还需要再看看 ActivityThread 这个类。当 Service 和 Activity 的生命周期处于 `handleStopService()` 、`handlePauseActivity()` 、`handleStopActivity()` 的时候，ActivityThread 会调用 `QueuedWork.waitToFinish()` 方法

```java
private void handleStopService(IBinder token) {
    Service s = mServices.remove(token);
    if (s != null) {
        try {
            ···
            //重点
            QueuedWork.waitToFinish();
            ···
        } catch (Exception e) {
            ···
        }
    } else {
        Slog.i(TAG, "handleStopService: token=" + token + " not found.");
    }
    //Slog.i(TAG, "Running services: " + mServices);
}
```

`QueuedWork.waitToFinish()` 方法会主动去执行所有的磁盘写入任务，并执行所有的 postWriteRunnable，这就造成了 Activity 或 Service 在切换生命周期的过程中有可能因为存在大量的磁盘写入任务而被阻塞住，最终导致 ANR

```java
public static void waitToFinish() {
    long startTime = System.currentTimeMillis();
    boolean hadMessages = false;
    Handler handler = getHandler();
    synchronized (sLock) {
        if (handler.hasMessages(QueuedWorkHandler.MSG_RUN)) {
            // Delayed work will be processed at processPendingWork() below
            handler.removeMessages(QueuedWorkHandler.MSG_RUN);
            if (DEBUG) {
                hadMessages = true;
                Log.d(LOG_TAG, "waiting");
            }
        }
        // We should not delay any work as this might delay the finishers
        sCanDelay = false;
    }
    StrictMode.ThreadPolicy oldPolicy = StrictMode.allowThreadDiskWrites();
    try {
        //执行所有的磁盘写入任务
        processPendingWork();
    } finally {
        StrictMode.setThreadPolicy(oldPolicy);
    }
    try {
        //执行所有的 postWriteRunnable
        while (true) {
            Runnable finisher;
            synchronized (sLock) {
                finisher = sFinishers.poll();
            }
            if (finisher == null) {
                break;
            }
            finisher.run();
        }
    } finally {
        sCanDelay = true;
    }
    synchronized (sLock) {
        long waitTime = System.currentTimeMillis() - startTime;
        if (waitTime > 0 || hadMessages) {
            mWaitTimes.add(Long.valueOf(waitTime).intValue());
            mNumWaits++;
            if (DEBUG || mNumWaits % 1024 == 0 || waitTime > MAX_WAIT_TIME_MILLIS) {
                mWaitTimes.log(LOG_TAG, "waited: ");
            }
        }
    }
}
```

ActivityThread 为什么要主动去触发执行所有的磁盘写入任务我无从得知，字节技术跳动团队给出的猜测是：**Google 在 Activity 和 Service 调用 onStop 之前阻塞主线程来处理 SP，我们能猜到的唯一原因是尽可能的保证数据的持久化。因为如果在运行过程中产生了 crash，也会导致 SP 未持久化，持久化本身是 IO 操作，也会失败**

综上所述，由于 SP 本身只支持全量更新，如果 SP 文件很大，即使是小数据量的 `apply/commit` 操作也有可能导致 ANR

# 正反面

SharedPreferencesImpl 在不同的系统版本中有着比较大的差别，例如 writeToFile 方法对于任务版本号的校验也是从 8.0 系统开始的，在 8.0 系统之前对于连续的 commit 和 apply 每次都会触发 I/O 操作，所以在 8.0 系统之前 ANR 问题会更加容易复现。我们需要根据系统版本来看待以上列举出来的各个缺陷

需要强调的是，SP 本身的定位是**轻量级数据存储**，设计初衷是用于存储**简单的数据结构**（基本数据类型），且提供了按模块分区存储的功能。如果开发者能够严格遵守这一个规范的话，那么其实以上所述的很多“缺陷”都是可以避免的。而 SP 之所以现在看起来问题很多，也是因为如今大部分应用的业务比以前复杂太多了，有些时候为了方便就直接用来存储非常复杂的数据结构，或者是没有做好数据分区存储，导致单个文件过大，这才是造成问题的主要原因

# 如何做好持久化

以下的示例代码估计是很多开发者的噩梦

```kotlin
val sharedPreference = getSharedPreferences("user_preference", Context.MODE_PRIVATE)
val name = sharedPreference.getString("name", "")
```

以上代码存在什么问题呢？我觉得至少有五点：

- 强引用到了 SP，导致后续需要切换存储库时需要全局搜索替换，工作量非常大
- key 值难维护，每次获取 value 时都需要显式声明 key 值
- 可读性差，键值对的含义基本只能靠 key 值进行表示
- 只支持基本数据类型，在存取自定义数据类型时存在很多重复工作。要向 SP 存入自定义的 JavaBean 对象时，只能将 Bean 对象转为 Json 字符串后存入 SP，在取值时再手动反序列化
- 数据类型不明确，基本只能靠注释来引导开发者使用正确的数据类型

开发者往往是会声明出各种 SpUtils 类进行多一层封装，但也没法彻底解决以上问题。SP 的确是存在着一些设计缺陷，但对于大部分应用开发者来说其实并没有多少选择，我们只能选择用或者不用，并没有多少余地可以来解决或者避免其存在的问题，我们往往只能在遇到问题后切换到其它的持久化存储方案

目前有两个比较知名的持久化存储方案：Jetpack DataStore 和腾讯的 MMKV，我们当然可以选择将项目中的 SP 切换为这两个库之一，但这也不禁让人想到一个问题，如果以后这两个库也遇到了问题甚至是直接被废弃了，难道我们又需要再来全局替换一遍吗？我们应该如何设计才能使得每次的替换成本降到最低呢？

在我看来，开发者在为项目引入一个新的依赖库之前就应该为以后移除该库做好准备，做好接口隔离，屏蔽具体的底层逻辑（当然，也不是每个依赖库都可以做到）。笔者的项目之前也是使用 SP 来存储配置信息，后来我也将其切换到了 MMKV，下面就来介绍下笔者当时是如何设计存储结构避免硬编码的

## 目前的效果

我将应用内所有需要存储的键值对数据分为了三类：**用户强关联数据、应用配置数据、不可二次变更的数据**。每一类数据的存储区域各不相同，互不影响。进行数据分组的好处就在于可以根据需要来清除特定数据，例如当用户退登后我们可以只清除 UserKVHolder，而 PreferenceKVHolder 和 FinalKVHolder 则可以一直保留

IKVHolder 接口定义了基本的存取方法，MMKVKVHolder 通过 MMKV 实现了具体的存储逻辑

```kotlin
//和用户强绑定的数据，在退出登录时需要全部清除，例如 UserBean
//设置 encryptKey 以便加密存储
private val UserKVHolder: IKVHolder = MMKVKVHolder("user", "加密key")

//和用户不强关联的数据，在退出登录时无需清除，例如夜间模式、字体大小等
private val PreferenceKVHolder: IKVHolder = MMKVKVHolder("preference")

//用于存储不会二次变更只用于历史溯源的数据，例如应用首次安装的时间、版本号、版本名等
private val FinalKVHolder: IKVHolder = MMKVKVFinalHolder("final")
```

之后我们就可以利用 Kotlin 强大的语法特性来定义键值对了

例如，对于和用户强关联的数据，每个键值对都定义为 UserKV 的一个属性字段，键值对的含义和作用通过属性名来进行标识，且键值对的 key 必须和属性名保持一致，这样可以避免 key 值重复。每个 getValue 操作也都支持设置默认值。IKVHolder 内部通过 Gson 来实现序列化和反序列化，这样 UserKV 就可以直接存储 JavaBean、JavaBeanList，Map 等数据结构了

```kotlin
object UserKV : IKVHolder by UserKVHolder {

    var name: String
        get() = get("name", "")
        set(value) = set("name", value)

    var blog: String
        get() = get("blog", "")
        set(value) = set("blog", value)

    var userBean: UserBean?
        get() = getBeanOrNull("userBean")
        set(value) = set("userBean", value)

    var userBeanOfDefault: UserBean
        get() = getBeanOrDefault(
            "userBeanOfDefault",
            UserBean("业志陈", "https://juejin.cn/user/923245496518439")
        )
        set(value) = set("userBeanOfDefault", value)

    var userBeanList: List<UserBean>
        get() = getBean("userBeanList")
        set(value) = set("userBeanList", value)

    var map: Map<Int, String>
        get() = getBean("map")
        set(value) = set("map", value)

}
```

此外，我们也可以在 setValue 方法中对 value 进行校验，避免无效值

```kotlin
object UserKV : IKVHolder by UserKVHolder {

    var age: Int
        get() = get("age", 0)
        set(value) {
            if (value <= 0) {
                return
            }
            set("age", value)
        }

}
```

之后我们在存取值时，就相当于在直接读写 UserKV 的属性值，也支持动态指定 Key 进行赋值取值，在易用性和可读性上相比 SharedPreferences 都有很大的提升，且对于外部来说完全屏蔽了具体的存储实现逻辑

```kotlin
//存值
UserKV.name = "业志陈"
UserKV.blog = "https://juejin.cn/user/923245496518439"

//取值
val name = UserKV.name
val blog = UserKV.blog

//动态指定 Key 进行赋值和取值
UserKV.set("name", "业志陈")
val name = UserKV.get("name", "")
```

## 如何设计的

首先，IKVHolder 定义了基本的存取方法，除了需要支持基本数据类型外，还需要支持自定义的数据类型。依靠 Kotlin 的 **扩展函数** 和 **内联函数** 这两个语法特性，我们在存取自定义类型时都无需声明泛型类型，使用上十分简洁。JsonHolder 则是通过 Gson 实现了基本的序列化和反序列化方法

```kotlin
interface IKVHolder {

    companion object {

        inline fun <reified T> IKVHolder.getBean(key: String): T {
            return JsonHolder.toBean(get(key, ""))
        }

        inline fun <reified T> IKVHolder.getBeanOrNull(key: String): T? {
            return JsonHolder.toBeanOrNull(get(key, ""))
        }

        inline fun <reified T> IKVHolder.getBeanOrDefault(key: String, defaultValue: T): T {
            return JsonHolder.toBeanOrDefault(get(key, ""), defaultValue)
        }

        fun toJson(ob: Any?): String {
            return JsonHolder.toJson(ob)
        }

    }

    //数据分组，用于标明不同范围内的数据缓存
    val keyGroup: String

    fun verifyBeforePut(key: String, value: Any?): Boolean

    fun get(key: String, default: Int): Int

    fun set(key: String, value: Int)

    fun <T> set(key: String, value: T?)

    fun containsKey(key: String): Boolean

    fun removeKey(vararg keys: String)

    fun allKeyValue(): Map<String, Any?>

    fun clear()
    
    ···

}
```

BaseMMKVKVHolder 实现了 IKVHolder 接口，内部引入了 MMKV 作为具体的持久化存储方案

```kotlin
/**
 * @param selfGroup 用于指定数据分组，不同分组下的数据互不关联
 * @param encryptKey 加密 key，如果为空则表示不进行加密
 */
sealed class BaseMMKVKVHolder constructor(
    selfGroup: String,
    encryptKey: String
) : IKVHolder {

    final override val keyGroup: String = selfGroup

    override fun verifyBeforePut(key: String, value: Any?): Boolean {
        return true
    }

    private val kv: MMKV? = if (encryptKey.isBlank()) MMKV.mmkvWithID(
        keyGroup,
        MMKV.MULTI_PROCESS_MODE
    ) else MMKV.mmkvWithID(keyGroup, MMKV.MULTI_PROCESS_MODE, encryptKey)

    override fun set(key: String, value: Int) {
        if (verifyBeforePut(key, value)) {
            kv?.putInt(key, value)
        }
    }

    override fun <T> set(key: String, value: T?) {
        if (verifyBeforePut(key, value)) {
            if (value == null) {
                removeKey(key)
            } else {
                set(key, toJson(value))
            }
        }
    }

    override fun get(key: String, default: Int): Int {
        return kv?.getInt(key, default) ?: default
    }

    override fun containsKey(key: String): Boolean {
        return kv?.containsKey(key) ?: false
    }

    override fun removeKey(vararg keys: String) {
        kv?.removeValuesForKeys(keys)
    }

    override fun allKeyValue(): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>()
        kv?.allKeys()?.forEach {
            map[it] = getObjectValue(kv, it)
        }
        return map
    }

    override fun clear() {
        kv?.clearAll()
    }

	···

}
```

BaseMMKVKVHolder 有两个子类，其区别只在于 MMKVKVFinalHolder 保存键值对后无法再次更改值，用于存储不会二次变更只用于历史溯源的数据，例如应用首次安装时的时间戳、版本号、版本名等

```kotlin
/**
 * @param selfGroup 用于指定数据分组，不同分组下的数据互不关联
 * @param encryptKey 加密 key，如果为空则表示不进行加密
 */
class MMKVKVHolder constructor(selfGroup: String, encryptKey: String = "") :
    BaseMMKVKVHolder(selfGroup, encryptKey)

/**
 * 存储后值无法二次变更
 * @param selfGroup 用于指定数据分组，不同分组下的数据互不关联
 * @param encryptKey 加密 key，如果为空则表示不进行加密
 */
class MMKVKVFinalHolder constructor(selfGroup: String, encryptKey: String = "") :
    BaseMMKVKVHolder(selfGroup, encryptKey) {

    override fun verifyBeforePut(key: String, value: Any?): Boolean {
        return !containsKey(key)
    }

}
```

通过接口隔离，UserKV 就完全不会接触到具体的存储实现机制了，对于开发者来说也只是在读写 UserKV 的一个属性字段而已，当后续我们需要替换存储方案时，也只需要去改动 MMKVKVHolder 的内部实现即可，上层应用完全不需要进行任何改动

## KVHolder

KVHolder 的实现思路还是十分简单的，再加上 Kotlin 本身强大的语法特性就进一步提高了易用性和可读性 😇😇 我也将其发布为开源库，感兴趣的读者可以直接远程导入依赖

```groovy
allprojects {
    repositories {
        maven { url "https://jitpack.io" }
    }
}

dependencies {
    implementation 'com.github.leavesCZY:KVHolder:latest_version'
}
```

GitHub 点击这里：[KVHolder](https://github.com/leavesCZY/KVHolder)
