> 公众号：[字节数组](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/adbc507fc3704fd8955aae739a433db2~tplv-k3u1fbpfcp-zoom-1.image)
>
> 希望对你有所帮助 🤣🤣

> 本系列文章会陆续对 Java 和 Android 的集合框架（JDK 1.8，Android SDK 30）中的几个常见容器结合源码进行介绍，了解不同容器在**数据结构、适用场景、优势点**上的不同，希望对你有所帮助 🤣🤣

本篇文章再来分析下 SparseArray 和 ArrayMap 这两个 Android 系统独有的集合框架类，这两个容器在使用上类似于 HashMap，都是用于存储键值对。由于 Android 系统对于内存比较敏感，所以 SparseArray 和 ArrayMap 在内存使用方面会比较克制，这里就来分析下其实现原理和优势点

# 一、SparseArray

使用 Android Studio 的同学可能遇到过一个现象，就是如果在代码中声明了 `Map<Integer,Object>` 类型变量的话，Android Studio 会提示：`Use new SparseArray<Object>(...) instead for better performance ...`，即**用 SparseArray< Object > 性能更优，可以用来替代 HashMap**

这里就来介绍下 SparseArray 的内部原理，看看它相比 HashMap 有什么性能优势

## 1、基本概念

SparseArray 的使用方式：

```java
SparseArray<String> sparseArray = new SparseArray<>();
sparseArray.put(100,"leavesC");
sparseArray.remove(100);
sparseArray.get(100);
sparseArray.removeAt(29);
```

SparseArray< E > 相当于 Map< Integer , E > ，key 值固定为 int 类型，在初始化时只需要声明 value 的数据类型即可，其内部用两个数组分别来存储 key 和 value：`int[] mKeys ; Object[] mValues`

mKeys 和 mValues 按照如下规则对应起来：

- 假设要向 SparseArray 存入 key 为 10，value 为 200 的键值对，则先将 10 存到 mKeys 中，假设 10 在 mKeys 中对应的索引值是 2，则将 value 存入 mValues[2] 中
- mKeys 中的元素值按照递增的方法进行存储，每次存放新的键值对时都通过二分查找的方式将 key 插入到 mKeys 中
- 当要从 SparseArray 取值时，先通过二分查找法找到 key 在 mKeys 中对应的索引，然后根据该索引从 mValues 中取值

从以上可以看出来的一点就是：SparseArray 避免了 HashMap 每次存取值时的装箱拆箱操作，key 值保持为基本数据类型 int，减少了性能开销

## 2、类声明

SparseArray 本身并没有直接继承于任何类，内部也没有使用到 Java 原生的集合框架，所以 SparseArray 是 Android 系统自己实现的一个集合容器类

```java
public class SparseArray<E> implements Cloneable
```

## 3、全局变量

`mGarbage` 是 SparseArray 的一个优化点之一，用于标记**当前是否有需要垃圾回收(GC)的元素**，当该值被置为 true 时，意味着当前存在无效元素，需要进行垃圾回收，但回收操作并不会马上进行，而是在后续操作中再统一进行

```java
//键值对被移除后对应的 value 会变成此值，用来当做 GC 标记位
private static final Object DELETED = new Object();

//用于标记当前是否有待垃圾回收(GC)的元素
private boolean mGarbage = false;

private int[] mKeys;

private Object[] mValues;

//当前集合元素的数量
//该值并不一定是时时处于正确状态，因为有可能出现只删除 key 和 value 两者之一的情况
//所以 size() 方法内都会先进行 GC
private int mSize;
```

## 4、构造函数

key 数组和 value 数组的默认大小都是 10，如果在初始化时已知最终数据量的大小，则可以直接指定初始容量，这样可以避免后续的扩容操作

```java
//设置数组的默认初始容量为10
public SparseArray() {
    this(10);
}

public SparseArray(int initialCapacity) {
    if (initialCapacity == 0) {
        mKeys = EmptyArray.INT;
        mValues = EmptyArray.OBJECT;
    } else {
        mValues = ArrayUtils.newUnpaddedObjectArray(initialCapacity);
        mKeys = new int[mValues.length];
    }
    mSize = 0;
}
```

## 5、添加元素

添加元素的方法有几个，主要看 `put(int key, E value)` 方法就可以，该方法用于存入 key 和 value 组成的键值对

按照前面所说的 SparseArray 存储键值对的规则，`put` 方法会先判断当前 mKeys 中是否已经有相同的 key 值，有的话就用 value 覆盖 mValues 中的旧值。如果不存在相同 key 值，在将 key 插入到 mKeys 后需要在 mValues 的相同索引位置插入 value。由于 mKeys 是按照大小对元素值进行排序存储的，所以将 key 插入到 mKeys 可能会导致元素重新排序，从而连锁导致 mValues 也需要重新排序

