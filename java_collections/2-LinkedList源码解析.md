LinkedList 同时实现了 List 接口和 Deque 接口，所以既可以将 LinkedList 当做一个有序容器，也可以将之看作一个队列（Queue），同时又可以看作一个栈（Stack）。虽然 LinkedList 和 ArrayList 一样都实现了 List 接口，但其底层是通过**双向链表**来实现的，所以插入和删除元素的效率都要比 ArrayList 高，但也因此随机访问的效率要比 ArrayList 低

#### 一、LinkedList 的类声明

```java
public class LinkedList<E>
        extends AbstractSequentialList<E>
        implements List<E>, Deque<E>, Cloneable, java.io.Serializable
```

从其实现的几个接口可以看出来，LinkedList 是支持快速访问，可克隆，可序列化的，而且可以将之看成一个支持有序访问的 队列/栈

上面说过，LinkedList 内部是通过双向链表的数据结构来实现的，对于链表中的结点来说，除了首尾两个结点外，其余每个结点除了存储本结点的数据元素外，还分别有两个指针用于指向其上下两个相邻结点，这个结点类就是 LinkedList 中的静态类 Node

```java
	//结点类
    private static class Node<E> {

        //当前结点包含的实际元素
        E item;

        //指向下一个结点
        Node<E> next;

        //指向上一个结点
        Node<E> prev;

        Node(Node<E> prev, E element, Node<E> next) {
            this.item = element;
            this.next = next;
            this.prev = prev;
        }
    }
```

#### 二、包含的成员变量

```java
 	//双向链表包含的结点总数
    transient int size = 0;

    //双向链表的头结点
    transient Node<E> first;

    //双向链表的尾结点
    transient Node<E> last;

    //序列化ID
    private static final long serialVersionUID = 876323262645176354L;
```

当中的成员变量 first 和 last 分别用于指向链表的头部和尾部结点，因此 LinkedList 的数据结构图是类似于这样的

