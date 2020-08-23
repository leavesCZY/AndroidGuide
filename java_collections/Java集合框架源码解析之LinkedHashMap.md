> 本系列文章会陆续对  Java 集合框架（Java Collections Framework，JDK1.8）中的几个常用容器结合源码进行介绍，帮助读者建立起对 Java 集合框架清晰而深入的理解，也算是对自己所学内容的一个总结归纳
>
> 项目主页：https://github.com/leavesC/AndroidGuide

HashMap 是用于映射(键值对)处理的数据类型，不保证元素的顺序按照插入顺序来排列，为了解决这一问题，Java 在 JDK1.4 以后提供了 LinkedHashMap 来实现有序的 HashMap

LinkedHashMap 是 HashMap 的子类，它保留了元素的插入顺序，在内部维护着一个按照元素插入顺序或者元素访问顺序来排列的链表，默认是按照元素的插入顺序来排列，就像使用 ArrayList 一样；如果是按照元素的访问顺序来排列，则访问元素后该元素将移至链表的尾部，可以以此来实现 LRUcache 缓存算法

#### 一、结点类

前面说了，LinkedHashMap 是 HashMap 的子类，即 LinkedHashMap 的主要数据结构实现还是依靠 HashMap 来实现，LinkedHashMap 只是对 HashMap 做的一层外部包装，这个从 LinkedHashMap 内声明的结点类就可以看出来

Entry 类在 Node 类的基础上扩展了两个新的成员变量，这两个成员变量就是 LinkedHashMap 来实现有序访问的关键，不管结点对象在 HashMap 内部为了解决哈希冲突采用的是链表还是红黑树，这两个变量的指向都不受数据结构的变化而影响

从这也可以看出集合框架在设计时一个很巧妙的地方：LinkedHashMap 内部没有新建一个链表用来维护元素的插入顺序，而是通过扩展父类来实现自身的功能

```java
    //LinkedHashMap 扩展了 HashMap.Node 类
    //在其基础上新增了两个成员变量用于指定上一个结点 before 和下一个结点 after
    static class Entry<K,V> extends HashMap.Node<K,V> {
        Entry<K,V> before, after;
        Entry(int hash, K key, V value, Node<K,V> next) {
            super(hash, key, value, next);
        }
    }
```

#### 二、成员变量

变量 **accessOrder** 用于决定 LinkedHashMap 中元素的排序方式，变量 **tail** 则用于帮助当 accessOrder 为 true 时最新使用的一个结点的指向

```java
    //序列化ID
    private static final long serialVersionUID = 3801124242820219131L;

    //指向双向链表的头结点
    transient LinkedHashMap.Entry<K,V> head;

    //指向最新插入的一个结点
    transient LinkedHashMap.Entry<K,V> tail;

    //如果为true，则内部元素按照访问顺序排序
    //如果为false，则内部元素按照插入顺序排序
    final boolean accessOrder;

```

#### 三、构造函数

```java
	//自定义初始容量与装载因子
    //内部元素按照插入顺序进行排序
    public LinkedHashMap(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
        accessOrder = false;
    }

    //自定义装载因子
    //内部元素按照插入顺序进行排序
    public LinkedHashMap(int initialCapacity) {
        super(initialCapacity);
        accessOrder = false;
    }

    //使用默认的初始容量以及装载因子
    //内部元素按照插入顺序进行排序
    public LinkedHashMap() {
        super();
        accessOrder = false;
    }

    //使用初始数据
    //内部元素按照插入顺序进行排序
    public LinkedHashMap(Map<? extends K, ? extends V> m) {
        super();
        accessOrder = false;
        putMapEntries(m, false);
    }

    /**
     * @param  initialCapacity 初始容量
     * @param  loadFactor      装载因子
     * @param  accessOrder     如果为true，则内部元素按照访问顺序排序；如果为false，则内部元素按照插入顺序排序
     */
    public LinkedHashMap(int initialCapacity, float loadFactor, boolean accessOrder) {
        super(initialCapacity, loadFactor);
        this.accessOrder = accessOrder;
    }

```

#### 四、插入元素

在 HashMap 中有三个空实现的函数，源码注释中也写明这三个函数是准备由 LinkedHashMap 来实现的

```java
    // Callbacks to allow LinkedHashMap post-actions
    void afterNodeAccess(Node<K,V> p) { }
    void afterNodeInsertion(boolean evict) { }
    void afterNodeRemoval(Node<K,V> p) { }
```

当中，如果在调用 `put(K key, V value)` 方法插入元素时覆盖了原有值，则`afterNodeAccess` 方法会被调用，该方法用于将最新访问的键值对移至链表的尾部，其在 LinkedHashMap 的实现如下所示

```java
	//当访问了结点 e 时调用
    //结点 e 是最新访问的一个结点，此处将结点 e 置为链表的尾结点
    void afterNodeAccess(Node<K,V> e) {
        //last 用来指向链表的尾结点
        LinkedHashMap.Entry<K,V> last;
        //只有当上一次访问的结点不是结点 e 时（(last = tail) != e），才需要进行下一步操作
        if (accessOrder && (last = tail) != e) {
            //p 是最新访问的一个结点，b 是结点 p 的上一个结点，a 是结点 p 的下一个结点
            LinkedHashMap.Entry<K,V> p = (LinkedHashMap.Entry<K,V>)e, b = p.before, a = p.after;
            //因为结点 p 将成为尾结点，所以 after 置为null
            p.after = null;
            //如果 b == null ，说明结点 p 是原链表的头结点，则此时将 head 指向下一个结点 a
            //如果 b != null ，则移除结点 b 对结点 p 的引用
            if (b == null)
                head = a;
            else
                b.after = a;
            //如果 a !=null，说明结点 p 不是原链表的尾结点，则移除结点 a 对结点 p 的引用
            //如果 a == null，则说明结点 p 是原链表的尾结点，则让 last 指向结点 b
            if (a != null)
                a.before = b;
            else
                last = b;
            //如果 last == null，说明原链表为空，则此时头结点就是结点 p
            //如果 last != null，则建立 last 和实际尾结点 p 之间的引用
            if (last == null)
                head = p;
            else {
                p.before = last;
                last.after = p;
            }
            //最新一个引用到的结点就是 tail
            tail = p;
            ++modCount;
        }
    }
```

