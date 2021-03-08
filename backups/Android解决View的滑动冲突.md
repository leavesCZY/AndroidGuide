当父容器与子 View 都可以滑动时，就会产生滑动冲突。例如 ViewPager 中包含了 ListView 时，ViewPager 可以横向滑动，而 ListView 可以竖向滑动，此时就会产生滑动冲突。而我们之所以在使用的过程中没有发现这个问题，是因为 ViewPager 内部已经处理好滑动冲突了

解决 View 之间的滑动冲突的方法分为两种，分别是外部拦截法和内部拦截法

### 一、外部拦截法

父容器根据需要在 `onInterceptTouchEvent` 方法中对触摸事件进行选择性拦截，思路可以看以下伪代码

```java
	public boolean onInterceptTouchEvent(MotionEvent event) {
        boolean intercepted = false;
        int x = (int) event.getX();
        int y = (int) event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                intercepted = false;
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                if (满足父容器的拦截要求) {
                    intercepted = true;
                } else {
                    intercepted = false;
                }
                break;
            }
            case MotionEvent.ACTION_UP: {
                intercepted = false;
                break;
            }
            default:
                break;
        }
        mLastXIntercept = x;
        mLastYIntercept = y;
        return intercepted;
    }
```

- 根据实际的业务需求，判断是否需要处理 ACTION_MOVE 事件，如果父 View 需要处理则返回 true，否则返回 false 并交由子 View 去处理
- ACTION_DOWN 事件需要返回 false，父容器不能进行拦截，否则根据 View 的事件分发机制，后续的 ACTION_MOVE 与 ACTION_UP 事件都将默认交由父容器进行处理
- 原则上 ACTION_UP 事件也需要返回 false，如果返回 true，那么子 View 将接收不到 ACTION_UP 事件，子 View 的onClick 事件也无法触发

### 二、内部拦截法

内部拦截法则是要求父容器不拦截任何事件，所有事件都传递给子 View，子 View 根据需求判断是自己消费事件还是传回给父容器进行处理，思路可以看以下伪代码：

子 View 修改其 `dispatchTouchEvent` 方法

```java
	public boolean dispatchTouchEvent(MotionEvent event) {
        int x = (int) event.getX();
        int y = (int) event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                parent.requestDisallowInterceptTouchEvent(true);
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                int deltaX = x - mLastX;
                int deltaY = y - mLastY;
                if (父容器需要此类点击事件) {
                    parent.requestDisallowInterceptTouchEvent(false);
                }
                break;
            }
            case MotionEvent.ACTION_UP: {
                break;
            }
            default:
                break;
        }
        mLastX = x;
        mLastY = y;
        return super.dispatchTouchEvent(event);
    }
```

父容器修改其 `onInterceptTouchEvent` 方法

```java
	public boolean onInterceptTouchEvent(MotionEvent event) {
        int action = event.getAction();
        if (action == MotionEvent.ACTION_DOWN) {
            return false;
        } else {
            return true;
        }
    }
```

- 内部拦截法要求父容器不能拦截 ACTION_DOWN 事件，否则一旦父容器拦截 ACTION_DOWN 事件，那么后续的触摸事件都不会传递给子View
- 滑动策略的逻辑放在子 View 的 `dispatchTouchEvent` 方法的 ACTION_MOVE 事件中，如果父容器需要处理事件则调用 `parent.requestDisallowInterceptTouchEvent(false)` 方法让父容器去拦截事件

### 三、滑动冲突实例

这里以 ViewPager 作为父容器，看看 ViewPager 与其内部 View 之间的滑动冲突情况

为了使 ViewPager 不处理滑动冲突，这里来重写其 `onInterceptTouchEvent()` 方法

```java
/**
 * 作者：leavesc
 * 时间：2018/7/15 10:26
 * 描述：
 */
public class MyViewPager extends ViewPager {

    public MyViewPager(@NonNull Context context) {
        super(context);
    }

    public MyViewPager(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return false;
    }

}
```

这里用一个布尔变量来控制 ViewPager 中每一个页面包含的是 ListView 还是 TextView

```java
public class MainActivity extends AppCompatActivity {

    private List<View> viewList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ViewPager viewPager = findViewById(R.id.viewPager);
        viewList = new ArrayList<>();
        initData(false);
        viewPager.setAdapter(new MyPagerAdapter(viewList));
    }

    private void initData(boolean flag) {
        for (int j = 0; j < 4; j++) {
            View view;
            if (flag) {
                ListView listView = new ListView(this);
                List<String> dataList = new ArrayList<>();
                for (int i = 0; i < 30; i++) {
                    dataList.add("leavesC " + i);
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, dataList);
                listView.setAdapter(adapter);
                view = listView;
            } else {
                TextView textView = new TextView(this);
                textView.setGravity(Gravity.CENTER);
                textView.setText("leavesC " + j);
                view = textView;
            }
            viewList.add(view);
        }
    }

}
```

