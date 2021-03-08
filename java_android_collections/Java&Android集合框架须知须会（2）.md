> 公众号：[字节数组](https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/36784c0d2b924b04afb5ee09eb16ca6f~tplv-k3u1fbpfcp-watermark.image)，热衷于分享 Android 系统源码解析，Jetpack 源码解析、热门开源库源码解析等面试必备的知识点

> 本系列文章会陆续对 Java 和 Android 的集合框架（JDK 1.8，Android SDK 30）中的几个常见容器结合源码进行介绍，了解不同容器在**数据结构、适用场景、优势点**上的不同，希望对你有所帮助

## 一、HashMap

HashMap 是一种用于存储键值对的数据类型，基于哈希表的 Map 接口的非同步实现，key 可以为 null，不允许插入重复的 key，允许 value 重复

HashMap 实际上是**数组+链表+红黑树**的结合体，其底层包含一个数组，数组中每一项元素的类型分为四种可能：**null、单独一个结点、链表、红黑树**（JDK1.8 开始通过使用红黑树来提高元素查找效率）。当往 HashMap 中存入元素时，会先根据 key 的哈希值得到该元素在数组中的位置（即数组下标），如果该位置上已经存放有其它元素了，那么在这个位置上的元素将以链表或者红黑树的形式来存放，如果该位置上没有元素，就直接向该位置存放元素。因此 HashMap 要求 key 必须是不可变对象，即 key 的哈希值不能发生改变，否则就会导致后续访问时无法定位到它的存放位置了

#### 1、哈希

Hash，一般翻译做哈希或者散列，是把输入的任意对象通过散列算法变换成固定长度的输出，该输出就是散列值。不同的输入可能会散列成相同的输出，所以不可能从散列值来确定唯一的输入值，但可以将散列值作为这个对象的一个特征

哈希的作用可以通过举一个例子来说明。假设存在一千个单词，现在需要从中找到“hello”这个单词的位置索引，那么最直观的做法就是将这些单词存储到一个长度为一千的数组中并进行遍历，最坏的结果就需要遍历一千次。如果单词数量越多，那么需要的数组空间就会越多，平均需要进行遍历的次数也会越高。为了节省内存空间并减少遍历次数，我们可以通过哈希算法拿到每个单词的哈希值，将这些哈希值映射为一个长度为一百的数组内的索引值，在该索引位置上保存对应的单词。如果采用的哈希算法足够优秀，不同的单词得到的哈希值就具有很大的随机性，这样一千个单词就可以均匀地分布到数组内了，最好的情况就是每个数组位置只保存十个单词，这十个单词再按照链表或者其它数据结构串联起来。这样我们在查找的时候只需要计算出“hello”对应的索引值，然后在这个索引位置遍历十个单词即可。如果数组空间足够大，哈希算法得到的索引值足够均匀，那么最好的情况就是只需要进行一次查找就可以得到目标结果，最坏的结果也只是需要查找该位置上的所有单词即可，大大减少了遍历次数

HashMap 内部就采用了哈希算法来存储元素。但由于哈希算法对于不同的输入有可能会散列成相同的输出，而且数组空间不可能是无限大的，所以在同个数组位置上就不可避免的需要存储多个元素了，这种情况就叫做**哈希冲突**。此外，HashMap 不保证元素的存储顺序和迭代顺序，因为根据需要 HashMap 会对元素重新哈希，元素的顺序也会被再次打乱，因此在不同时间段其存储顺序和迭代顺序都可能会发现变化。此外，HashMap 也不保证线程安全，如果有多个线程同时进行写操作的话可能会导致数据错乱甚至线程死锁

#### 2、类声明

```java
	public class HashMap<K, V> extends AbstractMap<K, V> implements Map<K, V>, Cloneable, Serializable
```

#### 3、常量

HashMap 中的全局常量主要看以下几个

```java
	//哈希桶数组的默认容量
    static final int DEFAULT_INITIAL_CAPACITY = 1 << 4;

    //哈希桶数组能够达到的最大容量
    static final int MAXIMUM_CAPACITY = 1 << 30;
	
	//装载因子
    static final float DEFAULT_LOAD_FACTOR = 0.75f;

    //为了提高效率，当链表的长度超出这个值时，就将链表转换为红黑树
    static final int TREEIFY_THRESHOLD = 8;
	
	//当红黑树的长度小于这个值时，就将红黑树转换为链表
	static final int UNTREEIFY_THRESHOLD = 6;
```

装载因子用于规定数组在自动扩容之前数据占有其容量的最高比例，即当数据量占有数组的容量达到这个比例后，数组将自动扩容。装载因子衡量的是一个散列表的空间的使用程度，装载因子越大表示散列表的装填程度越高，反之愈小。对于使用链表的散列表来说，查找一个元素的平均时间是O(1+a)，因此装载因子越大，对空间的利用程度就越高，相对应的是查找效率越低。如果装载因子太小，那么数组的数据将过于稀疏，对空间的利用率就变低，相应查找效率也会提升