`put` 方法从 mKeys 查找 key 用的是 ContainerHelpers 类提供的二分查找方法：`binarySearch`，用于获取 key 在 mKeys 中的当前索引（存在该 key 的话）或者是应该存放的位置的索引（不存在该 key），方法的返回值可以分为三种情况：

1. 如果 mKeys 中存在对应的 key，则直接返回对应的索引值
2. 如果 mKeys 中不存在对应的 key
   1. 假设 mKeys 中存在"值比 key 大且大小与 key 最接近的值的索引"为 presentIndex，则此方法的返回值为 ~presentIndex
   2. 如果 mKeys 中不存在比 key 还要大的值的话，则返回值为 ~mKeys.length

```java
// This is Arrays.binarySearch(), but doesn't do any argument validation.
static int binarySearch(int[] array, int size, int value) {
    int lo = 0;
    int hi = size - 1;
    while (lo <= hi) {
        final int mid = (lo + hi) >>> 1;
        final int midVal = array[mid];
        if (midVal < value) {
            lo = mid + 1;
        } else if (midVal > value) {
            hi = mid - 1;
        } else {
            return mid;  // value found
        }
    }
    return ~lo;  // value not present
}
```

可以看到，如果 mKeys 存在目标 key，那么返回值即对应的索引位置。如果不存在目标 key，其返回值也指向了应该让 key 存入的位置，因为当不存在目标 key 时，将计算出的索引值进行 ~ 运算后返回值一定是负数，从而与“找得到目标 key 的情况（返回值大于等于0）”的情况区分开。从这里可以看出该方法的巧妙之处，单纯的一个返回值就可以区分出多种情况，且通过这种方式来存放数据可以使得 mKeys 的内部值一直是按照值递增的方式来排序的

再来具体看看 put 方法的逻辑

```java
public void put(int key, E value) {
    //用二分查找法查找指定 key 在 mKeys 中的索引值
    int i = ContainerHelpers.binarySearch(mKeys, mSize, key);
    if (i >= 0) { //对应已经存在相同 key 的情况
        mValues[i] = value;
    } else {
        //取反，拿到真实的索引位置
        i = ~i;
        //如果目标位置还未赋值，则直接存入数据即可
        if (i < mSize && mValues[i] == DELETED) {
            mKeys[i] = key;
            mValues[i] = value;
            return;
        }
        //如果存在冗余数据，那么就先进行 GC
        if (mGarbage && mSize >= mKeys.length) {
            gc();
            //GC 后再次进行查找，因为值可能已经发生变化了
            i = ~ContainerHelpers.binarySearch(mKeys, mSize, key);
        }
        //索引 i 位置已经用于存储其它数据了，此时就需要对数组元素进行迁移
        //所以从索引 i 开始的所有数据都需要向后移动一位，并将 key 存到 mKeys[i]
        mKeys = GrowingArrayUtils.insert(mKeys, mSize, i, key);
        mValues = GrowingArrayUtils.insert(mValues, mSize, i, value);
        mSize++;
    }
}

//将索引 index 处的元素赋值为 value
//知道目标位置的话可以直接向 mValues 赋值
public void setValueAt(int index, E value) {
    if (index >= mSize && UtilConfig.sThrowExceptionForUpperArrayOutOfBounds) {
        // The array might be slightly bigger than mSize, in which case, indexing won't fail.
        // Check if exception should be thrown outside of the critical path.
        throw new ArrayIndexOutOfBoundsException(index);
    }
    //如果需要则先进行垃圾回收
    if (mGarbage) {
        gc();
    }
    mValues[index] = value;
}

//和 put 方法类似
//但在存入数据前先对数据大小进行了判断，有利于减少对 mKeys 进行二分查找的次数
//所以在“存入的 key 比现有的 mKeys 值都大”的情况下会比 put 方法性能高
public void append(int key, E value) {
    if (mSize != 0 && key <= mKeys[mSize - 1]) {
        put(key, value);
        return;
    }
    if (mGarbage && mSize >= mKeys.length) {
        gc();
    }
    mKeys = GrowingArrayUtils.append(mKeys, mSize, key);
    mValues = GrowingArrayUtils.append(mValues, mSize, value);
    mSize++;
}
```

## 6、移除元素

上文说了，布尔变量 `mGarbage` 用于标记**当前是否有待垃圾回收(GC)的元素**，当该值被置为 true 时，即意味着**当前状态需要进行垃圾回收，但回收操作并不马上进行，而是在后续操作中再完成**

以下几个方法在移除元素时，都只是切断了 mValues 对 value 的引用，而 mKeys 并没有进行回收，这个操作会留到 `gc()` 进行处理

