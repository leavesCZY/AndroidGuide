> 公众号：[字节数组](https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/36784c0d2b924b04afb5ee09eb16ca6f~tplv-k3u1fbpfcp-watermark.image)，热衷于分享 Android 系统源码解析，Jetpack 源码解析、热门开源库源码解析等面试必备的知识点

ConstraintLayout 目前是 Android Studio 的默认布局，其优势就是可以使用扁平化的视图层次结构（无嵌套视图组）来创建复杂多变的大型布局，在绘制效率上相对其它布局有很大优势。ConstraintLayout 与 RelativeLayout 相似，其中所有的视图均根据同级视图与父布局之间的关系来进行定位，但其灵活性要高于 RelativeLayout，并且更易于与 Android Studio 的布局编辑器配合使用

ConstraintLayout 能够灵活地定位和调整子 View 的大小，子 View 依靠约束关系来确定位置，且每个子 View 必须至少有一个**水平约束条件**加一个**垂直约束条件**，每个约束条件均表示与其它视图、父布局或隐形引导线之间连接或对齐方式。在一个约束关系中，需要有一个 Source（源）以及一个 Target（目标），Source 的位置依赖于 Target，可以理解为**通过约束关系 Source 与 Target 链接在了一起，Source 相对于 Target 的位置便是固定的了**

引入当前最新的 release 版本：

```groovy
dependencies {
    implementation "androidx.constraintlayout:constraintlayout:2.0.4"
}
```

### 一、相对定位

ConstraintLayout 最基本的属性包含以下几个，即 `layout_constraintXXX_toYYYOf` 格式的属性，用于将 ViewA 的 XXX 方向和 ViewB 的 YYY 方向进行约束。当中，ViewB 也可以是父容器 ConstraintLayout，用 parent 来表示。这些属性都是用于为控件添加垂直和水平方向的约束力，根据约束力的 “有无” 或者 “强弱”，控件会处于不同的位置

- `layout_constraintLeft_toLeftOf`
- `layout_constraintLeft_toRightOf`
- `layout_constraintRight_toLeftOf`
- `layout_constraintRight_toRightOf`
- `layout_constraintTop_toTopOf`
- `layout_constraintTop_toBottomOf`
- `layout_constraintBottom_toTopOf`
- `layout_constraintBottom_toBottomOf`
- `layout_constraintStart_toEndOf`
- `layout_constraintStart_toStartOf`
- `layout_constraintEnd_toStartOf`
- `layout_constraintEnd_toEndOf`
- `layout_constraintBaseline_toBaselineOf`

