> 公众号：[字节数组](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/adbc507fc3704fd8955aae739a433db2~tplv-k3u1fbpfcp-zoom-1.image)
>
> 希望对你有所帮助 🤣🤣

> 本系列文章会陆续对 Java 和 Android 的集合框架（JDK 1.8，Android SDK 30）中的几个常见容器结合源码进行介绍，了解不同容器在**数据结构、适用场景、优势点**上的不同，希望对你有所帮助 🤣🤣

# 一、数组和链表

很多集合框架在底层结构都使用到了**数组**和**链表**这两种数据结构，它们在**数据存储方式**和**优劣点**这两方面有着很大区别，这里先来介绍下这两者的结构和区别

## 1、数组

假设现在有六个元素存放在数组中，则数组在内存中的存储结构就如下所示

![](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/d35e498da26c4619acab1229db882021~tplv-k3u1fbpfcp-zoom-1.image)

1. 数组是一块连续的内存空间，元素按照坐标索引依次排列，可以直接通过坐标定位到每一个数据的内存地址，例如可以直接通过坐标 3 获取到 element4，省去了从头到尾的遍历操作，因此随机读取数据的效率较高
2. 相对应的，由于数组要求元素是连续存储的，因此在添加和移除数据时有可能需要移动大量数据，所以在添加和移除数据时效率较低
3. 数组在使用前需要先指定其空间大小，在声明空间大小后无法再次修改。如果我们在使用前已知待存入的数据量的话，自然可以将数组初始化为目标容量，这样就不会浪费内存空间了。但实际上数据量往往是未知的，经常会因为申请了较大的内存空间导致浪费，或者是申请少了导致需要后续扩容，而数组在扩容时只能创建一个新的数组并将数据整体迁移，这就影响到了数组的运行性能

ArrayList 底层就是用数组来存储数据

## 2、链表

假设现在有四个元素依靠链表来存放，链表在内存中的存储结构就如下所示

![](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/073ecd6842734463804b9fc6dcbd151d~tplv-k3u1fbpfcp-zoom-1.image)

1. 图中所展示的是一个双向链表，即每个结点除了包含实际的数据外，还存在两个引用分别指向上一个结点（prev）和下一个结点（next），各个结点通过这种双向链接从而串联在一起。此外还存在两个引用分别指向头结点（first）和尾结点（last），方便进行正向遍历和反向遍历
2. 链表不要求有连续的内存空间，新添加的结点可以在内存中的任何位置，只要上一个结点和下一个结点互相保存有对方的引用即可，这也导致在随机访问数据时只能遍历整个链表，在最坏的情况下甚至需要全量遍历。当然，可以根据实际情况来选择是正向遍历还是反向遍历，以此提高访问效率，但总的来说链表在随机访问数据时效率要比数组低
3. 在添加或移除元素时，只需要修改相邻结点对指定结点的引用即可，而不需像数组那样需要移动元素，因此链表在添加和移除元素时效率较高
4. 链表不需事先申请内存空间，根据实际使用情况进行动态申请即可
5. 此外还存在一种单向链表的结构，即每个结点包含对下一个结点的引用 next，但不包含 prev，所以单向链表只能从头到尾进行遍历

LinkedList 底层就是用链表来存储数据

# 二、ArrayList 

ArrayList 应该是大多数开发者使用得最为频繁的集合容器了，ArrayList 实现了 List 接口，是一个有序容器，即元素的存放顺序与添加顺序保持一致，允许添加相同元素，包括 null 。ArrayList 底层通过数组来进行数据存储，当向 ArrayList 中添加元素时如果发现数组空间不足，ArrayList 会自动对底层数组进行扩容并迁移现有数据

## 1、类声明

从 ArrayList 实现的接口可以看出来它是支持快速访问，可克隆，可序列化的

```java
public class ArrayList<E> extends AbstractList<E> 
    implements List<E>, RandomAccess, Cloneable, java.io.Serializable
```

## 2、成员变量

ArrayList 一共包含以下几个成员变量，主要看 elementData。elementData 是用于存放数据的底层数组，由于其数据类型声明为 Object，所以可以用来存放任何类型的数据。而 ArrayList 属于泛型类，如果我们在初始化时就指定了数据类型的话，依靠 Java 泛型为我们提供的语法糖，我们在向 elementData 存取数据时编译器就会自动进行类型校验和类型转换，确保存入和取出的数据类型是安全的