```java
public void delete(int key) {
    //用二分查找法查找指定 key 在 mKeys 中的索引值
    int i = ContainerHelpers.binarySearch(mKeys, mSize, key);
    if (i >= 0) {
        if (mValues[i] != DELETED) {
            mValues[i] = DELETED;
            //标记当前需要进行垃圾回收
            mGarbage = true;
        }
    }
}

public void remove(int key) {
    delete(key);
}

//和 delete 方法基本相同，差别在于会返回 key 对应的元素值
public E removeReturnOld(int key) {
    int i = ContainerHelpers.binarySearch(mKeys, mSize, key);
    if (i >= 0) {
        if (mValues[i] != DELETED) {
            final E old = (E) mValues[i];
            mValues[i] = DELETED;
            mGarbage = true;
            return old;
        }
    }
    return null;
}

//删除指定索引对应的元素值
public void removeAt(int index) {
    if (index >= mSize && UtilConfig.sThrowExceptionForUpperArrayOutOfBounds) {
        // The array might be slightly bigger than mSize, in which case, indexing won't fail.
        // Check if exception should be thrown outside of the critical path.
        throw new ArrayIndexOutOfBoundsException(index);
    }
    if (mValues[index] != DELETED) {
        mValues[index] = DELETED;
        //标记当前需要进行垃圾回收
        mGarbage = true;
    }
}

//删除从起始索引值 index 开始之后的 size 个元素值
public void removeAtRange(int index, int size) {
    //避免发生数组越界的情况
    final int end = Math.min(mSize, index + size);
    for (int i = index; i < end; i++) {
        removeAt(i);
    }
}

//移除所有元素值
public void clear() {
    int n = mSize;
    Object[] values = mValues;
    for (int i = 0; i < n; i++) {
        values[i] = null;
    }
    mSize = 0;
    mGarbage = false;
}
```

## 7、查找元素

查找元素的方法较多，逻辑都挺简单的

```java
//根据 key 查找相应的元素值，查找不到则返回默认值
@SuppressWarnings("unchecked")
public E get(int key, E valueIfKeyNotFound) {
    //用二分查找法查找指定 key 在 mKeys 中的索引值
    int i = ContainerHelpers.binarySearch(mKeys, mSize, key);
    //如果找不到该 key 或者该 key 尚未赋值，则返回默认值
    if (i < 0 || mValues[i] == DELETED) {
        return valueIfKeyNotFound;
    } else {
        return (E) mValues[i];
    }
}

//根据 key 查找相应的元素值，查找不到则返回 null
public E get(int key) {
    return get(key, null);
}

//因为 mValues 中的元素值并非一定是连贯的，有可能掺杂着 DELETED 
//所以在遍历前需要先进行 GC，这样通过数组取出的值才是正确的
@SuppressWarnings("unchecked")
public E valueAt(int index) {
    if (mGarbage) {
        gc();
    }
    return (E) mValues[index];
}

//根据索引值 index 查找对应的 key 
public int keyAt(int index) {
    if (mGarbage) {
        gc();
    }
    return mKeys[index];
}

//根据 key 对应的索引值
public int indexOfKey(int key) {
    if (mGarbage) {
        gc();
    }
    return ContainerHelpers.binarySearch(mKeys, mSize, key);
}

//根据 value 查找对应的索引值
public int indexOfValue(E value) {
    if (mGarbage) {
        gc();
    }
    for (int i = 0; i < mSize; i++) {
        if (mValues[i] == value) {
            return i;
        }
    }
    return -1;
}

//与 indexOfValue 方法类似，但 indexOfValue 方法是通过比较 == 来判断是否同个对象
//而此方法是通过 equals 方法来判断是否同个对象
public int indexOfValueByValue(E value) {
    if (mGarbage) {
        gc();
    }
    for (int i = 0; i < mSize; i++) {
        if (value == null) {
            if (mValues[i] == null) {
                return i;
            }
        } else {
            if (value.equals(mValues[i])) {
                return i;
            }
        }
    }
    return -1;
}
```

## 8、垃圾回收

因为 SparseArray 中会出现只移除 key 和 value 两者之一的情况，导致当前数组中的有效数据并不是全都紧挨着排列在一起的，即存在无效值，因此 `gc()` 方法会根据 mValues 中到底存在多少有效数据，将 mKeys 和 mValues 中的数据进行重新排列，将有意义的元素值紧挨着排序在一起

```java
private void gc() {
    int n = mSize;
    int o = 0;
    int[] keys = mKeys;
    Object[] values = mValues;
    for (int i = 0; i < n; i++) {
        Object val = values[i];
        //val 非 DELETED ，说明该位置可能需要移动数据
        if (val != DELETED) {
            //将索引 i 处的值赋值到索引 o 处
            //所以如果 i == o ，则不需要执行代码了
            if (i != o) {
                keys[o] = keys[i];
                values[o] = val;
                values[i] = null;
            }
            o++;
        }
    }
    mGarbage = false;
    mSize = o;
}
```