![](https://upload-images.jianshu.io/upload_images/2552605-5fc0a4fb9515e7c3.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

#### 三、构造函数

不同于 ArrayList，因为 LinkedList 使用的是链表结构，所以 LinkedList 不需要去请求一片连续的内存空间来存储数据，而是在每次有新的元素需要添加进来时，再来动态请求内存空间。因此 LinkedList 的两个构造函数要简单得多

```java
	public LinkedList() {
    }

    //传入初始数据
    public LinkedList(Collection<? extends E> c) {
        this();
        addAll(c);
    }
```

#### 四、添加元素

`add(E e)` 方法用于向链表的尾部添加结点，因为有 `last` 指向链表的尾结点，因此向尾部添加新元素只需要修改几个引用即可，效率较高

```java
    //将元素 e 作为尾结点添加
    //因为 LinkedList 允许添加相同元素，所以此方法固定返回 true
    public boolean add(E e) {
        linkLast(e);
        return true;
    }
    
    //将元素 e 置为尾结点
    void linkLast(E e) {
        //先保存原尾结点
        final Node<E> l = last;
        //构建新的尾结点，并指向原尾结点
        final Node<E> newNode = new Node<>(l, e, null);
        last = newNode;
        //如果原尾结点为 null，说明原链表包含的元素个数为 0，则此时插入的尾结点同时即为头结点
        //如果原尾结点不为 null，则将 next 指向新的尾结点
        if (l == null)
            first = newNode;
        else
            l.next = newNode;
        //元素个数加1
        size++;
        modCount++;
    }
```

![](https://upload-images.jianshu.io/upload_images/2552605-1a9718c006ce91af.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

`add(int index, E element)` 方法用于向指定索引处添加元素，需要先通过索引 index 获取相应位置的结点，并在该位置开辟一个新的结点来存储元素 element，最后还需要修改相邻结点间的引用

```java
 	//在索引 index 处插入元素 element
    public void add(int index, E element) {
        //判断索引大小是否合法，不合法则抛出 IndexOutOfBoundsException
        checkPositionIndex(index);
        //如果 index == size，则将 element 作为尾结点来添加
        //否则则在索引 index 前开辟一个新结点
        if (index == size)
            linkLast(element);
        else
            linkBefore(element, node(index));
    }

	//将元素 e 置为 succ 结点的上一个结点
    void linkBefore(E e, Node<E> succ) {
        //保存 succ 的上一个结点信息
        final Node<E> pred = succ.prev;
        //构建元素 e 对应的结点
        final Node<E> newNode = new Node<>(pred, e, succ);
        //将结点 succ 的上一个结点指向 newNode
        succ.prev = newNode;
        //如果 pred 为 null，说明 succ 是头结点，则将 newNode 置为新的头结点
        if (pred == null)
            first = newNode;
        else
            pred.next = newNode;
        //元素个数加1
        size++;
        modCount++;
    }
```

![](https://upload-images.jianshu.io/upload_images/2552605-817b1e290041b1dc.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

#### 五、移除元素

`remove()` 方法有两种重载形式，其内部都是通过调用 `unlink(Node<E> x)` 方法来移除指定结点在链表中的引用，不同于 ArrayList 在移除元素时可能导致的大量数据移动，LinkedList 只需要通过移除引用即可将指定元素从链表中移除

```java
    //移除索引 index 处的结点
    public E remove(int index) {
        //判断索引大小是否合法，不合法则抛出 IndexOutOfBoundsException
        checkElementIndex(index);
        return unlink(node(index));
    }

	//对链表进行正向遍历，移除第一个元素值为 o 的结点
    //如果移除成功则返回 true，否则返回 false
    public boolean remove(Object o) {
        if (o == null) {
            for (Node<E> x = first; x != null; x = x.next) {
                if (x.item == null) {
                    //移除结点 x
                    unlink(x);
                    return true;
                }
            }
        } else {
            for (Node<E> x = first; x != null; x = x.next) {
                if (o.equals(x.item)) {
                    //移除结点 x
                    unlink(x);
                    return true;
                }
            }
        }
        return false;
    }

	//移除结点 x 并返回其包含的元素值
    E unlink(Node<E> x) {
        final E element = x.item;
        final Node<E> next = x.next;
        final Node<E> prev = x.prev;
        //如果 prev == null，说明结点 x 为头结点，则将头结点置为原先的第二个结点
        //如果 prev != null，则移除对结点 x 的引用
        if (prev == null) {
            first = next;
        } else {
            prev.next = next;
            x.prev = null;
        }
        //如果 next == null，则说明结点 x 为尾结点，则将尾结点置为原先的倒数第二个结点
        //如果 next != null，则移除对结点 x 的引用
        if (next == null) {
            last = prev;
        } else {
            next.prev = prev;
            x.next = null;
        }
        //帮助GC回收
        x.item = null;
        //元素个数减1
        size--;
        modCount++;
        return element;
    }
```

#### 六、获取/修改 元素

在获取或修改指定索引的元素前，都需要先通过正向遍历或者反向遍历获取到该结点，如果集合包含的数据量很大，那么要相应的代价也会很大，因此说 LinkedList 的查找效率并不高

```java
    //获取索引 index 处的结点元素
    public E get(int index) {
        //判断索引大小是否合法，不合法则抛出 IndexOutOfBoundsException
        checkElementIndex(index);
        return node(index).item;
    }

    //将索引 index 处的结点包含的元素修改为 element，并返回旧元素
    public E set(int index, E element) {
        //判断索引大小是否合法，不合法则抛出 IndexOutOfBoundsException
        checkElementIndex(index);
        Node<E> x = node(index);
        E oldVal = x.item;
        x.item = element;
        return oldVal;
    }

    //获取索引 index 处的结点
    Node<E> node(int index) {
        //size >> 1 的含义即为：将 size 值除以 2
        //如果 index 靠近链表的头部，则从头部向尾部正向遍历查找结点
        //如果 index 靠近链表的尾部，则从尾部向头部反向遍历查找结点 
        if (index < (size >> 1)) {
            Node<E> x = first;
            for (int i = 0; i < index; i++)
                x = x.next;
            return x;
        } else {
            Node<E> x = last;
            for (int i = size - 1; i > index; i--)
                x = x.prev;
            return x;
        }
    }
```

#### 七、几个常用的方法

```java
	//判断是否包含元素 o
    public boolean contains(Object o) {
        return indexOf(o) != -1;
    }

    //获取元素个数
    public int size() {
        return size;
    }
    
    //清空链表元素
    //这里将各个结点之间的引用都切断了，这并不是必须的，但这样有助于GC回收
    public void clear() {
        for (Node<E> x = first; x != null; ) {
            Node<E> next = x.next;
            x.item = null;
            x.next = null;
            x.prev = null;
            x = next;
        }
        first = last = null;
        size = 0;
        modCount++;
    }
    
    //返回第一个元素值为 o 的结点所在的索引值
    //如果查找不到，则返回 -1
    public int indexOf(Object o) {
        int index = 0;
        if (o == null) {
            for (Node<E> x = first; x != null; x = x.next) {
                if (x.item == null)
                    return index;
                index++;
            }
        } else {
            for (Node<E> x = first; x != null; x = x.next) {
                if (o.equals(x.item))
                    return index;
                index++;
            }
        }
        return -1;
    }

    //返回最后一个元素值为 o 的结点所在的索引值
    //如果查找不到，则返回 -1
    public int lastIndexOf(Object o) {
        int index = size;
        if (o == null) {
            for (Node<E> x = last; x != null; x = x.prev) {
                index--;
                if (x.item == null)
                    return index;
            }
        } else {
            for (Node<E> x = last; x != null; x = x.prev) {
                index--;
                if (o.equals(x.item))
                    return index;
            }
        }
        return -1;
    }
```

#### 八、Deque接口

以上介绍的几个方法都是 **List** 接口中所声明的，接下来看下 **Deque** 接口中的方法

其实 Deque 接口中很多方法的含义都是类似的，且一些方法都是相互调用的，并不算复杂

```java
	//将元素 e 置为头结点
    public void addFirst(E e) {
        linkFirst(e);
    }

    //将元素 e 置为尾结点
    public void addLast(E e) {
        linkLast(e);
    }
    
    //将元素 e 作为尾结点添加
    public boolean offer(E e) {
        return add(e);
    }

    //将元素 e 作为头结点添加
    public boolean offerFirst(E e) {
        addFirst(e);
        return true;
    }

    //将元素 e 作为尾结点添加
    public boolean offerLast(E e) {
        addLast(e);
        return true;
    }

    //获取头部结点的元素值
    public E peekFirst() {
        final Node<E> f = first;
        return (f == null) ? null : f.item;
    }

    //获取尾部结点的元素值
    public E peekLast() {
        final Node<E> l = last;
        return (l == null) ? null : l.item;
    }

    //获取头部结点的元素值，并将之从链表中移除
    public E pollFirst() {
        final Node<E> f = first;
        return (f == null) ? null : unlinkFirst(f);
    }

    //获取尾部结点的元素值，并将之从链表中移除
    public E pollLast() {
        final Node<E> l = last;
        return (l == null) ? null : unlinkLast(l);
    }

    //将元素 e 作为头结点添加
    public void push(E e) {
        addFirst(e);
    }

    //获取头部结点的元素值，并将之从链表中移除
    public E pop() {
        return removeFirst();
    }

    //从链表头部向尾部正向遍历，移除第一个元素值为 o 的结点
    //如果移除成功则返回 true，否则返回 false
    public boolean removeFirstOccurrence(Object o) {
        return remove(o);
    }

    //从链表尾部向头部反向遍历，移除第一个元素值为 o 的结点
    //如果移除成功则返回 true，否则返回 false
    public boolean removeLastOccurrence(Object o) {
        if (o == null) {
            for (Node<E> x = last; x != null; x = x.prev) {
                if (x.item == null) {
                    unlink(x);
                    return true;
                }
            }
        } else {
            for (Node<E> x = last; x != null; x = x.prev) {
                if (o.equals(x.item)) {
                    unlink(x);
                    return true;
                }
            }
        }
        return false;
    }
```

#### 九、效率比较

上面说过，LinkedList 相比 ArrayList 添加和移除元素的效率会高些，但随机访问元素的效率要比 ArrayList 低，这里我也来做个测试，看下两者之间的差距

分别向 ArrayList 和 LinkedList 存入同等数据量的数据，然后各自移除 100 个元素以及遍历 10000 个元素，观察两者所用的时间

```java
public static void main(String[] args) {
        List<String> stringArrayList = new ArrayList<>();
        for (int i = 0; i < 300000; i++) {
            stringArrayList.add("leavesC " + i);
        }
        //开始时间
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            stringArrayList.remove(100 + i);
        }
        //结束时间
        long endTime = System.currentTimeMillis();
        System.out.println("移除 ArrayList 中的100个元素,用时：" + (endTime - startTime) + "毫秒");

        //开始时间
        startTime = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
            stringArrayList.get(i);
        }
        //结束时间
        endTime = System.currentTimeMillis();
        System.out.println("遍历 ArrayList 中的10000个元素,用时：" + (endTime - startTime) + "毫秒");


        List<String> stringLinkedList = new LinkedList<>();
        for (int i = 0; i < 300000; i++) {
            stringLinkedList.add("leavesC " + i);
        }
        //开始时间
        startTime = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            stringLinkedList.remove(100 + i);
        }
        //结束时间
        endTime = System.currentTimeMillis();
        System.out.println("移除 LinkedList 中的100个元素,用时：" + (endTime - startTime) + "毫秒");
        //开始时间
        startTime = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
            stringLinkedList.get(i);
        }
        //结束时间
        endTime = System.currentTimeMillis();
        System.out.println("遍历 LinkedList 中的10000个元素,用时：" + (endTime - startTime) + "毫秒");
    }
```
可以看出来，两者之间的差距还是非常大的，因此，在使用集合时需要根据实际情况来判断到底哪一种数据结构才更加适合

```java
移除 ArrayList 中的100个元素,用时：11毫秒
遍历 ArrayList 中的10000个元素,用时：1毫秒
移除 LinkedList 中的100个元素,用时：0毫秒
遍历 LinkedList 中的10000个元素,用时：246毫秒
```
