ConstraintLayout（约束布局）已经推出有一段时间了，在 Android Studio 中也作为了默认布局，能够减少布局的层级并改善布局性能，因此很有必要来研究下其功能与使用方法

ConstraintLayout 能够灵活地定位和调整子View的大小，子 View 依靠约束关系来确定位置。在一个约束关系中，需要有一个 Source（源）以及一个 Target（目标），Source 的位置依赖于 Target，可以理解为“通过约束关系，Source 与 Target链接在了一起，Source 相对于 Target 的位置便是固定的了

##**一、基本属性**

ConstraintLayout 最基本的属性控制有以下几个，即 layout_constraintXXX_toYYYOf 格式的属性，即将“View A”的方向 XXX 置于 “View B”的方向 YYY 。当中，View B 可以是父容器即 ConstraintLayout ，用“parent”来表示

 - layout_constraintBaseline_toBaselineOf （View A 内部文字与 View B 内部文字对齐）
 - layout_constraintLeft_toLeftOf  （View A 与 View B 左对齐）
 - layout_constraintLeft_toRightOf （View A 的左边置于 View B 的右边）
 - layout_constraintRight_toLeftOf （View A 的右边置于 View B 的左边）
 - layout_constraintRight_toRightOf 
 - layout_constraintTop_toTopOf
 - layout_constraintTop_toBottomOf
 - layout_constraintBottom_toTopOf
 - layout_constraintBottom_toBottomOf
 - layout_constraintStart_toEndOf
 - layout_constraintStart_toStartOf
 - layout_constraintEnd_toStartOf
 - layout_constraintEnd_toEndOf


各个方位的参照图可以看以下图片