## 9、优劣势总结

从上文的介绍来看，SparseArray 的主要优势有以下几点：

- 避免了基本数据类型 int 的装箱拆箱操作
- 和 HashMap 中每个存储结点都是一个类对象不同，SparseArray 不需要用于包装 key 的结构体，单个元素的存储成本更加低廉
- 在数据量不大的情况下，查找效率较高（二分查找法）
- 延迟了垃圾回收的时机，只在需要的时候才一次性进行


劣势有以下几点：

- 具有特定的适用范围，key 只能是 int 类型
- 插入键值对时可能需要移动大量的数组元素
- 数据量较大时，查找效率（二分查找法）会明显降低，需要经过多次折半查找才能定位到目标数据。而 HashMap 在没有哈希冲突的情况下只需要进行一次哈希计算即可定位到目标元素，有哈希冲突时也只需要对比链表或者红黑树上的元素即可

## 10、关联类

SparseArray 属于泛型类，所以即使 value 是基本数据类型也会被装箱和拆箱，如果想再省去这一部分开销的话，可以使用 SparseBooleanArray、SparseIntArray 和 SparseLongArray 等三个容器类，这三个容器的实现原理和 SparseArray 相同，但是 value 还是属于基本数据类型

此外，系统还提供了 LongSparseArray 这个容器类，其实现原理和 SparseArray 类似，但是 key 固定为 long 类型，value 通过泛型来声明，对于日常开发中比较有用的一点是可以用来根据 viewId 来存储 view 对象

# 二、ArrayMap

ArrayMap 属于泛型类，继承了 Map 接口，其使用方式和 HashMap 基本一样，但在内部逻辑上有着很大差异，所以需要了解其实现原理后才能明白 ArrayMap 到底适用于哪些场景

```java
public final class ArrayMap<K, V> implements Map<K, V>
```

## 1、存储机制

ArrayMap 中包含以下两个数组。mHashes 用于存储键值对中 key 的哈希值，mArray 则用于存储 key 和 value，即每个键值对会一起被存入 mArray 中

```java
// Hashes are an implementation detail. Use public key/value API.
int[] mHashes;

// Storage is an implementation detail. Use public key/value API.
Object[] mArray;
```

当向 ArrayMap 插入键值对时，会先计算出 key 的哈希值，将 keyHash 按照大小顺序存入 mHashes 中，拿到其位置索引 index。然后将 key 存入 mArray 的 index<<1 位置，将 value 存入 mArray 的 (index<<1 + 1) 位置，即 key 和 value 会存储在相邻的位置。从这个位置对应关系来看，mArray 的所需容量至少也需要是 mHashes 的两倍，且每个 key-value 的排列关系也是和 keyHash 的排列保持一致

当要通过 key 对象向 ArrayMap 取值时，就先计算出 keyHash，然后通过二分查找法找到 keyHash 在 mHashes 中的位置索引 index，然后在 (index<<1 + 1)位置从 mArray 拿到 value

## 2、添加元素

有几个用于添加元素的方法，当中重点看 `put` 方法即可，其它添加元素的方法都需要依靠该方法来实现。前文有讲到，key-value 最终是会相邻着存入 mArray 中的，而 key-value 在 mArray 中的位置是由 keyHash 和 mHashes 来共同决定的，`put` 方法的整体逻辑如下所述：

1. 根据二分查找法获取到 keyHash 在 mHashes 中的索引位置 index
2. 如果 index 大于等于 0，说明在 mArray 中 key 已存在，那么直接覆盖旧值即可，结束流程
3. 如果 index 小于 0，说明在 mArray 中 key 不存在，~index 此时代表的是 keyHash 按照递增顺序应该插入 mHashes 的位置
4. 判断是否需要扩容，需要的话则进行扩容。如果符合缓存标准的话，则会缓存扩容前的数组
5. 对最终的数组进行数据迁移，插入 key-value 和 keyHash

