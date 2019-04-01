package android.util;

import com.android.internal.util.ArrayUtils;
import com.android.internal.util.GrowingArrayUtils;

import libcore.util.EmptyArray;

/**
 * SparseArrays map integers to Objects.  Unlike a normal array of Objects,
 * there can be gaps in the indices.  It is intended to be more memory efficient
 * than using a HashMap to map Integers to Objects, both because it avoids
 * auto-boxing keys and its data structure doesn't rely on an extra entry object
 * for each mapping.
 *
 * <p>Note that this container keeps its mappings in an array data structure,
 * using a binary search to find keys.  The implementation is not intended to be appropriate for
 * data structures
 * that may contain large numbers of items.  It is generally slower than a traditional
 * HashMap, since lookups require a binary search and adds and removes require inserting
 * and deleting entries in the array.  For containers holding up to hundreds of items,
 * the performance difference is not significant, less than 50%.</p>
 *
 * <p>To help with performance, the container includes an optimization when removing
 * keys: instead of compacting its array immediately, it leaves the removed entry marked
 * as deleted.  The entry can then be re-used for the same key, or compacted later in
 * a single garbage collection step of all removed entries.  This garbage collection will
 * need to be performed at any time the array needs to be grown or the the map size or
 * entry values are retrieved.</p>
 *
 * <p>It is possible to iterate over the items in this container using
 * {@link #keyAt(int)} and {@link #valueAt(int)}. Iterating over the keys using
 * <code>keyAt(int)</code> with ascending values of the index will return the
 * keys in ascending order, or the values corresponding to the keys in ascending
 * order in the case of <code>valueAt(int)</code>.</p>
 */

 //SparseArray<E> 相当于 Map<Integer,E> ，key 存放在 mKeys 中，而该 key 对应的 value 则存放在 mValues 中
 //mKeys 和 mValues 通过如下方式对应起来：
 //假设要向 SparseArray 存入 key 为 10，value 为 200 的键值对，则先将 10 存到 mKeys 中，假设对应的索引值是 index ，则将 value 存入 mValues[index] 中

public class SparseArray<E> implements Cloneable {

    //数组元素在没有外部指定值时的默认元素值
    private static final Object DELETED = new Object();

    //用于标记当前是否有待垃圾回收(GC)的元素
    private boolean mGarbage = false;

    private int[] mKeys;

    private Object[] mValues;

    //当前集合元素大小
    //该值并不一定是时时处于正确状态，因为有可能出现只删除 key 和 value 两者之一的情况
    //所以在调用 size() 方法前都需要进行 GC
    private int mSize;

    //设置数组的默认初始容量为10
    public SparseArray() {
        this(10);
    }

    /**
     * Creates a new SparseArray containing no mappings that will not
     * require any additional memory allocation to store the specified
     * number of mappings.  If you supply an initial capacity of 0, the
     * sparse array will be initialized with a light-weight representation
     * not requiring any additional array allocations.
     */
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



    /******************************** 增添元素 ********************************/


    //将索引 index 处的元素赋值为 value
    //SparseArray 的元素值都是存到 mValues 中的，因此如果知道目标位置（index），则可以直接向数组 mValues 赋值
    public void setValueAt(int index, E value) {
        //如果需要则先进行垃圾回收
        if (mGarbage) {
            gc();
        }
        mValues[index] = value;
    }

    
    /**
     * Adds a mapping from the specified key to the specified value,
     * replacing the previous mapping from the specified key if there
     * was one.
     */
    public void put(int key, E value) {
        //用二分查找法查找指定 key 在 mKeys 中的索引值
        int i = ContainerHelpers.binarySearch(mKeys, mSize, key);
        //找得到则直接赋值
        if (i >= 0) {
            mValues[i] = value;
        } else {
            //binarySearch 方法的返回值分为两种情况：
            //1、如果存在对应的 key，则直接返回对应的索引值
            //2、如果不存在对应的 key
            //  2.1、假设 mKeys 中存在"值比 key 大且大小与 key 最接近的值的索引"为 presentIndex，则此方法的返回值为 ~presentIndex
            //  2.2、如果 mKeys 中不存在比 key 还要大的值的话，则返回值为 ~mKeys.length

            //可以看到，即使在 mKeys 中不存在目标 key，但其返回值也指向了应该让 key 存入的位置
            //通过将计算出的索引值进行 ~ 运算，则返回值一定是 0 或者负数，从而与“找得到目标key的情况（返回值大于0）”的情况区分开
            //且通过这种方式来存放数据，可以使得 mKeys 的值一直是处于有序状态的

            i = ~i;

            //如果目标位置还未赋值，则直接存入数据即可，对应的情况是 2.1
            if (i < mSize && mValues[i] == DELETED) {
                mKeys[i] = key;
                mValues[i] = value;
                return;
            }

            //以下操作对应两种情况：
            //1、对应 2.1 的一种特殊情况，即目标位置已用于存放其他值了
            //   此时就需要将从索引 i 开始的所有数据向后移动一位，并将 key 存到 mKeys[i]
            //2、对应的情况是 2.2

            if (mGarbage && mSize >= mKeys.length) {
                gc();
                //GC 后再次进行查找，因为值可能已经发生变化了
                i = ~ContainerHelpers.binarySearch(mKeys, mSize, key);
            }
            //通过复制或者扩容数组，将数据存放到数组中
            mKeys = GrowingArrayUtils.insert(mKeys, mSize, i, key);
            mValues = GrowingArrayUtils.insert(mValues, mSize, i, value);
            mSize++;
        }
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

    /******************************** 移除元素 ********************************/


    //如果存在 key 对应的元素值，则将其移除
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

    /******************************** 查找元素 ********************************/

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

    //获取当前集合元素大小
    public int size() {
        if (mGarbage) {
            gc();
        }
        return mSize;
    }

    //垃圾回收
    //因为 SparseArray 中可能出现只移除 value 和 value 两者之一的情况
    //所以此方法就用于移除无用的引用
    private void gc() {
        int n = mSize;
        //o 值用于表示 GC 后的元素个数
        int o = 0;
        int[] keys = mKeys;
        Object[] values = mValues;
        for (int i = 0; i < n; i++) {
            Object val = values[i];
            //元素值非默认值 DELETED ，说明该位置可能需要移动数据
            if (val != DELETED) {
                //以下代码片段用于将索引 i 处的值赋值到索引 o 处
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

    @Override
    @SuppressWarnings("unchecked")
    public SparseArray<E> clone() {
        SparseArray<E> clone = null;
        try {
            clone = (SparseArray<E>) super.clone();
            clone.mKeys = mKeys.clone();
            clone.mValues = mValues.clone();
        } catch (CloneNotSupportedException cnse) {
            /* ignore */
        }
        return clone;
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation composes a string by iterating over its mappings. If
     * this map contains itself as a value, the string "(this Map)"
     * will appear in its place.
     */
    @Override
    public String toString() {
        if (size() <= 0) {
            return "{}";
        }
        StringBuilder buffer = new StringBuilder(mSize * 28);
        buffer.append('{');
        for (int i=0; i<mSize; i++) {
            if (i > 0) {
                buffer.append(", ");
            }
            int key = keyAt(i);
            buffer.append(key);
            buffer.append('=');
            Object value = valueAt(i);
            if (value != this) {
                buffer.append(value);
            } else {
                buffer.append("(this Map)");
            }
        }
        buffer.append('}');
        return buffer.toString();
    }

}