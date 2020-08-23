> 本系列文章会陆续对  Java 集合框架（Java Collections Framework，JDK1.8）中的几个常用容器结合源码进行介绍，帮助读者建立起对 Java 集合框架清晰而深入的理解，也算是对自己所学内容的一个总结归纳
>
> 项目主页：https://github.com/leavesC/AndroidGuide

阅读本节内容需要读者对 HashMap 、HashSet 和 LinkedHashMap 的源码有所了解，因为 LinkedHashSet 的内部实现都是来自于这三个容器类，其内部源码十分简单，简单到它只有一个成员变量、四个构造函数、一个 Set 接口的方法

LinkedHashSet  的所有源码如下所示

```java
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
```

LinkedHashSet  继承于 HashSet，而 LinkedHashSet 调用的父类构造函数均是

```java
    private transient HashMap<E,Object> map;
    
    HashSet(int initialCapacity, float loadFactor, boolean dummy) {
        map = new LinkedHashMap<>(initialCapacity, loadFactor);
    }
```

即 LinkedHashSet  底层是依靠 LinkedHashMap 来实现数据存取的，而 LinkedHashMap 继承于 HashMap，在内部自己维护了一条双向链表用于保存元素的插入顺序，因此使得 LinkedHashSet 也具有了存取有序，元素唯一的特点

关于 LinkedHashSet 的源码实在也没什么好讲的，它的实现都是依靠其他容器类来组合支持的，所以如果想了解 LinkedHashSet ，就只能先去了解 HashMap 、HashSet 和 LinkedHashMap 的源码

**如果想多了解一些 Java 的集合框架源码解析，可以看这里：[Java集合框架源码解析](https://github.com/leavesC/AndroidGuide)**