```java
@Override
public V put(K key, V value) {
    final int osize = mSize;
    final int hash;
    int index;

    //第一步
    if (key == null) {
        hash = 0; 
        index = indexOfNull();
    } else {
        hash = mIdentityHashCode ? System.identityHashCode(key) : key.hashCode();
        index = indexOf(key, hash);
    }

    //第二步
    if (index >= 0) {
        index = (index<<1) + 1;
        final V old = (V)mArray[index];
        mArray[index] = value;
        return old;
    }

    //第三步
    index = ~index;

    //第四步
    if (osize >= mHashes.length) {
        //ArrayMap 的扩容机制相比 HashMap 会比较克制
        //当数组长度已超出 BASE_SIZE*2 后，数组容量按照 1.5 倍来扩容
        final int n = osize >= (BASE_SIZE*2) ? (osize+(osize>>1))
                : (osize >= BASE_SIZE ? (BASE_SIZE*2) : BASE_SIZE);

        if (DEBUG) Log.d(TAG, "put: grow from " + mHashes.length + " to " + n);

        final int[] ohashes = mHashes;
        final Object[] oarray = mArray;
        allocArrays(n);

        if (CONCURRENT_MODIFICATION_EXCEPTIONS && osize != mSize) {
            throw new ConcurrentModificationException();
        }

        if (mHashes.length > 0) {
            if (DEBUG) Log.d(TAG, "put: copy 0-" + osize + " to 0");
            System.arraycopy(ohashes, 0, mHashes, 0, ohashes.length);
            System.arraycopy(oarray, 0, mArray, 0, oarray.length);
        }

        freeArrays(ohashes, oarray, osize);
    }

    //第五步
    if (index < osize) {
        if (DEBUG) Log.d(TAG, "put: move " + index + "-" + (osize-index)
                + " to " + (index+1));
        System.arraycopy(mHashes, index, mHashes, index + 1, osize - index);
        System.arraycopy(mArray, index << 1, mArray, (index + 1) << 1, (mSize - index) << 1);
    }

    if (CONCURRENT_MODIFICATION_EXCEPTIONS) {
        if (osize != mSize || index >= mHashes.length) {
            throw new ConcurrentModificationException();
        }
    }
    mHashes[index] = hash;
    mArray[index<<1] = key;
    mArray[(index<<1)+1] = value;
    mSize++;
    return null;
}
```

`append` 方法也是用于添加元素的，带有一点“追加”的意思，如果外部可以确定本次插入的 key 的 hash 值比当前所有已有值都大的话，那么就可以直接向 mHashes 的尾部插入数据，从而节省了二分查找的过程。所以 `append` 方法会先和 mHashes 的最后一个元素值进行对比，如果 keyHash 比该值大的话就说明可以直接保存到尾部，校验不通过的话还是会调用 `put` 方法

```java
public void append(K key, V value) {
    int index = mSize;
    final int hash = key == null ? 0
            : (mIdentityHashCode ? System.identityHashCode(key) : key.hashCode());
    if (index >= mHashes.length) {
        throw new IllegalStateException("Array is full");
    }
    //如果 mHashes 当前的最后一个值比 hash 大，hash 没法直接插到尾部，那么就还是需要调用 put 方法
    if (index > 0 && mHashes[index-1] > hash) {
        RuntimeException e = new RuntimeException("here");
        e.fillInStackTrace();
        Log.w(TAG, "New hash " + hash
                + " is before end of array hash " + mHashes[index-1]
                + " at index " + index + " key " + key, e);
        put(key, value);
        return;
    }
    //将 key-value 直接插入到数组尾部
    mSize = index+1;
    mHashes[index] = hash;
    index <<= 1;
    mArray[index] = key;
    mArray[index+1] = value;
}
```

## 3、获取元素

获取元素的方法主要看 `indexOf(Object key, int hash)`方法即可，只要理解了该方法是如何获取 keyIndex 的，那么就能够对 ArrayMap 的存储结构有更明确的认知

`indexOf` 方法用于获取和 key，hash 均能对应上的元素的哈希值在 mHashes 中的索引位置。我们知道，keyHash 是存储在 mHashes 中的，而 key-value 又是存储在 mArray 中的，但我们无法只根据 keyHash 就准确对应上 key-value，因为不同的 key 有可能有相同的 hash 值，即需要考虑哈希冲突的情况，所以 `indexOf` 方法除了需要对比 hash 值大小是否相等外还需要对比 key 的相等性

1. 通过二分查找法获取到 mHashes 中和 hash 相等的值的索引 index
2. 如果 index 小于 0，说明不存在该 key，那么就返回 index，~index 就是 hash 插入 mHashes 后的位置索引。结束流程
3. index 大于等于 0，说明 key 有可能存在，之所以说可能，因为存在 key 不同但 hash 值相等的情况
4. 判断 mArray 中 index<<1 位置的元素是否和 key 相等，如果相等说明已经找到了目标位置，返回 index。结束流程
5. 此时可以确定发生了哈希冲突，那么就需要对 mArray 进行相等性对比了，而之所以要分为两个 for 循环也是为了减少遍历次数，因为相同 hash 值是会靠拢在一起的，所以分别向两侧进行遍历查找。如果 key 和 keyHash 的相等性均校验通过，那么就返回对应的索引。结束流程
6. 会执行到这里，说明还是没有找到和 key 相等的元素值，那么就拿到 hash 应该存入 mHashes 后的索引，~ 运算后返回