```java
//序列化ID
private static final long serialVersionUID = 8683452581122892189L;

//进行扩容操作后的最小容量
private static final int DEFAULT_CAPACITY = 10;

//如果外部为集合设置的初始化大小为 0，则将 elementData 指向此空数组
private static final Object[] EMPTY_ELEMENTDATA = {};

//如果在初始化集合时使用的是无参构造函数，则将 elementData 指向此空数组
private static final Object[] DEFAULTCAPACITY_EMPTY_ELEMENTDATA = {};

//用来存放元素的数组
transient Object[] elementData;

//集合大小
private int size;

//ArrayList 的快照版本号
protected transient int modCount = 0;
```

## 3、构造函数

如果已经知道目标数据量大小的话，在初始化 ArrayList 的时候我们可以直接传入最终的容量值，这样效率会更高一些。因为如果 initialCapacity 过大，则会造成内存浪费；如果 initialCapacity 过小，可能会导致后续需要多次扩容，每次扩容都需要复制原有数据到新数组，这会降低运行效率

如果我们使用的是无参构造函数或者是指定的 initialCapacity 为 0，此时也只会将 elementData 指向空数组，并不会新建一个数组变量

```java
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

//外部没有指定初始容量，暂且使用空数组
public ArrayList() {
    this.elementData = DEFAULTCAPACITY_EMPTY_ELEMENTDATA;
}

//传入一份初始数据来进行初始化
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
```

## 4、获取元素

在获取指定索引处的元素时，ArrayList 都是直接通过坐标值来获取元素，无需从头遍历，所以说 ArrayList 遍历和随机访问的效率较高

```java
@SuppressWarnings("unchecked")
E elementData(int index) {
    return (E) elementData[index];
}

public E get(int index) {
    //判断取值范围是否合法
    rangeCheck(index);
    return elementData(index);
}
```

## 5、添加元素

ArrayList 添加元素的操作就不是那么理想了。如果是直接向集合尾端添加数据，那么直接定位到该位置进行赋值即可；如果是向集合的中间位置 index 插入数据，则需要将数组中索引 index 后的所有数据向后推移一位，然后将数据插入到空出的位置上。此外，在插入数据前 elementData 可能已经空间不足了，那么还需要先进行扩容操作。扩容操作会创建一个新的符合大小的数组，并将原数组中的数据迁移到新数组中，然后让 elementData 指向新数组

由此可以看出来，向集合添加数据和进行扩容都可能会导致数组元素大量移动，所以说 ArrayList 存入数据的效率并不高

```java
public boolean add(E e) {
    //在需要的时候进行扩容
    ensureCapacityInternal(size + 1);
    elementData[size++] = e;
    return true;
}

public void add(int index, E element) {
    rangeCheckForAdd(index);
    //在需要的时候进行扩容
    ensureCapacityInternal(size + 1);
    //将索引 index 后的所有数值向后推移一位 
    System.arraycopy(elementData, index, elementData, index + 1,size - index);
    elementData[index] = element;
    size++;
}
```

以上说的是存入单个数据的情况，此外还有存入整个集合的情况

```java
//如果待添加的数据不为空则返回 true，否则返回 false
public boolean addAll(Collection<? extends E> c) {
    Object[] a = c.toArray();
    int numNew = a.length;
    ensureCapacityInternal(size + numNew);
    //将数组 a 复制到 elementData 的尾端
    System.arraycopy(a, 0, elementData, size, numNew);
    size += numNew;
    return numNew != 0;
}

//从指定索引处添加数据，如果待添加的数据不为空则返回 true，否则返回 false
public boolean addAll(int index, Collection<? extends E> c) {
    rangeCheckForAdd(index);
    Object[] a = c.toArray();
    int numNew = a.length;
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
```

## 6、移除元素

因为数组是一种内存地址连续的数据结构，所以移除某个元素同样可能导致大量元素移动

```java
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
```

## 7、扩容机制

再来看下数组的扩容机制的具体实现逻辑

