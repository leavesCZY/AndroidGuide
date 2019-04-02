Android 3.0（API 11）添加了一个强大的功能就是 Fragment （片段），主要是为了给大屏幕（如平板电脑）提供更加动态和灵活的 UI 设计支持。Fragment 是能够嵌入到活动中的组件，可以将多个片段组合在一个 Activity 中来构建多窗格 UI，有自己的生命周期，并且可以有也可以没有用户界面

片段必须始终嵌入在 Activity 中，其生命周期直接受宿主 Activity 生命周期的影响。 例如，当 Activity 暂停时，其中的所有片段也会暂停；当 Activity 被销毁时，所有片段也会被销毁。 不过，当 Activity 正在运行时，可以独立操纵每个片段，如添加或移除它们。 当执行此类片段事务时，也可以将其添加到由 Activity 管理的返回栈当中

## **一、片段的生命周期**
通过扩展 Fragment 类或其子类来创建一个片段。没有用户界面的片段，充当该片段所嵌入到的 Activity 的一个 worker ，当做 Activity 的不可见工作线程

为了有效地创建片段，需要了解片段的生命周期

 - onAttach                
  在片段与 Activity 关联之后调用
 - onCreate                
  在创建片段的时候调用
 - onCreateView            
  当为片段创建布局的时间调用，必须返回片段的根布局。如果片段未提供 UI，可以返回 null
 - onActivityCreate        
  告诉片段 Activity 的 onCreate() 方法已经完成
 - onStart                 
  当片段的视图对用户可见时调用
 - onResume                
  当 Activity 进入到 Resumed 状态的时候，也就意味着活动开始运行了，此时调用本方法
 - onPause                
  当 Activity 暂停的时候，调用该方法。系统将此方法作为用户离开片段的第一个信号（但并不总是意味着此片段会被销毁）进行调用。 通常应该在此方法内确认在当前用户会话结束后仍然有效的任何更改（因为用户可能不会返回）
 - onStop                  
  当 Activity 停止的时候调用
 - onDestoryView           
  调用本方法以允许片段释放用于视图的资源
 - onDestory               
  在片段销毁之前调用，以允许片段进行最好的清理工作
 - onDetach                
  当片段与 Activity 解除关联的时候调用

