ConstraintLayout 是目前 Android Studio 中项目的默认布局，目前已经到了 1.1.2 版本，这里就来介绍下它在新版本中的特性

#### 一、Circular Positioning

圆形定位用于将两个 View 以角度和距离这两个维度来进行定位，以两个 View 的中心点作为定位点

1. app:layout_constraintCircle 		    -目标 View 的 ID
2. app:layout_constraintCircleAngle           -对齐的角度
3. app:layout_constraintCircleRadius         -与目标 View 之间的距离（顺时针方向，0~360度）

```xml
<?xml version="1.0" encoding="utf-8"?>
<android.support.constraint.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    tools:context=".MainActivity">

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
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Source Button"
        android:textAllCaps="false"
        app:layout_constraintCircle="@id/btn_target"
        app:layout_constraintCircleAngle="45"
        app:layout_constraintCircleRadius="120dp" />

</android.support.constraint.ConstraintLayout>
```

![](https://upload-images.jianshu.io/upload_images/2552605-941f30e98061011a.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

#### 二、Enforcing constraints

在 1.1 版本之前，如果将控件的尺寸设置为 **wrap_content**，那么对控件设置的 **maxWidth**、**minHeight** 这些约束是不起作用的，而强制约束就用于使控件在设置 **wrap_content** 的情况下约束依然生效

1. app:layout_constrainedHeight="true | false"
2. app:layout_constrainedWidth="true | false"

```xml
<?xml version="1.0" encoding="utf-8"?>
<android.support.constraint.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    tools:context=".MainActivity">

    <ImageView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:src="@drawable/ic_launcher_background"
        app:layout_constrainedHeight="false"
        app:layout_constrainedWidth="false"
        app:layout_constraintHeight_max="50dp"
        app:layout_constraintLeft_toLeftOf="parent"
        app:layout_constraintRight_toRightOf="parent"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintWidth_max="50dp" />

    <ImageView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:src="@drawable/ic_launcher_background"
        app:layout_constrainedHeight="true"
        app:layout_constrainedWidth="true"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintHeight_max="50dp"
        app:layout_constraintLeft_toLeftOf="parent"
        app:layout_constraintRight_toRightOf="parent"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintWidth_max="50dp" />

    <ImageView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:src="@drawable/ic_launcher_background"
        app:layout_constrainedHeight="true"
        app:layout_constrainedWidth="false"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintHeight_max="50dp"
        app:layout_constraintLeft_toLeftOf="parent"
        app:layout_constraintRight_toRightOf="parent"
        app:layout_constraintWidth_max="50dp" />

</android.support.constraint.ConstraintLayout>
```
![](https://upload-images.jianshu.io/upload_images/2552605-7c8fa93cceadb556.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

#### 三、Dimensions

当控件的尺寸设置为 **0dp** 时，可以用以下两个属性来指定当前控件的宽度或高度占父控件的百分比，属性值在 0 到 1 之间

1. layout_constrainWidth_percent
2. layout_constrainHeight_percent

```xml
<?xml version="1.0" encoding="utf-8"?>
<android.support.constraint.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    tools:context=".MainActivity">

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

</android.support.constraint.ConstraintLayout>
```
![](https://upload-images.jianshu.io/upload_images/2552605-dc023b6b71e09761.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

#### 四、Barrier
很多时候我们都会遇到控件的大小随着其包含的数据的多少而改变的情况，而此时如果有多个控件之间是相互约束的话，就比较难来设定各个控件间的约束关系了
而 **Barrier**（屏障）就是用于这种情况，Barrier 和 **GuideLine** 一样是一个虚拟的 View，对界面是不可见的，只是用于辅助布局，而 Barrier 和 GuideLine 的区别在于它可以由多个 View 来决定其属性

Barrier 可以使用的属性有：

1. barrierDirection：用于设置 Barrier 的位置，属性值有：bottom、top、start、end、left、right
2. constraint_referenced_ids：用于设置 Barrier 所引用的控件的 ID，可同时设置多个
3. barrierAllowsGoneWidgets：默认为 true，当 Barrier 所引用的控件为 Gone 时，则 Barrier 的创建行为是在已 Gone 的控件已解析的位置上进行创建。如果设置为 false，则不会将 Gone 的控件考虑在内

```xml
<?xml version="1.0" encoding="utf-8"?>
<android.support.constraint.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    tools:context=".MainActivity">

    <Button
        android:id="@+id/btn_target"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="这是一段并不长的文本"
        android:textAllCaps="false"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent" />

    <Button
        android:id="@+id/btn_source"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="我也不知道说什么好"
        android:textAllCaps="false"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toBottomOf="@id/btn_target" />

    <android.support.constraint.Barrier
        android:id="@+id/barrier"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        app:barrierAllowsGoneWidgets="false"
        app:barrierDirection="end"
        app:constraint_referenced_ids="btn_target,btn_source" />

    <Button
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="那就随便写写吧"
        android:textAllCaps="false"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintStart_toEndOf="@id/barrier"
        app:layout_constraintTop_toBottomOf="@id/btn_target" />

</android.support.constraint.ConstraintLayout>
```

![](https://upload-images.jianshu.io/upload_images/2552605-6e81a264a2d3c017.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

此时还看不出来 Barrier 的作用，但当按钮内的文本内容增多时，就可以看出来了
![](https://upload-images.jianshu.io/upload_images/2552605-9db38b454404a05b.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

![](https://upload-images.jianshu.io/upload_images/2552605-a2410f15bfb3de17.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

#### 五、Group

Group 用于控制多个控件的可见性，如果要隐藏多个控件则不必再单独设置每个控件的可见性了，其使用到的属性有

1. constraint_referenced_ids ：用于设置所引用的控件的 ID

```xml
<?xml version="1.0" encoding="utf-8"?>
<android.support.constraint.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    tools:context=".MainActivity">

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

    <android.support.constraint.Group
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:visibility="visible"
        app:constraint_referenced_ids="btn_target, btn_source" />

</android.support.constraint.ConstraintLayout>
```

#### 六、Placeholder
Placeholder （占位符）用于和一个视图关联起来，通过 **setContentId()** 方法将占位符转换为指定的视图，即视图将在占位符所在位置上显示，如果此时布局中已包含该视图，则视图将从原有位置消失

```xml
<?xml version="1.0" encoding="utf-8"?>
<android.support.constraint.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    tools:context=".MainActivity">

    <Button
        android:id="@+id/btn_setContentId"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:onClick="setContentId"
        android:text="setContentId"
        android:textAllCaps="false" />

    <ImageView
        android:id="@+id/iv_target"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:src="@drawable/ic_launcher_background"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toBottomOf="@id/btn_setContentId" />

    <android.support.constraint.Placeholder
        android:id="@+id/placeholder"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent" />

</android.support.constraint.ConstraintLayout>
```

```java
        Placeholder placeholder = findViewById(R.id.placeholder);
        placeholder.setContentId(R.id.iv_target);
```

![](https://upload-images.jianshu.io/upload_images/2552605-de329c0a378ec248.gif?imageMogr2/auto-orient/strip)

此外也可以直接在布局文件中将占位符和视图 ID 绑定在一起

```xml
    <android.support.constraint.Placeholder
        android:id="@+id/placeholder"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        app:content="@id/iv_target"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent" />
```