官方默认的装载因子大小是 DEFAULT_LOAD_FACTOR，即 0.75，是平衡空间利用率和查找效率两者之后的结果。在实际情况中，如果内存空间较多而对时间效率要求很高，可以选择降低装载因子大小；如果内存空间紧张而对时间效率要求不高，则可以选择加大装载因子

此外，即使装载因子和哈希算法设计得再合理，也难免会出现由于哈希冲突导致链表长度过长的情况，这也将影响 HashMap 的性能。为了优化性能，从 JDK1.8 开始引入了红黑树，当链表长度超出 TREEIFY_THRESHOLD 规定的值时，链表就会被转换为红黑树，利用红黑树快速增删改查的特点以提高 HashMap 的性能

#### 4、变量

```java
    //哈希桶数组，在第一次使用时才初始化
    //容量值应是2的整数倍
    transient Node<K, V>[] table;

    /**
     * Holds cached entrySet(). Note that AbstractMap fields are used
     * for keySet() and values().
     */
    transient Set<Map.Entry<K, V>> entrySet;

    //Map的大小
    transient int size;

    //每当Map的结构发生变化时，此参数就会递增
    //当在对Map进行迭代操作时，迭代器会检查此参数值
    //如果检查到此参数的值发生变化，就说明在迭代的过程中Map的结构发生了变化，因此会直接抛出异常
    transient int modCount;

    //数组的扩容临界点，当数组的数据量达到这个值时就会进行扩容操作
    //计算方法：当前容量 x 装载因子
    int threshold;

    //使用的装载因子值
    final float loadFactor;
```

#### 5、构造函数

```java
	//设置Map的初始化大小和装载因子
    public HashMap(int initialCapacity, float loadFactor) {
        if (initialCapacity < 0)
            throw new IllegalArgumentException("Illegal initial capacity: " + initialCapacity);
        if (initialCapacity > MAXIMUM_CAPACITY)
            initialCapacity = MAXIMUM_CAPACITY;
        if (loadFactor <= 0 || Float.isNaN(loadFactor))
            throw new IllegalArgumentException("Illegal load factor: " + loadFactor);
        this.loadFactor = loadFactor;
        this.threshold = tableSizeFor(initialCapacity);
    }

    //设置初始化大小
    public HashMap(int initialCapacity) {
        this(initialCapacity, DEFAULT_LOAD_FACTOR);
    }

    //使用默认值
    public HashMap() {
        this.loadFactor = DEFAULT_LOAD_FACTOR;
    }

    //传入初始数据
    public HashMap(Map<? extends K, ? extends V> m) {
        this.loadFactor = DEFAULT_LOAD_FACTOR;
        putMapEntries(m, false);
    }
```

#### 6、插入键值对

在上边说过，HashMap 是 **数组+链表+红黑树** 的结合体，数组中每一项元素的类型分为四种可能：**null、单独一个结点、链表、红黑树**

每一个要插入的键值对都会被包装为 Node 对象，根据 key 的哈希值来决定 Node 对象在数组中的位置。如果计算出的位置此时不包含值，即为 null，则直接将 Node 对象放到该位置即可；如果不为 null ，则说明发生了哈希碰撞，此时就需要将 Node 对象插入到链表或者是红黑树中。如果 key 与链表或红黑树中某个已有结点的 key 相等（hash 值相等且两者 equals 成立），则新添加的 Node 对象将覆盖原有数据

**当哈希算法的计算结果越分散均匀，发生哈希碰撞的概率就越小，HashMap 的存取效率就会越高**

Node 类的声明如下所示

```java
    static class Node<K,V> implements Map.Entry<K,V> {
        
        //key 的哈希值
        final int hash;
        final K key;
        V value;
        //下一个结点
        Node<K,V> next;

        Node(int hash, K key, V value, Node<K,V> next) {
            this.hash = hash;
            this.key = key;
            this.value = value;
            this.next = next;
        }

        public final K getKey()        { return key; }
        public final V getValue()      { return value; }
        public final String toString() { return key + "=" + value; }

        public final int hashCode() {
            return Objects.hashCode(key) ^ Objects.hashCode(value);
        }

        public final V setValue(V newValue) {
            V oldValue = value;
            value = newValue;
            return oldValue;
        }

        public final boolean equals(Object o) {
            if (o == this)
                return true;
            if (o instanceof Map.Entry) {
                Map.Entry<?,?> e = (Map.Entry<?,?>)o;
                if (Objects.equals(key, e.getKey()) &&
                    Objects.equals(value, e.getValue()))
                    return true;
            }
            return false;
        }
    }
```

插入键值对的方法是 `put(K key, V value)` 

```java
	public V put(K key, V value) {
        return putVal(hash(key), key, value, false, true);
    }

    //计算 key 的哈希值
    static final int hash(Object key) {
        int h;
        return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
    }
```

putVal 方法较为复杂，因为该方法要考虑以下几种情况：

1. 如果 table 还未初始化或者容量为 0 则进行初始化和扩容
2. 判断是否存在哈希冲突
3. 如果不存在哈希冲突，则直接将该键值对存入计算出来的位置
4. 如果存在哈希冲突，则将键值对添加到该位置的红黑树或者链表上，并且在链表达到最大长度时将链表转换为红黑树
5. 当存在相同 key 的结点时，判断是否需要覆盖旧值
6. 为 LinkedHashMap 预留方法埋点
7. 当保存键值对后，进行必要的扩容