![](https://testczy.oss-cn-beijing.aliyuncs.com/%E6%96%87%E7%AB%A0/ConstraintLayout/2552605-fcbaeb45150186d9.png)

例如，根据约束的不同，控件在不同的方向上进行对齐

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    tools:context=".ConstraintLayoutActivity">

    <TextView
        android:id="@+id/tv1"
        android:layout_width="200dp"
        android:layout_height="150dp"
        android:layout_margin="20dp"
        android:background="#f5ec7e"
        android:gravity="center"
        android:text="Hello World!"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toStartOf="@+id/tv2"
        app:layout_constraintTop_toTopOf="parent" />

    <TextView
        android:id="@+id/tv2"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_marginEnd="20dp"
        android:background="#68b0f9"
        android:gravity="center"
        android:text="没有设置底部约束，所以只会顶部和黄色方块对齐"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toEndOf="@+id/tv1"
        app:layout_constraintTop_toTopOf="@+id/tv1" />

    <TextView
        android:id="@+id/tv3"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_marginEnd="20dp"
        android:background="#984ff7"
        android:gravity="center"
        android:text="上下均设置了约束，所以会居于中间"
        app:layout_constraintBottom_toBottomOf="@+id/tv1"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toEndOf="@+id/tv1"
        app:layout_constraintTop_toBottomOf="@+id/tv2" />

    <TextView
        android:id="@+id/tv4"
        android:layout_width="100dp"
        android:layout_height="100dp"
        android:background="#9C27B0"
        android:gravity="center"
        android:text="屏幕各个方向居中"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent" />

</androidx.constraintlayout.widget.ConstraintLayout>
```

![](https://testczy.oss-cn-beijing.aliyuncs.com/%E6%96%87%E7%AB%A0/ConstraintLayout/QQ%E6%88%AA%E5%9B%BE20201224233451.png)

### 二、约束力的强度

如果想要让控件的左右或者上下间距具有固定的比例，这种即在某个方向上其两边的约束力的强度有所不同，可以依靠 `layout_constraintHorizontal_bias` 和 `layout_constraintVertical_bias` 两个属性来设置控件在水平和垂直方向的偏移量

例如，可以来控制 TextView 的左右或者上下间距的百分比

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    tools:context=".ConstraintLayoutActivity">

    <TextView
        android:layout_width="200dp"
        android:layout_height="200dp"
        android:background="#FF9800"
        android:gravity="center"
        android:text="公众号：字节数组"
        android:textColor="@android:color/white"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintHorizontal_bias="0.9"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintVertical_bias="0.1" />

    <TextView
        android:layout_width="200dp"
        android:layout_height="200dp"
        android:background="#2196F3"
        android:gravity="center"
        android:text="https://github.com/leavesC"
        android:textColor="@android:color/white"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintHorizontal_bias="0.9"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintVertical_bias="0.9" />

    <TextView
        android:layout_width="200dp"
        android:layout_height="200dp"
        android:background="#4CAF50"
        android:gravity="center"
        android:text="Hello World!"
        android:textColor="@android:color/white"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintHorizontal_bias="0.1"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent" />

</androidx.constraintlayout.widget.ConstraintLayout>
```

![](https://testczy.oss-cn-beijing.aliyuncs.com/%E6%96%87%E7%AB%A0/ConstraintLayout/QQ%E6%88%AA%E5%9B%BE20201226133740.png)

### 三、宽高比

在使用其它布局类型时，如果想让控件在不同的屏幕上都保持固定的宽高比是比较麻烦的，但用 ConstraintLayout 就很简单。例如，如果我们想为 Activity 实现一个固定宽高比的顶部标题栏的话，可以将宽度设置为占满屏幕，高设置为 0dp，然后通过 `app:layout_constraintDimensionRatio` 属性设定宽高比为一个固定比例，此时 ConstraintLayout 就会自动根据屏幕的宽度来动态计算标题栏应该具有的高度

此外，要使用`layout_constraintDimensionRatio`属性，需要其宽度或者高度当中有一个值是**可知的**，且剩下的一个是 0dp。所谓的**可知**，即该值是已经具备了明确的约束条件。控件的宽高尺寸比例则通过 “float值” 或者 “宽度 : 高度” 的形式来设置，通过在比例值的前面添加 w 或者 h 来指明比例值是根据宽度还是高度来进行计算

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    tools:context=".ConstraintLayoutActivity">

    <TextView
        android:layout_width="0dp"
        android:layout_height="0dp"
        android:background="#f5ec7e"
        android:gravity="center"
        android:text="公众号：字节数组"
        android:textColor="@android:color/black"
        android:textSize="20sp"
        android:textStyle="bold"
        app:layout_constraintDimensionRatio="h,15:2"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent" />

    <TextView
        android:layout_width="100dp"
        android:layout_height="0dp"
        android:background="#5476fd"
        android:gravity="center"
        android:text="Hello World!"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintDimensionRatio="W,1:1"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent" />

</androidx.constraintlayout.widget.ConstraintLayout>
```

![](https://testczy.oss-cn-beijing.aliyuncs.com/%E6%96%87%E7%AB%A0/ConstraintLayout/QQ%E6%88%AA%E5%9B%BE20201226134656.png)

### 四、控件之间的宽高占比

ConstraintLayout 也可以像 LinearLayout 一样为子控件设置 `layout_weight` 属性，从而控件子控件之间的宽高占比，对应的属性是：`layout_constraintHorizontal_weight` 和 `layout_constraintVertical_weight`

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    tools:context=".ConstraintLayoutActivity">

    <TextView
        android:id="@+id/tv1"
        android:layout_width="0dp"
        android:layout_height="0dp"
        android:background="#f5ec7e"
        android:gravity="center"
        android:text="Hello World!"
        app:layout_constraintBottom_toTopOf="@id/tv4"
        app:layout_constraintEnd_toStartOf="@+id/tv2"
        app:layout_constraintHorizontal_weight="3"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintVertical_weight="1" />

    <TextView
        android:id="@+id/tv2"
        android:layout_width="0dp"
        android:layout_height="50dp"
        android:background="#55e4f4"
        android:gravity="center"
        android:text="Hello World!"
        app:layout_constraintEnd_toStartOf="@+id/tv3"
        app:layout_constraintHorizontal_weight="2"
        app:layout_constraintStart_toEndOf="@+id/tv1"
        app:layout_constraintTop_toTopOf="parent" />

    <TextView
        android:id="@+id/tv3"
        android:layout_width="0dp"
        android:layout_height="50dp"
        android:background="#f186ad"
        android:gravity="center"
        android:text="Hello World!"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintHorizontal_weight="1"
        app:layout_constraintStart_toEndOf="@+id/tv2"
        app:layout_constraintTop_toTopOf="parent" />

    <TextView
        android:id="@+id/tv4"
        android:layout_width="0dp"
        android:layout_height="0dp"
        android:background="#03A9F4"
        android:gravity="center"
        android:text="Hello World!"
        app:layout_constraintBottom_toTopOf="@id/tv5"
        app:layout_constraintEnd_toEndOf="@id/tv1"
        app:layout_constraintStart_toStartOf="@id/tv1"
        app:layout_constraintTop_toBottomOf="@id/tv1"
        app:layout_constraintVertical_weight="1" />

    <TextView
        android:id="@+id/tv5"
        android:layout_width="0dp"
        android:layout_height="0dp"
        android:background="#F44336"
        android:gravity="center"
        android:text="Hello World!"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintEnd_toEndOf="@id/tv4"
        app:layout_constraintStart_toStartOf="@id/tv4"
        app:layout_constraintTop_toBottomOf="@id/tv4"
        app:layout_constraintVertical_weight="1" />

</androidx.constraintlayout.widget.ConstraintLayout>
```

![](https://testczy.oss-cn-beijing.aliyuncs.com/%E6%96%87%E7%AB%A0/ConstraintLayout/QQ%E6%88%AA%E5%9B%BE20201226140239.png)

### 五、Dimensions

当控件的宽或者高设置为 0dp 时，可以用以下两个属性来指定控件的宽度或高度占父控件空间的百分比，属性值在 0 到 1 之间

1. layout_constrainWidth_percent
2. layout_constrainHeight_percent

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    tools:context=".ConstraintLayoutActivity">

    <Button
        android:id="@+id/btn_target"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:text="Target Button"
        android:textAllCaps="false"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintWidth_percent="0.8" />

    <Button
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:text="Source Button"
        android:textAllCaps="false"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toBottomOf="@id/btn_target"
        app:layout_constraintWidth_percent="0.5" />

</androidx.constraintlayout.widget.ConstraintLayout>
```

![](https://testczy.oss-cn-beijing.aliyuncs.com/%E6%96%87%E7%AB%A0/ConstraintLayout/QQ%E6%88%AA%E5%9B%BE20201225003233.png)

### 六、Visibility

在使用其它布局时，如果将 View 的 visibility 属性设置为 gone，那么其它原本依赖该 View 来参照定位的属性都会失效，而在 ConstraintLayout 布局中会有所不同

在以下布局中，红色方块位于屏幕右上角与黄色方块左下角形成的矩形的中间位置

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    tools:context=".ConstraintLayoutActivity">

    <TextView
        android:id="@+id/tv1"
        android:layout_width="100dp"
        android:layout_height="100dp"
        android:background="#f5ec7e"
        android:gravity="center"
        android:text="Hello World!"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent" />

    <TextView
        android:id="@+id/tv2"
        android:layout_width="100dp"
        android:layout_height="100dp"
        android:background="#fa6e61"
        android:gravity="center"
        android:text="Hello World!"
        app:layout_constraintBottom_toBottomOf="@+id/tv1"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="@+id/tv1"
        app:layout_constraintTop_toTopOf="parent" />

</androidx.constraintlayout.widget.ConstraintLayout>
```

![](https://testczy.oss-cn-beijing.aliyuncs.com/%E6%96%87%E7%AB%A0/ConstraintLayout/QQ%E6%88%AA%E5%9B%BE20201224231209.png)

而如果将黄色方块的 visibility 属性设置为 gone，那么红色方块的位置会发生变化。可以理解为黄色方块缩小为一个不可见的小点，位于其原先位置的中间，而红色方块则改为依照该点来进行定位

![](https://testczy.oss-cn-beijing.aliyuncs.com/%E6%96%87%E7%AB%A0/ConstraintLayout/QQ%E6%88%AA%E5%9B%BE20201224231313.png)

此外，红色方块也可以依靠以下几个属性来控制当黄色方块为 Gone 时红色方块的 margin 值，这类属性只有在黄色方块的 visibility 属性设置为 gone 时才会生效

- `layout_goneMarginStart`
- `layout_goneMarginEnd`
- `layout_goneMarginLeft`
- `layout_goneMarginTop`
- `layout_goneMarginRight`
- `layout_goneMarginBottom`

### 七、圆形定位

圆形定位用于将两个 View 以**角度**和**距离**这两个维度来进行定位，以两个 View 的中心点作为定位点

1. app:layout_constraintCircle 		           - 目标 View 的 ID
2. app:layout_constraintCircleAngle           - 对齐的角度
3. app:layout_constraintCircleRadius          - 与目标 View 之间的距离（顺时针方向，0~360度）

![](https://testczy.oss-cn-beijing.aliyuncs.com/%E6%96%87%E7%AB%A0/ConstraintLayout/circle1.png)

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    tools:context=".ConstraintLayoutActivity">

    <ImageView
        android:id="@+id/iv_a"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:src="@drawable/icon_a"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent" />

    <ImageView
        android:id="@+id/iv_b"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:src="@drawable/icon_b"
        app:layout_constraintCircle="@id/iv_a"
        app:layout_constraintCircleAngle="45"
        app:layout_constraintCircleRadius="200dp" />

    <ImageView
        android:id="@+id/iv_c"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:src="@drawable/icon_c"
        app:layout_constraintCircle="@id/iv_a"
        app:layout_constraintCircleAngle="180"
        app:layout_constraintCircleRadius="200dp" />

</androidx.constraintlayout.widget.ConstraintLayout>
```

![](https://testczy.oss-cn-beijing.aliyuncs.com/%E6%96%87%E7%AB%A0/ConstraintLayout/QQ%E6%88%AA%E5%9B%BE20201225002744.png)

### 八、Guideline

当需要一个任意位置的锚点时，可以使用指示线（Guideline）来帮助定位，Guideline 是 View 的子类，使用方式和普通的 View 相同，但 Guideline 有着如下的特殊属性：

- 宽度和高度均为 0
- 可见性为 View.GONE

即指示线只是为了帮助其他 View 进行定位，实际上并不会出现在页面上

例如，如下代码加入了两条 Guideline，可以选择使用百分比或实际距离来设置 Guideline 的位置，并通过 orientation 属性来设置 Guideline 的方向

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    tools:context=".ConstraintLayoutActivity">

    <androidx.constraintlayout.widget.Guideline
        android:id="@+id/guideline1"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        app:layout_constraintGuide_percent="0.5" />

    <androidx.constraintlayout.widget.Guideline
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

</androidx.constraintlayout.widget.ConstraintLayout>
```

设置横向指示线距离顶部 100dp，黄色方块根据该指示线来设定顶部位置。竖向指示线设置其横向距离百分比为 0.5，所以黄色方块的左侧会位于屏幕的中间位置

![](https://testczy.oss-cn-beijing.aliyuncs.com/%E6%96%87%E7%AB%A0/ConstraintLayout/QQ%E6%88%AA%E5%9B%BE20201224231530.png)

### 九、Barrier

很多时候我们都会遇到控件的宽高值随着其包含的数据的多少而改变的情况，而此时如果有多个控件之间是相互约束的话，就比较难来设定各个控件间的约束关系了，而 Barrier（屏障）就是用于解决这种情况。Barrier 和 GuideLine 一样是一个虚拟的 View，对界面是不可见的，只是用于辅助布局

Barrier 可以使用的属性有：

1. barrierDirection：用于设置 Barrier 的位置，属性值有：bottom、top、start、end、left、right
2. constraint_referenced_ids：用于设置 Barrier 所引用的控件的 ID，可同时设置多个
3. barrierAllowsGoneWidgets：默认为 true，当 Barrier 所引用的控件为 Gone 时，则 Barrier 的创建行为是在已 Gone 的控件已解析的位置上进行创建。如果设置为 false，则不会将 Gone 的控件考虑在内

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    tools:context=".ConstraintLayoutActivity">

    <TextView
        android:id="@+id/btn_target"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:background="#03A9F4"
        android:padding="10dp"
        android:text="这是一段并不长的文本"
        android:textAllCaps="false"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent" />

    <TextView
        android:id="@+id/btn_source"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:background="#009688"
        android:padding="10dp"
        android:text="我也不知道说什么好"
        android:textAllCaps="false"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toBottomOf="@id/btn_target" />

    <androidx.constraintlayout.widget.Barrier
        android:id="@+id/barrier"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        app:barrierAllowsGoneWidgets="false"
        app:barrierDirection="end"
        app:constraint_referenced_ids="btn_target,btn_source" />

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:background="#E91E63"
        android:padding="10dp"
        android:text="那就随便写写吧"
        android:textAllCaps="false"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintStart_toEndOf="@id/barrier"
        app:layout_constraintTop_toBottomOf="@id/btn_target" />

</androidx.constraintlayout.widget.ConstraintLayout>
```

![](https://testczy.oss-cn-beijing.aliyuncs.com/%E6%96%87%E7%AB%A0/ConstraintLayout/QQ%E6%88%AA%E5%9B%BE20201226151844.png)

布局文件中约束了红色方块必须是一直处于**蓝色方块+绿色方块**这个整体的右侧，此时还看不出来 Barrier 的作用，但当文本内容增多时，就可以看出来了。不管是蓝色方块还是绿色方块的宽度变大，红色方块都会自动向右侧移动

![](https://testczy.oss-cn-beijing.aliyuncs.com/%E6%96%87%E7%AB%A0/ConstraintLayout/QQ%E6%88%AA%E5%9B%BE20201226152135.png)

### 十、Group

Group 用于控制多个控件的可见性，先依靠 `constraint_referenced_ids`来绑定其它 View，之后就可以通过单独控制 Group 的可见性从而来间接改变绑定的 View 的可见性

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    tools:context=".ConstraintLayoutActivity">

    <Button
        android:id="@+id/btn_target"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Target Button"
        android:textAllCaps="false"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent" />

    <Button
        android:id="@+id/btn_source"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Source Button"
        android:textAllCaps="false"
        app:layout_constraintCircle="@id/btn_target"
        app:layout_constraintCircleAngle="45"
        app:layout_constraintCircleRadius="120dp" />

    <androidx.constraintlayout.widget.Group
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:visibility="visible"
        app:constraint_referenced_ids="btn_target, btn_source" />

</androidx.constraintlayout.widget.ConstraintLayout>
```

### 十一、Placeholder

Placeholder （占位符）用于和一个视图关联起来，通过 `setContentId()` 方法将占位符转换为指定的视图，即视图将在占位符所在位置上显示，如果此时布局中已包含该视图，则视图将从原有位置消失

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    tools:context=".ConstraintLayoutActivity">

    <Button
        android:id="@+id/btn_setContentId"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:onClick="setContentId"
        android:text="setContentId"
        android:textAllCaps="false"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent" />

    <ImageView
        android:id="@+id/iv_target"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:src="@drawable/icon_a"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toBottomOf="@id/btn_setContentId" />

    <androidx.constraintlayout.widget.Placeholder
        android:id="@+id/placeholder"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent" />

</androidx.constraintlayout.widget.ConstraintLayout>
```

```kotlin
        val placeholder = findViewById<Placeholder>(R.id.placeholder)
        placeholder.setContentId(R.id.iv_target)
```

![](http://testczy.oss-cn-beijing.aliyuncs.com/%E6%96%87%E7%AB%A0/ConstraintLayout/setContentId.gif)

### 十二、Chains

Chain 比较难描述，它是一种特殊的约束形式，多个控件通过明确的相互约束来互相约束对方的位置，从而形成一个链条，Chain 可以设定链条中的剩余空间的分发规则

例如，以下布局中三个 TextView 都明确规定了其左侧和右侧的约束条件，三个 TextView 形成了一个整体，此时它们就可以称为一条链条

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    tools:context=".ConstraintLayoutActivity">

    <TextView
        android:id="@+id/tv1"
        android:layout_width="wrap_content"
        android:layout_height="50dp"
        android:background="#f5ec7e"
        android:gravity="center"
        android:text="Hello World!"
        app:layout_constraintEnd_toStartOf="@+id/tv2"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent" />

    <TextView
        android:id="@+id/tv2"
        android:layout_width="wrap_content"
        android:layout_height="50dp"
        android:layout_marginTop="0dp"
        android:background="#ff538c"
        android:gravity="center"
        android:text="Hello World!"
        app:layout_constraintEnd_toStartOf="@+id/tv3"
        app:layout_constraintStart_toEndOf="@+id/tv1"
        app:layout_constraintTop_toTopOf="parent" />

    <TextView
        android:id="@+id/tv3"
        android:layout_width="wrap_content"
        android:layout_height="50dp"
        android:background="#41c0ff"
        android:gravity="center"
        android:text="Hello World!"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toEndOf="@+id/tv2"
        app:layout_constraintTop_toTopOf="parent" />

</androidx.constraintlayout.widget.ConstraintLayout>
```

![](https://testczy.oss-cn-beijing.aliyuncs.com/%E6%96%87%E7%AB%A0/ConstraintLayout/QQ%E6%88%AA%E5%9B%BE20201225001232.png)

链条分为**水平链条**和**竖直链条**两种，分别用 `layout_constraintHorizontal_chainStyle` 和 `layout_constraintVertical_chainStyle` 两个属性来设置，属性值有以下三种：

- spread（默认值）
- spread_inside
- packed

![](https://testczy.oss-cn-beijing.aliyuncs.com/%E6%96%87%E7%AB%A0/ConstraintLayout/2552605-dd99a16344da8c4a.png)

直接看效果图才容易理解各种效果

当值为 spread 以及控件宽度为 wrap_content 时

```xml
 android:layout_width="wrap_content"
 app:layout_constraintHorizontal_chainStyle="spread"
```

![](https://testczy.oss-cn-beijing.aliyuncs.com/%E6%96%87%E7%AB%A0/ConstraintLayout/QQ%E6%88%AA%E5%9B%BE20201225001521.png)

当参数值为 spread 以及控件宽度为 0dp 时

```xml
 android:layout_width="0dp"
 app:layout_constraintHorizontal_chainStyle="spread"
```

![](https://testczy.oss-cn-beijing.aliyuncs.com/%E6%96%87%E7%AB%A0/ConstraintLayout/QQ%E6%88%AA%E5%9B%BE20201225001830.png)

当参数值为 spread_inside 以及控件宽度为 wrap_content 时

```xml
 android:layout_width="wrap_content"
 app:layout_constraintHorizontal_chainStyle="spread_inside"
```

![](https://testczy.oss-cn-beijing.aliyuncs.com/%E6%96%87%E7%AB%A0/ConstraintLayout/QQ%E6%88%AA%E5%9B%BE20201225002028.png)

当参数值为 packed 以及控件宽度为 wrap_content 时

```xml
 android:layout_width="wrap_content"
 app:layout_constraintHorizontal_chainStyle="packed"
```

![](https://testczy.oss-cn-beijing.aliyuncs.com/%E6%96%87%E7%AB%A0/ConstraintLayout/QQ%E6%88%AA%E5%9B%BE20201225001927.png)

### 十三、Flow

[Flow](https://developer.android.google.cn/reference/androidx/constraintlayout/helper/widget/Flow?hl=en) 是一种新的虚拟布局，它专门用来构建链式排版效果，当出现空间不足的情况时能够自动换行，甚至是自动延展到屏幕的另一区域。当需要对多个元素进行链式布局，但不确定在运行时布局空间的实际大小是多少时 Flow 对你来说就非常有用。你可以使用 Flow 来实现让布局随着应用屏幕尺寸的变化 (比如设备发生旋转后出现的屏幕宽度变化) 而动态地进行自适应。此外，Flow 是一种虚拟布局，并不会作为视图添加到视图层级结构中，而是仅仅引用其它视图来辅助它们在布局系统中完成各自的布局功能

![](https://testczy.oss-cn-beijing.aliyuncs.com/%E6%96%87%E7%AB%A0/ConstraintLayout/2d4b3159aeec49e6a40d07f123efd095_tplv-k3u1fbpfcp-zoom-1.gif)



Flow 中最重要的一个配置选项是 `wrapMode`，它可以决定在内容溢出 (或出现换行) 时的布局行为，一共有三种模式：

- **none** – 所有引用的视图以一条链的方式进行布局，如果内容溢出则溢出内容不可见
- **chain** – 当出现溢出时，溢出的内容会自动换行，以新的一条链的方式进行布局
- **align** – 同 chain 类似，但是不以行而是以列的方式进行布局

![](https://testczy.oss-cn-beijing.aliyuncs.com/%E6%96%87%E7%AB%A0/ConstraintLayout/8ab1b82a76714c20842697309f260693_tplv-k3u1fbpfcp-zoom-1.gif)

例如，你可以在布局文件中引入五个 CardView，每个 CardView 的方向约束均交由 Flow 来控制，Flow 默认是以水平方向来展示，可以主动设置 `android:orientation="vertical"`改为竖直方向

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    tools:context=".FlowActivity">

    <include
        android:id="@+id/cardView1"
        layout="@layout/item_cardview" />

    <include
        android:id="@+id/cardView2"
        layout="@layout/item_cardview" />

    <include
        android:id="@+id/cardView3"
        layout="@layout/item_cardview" />

    <include
        android:id="@+id/cardView4"
        layout="@layout/item_cardview" />

    <include
        android:id="@+id/cardView5"
        layout="@layout/item_cardview" />

    <androidx.constraintlayout.helper.widget.Flow
        android:id="@+id/flow"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        app:constraint_referenced_ids="cardView1,cardView2,cardView3,cardView4,cardView5"
        app:flow_horizontalGap="30dp"
        app:flow_verticalGap="30dp"
        app:flow_wrapMode="none"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent" />

</androidx.constraintlayout.widget.ConstraintLayout>
```

#### none

此模式下控件不会自动换行，且由于屏幕宽度无法完整展示，所以只会展示一部分内容

![](http://testczy.oss-cn-beijing.aliyuncs.com/%E6%96%87%E7%AB%A0/ConstraintLayout/QQ%E6%88%AA%E5%9B%BE20201226163218.png)

该模式下可以同时使用的配置项有：

- flow_horizontalStyle = "spread|spread_inside|packed"   //Chains 链的展示形式
- flow_verticalStyle = "spread|spread_inside|packed"
- flow_horizontalBias = "*float*"    //只在 style 为 packed 时才生效，用于控制控件在水平方向上的偏移量
- flow_verticalBias = "*float*"
- flow_horizontalGap = "*dimension*"   //设置每个控件的左右间距
- flow_verticalGap = "*dimension*"
- flow_horizontalAlign = "start|end"
- flow_verticalAlign = "top|bottom|center|baseline

#### chain

此模式下控件会自动换行，且不足一行的内容会居中显示

![](http://testczy.oss-cn-beijing.aliyuncs.com/%E6%96%87%E7%AB%A0/ConstraintLayout/QQ%E6%88%AA%E5%9B%BE20201226170636.png)

此模式下可以同时使用的配置项有：

- flow_firstHorizontalStyle = "spread|spread_inside|packed"  //第一行 Chains 链的展示形式
- flow_firstVerticalStyle = "spread|spread_inside|packed" 
- flow_firstHorizontalBias = "*float*"   //只在 style 为 packed 时才生效，用于控制第一行在水平方向上的偏移量
- flow_firstVerticalBias = "*float*"
- flow_lastHorizontalStyle = "spread|spread_inside|packed"  //最后一行 Chains 链的展示形式
- flow_lastHorizontalBias = "*float*"

看个例子：

```xml
    <androidx.constraintlayout.helper.widget.Flow
        android:id="@+id/flow"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        app:constraint_referenced_ids="cardView1,cardView2,cardView3,cardView4,cardView5"
        app:flow_firstHorizontalStyle="spread_inside"
        app:flow_horizontalGap="30dp"
        app:flow_lastHorizontalBias="1"
        app:flow_lastHorizontalStyle="packed"
        app:flow_verticalGap="30dp"
        app:flow_wrapMode="chain"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent" />
```

由于 `flow_firstHorizontalStyle` 值为 `spread_inside`，所以首行会往两侧靠边。由于 `flow_lastHorizontalBias`值为 1，所以最后一行也会直接往右靠拢

![](http://testczy.oss-cn-beijing.aliyuncs.com/%E6%96%87%E7%AB%A0/ConstraintLayout/QQ%E6%88%AA%E5%9B%BE20201226171313.png)

#### aligned

此模式和 chain 类似，区别在于不足一行的内容会靠边对齐显示

![](http://testczy.oss-cn-beijing.aliyuncs.com/%E6%96%87%E7%AB%A0/ConstraintLayout/QQ%E6%88%AA%E5%9B%BE20201226171558.png)

### 十四、Layer

[Layer](https://developer.android.google.cn/reference/androidx/constraintlayout/helper/widget/Layer) 作为一种新的辅助工具，可以在多个视图上创建一个虚拟的图层 (layer)，和 Flow 不同，它并不会对视图进行布局，而是对多个视图同时进行变换 (transformation) 操作。如果想对多个视图整体进行旋转 (rotate)、平移 (translate) 或缩放 (scale) 操作，那么 Layer 将会是最佳的选择

在布局文件中先通过 Layer 引用需要进行变换的所有 View，可以不用对 Layer 进行位置约束

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    tools:context=".LayerActivity">

    <TextView
        android:id="@+id/tv1"
        android:layout_width="100dp"
        android:layout_height="100dp"
        android:background="#f5ec7e"
        android:gravity="center"
        android:text="Hello World!"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent" />

    <TextView
        android:id="@+id/tv2"
        android:layout_width="100dp"
        android:layout_height="100dp"
        android:background="#FF9800"
        android:gravity="center"
        android:text="Hello World!"
        app:layout_constraintStart_toEndOf="@id/tv1"
        app:layout_constraintTop_toBottomOf="@id/tv1" />

    <TextView
        android:id="@+id/tv3"
        android:layout_width="100dp"
        android:layout_height="100dp"
        android:background="#2196F3"
        android:gravity="center"
        android:text="Hello World!"
        app:layout_constraintStart_toEndOf="@id/tv2"
        app:layout_constraintTop_toBottomOf="@id/tv2" />

    <androidx.constraintlayout.helper.widget.Layer
        android:id="@+id/layer"
        android:layout_width="0dp"
        android:layout_height="0dp"
        app:constraint_referenced_ids="tv1, tv2, tv3"
        tools:ignore="MissingConstraints" />

    <Button
        android:id="@+id/btn_test"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:padding="20dp"
        android:text="开启动画"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent" />

</androidx.constraintlayout.widget.ConstraintLayout>
```

然后在代码中直接对 Layer 进行动画操作，这样其引用到的所有 View 都会进行整体动画

```kotlin
/**
 * @Author: leavesC
 * @Date: 2020/12/26 22:06
 * @Desc:
 * @Github：https://github.com/leavesC
 */
class LayerActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_layer)
        btn_test.setOnClickListener {
            val layer = findViewById<Layer>(R.id.layer)
            val animator = ValueAnimator.ofFloat(0f, 360f)
            animator.addUpdateListener { animation ->
                val angle = animation.animatedValue as Float
                layer.rotation = angle
                layer.scaleX = 1 + (180 - abs(angle - 180)) / 20f
                layer.scaleY = 1 + (180 - abs(angle - 180)) / 20f
                val translationX = 500 * sin(Math.toRadians((angle * 5).toDouble())).toFloat()
                val translationY = 500 * sin(Math.toRadians((angle * 7).toDouble())).toFloat()
                layer.translationX = translationX
                layer.translationY = translationY
            }
            animator.duration = 6000
            animator.start()
        }
    }

}
```

![](http://testczy.oss-cn-beijing.aliyuncs.com/%E6%96%87%E7%AB%A0/ConstraintLayout/layer.gif)

此外，Layer 比较有用的一个点就是可以用于设置背景色，以前如果我们想要对某块区域设置一个背景色的话往往需要多嵌套一层，而如果使用 Layer 的话则可以直接设置，不需要进行嵌套

### 十五、ConstraintSet

Layer 是对 ConstraintLayout 内的一部分控件做动画变换，ConstraintSet 则是用于对 ConstraintLayout 整体进行一次动画变换

ConstraintSet 可以理解为 ConstraintLayout 对其所有子控件的约束规则的集合。在不同的交互规则下，我们可能需要改变 ConstraintLayout 内的所有子控件的约束条件，即子控件的位置需要做一个大调整，ConstraintSet 就用于实现平滑地改变子控件的位置

例如，我们需要在不同的场景下使用两种不同的布局形式，先定义好这两种布局文件，其中子 View 的 Id 必须保持一致，View 的约束条件则可以随意设置。然后在代码中通过 ConstraintSet 来加载这两个布局文件的约束规则，apply 给 ConstraintLayout 后即可平滑地切换两种布局效果

```kotlin
/**
 * @Author: leavesC
 * @Date: 2020/12/26 23:02
 * @Desc:
 * @Github：https://github.com/leavesC
 */
class ConstraintSetActivity : AppCompatActivity() {

    companion object {

        private const val SHOW_BIG_IMAGE = "showBigImage"

    }

    private var showBigImage = false

    private val constraintSetNormal = ConstraintSet()

    private val constraintSetBig = ConstraintSet()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_constraint_set)
        //获取初始的约束集
        constraintSetNormal.clone(cl_rootView)
        //加载目标约束集
        constraintSetBig.load(this, R.layout.activity_constraint_set_big)
        if (savedInstanceState != null) {
            val previous = savedInstanceState.getBoolean(SHOW_BIG_IMAGE)
            if (previous != showBigImage) {
                showBigImage = previous
                applyConfig()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(SHOW_BIG_IMAGE, showBigImage)
    }

    fun toggleMode(view: View) {
        TransitionManager.beginDelayedTransition(cl_rootView)
        showBigImage = !showBigImage
        applyConfig()
    }

    //将约束集应用到控件上
    private fun applyConfig() {
        if (showBigImage) {
            constraintSetBig.applyTo(cl_rootView)
        } else {
            constraintSetNormal.applyTo(cl_rootView)
        }
    }

}
```

![](https://testczy.oss-cn-beijing.aliyuncs.com/%E6%96%87%E7%AB%A0/ConstraintLayout/ConstraintSetsassa.gif)

### 十六、ConstraintHelper

Flow 和 Layer 都是 ConstraintHelper 的子类，这两者都属于辅助布局的工具类，ConstraintLayout 也开放了 ConstraintHelper 交由开发者自己去进行自定义

例如，我们可以来实现这么一种逐步展开的动画效果

![](https://testczy.oss-cn-beijing.aliyuncs.com/%E6%96%87%E7%AB%A0/ConstraintLayout/ConstraintHelperSAFSAFSAFS.gif)

继承 ConstraintHelper，在 `updatePostLayout`方法中遍历其引用的所有控件，然后对每个控件应用 CircularReveal 动画。`updatePostLayout`方法会在执行 onLayout 之前被调用

```kotlin
/**
 * @Author: leavesC
 * @Date: 2020/12/26 23:47
 * @Desc:
 * @Github：https://github.com/leavesC
 */
class CircularRevealHelper @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ConstraintHelper(context, attrs, defStyleAttr) {

    override fun updatePostLayout(container: ConstraintLayout) {
        super.updatePostLayout(container)
        val views = getViews(container)
        for (view in views) {
            val anim = ViewAnimationUtils.createCircularReveal(
                view, view.width / 2,
                view.height / 2, 0f,
                hypot((view.height / 2).toDouble(), (view.width / 2).toDouble()).toFloat()
            )
            anim.duration = 3000
            anim.start()
        }
    }

}
```

在布局文件中引用需要执行动画的 View 即可

```kotlin
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    tools:context=".ConstraintHelperActivity">

    <ImageView
        android:id="@+id/imageView"
        android:layout_width="300dp"
        android:layout_height="300dp"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent"
        app:srcCompat="@drawable/icon_avatar" />

    <github.leavesc.constraint_layout.CircularRevealHelper
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        app:constraint_referenced_ids="imageView"
        tools:ignore="MissingConstraints" />

</androidx.constraintlayout.widget.ConstraintLayout>
```

### 十七、ImageFilterView

ImageFilterView 是放在 ConstraintLayout 的 `utils.widget`包下的一个 View，从包名可以猜测 ImageFilterView 只是 Google 官方提供的一个额外的工具属性的类，和 ConstraintLayout 本身并没有啥关联

ImageFilterView 直接继承于 AppCompatImageView，在其基础上扩展了很多用于实现图形变换的功能

| 属性         | 含义                                                         |
| ------------ | ------------------------------------------------------------ |
| altSrc       | 用于指定要从 src 变换成的目标图片，可以依靠 crossfade 来实现淡入淡出 |
| crossfade    | 设置 src 和 altSrc 两张图片之间的混合程度。0=src 1=altSrc图像 |
| saturation   | 饱和度。0=灰度，1=原始，2=过饱和                             |
| brightness   | 亮度。0=黑色，1=原始，2=两倍亮度                             |
| warmth       | 色温。1=自然，2=暖色，0.5=冷色                               |
| contrast     | 对比度。1=不变，0=灰色，2=高对比度                           |
| round        | 用于实现圆角，以 dimension 为值                              |
| roundPercent | 用于实现圆角，取值在 0f-1f 之间，为 1f 时将形成一张圆形图片  |

看个例子。在 xml 中声明多个 ImageFilterView

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    tools:context=".ImageFilterViewActivity">

    <androidx.constraintlayout.utils.widget.ImageFilterView
        android:id="@+id/imageView1"
        android:layout_width="100dp"
        android:layout_height="100dp"
        app:layout_constraintEnd_toStartOf="@id/imageView2"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent"
        app:srcCompat="@drawable/icon_avatar_normal" />

	//省略其它 ImageFilterView

    <androidx.appcompat.widget.AppCompatSeekBar
        android:id="@+id/seekBar"
        style="@style/Widget.AppCompat.ProgressBar.Horizontal"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_margin="20dp"
        android:max="100"
        android:progress="0"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent" />

</androidx.constraintlayout.widget.ConstraintLayout>
```

在代码中来调整以上属性值

```kotlin
/**
 * @Author: leavesC
 * @Date: 2020/12/27 0:17
 * @Desc:
 * @Github：https://github.com/leavesC
 */
class ImageFilterViewActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_filter_view)
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val realProgress = (progress / 100.0).toFloat()

                    imageView1.saturation = realProgress * 20
                    imageView2.brightness = 1 - realProgress

                    imageView3.warmth = realProgress * 20
                    imageView4.contrast = realProgress * 2

                    imageView5.round = realProgress * 40
                    imageView6.roundPercent = realProgress

                    imageView7.crossfade = realProgress
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {

            }
        })
    }

}
```

![](http://testczy.oss-cn-beijing.aliyuncs.com/%E6%96%87%E7%AB%A0/ConstraintLayout/ImageFilterViewsafafsa.gif)

### 十八、Demo 下载

示例代码我均已放到 Github，请查收：[AndroidOpenSourceDemo](https://github.com/leavesC/AndroidOpenSourceDemo)

### 十九、参考资料

- https://developer.android.google.cn/reference/androidx/constraintlayout/widget/ConstraintLayout
- https://juejin.cn/post/6905216987496972302
