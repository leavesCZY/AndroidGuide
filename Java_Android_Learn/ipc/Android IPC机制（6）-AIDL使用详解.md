##**一、概述**
AIDL 意思即 Android Interface Definition Language，翻译过来就是Android接口定义语言，是用于定义服务器和客户端通信接口的一种描述语言，可以拿来生成用于IPC的代码。从某种意义上说AIDL其实是一个模板，因为在使用过程中，实际起作用的并不是AIDL文件，而是据此而生成的一个IInterface的实例代码，AIDL其实是为了避免我们重复编写代码而出现的一个模板

设计AIDL这门语言的目的就是为了实现进程间通信。在Android系统中，每个进程都运行在一块独立的内存中，在其中完成自己的各项活动，与其他进程都分隔开来。可是有时候我们又有应用间进行互动的需求，比较传递数据或者任务委托等，AIDL就是为了满足这种需求而诞生的。通过AIDL，可以在一个进程中获取另一个进程的数据和调用其暴露出来的方法，从而满足进程间通信的需求

通常，暴露方法给其他应用进行调用的应用称为服务端，调用其他应用的方法的应用称为客户端，客户端通过绑定服务端的Service来进行交互

##**二、语法**
AIDL的语法十分简单，与Java语言基本保持一致，需要记住的规则有以下几点：

  1. AIDL文件以 **.aidl** 为后缀名
  2. AIDL支持的数据类型分为如下几种：
  - 八种基本数据类型：byte、char、short、int、long、float、double、boolean
  - String，CharSequence
  - 实现了Parcelable接口的数据类型
  - List 类型。List承载的数据必须是AIDL支持的类型，或者是其它声明的AIDL对象
  - Map类型。Map承载的数据必须是AIDL支持的类型，或者是其它声明的AIDL对象
  3. AIDL文件可以分为两类。一类用来声明实现了Parcelable接口的数据类型，以供其他AIDL文件使用那些非默认支持的数据类型。还有一类是用来定义接口方法，声明要暴露哪些接口给客户端调用，定向Tag就是用来标注这些方法的参数值
  4. 定向Tag。定向Tag表示在跨进程通信中数据的流向，用于标注方法的参数值，分为 in、out、inout 三种。其中 in 表示数据只能由客户端流向服务端， out 表示数据只能由服务端流向客户端，而 inout 则表示数据可在服务端与客户端之间双向流通。此外，如果AIDL方法接口的参数值类型是：基本数据类型、String、CharSequence或者其他AIDL文件定义的方法接口，那么这些参数值的定向 Tag 默认是且只能是 in，所以除了这些类型外，其他参数值都需要明确标注使用哪种定向Tag。定向Tag具体的使用差别后边会有介绍
  5. 明确导包。在AIDL文件中需要明确标明引用到的数据类型所在的包名，即使两个文件处在同个包名下

##**三、服务端编码**
这里来实际完成一个例子作为示范，需要实现的功能是：客户端通过绑定服务端的Service的方式来调用服务端的方法，获取服务端的书籍列表并向其添加书籍，实现应用间的数据共享

首先是服务端的代码
新建一个工程，包名就定义为 **com.czy.server**
首先，在应用中需要用到一个 Book 类，而 Book 类是两个应用间都需要使用到的，所以也需要在AIDL文件中声明Book类，为了避免出现类名重复导致无法创建文件的错误，这里需要先建立 Book AIDL 文件，之后再创建 Book 类

右键点击新建一个AIDL文件，命名为 Book