`ensureCapacity` 方法的入参参数 minCapacity 就用于指定希望扩容后的最小空间，但 minCapacity 最终不会小于 DEFAULT_CAPACITY，即扩容后的数组容量不会小于 10。之所以要进行最小容量的限制，是为了减少多次扩容的可能性，10 以内的数组很容易就发生扩容

如果在初始化 ArrayList 前已知目标数据的数据量，最好就使用`ArrayList(int initialCapacity)`来进行初始化，直接让底层数组扩充到目标大小，或者是在添加数据前就调用 `ensureCapacity` 方法直接让数组扩容到目标大小，避免之后赋值过程中多次扩容

```java
public void ensureCapacity(int minCapacity) {
    int minExpand = (elementData != DEFAULTCAPACITY_EMPTY_ELEMENTDATA)
        ? 0
        : DEFAULT_CAPACITY;
    if (minCapacity > minExpand) {
        ensureExplicitCapacity(minCapacity);
    }
}

private void ensureCapacityInternal(int minCapacity) {
    if (elementData == DEFAULTCAPACITY_EMPTY_ELEMENTDATA) {
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
```

实际上完成扩容操作的是 `grow(int minCapacity)` 方法。在扩容前，会先判断如果将容量提升到当前的 1.5 倍是否能达到 minCapacity 的要求 ，如果符合要求则直接将容量扩充到当前的 1.5 倍，否则扩充到 minCapacity，但最终容量不能大于 Integer.MAX_VALUE

构建出一个新的符合大小的数组后，就将原数组中的元素复制到新数组中，至此就完成了扩容

```java
//数组可扩容到的最大容量
private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

private void grow(int minCapacity) {
    int oldCapacity = elementData.length;
    //假设扩容后的空间大小是原先的1.5倍
    int newCapacity = oldCapacity + (oldCapacity >> 1);
    if (newCapacity - minCapacity < 0)
        newCapacity = minCapacity;
    if (newCapacity - MAX_ARRAY_SIZE > 0)
        newCapacity = hugeCapacity(minCapacity);
    elementData = Arrays.copyOf(elementData, newCapacity);
}
```

## 8、修改元素

```java
//将索引 index 出的元素值置为 element，并返回原始数值
public E set(int index, E element) {
    rangeCheck(index);
    E oldValue = elementData(index);
    elementData[index] = element;
    return oldValue;
}
```

## 9、遍历数组

遍历数组的方法包含以下几个，逻辑都比较简单，直接看注释即可。一个比较重要的知识点是看方法内部对 modCount 的校验

```java
@Override
public void forEach(Consumer<? super E> action) {
    Objects.requireNonNull(action);
    final int expectedModCount = modCount;
    @SuppressWarnings("unchecked")
    final E[] elementData = (E[]) this.elementData;
    final int size = this.size;
    for (int i=0; modCount == expectedModCount && i < size; i++) {
        //将集合元素依次传递给 accept 方法
        action.accept(elementData[i]);
    }
    //如果 modCount 值被改动了，说明遍历过程中数组有被改动到
    //那么就停止遍历并抛出异常
    if (modCount != expectedModCount) {
        throw new ConcurrentModificationException();
    }
}

//按照给定规则对集合元素进行过滤，如果元素符合过滤规则那就将之移除
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
    //不允许在排序的过程中集合被其它方法修改了数组（例如：移除元素）
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
        //移除尾部的无效数据，有利于GC回收
        for (int k=newSize; k < size; k++) {
            elementData[k] = null;
        }
        this.size = newSize;
        //不允许在排序的过程中集合被其它方法修改了数组（例如：移除元素）
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
    //不允许在排序的过程中集合被其它方法修改了数组（例如：移除元素）
    if (modCount != expectedModCount) {
        throw new ConcurrentModificationException();
    }
    modCount++;
}
```

## 10、迭代器

ArrayList 内部包含一个用于迭代元素的 Iterator 实现类，其用法如下所示

```java
public static void main(String[] args) {
    List<String> stringList = new ArrayList<>();
    stringList.add("https://github.com/leavesCZY");
    Iterator<String> iterator = stringList.iterator();
    if (iterator.hasNext()) {
        String next = iterator.next();
        System.out.println(next);
    }
}
```

在这里有个小细节，ArrayList 里多处使用到了 modCount 这个成员变量，modCount 相当于对 ArrayList 的一个简单“快照”，即类似于 ArrayList 的一个版本号，每当添加、移除和修改元素时，modCount 都会递增

