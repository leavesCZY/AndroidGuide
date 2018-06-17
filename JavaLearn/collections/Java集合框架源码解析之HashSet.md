HashSet 实现了 Set 接口，不允许插入重复的元素，允许包含 null 元素，且不保证元素迭代顺序，特别是不保证该顺序恒久不变

HashSet 的代码十分简单，去掉注释后的代码不到两百行。HashSet 底层是通过 HashMap 来实现的，如果看过我上一篇关于 HashMap 源码的解析，再来看 HashSet 就会有一种“不过如此”的感觉了

在向 HashSet 添加元素时，HashSet 会将该操作转换为向 HashMap 添加键值对，如果 HashMap 中包含 key 值与待插入元素相等的键值对（hashCode() 方法返回值相等，通过 equals() 方法比较也返回 true），则待添加的键值对的 value 会覆盖原有数据，但 key 不会有所改变，因此如果向 HashSet 添加一个已存在的元素时，元素不会被存入 HashMap 中，从而实现了 HashSet 元素不重复的特征

在此就直接贴出源代码了

```java
package java.util;

import java.io.InvalidObjectException;

public class HashSet<E>
    extends AbstractSet<E>
    implements Set<E>, Cloneable, java.io.Serializable{

    //序列化ID
    static final long serialVersionUID = -5024744406713321676L;

    //HashSet 底层用 HashMap 来存放数据
    //Key值由外部传入，Value则由 HashSet 内部来维护
    private transient HashMap<E,Object> map;

    //HashMap 中所有键值对都共享同一个值
    //即所有存入 HashMap 的键值对都是使用这个对象作为值
    private static final Object PRESENT = new Object();

    //无参构造函数，HashMap 使用默认的初始化大小和装载因子
    public HashSet() {
        map = new HashMap<>();
    }

    //使用默认的装载因子，并以此来计算 HashMap 的初始化大小
    //+1 是为了弥补精度损失
    public HashSet(Collection<? extends E> c) {
        map = new HashMap<>(Math.max((int) (c.size()/.75f) + 1, 16));
        addAll(c);
    }

    //为 HashMap 自定义初始化大小和装载因子
    public HashSet(int initialCapacity, float loadFactor) {
        map = new HashMap<>(initialCapacity, loadFactor);
    }

    //为 HashMap 自定义初始化大小
    public HashSet(int initialCapacity) {
        map = new HashMap<>(initialCapacity);
    }

    //此构造函数为包访问权限，只用于对 LinkedHashSet 的支持
    HashSet(int initialCapacity, float loadFactor, boolean dummy) {
        map = new LinkedHashMap<>(initialCapacity, loadFactor);
    }

    //将对 HashSet 的迭代转换为对 HashMap 的 Key 值的迭代
    public Iterator<E> iterator() {
        return map.keySet().iterator();
    }

    //获取集合中的元素数量
    public int size() {
        return map.size();
    }

    //判断集合是否为空
    public boolean isEmpty() {
        return map.isEmpty();
    }

    //判断集合是否包含指定元素
    public boolean contains(Object o) {
        return map.containsKey(o);
    }

    //如果 HashSet 中不包含元素 e，则添加该元素，并返回 true
    //如果 HashSet 中包含元素 e，则不会影响 HashSet ，并返回 false
    //该方法将向 HashSet 添加元素 e 的操作转换为向 HashMap 添加键值对
    //如果 HashMap 中包含 key 值与 e 相等的结点（hashCode() 方法返回值相等，通过 equals() 方法比较也返回 true）
    //则新添加的结点的 value 会覆盖原有数据，但 key 不会有所改变
    //因此如果向 HashSet 添加一个已存在的元素时，元素不会被存入 HashMap 中
    //从而实现了 HashSet 元素不重复的特征
    public boolean add(E e) {
        return map.put(e, PRESENT)==null;
    }

    //移除集合中的元素 o
    //如果集合不包含元素 o，则返回 false
    public boolean remove(Object o) {
        return map.remove(o)==PRESENT;
    }

    //清空集合中的元素
    public void clear() {
        map.clear();
    }

    /**
     * Returns a shallow copy of this <tt>HashSet</tt> instance: the elements
     * themselves are not cloned.
     *
     * @return a shallow copy of this set
     */
    @SuppressWarnings("unchecked")
    public Object clone() {
        try {
            HashSet<E> newSet = (HashSet<E>) super.clone();
            newSet.map = (HashMap<E, Object>) map.clone();
            return newSet;
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e);
        }
    }

    /**
     * Save the state of this <tt>HashSet</tt> instance to a stream (that is,
     * serialize it).
     *
     * @serialData The capacity of the backing <tt>HashMap</tt> instance
     *             (int), and its load factor (float) are emitted, followed by
     *             the size of the set (the number of elements it contains)
     *             (int), followed by all of its elements (each an Object) in
     *             no particular order.
     */
    private void writeObject(java.io.ObjectOutputStream s)
        throws java.io.IOException {
        // Write out any hidden serialization magic
        s.defaultWriteObject();

        // Write out HashMap capacity and load factor
        s.writeInt(map.capacity());
        s.writeFloat(map.loadFactor());

        // Write out size
        s.writeInt(map.size());

        // Write out all elements in the proper order.
        for (E e : map.keySet())
            s.writeObject(e);
    }

    /**
     * Reconstitute the <tt>HashSet</tt> instance from a stream (that is,
     * deserialize it).
     */
    private void readObject(java.io.ObjectInputStream s)
        throws java.io.IOException, ClassNotFoundException {
        // Read in any hidden serialization magic
        s.defaultReadObject();

        // Read capacity and verify non-negative.
        int capacity = s.readInt();
        if (capacity < 0) {
            throw new InvalidObjectException("Illegal capacity: " +
                                             capacity);
        }

        // Read load factor and verify positive and non NaN.
        float loadFactor = s.readFloat();
        if (loadFactor <= 0 || Float.isNaN(loadFactor)) {
            throw new InvalidObjectException("Illegal load factor: " +
                                             loadFactor);
        }

        // Read size and verify non-negative.
        int size = s.readInt();
        if (size < 0) {
            throw new InvalidObjectException("Illegal size: " +
                                             size);
        }

        // Set the capacity according to the size and load factor ensuring that
        // the HashMap is at least 25% full but clamping to maximum capacity.
        capacity = (int) Math.min(size * Math.min(1 / loadFactor, 4.0f),
                HashMap.MAXIMUM_CAPACITY);

        // Create backing HashMap
        map = (((HashSet<?>)this) instanceof LinkedHashSet ?
               new LinkedHashMap<E,Object>(capacity, loadFactor) :
               new HashMap<E,Object>(capacity, loadFactor));

        // Read in all elements in the proper order.
        for (int i=0; i<size; i++) {
            @SuppressWarnings("unchecked")
                E e = (E) s.readObject();
            map.put(e, PRESENT);
        }
    }

    /**
     * Creates a <em><a href="Spliterator.html#binding">late-binding</a></em>
     * and <em>fail-fast</em> {@link Spliterator} over the elements in this
     * set.
     *
     * <p>The {@code Spliterator} reports {@link Spliterator#SIZED} and
     * {@link Spliterator#DISTINCT}.  Overriding implementations should document
     * the reporting of additional characteristic values.
     *
     * @return a {@code Spliterator} over the elements in this set
     * @since 1.8
     */
    //为了并行遍历数据源中的元素而设计的迭代器
    public Spliterator<E> spliterator() {
        return new HashMap.KeySpliterator<E,Object>(map, 0, -1, 0, 0);
    }
    
}
```

#### 更详细的源码解析可以看这里：[JavaLearn](https://github.com/leavesC/JavaLearn)