![这里写图片描述](http://upload-images.jianshu.io/upload_images/2552605-82215f275d6a1794?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

片段在 onCreateView() 方法中创建一个视图并返回，方法签名如下

```java
	public View onCreateView(LayoutInflater , ViewGroup , Bundle )
```
第一个参数用来将任意的视图填充到片段中，第二个参数是要附加的片段的父视图，第三个参数是一个 Bundle ，如果它不为空，就包含了之前保存的状态信息

加载视图使用的是如下方法

```java
	View inflate(int resource, ViewGroup root, boolean attachToRoot)
```
inflate() 方法带有三个参数：

 - 想要扩展的布局的资源 ID
 - 将作为扩展布局父项的 ViewGroup
 - 指示是否应该在扩展期间将扩展布局附加至 root 的布尔值（在下面的实例代码中使用的是 false，因为系统已经将扩展布局插入 ViewGroup ，传递 true 值会在最终布局中创建一个多余的视图组）

需要注意的是，片段不应了解其视图或其他片段的任何事情。如果想要监听一个片段所发生的事件，而它有可能影响到活动或视图或其他片段，那么，不要在片段类中编写监听器。相反，可以选择触发一个新的事件作为对片段事件的响应，并且让活动来处理它

## **二、使用片段**
要在一个 Activity 中使用片段，可以选择在布局文件中使用 **Fragment** 元素来声明。在 **andorid:name** 属性中指定要在布局中实例化的 Fragment 类，并且用 **android:id** 属性指定一个标识符

每个片段都需要一个唯一的标识符，重启 Activity 时，系统可以使用该标识符来恢复片段，也可以使用该标识符来捕获片段以执行某些事务。 可以通过三种方式为片段提供 ID：

 - 为 android:id 属性提供唯一 ID
 - 为 android:tag 属性提供唯一字符串
 - 如果未给以上的两个属性提供值，系统会使用容器视图的 ID

```java
    <fragment
        android:id="@+id/fragment_select"
        android:name="com.example.zy.myapplication.SelectFragment"
        android:layout_width="match_parent"
        android:layout_height="match_parent" />
```

当系统创建 Activity 布局时，会实例化在布局中指定的每个片段，并为每个片段调用 **onCreateView()** 方法以检索每个片段的布局。系统会直接插入片段返回的 View 来替代 **< fragment >** 元素

此外，也可以通过代码在 Activity 运行期间随时将片段添加到 Activity 布局中，只需指定要将片段放入哪个 ViewGroup 中即可
在 Activity 中通过使用 **FragmentManager** 来管理片段，通过调用 **getFragmentManager()** 或者 **getSupportFragmentManager()** 来获取 FragmentManager 的实例
然后，在 FragmentManager 调用 **beginTransaction()** 方法来获取 **FragmentTransaction** 对象，从而得以执行片段事务（如添加、移除或替换片段）

```java
	FragmentManager fragmentManager = getSupportFragmentManager();
	FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
```

FragmentTransaction 类提供了方法来完成添加、删除、替换片段等操作。一旦完成了操作，调用 **FragmentTransaction.commit()** 方法来提交修改

可以使用如下方法来为活动添加一个片段

```java
	FragmentTransaction.add(int containerViewId, Fragment fragment, String tag)
```
containerViewId 参数用于指定要添加的视图的 ID，tag 参数是自定义标签，可用于以下方法中来从而来获取片段

```java
	Fragment findFragmentByTag(String tag)
```
如果不需要指定 Tag ，可以使用另一个重载方法来添加片段

```javascript
	FragmentTransaction add(int containerViewId, Fragment fragment)
```
要从活动中删除一个片段，可以调用以下方法

```java
	FragmentTransaction remove(Fragment fragment)
```
要使用另一个片段来替换当前片段，可以使用如下方法

```java
	FragmentTransaction replace(int containerViewId, Fragment fragment, String tag)
```

此外，可以使用片段为 Activity 提供后台行为，而不显示额外 UI，要想添加没有 UI 的片段，可使用如下方法

```java
	FragmentTransaction add(Fragment fragment, String tag)
```

为片段提供一个唯一的字符串标记 tag，而不是视图 ID，如果片段没有 UI，则字符串标记将是标识它的唯一方式。该方法会添加片段，但由于它并不与 Activity 布局中的视图关联，因此不会收到对 onCreateView() 的调用。因此不需要实现该方法


当完成了片段的修改后，需要提交以使修改生效

```java
	FragmentTransaction.commit()
```

## **三、管理片段的生命周期**
管理片段生命周期与管理 Activity 生命周期很相似。和 Activity 一样，片段也以三种状态存在：

 - 运行中
  片段在运行状态下的 Activity 中可见
 - 暂停
  另一个 Activity 位于前台并具有焦点，但此片段所在的 Activity 仍然可见（前台 Activity 部分透明或未覆盖整个屏幕）
 - 停止
  此时片段处于不可见状态。宿主 Activity 已停止，或片段已从 Activity 中移除但已添加到返回栈中。 停止的片段仍然处于活动状态，系统会保留所有状态和成员信息。不过，它对用户不再可见，如果 Activity 被终止，它也会被终止

与 Activity 一样，假使 Activity 的进程被终止，如果需要在重建 Activity 时恢复片段状态，开发者也可以使用 Bundle 保留片段的状态。可以在片段的 **onSaveInstanceState()** 回调期间保存状态，并可在 **onCreate()**、**onCreateView()** 或 **onActivityCreated()** 期间恢复状态

Activity 生命周期与片段生命周期之间的最显著差异在于它们在其各自返回栈中的存储方式。 默认情况下，Activity 停止时会被放入由系统管理的 Activity 返回栈。不过，仅当在移除片段的事务执行期间通过调用 addToBackStack() 显式请求保存实例时，系统才会将片段放入由宿主 Activity 管理的返回栈

在其他方面，管理片段生命周期与管理 Activity 生命周期非常相似。 因此，管理 Activity 生命周期的做法同样适用于片段，但还是需要了解 Activity 的生命周期对片段生命周期的影响

片段所在的 Activity 的生命周期会直接影响片段的生命周期，其表现为，Activity 的每次生命周期回调都会引发每个片段的类似回调。 例如，当 Activity 收到 onPause() 时，Activity 中的每个片段也会收到 onPause()

不过，片段还有几个额外的生命周期回调，用于处理与 Activity 的唯一交互，以执行构建和销毁片段 UI 等操作。 这些额外的回调方法是：

 - onAttach()
  在片段已与 Activity 关联时调用（Activity 传递到此方法内）
 - onCreateView()
  调用它可创建与片段关联的视图层次结构
 - onActivityCreated()
  在 Activity 的 onCreate() 方法已返回时调用
 - onDestroyView()
  在移除与片段关联的视图层次结构时调用
 - onDetach()
  在取消片段与 Activity 的关联时调用

从下图中可以看到 Activity 的每个连续状态如何决定片段可以收到的回调方法。 例如，当 Activity 收到其 onCreate() 回调时，Activity 中的片段只会收到 onActivityCreated() 回调。一旦 Activity 达到 Resume 状态，就可以随意向 Activity 添加片段和移除其中的片段。 因此，只有当 Activity 处于 Resume 状态时，片段的生命周期才能独立变化。当 Activity 离开 Resume 状态时，片段会在 Activity 的推动下再次经历其生命周期

![这里写图片描述](http://upload-images.jianshu.io/upload_images/2552605-c0bf8ba96fca123f?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

## **四、具体使用**
### **4.1、在 Activity 的布局文件内声明片段**
先来看下效果图

![这里写图片描述](http://upload-images.jianshu.io/upload_images/2552605-68af539a2c5f208a?imageMogr2/auto-orient/strip)

该界面一共使用了两个 Fragment ，一个片段用于列出字符串列表，一个片段用于显示图片

首先定义左边的片段。声明了一个接口 Callback 用于处理回调事件，并要求父视图必须实现该接口，否则会抛出运行时异常 RuntimeException

```java
public class SelectFragment extends Fragment {

    public interface Callback {
        void onItemSelected(int id);
    }

    private Callback mListener;

    public SelectFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final String[] names = {"Hi", "Hello", "叶应是叶"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_activated_1, names);
        View view = inflater.inflate(R.layout.fragment_select, container, false);
        ListView listView = (ListView) view.findViewById(R.id.listView);
        listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (mListener != null) {
                    mListener.onItemSelected(position);
                }
            }
        });
        listView.setAdapter(adapter);
        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof Callback) {
            mListener = (Callback) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement Callback");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

}
```

使用到的布局文件

```java
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    tools:context="com.example.zy.myapplication.SelectFragment">

    <ListView
        android:id="@+id/listView"
        android:layout_width="match_parent"
        android:layout_height="match_parent" />

</FrameLayout>

```

然后是右边的片段。提供了一个用于修改 ImageView 图片的方法

```java
public class ResultFragment extends Fragment {


    public ResultFragment() {
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_result, container, false);
    }

    public void showResult(int id) {
        int images[] = {R.drawable.hello, R.drawable.hi, R.drawable.ye};
        ImageView imageView = (ImageView) getView().findViewById(R.id.imageView);
        imageView.setImageResource(images[id]);
    }

}

```

使用到的布局文件

```java
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    tools:context="com.example.zy.myapplication.ResultFragment">

    <ImageView
        android:id="@+id/imageView"
        android:layout_width="match_parent"
        android:layout_height="match_parent" />

</FrameLayout>

```
然后在 Activity 的布局文件中声明即可。使两个片段的宽度以 1：2.5的方式占据整个屏幕空间

```java
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    android:id="@+id/activity_main"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="horizontal"
    tools:context="com.example.zy.myapplication.MainActivity">

    <fragment
        android:id="@+id/fragment_select"
        android:name="com.example.zy.myapplication.SelectFragment"
        android:layout_width="0dp"
        android:layout_height="match_parent"
        android:layout_weight="1" />

    <fragment
        android:id="@+id/fragment_result"
        android:name="com.example.zy.myapplication.ResultFragment"
        android:layout_width="0dp"
        android:layout_height="match_parent"
        android:layout_weight="2.5" />

</LinearLayout>

```
然后在 Activity 中实现接口即可

```java
public class MainActivity extends AppCompatActivity implements SelectFragment.Callback {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    public void onItemSelected(int id) {
        ResultFragment resultFragment = (ResultFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_result);
        resultFragment.showResult(id);
    }
}
```

### **4.2、通过编程方式将片段添加到某个现有 ViewGroup**
先来看效果图

![这里写图片描述](http://upload-images.jianshu.io/upload_images/2552605-0da2bcb568fab59a?imageMogr2/auto-orient/strip)

首先创建一个 Fragment 子类，在 onCreate() 方法中获取传递来的参数，并显示在 TextView 中

```java
public class BlankFragment extends Fragment {

    private String mParam;

    public BlankFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam = getArguments().getString("Key");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_blank, container, false);
        TextView textView = (TextView) view.findViewById(R.id.textView);
        textView.setText(mParam);
        return view;
    }

}
```
使用到的布局文件

```java
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    tools:context="com.example.zy.myapplication.BlankFragment">

    <TextView
        android:textSize="50sp"
        android:id="@+id/textView"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_gravity="center"
        android:textColor="#378fe7" />

</FrameLayout>

```

在主布局中插入三个按钮，FrameLayout 是用于容纳 Fragment 的 ViewGroup 

```java
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    android:id="@+id/activity_main"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="horizontal"
    tools:context="com.example.zy.myapplication.MainActivity">

    <LinearLayout
        android:layout_width="0dp"
        android:layout_height="match_parent"
        android:layout_weight="1"
        android:background="#bed2e6"
        android:orientation="vertical">

        <Button
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:onClick="replaceFragment"
            android:text="replace Fragment"
            android:textAllCaps="false" />

        <Button
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:onClick="addFragment"
            android:text="add Fragment"
            android:textAllCaps="false" />

        <Button
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:onClick="removeFragment"
            android:text="回退 Fragment"
            android:textAllCaps="false" />

    </LinearLayout>

    <FrameLayout
        android:id="@+id/fragment_content"
        android:layout_width="0dp"
        android:layout_height="match_parent"
        android:layout_weight="3" />

</LinearLayout>

```
然后在 Activity 类当中提供添加、替换、移除片段的方法。当中有一个问题需要注意，如果直接使用 replace() 方法的话，不会造成 Fragment 前后层叠；如果是使用 add() 方法，如果不 hide 前一个片段，就会造成界面前后层叠。所以需要为每个片段设置一个 tag，在需要的时候通过 tag 来获取片段，然后将之 hide

```java
public class MainActivity extends AppCompatActivity {

    // 用来标志当前Fragment
    private int tag = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void replaceFragment(View view) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        BlankFragment blankFragment = new BlankFragment();
        Bundle bundle = new Bundle();
        String param = String.valueOf(tag++);
        bundle.putString("Key", param);
        blankFragment.setArguments(bundle);
        fragmentTransaction.replace(R.id.fragment_content, blankFragment, param);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    public void addFragment(View view) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        BlankFragment blankFragment = new BlankFragment();
        Bundle bundle = new Bundle();
        String param = String.valueOf(tag++);
        bundle.putString("Key", param);
        blankFragment.setArguments(bundle);
        // 在添加新的Fragment之前需要先隐藏前一个Fragment，否则会造成前后层叠
        if (fragmentManager.findFragmentByTag(String.valueOf(tag - 2)) != null) {
            fragmentTransaction.hide(fragmentManager.findFragmentByTag(String.valueOf(tag - 2)));
        }
        fragmentTransaction.add(R.id.fragment_content, blankFragment, param);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    public void removeFragment(View view) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.popBackStack();
        tag--;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        tag--;
    }
}
```