在我们遍历 ArrayList 的过程中，如果同时进行增减元素的操作，或者是存在多线程同时增减元素，那么就会导致遍历结果变得不可靠，或者是直接就导致数组越界异常，所以 ArrayList 就通过 modCount 来标记当前的迭代行为是否处于可靠状态。如果在遍历数组元素的过程中判断到 modCount 的值前后发生了变化，就说明在遍历过程中 ArrayList 被改动了，此时就认定遍历结果不可靠，直接抛出异常。需要注意的是，modCount 做的只是一个简单校验，无法准确判断出当前的遍历操作就真的是安全的

```java
protected transient int modCount = 0;

public Iterator<E> iterator() {
    return new Itr();
}

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
        //如果索引值超出取值范围则抛出异常
        if (i >= size)
            throw new NoSuchElementException();
        Object[] elementData = ArrayList.this.elementData;
        //如果索引值超出数组的可索引范围则抛出异常
        if (i >= elementData.length)
            throw new ConcurrentModificationException();
        cursor = i + 1;
        return (E) elementData[lastRet = i];
    }

    //移除 lastRet 指向的元素
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

    //遍历从索引 cursor 开始之后剩下的元素
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
```

## 11、效率测试

最后再来测试下 ArrayList 扩容次数的高低对其运行效率的影响

对三个 ArrayList 存入相同数据量的数据，但分别为 ArrayList 指定不同的初始化大小

```java
public static void main(String[] args) {
    long startTime = System.currentTimeMillis();
    List<String> stringList = new ArrayList<>();
    for (int i = 0; i < 300000; i++) {
        stringList.add("leavesC " + i);
    }
    long endTime = System.currentTimeMillis();
    System.out.println("初始容量为0，所用时间：" + (endTime - startTime) + "毫秒");


    startTime = System.currentTimeMillis();
    List<String> stringList2 = new ArrayList<>(100000);
    for (int i = 0; i < 300000; i++) {
        stringList2.add("leavesC " + i);
    }
    endTime = System.currentTimeMillis();
    System.out.println("初始容量为100000，所用时间：" + (endTime - startTime) + "毫秒");


    startTime = System.currentTimeMillis();
    List<String> stringList3 = new ArrayList<>(300000);
    for (int i = 0; i < 300000; i++) {
        stringList3.add("leavesC " + i);
    }
    endTime = System.currentTimeMillis();
    System.out.println("初始容量为300000，所用时间：" + (endTime - startTime) + "毫秒");
}
```

三种方式下 ArrayList 之间的运行效率差距还是很大的，虽然这种测试方法并不严谨，但也可以看到在省去扩容操作后 ArrayList 的运行效率还是提升了很多的

```java
初始容量为0，所用时间：39毫秒
初始容量为100000，所用时间：32毫秒
初始容量为300000，所用时间：13毫秒
```

# 三、LinkedList

LinkedList 同时实现了 List 接口和 Deque 接口，所以既可以将 LinkedList 当做一个有序容器，也可以将之看作一个队列（Queue），同时又可以看作一个栈（Stack）。虽然 LinkedList 和 ArrayList 一样都实现了 List 接口，但其底层是通过**双向链表**来实现的，所以插入和删除元素的效率都要比 ArrayList 高，但也因此随机访问的效率要比 ArrayList 低

## 1、类声明

从 LinkedList 实现的几个接口可以看出来，LinkedList 是支持快速访问，可克隆，可序列化的，而且可以将之看成一个**支持有序访问的队列或者栈**

```java
public class LinkedList<E> extends AbstractSequentialList<E> 
    implements List<E>, Deque<E>, Cloneable, java.io.Serializable
```

LinkedList 内部通过双向链表的数据结构来实现的，每个链表结点除了存储本结点的数据元素外，还有两个指针分别用于指向其上下两个相邻结点，这个结点就是 LinkedList 中的静态类 Node

```java
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

## 2、成员变量

```java
//双向链表包含的结点总数，即数据总量
transient int size = 0;

//双向链表的头结点
transient Node<E> first;

//双向链表的尾结点
transient Node<E> last;