```java
int indexOf(Object key, int hash) {
    final int N = mSize;
    // Important fast case: if nothing is in here, nothing to look for.
    if (N == 0) {
        return ~0;
    }
    //第一步
    int index = binarySearchHashes(mHashes, N, hash);

    // If the hash code wasn't found, then we have no entry for this key.
    //第二步
    if (index < 0) {
        return index;
    }

    // If the key at the returned index matches, that's what we want.
    //第四步
    if (key.equals(mArray[index<<1])) {
        return index;
    }

    //第五步
    // Search for a matching key after the index.
    int end;
    for (end = index + 1; end < N && mHashes[end] == hash; end++) {
        if (key.equals(mArray[end << 1])) return end;
    }
    // Search for a matching key before the index.
    for (int i = index - 1; i >= 0 && mHashes[i] == hash; i--) {
        if (key.equals(mArray[i << 1])) return i;
    }

    // Key not found -- return negative value indicating where a
    // new entry for this key should go.  We use the end of the
    // hash chain to reduce the number of array entries that will
    // need to be copied when inserting.
    //第六步
    return ~end;
}
```

## 4、缓存机制

ArrayMap 内部包含了对 mHashes 和 mArray 这两个数组进行缓存的机制，避免由于频繁创建数组而造成内存抖动，这一点还是比较有意义的。在 Android 系统中 Bundle 是使用得很频繁的一个类，其内部就通过 ArrayMap 来存储键值对，这可以从 Bundle 的父类 BaseBundle 看到。所以 ArrayMap 的数组缓存机制在我看来更多的是面对系统运行时的优化措施

```java
public class BaseBundle {
    
    @UnsupportedAppUsage
    ArrayMap<String, Object> mMap = null;
    
    public void putBoolean(@Nullable String key, boolean value) {
        unparcel();
        mMap.put(key, value);
    }

    void putByte(@Nullable String key, byte value) {
        unparcel();
        mMap.put(key, value);
    }

    void putChar(@Nullable String key, char value) {
        unparcel();
        mMap.put(key, value);
    }
    
    ···
    
}
```

`put` 方法内部就使用到了数组的缓存和复用机制

```java
@Override
public V put(K key, V value) {
    ···
    if (osize >= mHashes.length) {
        final int n = osize >= (BASE_SIZE*2) ? (osize+(osize>>1))
                : (osize >= BASE_SIZE ? (BASE_SIZE*2) : BASE_SIZE);

        if (DEBUG) Log.d(TAG, "put: grow from " + mHashes.length + " to " + n);

        final int[] ohashes = mHashes;
        final Object[] oarray = mArray;
        //尝试通过数组复用机制来初始化 mHashes 和 mArray
        allocArrays(n);

        if (CONCURRENT_MODIFICATION_EXCEPTIONS && osize != mSize) {
            throw new ConcurrentModificationException();
        }

        if (mHashes.length > 0) {
            if (DEBUG) Log.d(TAG, "put: copy 0-" + osize + " to 0");
            System.arraycopy(ohashes, 0, mHashes, 0, ohashes.length);
            System.arraycopy(oarray, 0, mArray, 0, oarray.length);
        }
        //尝试回收 ohashes 和 oarray
        freeArrays(ohashes, oarray, osize);
    }
    ···
    return null;
}
```

### 缓存数组

实现数组缓存逻辑对应的是 `freeArrays` 方法，该方法就用于缓存 mHashes 和 mArray。每当 ArrayMap 完成数组扩容后就会调用此方法对扩容前的数组进行缓存，但也不是所有数组都会进行缓存，而是有着数组长度和缓存总数这两方面的限制

首先，ArrayMap 包含了多个全局的静态变量和静态常量用于控制及实现数组缓存。从`freeArrays`方法可以看出来，if 和 else 语句块的逻辑是基本完全一样的，其区别只在于触发缓存的条件和使用的缓存池不一样

例如，如果 hashes 的数组长度是 BASE_SIZE * 2，且当前缓存总数没有超出 CACHE_SIZE，那么缓存的数组就保存在 mTwiceBaseCache 所构造的的单向链表中。mTwiceBaseCache 采用单向链表的结构来串联多个数组，要缓存的 mArray 的第一个数组元素值会先指向 mTwiceBaseCache，第二个数组元素值会指向 mHashes，之后 mArray 会成为单向链表的新的头结点，即 mArray 成为了新的 mTwiceBaseCache。在这种缓存机制下，最终 mTwiceBaseCache 指向的其实是本次缓存的 mArray，mArray 的第一个元素值指向的又是上一次缓存的 mArray，第二个元素值指向的是本次缓存的 mHashes，从而形成了一个单向链表结构