![](http://upload-images.jianshu.io/upload_images/2552605-fcbaeb45150186d9.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)


ConstraintLayout 的这些属性是为控件添加了某个方向的约束力，根据某个方向约束力的“有无”或“强弱”，控件会位于不同的位置
例如，看以下代码，根据某个方向的约束的不同，控件会居中或者不会

```
<?xml version="1.0" encoding="utf-8"?>
<android.support.constraint.ConstraintLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    tools:context="com.czy.constraintlayoutdemo.MainActivity">

    <TextView
        android:id="@+id/tv1"
        android:layout_width="200dp"
        android:layout_height="150dp"
        android:layout_margin="20dp"
        android:background="#f5ec7e"
        android:gravity="center"
        android:text="Hello World!"
        app:layout_constraintLeft_toLeftOf="parent"
        app:layout_constraintRight_toLeftOf="@+id/tv2"
        app:layout_constraintTop_toTopOf="parent"/>

    <TextView
        android:id="@+id/tv2"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_marginEnd="20dp"
        android:background="#68b0f9"
        android:gravity="center"
        android:text="没有设置底部约束，所以不会居于中间"
        app:layout_constraintLeft_toRightOf="@+id/tv1"
        app:layout_constraintRight_toRightOf="parent"
        app:layout_constraintTop_toTopOf="@+id/tv1"/>

    <TextView
        android:id="@+id/tv3"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_marginEnd="20dp"
        android:background="#984ff7"
        android:gravity="center"
        android:text="上下均设置了约束，所以会居于中间"
        app:layout_constraintBottom_toBottomOf="@+id/tv1"
        app:layout_constraintLeft_toRightOf="@+id/tv1"
        app:layout_constraintRight_toRightOf="parent"
        app:layout_constraintTop_toBottomOf="@+id/tv2"/>

</android.support.constraint.ConstraintLayout>

```

![](http://upload-images.jianshu.io/upload_images/2552605-05fe2d4fe78df37e.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

可以看到，蓝色方块由于只设置了顶部约束而没有设置底部约束，所有蓝色方块只会与黄色方块顶部对齐，而紫色方块由于上下均设置了约束，且约束力的“强度”相同，所以紫色方块会呈现居中效果

##**二、约束力的强度**

那么，约束力的“强度”该如何设定呢？可以依靠 layout_constraintHorizontal_bias 和 layout_constraintVertical_bias 两个属性，即用来设置控件在水平和垂直方向的偏移量
例如，以下布局中， TextView 应该是处于水平和垂直两个方向居中的

```
<?xml version="1.0" encoding="utf-8"?>
<android.support.constraint.ConstraintLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    tools:context="com.czy.constraintlayoutdemo.MainActivity">

    <TextView
        android:id="@+id/tv1"
        android:layout_width="200dp"
        android:layout_height="200dp"
        android:background="#f5ec7e"
        android:gravity="center"
        android:text="Hello World!"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintLeft_toLeftOf="parent"
        app:layout_constraintRight_toRightOf="parent"
        app:layout_constraintTop_toTopOf="parent"/>

</android.support.constraint.ConstraintLayout>

```

![](http://upload-images.jianshu.io/upload_images/2552605-9b5e3f51a7532686.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

而在以下代码中，黄色方块左侧所占的剩余空间从50%变成了100%，上侧所占的剩余空间从50%变成了10%

```
<?xml version="1.0" encoding="utf-8"?>
<android.support.constraint.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    tools:context="com.czy.constraintlayoutdemo.MainActivity">

    <TextView
        android:id="@+id/tv1"
        android:layout_width="200dp"
        android:layout_height="200dp"
        android:background="#f5ec7e"
        android:gravity="center"
        android:text="Hello World!"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintHorizontal_bias="1"
        app:layout_constraintLeft_toLeftOf="parent"
        app:layout_constraintRight_toRightOf="parent"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintVertical_bias="0.1" />

</android.support.constraint.ConstraintLayout>
```

![](http://upload-images.jianshu.io/upload_images/2552605-3aaecad9f100d6d1.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

##**三、Visibility 属性**

在以前的布局中，如果 View 的 visibility 属性设置为 gone，那么其他原本依赖该 View 来参照定位的属性都会失效，而在 ConstraintLayout 布局中会有所不同
例如，在以下布局中，红色方块位于屏幕右上角与黄色方块左下角形成的矩形的中间位置

```
<?xml version="1.0" encoding="utf-8"?>
<android.support.constraint.ConstraintLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    tools:context="com.czy.constraintlayoutdemo.MainActivity">

    <TextView
        android:id="@+id/tv1"
        android:layout_width="100dp"
        android:layout_height="100dp"
        android:background="#f5ec7e"
        android:gravity="center"
        android:text="Hello World!"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintLeft_toLeftOf="parent"
        app:layout_constraintRight_toRightOf="parent"
        app:layout_constraintTop_toTopOf="parent"/>

    <TextView
        android:id="@+id/tv2"
        android:layout_width="100dp"
        android:layout_height="100dp"
        android:background="#fa6e61"
        android:gravity="center"
        android:text="Hello World!"
        app:layout_constraintBottom_toBottomOf="@+id/tv1"
        app:layout_constraintLeft_toLeftOf="@+id/tv1"
        app:layout_constraintRight_toRightOf="parent"
        app:layout_constraintTop_toTopOf="parent"/>

</android.support.constraint.ConstraintLayout>

```

![](http://upload-images.jianshu.io/upload_images/2552605-a2b88dba5e759cfb.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

如果将黄色方块的 visibility 属性设置为 gone，那么，红色方块的位置会发生变化。可以理解为黄色方块缩小为一个不可见的小点，位于其原先位置的中间，而红色方块则改为依照该点来进行定位

![](http://upload-images.jianshu.io/upload_images/2552605-35ff1aeb88d761d0.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

此时，红色方块可以依靠 layout_goneMarginBottom 、layout_goneMarginStart等属性来设置与黄色方块之间的边距，这类属性只有在黄色方块的 visibility 属性设置为gone时才会生效

##**四、宽高比**
在其他布局中，如果想根据屏幕的宽度来为 View 设置固定的宽高比的话是比较麻烦的，但在 ConstraintLayout 中可以直接设置
例如，如果想实现一个固定宽高比的顶部标题栏的话，可以将宽和高设置为 0dp，然后为其设置 app:layout_constraintDimensionRatio 属性，设定宽高比为16：7
```
<?xml version="1.0" encoding="utf-8"?>
<android.support.constraint.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    tools:context="com.czy.constraintlayoutdemo.MainActivity">

    <TextView
        android:id="@+id/tv1"
        android:layout_width="0dp"
        android:layout_height="0dp"
        android:background="#f5ec7e"
        android:gravity="center"
        android:text="Hello World!"
        app:layout_constraintDimensionRatio="h,16:7"
        app:layout_constraintLeft_toLeftOf="parent"
        app:layout_constraintRight_toRightOf="parent"
        app:layout_constraintTop_toTopOf="parent" />

    <TextView
        android:id="@+id/tv2"
        android:layout_width="100dp"
        android:layout_height="0dp"
        android:background="#5476fd"
        android:gravity="center"
        android:text="Hello World!"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintDimensionRatio="W,15:25"
        app:layout_constraintLeft_toLeftOf="parent"
        app:layout_constraintRight_toRightOf="parent"
        app:layout_constraintTop_toBottomOf="@+id/tv1" />

</android.support.constraint.ConstraintLayout>

```

![](http://upload-images.jianshu.io/upload_images/2552605-6b74e0e90e37cb8a.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

要使用 layout_constraintDimensionRatio 属性，需要至少设置宽度或者高度为 0dp，宽高的尺寸比例可以通过“float值”或者“宽度：高度”的形式来设置
如果宽度和高度都是 0dp ，系统会使用满足所有约束条件和宽高比率值的最大尺寸
如果要根据其中一个尺寸来约束另外一个尺寸，则可以在比率值的前面添加 W 或者 H 来指明约束宽度或者高度

##**五、设置控件之间的宽高占比**
ConstraintLayout 也可以像 LinearLayout 一样为子控件设置 layout_weight 属性，从而控件子控件之间的宽高占比
```
<?xml version="1.0" encoding="utf-8"?>
<android.support.constraint.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    tools:context="com.czy.constraintlayoutdemo.MainActivity">

    <TextView
        android:id="@+id/tv1"
        android:layout_width="0dp"
        android:layout_height="50dp"
        android:background="#f5ec7e"
        android:gravity="center"
        android:text="Hello World!"
        app:layout_constraintHorizontal_chainStyle="spread_inside"
        app:layout_constraintHorizontal_weight="1.5"
        app:layout_constraintLeft_toLeftOf="parent"
        app:layout_constraintRight_toLeftOf="@+id/tv2"
        app:layout_constraintTop_toTopOf="parent" />

    <TextView
        android:id="@+id/tv2"
        android:layout_width="0dp"
        android:layout_height="50dp"
        android:background="#55e4f4"
        android:gravity="center"
        android:text="Hello World!"
        app:layout_constraintHorizontal_chainStyle="spread_inside"
        app:layout_constraintHorizontal_weight="1"
        app:layout_constraintLeft_toRightOf="@+id/tv1"
        app:layout_constraintRight_toLeftOf="@+id/tv3"
        app:layout_constraintTop_toTopOf="parent" />

    <TextView
        android:id="@+id/tv3"
        android:layout_width="0dp"
        android:layout_height="50dp"
        android:background="#f186ad"
        android:gravity="center"
        android:text="Hello World!"
        app:layout_constraintHorizontal_chainStyle="spread_inside"
        app:layout_constraintHorizontal_weight="1"
        app:layout_constraintLeft_toRightOf="@+id/tv2"
        app:layout_constraintRight_toRightOf="parent"
        app:layout_constraintTop_toTopOf="parent" />

</android.support.constraint.ConstraintLayout>
```

![](http://upload-images.jianshu.io/upload_images/2552605-46d7c524fd926654.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

##**六、锚向指示线**
当需要一个任意位置的锚点时，可以使用指示线（Guideline）来帮助定位，指示线实际上是 View 的子类，使用方式和普通的 View 相同，但指示线有着如下的特殊属性：

 - 宽度和高度均为0
 - 可见性为 View.GONE

即指示线只是为了帮助其他 View 定位而存在，实际上并不会出现在实际界面中
例如，如下代码加入了两条指示线，可以选择使用百分比或实际距离来设置指示线的位置，此外也可以通过 orientation 属性来设置指示线的方向
```
<?xml version="1.0" encoding="utf-8"?>
<android.support.constraint.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    tools:context="com.czy.constraintlayoutdemo.MainActivity">

    <android.support.constraint.Guideline
        android:id="@+id/guideline1"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        app:layout_constraintGuide_percent="0.5" />

    <android.support.constraint.Guideline
        android:id="@+id/guideline2"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        app:layout_constraintGuide_begin="100dp" />

    <TextView
        android:id="@+id/tv1"
        android:layout_width="150dp"
        android:layout_height="150dp"
        android:background="#f5ec7e"
        android:gravity="center"
        android:text="Hello World!"
        app:layout_constraintLeft_toRightOf="@+id/guideline1"
        app:layout_constraintTop_toBottomOf="@+id/guideline2" />

</android.support.constraint.ConstraintLayout>
```
设置横向指示线距离顶部 100dp


![](http://upload-images.jianshu.io/upload_images/2552605-734ba0bba31918cd.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

竖向指示线设置其距离百分比为 0.5，所以是位于屏幕的中间位置

![](http://upload-images.jianshu.io/upload_images/2552605-833aad3676cfc1c8.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

##**七、Chains链**
Chain 链比较难描述，它是一种特殊的约束，可以为多个通过 chain 链连接的 View 来分发剩余空间位置，类似于 LinearLayout 中的权重比 weight 属性，但 Chains 链要强大得多

如果几个View之间通过双向连接而互相约束对方的位置，那么将其视为链条

例如，以下布局代码所呈现出来的效果，就可以称为一条链条，在这条链的最左侧的元素成为链头，可以在其身上设置一些属性以决定这个链的展示效果
```
<?xml version="1.0" encoding="utf-8"?>
<android.support.constraint.ConstraintLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    tools:context="com.czy.constraintlayoutdemo.MainActivity">

    <TextView
        android:id="@+id/tv1"
        android:layout_width="wrap_content"
        android:layout_height="50dp"
        android:background="#f5ec7e"
        android:gravity="center"
        android:text="Hello World!"
        app:layout_constraintLeft_toLeftOf="parent"
        app:layout_constraintRight_toLeftOf="@+id/tv2"
        app:layout_constraintTop_toTopOf="parent"/>

    <TextView
        android:id="@+id/tv2"
        android:layout_width="wrap_content"
        android:layout_height="50dp"
        android:layout_marginTop="0dp"
        android:background="#ff538c"
        android:gravity="center"
        android:text="Hello World!"
        app:layout_constraintLeft_toRightOf="@+id/tv1"
        app:layout_constraintRight_toLeftOf="@+id/tv3"
        app:layout_constraintTop_toTopOf="parent"/>

    <TextView
        android:id="@+id/tv3"
        android:layout_width="wrap_content"
        android:layout_height="50dp"
        android:background="#41c0ff"
        android:gravity="center"
        android:text="Hello World!"
        app:layout_constraintLeft_toRightOf="@+id/tv2"
        app:layout_constraintRight_toRightOf="parent"
        app:layout_constraintTop_toTopOf="parent"/>

</android.support.constraint.ConstraintLayout>

```

![](http://upload-images.jianshu.io/upload_images/2552605-1786e81381fb0c1c.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

链条分为水平链条和竖直链条两种，分别用 layout_constraintHorizontal_chainStyle 和 layout_constraintVertical_chainStyle 两个属性来设置
属性值有以下三种：

 - spread
 - spread_inside
 - packed

默认值为 spread

可以通过下图来理解各个参数值的含义

![](http://upload-images.jianshu.io/upload_images/2552605-dd99a16344da8c4a.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

当参数值为 spread 以及控件宽度非 0 时

```
 android:layout_width="wrap_content"
 app:layout_constraintHorizontal_chainStyle="spread"
```

![](http://upload-images.jianshu.io/upload_images/2552605-7bc7b4a9536c168e.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

当参数值为 spread 以及控件宽度为 0 时

```
 android:layout_width="0dp"
 app:layout_constraintHorizontal_chainStyle="spread"
```

![](http://upload-images.jianshu.io/upload_images/2552605-0c1147a600147646.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

当参数值为 packed 以及控件宽度非0时

```
 android:layout_width="wrap_content"
 app:layout_constraintHorizontal_chainStyle="packed"
```

![](http://upload-images.jianshu.io/upload_images/2552605-7cd758817bd4f8c7.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

当参数值为 spread_inside 以及控件宽度非0时

```
 android:layout_width="wrap_content"
 app:layout_constraintHorizontal_chainStyle="spread_inside"
```

![](http://upload-images.jianshu.io/upload_images/2552605-bd99d179530d39b7.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)