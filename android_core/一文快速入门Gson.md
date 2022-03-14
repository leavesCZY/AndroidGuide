> 公众号：[字节数组](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/adbc507fc3704fd8955aae739a433db2~tplv-k3u1fbpfcp-zoom-1.image)
>
> 希望对你有所帮助 🤣🤣

Json 是一种文本形式的数据交换格式，比 xml 更为轻量。Json 的解析和生成的方式很多，在 Android 平台上最常用的类库有 Gson 和 FastJson 两种，这里要介绍的是 Gson

Gson 的 GitHub 主页点击这里：[Gson](https://github.com/google/gson)

# 一、基本用法

## 1、Gson 对象

在进行序列化与反序列操作前，需要先实例化一个 `com .google.gson.Gson` 对象，获取 Gson 对象的方法有两种

```java
        //通过构造函数来获取
        Gson gson = new Gson();
        //通过 GsonBuilder 来获取，可以进行多项特殊配置
        Gson gson = new GsonBuilder().create();
```

## 2、生成  Json

利用 Gson 可以很方便地生成 Json 字符串，通过使用 `addProperty` 的四个重载方法

```java
    public static void main(String[] args) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("String", "leavesC");
        jsonObject.addProperty("Number_Integer", 23);
        jsonObject.addProperty("Number_Double", 22.9);
        jsonObject.addProperty("Boolean", true);
        jsonObject.addProperty("Char", 'c');
        System.out.println();
        System.out.println(jsonObject);
    }
```

![](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/86dce58efe1b406db415b9ed6cbbe40b~tplv-k3u1fbpfcp-zoom-1.image)

`addProperty` 方法底层调用的是 `add(String property, JsonElement value)` 方法，即将基本数据类型转化为了 **JsonElement** 对象，JsonElement 是一个抽象类，而 **JsonObject** 继承了 JsonElement ，因此我们可以通过 JsonObject 自己来构建一个 JsonElement 

```java
    public static void main(String[] args) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("String", "leavesC");
        jsonObject.addProperty("Number", 23);
        jsonObject.addProperty("Number", 22.9);
        jsonObject.addProperty("Boolean", true);
        jsonObject.addProperty("Char", 'c');

        JsonObject jsonElement = new JsonObject();
        jsonElement.addProperty("Boolean", false);
        jsonElement.addProperty("Double", 25.9);
        jsonElement.addProperty("Char", 'c');
        jsonObject.add("JsonElement", jsonElement);

        System.out.println();
        System.out.println(jsonObject);
    }
```

![](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/41f93e3921b8496eaaf6860573be42c8~tplv-k3u1fbpfcp-zoom-1.image)

## 3、Json 与 Array、List 的转化

Json 数组与字符串数组

```java
    public static void main(String[] args) {
        //Json数组 转为 字符串数组
        Gson gson = new Gson();
        String jsonArray = "[\"https://github.com/leavesCZY\",\"https://www.jianshu.com/u/9df45b87cfdf\",\"Java\",\"Kotlin\",\"Git\",\"GitHub\"]";
        String[] strings = gson.fromJson(jsonArray, String[].class);
        System.out.println("Json数组 转为 字符串数组: ");
        for (String string : strings) {
            System.out.println(string);
        }
        //字符串数组 转为 Json数组
        jsonArray = gson.toJson(jsonArray, new TypeToken<String>() {
        }.getType());
        System.out.println("\n字符串数组 转为 Json数组: ");
        System.out.println(jsonArray);
    }
```

![](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/eff362e498f84aa09b3e61eaa77a11a6~tplv-k3u1fbpfcp-zoom-1.image)

Json 数组 与 List

```java
    public static void main(String[] args) {
        //Json数组 转为 List
        Gson gson = new Gson();
        String jsonArray = "[\"https://github.com/leavesCZY\",\"https://www.jianshu.com/u/9df45b87cfdf\",\"Java\",\"Kotlin\",\"Git\",\"GitHub\"]";
        List<String> stringList = gson.fromJson(jsonArray, new TypeToken<List<String>>() {
        }.getType());
        System.out.println("\nJson数组 转为 List: ");
        for (String string : stringList) {
            System.out.println(string);
        }
        //List 转为 Json数组
        jsonArray = gson.toJson(stringList, new TypeToken<List<String>>() {
        }.getType());
        System.out.println("\nList 转为 Json数组: ");
        System.out.println(jsonArray);
    }
```

![](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/0ad6694864e94a739be74332b979523e~tplv-k3u1fbpfcp-zoom-1.image)

## 4、序列化与反序列化

Gson 也提供了 `toJson()` 和 `fromJson()` 两个方法用于转化 Model 与 Json，前者实现了序列化，后者实现了反序列化

首先，声明一个 User 类

```java
/**
 * @Author: leavesCZY
 * @Date: 2018/3/17 18:32
 * @Github：https://github.com/leavesCZY
 */
public class User {

    private String name;

    private int age;

    private boolean sex;

    public User(String name, int age, boolean sex) {
        this.name = name;
        this.age = age;
        this.sex = sex;
    }

    @Override
    public String toString() {
        return "User{" +
                "name='" + name + '\'' +
                ", age=" + age +
                ", sex=" + sex +
                '}';
    }

}
```

序列化的方法很简单，调用 gson 对象的 toJson 方法，传入要序列化的对象

```java
    public static void main(String[] args) {
        //序列化
        User user = new User("leavesC", 24, true);
        Gson gson = new Gson();
        System.out.println();
        System.out.println(gson.toJson(user));
    }
```

![](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/e192083ec56b4b38ba7634f4bb9e9ac5~tplv-k3u1fbpfcp-zoom-1.image)

反序化的方式也类似

```java
    public static void main(String[] args) {
        //反序列化
        String userJson = "{\"name\":\"leavesC\",\"age\":24,\"sex\":true}";
        Gson gson = new Gson();
        User user = gson.fromJson(userJson, User.class);
        System.out.println();
        System.out.println(user);
    }
```

# 二、属性重命名

继续使用上一节声明的 User 类，根据 User 类声明的各个属性名，移动端的开发者希望接口返回的数据格式即是如下这样的

```java
{"name":"leavesC","age":24,"sex":true}
```
如果没有和服务器端沟通好或者是 API 改版了，接口返回的数据格式可能是这样的

```java
{"Name":"leavesC","age":24,"sex":true}
```

```java
{"userName":"leavesC","age":24,"sex":true}
```

如果继续使用上一节介绍的方法，那无疑会解析出错

例如

```java
    public static void main(String[] args) {
        //反序列化
        String userJson = "{\"userName\":\"leavesC\",\"age\":24,\"sex\":true}";
        Gson gson = new Gson();
        User user = gson.fromJson(userJson, User.class);
        System.out.println();
        System.out.println(user);
    }
```
name 属性值解析不到，所以为 null

![](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/71d1516941744b9d82932b1cb681aa2a~tplv-k3u1fbpfcp-zoom-1.image)


此时为了兼顾多种格式的数据，就需要使用 **SerializedName** 注解

根据 SerializedName 的声明来看，SerializedName 包含两个属性值，一个是字符串，一个是字符串数组，而字符串数组含有默认值

```java
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface SerializedName {
    String value();

    String[] alternate() default {};
}
```

SerializedName 的作用是为了在序列化或反序列化时，指导 Gson 如果将原有的属性名和其它特殊情况下的属性名联系起来

例如，修改 User 类，为 name 声明 SerializedName 注解，注解值为 userName

```java
/**
 * @Author: leavesCZY
 * @Date: 2018/3/17 18:32
 * @Github：https://github.com/leavesCZY
 */
public class User {

    @SerializedName("userName")
    private String name;

    private int age;

    private boolean sex;

}
```

在序列时，Json 格式就会相应改变

```java
    public static void main(String[] args) {
        //序列化
        User user = new User("leavesC", 24, true);
        Gson gson = new Gson();
        System.out.println();
        System.out.println(gson.toJson(user));
    }
```

![](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/3cdb00c69827421a845406668a66d9c6~tplv-k3u1fbpfcp-zoom-1.image)

在反序列化时也一样，能够解析到正确的属性值

```java
    public static void main(String[] args) {
        //反序列化
        String userJson = "{\"userName\":\"leavesC\",\"age\":24,\"sex\":true}";
        Gson gson = new Gson();
        User user = gson.fromJson(userJson, User.class);
        System.out.println();
        System.out.println(user);
    }
```

![](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/a22237127f4d424485f7dc05d1fc6b63~tplv-k3u1fbpfcp-zoom-1.image)

还有个问题没解决，为了应对多种属性名不一致的情况，难道我们要声明多个 User 类吗？这显然是不现实的，所以还需要为 User 类设置多个备选属性名，这就需要用到 SerializedName 注解的另一个属性值 **alternate** 了。

```java
/**
 * @Author: leavesCZY
 * @Date: 2018/3/17 18:32
 * @Github：https://github.com/leavesCZY
 */
public class User {

    @SerializedName(value = "userName", alternate = {"user_name", "Name"})
    private String name;

    private int age;

    private boolean sex;

}

```

以下几种情况都能够被正确的反序列化

```java
    public static void main(String[] args) {
        //反序列化
        Gson gson = new Gson();
        String userJson = "{\"userName\":\"leavesC\",\"age\":24,\"sex\":true}";
        User user = gson.fromJson(userJson, User.class);
        System.out.println();
        System.out.println(user);

        userJson = "{\"user_name\":\"leavesC\",\"age\":24,\"sex\":true}";
        user = gson.fromJson(userJson, User.class);
        System.out.println();
        System.out.println(user);

        userJson = "{\"Name\":\"leavesC\",\"age\":24,\"sex\":true}";
        user = gson.fromJson(userJson, User.class);
        System.out.println();
        System.out.println(user);
    }
```

![](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/7a265bd984754066abad2a908280fa5b~tplv-k3u1fbpfcp-zoom-1.image)

# 三、字段过滤

有时候并不是所有的字段都需要进行系列化和反序列化，因此需要对某些字段进行排除，有四种方法可以来实现这种需求。

## 1、基于@Expose注解

Expose 注解包含两个属性值，且均声明了默认值。Expose 的含义即为“暴露”，即用于对外暴露字段，serialize 用于指定是否进行序列化，deserialize 用于指定是否进行反序列化。如果字段不声明 Expose 注解，则意味着不进行序列化和反序列化操作，相当于两个属性值均为 false 。此外，Expose 注解需要和 GsonBuilder 构建的 Gson 对象一起使用才能生效

```java
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface Expose {
    boolean serialize() default true;

    boolean deserialize() default true;
}

```

Expose 注解的注解值声明情况有四种

```java
    @Expose(serialize = true, deserialize = true)   //序列化和反序列化都生效
    @Expose(serialize = false, deserialize = true)  //序列化时不生效，反序列化时生效
    @Expose(serialize = true, deserialize = false)  //序列化时生效，反序列化时不生效
    @Expose(serialize = false, deserialize = false) //序列化和反序列化都不生效，和不写注解一样
```

现在来看个例子，修改 User 类

```java
/**
 * @Author: leavesCZY
 * @Date: 2018/3/17 18:32
 * @Github：https://github.com/leavesCZY
 */
public class User {

    @Expose(serialize = true, deserialize = true)   //序列化和反序列化都生效
    private String a;

    @Expose(serialize = false, deserialize = true)  //序列化时不生效，反序列化时生效
    private String b;

    @Expose(serialize = true, deserialize = false)  //序列化时生效，反序列化时不生效
    private String c;

    @Expose(serialize = false, deserialize = false) //序列化和反序列化都不生效，和不写注解一样
    private String d;

    private String e;

    public User(String a, String b, String c, String d, String e) {
        this.a = a;
        this.b = b;
        this.c = c;
        this.d = d;
        this.e = e;
    }

    @Override
    public String toString() {
        return "User{" +
                "a='" + a + '\'' +
                ", b='" + b + '\'' +
                ", c='" + c + '\'' +
                ", d='" + d + '\'' +
                ", e='" + e + '\'' +
                '}';
    }

}
```

按照如上的注解值，只有声明了 Expose 注解且 serialize 值为 true 的字段才能被序列化，只有声明了 Expose 注解且 deserialize 值为 true 的字段才能被反序列化

```java
    public static void main(String[] args) {
        Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
        User user = new User("A", "B", "C", "D", "E");
        System.out.println();
        System.out.println(gson.toJson(user));

        String json = "{\"a\":\"A\",\"b\":\"B\",\"c\":\"C\",\"d\":\"D\",\"e\":\"E\"}";
        user = gson.fromJson(json, User.class);
        System.out.println();
        System.out.println(user.toString());
    }
```

![](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/1db89d97907f42be91eab1637c1bcb22~tplv-k3u1fbpfcp-zoom-1.image)

## 2、基于版本

Gson 提供了 @Since 和 @Until 两个注解基于版本对字段进行过滤，@Since 和 @Until 都包含一个 Double 属性值，用于设置版本号。Since 的意思是“自……开始”，Until 的意思是“到……为止”，一样要和 GsonBuilder 配合使用

```java
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.TYPE})
public @interface Since {
    double value();
}

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.TYPE})
public @interface Until {
    double value();
}
```

当版本( GsonBuilder 设置的版本) 大于或等于 Since 属性值或小于 Until 属性值时字段会进行序列化和反序列化操作，而没有声明注解的字段都会加入序列化和反序列操作

现在来看个例子，修改 User 类

```java
/**
 * @Author: leavesCZY
 * @Date: 2018/3/17 18:32
 * @Github：https://github.com/leavesCZY
 */
public class User {

    @Since(1.4)
    private String a;

    @Since(1.6)
    private String b;

    @Since(1.8)
    private String c;

    @Until(1.6)
    private String d;

    @Until(2.0)
    private String e;

    public User(String a, String b, String c, String d, String e) {
        this.a = a;
        this.b = b;
        this.c = c;
        this.d = d;
        this.e = e;
    }

    @Override
    public String toString() {
        return "User{" +
                "a='" + a + '\'' +
                ", b='" + b + '\'' +
                ", c='" + c + '\'' +
                ", d='" + d + '\'' +
                ", e='" + e + '\'' +
                '}';
    }

}

```

```java
    public static void main(String[] args) {
        Gson gson = new GsonBuilder().setVersion(1.6).create();
        User user = new User("A", "B", "C", "D", "E");
        System.out.println();
        System.out.println(gson.toJson(user));

        String json = "{\"a\":\"A\",\"b\":\"B\",\"c\":\"C\",\"d\":\"D\",\"e\":\"E\"}";
        user = gson.fromJson(json, User.class);
        System.out.println();
        System.out.println(user.toString());
    }
```

![](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/750f3b022d2d46889bbdeba6be5fcb3d~tplv-k3u1fbpfcp-zoom-1.image)

## 3、基于访问修饰符

访问修饰符由 **java.lang.reflect.Modifier** 提供 int 类型的定义，而 GsonBuilder 对象的 `excludeFieldsWithModifiers`方法接收一个 int 类型可变参数，指定不进行序列化和反序列化操作的访问修饰符字段

看个例子

```java
/**
 * @Author: leavesCZY
 * @Date: 2018/3/17 18:32
 * @Github：https://github.com/leavesCZY
 */
public class ModifierSample {

    public String publicField = "public";

    protected String protectedField = "protected";

    private String privateField = "private";

    String defaultField = "default";

    final String finalField = "final";

    static String staticField = "static";

}
```

```java
    public static void main(String[] args) {
        Gson gson = new GsonBuilder().excludeFieldsWithModifiers(Modifier.PRIVATE, Modifier.STATIC).create();
        ModifierSample modifierSample = new ModifierSample();
        System.out.println(gson.toJson(modifierSample));
    }
```

![](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/ba1e07967c374dda91f1e10691fb6ac3~tplv-k3u1fbpfcp-zoom-1.image)

## 4、基于策略

GsonBuilder 类包含 `setExclusionStrategies(ExclusionStrategy... strategies)`方法用于传入不定长参数的策略方法，用于直接排除指定字段名或者指定字段类型

看个例子

```java
/**
 * @Author: leavesCZY
 * @Date: 2018/3/17 18:32
 * @Github：https://github.com/leavesCZY
 */
public class Strategies {

    private String stringField;

    private int intField;

    private double doubleField;

    public Strategies(String stringField, int intField, double doubleField) {
        this.stringField = stringField;
        this.intField = intField;
        this.doubleField = doubleField;
    }

    @Override
    public String toString() {
        return "Strategies{" +
                "stringField='" + stringField + '\'' +
                ", intField=" + intField +
                ", doubleField=" + doubleField +
                '}';
    }

}
```

```java
public static void main(String[] args) {
        Gson gson = new GsonBuilder().setExclusionStrategies(new ExclusionStrategy() {
            @Override
            public boolean shouldSkipField(FieldAttributes fieldAttributes) {
                //排除指定字段名
                return fieldAttributes.getName().equals("intField");
            }

            @Override
            public boolean shouldSkipClass(Class<?> aClass) {
                //排除指定字段类型
                return aClass.getName().equals(double.class.getName());
            }
        }).create();

        Strategies strategies = new Strategies("stringField", 111, 11.22);
        System.out.println();
        System.out.println(gson.toJson(strategies));

        String json = "{\"stringField\":\"stringField\",\"intField\":111,\"doubleField\":11.22}";
        strategies = gson.fromJson(json, Strategies.class);
        System.out.println();
        System.out.println(strategies);
    }
```

字段名为 "intField" 和字段类型为 double 的字段都会被排除掉

![](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/b5ede196fb3b49dc89d689b63856d043~tplv-k3u1fbpfcp-zoom-1.image)

`setExclusionStrategies` 方法在序列化和反序列化时都会生效，如果只是想指定其中一种情况下的排除策略或分别指定排除策略，可以改为使用以下两个方法

```java
addSerializationExclusionStrategy(ExclusionStrategy strategy);

addDeserializationExclusionStrategy(ExclusionStrategy strategy);
```

# 四、个性化配置

## 1、输出 null

对于 Gson 而言，在序列化时如果某个属性值为 null 的话，那么在序列化时该字段不会参与进来，如果想要显示输出该字段的话，可以通过 GsonBuilder 进行配置

```java
/**
 * @Author: leavesCZY
 * @Date: 2018/3/17 18:32
 * @Github：https://github.com/leavesCZY
 */
public class Strategies {

    private String stringField;

    private int intField;

    private double doubleField;

}
```

```java
 public static void main(String[] args) {
        Gson gson = new GsonBuilder()
                .serializeNulls() //输出null
                .create();
        Strategies strategies = new Strategies(null, 24, 22.333);
        System.out.println();
        System.out.println(gson.toJson(strategies));
 }
```

![](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/b0c33efba86344c6974cacebcee3470f~tplv-k3u1fbpfcp-zoom-1.image)

## 2、格式化输出Json

默认的序列化后的 Josn 字符串并不太直观，可以选择格式化输出

```java
    public static void main(String[] args) {
        Gson gson = new GsonBuilder()
                .serializeNulls() //输出null
                .setPrettyPrinting()//格式化输出
                .create();
        Strategies strategies = new Strategies(null, 24, 22.333);
        System.out.println();
        System.out.println(gson.toJson(strategies));
    }
```

![](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/c28fa45112934426a4b7eb1ecf16e70c~tplv-k3u1fbpfcp-zoom-1.image)

## 3、格式化时间

Gson 也可以对时间值进行格式化

```java
/**
 * @Author: leavesCZY
 * @Date: 2018/3/17 18:32
 * @Github：https://github.com/leavesCZY
 */
public class Strategies {

    private Date date;

    private Date date2;

    public Strategies(Date date, Date date2) {
        this.date = date;
        this.date2 = date2;
    }

    @Override
    public String toString() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS", Locale.CHINA);
        return "Strategies{" +
                "date=" + simpleDateFormat.format(date) +
                ", date2=" + simpleDateFormat.format(date2) +
                '}';
    }

}

```

```java
public static void main(String[] args) {
        Gson gson = new GsonBuilder()
                .setPrettyPrinting()//格式化输出
                .setDateFormat("yyyy-MM-dd HH:mm:ss:SSS")//格式化时间
                .create();
        Date date = new Date();
        Strategies strategies = new Strategies(date, new Date(date.getTime() + 1000000));
        System.out.println();
        System.out.println(gson.toJson(strategies));

        String json = "{\n" +
                "  \"date\": \"2018-03-17 19:38:50:033\",\n" +
                "  \"date2\": \"2018-03-17 19:55:30:033\"\n" +
                "}";
        System.out.println();
        System.out.println(gson.fromJson(json, Strategies.class));
    }
```

![](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/367f08a42e6244d3b539d8cfd8db7ecc~tplv-k3u1fbpfcp-zoom-1.image)

# 五、TypeAdapter

TypeAdapter 是一个泛型抽象类，用于接管某种类型的序列化和反序列化过程，包含两个抽象方法，分别用于自定义序列化和反序列化过程

```java
public abstract void write(JsonWriter var1, T var2) throws IOException;

public abstract T read(JsonReader var1) throws IOException;
```

下面看个简单的例子

```java
/**
 * @Author: leavesCZY
 * @Date: 2018/3/17 18:32
 * @Github：https://github.com/leavesCZY
 */
public class User {

    private String name;

    private int age;

    private boolean sex;

    public User() {
    }

    public User(String name, int age, boolean sex) {
        this.name = name;
        this.age = age;
        this.sex = sex;
    }
    
    @Override
    public String toString() {
        return "User{" +
                "name='" + name + '\'' +
                ", age=" + age +
                ", sex=" + sex +
                '}';
    }

}
```

定义 TypeAdapter 的子类 UserTypeAdapter 来接管 User 类的序列化和反序列化过程

这里设定当 User 类序列化时 Json 中的Key值都是大写字母开头，反序列化时支持“name”和“Name”两种不同的 Json 风格

```java
public class UserTypeAdapter extends TypeAdapter<User> {

    @Override
    public void write(JsonWriter jsonWriter, User user) throws IOException {
        //流式序列化成对象开始
        jsonWriter.beginObject();
        //将Json的Key值都指定为大写字母开头
        jsonWriter.name("Name").value(user.getName());
        jsonWriter.name("Age").value(user.getAge());
        jsonWriter.name("Sex").value(user.isSex());
        //流式序列化结束
        jsonWriter.endObject();
    }

    @Override
    public User read(JsonReader jsonReader) throws IOException {
        User user = new User();
        //流式反序列化开始
        jsonReader.beginObject();
        while (jsonReader.hasNext()) {
            switch (jsonReader.nextName()) {
                //首字母大小写均合法
                case "name":
                case "Name":
                    user.setName(jsonReader.nextString());
                    break;
                case "age":
                    user.setAge(jsonReader.nextInt());
                    break;
                case "sex":
                    user.setSex(jsonReader.nextBoolean());
                    break;
            }

        }
        //流式反序列化结束
        jsonReader.endObject();
        return user;
    }

}
```

```java
    public static void main(String[] args) {
        Gson gson = new GsonBuilder().registerTypeAdapter(User.class, new UserTypeAdapter()).create();
        User user = new User("leavesC", 24, true);
        System.out.println();
        System.out.println(gson.toJson(user));

        String json = "{\"Name\":\"leavesC\",\"age\":24,\"sex\":true}";
        user = gson.fromJson(json, User.class);
        System.out.println();
        System.out.println(user);
    }
```

可以看到 User 类按照预定义的策略来完成序列化和反序列化了

![](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/317d550269a64d46b747d68c64203f26~tplv-k3u1fbpfcp-zoom-1.image)

# 六、JsonSerializer 和 JsonDeserializer

TypeAdapter 将序列化和反序列操作都接管了过来，其实 Gson 还提供了只接管序列化过程的接口，即 JsonSerializer

看个例子

```java
    public static void main(String[] args) {
        Gson gson = new GsonBuilder().registerTypeAdapter(User.class, new JsonSerializer<User>() {
            @Override
            public JsonElement serialize(User user, Type type, JsonSerializationContext jsonSerializationContext) {
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("NameHi", user.getName());
                jsonObject.addProperty("Sex", user.isSex());
                jsonObject.addProperty("Age", user.getAge());
                return jsonObject;
            }
        }).create();
        User user = new User("leavesC", 24, true);
        System.out.println();
        System.out.println(gson.toJson(user));
    }
```

![](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/a1ec92160824423da39f6c0409e27a26~tplv-k3u1fbpfcp-zoom-1.image)

相对应的，JsonDeserializer 接口提供了反序列化的接口

```java
public static void main(String[] args) {
        Gson gson = new GsonBuilder().registerTypeAdapter(User.class, new JsonDeserializer<User>() {
            @Override
            public User deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
                JsonObject jsonObject = jsonElement.getAsJsonObject();
                String name = null;
                //同时支持 userName 和 name 两种情况
                if (jsonObject.has("userName")) {
                    name = jsonObject.get("userName").getAsString();
                } else if (jsonObject.has("name")) {
                    name = jsonObject.get("name").getAsString();
                }
                int age = jsonObject.get("age").getAsInt();
                boolean sex = jsonObject.get("sex").getAsBoolean();
                return new User(name, age, sex);
            }
        }).create();
        String json = "{\"userName\":\"leavesC\",\"sex\":true,\"age\":24}";
        User user = gson.fromJson(json, User.class);
        System.out.println();
        System.out.println(user);

        json = "{\"name\":\"leavesC\",\"sex\":true,\"age\":24}";
        user = gson.fromJson(json, User.class);
        System.out.println();
        System.out.println(user);
    }
```

![](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/b9adbf6fa0b94ee4b0f2a32e26925174~tplv-k3u1fbpfcp-zoom-1.image)

这里有个比较麻烦的地方，那就是在使用 **TypeAdapter 、JsonSerializer** 和 **JsonDeserializer** 时，总需要调用 **registerTypeAdapter** 方法进行注册，那有没有更简单的注册方法呢？
有的，Gosn 还提供了另一个注解 **@JsonAdapter** 用于进行简单的声明

类似于这样，声明了 User 类的序列化或反序列化操作由 UserTypeAdapter 完成，注解的优先级高于 **registerTypeAdapter** 方法

```java
@JsonAdapter(UserTypeAdapter.class)
public class User {

}
```

# 七、TypeAdapterFactory

TypeAdapterFactory 是用于创建 TypeAdapter 的工厂类，通过参数 TypeToken 来查找确定对应的 TypeAdapter，如果没有就返回 null 并由 Gson 默认的处理方法来进行序列化和反序列化操作，否则就由用户预定义的 TypeAdapter 来进行处理

```java
        Gson gson = new GsonBuilder().registerTypeAdapterFactory(new TypeAdapterFactory() {
            @Override
            public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken) {
                //如果 Gson 需要与 User 类相关的 TypeAdapter ，则返回我们自己定义的 TypeAdapter 对象
                if (typeToken.getType().getTypeName().equals(User.class.getTypeName())) {
                    return (TypeAdapter<T>) new UserTypeAdapter();
                }
                //找不到则返回null
                return null;
            }
        }).create();
```


# 八、结语

这一篇文章好像写得太长了一点？Gson 的知识点介绍到这里也差不多了，以后如果还发现新内容的话我会继续补充，现在就先这样啦

我的 GitHub： [leavesC](https://github.com/leavesCZY)  -> https://github.com/leavesCZY