//序列化ID
private static final long serialVersionUID = 876323262645176354L;
```

当中的成员变量 first 和 last 分别用于指向链表的头部和尾部结点，因此 LinkedList 的数据结构图是类似于这样的

![](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/5ea71d0950c04d8a947867ad892ff6c6~tplv-k3u1fbpfcp-zoom-1.image)

## 3、构造函数

LinkedList 不需要去请求一片连续的内存空间来存储数据，而是在每次有新的元素需要添加时再来动态请求内存空间，因此 LinkedList 的两个构造函数都很简单

```java
public LinkedList() {
}

//传入初始数据
public LinkedList(Collection<? extends E> c) {
    this();
    addAll(c);
}
```

## 4、添加元素

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

![](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/83bed0a5917a494c88aa6edc2be08ab6~tplv-k3u1fbpfcp-zoom-1.image)

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

![](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/6853b99e131840ccb6c4efaa2584e399~tplv-k3u1fbpfcp-zoom-1.image)

## 5、移除元素

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

## 6、随机访问元素

对于单向链表来说，如果想随机定位到某个结点，那么只能通过从头结点开始遍历的方式来定位，最极端的情况下需要遍历整个链表才能定位到目标结点。如果是双向链表，则可以选择正向遍历或者反向遍历，最极端的情况下需要遍历一半链表才能定位到目标结点。所以，相比数组来说 LinkedList 的随机访问效率并不高

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
    //最极端的情况下遍历一半元素才能定位到目标节点
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

## 7、几个常用的方法

```java
//判断是否包含元素 o
public boolean contains(Object o) {
    return indexOf(o) != -1;
}

//获取元素个数
public int size() {
    return size;
}

//清空链表元素，将各个结点之间的引用都切断
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

## 8、Deque 接口

以上介绍的几个方法都是 List 接口中所声明的，接下来看下 Deque 接口中的方法

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

## 9、效率测试

上面说过，LinkedList 相比 ArrayList 在添加和移除元素时效率上会高很多，但随机访问元素的效率要比 ArrayList 低，这里也来做个测试，验证两者之间的差别

分别向 ArrayList 和 LinkedList 存入同等数据量的数据，然后各自移除 100 个元素以及遍历 10000 个元素，观察两者所用的时间

ArrayList：

```java
public static void main(String[] args) {
    List<String> stringArrayList = new ArrayList<>();
    for (int i = 0; i < 300000; i++) {
        stringArrayList.add("leavesC " + i);
    }

    long startTime = System.currentTimeMillis();
    for (int i = 0; i < 100; i++) {
        stringArrayList.remove(100 + i);
    }
    long endTime = System.currentTimeMillis();
    System.out.println("移除 ArrayList 中的100个元素,用时：" + (endTime - startTime) + "毫秒");

    startTime = System.currentTimeMillis();
    for (int i = 0; i < 10000; i++) {
        stringArrayList.get(i);
    }
    endTime = System.currentTimeMillis();
    System.out.println("遍历 ArrayList 中的10000个元素,用时：" + (endTime - startTime) + "毫秒");
}
```

LinkedList：

```java
public static void main(String[] args) {
    List<String> stringLinkedList = new LinkedList<>();
    for (int i = 0; i < 300000; i++) {
        stringLinkedList.add("leavesC " + i);
    }

    long startTime = System.currentTimeMillis();
    for (int i = 0; i < 100; i++) {
        stringLinkedList.remove(100 + i);
    }
    long endTime = System.currentTimeMillis();
    System.out.println("移除 LinkedList 中的100个元素,用时：" + (endTime - startTime) + "毫秒");

    startTime = System.currentTimeMillis();
    for (int i = 0; i < 10000; i++) {
        stringLinkedList.get(i);
    }
    endTime = System.currentTimeMillis();
    System.out.println("遍历 LinkedList 中的10000个元素,用时：" + (endTime - startTime) + "毫秒");
}

```

可以看出来两者之间的差距还是非常大的，在使用集合框架前需要根据实际应用场景来决定使用哪一个

```java
移除 ArrayList 中的100个元素,用时：18毫秒
遍历 ArrayList 中的10000个元素,用时：1毫秒
    
移除 LinkedList 中的100个元素,用时：0毫秒
遍历 LinkedList 中的10000个元素,用时：237毫秒
```