```java
	/**
     * @param hash         hash for key
     * @param key          the key
     * @param value        the value to put
     * @param onlyIfAbsent 为 true 表示不会覆盖有相同 key 的非 null value，否则会覆盖原有值
     * @param evict        if false, the table is in creation mode.
     * @return previous value, or null if none
     */
    final V putVal(int hash, K key, V value, boolean onlyIfAbsent, boolean evict) {
        Node<K, V>[] tab;
        Node<K, V> p;
        int n, i;
        //如果 table 还未初始化或者容量为0，则调用 resize 方法进行初始化
        if ((tab = table) == null || (n = tab.length) == 0)
            n = (tab = resize()).length;
        
        //判断要存入的 key 是否存在哈希冲突
        //p 指向了键值对希望存入的数组位置
        //p 等于 null 说明不存在冲突
        if ((p = tab[i = (n - 1) & hash]) == null)
            //直接在索引 i 处构建包含待存入元素的结点
            tab[i] = newNode(hash, key, value, null);
        
        else { //走入本分支，说明待存入的 key 存在哈希冲突
            
            Node<K, V> e;
            K k;
            //p 值已在上一个 if 语句中赋值了，此处就直接来判断 Node key 的相等性
            if (p.hash == hash && ((k = p.key) == key || (key != null && key.equals(k))))
                //会走进这里，说明 p 结点 key 和待存入的键值对 key 相等
                //此时该位置可能只有一个结点，也有可能是红黑树或者链表，
                //那么 e 就指向该冲突结点
                //此时就已经找到了键值对待存入的位置了
                e = p;
            
            //如果 Node key 不相等，且头结点是 TreeNode 类型，说明此时该位置当前是采用红黑树来处理哈希冲突
            else if (p instanceof TreeNode)
                //如果红黑树中不存在相同 key 的话则插入保存键值对并返回 null，否则不保存并返回该该相同 key 的结点
                e = ((TreeNode<K, V>) p).putTreeVal(this, tab, hash, key, value);
            
            else { //该位置当前是采用链表来处理哈希冲突
                for (int binCount = 0; ; ++binCount) {
                    if ((e = p.next) == null) {
                        //会走进这里，说明遍历到了链表尾部，且链表中每个结点的 key 均不相等
                        //那么就将其添加到链表尾部
                        p.next = newNode(hash, key, value, null);
                        //如果链表的长度已达到允许的最大长度，那么就将链表转换为红黑树
                        if (binCount >= TREEIFY_THRESHOLD - 1) // -1 for 1st
                            treeifyBin(tab, hash);
                        break;
                    }   
                    if (e.hash == hash && ((k = e.key) == key || (key != null && key.equals(k))))
                        //找到了相同 key 的结点，即 e
                        break;
                    p = e;
                }
            }
            
            //如果 e != null，说明原先存在相同 key 的键值对
            //那么就再来判断下是否需要覆盖 value
            if (e != null) {    
                V oldValue = e.value;       
                //如果 onlyIfAbsent 为 false 或者 oldValue 为 null 则覆盖原有值
                if (!onlyIfAbsent || oldValue == null)
                    e.value = value;
                
                //用于 LinkedHashMap ，在 HashMap 中是空实现
                afterNodeAccess(e);
                return oldValue;
            }
        }
        
        ++modCount;
        
        //判断是否需要扩容
        if (++size > threshold)
            resize();
        
        //用于 LinkedHashMap ，在 HashMap 中是空实现
        afterNodeInsertion(evict);
        return null;
    }
```

#### 7、获取 value

获取 value 对应的是 `get(Object key)`方法

```java
	public V get(Object key) {
        Node<K, V> e;
        return (e = getNode(hash(key), key)) == null ? null : e.value;
    }

    //根据 key 获取结点
    final Node<K, V> getNode(int hash, Object key) {
        Node<K, V>[] tab;
        Node<K, V> first, e;
        int n;
        K k;
        //只有当 table 不为空且 hash 对应的位置不为 null 时说明才有可能存在该 key
        if ((tab = table) != null && (n = tab.length) > 0 && (first = tab[(n - 1) & hash]) != null) {
            if (first.hash == hash && ((k = first.key) == key || (key != null && key.equals(k))))
                //如果与头结点相等的话说明找到了对应值
                return first;
            // e != null 说明存在该位置存在链表或红黑树，那么就从这两者中获取
            if ((e = first.next) != null) {
                if (first instanceof TreeNode) //红黑树
                    return ((TreeNode<K, V>) first).getTreeNode(hash, key);
                do { //链表
                    if (e.hash == hash && ((k = e.key) == key || (key != null && key.equals(k))))
                        return e;
                } while ((e = e.next) != null);
            }
        }
        return null;
    }
```

#### 8、移除结点

从 Map 中移除键值对的操作，对于其底层数据结构的体现就是要移除对某个 Node 对象的引用，这个数据结构可能是数组、红黑树、或者链表