```java
//用于缓存长度为 BASE_SIZE 的数组
static Object[] mBaseCache;
//mBaseCache 已缓存的数组个数
static int mBaseCacheSize;

//用于缓存长度为 BASE_SIZE * 2 的数组
static Object[] mTwiceBaseCache;
//mTwiceBaseCache 已缓存的数组个数
static int mTwiceBaseCacheSize;

private static final int BASE_SIZE = 4;

//mBaseCacheSize 和 mTwiceBaseCacheSize 的最大缓存个数
private static final int CACHE_SIZE = 10;

//用来当做同步锁
private static final Object sBaseCacheLock = new Object();
private static final Object sTwiceBaseCacheLock = new Object();

//缓存 hashes 和 array
private static void freeArrays(final int[] hashes, final Object[] array, final int size) {
    if (hashes.length == (BASE_SIZE*2)) {
        synchronized (sTwiceBaseCacheLock) {
            if (mTwiceBaseCacheSize < CACHE_SIZE) {
                  //第一个元素指向 mTwiceBaseCache
                array[0] = mTwiceBaseCache;
                  //第二个元素指向 hashes
                array[1] = hashes;
                for (int i=(size<<1)-1; i>=2; i--) {
                    //切除多余引用，避免内存泄漏，有利于 GC
                    array[i] = null;
                }
                //array 成为单链表的头结点
                mTwiceBaseCache = array;
                mTwiceBaseCacheSize++;
                if (DEBUG) Log.d(TAG, "Storing 2x cache " + array
                        + " now have " + mTwiceBaseCacheSize + " entries");
            }
        }
    } else if (hashes.length == BASE_SIZE) {
        synchronized (sBaseCacheLock) {
            if (mBaseCacheSize < CACHE_SIZE) {
                array[0] = mBaseCache;
                array[1] = hashes;
                for (int i=(size<<1)-1; i>=2; i--) {
                    array[i] = null;
                }
                mBaseCache = array;
                mBaseCacheSize++;
                if (DEBUG) Log.d(TAG, "Storing 1x cache " + array
                        + " now have " + mBaseCacheSize + " entries");
            }
        }
    }
}
```

### 复用数组

缓存数组的目的自然就是为了后续复用，数组的复用逻辑对应的是 `allocArrays` 方法，该方法用于为 mHashes 和 mArray 申请一个更大容量的数组空间，通过复用数组或者全新初始化来获得

在进行数组缓存的时候会判断数组长度，只有当长度是 BASE_SIZE * 2 或 BASE_SIZE 时才会进行缓存，那么自然只有当数组的目标长度 size 是这两个值之一才符合复用条件了。`allocArrays` 方法取出缓存的逻辑也很简单，只需要取出单向链表的头结点赋值给 mHashes 和 mArray，同时使链表的第二个结点成为新的头结点即可。如果不符合复用条件或者复用失败，那么就还是需要重新构建两个新的数组对象

```java
//用于缓存长度为 BASE_SIZE 的数组
static Object[] mBaseCache;
//mBaseCache 已缓存的数组个数
static int mBaseCacheSize;

//用于缓存长度为 BASE_SIZE * 2 的数组
static Object[] mTwiceBaseCache;
//mTwiceBaseCache 已缓存的数组个数
static int mTwiceBaseCacheSize;

private static final int BASE_SIZE = 4;

//mBaseCacheSize 和 mTwiceBaseCacheSize 的最大缓存个数
private static final int CACHE_SIZE = 10;

//用来当做同步锁
private static final Object sBaseCacheLock = new Object();
private static final Object sTwiceBaseCacheLock = new Object();

private void allocArrays(final int size) {
    if (mHashes == EMPTY_IMMUTABLE_INTS) {
        throw new UnsupportedOperationException("ArrayMap is immutable");
    }
    if (size == (BASE_SIZE*2)) {
        synchronized (sTwiceBaseCacheLock) {
            if (mTwiceBaseCache != null) {
                final Object[] array = mTwiceBaseCache;
                mArray = array;
                try {
                    //指向头结点的下一个结点，即原先的第二个结点将成为链表新的头结点
                    mTwiceBaseCache = (Object[]) array[0];
                    mHashes = (int[]) array[1];
                    if (mHashes != null) {
                        //符合复用条件，切除多余引用
                        array[0] = array[1] = null;
                        mTwiceBaseCacheSize--;
                        if (DEBUG) {
                            Log.d(TAG, "Retrieving 2x cache " + mHashes
                                    + " now have " + mTwiceBaseCacheSize + " entries");
                        }
                        return;
                    }
                } catch (ClassCastException e) {
                }
                // Whoops!  Someone trampled the array (probably due to not protecting
                // their access with a lock).  Our cache is corrupt; report and give up.
                Slog.wtf(TAG, "Found corrupt ArrayMap cache: [0]=" + array[0]
                        + " [1]=" + array[1]);
                //会执行到这里，说明缓存机制发现问题，则弃用之前的所有缓存
                mTwiceBaseCache = null;
                mTwiceBaseCacheSize = 0;
            }
        }
    } else if (size == BASE_SIZE) {
        synchronized (sBaseCacheLock) {
            if (mBaseCache != null) {
                final Object[] array = mBaseCache;
                mArray = array;
                try {
                    mBaseCache = (Object[]) array[0];
                    mHashes = (int[]) array[1];
                    if (mHashes != null) {
                        array[0] = array[1] = null;
                        mBaseCacheSize--;
                        if (DEBUG) {
                            Log.d(TAG, "Retrieving 1x cache " + mHashes
                                    + " now have " + mBaseCacheSize + " entries");
                        }
                        return;
                    }
                } catch (ClassCastException e) {
                }
                // Whoops!  Someone trampled the array (probably due to not protecting
                // their access with a lock).  Our cache is corrupt; report and give up.
                Slog.wtf(TAG, "Found corrupt ArrayMap cache: [0]=" + array[0]
                        + " [1]=" + array[1]);
                mBaseCache = null;
                mBaseCacheSize = 0;
            }
        }
    }

    mHashes = new int[size];
    mArray = new Object[size<<1];
}
```

