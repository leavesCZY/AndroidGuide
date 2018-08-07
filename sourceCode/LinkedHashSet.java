package java.util;

public class LinkedHashSet<E>
    extends HashSet<E>
    implements Set<E>, Cloneable, java.io.Serializable {

    //序列化ID
    private static final long serialVersionUID = -2851667679971038690L;

    //自定义初始容量与装载因子
    public LinkedHashSet(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor, true);
    }

    //自定义初始容量
    public LinkedHashSet(int initialCapacity) {
        super(initialCapacity, .75f, true);
    }

    //使用默认的初始容量以及装载因子
    public LinkedHashSet() {
        super(16, .75f, true);
    }

    //使用初始数据、默认的初始容量以及装载因子
    public LinkedHashSet(Collection<? extends E> c) {
        super(Math.max(2*c.size(), 11), .75f, true);
        addAll(c);
    }

    //并行遍历迭代器
    @Override
    public Spliterator<E> spliterator() {
        return Spliterators.spliterator(this, Spliterator.DISTINCT | Spliterator.ORDERED);
    }

}