```java
	//如果真的存在该 key，则返回对应的 value，否则返回 null
	public V remove(Object key) {
        Node<K, V> e;
        return (e = removeNode(hash(key), key, null, false, true)) == null ?
                null : e.value;
    }

    /**
     * @param value       key对应的值，只有当matchValue为true时才需要使用到，否则忽略该值
     * @param matchValue  如果为 true ，则只有当找到key和value均匹配的结点时才会移除该结点，否则只要key相等就直接移除该元素
     * @param movable if false do not move other nodes while removing
     * @return the node, or null if none
     */
    final Node<K, V> removeNode(int hash, Object key, Object value,
                                boolean matchValue, boolean movable) {
        Node<K, V>[] tab;
        Node<K, V> p;
        int n, index;
        //只有当 table 不为空且 hash 对应的位置不为 null 时说明才有可能存在该 key
        if ((tab = table) != null && (n = tab.length) > 0 && (p = tab[index = (n - 1) & hash]) != null) {
            Node<K, V> node = null, e;
            K k;
            V v;
            if (p.hash == hash && ((k = p.key) == key || (key != null && key.equals(k))))
                //如果与头结点 p 的 key 相等，那么就已经找到了目标 node
                node = p;
            else if ((e = p.next) != null) { //存在红黑树或者链表
                if (p instanceof TreeNode) //红黑树
                    node = ((TreeNode<K, V>) p).getTreeNode(hash, key);
                else { //链表
                    do {
                        if (e.hash == hash && ((k = e.key) == key || (key != null && key.equals(k)))) {
                            node = e;
                            break;
                        }
                        p = e;
                    } while ((e = e.next) != null);
                }
            }
            
            //node != null 说明存在 key 对应结点
            //如果 matchValue 为 false ，则此处就可以直接移除结点 node
            //如果 matchValue 为 true ，则当 value 相等时才需要移除该结点
            if (node != null && (!matchValue || (v = node.value) == value || (value != null && value.equals(v)))) {
                if (node instanceof TreeNode) //红黑树
                    ((TreeNode<K, V>) node).removeTreeNode(this, tab, movable);
                else if (node == p) //对应 key 与头结点相等的情况，此时直接将指针移向下一位即可
                    tab[index] = node.next;
                else //链表
                    p.next = node.next;
                ++modCount;
                --size;
                //用于 LinkedHashMap ，在 HashMap 中是空实现
                afterNodeRemoval(node);
                return node;
            }
        }
        return null;
    }
```

#### 9、哈希算法

在插入、查询和移除键值对时，定位到哈希桶数组的对应位置都是很关键的第一步，只有 HashMap 中的元素尽量分布均匀，才能尽量让数组中的每个位置都只保存一个 Node，避免频繁地去构建和遍历链表或者红黑树，这就需要依靠于一个比较好的哈希算法了

以下是 HashMap 中计算 key 值的哈希值以及根据哈希值获取其在哈希桶数组中位置的方法

```java
	static final int hash(Object key) {
        int h;
        //高位参与运算
        return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
    }

    //根据 key 值获取 Value
    public V get(Object key) {
        Node<K, V> e;
        return (e = getNode(hash(key), key)) == null ? null : e.value;
    }

	//查找指定结点
    final Node<K, V> getNode(int hash, Object key) {
		···
        //只有当 table 不为空且 hash 对应的位置不为 null 才有可获取的元素值
        if ((tab = table) != null && (n = tab.length) > 0 && (first = tab[(n - 1) & hash]) != null) {
           ···
        }
        return null;
    }
```

确定键值对在哈希桶数组的位置的步骤分为三步：

1. 计算 key 的 hashCode：h = key.hashCode()
2. 高位运算：h >>> 16
3. 取模运算：（n - 1) & hash

我也不懂这么取值的优点在于哪里，就不多说了

#### 10、扩容

如果哈希桶数组很大，即使是较差的哈希算法，元素也会比较分散；如果哈希桶数组很小，即使是好的哈希算法也会出现较多哈希碰撞的情况，所以就需要在空间成本和时间成本之间权衡，除了需要设计较好的哈希算法以便减少哈希冲突外，也需要在合适的的时机对哈希桶数组进行扩容

当 HashMap 中的元素越来越多时，因为数组的容量是固定的，所以哈希冲突的几率也会越来越高，为了提高效率，此时就需要对 HashMap 中的数组进行扩容，而扩容操作最消耗性能的地方就在于：**原数组中的数据必须重新计算其在新数组中的位置并迁移到新数组中**

那么 HashMap 扩容操作的触发时机是什么时候呢？当 HashMap 中的元素个数超出 threshold 时（数组容量 与 loadFactor 的乘积），就会进行数组扩容。例如，假设数组当前大小是16，loadFactor 值是0.75，那么当 HashMap 中的元素个数达到12个时，就会自动触发扩容操作，把数组的大小扩充到 2 * 16 = 32，即扩大一倍，然后重新计算每个元素在新数组中的位置，这是一个非常消耗性能的操作，所以如果已经预知到待存入 HashMap 的数据量，那么在初始化 HashMap 时直接指定初始化大小会是一种更为高效的做法