当子 View 为 TextView 时
![](https://upload-images.jianshu.io/upload_images/2552605-58f98e519deee3a9.gif?imageMogr2/auto-orient/strip)

然而此时还是没有发生滑动冲突，ViewPager 还是可以正常使用。这是因为 TextView 默认是不可点击的，因此 TextView 并不会消费触摸事件，触摸事件最后还是传回给 ViewPager 进行处理，因为此时还是可以正常使用

如果为 TextView 设置  `textView.setClickable(true);`，就会使得 ViewPager 无法滑动
![](https://upload-images.jianshu.io/upload_images/2552605-c1ebb7dd9129207b.gif?imageMogr2/auto-orient/strip)

当子 View 为 ListView 时，则只能上下滑动，而无法左右滑动

![](https://upload-images.jianshu.io/upload_images/2552605-2b2081a2dc89fa59.gif?imageMogr2/auto-orient/strip)

### 四、通过外部拦截法解决滑动冲突

外部拦截法仅需要修改父容器的 `onInterceptTouchEvent()` 方法即可，通过滑动时横向滑动距离与竖向滑动距离之间的大小，判断是否在进行左右滑动，如果判断出当前是滑动操作，则使 ViewPager 消费该事件

```java
/**
 * 作者：leavesc
 * 时间：2018/7/15 10:26
 * 描述：
 */
public class MyViewPager extends ViewPager {

    public MyViewPager(@NonNull Context context) {
        super(context);
    }

    public MyViewPager(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    private int lastXIntercept;

    private int lastYIntercept;

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        boolean intercepted = false;
        int x = (int) ev.getX();
        int y = (int) ev.getY();
        final int action = ev.getAction() & MotionEvent.ACTION_MASK;
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                //不拦截此事件
                intercepted = false;
                //调用 ViewPager的 onInterceptTouchEvent 方法用于初始化 mActivePointerId
                super.onInterceptTouchEvent(ev);
                break;
            case MotionEvent.ACTION_MOVE:
                //横向位移增量
                int deltaX = x - lastXIntercept;
                //竖向位移增量
                int deltaY = y - lastYIntercept;
                //如果横向滑动距离大于竖向滑动距离，则认为使用者是想要左右滑动
                //此时就使 ViewPager 拦截此事件
                intercepted = Math.abs(deltaX) > Math.abs(deltaY);
                break;
            case MotionEvent.ACTION_UP:
                //不拦截此事件
                intercepted = false;
                break;
            default:
                break;
        }
        lastXIntercept = x;
        lastYIntercept = y;
        return intercepted;
    }

}
```

### 五、通过内部拦截法解决滑动冲突

内部拦截法需要重写 ListView 的 `dispatchTouchEvent` 方法

```java
/**
 * 作者：leavesc
 * 时间：2018/7/15 12:40
 * 描述：
 */
public class MyListView extends ListView {

    public MyListView(Context context) {
        super(context);
    }

    public MyListView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    private int lastX;

    private int lastY;

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        int x = (int) ev.getX();
        int y = (int) ev.getY();
        final int action = ev.getAction() & MotionEvent.ACTION_MASK;
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                getParent().requestDisallowInterceptTouchEvent(true);
                break;
            case MotionEvent.ACTION_MOVE:
                //横向位移增量
                int deltaX = x - lastX;
                //竖向位移增量
                int deltaY = y - lastY;
                //如果横向滑动距离大于竖向滑动距离，则认为使用者是想要左右滑动
                //此时就通知父容器 ViewPager 处理此事件
                if (Math.abs(deltaX) > Math.abs(deltaY)) {
                    getParent().requestDisallowInterceptTouchEvent(false);
                }
                break;
            case MotionEvent.ACTION_UP:
                break;
            default:
                break;
        }
        lastX = x;
        lastY = y;
        return super.dispatchTouchEvent(ev);
    }

}
```

同时也需要修改 MyViewPager 的 `onInterceptTouchEvent` 方法

```java
/**
 * 作者：leavesc
 * 时间：2018/7/15 10:26
 * 描述：
 */
public class MyViewPager extends ViewPager {

    public MyViewPager(@NonNull Context context) {
        super(context);
    }

    public MyViewPager(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        final int action = ev.getAction() & MotionEvent.ACTION_MASK;
        if (action == MotionEvent.ACTION_DOWN) {
            super.onInterceptTouchEvent(ev);
            return false;
        }
        return true;
    }

}
```