```java
package java.util;

import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

public class ArrayList<E> extends AbstractList<E>
        implements List<E>, RandomAccess, Cloneable, java.io.Serializable
{

    //序列化ID
    private static final long serialVersionUID = 8683452581122892189L;

    //集合默认的初始大小
    private static final int DEFAULT_CAPACITY = 10;

    //如果外部为集合设置的初始化大小为 0，则 elementData 指向空数组对象 EMPTY_ELEMENTDATA
    private static final Object[] EMPTY_ELEMENTDATA = {};

    //如果在初始化集合时使用的是无参数的构造函数，则 elementData 指向空数组对象 DEFAULTCAPACITY_EMPTY_ELEMENTDATA
    private static final Object[] DEFAULTCAPACITY_EMPTY_ELEMENTDATA = {};

    //包含实际元素的数组
    transient Object[] elementData;

    //集合大小
    private int size;

    //指定集合的初始容量，以此来进行数组的初始化操作
    public ArrayList(int initialCapacity) {
        if (initialCapacity > 0) {
            this.elementData = new Object[initialCapacity];
        } else if (initialCapacity == 0) {
            this.elementData = EMPTY_ELEMENTDATA;
        } else {
            throw new IllegalArgumentException("Illegal Capacity: "+initialCapacity);
        }
    }

    //使用默认的初始化大小
    public ArrayList() {
        this.elementData = DEFAULTCAPACITY_EMPTY_ELEMENTDATA;
    }

    //传入一份初始数据，以此进行初始化
    public ArrayList(Collection<? extends E> c) {
        elementData = c.toArray();
        if ((size = elementData.length) != 0) {
            // c.toArray might (incorrectly) not return Object[] (see 6260652)
            if (elementData.getClass() != Object[].class)
                elementData = Arrays.copyOf(elementData, size, Object[].class);
        } else {
            this.elementData = EMPTY_ELEMENTDATA;
        }
    }

    //移除不包含元素的数组空间，将数组的空间缩减到和集合同个大小
    public void trimToSize() {
        //每当集合的结构发生变化时，modCount 就会递增
        //当在对集合进行迭代操作时，迭代器会检查此参数值
        //如果检查到此参数的值发生变化，就说明在迭代的过程中集合的结构发生了变化，因此会直接抛出异常
        modCount++;
        if (size < elementData.length) {
            elementData = (size == 0)
              ? EMPTY_ELEMENTDATA
              : Arrays.copyOf(elementData, size);
        }
    }

    //触发集合进行扩容操作，参数 minCapacity 表示集合扩容后的最小空间
    //如果在向集合进行赋值操作前已知数据量大小，则直接调用此方法让集合直接扩容到该大小有助于提高集合的运行效率
    //如果是让集合在赋值的过程中自动扩容，则可能会需要进行多次扩容操作，而每次扩容都需要复制原有数据到新数组，这会导致运行效率降低
    public void ensureCapacity(int minCapacity) {
        //1. 如果当前数组还未进行任何赋值操作，即 elementData == DEFAULTCAPACITY_EMPTY_ELEMENTDATA，则数组空间只由参数 minCapacity 影响
        //2. 如果当前数组已进行过赋值操作，即 elementData != DEFAULTCAPACITY_EMPTY_ELEMENTDATA，则当前数组大小可能还是在使用默认容量 DEFAULT_CAPACITY
        //   则此时只有当 minCapacity 大于 DEFAULT_CAPACITY 时，才需要进行扩容
        int minExpand = (elementData != DEFAULTCAPACITY_EMPTY_ELEMENTDATA)
            // any size if not default element table
            ? 0
            // larger than default for default empty table. It's already
            // supposed to be at default size.
            : DEFAULT_CAPACITY;

        if (minCapacity > minExpand) {
            ensureExplicitCapacity(minCapacity);
        }
    }

    //检查当前的数组容量，minCapacity 用于指定当前需要的最小空间
    //如果数组容量没有达到一定标准，则需要进行扩容
    private void ensureCapacityInternal(int minCapacity) {
        if (elementData == DEFAULTCAPACITY_EMPTY_ELEMENTDATA) {
            //数组空间至少需要扩容到 DEFAULT_CAPACITY
            minCapacity = Math.max(DEFAULT_CAPACITY, minCapacity);
        }
        ensureExplicitCapacity(minCapacity);
    }

    private void ensureExplicitCapacity(int minCapacity) {
        modCount++;
        //如果当前数组大小的确是比需要的最小空间 minCapacity 小，则进行扩容
        if (minCapacity - elementData.length > 0)
            grow(minCapacity);
    }

    //数组可扩充到的最大空间
    private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

    //对数组进行扩容
    private void grow(int minCapacity) {
        //扩容前的数组大小
        int oldCapacity = elementData.length;
        //oldCapacity >> 1 的含义即为：将 oldCapacity 值除以 2
        //即先假设扩容后的空间大小是原先的1.5倍
        int newCapacity = oldCapacity + (oldCapacity >> 1);
        //如果 newCapacity 依然是达不到最小空间要求，则直接将空间扩大到由 minCapacity 指定的大小
        if (newCapacity - minCapacity < 0)
            newCapacity = minCapacity;
        //如果扩容后的数组空间超出了最大容量限制，则将容量定为 Integer.MAX_VALUE
        if (newCapacity - MAX_ARRAY_SIZE > 0)
            newCapacity = hugeCapacity(minCapacity);
        //构建符合容量大小的数组并复制原数组的数据
        elementData = Arrays.copyOf(elementData, newCapacity);
    }

    private static int hugeCapacity(int minCapacity) {
        if (minCapacity < 0) // overflow
            throw new OutOfMemoryError();
        return (minCapacity > MAX_ARRAY_SIZE) ?
            Integer.MAX_VALUE :
            MAX_ARRAY_SIZE;
    }

    //获取集合大小
    public int size() {
        return size;
    }

    //判断集合是否为空
    public boolean isEmpty() {
        return size == 0;
    }

    //判断集合是否包含对象 o
    public boolean contains(Object o) {
        return indexOf(o) >= 0;
    }

    //获取对象 o 在集合中所占的第一个索引位置
    //如果集合不包含对象 o，则返回 -1
    public int indexOf(Object o) {
        if (o == null) {
            for (int i = 0; i < size; i++)
                if (elementData[i]==null)
                    return i;
        } else {
            for (int i = 0; i < size; i++)
                if (o.equals(elementData[i]))
                    return i;
        }
        return -1;
    }

    //获取对象 o 在集合中所占的最后一个索引位置
    //如果集合不包含对象 o，则返回 -1
    public int lastIndexOf(Object o) {
        if (o == null) {
            for (int i = size-1; i >= 0; i--)
                if (elementData[i]==null)
                    return i;
        } else {
            for (int i = size-1; i >= 0; i--)
                if (o.equals(elementData[i]))
                    return i;
        }
        return -1;
    }

    /**
     * Returns a shallow copy of this <tt>ArrayList</tt> instance.  (The
     * elements themselves are not copied.)
     *
     * @return a clone of this <tt>ArrayList</tt> instance
     */
    public Object clone() {
        try {
            ArrayList<?> v = (ArrayList<?>) super.clone();
            v.elementData = Arrays.copyOf(elementData, size);
            v.modCount = 0;
            return v;
        } catch (CloneNotSupportedException e) {
            // this shouldn't happen, since we are Cloneable
            throw new InternalError(e);
        }
    }

    //将集合转换为数组
    public Object[] toArray() {
        return Arrays.copyOf(elementData, size);
    }

    //返回包含所有元素值的数组对象
    //数组类型由传入的参数类型来决定
    @SuppressWarnings("unchecked")
    public <T> T[] toArray(T[] a) {
        if (a.length < size)
            // Make a new array of a's runtime type, but my contents:
            return (T[]) Arrays.copyOf(elementData, size, a.getClass());
        System.arraycopy(elementData, 0, a, 0, size);
        if (a.length > size)
            a[size] = null;
        return a;
    }

    //通过索引直接访问数组
    @SuppressWarnings("unchecked")
    E elementData(int index) {
        return (E) elementData[index];
    }

    //获取索引 index 处的元素值
    public E get(int index) {
        rangeCheck(index);
        return elementData(index);
    }

    //将索引 index 出的元素值置为 element，并返回原始数值
    public E set(int index, E element) {
        rangeCheck(index);
        E oldValue = elementData(index);
        elementData[index] = element;
        return oldValue;
    }

    //向集合添加数据
    public boolean add(E e) {
        //检查是否需要扩容
        ensureCapacityInternal(size + 1);
        //赋值
        elementData[size++] = e;
        return true;
    }

    //将元素 element 添加索引 index 位置
    public void add(int index, E element) {
        rangeCheckForAdd(index);
        //检查是否需要扩容
        ensureCapacityInternal(size + 1);
        //将索引 index 后的所有数值向后推移一位 
        System.arraycopy(elementData, index, elementData, index + 1,size - index);
        //将 element 插入到空出的位置
        elementData[index] = element;
        //集合大小加1
        size++;
    }

    //移除指定索引处的元素值，并返回该值
    public E remove(int index) {
        rangeCheck(index);
        modCount++;
        //待移除的元素值
        E oldValue = elementData(index);
        //因为要移除元素导致需要移动的元素数量
        int numMoved = size - index - 1;
        //因为要移除的元素可能刚好是数组最后一位，所以 numMoved 可能为 0
        //所以只在 numMoved > 0 的时候才需要对数组的元素值进行移动
        if (numMoved > 0)
            System.arraycopy(elementData, index+1, elementData, index, numMoved);
        //不管数组是否需要对元素值进行移动，数组的最后一位都是无效数据了
        //此处将之置为 null 以帮助GC回收                
        elementData[--size] = null;
        return oldValue;
    }

    //移除集合中包含的第一位元素值为 o 的对象
    //如果包含该对象，则返回 true ，否则返回 false
    public boolean remove(Object o) {
        if (o == null) {
            for (int index = 0; index < size; index++)
                if (elementData[index] == null) {
                    fastRemove(index);
                    return true;
                }
        } else {
            for (int index = 0; index < size; index++)
                if (o.equals(elementData[index])) {
                    fastRemove(index);
                    return true;
                }
        }
        return false;
    }

    //移除指定索引处的元素
    private void fastRemove(int index) {
        modCount++;
        //需要移动的数组元素数量
        int numMoved = size - index - 1;
        //因为要移除的元素可能刚好是数组最后一位，所以 numMoved 可能为 0
        //所以只在 numMoved > 0 的时候才需要对数组的元素值进行移动
        if (numMoved > 0)
            System.arraycopy(elementData, index+1, elementData, index, numMoved);
        //不管数组是否需要对元素值进行移动，数组的最后一位都是无效数据了
        //此处将之置为 null 以帮助GC回收 
        elementData[--size] = null;
    }

    //清空集合元素
    public void clear() {
        modCount++;
        for (int i = 0; i < size; i++)
            elementData[i] = null;
        size = 0;
    }

    //向集合添加数据
    //如果待添加的数据不为空则返回 true，否则返回 false
    public boolean addAll(Collection<? extends E> c) {
        Object[] a = c.toArray();
        int numNew = a.length;
        //检查是否需要扩容
        ensureCapacityInternal(size + numNew);
        //将数组 a 复制到 elementData 的尾端
        System.arraycopy(a, 0, elementData, size, numNew);
        size += numNew;
        return numNew != 0;
    }

    //从指定索引处添加数据
    //如果待添加的数据不为空则返回 true，否则返回 false
    public boolean addAll(int index, Collection<? extends E> c) {
        rangeCheckForAdd(index);
        Object[] a = c.toArray();
        int numNew = a.length;
        //检查是否需要扩容
        ensureCapacityInternal(size + numNew);
        //需要移动的数组元素数量
        int numMoved = size - index;
        //因为要添加的数据可能刚好是从数组最尾端开始添加，所以 numMoved 可能为 0
        //所以只在 numMoved > 0 的时候才需要对数组的元素值进行移动，以此空出位置给数组 a
        if (numMoved > 0)
            System.arraycopy(elementData, index, elementData, index + numNew, numMoved);
        //将数组 a 包含的数据添加到 elementData 中
        System.arraycopy(a, 0, elementData, index, numNew);
        size += numNew;
        return numNew != 0;
    }

    //移除两个索引值之间的所有数据
    //包含 fromIndex 对应的元素，不包含 toIndex 对应的元素
    protected void removeRange(int fromIndex, int toIndex) {
        modCount++;
        //需要移动的数组元素数量
        //此处是直接将 toIndex 后的数组元素向前移动，以此来覆盖 fromIndex 到 toIndex 之间的数据
        int numMoved = size - toIndex;
        System.arraycopy(elementData, toIndex, elementData, fromIndex, numMoved);
        //移除数据后集合的大小
        int newSize = size - (toIndex-fromIndex);
        //将无效数据置为 null，帮助GC回收
        for (int i = newSize; i < size; i++) {
            elementData[i] = null;
        }
        size = newSize;
    }

    //检查索引值的有效性
    private void rangeCheck(int index) {
        if (index >= size)
            throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
    }

    //检查索引值的有效性
    private void rangeCheckForAdd(int index) {
        if (index > size || index < 0)
            throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
    }

    /**
     * Constructs an IndexOutOfBoundsException detail message.
     * Of the many possible refactorings of the error handling code,
     * this "outlining" performs best with both server and client VMs.
     */
    private String outOfBoundsMsg(int index) {
        return "Index: "+index+", Size: "+size;
    }

    //移除集合中所有 c 包含的相同元素
    //如果有改动到集合，则返回 true，否则返回 false
    public boolean removeAll(Collection<?> c) {
        Objects.requireNonNull(c);
        return batchRemove(c, false);
    }

    //只保留集合和 c 包含的相同数据
    //如果有改动到集合，则返回 true，否则返回 false
    public boolean retainAll(Collection<?> c) {
        Objects.requireNonNull(c);
        return batchRemove(c, true);
    }

    //对集合进行数据过滤
    //如果有改动到集合，则返回 true，否则返回 false
    private boolean batchRemove(Collection<?> c, boolean complement) {
        final Object[] elementData = this.elementData;
        int r = 0, w = 0;
        boolean modified = false;
        try {
            for (; r < size; r++)
                //elementData 中符合条件的元素将逐渐从尾部向头部集中    
                if (c.contains(elementData[r]) == complement)
                    elementData[w++] = elementData[r];
        } finally {
            //r != size 的情况应该是在发生了异常时才会出现
            //此处直接将未遍历到的元素添加回 elementData 尾端
            if (r != size) {
                System.arraycopy(elementData, r,
                                 elementData, w,
                                 size - r);
                w += size - r;
            }
            //w != size 说明有元素被移除了
            if (w != size) {
                //清空无效数据，帮助GC回收
                for (int i = w; i < size; i++)
                    elementData[i] = null;
                modCount += size - w;
                size = w;
                modified = true;
            }
        }
        return modified;
    }

    /**
     * Save the state of the <tt>ArrayList</tt> instance to a stream (that
     * is, serialize it).
     *
     * @serialData The length of the array backing the <tt>ArrayList</tt>
     *             instance is emitted (int), followed by all of its elements
     *             (each an <tt>Object</tt>) in the proper order.
     */
    private void writeObject(java.io.ObjectOutputStream s)
        throws java.io.IOException{
        // Write out element count, and any hidden stuff
        int expectedModCount = modCount;
        s.defaultWriteObject();
        // Write out size as capacity for behavioural compatibility with clone()
        s.writeInt(size);
        // Write out all elements in the proper order.
        for (int i=0; i<size; i++) {
            s.writeObject(elementData[i]);
        }
        if (modCount != expectedModCount) {
            throw new ConcurrentModificationException();
        }
    }

    /**
     * Reconstitute the <tt>ArrayList</tt> instance from a stream (that is,
     * deserialize it).
     */
    private void readObject(java.io.ObjectInputStream s)
        throws java.io.IOException, ClassNotFoundException {
        elementData = EMPTY_ELEMENTDATA;
        // Read in size, and any hidden stuff
        s.defaultReadObject();
        // Read in capacity
        s.readInt(); // ignored
        if (size > 0) {
            // be like clone(), allocate array based upon size not capacity
            ensureCapacityInternal(size);
            Object[] a = elementData;
            // Read in all elements in the proper order.
            for (int i=0; i<size; i++) {
                a[i] = s.readObject();
            }
        }
    }

    //获取集合元素迭代器，可以正向遍历以及反向遍历，提供添加、移除、修改元素的功能
    //如果在迭代的过程中数组的数据结构发生了变化，则抛出 ConcurrentModificationException 异常
    public ListIterator<E> listIterator(int index) {
        if (index < 0 || index > size)
            throw new IndexOutOfBoundsException("Index: "+index);
        return new ListItr(index);
    }

    /**
     * Returns a list iterator over the elements in this list (in proper
     * sequence).
     *
     * <p>The returned list iterator is <a href="#fail-fast"><i>fail-fast</i></a>.
     *
     * @see #listIterator(int)
     */
    public ListIterator<E> listIterator() {
        return new ListItr(0);
    }

    //返回集合迭代器
    public Iterator<E> iterator() {
        return new Itr();
    }

    //一个优化版本的迭代器
    private class Itr implements Iterator<E> {
        
        //lastRet 指向的元素的下一个元素的索引
        int cursor;

        //最后一个返回的元素的索引
        //如果值为 -1，说明还未返回过元素或者改元素被移除了
        int lastRet = -1;

        //用于验证集合的数据结构在迭代的过程中是否被修改了
        int expectedModCount = modCount;

        //是否还有元素未被遍历
        public boolean hasNext() {
            return cursor != size;
        }

        //获取下一个元素
        @SuppressWarnings("unchecked")
        public E next() {
            checkForComodification();
            int i = cursor;
            //如果索引值超出集合的可索引范围则抛出异常
            if (i >= size)
                throw new NoSuchElementException();
            Object[] elementData = ArrayList.this.elementData;
            //如果索引值超出数组的可索引范围则抛出异常
            if (i >= elementData.length)
                throw new ConcurrentModificationException();
            //游标递增
            cursor = i + 1;
            return (E) elementData[lastRet = i];
        }

        //移除 lastRet 位置的元素
        public void remove() {
            if (lastRet < 0)
                throw new IllegalStateException();
            checkForComodification();
            try {
                ArrayList.this.remove(lastRet);
                //因为 lastRet 位置原始的元素被移除了，所以此时 lastRet 指向的元素是原先 lastRet+1 位置的元素
                cursor = lastRet;
                lastRet = -1;
                //因为是 Itr 主动对集合进行修改，所以此处需要主动更新 expectedModCount 值，避免之后抛出异常
                expectedModCount = modCount;
            } catch (IndexOutOfBoundsException ex) {
                throw new ConcurrentModificationException();
            }
        }

        //遍历集合从索引 cursor 开始之后剩下的元素
        @Override
        @SuppressWarnings("unchecked")
        public void forEachRemaining(Consumer<? super E> consumer) {
            Objects.requireNonNull(consumer);
            final int size = ArrayList.this.size;
            int i = cursor;
            if (i >= size) {
                return;
            }
            final Object[] elementData = ArrayList.this.elementData;
            if (i >= elementData.length) {
                throw new ConcurrentModificationException();
            }
            //遍历调用 accept 方法
            while (i != size && modCount == expectedModCount) {
                consumer.accept((E) elementData[i++]);
            }
            cursor = i;
            lastRet = i - 1;
            checkForComodification();
        }

        //判断迭代器在遍历集合的过程中，集合是否被外部改动了（例如被其它迭代器移除了元素）
        //如果是的话则抛出异常
        final void checkForComodification() {
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();
        }
    }

    private class ListItr extends Itr implements ListIterator<E> {

        ListItr(int index) {
            super();
            cursor = index;
        }

        public boolean hasPrevious() {
            return cursor != 0;
        }

        public int nextIndex() {
            return cursor;
        }

        public int previousIndex() {
            return cursor - 1;
        }

        @SuppressWarnings("unchecked")
        public E previous() {
            checkForComodification();
            int i = cursor - 1;
            if (i < 0)
                throw new NoSuchElementException();
            Object[] elementData = ArrayList.this.elementData;
            if (i >= elementData.length)
                throw new ConcurrentModificationException();
            cursor = i;
            return (E) elementData[lastRet = i];
        }

        public void set(E e) {
            if (lastRet < 0)
                throw new IllegalStateException();
            checkForComodification();
            try {
                ArrayList.this.set(lastRet, e);
            } catch (IndexOutOfBoundsException ex) {
                throw new ConcurrentModificationException();
            }
        }

        public void add(E e) {
            checkForComodification();
            try {
                int i = cursor;
                ArrayList.this.add(i, e);
                cursor = i + 1;
                lastRet = -1;
                expectedModCount = modCount;
            } catch (IndexOutOfBoundsException ex) {
                throw new ConcurrentModificationException();
            }
        }
    }

    //获取由参数值指定范围的子集合
    //其实是对集合其指定范围内的元素进行包装，把对子集合的操作映射到父集合上
    public List<E> subList(int fromIndex, int toIndex) {
        subListRangeCheck(fromIndex, toIndex, size);
        return new SubList(this, 0, fromIndex, toIndex);
    }

    static void subListRangeCheck(int fromIndex, int toIndex, int size) {
        if (fromIndex < 0)
            throw new IndexOutOfBoundsException("fromIndex = " + fromIndex);
        if (toIndex > size)
            throw new IndexOutOfBoundsException("toIndex = " + toIndex);
        if (fromIndex > toIndex)
            throw new IllegalArgumentException("fromIndex(" + fromIndex +
                                               ") > toIndex(" + toIndex + ")");
    }

    private class SubList extends AbstractList<E> implements RandomAccess {

        //父集合对象
        private final AbstractList<E> parent;

        //子集合起始索引值相对父集合的偏移量
        //在对子集合进行添加元素和移除元素等操作时，都需要加上此偏移量
        private final int parentOffset;

        //额外的偏移量
        private final int offset;

        //子集合的元素个数
        int size;

        SubList(AbstractList<E> parent,
                int offset, int fromIndex, int toIndex) {
            this.parent = parent;
            this.parentOffset = fromIndex;
            this.offset = offset + fromIndex;
            this.size = toIndex - fromIndex;
            this.modCount = ArrayList.this.modCount;
        }

        //index 值是相对于子集合的索引，需要将之映射到父集合对象
        //SubList 的方法基本都与 ArrayList 的类似，后边不再赘述
        public E set(int index, E e) {
            rangeCheck(index);
            checkForComodification();
            E oldValue = ArrayList.this.elementData(offset + index);
            ArrayList.this.elementData[offset + index] = e;
            return oldValue;
        }

        public E get(int index) {
            rangeCheck(index);
            checkForComodification();
            return ArrayList.this.elementData(offset + index);
        }

        public int size() {
            checkForComodification();
            return this.size;
        }

        public void add(int index, E e) {
            rangeCheckForAdd(index);
            checkForComodification();
            parent.add(parentOffset + index, e);
            this.modCount = parent.modCount;
            this.size++;
        }

        public E remove(int index) {
            rangeCheck(index);
            checkForComodification();
            E result = parent.remove(parentOffset + index);
            this.modCount = parent.modCount;
            this.size--;
            return result;
        }

        protected void removeRange(int fromIndex, int toIndex) {
            checkForComodification();
            parent.removeRange(parentOffset + fromIndex,
                               parentOffset + toIndex);
            this.modCount = parent.modCount;
            this.size -= toIndex - fromIndex;
        }

        public boolean addAll(Collection<? extends E> c) {
            return addAll(this.size, c);
        }

        public boolean addAll(int index, Collection<? extends E> c) {
            rangeCheckForAdd(index);
            int cSize = c.size();
            if (cSize==0)
                return false;

            checkForComodification();
            parent.addAll(parentOffset + index, c);
            this.modCount = parent.modCount;
            this.size += cSize;
            return true;
        }

        public Iterator<E> iterator() {
            return listIterator();
        }

        public ListIterator<E> listIterator(final int index) {
            checkForComodification();
            rangeCheckForAdd(index);
            final int offset = this.offset;

            return new ListIterator<E>() {
                int cursor = index;
                int lastRet = -1;
                int expectedModCount = ArrayList.this.modCount;

                public boolean hasNext() {
                    return cursor != SubList.this.size;
                }

                @SuppressWarnings("unchecked")
                public E next() {
                    checkForComodification();
                    int i = cursor;
                    if (i >= SubList.this.size)
                        throw new NoSuchElementException();
                    Object[] elementData = ArrayList.this.elementData;
                    if (offset + i >= elementData.length)
                        throw new ConcurrentModificationException();
                    cursor = i + 1;
                    return (E) elementData[offset + (lastRet = i)];
                }

                public boolean hasPrevious() {
                    return cursor != 0;
                }

                @SuppressWarnings("unchecked")
                public E previous() {
                    checkForComodification();
                    int i = cursor - 1;
                    if (i < 0)
                        throw new NoSuchElementException();
                    Object[] elementData = ArrayList.this.elementData;
                    if (offset + i >= elementData.length)
                        throw new ConcurrentModificationException();
                    cursor = i;
                    return (E) elementData[offset + (lastRet = i)];
                }

                @SuppressWarnings("unchecked")
                public void forEachRemaining(Consumer<? super E> consumer) {
                    Objects.requireNonNull(consumer);
                    final int size = SubList.this.size;
                    int i = cursor;
                    if (i >= size) {
                        return;
                    }
                    final Object[] elementData = ArrayList.this.elementData;
                    if (offset + i >= elementData.length) {
                        throw new ConcurrentModificationException();
                    }
                    while (i != size && modCount == expectedModCount) {
                        consumer.accept((E) elementData[offset + (i++)]);
                    }
                    // update once at end of iteration to reduce heap write traffic
                    lastRet = cursor = i;
                    checkForComodification();
                }

                public int nextIndex() {
                    return cursor;
                }

                public int previousIndex() {
                    return cursor - 1;
                }

                public void remove() {
                    if (lastRet < 0)
                        throw new IllegalStateException();
                    checkForComodification();

                    try {
                        SubList.this.remove(lastRet);
                        cursor = lastRet;
                        lastRet = -1;
                        expectedModCount = ArrayList.this.modCount;
                    } catch (IndexOutOfBoundsException ex) {
                        throw new ConcurrentModificationException();
                    }
                }

                public void set(E e) {
                    if (lastRet < 0)
                        throw new IllegalStateException();
                    checkForComodification();

                    try {
                        ArrayList.this.set(offset + lastRet, e);
                    } catch (IndexOutOfBoundsException ex) {
                        throw new ConcurrentModificationException();
                    }
                }

                public void add(E e) {
                    checkForComodification();

                    try {
                        int i = cursor;
                        SubList.this.add(i, e);
                        cursor = i + 1;
                        lastRet = -1;
                        expectedModCount = ArrayList.this.modCount;
                    } catch (IndexOutOfBoundsException ex) {
                        throw new ConcurrentModificationException();
                    }
                }

                final void checkForComodification() {
                    if (expectedModCount != ArrayList.this.modCount)
                        throw new ConcurrentModificationException();
                }
            };
        }

        public List<E> subList(int fromIndex, int toIndex) {
            subListRangeCheck(fromIndex, toIndex, size);
            return new SubList(this, offset, fromIndex, toIndex);
        }

        private void rangeCheck(int index) {
            if (index < 0 || index >= this.size)
                throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
        }

        private void rangeCheckForAdd(int index) {
            if (index < 0 || index > this.size)
                throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
        }

        private String outOfBoundsMsg(int index) {
            return "Index: "+index+", Size: "+this.size;
        }

        private void checkForComodification() {
            if (ArrayList.this.modCount != this.modCount)
                throw new ConcurrentModificationException();
        }

        public Spliterator<E> spliterator() {
            checkForComodification();
            return new ArrayListSpliterator<E>(ArrayList.this, offset,
                                               offset + this.size, this.modCount);
        }
    }

    //遍历集合元素
    @Override
    public void forEach(Consumer<? super E> action) {
        Objects.requireNonNull(action);
        final int expectedModCount = modCount;
        @SuppressWarnings("unchecked")
        final E[] elementData = (E[]) this.elementData;
        final int size = this.size;
        //如果 modCount 值被改动，则直接停止遍历并抛出异常
        for (int i=0; modCount == expectedModCount && i < size; i++) {
            //将集合元素依次传递给 accept 方法
            action.accept(elementData[i]);
        }
        if (modCount != expectedModCount) {
            throw new ConcurrentModificationException();
        }
    }

    /**
     * Creates a <em><a href="Spliterator.html#binding">late-binding</a></em>
     * and <em>fail-fast</em> {@link Spliterator} over the elements in this
     * list.
     *
     * <p>The {@code Spliterator} reports {@link Spliterator#SIZED},
     * {@link Spliterator#SUBSIZED}, and {@link Spliterator#ORDERED}.
     * Overriding implementations should document the reporting of additional
     * characteristic values.
     *
     * @return a {@code Spliterator} over the elements in this list
     * @since 1.8
     */
    @Override
    public Spliterator<E> spliterator() {
        return new ArrayListSpliterator<>(this, 0, -1, 0);
    }

    /** Index-based split-by-two, lazily initialized Spliterator */
    static final class ArrayListSpliterator<E> implements Spliterator<E> {

        private final ArrayList<E> list;

        private int index; // current index, modified on advance/split

        private int fence; // -1 until used; then one past last index

        private int expectedModCount; // initialized when fence set

        /** Create new spliterator covering the given  range */
        ArrayListSpliterator(ArrayList<E> list, int origin, int fence,
                             int expectedModCount) {
            this.list = list; // OK if null unless traversed
            this.index = origin;
            this.fence = fence;
            this.expectedModCount = expectedModCount;
        }

        private int getFence() { // initialize fence to size on first use
            int hi; // (a specialized variant appears in method forEach)
            ArrayList<E> lst;
            if ((hi = fence) < 0) {
                if ((lst = list) == null)
                    hi = fence = 0;
                else {
                    expectedModCount = lst.modCount;
                    hi = fence = lst.size;
                }
            }
            return hi;
        }

        public ArrayListSpliterator<E> trySplit() {
            int hi = getFence(), lo = index, mid = (lo + hi) >>> 1;
            return (lo >= mid) ? null : // divide range in half unless too small
                new ArrayListSpliterator<E>(list, lo, index = mid,
                                            expectedModCount);
        }

        public boolean tryAdvance(Consumer<? super E> action) {
            if (action == null)
                throw new NullPointerException();
            int hi = getFence(), i = index;
            if (i < hi) {
                index = i + 1;
                @SuppressWarnings("unchecked") E e = (E)list.elementData[i];
                action.accept(e);
                if (list.modCount != expectedModCount)
                    throw new ConcurrentModificationException();
                return true;
            }
            return false;
        }

        public void forEachRemaining(Consumer<? super E> action) {
            int i, hi, mc; // hoist accesses and checks from loop
            ArrayList<E> lst; Object[] a;
            if (action == null)
                throw new NullPointerException();
            if ((lst = list) != null && (a = lst.elementData) != null) {
                if ((hi = fence) < 0) {
                    mc = lst.modCount;
                    hi = lst.size;
                }
                else
                    mc = expectedModCount;
                if ((i = index) >= 0 && (index = hi) <= a.length) {
                    for (; i < hi; ++i) {
                        @SuppressWarnings("unchecked") E e = (E) a[i];
                        action.accept(e);
                    }
                    if (lst.modCount == mc)
                        return;
                }
            }
            throw new ConcurrentModificationException();
        }

        public long estimateSize() {
            return (long) (getFence() - index);
        }

        public int characteristics() {
            return Spliterator.ORDERED | Spliterator.SIZED | Spliterator.SUBSIZED;
        }
    }

    //按照给定规则对集合元素进行过滤，如果元素符合过滤规则 filter 则将之移除
    @Override
    public boolean removeIf(Predicate<? super E> filter) {
        Objects.requireNonNull(filter);
        //要移除的元素个数
        int removeCount = 0;
        //用于标记集合是哪个索引位置需要被移除
        final BitSet removeSet = new BitSet(size);
        final int expectedModCount = modCount;
        final int size = this.size;
        for (int i=0; modCount == expectedModCount && i < size; i++) {
            @SuppressWarnings("unchecked")
            final E element = (E) elementData[i];
            //依次判断集合元素是否符合过滤规则
            if (filter.test(element)) {
                //set 方法将导致索引位置 i 的元素变为 true
                removeSet.set(i);
                removeCount++;
            }
        }
        if (modCount != expectedModCount) {
            throw new ConcurrentModificationException();
        }
        //只有 removeCount > 0 才说明需要移除元素
        final boolean anyToRemove = removeCount > 0;
        if (anyToRemove) {
            //集合移除指定元素后的大小
            final int newSize = size - removeCount;
            for (int i=0, j=0; (i < size) && (j < newSize); i++, j++) {
                //略过被标记为 true 的位置，直接跳到不需要移除元素的数组索引位
                i = removeSet.nextClearBit(i);
                //有效数据逐渐从尾部向头部聚集
                elementData[j] = elementData[i];
            }
            //移除尾部的无效数据，帮助GC回收
            for (int k=newSize; k < size; k++) {
                elementData[k] = null;
            }
            this.size = newSize;
            if (modCount != expectedModCount) {
                throw new ConcurrentModificationException();
            }
            modCount++;
        }

        return anyToRemove;
    }

    //将集合元素遍历传递给 operator，并将原始数据替换为 operator 的返回值
    @Override
    @SuppressWarnings("unchecked")
    public void replaceAll(UnaryOperator<E> operator) {
        Objects.requireNonNull(operator);
        final int expectedModCount = modCount;
        final int size = this.size;
        for (int i=0; modCount == expectedModCount && i < size; i++) {
            //依次传递数组元素给 apply 方法，并将其返回值替换原始数据
            elementData[i] = operator.apply((E) elementData[i]);
        }
        //不允许在排序的过程中集合被其它方法修改了数据结构（例如：移除元素）
        if (modCount != expectedModCount) {
            throw new ConcurrentModificationException();
        }
        modCount++;
    }

    //将集合按照指定规则进行排序
    @Override
    @SuppressWarnings("unchecked")
    public void sort(Comparator<? super E> c) {
        final int expectedModCount = modCount;
        Arrays.sort((E[]) elementData, 0, size, c);
        //不允许在排序的过程中集合被其它方法修改了数据结构（例如：移除元素）
        if (modCount != expectedModCount) {
            throw new ConcurrentModificationException();
        }
        modCount++;
    }

}

```