默认情况下，数组的容量是 16，loadFactor 是 0.75，这是**平衡空间利用率和时间效率两者**之后的结果

初始化数组和扩容数组这两个操作对应的是 `resize()`方法

```java
	final Node<K, V>[] resize() {
        //扩容前的数组
        Node<K, V>[] oldTab = table;
        //扩容前数组的容量
        int oldCap = (oldTab == null) ? 0 : oldTab.length;
        //当前的扩容临界值
        int oldThr = threshold;
        //扩容后的数组容量和扩容临界值
        int newCap, newThr = 0;
        if (oldCap > 0) { 
            //oldCap > 0 对应的是 table 已被初始化的情况，此时是来判断是否需要进行扩容
            
            //如果数组已达到最大容量，则不再进行扩容，并将扩容临界点 threshold 提升到 Integer.MAX_VALUE，结束
            if (oldCap >= MAXIMUM_CAPACITY) {
                threshold = Integer.MAX_VALUE;
                return oldTab;
            } else if ((newCap = oldCap << 1) < MAXIMUM_CAPACITY && oldCap >= DEFAULT_INITIAL_CAPACITY) {
                //如果将数组的现有容量提升到两倍依然小于 MAXIMUM_CAPACITY，且现有容量大于等于 DEFAULT_INITIAL_CAPACITY
                //则将数组的容量和扩容临界值均提升为原先的两倍
                newThr = oldThr << 1;
            } 
            
            //此处应该还有一种情况
            //即将数组的现有容量提升到现在的两倍后大于等于 MAXIMUM_CAPACITY 的情况
            //此时 newThr 等于 0，newCap 等于 oldCap 的两倍值
            //此处并没有对 newCap 的数值进行还原，说明 HashMap 是允许扩容后容量超出 MAXIMUM_CAPACITY 的
            //只是在现有容量超出 MAXIMUM_CAPACITY 后，不允许再次进行扩容
        } else if (oldThr > 0) { 
            //oldCap <= 0 && oldThr > 0
            //对应的是 table 还未被初始化，且在调用构造函数时有传入 initialCapacity 或者 Map 的情况
            //此时就直接将容量提升为 threshold，在后边重新计算新的扩容临界值
            newCap = oldThr;
        } else { 
            //oldCap <= 0 && oldThr <= 0
            //对应的是 table 还未被初始化，且调用的是无参构造函数
            //将 table 的容量扩充到默认大小，并使用默认的装载因子来计算扩容临界值
            newCap = DEFAULT_INITIAL_CAPACITY;
            newThr = (int) (DEFAULT_LOAD_FACTOR * DEFAULT_INITIAL_CAPACITY);
        }
        if (newThr == 0) {
            float ft = (float) newCap * loadFactor;
            //计算扩容后新的扩容临界值
            newThr = (newCap < MAXIMUM_CAPACITY && ft < (float) MAXIMUM_CAPACITY ? (int) ft : Integer.MAX_VALUE);
        }
        threshold = newThr;
        @SuppressWarnings({"rawtypes", "unchecked"})
        Node<K, V>[] newTab = (Node<K, V>[]) new Node[newCap];
        table = newTab;
        //如果旧数组中存在值，则需要将其中的数据复制到新数组中
        if (oldTab != null) {
            for (int j = 0; j < oldCap; ++j) {
                Node<K, V> e;
                if ((e = oldTab[j]) != null) {
                    oldTab[j] = null;
                    //e.next == null 说明元素 e 没有产生 hash 冲突，因此可以直接转移该元素
                    if (e.next == null)
                        //计算元素 e 在新数组中的位置
                        newTab[e.hash & (newCap - 1)] = e;
                    else if (e instanceof TreeNode) //存在哈希冲突且是用了红黑树
                        ((TreeNode<K, V>) e).split(this, newTab, j, oldCap);
                    else { //存在哈希冲突且是用了链表
                        Node<K, V> loHead = null, loTail = null;
                        Node<K, V> hiHead = null, hiTail = null;
                        Node<K, V> next;
                        do {
                            next = e.next;
                            if ((e.hash & oldCap) == 0) {
                                if (loTail == null)
                                    loHead = e;
                                else
                                    loTail.next = e;
                                loTail = e;
                            } else {
                                if (hiTail == null)
                                    hiHead = e;
                                else
                                    hiTail.next = e;
                                hiTail = e;
                            }
                        } while ((e = next) != null);
                        if (loTail != null) {
                            loTail.next = null;
                            newTab[j] = loHead;
                        }
                        if (hiTail != null) {
                            hiTail.next = null;
                            newTab[j + oldCap] = hiHead;
                        }
                    }
                }
            }
        }
        return newTab;
    }
```

#### 11、效率测试

这里来测试下不同的初始化大小和不同情况下的 hashCode 值对 HashMap 运行效率的影响

首先来定义作为键值对 key 的类，`hashCode()` 方法直接返回其 value 属性

```java
public class Key {

    private int value;

    public Key(int value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Key key = (Key) o;
        return value == key.value;
    }

    @Override
    public int hashCode() {
        return value;
    }

}
```