![](http://upload-images.jianshu.io/upload_images/2552605-ae9931fc9a51d441.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

创建完成后，系统就会默认创建一个 aidl 文件夹，文件夹下的目录结构即是工程的包名，Book.aidi 文件就在其中

Book.aidl 文件中会有一个默认方法，可以删除掉

![](http://upload-images.jianshu.io/upload_images/2552605-2b4708f924c996e5.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

此时就可以来定义Book类了，Book类只包含一个 Name 属性，并使之实现 Parcelable 接口

```java
/**
 * 作者：叶应是叶
 * 时间：2017/8/25 23:31
 * 描述：
 */
public class Book implements Parcelable {

    private String name;

    public Book(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "book name：" + name;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.name);
    }

    public void readFromParcel(Parcel dest) {
        name = dest.readString();
    }

    protected Book(Parcel in) {
        this.name = in.readString();
    }

    public static final Creator<Book> CREATOR = new Creator<Book>() {
        @Override
        public Book createFromParcel(Parcel source) {
            return new Book(source);
        }

        @Override
        public Book[] newArray(int size) {
            return new Book[size];
        }
    };
}
    
```
现在再来修改 Book.aidl 文件，将之改为声明Parcelable数据类型的AIDL文件

```java
package com.czy.server;

parcelable Book;
```

此外，根据一开始的设想，服务端需要暴露给客户端一个获取书籍列表以及一个添加书籍的方法，这两个方法首先要定义在AIDL文件中，命名为 BookController.aidl，注意这里需要明确导包

```java
package com.czy.server;
import com.czy.server.Book;

interface BookController {

    List<Book> getBookList();

    void addBookInOut(inout Book book);

}
```
上面说过，在进程间通信中真正起作用的并不是 AIDL 文件，而是系统据此而生成的文件，可以在以下目录中查看系统生成的文件。之后需要使用到当中的内部静态抽象类 Stub

创建或修改过AIDL文件后需要clean下工程，使系统及时生成我们需要的文件

![](http://upload-images.jianshu.io/upload_images/2552605-76b54cda3a1f4399.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

现在需要来创建一个 Service 供客户端远程绑定了，这里命名为 AIDLService

```java
/**
 * 作者：叶应是叶
 * 时间：2017/8/26 0:07
 * 描述：
 */
public class AIDLService extends Service {

    private final String TAG = "Server";

    private List<Book> bookList;

    public AIDLService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        bookList = new ArrayList<>();
        initData();
    }

    private void initData() {
        Book book1 = new Book("活着");
        Book book2 = new Book("或者");
        Book book3 = new Book("叶应是叶");
        Book book4 = new Book("https://github.com/leavesC");
        Book book5 = new Book("http://www.jianshu.com/u/9df45b87cfdf");
        Book book6 = new Book("http://blog.csdn.net/new_one_object");
        bookList.add(book1);
        bookList.add(book2);
        bookList.add(book3);
        bookList.add(book4);
        bookList.add(book5);
        bookList.add(book6);
    }

    private final BookController.Stub stub = new BookController.Stub() {

        @Override
        public List<Book> getBookList() throws RemoteException {
            return bookList;
        }

        @Override
        public void addBookInOut(Book book) throws RemoteException {
            if (book != null) {
                book.setName("服务器改了新书的名字 InOut");
                bookList.add(book);
            } else {
                Log.e(TAG, "接收到了一个空对象 InOut");
            }
        }

    };

    @Override
    public IBinder onBind(Intent intent) {
        return stub;
    }

}
```
可以看到， onBind 方法返回的就是 BookController.Stub 对象，实现当中定义的两个方法

最后，服务端还有一个地方需要注意，因为服务端的Service需要被客户端来远程绑定，所以客户端要能够找到这个Service，可以通过先指定包名，之后再配置Action值或者直接指定Service类名的方式来绑定Service
如果是通过指定Action值的方式来绑定Service，那还需要将Service的声明改为如下所示：

![](http://upload-images.jianshu.io/upload_images/2552605-6be64b673cdd433f.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

本例子采用配置 Action 的方案

##**四、客户端编码**
客户端需要再创建一个新的工程，包名命名为 **com.czy.client**

首先，需要把服务端的AIDL文件以及Book类复制过来，将 aidl 文件夹整个复制到和Java文件夹同个层级下，不需要改动任何代码

![](http://upload-images.jianshu.io/upload_images/2552605-4f7fca63ba90a48a.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

之后，需要创建和服务端Book类所在的相同包名来存放 Book类

![](http://upload-images.jianshu.io/upload_images/2552605-f2176c351d104faf.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

修改布局文件，添加两个按钮

```java
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:gravity="center"
    android:orientation="vertical">

    <Button
        android:id="@+id/btn_getBookList"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="获取书籍列表" />

    <Button
        android:id="@+id/btn_addBook_inOut"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="InOut 添加书籍" />

</LinearLayout>

```

```java
/**
 * 作者：叶应是叶
 * 时间：2017/8/26 0:34
 * 描述：
 */
public class MainActivity extends AppCompatActivity {

    private final String TAG = "Client";

    private BookController bookController;

    private boolean connected;

    private List<Book> bookList;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            bookController = BookController.Stub.asInterface(service);
            connected = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            connected = false;
        }
    };

    private View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btn_getBookList:
                    if (connected) {
                        try {
                            bookList = bookController.getBookList();
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                        log();
                    }
                    break;
                case R.id.btn_addBook_inOut:
                    if (connected) {
                        Book book = new Book("这是一本新书 InOut");
                        try {
                            bookController.addBookInOut(book);
                            Log.e(TAG, "向服务器以InOut方式添加了一本新书");
                            Log.e(TAG, "新书名：" + book.getName());
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.btn_getBookList).setOnClickListener(clickListener);
        findViewById(R.id.btn_addBook_inOut).setOnClickListener(clickListener);
        bindService();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (connected) {
            unbindService(serviceConnection);
        }
    }

    private void bindService() {
        Intent intent = new Intent();
        intent.setPackage("com.czy.server");
        intent.setAction("com.czy.server.action");
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void log() {
        for (Book book : bookList) {
            Log.e(TAG, book.toString());
        }
    }

}
```
两个按钮分别用于获取服务端的书籍列表和添加书籍，在添加书籍时，服务端还改变了Book对象的Name属性，据此观察客户端和服务端数据的变化情况

首先点击获取书籍列表，数据获取无误

![](http://upload-images.jianshu.io/upload_images/2552605-4bd8f66a2b50b903.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

再点击按钮添加书籍，可以看到，服务端对数据的修改也同时同步到了客户端这边

![](http://upload-images.jianshu.io/upload_images/2552605-248a010a55429fa5.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

到此为止，客户端和服务端之间的通信已经实现了，客户端获取到了服务端的数据，也向服务端传送了数据

##**五、定向Tag**
最后，我再来讲下三种定向Tag之间的差别。上边使用的是 InOut 类型，服务端对数据的改变同时也同步到了客户端，因此可以说两者之间数据是双向流动的

In 类型的表现形式是：数据只能由客户端传向服务端，服务端对数据的修改不会影响到客户端

Out类型的表现形式是：数据只能由服务端传向客户端，即使客户端向方法接口传入了一个对象，该对象中的属性值也是为空的，即不包含任何数据，服务端获取到该对象后，对该对象的任何操作，就会同步到客户端这边

这里再来实际演示一下

先修改服务器端的 BookController.aidl 文件，向之添加两个新方法

```java
package com.czy.server;
import com.czy.server.Book;

interface BookController {

    List<Book> getBookList();

    void addBookInOut(inout Book book);

    void addBookIn(in Book book);

    void addBookOut(out Book book);

}
```

则 AIDLService 类的 BookController.Stub 对象就需要修改为如下所示：

```java
private final BookController.Stub stub = new BookController.Stub() {
        @Override
        public List<Book> getBookList() throws RemoteException {
            return bookList;
        }

        @Override
        public void addBookInOut(Book book) throws RemoteException {
            if (book != null) {
                book.setName("服务器改了新书的名字 InOut");
                bookList.add(book);
            } else {
                Log.e(TAG, "接收到了一个空对象 InOut");
            }
        }

        @Override
        public void addBookIn(Book book) throws RemoteException {
            if (book != null) {
                book.setName("服务器改了新书的名字 In");
                bookList.add(book);
            } else {
                Log.e(TAG, "接收到了一个空对象 In");
            }
        }

        @Override
        public void addBookOut(Book book) throws RemoteException {
            if (book != null) {
                Log.e(TAG, "客户端传来的书的名字：" + book.getName());
                book.setName("服务器改了新书的名字 Out");
                bookList.add(book);
            } else {
                Log.e(TAG, "接收到了一个空对象 Out");
            }
        }

    };
```

同步修改客户端的 BookController.aidl 文件

向布局文件多增添两个按钮，分别用于添加不同的 定向Tag 的数据

```java
private View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btn_getBookList:
                    if (connected) {
                        try {
                            bookList = bookController.getBookList();
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                        log();
                    }
                    break;
                case R.id.btn_addBook_inOut:
                    if (connected) {
                        Book book = new Book("这是一本新书 InOut");
                        try {
                            bookController.addBookInOut(book);
                            Log.e(TAG, "向服务器以InOut方式添加了一本新书");
                            Log.e(TAG, "新书名：" + book.getName());
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                    break;
                case R.id.btn_addBook_in:
                    if (connected) {
                        Book book = new Book("这是一本新书 In");
                        try {
                            bookController.addBookIn(book);
                            Log.e(TAG, "向服务器以In方式添加了一本新书");
                            Log.e(TAG, "新书名：" + book.getName());
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                    break;
                case R.id.btn_addBook_out:
                    if (connected) {
                        Book book = new Book("这是一本新书 Out");
                        try {
                            bookController.addBookOut(book);
                            Log.e(TAG, "向服务器以Out方式添加了一本新书");
                            Log.e(TAG, "新书名：" + book.getName());
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                    break;
            }
        }
    };
```

此外，还有个地方需要修改下，即Book类。因为 Out 类型的确会使得客户端传一个不包含任何数据的对象回服务端，但该对象却不是直接就等于 null ，所以说明系统还是需要实例化 Book 类，但当前 Book 类只有一个有参构造函数，所以还需要修改 Book 类，为之添加一个无参构造函数以供系统使用

分别点击三个按钮，可以看到，服务端在获取到客户端以Out方式传来的Book对象时，的确是不包含书名这个属性值

![](http://upload-images.jianshu.io/upload_images/2552605-e3fb8c364f750848.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

关于AIDL的知识就讲到这里了，这里也提供上述代码下载，为了方便，我将 Client 端的代码保存成压缩文件放到 Server 端工程文件下

###**[Android AIDL使用详解](https://github.com/leavesC/AIDL_Demo)**