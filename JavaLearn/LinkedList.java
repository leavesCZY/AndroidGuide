package java.util;

import java.util.function.Consumer;

public class LinkedList<E>
        extends AbstractSequentialList<E>
        implements List<E>, Deque<E>, Cloneable, java.io.Serializable {

    //双向链表包含的结点总数
    transient int size = 0;

    //双向链表的头结点
    transient Node<E> first;

    //双向链表的尾结点
    transient Node<E> last;

    //序列化ID
    private static final long serialVersionUID = 876323262645176354L;

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

    public LinkedList() {
    }

    //传入初始数据
    public LinkedList(Collection<? extends E> c) {
        this();
        addAll(c);
    }

    //将元素 e 置为头结点
    private void linkFirst(E e) {
        //先保存原头结点
        final Node<E> f = first;
        //构建新的头结点，并指向原头结点
        final Node<E> newNode = new Node<>(null, e, f);
        first = newNode;
        //如果原头结点为 null，说明原链表包含的元素个数为 0，则此时插入的头结点同时即为尾结点
        //如果原头结点不为 null，则将 prev 指向新的头结点
        if (f == null)
            last = newNode;
        else
            f.prev = newNode;
        //元素个数加1
        size++;
        //每当链表的结构发生变化时，此参数就会递增
        //当在对链表进行迭代操作时，迭代器会检查此参数值
        //如果检查到此参数的值发生变化，就说明在迭代的过程中链表的结构发生了变化，因此会直接抛出异常
        modCount++;
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

    //移除头结点 f 并返回其包含的元素值
    private E unlinkFirst(Node<E> f) {
        //取出头结点包含的元素值
        final E element = f.item;
        //取出头结点指向的下一个结点
        final Node<E> next = f.next;
        //帮助GC回收
        f.item = null;
        f.next = null;
        //头结点变为原先的第二个结点
        first = next;
        //如果 next 为 null，说明原先的链表只包含一个元素，则移除头结点后元素个数为空，头结点和尾结点均为 null
        //如果 next 不为 null，则作为头结点，其指向的上一个结点应为 null
        if (next == null)
            last = null;
        else
            next.prev = null;
        //元素个数减1
        size--;
        modCount++;
        return element;
    }

    //移除尾结点 l 并返回其包含的元素值
    private E unlinkLast(Node<E> l) {
        //取出尾结点包含的元素值
        final E element = l.item;
        //取出尾结点指向的上一个结点
        final Node<E> prev = l.prev;
        //帮助GC回收
        l.item = null;
        l.prev = null;
        //尾结点变为原先倒数的第二个结点
        last = prev;
        //如果 prev 为 null，说明原先的链表只包含一个元素，则移除尾结点后元素个数为空，头结点和尾结点均为 null
        //如果 prev 不为 null，则作为尾结点，其指向的下一个结点应为 null
        if (prev == null)
            first = null;
        else
            prev.next = null;
        //元素个数减1
        size--;
        modCount++;
        return element;
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

    //获取头结点
    public E getFirst() {
        final Node<E> f = first;
        if (f == null)
            throw new NoSuchElementException();
        return f.item;
    }

    //获取尾结点
    public E getLast() {
        final Node<E> l = last;
        if (l == null)
            throw new NoSuchElementException();
        return l.item;
    }

    //将头部结点从链表中移除，并返回其包含的元素值
    public E removeFirst() {
        final Node<E> f = first;
        if (f == null)
            throw new NoSuchElementException();
        return unlinkFirst(f);
    }

    //将尾部结点从链表中移除，并返回其包含的元素值
    public E removeLast() {
        final Node<E> l = last;
        if (l == null)
            throw new NoSuchElementException();
        return unlinkLast(l);
    }

    //将元素 e 置为头结点
    public void addFirst(E e) {
        linkFirst(e);
    }

    //将元素 e 置为尾结点
    public void addLast(E e) {
        linkLast(e);
    }

    //判断是否包含元素 o
    public boolean contains(Object o) {
        return indexOf(o) != -1;
    }

    //获取元素个数
    public int size() {
        return size;
    }

    //将元素 e 作为尾结点添加
    //因为 LinkedList 允许添加相同元素，所以此方法固定返回 true
    public boolean add(E e) {
        linkLast(e);
        return true;
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

    //从尾结点开始添加集合 c
    public boolean addAll(Collection<? extends E> c) {
        return addAll(size, c);
    }

    //以索引 index 为起点插入集合 c，不会覆盖原有数据 
    public boolean addAll(int index, Collection<? extends E> c) {
        //判断索引大小是否合法，不合法则抛出 IndexOutOfBoundsException
        checkPositionIndex(index);
        //将集合转换为数组
        Object[] a = c.toArray();
        int numNew = a.length;
        if (numNew == 0)
            return false;
        //用于标记要从哪个结点开始插入数据
        Node<E> succ;
        //succ 的前一个结点
        Node<E> pred;
        //如果 index == size，则说明是要将数据直接添加到尾部
        //如果 index != size，则定位到数据要插入的起始结点，从该结点插入数据
        if (index == size) {
            succ = null;
            pred = last;
        } else {
            succ = node(index);
            pred = succ.prev;
        }
        for (Object o : a) {
            @SuppressWarnings("unchecked") 
            E e = (E) o;
            Node<E> newNode = new Node<>(pred, e, null);
            //如果 pred == null，说明是要从原头结点处开始插入数据或者是原链表数据量为0，此时新的头结点即为 newNode
            //如果 pred != null，则更新结点指针
            if (pred == null)
                first = newNode;
            else
                pred.next = newNode;
            pred = newNode;
        }
        //如果 succ == null，说明是将数据直接添加到尾部的情况，此时直接更新指针引用
        //否则此时需要将因为插入新的链表片段导致原链表断开的结点重新连接上
        if (succ == null) {
            last = pred;
        } else {
            pred.next = succ;
            succ.prev = pred;
        }
        //更新元素数量
        size += numNew;
        modCount++;
        return true;
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

    //移除索引 index 处的结点
    public E remove(int index) {
        //判断索引大小是否合法，不合法则抛出 IndexOutOfBoundsException
        checkElementIndex(index);
        return unlink(node(index));
    }

    //判断 index 是否可用于索引数据
    private boolean isElementIndex(int index) {
        return index >= 0 && index < size;
    }

    //判断 index 是否可用于迭代器或者是元素插入操作
    private boolean isPositionIndex(int index) {
        return index >= 0 && index <= size;
    }

    /**
     * Constructs an IndexOutOfBoundsException detail message.
     * Of the many possible refactorings of the error handling code,
     * this "outlining" performs best with both server and client VMs.
     */
    private String outOfBoundsMsg(int index) {
        return "Index: " + index + ", Size: " + size;
    }

    //判断 index 是否可用于索引数据
    private void checkElementIndex(int index) {
        if (!isElementIndex(index))
            throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
    }

    //判断 index 是否可用于迭代器或者是元素插入操作
    private void checkPositionIndex(int index) {
        if (!isPositionIndex(index))
            throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
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

    //获取头部结点的元素值
    public E peek() {
        final Node<E> f = first;
        return (f == null) ? null : f.item;
    }

    //获取头部结点的元素值
    public E element() {
        return getFirst();
    }

    //获取头部结点的元素值，并将之从链表中移除
    public E poll() {
        final Node<E> f = first;
        return (f == null) ? null : unlinkFirst(f);
    }

    //获取头部结点的元素值，并将之从链表中移除
    public E remove() {
        return removeFirst();
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

    //获取集合元素迭代器，可以正向遍历以及反向遍历，提供添加、移除、修改元素的功能
    //如果在迭代的过程中链表的数据结构发生了变化，则抛出 ConcurrentModificationException 异常
    public ListIterator<E> listIterator(int index) {
        checkPositionIndex(index);
        return new ListItr(index);
    }

    private class ListItr implements ListIterator<E> {

        //迭代器最后一个获取的结点
        private Node<E> lastReturned;

        //如果是正向迭代，则 next 指向的是 lastReturned 的下一个结点
        //如果是反向迭代，则 next 指向 lastReturned
        private Node<E> next;

        //游标
        private int nextIndex;
        
        //用于验证链表的数据结构在迭代的过程中是否被修改了
        private int expectedModCount = modCount;

        ListItr(int index) {
            //传入参数 index 用于标记迭代操作的起始位置
            //如果 index == size，则 next 赋值为 null，否则定位到该索引位置的结点
            next = (index == size) ? null : node(index);
            nextIndex = index;
        }

        //用于判断正向迭代是否还有下一个可获取的元素
        public boolean hasNext() {
            return nextIndex < size;
        }

        //获取当前游标正向迭代时指向的下一个结点的元素
        public E next() {
            checkForComodification();
            //如果没有下一个元素了，则抛出异常
            if (!hasNext())
                throw new NoSuchElementException();
            lastReturned = next;
            next = next.next;
            //因为结点是向链表尾部移动，所以游标加1
            nextIndex++;
            return lastReturned.item;
        }

        //用于判断反向迭代时是否还有下一个可获取的元素
        public boolean hasPrevious() {
            return nextIndex > 0;
        }

        //用于获取当前结点的上一个结点
        public E previous() {
            checkForComodification();
            //如果没有下一个元素了，则抛出异常
            if (!hasPrevious())
                throw new NoSuchElementException();
            //如果 next == null，此时就将定位点都移到链表尾结点，是为了循环遍历？
            //如果 next != null，则将定位点移到上一个结点
            lastReturned = next = (next == null) ? last : next.prev;
            //因为结点是向链表头部移动，所以索引减1
            nextIndex--;
            return lastReturned.item;
        }

        public int nextIndex() {
            return nextIndex;
        }

        public int previousIndex() {
            return nextIndex - 1;
        }

        //移除结点 lastReturned
        public void remove() {
            checkForComodification();
            if (lastReturned == null)
                throw new IllegalStateException();
            Node<E> lastNext = lastReturned.next;
            unlink(lastReturned);
            //如果 next == lastReturned，说明当前是从链表的头部向尾部遍历
            if (next == lastReturned)
                next = lastNext;
            else
                nextIndex--;
            lastReturned = null;
            expectedModCount++;
        }

        //重置结点 lastReturned 包含的元素值
        public void set(E e) {
            if (lastReturned == null)
                throw new IllegalStateException();
            checkForComodification();
            lastReturned.item = e;
        }

        //向链表添加结点
        public void add(E e) {
            checkForComodification();
            lastReturned = null;
            //如果 next == null，说明已经是遍历到结点尾部，则直接将新元素添加在最尾端
            //如果 next != null，则在 next 结点之前开辟一个新结点用来容纳元素 e
            if (next == null)
                linkLast(e);
            else
                linkBefore(e, next);
            nextIndex++;
            expectedModCount++;
        }

        //正向遍历链表剩下的结点
        public void forEachRemaining(Consumer<? super E> action) {
            Objects.requireNonNull(action);
            while (modCount == expectedModCount && nextIndex < size) {
                action.accept(next.item);
                lastReturned = next;
                next = next.next;
                nextIndex++;
            }
            checkForComodification();
        }

        //判断集合的结构在修改过程中是否被修改了
        //如果是则抛出异常
        final void checkForComodification() {
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();
        }
    }

    //获取反向迭代器
    public Iterator<E> descendingIterator() {
        return new DescendingIterator();
    }

    //用于进行反向迭代操作的适配器
    private class DescendingIterator implements Iterator<E> {
        
        private final ListItr itr = new ListItr(size());

        //是否有下一个未迭代的元素
        public boolean hasNext() {
            return itr.hasPrevious();
        }

        //获取下一个元素
        public E next() {
            return itr.previous();
        }

        //移除当前定位的索引的元素
        public void remove() {
            itr.remove();
        }
    }

    @SuppressWarnings("unchecked")
    private LinkedList<E> superClone() {
        try {
            return (LinkedList<E>) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e);
        }
    }

    /**
     * Returns a shallow copy of this {@code LinkedList}. (The elements
     * themselves are not cloned.)
     *
     * @return a shallow copy of this {@code LinkedList} instance
     */
    public Object clone() {
        LinkedList<E> clone = superClone();

        // Put clone into "virgin" state
        clone.first = clone.last = null;
        clone.size = 0;
        clone.modCount = 0;

        // Initialize clone with our elements
        for (Node<E> x = first; x != null; x = x.next)
            clone.add(x.item);

        return clone;
    }

    //返回包含所有元素值的数组对象
    public Object[] toArray() {
        Object[] result = new Object[size];
        int i = 0;
        for (Node<E> x = first; x != null; x = x.next)
            result[i++] = x.item;
        return result;
    }

    //返回包含所有元素值的数组对象
    //数组类型由传入的参数类型来决定
    @SuppressWarnings("unchecked")
    public <T> T[] toArray(T[] a) {
        //如果传入的数组的大小小于 LinkedList 的元素总数 size
        //则重新构建一个大小为 size 的数组
        if (a.length < size)
            a = (T[]) java.lang.reflect.Array.newInstance(
                    a.getClass().getComponentType(), size);
        int i = 0;
        Object[] result = a;
        for (Node<E> x = first; x != null; x = x.next)
            result[i++] = x.item;

        if (a.length > size)
            a[size] = null;

        return a;
    }

    /**
     * Saves the state of this {@code LinkedList} instance to a stream
     * (that is, serializes it).
     *
     * @serialData The size of the list (the number of elements it
     * contains) is emitted (int), followed by all of its
     * elements (each an Object) in the proper order.
     */
    private void writeObject(java.io.ObjectOutputStream s)
            throws java.io.IOException {
        // Write out any hidden serialization magic
        s.defaultWriteObject();

        // Write out size
        s.writeInt(size);

        // Write out all elements in the proper order.
        for (Node<E> x = first; x != null; x = x.next)
            s.writeObject(x.item);
    }

    /**
     * Reconstitutes this {@code LinkedList} instance from a stream
     * (that is, deserializes it).
     */
    @SuppressWarnings("unchecked")
    private void readObject(java.io.ObjectInputStream s)
            throws java.io.IOException, ClassNotFoundException {
        // Read in any hidden serialization magic
        s.defaultReadObject();

        // Read in size
        int size = s.readInt();

        // Read in all elements in the proper order.
        for (int i = 0; i < size; i++)
            linkLast((E) s.readObject());
    }

    /**
     * Creates a <em><a href="Spliterator.html#binding">late-binding</a></em>
     * and <em>fail-fast</em> {@link Spliterator} over the elements in this
     * list.
     * <p>
     * <p>The {@code Spliterator} reports {@link Spliterator#SIZED} and
     * {@link Spliterator#ORDERED}.  Overriding implementations should document
     * the reporting of additional characteristic values.
     *
     * @return a {@code Spliterator} over the elements in this list
     * @implNote The {@code Spliterator} additionally reports {@link Spliterator#SUBSIZED}
     * and implements {@code trySplit} to permit limited parallelism..
     * @since 1.8
     */
    @Override
    public Spliterator<E> spliterator() {
        return new LLSpliterator<E>(this, -1, 0);
    }

    /**
     * A customized variant of Spliterators.IteratorSpliterator
     */
    static final class LLSpliterator<E> implements Spliterator<E> {
        static final int BATCH_UNIT = 1 << 10;  // batch array size increment
        static final int MAX_BATCH = 1 << 25;  // max batch array size;
        final LinkedList<E> list; // null OK unless traversed
        Node<E> current;      // current node; null until initialized
        int est;              // size estimate; -1 until first needed
        int expectedModCount; // initialized when est set
        int batch;            // batch size for splits

        LLSpliterator(LinkedList<E> list, int est, int expectedModCount) {
            this.list = list;
            this.est = est;
            this.expectedModCount = expectedModCount;
        }

        final int getEst() {
            int s; // force initialization
            final LinkedList<E> lst;
            if ((s = est) < 0) {
                if ((lst = list) == null)
                    s = est = 0;
                else {
                    expectedModCount = lst.modCount;
                    current = lst.first;
                    s = est = lst.size;
                }
            }
            return s;
        }

        public long estimateSize() {
            return (long) getEst();
        }

        public Spliterator<E> trySplit() {
            Node<E> p;
            int s = getEst();
            if (s > 1 && (p = current) != null) {
                int n = batch + BATCH_UNIT;
                if (n > s)
                    n = s;
                if (n > MAX_BATCH)
                    n = MAX_BATCH;
                Object[] a = new Object[n];
                int j = 0;
                do {
                    a[j++] = p.item;
                } while ((p = p.next) != null && j < n);
                current = p;
                batch = j;
                est = s - j;
                return Spliterators.spliterator(a, 0, j, Spliterator.ORDERED);
            }
            return null;
        }

        public void forEachRemaining(Consumer<? super E> action) {
            Node<E> p;
            int n;
            if (action == null) throw new NullPointerException();
            if ((n = getEst()) > 0 && (p = current) != null) {
                current = null;
                est = 0;
                do {
                    E e = p.item;
                    p = p.next;
                    action.accept(e);
                } while (p != null && --n > 0);
            }
            if (list.modCount != expectedModCount)
                throw new ConcurrentModificationException();
        }

        public boolean tryAdvance(Consumer<? super E> action) {
            Node<E> p;
            if (action == null) throw new NullPointerException();
            if (getEst() > 0 && (p = current) != null) {
                --est;
                E e = p.item;
                current = p.next;
                action.accept(e);
                if (list.modCount != expectedModCount)
                    throw new ConcurrentModificationException();
                return true;
            }
            return false;
        }

        public int characteristics() {
            return Spliterator.ORDERED | Spliterator.SIZED | Spliterator.SUBSIZED;
        }
    }

}
