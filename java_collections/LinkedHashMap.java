package java.util;

import java.util.function.Consumer;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.io.IOException;

public class LinkedHashMap<K,V>
    extends HashMap<K,V>
    implements Map<K,V>
{

    //LinkedHashMap 扩展了 HashMap.Node 类
    //在其基础上新增了两个成员变量用于指定上一个结点 before 和下一个结点 after
    static class Entry<K,V> extends HashMap.Node<K,V> {
        Entry<K,V> before, after;
        Entry(int hash, K key, V value, Node<K,V> next) {
            super(hash, key, value, next);
        }
    }

    //序列化ID
    private static final long serialVersionUID = 3801124242820219131L;

    //指向双向链表的头结点
    transient LinkedHashMap.Entry<K,V> head;

    //指向最新插入的一个结点
    transient LinkedHashMap.Entry<K,V> tail;

    //如果为true，则内部元素按照访问顺序排序
    //如果为false，则内部元素按照插入顺序排序
    final boolean accessOrder;

    //将结点 p 插入链表尾部
    private void linkNodeLast(LinkedHashMap.Entry<K,V> p) {
        LinkedHashMap.Entry<K,V> last = tail;
        tail = p;
        //如果 last == null，说明此前链表为空，则头结点应为 p
        if (last == null)
            head = p;
        else {
            //更新结点间的引用
            p.before = last;
            last.after = p;
        }
    }

    //将结点 src 替换为 dst
    private void transferLinks(LinkedHashMap.Entry<K,V> src, LinkedHashMap.Entry<K,V> dst) {
        //（b 和 dst 的上一个结点）指向 src 的上一个结点
        //（a 和 dst 的下一个结点）指向 src 的下一个结点
        LinkedHashMap.Entry<K,V> b = dst.before = src.before;
        LinkedHashMap.Entry<K,V> a = dst.after = src.after;
        if (b == null)
            head = dst;
        else
            b.after = dst;
        if (a == null)
            tail = dst;
        else
            a.before = dst;
    }

    //重置
    void reinitialize() {
        super.reinitialize();
        head = tail = null;
    }

    //重载父类 HashMap 的方法
    //构建一个新结点，并将结点插入到链表尾部
    Node<K,V> newNode(int hash, K key, V value, Node<K,V> e) {
        LinkedHashMap.Entry<K,V> p =
            new LinkedHashMap.Entry<K,V>(hash, key, value, e);
        linkNodeLast(p);
        return p;
    }

    //重载父类 HashMap 的方法
    //用于将 TreeNodes 转换到普通节点
    Node<K,V> replacementNode(Node<K,V> p, Node<K,V> next) {
        LinkedHashMap.Entry<K,V> q = (LinkedHashMap.Entry<K,V>)p;
        LinkedHashMap.Entry<K,V> t =
            new LinkedHashMap.Entry<K,V>(q.hash, q.key, q.value, next);
        transferLinks(q, t);
        return t;
    }

    //重载父类 HashMap 的方法
    //构建一个新的红黑树结点，并将结点插入到链表尾部
    TreeNode<K,V> newTreeNode(int hash, K key, V value, Node<K,V> next) {
        TreeNode<K,V> p = new TreeNode<K,V>(hash, key, value, next);
        linkNodeLast(p);
        return p;
    }

    //重载父类 HashMap 的方法
    //依据当前结点构造一个树结点
    TreeNode<K,V> replacementTreeNode(Node<K,V> p, Node<K,V> next) {
        LinkedHashMap.Entry<K,V> q = (LinkedHashMap.Entry<K,V>)p;
        TreeNode<K,V> t = new TreeNode<K,V>(q.hash, q.key, q.value, next);
        transferLinks(q, t);
        return t;
    }

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

    //在插入元素后调用，此方法可用于 LRUcache 算法中移除最近最少使用的元素
    void afterNodeInsertion(boolean evict) {
        LinkedHashMap.Entry<K,V> first;
        if (evict && (first = head) != null && removeEldestEntry(first)) {
            K key = first.key;
            removeNode(hash(key), key, null, false, true);
        }
    }

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

    void internalWriteEntries(java.io.ObjectOutputStream s) throws IOException {
        for (LinkedHashMap.Entry<K,V> e = head; e != null; e = e.after) {
            s.writeObject(e.key);
            s.writeObject(e.value);
        }
    }

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

    //判断集合是否包含值为 value 的键值对
    public boolean containsValue(Object value) {
        for (LinkedHashMap.Entry<K,V> e = head; e != null; e = e.after) {
            V v = e.value;
            if (v == value || (value != null && value.equals(v)))
                return true;
        }
        return false;
    }

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

    //清空集合元素
    public void clear() {
        super.clear();
        head = tail = null;
    }

    //如果在构造函数中参数 accessOrder 传入了 true ，则链表将按照访问顺序来排列
    //即最新访问的结点将处于链表的尾部，依此可以来构建 LRUcache 缓存
    //此方法就用于决定是否移除最旧的缓存，默认返回 false
    protected boolean removeEldestEntry(Map.Entry<K,V> eldest) {
        return false;
    }

    //用于获取集合中的所有键
    public Set<K> keySet() {
        Set<K> ks = keySet;
        if (ks == null) {
            ks = new LinkedKeySet();
            keySet = ks;
        }
        return ks;
    }

    final class LinkedKeySet extends AbstractSet<K> {
        public final int size()                 { return size; }
        public final void clear()               { LinkedHashMap.this.clear(); }
        public final Iterator<K> iterator() {
            return new LinkedKeyIterator();
        }
        public final boolean contains(Object o) { return containsKey(o); }
        public final boolean remove(Object key) {
            return removeNode(hash(key), key, null, false, true) != null;
        }
        public final Spliterator<K> spliterator()  {
            return Spliterators.spliterator(this, Spliterator.SIZED |
                                            Spliterator.ORDERED |
                                            Spliterator.DISTINCT);
        }
        public final void forEach(Consumer<? super K> action) {
            if (action == null)
                throw new NullPointerException();
            int mc = modCount;
            for (LinkedHashMap.Entry<K,V> e = head; e != null; e = e.after)
                action.accept(e.key);
            if (modCount != mc)
                throw new ConcurrentModificationException();
        }
    }

    //用于获取集合中的所有值
    public Collection<V> values() {
        Collection<V> vs = values;
        if (vs == null) {
            vs = new LinkedValues();
            values = vs;
        }
        return vs;
    }

    final class LinkedValues extends AbstractCollection<V> {
        public final int size()                 { return size; }
        public final void clear()               { LinkedHashMap.this.clear(); }
        public final Iterator<V> iterator() {
            return new LinkedValueIterator();
        }
        public final boolean contains(Object o) { return containsValue(o); }
        public final Spliterator<V> spliterator() {
            return Spliterators.spliterator(this, Spliterator.SIZED |
                                            Spliterator.ORDERED);
        }
        public final void forEach(Consumer<? super V> action) {
            if (action == null)
                throw new NullPointerException();
            int mc = modCount;
            for (LinkedHashMap.Entry<K,V> e = head; e != null; e = e.after)
                action.accept(e.value);
            if (modCount != mc)
                throw new ConcurrentModificationException();
        }
    }

    /**
     * Returns a {@link Set} view of the mappings contained in this map.
     * The set is backed by the map, so changes to the map are
     * reflected in the set, and vice-versa.  If the map is modified
     * while an iteration over the set is in progress (except through
     * the iterator's own <tt>remove</tt> operation, or through the
     * <tt>setValue</tt> operation on a map entry returned by the
     * iterator) the results of the iteration are undefined.  The set
     * supports element removal, which removes the corresponding
     * mapping from the map, via the <tt>Iterator.remove</tt>,
     * <tt>Set.remove</tt>, <tt>removeAll</tt>, <tt>retainAll</tt> and
     * <tt>clear</tt> operations.  It does not support the
     * <tt>add</tt> or <tt>addAll</tt> operations.
     * Its {@link Spliterator} typically provides faster sequential
     * performance but much poorer parallel performance than that of
     * {@code HashMap}.
     *
     * @return a set view of the mappings contained in this map
     */
    public Set<Map.Entry<K,V>> entrySet() {
        Set<Map.Entry<K,V>> es;
        return (es = entrySet) == null ? (entrySet = new LinkedEntrySet()) : es;
    }

    final class LinkedEntrySet extends AbstractSet<Map.Entry<K,V>> {
        public final int size()                 { return size; }
        public final void clear()               { LinkedHashMap.this.clear(); }
        public final Iterator<Map.Entry<K,V>> iterator() {
            return new LinkedEntryIterator();
        }
        public final boolean contains(Object o) {
            if (!(o instanceof Map.Entry))
                return false;
            Map.Entry<?,?> e = (Map.Entry<?,?>) o;
            Object key = e.getKey();
            Node<K,V> candidate = getNode(hash(key), key);
            return candidate != null && candidate.equals(e);
        }
        public final boolean remove(Object o) {
            if (o instanceof Map.Entry) {
                Map.Entry<?,?> e = (Map.Entry<?,?>) o;
                Object key = e.getKey();
                Object value = e.getValue();
                return removeNode(hash(key), key, value, true, true) != null;
            }
            return false;
        }
        public final Spliterator<Map.Entry<K,V>> spliterator() {
            return Spliterators.spliterator(this, Spliterator.SIZED |
                                            Spliterator.ORDERED |
                                            Spliterator.DISTINCT);
        }
        public final void forEach(Consumer<? super Map.Entry<K,V>> action) {
            if (action == null)
                throw new NullPointerException();
            int mc = modCount;
            for (LinkedHashMap.Entry<K,V> e = head; e != null; e = e.after)
                action.accept(e);
            if (modCount != mc)
                throw new ConcurrentModificationException();
        }
    }

    //将键值对依次传递给函数 apply
    public void forEach(BiConsumer<? super K, ? super V> action) {
        if (action == null)
            throw new NullPointerException();
        int mc = modCount;
        for (LinkedHashMap.Entry<K,V> e = head; e != null; e = e.after)
            action.accept(e.key, e.value);
        if (modCount != mc)
            throw new ConcurrentModificationException();
    }

    //将键值对依次传递给函数 apply，并以函数的返回值作为键值对的新值
    public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
        if (function == null)
            throw new NullPointerException();
        int mc = modCount;
        for (LinkedHashMap.Entry<K,V> e = head; e != null; e = e.after)
            e.value = function.apply(e.key, e.value);
        if (modCount != mc)
            throw new ConcurrentModificationException();
    }

    //迭代器
    abstract class LinkedHashIterator {

        //下一个结点
        LinkedHashMap.Entry<K,V> next;

        //当前结点
        LinkedHashMap.Entry<K,V> current;

        int expectedModCount;

        LinkedHashIterator() {
            next = head;
            expectedModCount = modCount;
            current = null;
        }

        //判断是否还有下一个元素
        public final boolean hasNext() {
            return next != null;
        }

        //用于获取下一个结点
        final LinkedHashMap.Entry<K,V> nextNode() {
            LinkedHashMap.Entry<K,V> e = next;
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();
            if (e == null)
                throw new NoSuchElementException();
            current = e;
            next = e.after;
            return e;
        }

        //移除结点 current
        public final void remove() {
            Node<K,V> p = current;
            if (p == null)
                throw new IllegalStateException();
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();
            current = null;
            K key = p.key;
            removeNode(hash(key), key, null, false, false);
            expectedModCount = modCount;
        }
    }

    //用于迭代 key
    final class LinkedKeyIterator extends LinkedHashIterator
        implements Iterator<K> {
        public final K next() { return nextNode().getKey(); }
    }

    //用于迭代 value
    final class LinkedValueIterator extends LinkedHashIterator
        implements Iterator<V> {
        public final V next() { return nextNode().value; }
    }

    //用于迭代键值对
    final class LinkedEntryIterator extends LinkedHashIterator
        implements Iterator<Map.Entry<K,V>> {
        public final Map.Entry<K,V> next() { return nextNode(); }
    }

}