初始化大小从 200 到 200000 之间以 10 倍的倍数递增，向不同 HashMap 存入同等数据量的数据，观察存入数据所需要的时间

```java
public class Test {

    private static final int MAX_KEY = 20000;

    private static final Key[] KEYS = new Key[MAX_KEY];

    static {
        for (int i = 0; i < MAX_KEY; i++) {
            KEYS[i] = new Key(i);
        }
    }

    private static void test(int size) {
        long startTime = System.currentTimeMillis();
        Map<Key, Integer> map = new HashMap<>(size);
        for (int i = 0; i < MAX_KEY; i++) {
            map.put(KEYS[i], i);
        }
        long endTime = System.currentTimeMillis();
        System.out.println("初始化大小是：" + size + "，用时：" + (endTime - startTime) + "毫秒");
    }

    public static void main(String[] args) {
        for (int i = 20; i <= MAX_KEY; i *= 10) {
            test(i);
        }
    }

}
```

在上述例子中，各个 Key 对象之间的哈希值各不相同，所以键值对在哈希桶数组中的分布可以说是很均匀的了，此时主要影响性能的就是扩容机制了，由日志可以看出此时不同的初始化大小对 HashMap 的性能影响还不大

```java
初始化大小是：20，用时：4毫秒
初始化大小是：200，用时：3毫秒
初始化大小是：2000，用时：4毫秒
初始化大小是：20000，用时：2毫秒
```

如果让 Key 类的 `hashCode()` 方法固定返回 100，那么每个 Key 对象在存在 HashMap 时肯定都会发生哈希冲突

```java
    @Override
    public int hashCode() {
        return 100;
    }
```

可以看到此时存入同等数据量的数据所需要的时间就呈几何数增长了，说明如果存在大量哈希冲突的话对 HashMap 的影响还是很大的

```java
初始化大小是：20，用时：2056毫秒
初始化大小是：200，用时：1902毫秒
初始化大小是：2000，用时：1892毫秒
初始化大小是：20000，用时：1865毫秒
```

## 二、LinkedHashMap

HashMap 并不保证元素的存储顺序和迭代顺序能够和存入顺序保持一致，即 HashMap 本身是无序的。为了解决这一个问题，Java 提供了 LinkedHashMap 来实现有序的 HashMap

#### 1、类声明

LinkedHashMap 是 HashMap 的子类，它保留了元素的插入顺序，其内部维护着一个**按照元素插入顺序**或者**元素访问顺序**来排列的链表，默认是按照元素的插入顺序来排列，就像使用 ArrayList 一样；如果是按照元素的访问顺序来排列，那么每次访问元素后该元素将移至链表的尾部，可以靠此来实现 LRUcache 缓存算法

```java
	public class LinkedHashMap<K,V> extends HashMap<K,V> implements Map<K,V>
```

#### 2、结点类

HashMap 中每个存入的键值对都会被包装为 Node 对象，LinkedHashMap 则是包装为 Entry 对象，看 `newNode` 方法就知道了。Entry 类在 Node 类的基础上扩展了两个新的成员变量：before 和 after，这两个变量就是 LinkedHashMap 来实现有序访问的关键。每当保存了新的键值对，Entry 就会通过这两个变量将其和之前的键值对串联起来，保存为链表的尾结点，从而保留了键值对的顺序信息

不管 Entry 在 HashMap 内部为了解决哈希冲突采用的是链表还是红黑树，这两个变量的指向都不受数据结构变化的影响。从这也可以看出集合框架在设计时一个很巧妙的地方：LinkedHashMap 内部没有新建一个链表用来维护元素的插入顺序，而是通过扩展父类来实现扩展功能

```java
	static class Entry<K,V> extends HashMap.Node<K,V> {
        //用于指定上一个结点 before 和下一个结点 after
        Entry<K,V> before, after;
        Entry(int hash, K key, V value, Node<K,V> next) {
            super(hash, key, value, next);
        }
    }

    Node<K,V> newNode(int hash, K key, V value, Node<K,V> e) {
        LinkedHashMap.Entry<K,V> p = new LinkedHashMap.Entry<K,V>(hash, key, value, e);
        linkNodeLast(p);
        return p;
    }
```

#### 3、变量

变量 accessOrder 用于决定 LinkedHashMap 中元素的排序方式，如果为 true 就按照元素访问顺序来排序，为 false 就按照元素插入顺序来排序

```java
    //序列化ID
    private static final long serialVersionUID = 3801124242820219131L;

    //指向双向链表的头结点
    transient LinkedHashMap.Entry<K,V> head;

    //指向最新访问的结点
    transient LinkedHashMap.Entry<K,V> tail;

    final boolean accessOrder;
```

#### 4、构造函数

默认情况下 LinkedHashMap 都是按照元素插入顺序来排序