此外，当 put 方法调用结束时，`afterNodeInsertion` 方法会被调用，用于判断是否移除最近最少使用的元素，依此可以来构建 LRUcache 缓存

```java
    //在插入元素后调用，此方法可用于 LRUcache 算法中移除最近最少使用的元素
    void afterNodeInsertion(boolean evict) {
        LinkedHashMap.Entry<K,V> first;
        if (evict && (first = head) != null && removeEldestEntry(first)) {
            K key = first.key;
            removeNode(hash(key), key, null, false, true);
        }
    }

	//如果在构造函数中参数 accessOrder 传入了 true ，则链表将按照访问顺序来排列
    //即最新访问的结点将处于链表的尾部，依此可以来构建 LRUcache 缓存
    //此方法就用于决定是否移除最旧的缓存，默认返回 false
    protected boolean removeEldestEntry(Map.Entry<K,V> eldest) {
        return false;
    }
```

#### 五、访问元素

在访问元素时，如果 accessOrder 为 true ，则会将访问的元素移至链表的尾部，由于链表内结点位置的改变仅仅是修改几个引用即可，所以这个操作还是非常轻量级的 

```java
	//获取键值为 key 的键值对的 value
    public V get(Object key) {
        Node<K,V> e;
        if ((e = getNode(hash(key), key)) == null)
            return null;
        if (accessOrder)
            afterNodeAccess(e);
        return e.value;
    }

    //获取键值为 key 的键值对的 value，如果 key 不存在，则返回默认值 defaultValue
    public V getOrDefault(Object key, V defaultValue) {
       Node<K,V> e;
       if ((e = getNode(hash(key), key)) == null)
           return defaultValue;
       if (accessOrder)
           afterNodeAccess(e);
       return e.value;
   }
```

#### 六、移除元素

当 HashMap 内部移除了某个结点时，LinkedHashMap 也要移除维护的链表中对该结点的引用，对应的是以下方法

```java
    //在移除结点 e 后调用
    void afterNodeRemoval(Node<K,V> e) {
        //结点 b 指向结点 e 的上一个结点，结点 a 指向结点 e 的下一个结点
        LinkedHashMap.Entry<K,V> p = (LinkedHashMap.Entry<K,V>)e, b = p.before, a = p.after;
        //移除结点 p 对相邻结点的引用
        p.before = p.after = null;
        //如果 b == null，说明结点 p 是原链表的头结点，则移除结点 p 后新的头结点是 a
        //如果 b != null，则更新结点间的引用
        if (b == null)
            head = a;
        else
            b.after = a;
        //如果 a == null，说明结点 a 是尾结点，则移除结点 p 后最新一个访问的结点就是原倒数第二的结点
        //如果 a != null，则更新结点间的引用
        if (a == null)
            tail = b;
        else
            a.before = b;
    }
```

#### 七、LRUCache

在 Android 的实际应用开发中，LRUCache 算法是很常见的，一种典型的用途就是用来在内存中缓存 Bitmap，因为从 IO 流中读取 Bitmap 的资源消耗较大，为了防止多次从磁盘中读取某张图片，所以可以选择在内存中缓存 Bitmap。但内存空间也是有限的，所以也不能每张图片都进行缓存，需要有选择性地缓存一定数量的图片，而最近最少使用算法（LRUCache）是一个可行的选择

这里利用 LinkedHashMap 可以按照元素使用顺序进行排列的特点，来实现一个 LRUCache 策略的缓存

```java
public class LRUCache {

    private static class LRUCacheMap<K, V> extends LinkedHashMap<K, V> {

        //最大的缓存数量
        private final int maxCacheSize;

        public LRUCacheMap(int maxCacheSize) {
            super(16, 0.75F, true);
            this.maxCacheSize = maxCacheSize;
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
            return size() > maxCacheSize;
        }

    }

    public static void main(String[] args) {
        //最大的缓存数量
        final int maxCacheSize = 5;
        LRUCacheMap<String, Integer> map = new LRUCacheMap<>(maxCacheSize);
        for (int i = 0; i < maxCacheSize; i++) {
            map.put("leavesC_" + i, i);
        }
        //输出结果是：leavesC_0 leavesC_1 leavesC_2 leavesC_3 leavesC_4
        System.out.println();
        Set<String> keySet = map.keySet();
        keySet.forEach(key -> System.out.print(key + " "));

        //获取链表的头结点的值，使之移动到链表尾部
        map.get("leavesC_0");
        System.out.println();
        keySet = map.keySet();
        //输出结果是：//leavesC_1 leavesC_2 leavesC_3 leavesC_4 leavesC_0
        keySet.forEach(key -> System.out.print(key + " "));

        //向链表添加元素，使用达到缓存的最大数量
        map.put("leavesC_5", 5);
        System.out.println();
        //输出结果是：//leavesC_2 leavesC_3 leavesC_4 leavesC_0 leavesC_5
        //最近最少使用的元素 leavesC_1 被移除了
        keySet.forEach(key -> System.out.print(key + " "));
    }

}
```