### 总结

上文说了，只有长度为 BASE_SIZE 或者 BASE_SIZE * 2 的数组才会被缓存复用，而 mHashes 和 mArray 的扩容操作也会尽量使得扩容后的数组长度就是这两个值之一，这可以从 `put` 方法计算扩容后容量的算法看出来

```java
@Override
public V put(K key, V value) {
    final int osize = mSize;
    final int hash;
    ···
    if (osize >= mHashes.length) {
        //计算数组扩容后的大小
        final int n = osize >= (BASE_SIZE*2) ? (osize+(osize>>1))
                : (osize >= BASE_SIZE ? (BASE_SIZE*2) : BASE_SIZE);

        if (DEBUG) Log.d(TAG, "put: grow from " + mHashes.length + " to " + n);

        final int[] ohashes = mHashes;
        final Object[] oarray = mArray;

        allocArrays(n);

        ···

        freeArrays(ohashes, oarray, osize);
    }
    ···
    return null;
}
```

所以说，虽然 ArrayMap 的构造函数中并没有直接将 BASE_SIZE 作为数组的默认长度，但是在扩容过程中会尽量往 BASE_SIZE 和 BASE_SIZE * 2 这两个值靠拢，这就有利于尽量实现数组复用

此外，ArrayMap 的扩容操作在申请内存时也显得比较克制，在数组长度超出 BASE_SIZE * 2  后，只是扩容到当前的 1.5 倍，且也只在 mHashes 容量不足时才会触发扩容机制。而 HashMap 在达到负载因子设定的比例后（此时数组未满）就会触发扩容机制，而且也是按照扩充到两倍容量的方式进行扩容。所以说，ArrayMap 对于内存空间的利用效率会更高一些

## 5、优劣势总结

ArrayMap 的适用场景可以从它的缓存机制就看出来一些，它会缓存容量为 4 或者 8 的数组并进行后续复用，而这两个值可以说都是比较小的。Android 系统对于内存比较敏感，需要存储键值对时面对的往往是使用频率高但数据量小的场景。例如我们在跳转到 Activity 时往往是通过 Bundle 来存储跳转参数，但数据量一般都很少，所以 Bundle 内部就使用到了 ArrayMap 来存储键值对。ArrayMap 在内存申请时相比 HashMap 会比较克制，键值对会以更加紧密的数据结构存储在一起，对内存利用率会更高一些

而相对的，ArrayMap 的这种存储结构也导致了其查找效率相比 HashMap 要低很多。在数据量大时，ArrayMap 可能需要通过多次二分查找才能定位到元素，而 HashMap 在没有哈希冲突的情况下只需要经过一次哈希计算即可定位到元素，即使有哈希冲突也只需要遍历发生冲突的部分元素即可

所以说， ArrayMap 适用于数据量较小的场景，此时查找效率也不会受多大影响，而内存利用率能够显著提高。如果数据量较大，那就可以考虑使用 HashMap 来替代了

## 6、关联类

系统还包含了一个用于存储不重复元素值的集合框架类：ArraySet，从名字就可以猜到 ArraySet 实现了 Set 接口。ArraySet 内部一样使用两个数组来存储 hash 和 value，即 mHashes 和 mArray，在实现逻辑上基本和 ArrayMap 一样，只是会在存值的时候判断 value 是否重复而已，这里就不再赘述了