```java
	public LinkedHashMap(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
        accessOrder = false;
    }

    public LinkedHashMap(int initialCapacity) {
        super(initialCapacity);
        accessOrder = false;
    }

    public LinkedHashMap() {
        super();
        accessOrder = false;
    }

    public LinkedHashMap(Map<? extends K, ? extends V> m) {
        super();
        accessOrder = false;
        putMapEntries(m, false);
    }

    public LinkedHashMap(int initialCapacity, float loadFactor, boolean accessOrder) {
        super(initialCapacity, loadFactor);
        this.accessOrder = accessOrder;
    }
```

#### 5、预留的方法

在 HashMap 中有三个预留的空方法，源码注释中也写明这三个函数就是为 LinkedHashMap 预留的

```java
    // Callbacks to allow LinkedHashMap post-actions
    void afterNodeAccess(Node<K,V> p) { }
    void afterNodeInsertion(boolean evict) { }
    void afterNodeRemoval(Node<K,V> p) { }
```

当 HashMap 中的某个结点被访问了（例如调用了 get 方法）且 accessOrder 为 true，那么`afterNodeAccess` 方法就会被调用，该方法用于将最新访问的键值对移至链表的尾部，由于链表内结点位置的改变仅仅是修改几个引用即可，所以这个操作还是非常轻量级的 

```java
	public V get(Object key) {
        Node<K,V> e;
        if ((e = getNode(hash(key), key)) == null)
            return null;
        if (accessOrder)
            afterNodeAccess(e);
        return e.value;
    }

	//当访问了结点 e 时调用
    //结点 e 是最新访问的一个结点，此时就将结点 e 置为链表的尾结点
    void afterNodeAccess(Node<K,V> e) {
        //last 用来指向链表的尾结点
        LinkedHashMap.Entry<K,V> last;
        //只有当 last 和 e 不相等时才需要进行下一步，如果相等说明 e 已经在链表尾部了
        if (accessOrder && (last = tail) != e) {
            LinkedHashMap.Entry<K,V> p = (LinkedHashMap.Entry<K,V>)e, b = p.before, a = p.after;
            //因为结点 p 将成为尾结点，所以 after 置为null
            p.after = null;
            //如果 b == null ，说明结点 p 此时是链表的头结点，那 a 就会成为新的头结点
            //如果 b != null ，则移除结点 b 对结点 p 的引用并和 a 串联起来
            if (b == null)
                head = a;
            else
                b.after = a;
            //如果 a != null，说明结点 p 此时不是链表的尾结点，则移除结点 a 对结点 p 的引用并和 b 串联起来
            //如果 a == null，则说明结点 p 此时是链表的尾结点，那 a 就会成为新的尾结点
            if (a != null)
                a.before = b;
            else
                last = b;
            //如果 last == null，说明原链表为空，则此时头结点就是结点 p
            //如果 last != null，则 p 就会成为新的尾结点
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

当 put 方法被调用时`afterNodeInsertion` 方法也会被调用，该方法用于判断是否移除最近最少使用的元素，依此可以来构建 LRUcache 缓存

```java
    //在插入元素后调用，此方法可用于 LRUcache 算法中移除最近最少使用的元素
    void afterNodeInsertion(boolean evict) {
        LinkedHashMap.Entry<K,V> first;
        if (evict && (first = head) != null && removeEldestEntry(first)) {
            K key = first.key;
            removeNode(hash(key), key, null, false, true);
        }
    }

    //此方法就用于决定是否移除最旧的缓存，默认返回 false
	//可以通过重写该方法来实现按照特定规则移除旧数据
    protected boolean removeEldestEntry(Map.Entry<K,V> eldest) {
        return false;
    }
```

当 HashMap 内部移除了某个结点时，LinkedHashMap 也要通过 `afterNodeRemoval` 方法将对该结点的引用从维护的链表中移除

```java
    //在移除结点 e 后调用
    void afterNodeRemoval(Node<K,V> e) {
        LinkedHashMap.Entry<K,V> p = (LinkedHashMap.Entry<K,V>)e, b = p.before, a = p.after;
        //移除结点 p 对相邻结点的引用
        p.before = p.after = null;
        //如果 b == null，说明结点 p 是链表的头结点，则 a 将成为新的头结点
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

#### 6、LRUCache

在 Android 端的应用开发中，LRUCache 算法（最近最少使用算法）是很常见的，一种典型的用途就是用来在内存中缓存 Bitmap，因为从 IO 流中读取 Bitmap 的资源消耗较大，为了防止多次从磁盘中读取某张图片，所以通常会在内存中缓存 Bitmap。但内存空间也是有限的，所以也不能每张图片都进行缓存，需要有选择性地缓存一定数量的图片，LRUCache 就是最常见的缓存方案之一

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
        //最大缓存数量是 5
        LRUCacheMap<String, Integer> map = new LRUCacheMap<>(5);
        map.put("Java", 1);
        map.put("Jetpack", 2);
        map.put("Kotlin", 3);
        map.put("业志陈", 4);
        map.put("字节数组", 5);
        map.put("leaveC", 6);

        System.out.println();
        Set<String> keySet = map.keySet();
        //输出结果是：Jetpack Kotlin 业志陈 字节数组 leaveC
        keySet.forEach(key -> System.out.print(key + " "));

        //获取链表的头结点的值，使之移动到链表尾部
        map.get("Jetpack");
        System.out.println();
        keySet = map.keySet();
        //输出结果是：Kotlin 业志陈 字节数组 leaveC Jetpack
        keySet.forEach(key -> System.out.print(key + " "));

        //向链表添加元素
        map.put("Dart", 5);
        System.out.println();
        //输出结果是：业志陈 字节数组 leaveC Jetpack Dart
        keySet.forEach(key -> System.out.print(key + " "));
    }

}
```

## 三、HashSet

HashSet 实现了 Set 接口，不允许插入重复的元素，允许包含 null 元素，且不保证元素的迭代顺序，源码十分简单，去掉注释后不到两百行，因为其底层也是通过 HashMap 来实现的，看了上面关于 HashMap 源码的解析后再来看 HashSet 就会有一种“不过如此”的感觉了

向 HashSet 添加的值都会被转换为一个键值对，key 即外部传入的值，value 则由 HashSet 来提供。如果 HashMap 中存在某个 key 值与外部传入的值相等（hashCode() 方法返回值相等， equals() 方法也返回 true），那么待添加的键值对的 value 会覆盖原有数据，但 key 不会改变，即如果向 HashSet 添加一个已存在的元素时，并不会被存到 HashMap 中，从而实现了 HashSet 元素不重复的特性

在此就直接贴出源代码了

```java
public class HashSet<E> extends AbstractSet<E> implements Set<E>, Cloneable, java.io.Serializable{

    static final long serialVersionUID = -5024744406713321676L;

    //HashSet 底层用 HashMap 来存放数据
    //Key 值由外部传入，Value 则由 HashSet 内部来维护
    private transient HashMap<E,Object> map;

    //HashMap 中所有键值对都共享同一个值
    //即所有存入 HashMap 的键值对都是使用这个对象作为值
    private static final Object PRESENT = new Object();

    public HashSet() {
        map = new HashMap<>();
    }

    //使用默认的装载因子，并以此来计算 HashMap 的初始化大小
    //+1 是为了弥补精度损失
    public HashSet(Collection<? extends E> c) {
        map = new HashMap<>(Math.max((int) (c.size()/.75f) + 1, 16));
        addAll(c);
    }

    public HashSet(int initialCapacity, float loadFactor) {
        map = new HashMap<>(initialCapacity, loadFactor);
    }

    public HashSet(int initialCapacity) {
        map = new HashMap<>(initialCapacity);
    }

    //此构造函数为包访问权限，只用于支持 LinkedHashSet
    HashSet(int initialCapacity, float loadFactor, boolean dummy) {
        map = new LinkedHashMap<>(initialCapacity, loadFactor);
    }

    //将对 HashSet 的迭代转换为对 HashMap 的 Key 值的迭代
    public Iterator<E> iterator() {
        return map.keySet().iterator();
    }

    public int size() {
        return map.size();
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    public boolean contains(Object o) {
        return map.containsKey(o);
    }

    //如果 HashMap 中不包含 key 是 e 的键值对，则添加该元素并返回 true
    //如果包含则只会覆盖 value 而不会影响 key，同时返回 false
    //从而实现 HashSet key 不重复的特性
    public boolean add(E e) {
        return map.put(e, PRESENT)==null;
    }

    public boolean remove(Object o) {
        return map.remove(o)==PRESENT;
    }

    public void clear() {
        map.clear();
    }
    
}
```

## 四、LinkedHashSet

LinkedHashSet 其内部源码十分简单，简单到只有几十行代码，从其名字就可以猜出它是 HashSet 的子类，并且是依靠链表来实现有序的 HashSet

HashSet 为 LinkedHashSet 预留了一个构造函数，其 dummy 参数并没有实际意义，只是为了和其它构造函数区分开。其它构造函数会将 map 变量初始化为 HashMap 类型变量，特意预留的构造函数则是会初始化为 LinkedHashMap 类型变量，从而通过 LinkedHashMap 内部的双向链表来实现 LinkedHashSet 自身存取有序，元素唯一的特性

```java
public class HashSet<E> extends AbstractSet<E> implements Set<E>, Cloneable, java.io.Serializable {

	private transient HashMap<E,Object> map;
    
    HashSet(int initialCapacity, float loadFactor, boolean dummy) {
        map = new LinkedHashMap<>(initialCapacity, loadFactor);
    }
    
}


public class LinkedHashSet<E> extends HashSet<E> implements Set<E>, Cloneable, java.io.Serializable {

    private static final long serialVersionUID = -2851667679971038690L;

    public LinkedHashSet(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor, true);
    }

    public LinkedHashSet(int initialCapacity) {
        super(initialCapacity, .75f, true);
    }

    //使用默认的初始容量以及装载因子
    public LinkedHashSet() {
        super(16, .75f, true);
    }

    public LinkedHashSet(Collection<? extends E> c) {
        super(Math.max(2*c.size(), 11), .75f, true);
        addAll(c);
    }
    
    @Override
    public Spliterator<E> spliterator() {
        return Spliterators.spliterator(this, Spliterator.DISTINCT | Spliterator.ORDERED);
    }

}
```