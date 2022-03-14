> 公众号：[字节数组](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/adbc507fc3704fd8955aae739a433db2~tplv-k3u1fbpfcp-zoom-1.image)
>
> 希望对你有所帮助 🤣🤣

# 一、概述

在 RxJava 中，一个实现了 `Observer` 接口的对象可以订阅一个 `Observable` 类的实例。订阅者对 `Observable` 发射的任何数据或数据序列作出响应。这种模式简化了并发操作，因为它不需要阻塞等待 `Observable` 发射数据，而是创建了一个处于待命状态的观察者哨兵，哨兵在未来某个时刻响应 `Observable` 的通知。RxJava 提供了一套异步编程的 API，并且支持链式调用，所以使用 RxJava 编写的代码的逻辑会非常简洁

RxJava 有以下三个最基本的元素：

1. 被观察者（Observable）
2. 观察者（Observer）
3. 订阅（subscribe）

创建被观察者

```java
		Observable<Integer> observable = Observable.create(new ObservableOnSubscribe<Integer>() {
            @Override
            public void subscribe(ObservableEmitter<Integer> e) {
                Log.e(TAG, "subscribe");
                Log.e(TAG, "currentThread name: " + Thread.currentThread().getName());
                e.onNext(1);
                e.onNext(2);
                e.onNext(3);
                e.onComplete();
            }
        });
```

创建观察者

```java
		Observer<Integer> observer = new Observer<Integer>() {
            @Override
            public void onSubscribe(Disposable d) {
                Log.e(TAG, "onSubscribe");
            }

            @Override
            public void onNext(Integer integer) {
                Log.e(TAG, "onNext: " + integer);
            }

            @Override
            public void onError(Throwable e) {
                Log.e(TAG, "onError: " + e.getMessage());
            }

            @Override
            public void onComplete() {
                Log.e(TAG, "onComplete");
            }
        };
```

完成观察者与被观察者之间的订阅关系

```java
	 observable.subscribe(observer);
```

也可以以链式调用的方式来完成订阅

```java
		Observable.create(new ObservableOnSubscribe<Integer>() {
            @Override
            public void subscribe(ObservableEmitter<Integer> e) {
                Log.e(TAG, "subscribe");
                Log.e(TAG, "currentThread name: " + Thread.currentThread().getName());
                e.onNext(1);
                e.onNext(2);
                e.onNext(3);
                e.onComplete();
            }
        }).subscribe(new Observer<Integer>() {
            @Override
            public void onSubscribe(Disposable d) {
                Log.e(TAG, "onSubscribe");
            }

            @Override
            public void onNext(Integer integer) {
                Log.e(TAG, "onNext: " + integer);
            }

            @Override
            public void onError(Throwable e) {
                Log.e(TAG, "onError: " + e.getMessage());
            }

            @Override
            public void onComplete() {
                Log.e(TAG, "onComplete");
            }
        });
```

最终的输出结果是一样的

```java
    onSubscribe
    subscribe
    currentThread name: main
    onNext: 1
    onNext: 2
    onNext: 3
    onComplete
```

被观察者发送的事件类型有以下几种

| 事件种类     | 作用                                                         |
| ------------ | ------------------------------------------------------------ |
| onNext()     | 发送该事件时，观察者会回调 onNext() 方法                     |
| onError()    | 发送该事件时，观察者会回调 onError() 方法，当发送该事件之后，其他事件将不会继续发送 |
| onComplete() | 发送该事件时，观察者会回调 onComplete() 方法，当发送该事件之后，其他事件将不会继续发送 |

下面来讲解 RxJava 中各种常见的操作符

# 二、创建操作符

## 1、create()

用于创建一个 `Observable`。一个正确的 `Observable` 必须尝试调用观察者的 `onCompleted` 方法或者 `onError` 方法**有且仅有一次**，而且此后不能再调用`Observable` 的任何其它方法

```java
		Observable<Integer> observable = Observable.create(new ObservableOnSubscribe<Integer>() {
            @Override
            public void subscribe(ObservableEmitter<Integer> e) {
                Log.e(TAG, "subscribe");
                Log.e(TAG, "currentThread name: " + Thread.currentThread().getName());
                e.onNext(1);
                e.onNext(2);
                e.onNext(3);
                e.onComplete();
            }
        });
```

## 2、just()

创建一个 `Observable`并发送事件，发送的事件总数不可以超出十个

```java
		Observable.just(1, 2, 3).subscribe(new Observer<Integer>() {
            @Override
            public void onSubscribe(Disposable d) {
                Log.e(TAG, "onSubscribe");
            }

            @Override
            public void onNext(Integer integer) {
                Log.e(TAG, "onNext: " + integer);
            }

            @Override
            public void onError(Throwable e) {
                Log.e(TAG, "onError: " + e.getMessage());
            }

            @Override
            public void onComplete() {
                Log.e(TAG, "onComplete");
            }
        });
```

```java
    onSubscribe
    onNext: 1
    onNext: 2
    onNext: 3
    onComplete
```

## 3、fromArray 

和 `just()` 类似，但 `fromArray` 可以传入多于十个的变量，并且可以传入一个数组

```java
	    Integer[] arrays = new Integer[]{1, 2, 3};
        Observable.fromArray(arrays).subscribe(new Observer<Integer>() {
            @Override
            public void onSubscribe(Disposable d) {
                Log.e(TAG, "onSubscribe");
            }

            @Override
            public void onNext(Integer integer) {
                Log.e(TAG, "onNext: " + integer);
            }

            @Override
            public void onError(Throwable e) {
                Log.e(TAG, "onError: " + e.getMessage());
            }

            @Override
            public void onComplete() {
                Log.e(TAG, "onComplete");
            }
        });
```

## 4、fromCallable

这里的 `Callable` 是指 `java.util.concurrent` 中的 `Callable`，`Callable` 和 `Runnable` 的用法基本一致，只是它包含一个返回值，这个结果值就是发给观察者的

```java
        Observable.fromCallable(new Callable<Integer>() {
            @Override
            public Integer call() {
                return 100;
            }
        });
```

## 5、fromFuture

这里的 `Future` 是指 `java.util.concurrent` 中的 `Future`，`Future` 的作用是增加了 `cancel()` 等方法操作 `Callable`，它可以通过 `get()` 方法来获取 `Callable` 返回的值

```java
		final FutureTask<Integer> futureTask = new FutureTask<>(new Callable<Integer>() {
            @Override
            public Integer call() {
                return 12;
            }
        });
        Observable.fromFuture(futureTask).doOnSubscribe(new Consumer<Disposable>() {
            @Override
            public void accept(Disposable disposable) {
                futureTask.run();
            }
        }).subscribe(new Consumer<Integer>() {
            @Override
            public void accept(Integer integer) {
                Log.e(TAG, "accept: " + integer);
            }
        });
```

## 6、fromIterable()

用于发送一个 `List` 集合数据给观察者

```java
	    List<Integer> integerList = new ArrayList<>();
        integerList.add(1);
        integerList.add(2);
        integerList.add(3);
        Observable.fromIterable(integerList).subscribe(new Consumer<Integer>() {
            @Override
            public void accept(Integer integer) {
                Log.e(TAG, "accept: " + integer);
            }
        });
```

## 7、defer()

`defer` 操作符会一直等待直到有观察者订阅它，然后它使用 `Observable` 工厂方法生成一个 `Observable`。它对每个观察者都这样做，因此尽管每个订阅者都以为自己订阅的是同一个 `Observable` ，实际上每个订阅者获取到的都是它们自己的单独的数据序列。在某些情况下，直到发生订阅时才生成 `Observable` 可以确保 `Observable` 包含最新的数据

```java
    //全局变量
    private Integer value = 100;

	Observable<Integer> observable = Observable.defer(new Callable<ObservableSource<? extends Integer>>() {
            @Override
            public ObservableSource<? extends Integer> call() {
                return Observable.just(value);
            }
        });
        value = 200;
        observable.subscribe(new Consumer<Integer>() {
            @Override
            public void accept(Integer integer) {
                Log.e(TAG, "accept: " + integer);
            }
        });
        value = 300;
        observable.subscribe(new Consumer<Integer>() {
            @Override
            public void accept(Integer integer) {
                Log.e(TAG, "accept: " + integer);
            }
        });
```

```java
    accept: 200
    accept: 300
```

`defer()` 操作符能使得每次订阅操作都创建被观察者，因此两次订阅操作会创建不同的被观察者对象，因此两次打印操作返回的值并不一样

## 8、timer()

延迟指定时间后会发送一个大小为 `0L` 的值给观察者

```java
       Observable.timer(2, TimeUnit.SECONDS)
           .subscribe(new Consumer<Long>() {
               @Override
               public void accept(Long aLong) {

               }
           });
```

## 9、interval()

每隔一段时间就发送一个事件，传递的值从 0 开始并不断增 1

```java
	Observable.interval(2, TimeUnit.SECONDS)
        .subscribe(new Consumer<Long>() {
            @Override
            public void accept(Long aLong) {
                Log.e(TAG, "value is: " + aLong);
            }
        });
```

## 10、intervalRange()

可以指定发送事件的开始值和数量，其他与 `interval()` 的功能一样

```java
			Observable.intervalRange(2, 3, 4, 5, TimeUnit.SECONDS)
                .subscribe(new Observer<Long>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        Log.e(TAG, "onSubscribe");
                    }

                    @Override
                    public void onNext(Long aLong) {
                        Log.e(TAG, "onNext：" + aLong);
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e(TAG, "onError: " + e.getMessage());
                    }

                    @Override
                    public void onComplete() {
                        Log.e(TAG, "onComplete");
                    }
                });
```

起始值从 2 开始递增，事件共传递三次，第一次事件在订阅后延迟 4 秒触发，之后每次延迟 5 秒

```java
10-06 10:48:40.017 17976-17976/leavesc.hello.rxjavademo E/MainActivity: onSubscribe
10-06 10:48:44.017 17976-17990/leavesc.hello.rxjavademo E/MainActivity: onNext：2
10-06 10:48:49.017 17976-17990/leavesc.hello.rxjavademo E/MainActivity: onNext：3
10-06 10:48:54.017 17976-17990/leavesc.hello.rxjavademo E/MainActivity: onNext：4
10-06 10:48:54.017 17976-17990/leavesc.hello.rxjavademo E/MainActivity: onComplete
```

## 11、range()

发送指定范围的事件序列

```java
			Observable.range(2, 5)
                .subscribe(new Consumer<Integer>() {
                    @Override
                    public void accept(Integer integer) {
                        Log.e(TAG, "values is :" + integer);
                    }
                });
```

```java
    values is :2
    values is :3
    values is :4
    values is :5
    values is :6
```

## 12、rangeLong()

作用与 `range()` 一样，只是数据类型是 `Long`

```java
       		 Observable.rangeLong((2, 5)
                .subscribe(new Consumer<Long>() {
                    @Override
                    public void accept(Long aLong) {
                        Log.e(TAG, "values is :" + aLong);
                    }
                });
```

## 13、empty() & never() & error()

`empty()` 直接发送 `onComplete()` 事件

```java
		Observable.empty().subscribe(new Observer<Object>() {
            @Override
            public void onSubscribe(Disposable d) {
                Log.e(TAG, "onSubscribe");
            }

            @Override
            public void onNext(Object object) {
                Log.e(TAG, "onNext: " + object);
            }

            @Override
            public void onError(Throwable e) {
                Log.e(TAG, "onError: " + e.getMessage());
            }

            @Override
            public void onComplete() {
                Log.e(TAG, "onComplete");
            }
        });
```

打印结果

```java
    onSubscribe
    onComplete
```

换成 `never()`

```java
onSubscribe
```

换成 `error()`

```java
Observable.error(new Throwable("Hello")).subscribe(new Observer<Object>() {
            @Override
            public void onSubscribe(Disposable d) {
                Log.e(TAG, "onSubscribe");
            }

            @Override
            public void onNext(Object object) {
                Log.e(TAG, "onNext: " + object);
            }

            @Override
            public void onError(Throwable e) {
                Log.e(TAG, "onError: " + e.getMessage());
            }

            @Override
            public void onComplete() {
                Log.e(TAG, "onComplete");
            }
        });
```

```java
	onSubscribe
    onError: Hello
```

# 三、转换操作符

## 1、map()

`map()` 用于将被观察者发送的数据类型转变成其他的类型

```java
	Observable.just(1, 2, 3)
        .map(new Function<Integer, String>() {
            @Override
            public String apply(Integer integer) {
                return "I'm " + integer;
            }
        })
        .subscribe(new Consumer<String>() {
            @Override
            public void accept(String s) {
                Log.e(TAG, s);
            }
        });
```

```java
10-06 10:53:16.364 18099-18099/leavesc.hello.rxjavademo E/MainActivity: I'm 1
10-06 10:53:16.364 18099-18099/leavesc.hello.rxjavademo E/MainActivity: I'm 2
10-06 10:53:16.364 18099-18099/leavesc.hello.rxjavademo E/MainActivity: I'm 3
```

## 2、flatMap()

用于将事件序列中的元素进行整合加工，返回一个新的被观察者

```java
        List<List<String>> listArrayList = new ArrayList<>();

        List<String> stringList = new ArrayList<>();
        for (int j = 0; j < 2; j++) {
            stringList.add("A_" + j);
        }
        listArrayList.add(stringList);

        stringList = new ArrayList<>();
        for (int j = 0; j < 2; j++) {
            stringList.add("B_" + j);
        }
        listArrayList.add(stringList);

        Observable.fromIterable(listArrayList).flatMap(new Function<List<String>, ObservableSource<String>>() {
            @Override
            public ObservableSource<String> apply(List<String> stringList1) throws Exception {
                return Observable.fromIterable(stringList1);
            }
        }).subscribe(new Consumer<String>() {
            @Override
            public void accept(String s) throws Exception {
                Log.e(TAG, "value is: " + s);
            }
        });
```

```java
10-06 11:02:47.246 18230-18230/leavesc.hello.rxjavademo E/MainActivity: value is: A_0
10-06 11:02:47.246 18230-18230/leavesc.hello.rxjavademo E/MainActivity: value is: A_1
10-06 11:02:47.246 18230-18230/leavesc.hello.rxjavademo E/MainActivity: value is: B_0
10-06 11:02:47.246 18230-18230/leavesc.hello.rxjavademo E/MainActivity: value is: B_1
```

## 3、concatMap()

`concatMap()` 和 `flatMap()` 基本一样，只不过 `concatMap()` 转发出来的事件是有序的，而 `flatMap()` 是无序的

还是用 `flatMap()`的例子来看

```java
Observable.fromIterable(listArrayList).flatMap(new Function<List<String>, ObservableSource<String>>() {
            @Override
            public ObservableSource<String> apply(List<String> stringList1) throws Exception {
                if (stringList1.get(0).startsWith("A")) {
                    return Observable.fromIterable(stringList1).delay(200, TimeUnit.MILLISECONDS);
                }
                return Observable.fromIterable(stringList1);
            }
        }).subscribe(new Consumer<String>() {
            @Override
            public void accept(String s) throws Exception {
                Log.e(TAG, "value is: " + s);
            }
        });
```

进行了一次延时操作，可以看到两次事件的发送顺序颠倒了

```java
10-06 11:07:30.753 18702-18702/leavesc.hello.rxjavademo E/MainActivity: value is: B_0
10-06 11:07:30.753 18702-18702/leavesc.hello.rxjavademo E/MainActivity: value is: B_1
10-06 11:07:30.953 18702-18716/leavesc.hello.rxjavademo E/MainActivity: value is: A_0
10-06 11:07:30.953 18702-18716/leavesc.hello.rxjavademo E/MainActivity: value is: A_1
```

使用 `concatMap()` 则顺序将保持一致

## 4、buffer()

从需要发送的事件当中获取指定数量的事件，并将这些事件放到缓冲区当中一并发出。`buffer` 有两个参数，参数一`count`用于指点缓冲区大小，参数二 `skip`用指定当缓冲区满了时，发送下一次事件序列的时候要跳过多少元素

```java
			Observable.just(1, 2, 3, 4, 5, 6)
                .buffer(2, 2)
                .subscribe(new Observer<List<Integer>>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        Log.e(TAG, "onSubscribe");
                    }

                    @Override
                    public void onNext(List<Integer> integers) {
                        Log.e(TAG, "缓冲区大小： " + integers.size());
                        for (Integer i : integers) {
                            Log.e(TAG, "元素： " + i);
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e(TAG, "onError: " + e.getMessage());
                    }

                    @Override
                    public void onComplete() {
                        Log.e(TAG, "onComplete");
                    }
                });
```

```java
 onSubscribe
 缓冲区大小： 2
 元素： 1
 元素： 2
 缓冲区大小： 2
 元素： 3
 元素： 4
 缓冲区大小： 2
 元素： 5
 元素： 6
 onComplete
```

## 5、groupBy()

用于将数据进行分组，每个分组都会返回一个被观察者。`groupBy()` 方法的返回值用于指定分组名，每返回一个新值就代表会创建一个分组

```java
 			Observable.just(1, 2, 3, 4, 5, 6, 7)
                .groupBy(new Function<Integer, String>() {
                    @Override
                    public String apply(Integer integer) {
                        if (integer < 4) {
                            return "hello";
                        }
                        return "hi";
                    }
                })
                .subscribe(new Observer<GroupedObservable<String, Integer>>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        Log.e(TAG, "onSubscribe");
                    }

                    @Override
                    public void onNext(final GroupedObservable<String, Integer> observable) {
                        observable.subscribe(new Observer<Integer>() {
                            @Override
                            public void onSubscribe(Disposable d) {
                                Log.e(TAG, "GroupedObservable onSubscribe");
                            }

                            @Override
                            public void onNext(Integer integer) {
                                Log.e(TAG, "GroupedObservable onNext key :" + observable.getKey());
                                Log.e(TAG, "GroupedObservable onNext value :" + integer);
                            }

                            @Override
                            public void onError(Throwable e) {
                                Log.e(TAG, "GroupedObservable onError");
                            }

                            @Override
                            public void onComplete() {
                                Log.e(TAG, "GroupedObservable onComplete");
                            }
                        });
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e(TAG, "onError");
                    }

                    @Override
                    public void onComplete() {
                        Log.e(TAG, "onComplete");
                    }
                });
```

```java
10-06 11:16:35.616 19015-19015/? E/MainActivity: onSubscribe
10-06 11:16:35.616 19015-19015/? E/MainActivity: GroupedObservable onSubscribe
10-06 11:16:35.616 19015-19015/? E/MainActivity: GroupedObservable onNext key :hello
10-06 11:16:35.616 19015-19015/? E/MainActivity: GroupedObservable onNext value :1
10-06 11:16:35.616 19015-19015/? E/MainActivity: GroupedObservable onNext key :hello
10-06 11:16:35.616 19015-19015/? E/MainActivity: GroupedObservable onNext value :2
10-06 11:16:35.616 19015-19015/? E/MainActivity: GroupedObservable onNext key :hello
10-06 11:16:35.616 19015-19015/? E/MainActivity: GroupedObservable onNext value :3
10-06 11:16:35.616 19015-19015/? E/MainActivity: GroupedObservable onSubscribe
10-06 11:16:35.616 19015-19015/? E/MainActivity: GroupedObservable onNext key :hi
10-06 11:16:35.616 19015-19015/? E/MainActivity: GroupedObservable onNext value :4
10-06 11:16:35.616 19015-19015/? E/MainActivity: GroupedObservable onNext key :hi
10-06 11:16:35.616 19015-19015/? E/MainActivity: GroupedObservable onNext value :5
10-06 11:16:35.616 19015-19015/? E/MainActivity: GroupedObservable onNext key :hi
10-06 11:16:35.616 19015-19015/? E/MainActivity: GroupedObservable onNext value :6
10-06 11:16:35.616 19015-19015/? E/MainActivity: GroupedObservable onNext key :hi
10-06 11:16:35.616 19015-19015/? E/MainActivity: GroupedObservable onNext value :7
10-06 11:16:35.616 19015-19015/? E/MainActivity: GroupedObservable onComplete
10-06 11:16:35.616 19015-19015/? E/MainActivity: GroupedObservable onComplete
10-06 11:16:35.616 19015-19015/? E/MainActivity: onComplete
```

## 6、scan()

`scan()` 操作符对原始 `Observable` 发射的第一条数据应用一个函数，然后将那个函数的结果作为自己的第一项数据发射。它将函数的结果同第二项数据一起填充给这个函数来产生它自己的第二项数据。它持续进行这个过程来产生剩余的数据序列

```java
		Observable.just(1, 5, 8, 12).scan(new BiFunction<Integer, Integer, Integer>() {
            @Override
            public Integer apply(Integer integer, Integer integer2) {
                Log.e(TAG, "integer : " + integer);
                Log.e(TAG, "integer2 : " + integer2);
                return integer + integer2;
            }
        }).subscribe(new Consumer<Integer>() {
            @Override
            public void accept(Integer integer) {
                Log.e(TAG, "accept : " + integer);
            }
        });
```

```java
10-06 11:25:19.389 19158-19158/leavesc.hello.rxjavademo E/MainActivity: accept : 1
10-06 11:25:19.389 19158-19158/leavesc.hello.rxjavademo E/MainActivity: integer : 1
10-06 11:25:19.389 19158-19158/leavesc.hello.rxjavademo E/MainActivity: integer2 : 5
10-06 11:25:19.399 19158-19158/leavesc.hello.rxjavademo E/MainActivity: accept : 6
10-06 11:25:19.399 19158-19158/leavesc.hello.rxjavademo E/MainActivity: integer : 6
10-06 11:25:19.399 19158-19158/leavesc.hello.rxjavademo E/MainActivity: integer2 : 8
10-06 11:25:19.399 19158-19158/leavesc.hello.rxjavademo E/MainActivity: accept : 14
10-06 11:25:19.399 19158-19158/leavesc.hello.rxjavademo E/MainActivity: integer : 14
10-06 11:25:19.409 19158-19158/leavesc.hello.rxjavademo E/MainActivity: integer2 : 12
10-06 11:25:19.409 19158-19158/leavesc.hello.rxjavademo E/MainActivity: accept : 26
```

# 四、组合操作符

## 1、concat() & concatArray()

用于将多个观察者组合在一起，然后按照参数的传入顺序发送事件，`concat()` 最多只可以发送4个事件

```java
		Observable.concat(Observable.just(1, 2),
                Observable.just(3, 4),
                Observable.just(5, 6),
                Observable.just(7, 8)).subscribe(new Consumer<Integer>() {
            @Override
            public void accept(Integer integer) throws Exception {
                Log.e(TAG, "accept: " + integer);
            }
        });
```

```java
accept: 1
accept: 2
accept: 3
accept: 4
accept: 5
accept: 6
accept: 7
accept: 8
```

`concatArray()` 作用与 `concat()` 作用一样，不过前者可以发送多于 4 个的被观察者

## 2、merge() & mergeArray()

这个方法与 `concat()` 作用基本一样，只是 `concat()` 是串行发送事件，而 `merge()` 并行发送事件

```java
	Observable.merge(Observable.interval(1, TimeUnit.SECONDS).map(new Function<Long, String>() {
                    @Override
                    public String apply(Long aLong) {
                        return "Test_A_" + aLong;
                    }
                }),
                Observable.interval(1, TimeUnit.SECONDS).map(new Function<Long, String>() {
                    @Override
                    public String apply(Long aLong) {
                        return "Test_B_" + aLong;
                    }
                })).subscribe(new Consumer<String>() {
            @Override
            public void accept(String s) {
                Log.e(TAG, "accept: " + s);
            }
        });
```

```java
Test_A_0
Test_B_0
Test_A_1
Test_B_1
Test_A_2
Test_B_2
Test_B_3
Test_A_3
Test_A_4
Test_B_4
Test_A_5
Test_B_5
```

`mergeArray()` 可以发送 4 个以上的被观察者

## 3、concatArrayDelayError()  &  mergeArrayDelayError()

在 `concatArray()` 和 `mergeArray()` 两个方法当中，如果其中有一个被观察者发送了一个 `Error` 事件，那么就会停止发送事件，如果想 `onError()` 事件延迟到所有被观察者都发送完事件后再执行的话，可以使用  `concatArrayDelayError()` 和 `mergeArrayDelayError()`

首先使用 `concatArray()` 来验证其发送 `onError()` 事件是否会中断其他被观察者的发送事件

```java
Observable.concatArray(Observable.create(new ObservableOnSubscribe<Integer>() {
            @Override
            public void subscribe(ObservableEmitter<Integer> emitter) {
                emitter.onNext(1);
                emitter.onNext(2);
                emitter.onError(new Exception("Normal Exception"));
            }
        }), Observable.just(30, 40, 50)).subscribe(new Observer<Integer>() {
            @Override
            public void onSubscribe(Disposable d) {

            }

            @Override
            public void onNext(Integer integer) {
                Log.e(TAG, "onNext: " + integer);
            }

            @Override
            public void onError(Throwable e) {
                Log.e(TAG, "onError: " + e.getMessage());
            }

            @Override
            public void onComplete() {

            }
        });
```

```java
onNext: 1
onNext: 2
onError: Normal Exception
```

从结果可以知道，确实中断了，现在换用 `concatArrayDelayError()`

```java
10-06 04:00:04.935 6514-6514/? E/MainActivity: onNext: 1
10-06 04:00:04.935 6514-6514/? E/MainActivity: onNext: 2
10-06 04:00:04.935 6514-6514/? E/MainActivity: onNext: 30
10-06 04:00:04.935 6514-6514/? E/MainActivity: onNext: 40
10-06 04:00:04.935 6514-6514/? E/MainActivity: onNext: 50
10-06 04:00:04.935 6514-6514/? E/MainActivity: onError: Normal Exception
```

从结果可以看到，`onError` 事件是在所有被观察者发送完事件才发送的

## 4、zip()

`zip()` 操作符返回一个 `Obversable`，它使用这个函数按顺序结合两个或多个 Observables 发射的数据项，然后它发射这个函数返回的结果。它按照严格的顺序应用这个函数。它只发射与发射数据项最少的那个 Observable 一样多的数据

```java
		Observable.zip(Observable.just(1, 2, 3, 4), Observable.just(5, 6, 7, 8, 9),
                new BiFunction<Integer, Integer, String>() {
                    @Override
                    public String apply(Integer integer, Integer integer2) throws Exception {
                        return String.valueOf(integer) + "_" + String.valueOf(integer2);
                    }
                })
                .subscribe(new Consumer<String>() {
                    @Override
                    public void accept(String s) throws Exception {
                        Log.e(TAG, "accept: " + s);
                    }
                });
```

```java
accept: 1_5
accept: 2_6
accept: 3_7
accept: 4_8
```

## 5、combineLatest() & combineLatestDelayError()

`combineLatest()` 的作用与 `zip()` 类似，`combineLatest()` 发送事件的序列是与发送的时间线有关的，当两个 `Observables` 中的任何一个发射了一个数据时，通过一个指定的函数组合每个 `Observable` 发射的最新数据，然后发射这个函数的结果

```java
Observable.zip(
               Observable.intervalRange(1, 4, 1, 1, TimeUnit.SECONDS)
                        .map(new Function<Long, String>() {
                            @Override
                            public String apply(Long aLong) {
                                String s1 = "A" + aLong;
                                Log.e(TAG, "A 发送的事件 " + s1);
                                return s1;
                            }
                        }), Observable.intervalRange(1, 4, 2, 1, TimeUnit.SECONDS)
                        .map(new Function<Long, String>() {
                            @Override
                            public String apply(Long aLong) {
                                String s1 = "B" + aLong;
                                Log.e(TAG, "B 发送的事件 " + s1);
                                return s1;
                            }
                        }),
                new BiFunction<String, String, String>() {
                    @Override
                    public String apply(String value1, String value2) throws Exception {
                        return value1 + "_" + value2;
                    }
                })
                .subscribe(new Consumer<String>() {
                    @Override
                    public void accept(String s) throws Exception {
                        Log.e(TAG, "accept: " + s);
                    }
                });
```

```java
10-06 05:17:06.337 7227-7241/leavesc.hello.rxjavademo E/MainActivity: A 发送的事件 A1
10-06 05:17:07.337 7227-7241/leavesc.hello.rxjavademo E/MainActivity: A 发送的事件 A2
10-06 05:17:07.337 7227-7242/leavesc.hello.rxjavademo E/MainActivity: B 发送的事件 B1
10-06 05:17:07.337 7227-7242/leavesc.hello.rxjavademo E/MainActivity: accept: A1_B1
10-06 05:17:08.337 7227-7241/leavesc.hello.rxjavademo E/MainActivity: A 发送的事件 A3
10-06 05:17:08.337 7227-7242/leavesc.hello.rxjavademo E/MainActivity: B 发送的事件 B2
10-06 05:17:08.337 7227-7242/leavesc.hello.rxjavademo E/MainActivity: accept: A2_B2
10-06 05:17:09.337 7227-7242/leavesc.hello.rxjavademo E/MainActivity: B 发送的事件 B3
10-06 05:17:09.337 7227-7242/leavesc.hello.rxjavademo E/MainActivity: accept: A3_B3
10-06 05:17:09.337 7227-7241/leavesc.hello.rxjavademo E/MainActivity: A 发送的事件 A4
10-06 05:17:10.337 7227-7242/leavesc.hello.rxjavademo E/MainActivity: B 发送的事件 B4
10-06 05:17:10.337 7227-7242/leavesc.hello.rxjavademo E/MainActivity: accept: A4_B4
```

当发送 A1 和 A2 事件时，B 并没有发送任何事件，所以不会触发到 `accept` 方法。当发送了 B1 事件之后，就会与 A 最新发送的事件 A2 结合成 A1_B2，之后的发射规则也以此类推

 `combineLatestDelayError()` 多了延迟发送 `onError()` 的功能

## 6、reduce()

与 `scan()` 操作符的作用类似，也是将发送数据以一定逻辑聚合起来，区别在于 `scan()` 每处理一次数据就会将事件发送给观察者，而 `reduce()` 会将所有数据聚合在一起才会发送事件给观察者

```java
Observable.just(1, 3, 5, 7).reduce(new BiFunction<Integer, Integer, Integer>() {
            @Override
            public Integer apply(Integer integer, Integer integer2) throws Exception {
                Log.e(TAG, "integer1 : " + integer);
                Log.e(TAG, "integer2 : " + integer2);
                return integer + integer2;
            }
        }).subscribe(new Consumer<Integer>() {
            @Override
            public void accept(Integer integer) throws Exception {
                Log.e(TAG, "accept : " + integer);
            }
        });
```

```java
integer1 : 1
integer2 : 3
integer1 : 4
integer2 : 5
integer1 : 9
integer2 : 7
accept : 16
```

## 7、collect()

`collect()` 与 `reduce()` 类似，但它的目的是收集原始 Observable 发射的所有数据到一个可变的数据结构

```java
Observable.just(1, 2, 3, 4)
                .collect(new Callable<ArrayList<Integer>>() {
                    @Override
                    public ArrayList<Integer> call() throws Exception {
                        return new ArrayList<>();
                    }
                }, new BiConsumer<ArrayList<Integer>, Integer>() {
                    @Override
                    public void accept(ArrayList<Integer> integers, Integer integer) throws Exception {
                        integers.add(integer);
                    }
                })
                .subscribe(new Consumer<ArrayList<Integer>>() {
                    @Override
                    public void accept(ArrayList<Integer> integers) throws Exception {
                        Log.e(TAG, "accept : " + integers);
                    }
                });
```

```java
accept : [1, 2, 3, 4]
```

## 8、startWith()  &  startWithArray()

在发送事件之前追加事件，`startWith()` 追加一个事件，`startWithArray()` 可以追加多个事件，追加的事件会先发出

```java
        Observable.just(4, 5)
                .startWithArray(2, 3)
                .startWith(1)
                .subscribe(new Consumer<Integer>() {
                    @Override
                    public void accept(Integer integer) throws Exception {
                        Log.e(TAG, "accept : " + integer);
                    }
                });
```

```java
10-06 05:38:21.081 8033-8033/leavesc.hello.rxjavademo E/MainActivity: accept : 1
10-06 05:38:21.081 8033-8033/leavesc.hello.rxjavademo E/MainActivity: accept : 2
10-06 05:38:21.081 8033-8033/leavesc.hello.rxjavademo E/MainActivity: accept : 3
10-06 05:38:21.081 8033-8033/leavesc.hello.rxjavademo E/MainActivity: accept : 4
10-06 05:38:21.081 8033-8033/leavesc.hello.rxjavademo E/MainActivity: accept : 5
```

## 9、count()

返回被观察者发送事件的数量

```java
        Observable.just(1, 2, 3)
                .count()
                .subscribe(new Consumer<Long>() {
                    @Override
                    public void accept(Long aLong) throws Exception {
                        Log.e(TAG, "aLong : " + aLong);
                    }
                });
```

```
aLong : 3
```

# 五、功能操作符

## 1、delay()

延迟一段事件再发送事件

```java
        Observable.just(1, 2, 3)
                .delay(3, TimeUnit.SECONDS)
                .subscribe(new Consumer<Integer>() {
                    @Override
                    public void accept(Integer value) throws Exception {
                        Log.e(TAG, "value : " + value);
                    }
                });
```

## 2、doOnEach()

`Observable` 发送一次事件之前都会回调这个方法

```java
Observable.just(1, 2, 3)
                .doOnEach(new Consumer<Notification<Integer>>() {
                    @Override
                    public void accept(Notification<Integer> integerNotification) throws Exception {
                        Log.e(TAG, "integerNotification value : " + integerNotification.getValue());
                    }
                })
                .subscribe(new Consumer<Integer>() {
                    @Override
                    public void accept(Integer value) throws Exception {
                        Log.e(TAG, "accept : " + value);
                    }
                });
```

```java
10-06 05:53:28.510 8645-8645/? E/MainActivity: integerNotification value : 1
10-06 05:53:28.510 8645-8645/? E/MainActivity: accept : 1
10-06 05:53:28.510 8645-8645/? E/MainActivity: integerNotification value : 2
10-06 05:53:28.510 8645-8645/? E/MainActivity: accept : 2
10-06 05:53:28.510 8645-8645/? E/MainActivity: integerNotification value : 3
10-06 05:53:28.510 8645-8645/? E/MainActivity: accept : 3
10-06 05:53:28.510 8645-8645/? E/MainActivity: integerNotification value : null
```

## 3、doOnNext()

`Observable` 发送 `onNext()` 之前都会先回调这个方法

```java
Observable.just(1, 2, 3)
                .doOnNext(new Consumer<Integer>() {
                    @Override
                    public void accept(Integer integer) throws Exception {
                        Log.e(TAG, "doOnNext accept : " + integer);
                    }
                })
                .subscribe(new Consumer<Integer>() {
                    @Override
                    public void accept(Integer value) throws Exception {
                        Log.e(TAG, "accept : " + value);
                    }
                });
```

```java
10-06 05:55:25.618 8758-8758/leavesc.hello.rxjavademo E/MainActivity: doOnNext accept : 1
10-06 05:55:25.618 8758-8758/leavesc.hello.rxjavademo E/MainActivity: accept : 1
10-06 05:55:25.618 8758-8758/leavesc.hello.rxjavademo E/MainActivity: doOnNext accept : 2
10-06 05:55:25.618 8758-8758/leavesc.hello.rxjavademo E/MainActivity: accept : 2
10-06 05:55:25.618 8758-8758/leavesc.hello.rxjavademo E/MainActivity: doOnNext accept : 3
10-06 05:55:25.618 8758-8758/leavesc.hello.rxjavademo E/MainActivity: accept : 3
```

## 4、doAfterNext()

`Observable` 发送 `onNext()` 之后都会回调这个方法

```java
Observable.just(1, 2, 3)
                .doAfterNext(new Consumer<Integer>() {
                    @Override
                    public void accept(Integer integer) throws Exception {
                        Log.e(TAG, "doOnNext accept : " + integer);
                    }
                })
                .subscribe(new Consumer<Integer>() {
                    @Override
                    public void accept(Integer value) throws Exception {
                        Log.e(TAG, "accept : " + value);
                    }
                });
```

```java
10-06 05:57:09.357 8872-8872/leavesc.hello.rxjavademo E/MainActivity: accept : 1
10-06 05:57:09.357 8872-8872/leavesc.hello.rxjavademo E/MainActivity: doOnNext accept : 1
10-06 05:57:09.357 8872-8872/leavesc.hello.rxjavademo E/MainActivity: accept : 2
10-06 05:57:09.357 8872-8872/leavesc.hello.rxjavademo E/MainActivity: doOnNext accept : 2
10-06 05:57:09.357 8872-8872/leavesc.hello.rxjavademo E/MainActivity: accept : 3
10-06 05:57:09.357 8872-8872/leavesc.hello.rxjavademo E/MainActivity: doOnNext accept : 3
```

## 5、doOnComplete()

`Observable` 调用 `onComplete()` 之前都会回调这个方法

```java
Observable.just(1, 2, 3)
                .doOnComplete(new Action() {
                    @Override
                    public void run() throws Exception {
                        Log.e(TAG, "doOnComplete run()");
                    }
                })
                .subscribe(new Consumer<Integer>() {
                    @Override
                    public void accept(Integer value) throws Exception {
                        Log.e(TAG, "accept : " + value);
                    }
                });
```

```java
10-06 06:08:43.688 8982-8982/leavesc.hello.rxjavademo E/MainActivity: accept : 1
10-06 06:08:43.688 8982-8982/leavesc.hello.rxjavademo E/MainActivity: accept : 2
10-06 06:08:43.688 8982-8982/leavesc.hello.rxjavademo E/MainActivity: accept : 3
10-06 06:08:43.688 8982-8982/leavesc.hello.rxjavademo E/MainActivity: doOnComplete run()
```

## 6、doOnError()

`Observable` 发送 `onError()` 之前都会回调这个方法

```java
Observable.create(new ObservableOnSubscribe<Integer>() {
            @Override
            public void subscribe(ObservableEmitter<Integer> emitter) throws Exception {
                emitter.onNext(1);
                emitter.onNext(2);
                emitter.onError(new Exception("Normal Exception"));
            }
        }).doOnError(new Consumer<Throwable>() {
            @Override
            public void accept(Throwable throwable) throws Exception {
                Log.e(TAG, "doOnError accept() : " + throwable.getMessage());
            }
        }).subscribe(new Observer<Integer>() {
            @Override
            public void onSubscribe(Disposable d) {

            }

            @Override
            public void onNext(Integer integer) {
                Log.e(TAG, "onNext : " + integer);
            }

            @Override
            public void onError(Throwable e) {
                Log.e(TAG, "onError : " + e.getMessage());
            }

            @Override
            public void onComplete() {

            }
        });
```

```java
10-06 06:14:17.894 9230-9230/? E/MainActivity: onNext : 1
10-06 06:14:17.894 9230-9230/? E/MainActivity: onNext : 2
10-06 06:14:17.894 9230-9230/? E/MainActivity: doOnError accept() : Normal Exception
10-06 06:14:17.894 9230-9230/? E/MainActivity: onError : Normal Exception
```

## 7、doOnSubscribe()

`Observable` 发送 `onSubscribe()` 之前会回调这个方法

## 8、doOnDispose()

当调用 `Disposable` 的 `dispose()` 之后会回调该方法

## 9、doOnLifecycle()

在回调 `onSubscribe` 之前回调该方法的第一个参数的回调方法，可以使用该回调方法决定是否取消订阅，`doOnLifecycle()` 第二个参数的回调方法的作用与 `doOnDispose()` 一样

```java
Observable.create(new ObservableOnSubscribe<Integer>() {
            @Override
            public void subscribe(ObservableEmitter<Integer> emitter) throws Exception {
                emitter.onNext(1);
                emitter.onNext(2);
                emitter.onComplete();
            }
        }).doOnLifecycle(new Consumer<Disposable>() {
            @Override
            public void accept(Disposable disposable) throws Exception {
                Log.e(TAG, "doOnLifecycle accept");
            }
        }, new Action() {
            @Override
            public void run() throws Exception {
                Log.e(TAG, "doOnLifecycle run");
            }
        }).subscribe(new Observer<Integer>() {

            private Disposable disposable;

            @Override
            public void onSubscribe(Disposable d) {
                Log.e(TAG, "onSubscribe");
                this.disposable = d;
            }

            @Override
            public void onNext(Integer integer) {
                Log.e(TAG, "onNext : " + integer);
                disposable.dispose();
            }

            @Override
            public void onError(Throwable e) {
                Log.e(TAG, "onError : " + e.getMessage());
            }

            @Override
            public void onComplete() {
                Log.e(TAG, "onComplete");
            }
        });
```

```java
10-06 06:31:45.011 9602-9602/leavesc.hello.rxjavademo E/MainActivity: doOnLifecycle accept
10-06 06:31:45.011 9602-9602/leavesc.hello.rxjavademo E/MainActivity: onSubscribe
10-06 06:31:45.011 9602-9602/leavesc.hello.rxjavademo E/MainActivity: onNext : 1
10-06 06:31:45.011 9602-9602/leavesc.hello.rxjavademo E/MainActivity: doOnLifecycle run
```

## 10、doOnTerminate() & doAfterTerminate()

`doOnTerminate` 是在 `onError` 或者 `onComplete` 发送之前回调，而 `doAfterTerminate` 则是 `onError` 或者 `onComplete` 发送之后回调

```java
Observable.create(new ObservableOnSubscribe<Integer>() {
            @Override
            public void subscribe(ObservableEmitter<Integer> emitter) throws Exception {
                emitter.onNext(1);
                emitter.onNext(2);
                emitter.onComplete();
            }
        }).doOnTerminate(new Action() {
            @Override
            public void run() throws Exception {
                Log.e(TAG, "doOnTerminate run");
            }
        }).doAfterTerminate(new Action() {
            @Override
            public void run() throws Exception {
                Log.e(TAG, "doAfterTerminate run");
            }
        }).subscribe(new Observer<Integer>() {

            @Override
            public void onSubscribe(Disposable d) {
                Log.e(TAG, "onSubscribe");
            }

            @Override
            public void onNext(Integer integer) {
                Log.e(TAG, "onNext : " + integer);
            }

            @Override
            public void onError(Throwable e) {
                Log.e(TAG, "onError : " + e.getMessage());
            }

            @Override
            public void onComplete() {
                Log.e(TAG, "onComplete");
            }
        });
```

```java
10-06 06:34:55.968 9713-9713/? E/MainActivity: onSubscribe
10-06 06:34:55.968 9713-9713/? E/MainActivity: onNext : 1
10-06 06:34:55.968 9713-9713/? E/MainActivity: onNext : 2
10-06 06:34:55.968 9713-9713/? E/MainActivity: doOnTerminate run
10-06 06:34:55.968 9713-9713/? E/MainActivity: onComplete
10-06 06:34:55.968 9713-9713/? E/MainActivity: doAfterTerminate run
```

## 11、doFinally()

在所有事件发送完毕之后回调该方法。 `doFinally()` 和 `doAfterTerminate()` 的区别在于取消订阅时，如果取消订阅，之后 `doAfterTerminate()` 就不会被回调，而 `doFinally()` 无论怎么样都会被回调，且都会在事件序列的最后

## 12、onErrorReturn()

当接受到一个 `onError()` 事件之后回调，返回的值会回调 `onNext()` 方法，并正常结束该事件序列

```java
Observable.create(new ObservableOnSubscribe<Integer>() {
            @Override
            public void subscribe(ObservableEmitter<Integer> emitter) throws Exception {
                emitter.onNext(1);
                emitter.onNext(2);
                emitter.onError(new Exception("Normal Exception"));
            }
        }).onErrorReturn(new Function<Throwable, Integer>() {
            @Override
            public Integer apply(Throwable throwable) throws Exception {
                return 7;
            }
        }).subscribe(new Observer<Integer>() {

            @Override
            public void onSubscribe(Disposable d) {
                Log.e(TAG, "onSubscribe");
            }

            @Override
            public void onNext(Integer integer) {
                Log.e(TAG, "onNext : " + integer);
            }

            @Override
            public void onError(Throwable e) {
                Log.e(TAG, "onError : " + e.getMessage());
            }

            @Override
            public void onComplete() {
                Log.e(TAG, "onComplete");
            }
        });
```

```java
10-06 06:43:13.702 9946-9946/leavesc.hello.rxjavademo E/MainActivity: onSubscribe
10-06 06:43:13.702 9946-9946/leavesc.hello.rxjavademo E/MainActivity: onNext : 1
10-06 06:43:13.702 9946-9946/leavesc.hello.rxjavademo E/MainActivity: onNext : 2
10-06 06:43:13.712 9946-9946/leavesc.hello.rxjavademo E/MainActivity: onNext : 7
10-06 06:43:13.712 9946-9946/leavesc.hello.rxjavademo E/MainActivity: onComplete
```

## 13、onErrorResumeNext()

当接收到 `onError()` 事件时，返回一个新的 `Observable`，并正常结束事件序列

```java
Observable.create(new ObservableOnSubscribe<Integer>() {
            @Override
            public void subscribe(ObservableEmitter<Integer> emitter) throws Exception {
                emitter.onNext(1);
                emitter.onNext(2);
                emitter.onError(new Exception("Normal Exception"));
            }
        }).onErrorResumeNext(new Function<Throwable, ObservableSource<? extends Integer>>() {
            @Override
            public ObservableSource<? extends Integer> apply(Throwable throwable) throws Exception {
                Log.e(TAG, "onErrorResumeNext apply: " + throwable.getMessage());
                return Observable.just(4, 5, 6);
            }
        }).subscribe(new Observer<Integer>() {

            @Override
            public void onSubscribe(Disposable d) {
                Log.e(TAG, "onSubscribe");
            }

            @Override
            public void onNext(Integer integer) {
                Log.e(TAG, "onNext : " + integer);
            }

            @Override
            public void onError(Throwable e) {
                Log.e(TAG, "onError : " + e.getMessage());
            }

            @Override
            public void onComplete() {
                Log.e(TAG, "onComplete");
            }
        });
```

```java
10-06 06:46:36.650 10243-10243/leavesc.hello.rxjavademo E/MainActivity: onSubscribe
10-06 06:46:36.650 10243-10243/leavesc.hello.rxjavademo E/MainActivity: onNext : 1
10-06 06:46:36.650 10243-10243/leavesc.hello.rxjavademo E/MainActivity: onNext : 2
10-06 06:46:36.650 10243-10243/leavesc.hello.rxjavademo E/MainActivity: onErrorResumeNext apply: Normal Exception
10-06 06:46:36.650 10243-10243/leavesc.hello.rxjavademo E/MainActivity: onNext : 4
10-06 06:46:36.650 10243-10243/leavesc.hello.rxjavademo E/MainActivity: onNext : 5
10-06 06:46:36.650 10243-10243/leavesc.hello.rxjavademo E/MainActivity: onNext : 6
10-06 06:46:36.650 10243-10243/leavesc.hello.rxjavademo E/MainActivity: onComplete
```

## 14、 onExceptionResumeNext()

与 `onErrorResumeNext()` 作用基本一致，但是这个方法只能捕捉 `Exception`，不能捕获 `Error`

```java
Observable.create(new ObservableOnSubscribe<Integer>() {
    @Override
    public void subscribe(ObservableEmitter<Integer> emitter) throws Exception {
        emitter.onNext(1);
        emitter.onNext(2);
        emitter.onError(new Exception("Normal Exception"));
    }
}).onExceptionResumeNext(new Observable<Integer>() {
    @Override
    protected void subscribeActual(Observer<? super Integer> observer) {
        Log.e(TAG, "onExceptionResumeNext subscribeActual");
        observer.onNext(3);
        observer.onComplete();
    }
}).subscribe(new Observer<Integer>() {

    @Override
    public void onSubscribe(Disposable d) {
        Log.e(TAG, "onSubscribe");
    }

    @Override
    public void onNext(Integer integer) {
        Log.e(TAG, "onNext : " + integer);
    }

    @Override
    public void onError(Throwable e) {
        Log.e(TAG, "onError : " + e.getMessage());
    }

    @Override
    public void onComplete() {
        Log.e(TAG, "onComplete");
    }
});
```

```java
10-06 06:51:49.396 10369-10369/leavesc.hello.rxjavademo E/MainActivity: onSubscribe
10-06 06:51:49.396 10369-10369/leavesc.hello.rxjavademo E/MainActivity: onNext : 1
10-06 06:51:49.396 10369-10369/leavesc.hello.rxjavademo E/MainActivity: onNext : 2
10-06 06:51:49.396 10369-10369/leavesc.hello.rxjavademo E/MainActivity: onExceptionResumeNext subscribeActual
10-06 06:51:49.396 10369-10369/leavesc.hello.rxjavademo E/MainActivity: onNext : 3
10-06 06:51:49.396 10369-10369/leavesc.hello.rxjavademo E/MainActivity: onComplete
```

将 `emitter.onError(new Exception("Normal Exception"))` 改为 `emitter.onError(new Error("Normal Exception"));`

异常将不会被捕获

```java
10-06 06:53:21.655 10479-10479/leavesc.hello.rxjavademo E/MainActivity: onSubscribe
10-06 06:53:21.655 10479-10479/leavesc.hello.rxjavademo E/MainActivity: onNext : 1
10-06 06:53:21.655 10479-10479/leavesc.hello.rxjavademo E/MainActivity: onNext : 2
10-06 06:53:21.655 10479-10479/leavesc.hello.rxjavademo E/MainActivity: onError : Normal Exception
```

## 15、retry()

如果出现错误事件，则会重新发送所有事件序列指定次数

```java
Observable.create(new ObservableOnSubscribe<Integer>() {
            @Override
            public void subscribe(ObservableEmitter<Integer> emitter) throws Exception {
                emitter.onNext(1);
                emitter.onNext(2);
                emitter.onError(new Error("Normal Exception"));
            }
        }).retry(2).subscribe(new Observer<Integer>() {

            @Override
            public void onSubscribe(Disposable d) {
                Log.e(TAG, "onSubscribe");
            }

            @Override
            public void onNext(Integer integer) {
                Log.e(TAG, "onNext : " + integer);
            }

            @Override
            public void onError(Throwable e) {
                Log.e(TAG, "onError : " + e.getMessage());
            }

            @Override
            public void onComplete() {
                Log.e(TAG, "onComplete");
            }
        });
```

```java
10-06 06:55:17.273 10591-10591/? E/MainActivity: onSubscribe
10-06 06:55:17.273 10591-10591/? E/MainActivity: onNext : 1
10-06 06:55:17.273 10591-10591/? E/MainActivity: onNext : 2
10-06 06:55:17.273 10591-10591/? E/MainActivity: onNext : 1
10-06 06:55:17.273 10591-10591/? E/MainActivity: onNext : 2
10-06 06:55:17.273 10591-10591/? E/MainActivity: onNext : 1
10-06 06:55:17.273 10591-10591/? E/MainActivity: onNext : 2
10-06 06:55:17.273 10591-10591/? E/MainActivity: onError : Normal Exception
```

## 16、retryUntil()

出现错误事件之后，可以通过此方法判断是否继续发送事件

```java
    private int index = 1;

Observable.create(new ObservableOnSubscribe<Integer>() {
            @Override
            public void subscribe(ObservableEmitter<Integer> emitter) throws Exception {
                emitter.onNext(1);
                emitter.onNext(2);
                emitter.onError(new Exception("Normal Exception"));
            }
        }).retryUntil(new BooleanSupplier() {
            @Override
            public boolean getAsBoolean() throws Exception {
                Log.e(TAG, "getAsBoolean");
                return index == 7;
            }
        }).subscribe(new Observer<Integer>() {

            @Override
            public void onSubscribe(Disposable d) {
                Log.e(TAG, "onSubscribe");
            }

            @Override
            public void onNext(Integer integer) {
                Log.e(TAG, "onNext : " + integer);
                index++;
            }

            @Override
            public void onError(Throwable e) {
                Log.e(TAG, "onError : " + e.getMessage());
            }

            @Override
            public void onComplete() {
                Log.e(TAG, "onComplete");
            }
        });
```

```java
10-06 07:19:07.675 11433-11433/leavesc.hello.rxjavademo E/MainActivity: onSubscribe
10-06 07:19:07.675 11433-11433/leavesc.hello.rxjavademo E/MainActivity: onNext : 1
10-06 07:19:07.675 11433-11433/leavesc.hello.rxjavademo E/MainActivity: onNext : 2
10-06 07:19:07.675 11433-11433/leavesc.hello.rxjavademo E/MainActivity: getAsBoolean
10-06 07:19:07.675 11433-11433/leavesc.hello.rxjavademo E/MainActivity: onNext : 1
10-06 07:19:07.675 11433-11433/leavesc.hello.rxjavademo E/MainActivity: onNext : 2
10-06 07:19:07.675 11433-11433/leavesc.hello.rxjavademo E/MainActivity: getAsBoolean
10-06 07:19:07.675 11433-11433/leavesc.hello.rxjavademo E/MainActivity: onNext : 1
10-06 07:19:07.675 11433-11433/leavesc.hello.rxjavademo E/MainActivity: onNext : 2
10-06 07:19:07.675 11433-11433/leavesc.hello.rxjavademo E/MainActivity: getAsBoolean
10-06 07:19:07.675 11433-11433/leavesc.hello.rxjavademo E/MainActivity: onError : Normal Exception
```

## 17、repeat()

以指定次数重复发送被观察者的事件

```java
Observable.create(new ObservableOnSubscribe<Integer>() {
            @Override
            public void subscribe(ObservableEmitter<Integer> emitter) throws Exception {
                emitter.onNext(1);
                emitter.onNext(2);
                emitter.onComplete();
            }
        }).repeat(2).subscribe(new Observer<Integer>() {

            @Override
            public void onSubscribe(Disposable d) {
                Log.e(TAG, "onSubscribe");
            }

            @Override
            public void onNext(Integer integer) {
                Log.e(TAG, "onNext : " + integer);
            }

            @Override
            public void onError(Throwable e) {
                Log.e(TAG, "onError : " + e.getMessage());
            }

            @Override
            public void onComplete() {
                Log.e(TAG, "onComplete");
            }
        });
```

```java
10-06 07:38:47.680 12155-12155/? E/MainActivity: onSubscribe
10-06 07:38:47.690 12155-12155/? E/MainActivity: onNext : 1
10-06 07:38:47.690 12155-12155/? E/MainActivity: onNext : 2
10-06 07:38:47.690 12155-12155/? E/MainActivity: onNext : 1
10-06 07:38:47.690 12155-12155/? E/MainActivity: onNext : 2
10-06 07:38:47.690 12155-12155/? E/MainActivity: onComplete
```

## 18、repeatWhen()

返回一个新的被观察者来决定是否重复发送事件。如果新的被观察者返回 `onComplete` 或者 `onError` 事件，则旧的被观察者不会发送事件。如果新的被观察者返回其他事件，则旧的观察者会发送事件

```java
Observable.create(new ObservableOnSubscribe<Integer>() {
            @Override
            public void subscribe(ObservableEmitter<Integer> e) throws Exception {
                e.onNext(1);
                e.onNext(2);
                e.onNext(3);
                e.onComplete();
            }
        }).repeatWhen(new Function<Observable<Object>, ObservableSource<?>>() {
            @Override
            public ObservableSource<?> apply(Observable<Object> objectObservable) throws Exception {
//                return Observable.empty();
//                return Observable.error(new Exception("Normal Exception"));
//                return Observable.just(1);
            }
        }).subscribe(new Observer<Integer>() {

            @Override
            public void onSubscribe(Disposable d) {
                Log.e(TAG, "onSubscribe");
            }

            @Override
            public void onNext(Integer integer) {
                Log.e(TAG, "onNext : " + integer);
            }

            @Override
            public void onError(Throwable e) {
                Log.e(TAG, "onError : " + e.getMessage());
            }

            @Override
            public void onComplete() {
                Log.e(TAG, "onComplete");
            }
        });
```

以上三种情况的输出结果分别是

```java
10-06 14:29:05.641 20921-20921/leavesc.hello.rxjavademo E/MainActivity: onSubscribe
10-06 14:29:05.641 20921-20921/leavesc.hello.rxjavademo E/MainActivity: onComplete
```

```java
10-06 14:29:36.150 21027-21027/? E/MainActivity: onSubscribe
10-06 14:29:36.150 21027-21027/? E/MainActivity: onError : Normal Exception
```

```java
10-06 14:30:33.220 21135-21135/leavesc.hello.rxjavademo E/MainActivity: onSubscribe
10-06 14:30:33.220 21135-21135/leavesc.hello.rxjavademo E/MainActivity: onNext : 1
10-06 14:30:33.220 21135-21135/leavesc.hello.rxjavademo E/MainActivity: onNext : 2
10-06 14:30:33.220 21135-21135/leavesc.hello.rxjavademo E/MainActivity: onNext : 3
10-06 14:30:33.220 21135-21135/leavesc.hello.rxjavademo E/MainActivity: onComplete
```

## 19、subscribeOn() & observeOn()

`subscribeOn()` 用于指定被观察者的线程，要注意的时，如果多次调用此方法，只有第一次有效

`observeOn()` 用于指定观察者的线程，每指定一次就会生效一次

```java
Observable.create(new ObservableOnSubscribe<Integer>() {
            @Override
            public void subscribe(ObservableEmitter<Integer> emitter) throws Exception {
                Log.e(TAG, "Observable Thread Name:  " + Thread.currentThread().getName());
                emitter.onNext(1);
                emitter.onNext(2);
                emitter.onComplete();
            }
        }).subscribeOn(Schedulers.newThread()).observeOn(AndroidSchedulers.mainThread()).subscribe(new Observer<Integer>() {

            @Override
            public void onSubscribe(Disposable d) {
                Log.e(TAG, "onSubscribe");
                Log.e(TAG, "Observer Thread Name:  " + Thread.currentThread().getName());
            }

            @Override
            public void onNext(Integer integer) {
                Log.e(TAG, "onNext : " + integer);
            }

            @Override
            public void onError(Throwable e) {
                Log.e(TAG, "onError : " + e.getMessage());
            }

            @Override
            public void onComplete() {
                Log.e(TAG, "onComplete");
            }
        });
```

```java
10-06 07:54:02.839 12629-12629/leavesc.hello.rxjavademo E/MainActivity: onSubscribe
10-06 07:54:02.839 12629-12629/leavesc.hello.rxjavademo E/MainActivity: Observer Thread Name:  main
10-06 07:54:02.839 12629-12643/leavesc.hello.rxjavademo E/MainActivity: Observable Thread Name:  RxNewThreadScheduler-1
10-06 07:54:02.859 12629-12629/leavesc.hello.rxjavademo E/MainActivity: onNext : 1
10-06 07:54:02.869 12629-12629/leavesc.hello.rxjavademo E/MainActivity: onNext : 2
10-06 07:54:02.869 12629-12629/leavesc.hello.rxjavademo E/MainActivity: onComplete
```

| 调度器                         | 作用                                       |
| ------------------------------ | ------------------------------------------ |
| Schedulers.computation( )      | 用于使用计算任务，如事件循环和回调处理     |
| Schedulers.immediate( )        | 当前线程                                   |
| Schedulers.io( )               | 用于 IO 密集型任务，如果异步阻塞 IO 操作。 |
| Schedulers.newThread( )        | 创建一个新的线程                           |
| AndroidSchedulers.mainThread() | Android 的 UI 线程，用于操作 UI。          |

# 六、过滤操作符

## 1、filter()

通过一定逻辑来过滤被观察者发送的事件，如果返回 `true` 则会发送事件，否则不会发送

```java
        Observable.just(1, 2, 3, 4).filter(new Predicate<Integer>() {
            @Override
            public boolean test(Integer integer) throws Exception {
                return integer % 2 == 0;
            }
        }).subscribe(new Consumer<Integer>() {
            @Override
            public void accept(Integer integer) throws Exception {
                Log.e(TAG, "accept : " + integer);
            }
        });
```

```java
10-06 07:57:48.196 12753-12753/? E/MainActivity: accept : 2
10-06 07:57:48.196 12753-12753/? E/MainActivity: accept : 4
```

## 2、ofType()

过滤不符合该类型的事件

```java
        Observable.just(1, 2, "Hi", 3, 4, "Hello").ofType(Integer.class).subscribe(new Consumer<Integer>() {
            @Override
            public void accept(Integer integer) throws Exception {
                Log.e(TAG, "accept : " + integer);
            }
        });
```

```java
10-06 07:59:41.265 12857-12857/leavesc.hello.rxjavademo E/MainActivity: accept : 1
10-06 07:59:41.265 12857-12857/leavesc.hello.rxjavademo E/MainActivity: accept : 2
10-06 07:59:41.265 12857-12857/leavesc.hello.rxjavademo E/MainActivity: accept : 3
10-06 07:59:41.265 12857-12857/leavesc.hello.rxjavademo E/MainActivity: accept : 4
```

## 3、skip()

以正序跳过指定数量的事件

```java
        Observable.just(1, 2, 3, 4).skip(2).subscribe(new Consumer<Integer>() {
            @Override
            public void accept(Integer integer) throws Exception {
                Log.e(TAG, "accept : " + integer);
            }
        });
```

```java
10-06 08:01:09.183 12971-12971/leavesc.hello.rxjavademo E/MainActivity: accept : 3
10-06 08:01:09.183 12971-12971/leavesc.hello.rxjavademo E/MainActivity: accept : 4
```

## 4、skipLast()

以反序跳过指定数量的事件

```java
        Observable.just(1, 2, 3, 4).skipLast(2).subscribe(new Consumer<Integer>() {
            @Override
            public void accept(Integer integer) throws Exception {
                Log.e(TAG, "accept : " + integer);
            }
        });
```

```java
10-06 08:02:00.753 13079-13079/leavesc.hello.rxjavademo E/MainActivity: accept : 1
10-06 08:02:00.753 13079-13079/leavesc.hello.rxjavademo E/MainActivity: accept : 2
```

## 5、distinct()

过滤事件序列中的重复事件

```java
        Observable.just(1, 2, 1, 2, 3, 4, 3).distinct().subscribe(new Consumer<Integer>() {
            @Override
            public void accept(Integer integer) throws Exception {
                Log.e(TAG, "accept : " + integer);
            }
        });
```

```java
10-06 08:03:27.402 13189-13189/leavesc.hello.rxjavademo E/MainActivity: accept : 1
10-06 08:03:27.402 13189-13189/leavesc.hello.rxjavademo E/MainActivity: accept : 2
10-06 08:03:27.402 13189-13189/leavesc.hello.rxjavademo E/MainActivity: accept : 3
10-06 08:03:27.402 13189-13189/leavesc.hello.rxjavademo E/MainActivity: accept : 4
```

## 6、distinctUntilChanged()

过滤掉连续重复的事件

```java
        Observable.just(1, 2, 2, 1, 3, 4, 3, 3).distinctUntilChanged().subscribe(new Consumer<Integer>() {
            @Override
            public void accept(Integer integer) throws Exception {
                Log.e(TAG, "accept : " + integer);
            }
        });
```

```java
10-06 08:04:44.531 13294-13294/leavesc.hello.rxjavademo E/MainActivity: accept : 1
10-06 08:04:44.541 13294-13294/leavesc.hello.rxjavademo E/MainActivity: accept : 2
10-06 08:04:44.541 13294-13294/leavesc.hello.rxjavademo E/MainActivity: accept : 1
10-06 08:04:44.541 13294-13294/leavesc.hello.rxjavademo E/MainActivity: accept : 3
10-06 08:04:44.541 13294-13294/leavesc.hello.rxjavademo E/MainActivity: accept : 4
10-06 08:04:44.541 13294-13294/leavesc.hello.rxjavademo E/MainActivity: accept : 3
```

## 7、take()

控制观察者接收事件的数量

```java
        Observable.just(1, 2, 2, 1, 3, 4, 3, 3).take(3).subscribe(new Consumer<Integer>() {
            @Override
            public void accept(Integer integer) throws Exception {
                Log.e(TAG, "accept : " + integer);
            }
        });
```

```java
10-06 08:05:43.520 13397-13397/? E/MainActivity: accept : 1
10-06 08:05:43.520 13397-13397/? E/MainActivity: accept : 2
10-06 08:05:43.520 13397-13397/? E/MainActivity: accept : 2
```

## 8、debounce()

如果两个事件发送的时间间隔小于设定的时间间隔，则前一件事件不会发送给观察者

```java
 Observable.create(new ObservableOnSubscribe<Integer>() {
            @Override
            public void subscribe(ObservableEmitter<Integer> emitter) throws Exception {
                emitter.onNext(1);
                Thread.sleep(900);
                emitter.onNext(2);
            }
        }).debounce(1, TimeUnit.SECONDS).subscribe(new Consumer<Integer>() {
            @Override
            public void accept(Integer integer) throws Exception {
                Log.e(TAG, "accept : " + integer);
            }
        });
```

```java
10-06 08:08:59.337 13509-13523/leavesc.hello.rxjavademo E/MainActivity: accept : 2
```

## 9、firstElement() && lastElement()

`firstElement()` 取事件序列的第一个元素，`lastElement()` 取事件序列的最后一个元素

```java
        Observable.just(1, 2, 3, 4, 5).firstElement().subscribe(new Consumer<Integer>() {
            @Override
            public void accept(Integer integer) throws Exception {
                Log.e(TAG, "accept : " + integer);
            }
        });
```

## 10、elementAt() & elementAtOrError()

`elementAt()` 可以指定取出事件序列中事件，但是输入的 `index` 超出事件序列的总数的话就不会触发任何调用，想触发异常信息的话就用 `elementAtOrError()` 

```java
        Observable.just(1, 2, 3, 4, 5).elementAt(5).subscribe(new Consumer<Integer>() {
            @Override
            public void accept(Integer integer) throws Exception {
                Log.e(TAG, "accept : " + integer);
            }
        });
```

以上代码不会触发任何

改用为 `elementAtOrError()`，则会抛出异常

```java
        Observable.just(1, 2, 3, 4, 5).elementAtOrError(5).subscribe(new Consumer<Integer>() {
            @Override
            public void accept(Integer integer) throws Exception {
                Log.e(TAG, "accept : " + integer);
            }
        });
```

```java
Process: leavesc.hello.rxjavademo, PID: 13948
    io.reactivex.exceptions.OnErrorNotImplementedException: The exception was not handled due to missing onError handler in the subscribe() method call. Further reading: https://github.com/ReactiveX/RxJava/wiki/Error-Handling | null
        at io.reactivex.internal.functions.Functions$OnErrorMissingConsumer.accept(Functions.java:704)
        at io.reactivex.internal.functions.Functions$OnErrorMissingConsumer.accept(Functions.java:701)
        at io.reactivex.internal.observers.ConsumerSingleObserver.onError(ConsumerSingleObserver.java:46)
        at io.reactivex.internal.operators.observable.ObservableElementAtSingle$ElementAtObserver.onComplete(ObservableElementAtSingle.java:115)
        at io.reactivex.internal.operators.observable.ObservableFromArray$FromArrayDisposable.run(ObservableFromArray.java:111)
        at io.reactivex.internal.operators.observable.ObservableFromArray.subscribeActual(ObservableFromArray.java:37)
        at io.reactivex.Observable.subscribe(Observable.java:12090)
        at io.reactivex.internal.operators.observable.ObservableElementAtSingle.subscribeActual(ObservableElementAtSingle.java:37)
        at io.reactivex.Single.subscribe(Single.java:3438)
        at io.reactivex.Single.subscribe(Single.java:3424)
```

# 七、条件操作符

## 1、all()

判断事件序列是否全部满足某个事件，如果都满足则返回 `true`，反之则返回 `false`

```java
        Observable.just(1, 2, 3, 4, 5).all(new Predicate<Integer>() {
            @Override
            public boolean test(Integer integer) throws Exception {
                return integer % 2 == 0;
            }
        }).subscribe(new Consumer<Boolean>() {
            @Override
            public void accept(Boolean aBoolean) throws Exception {
                Log.e(TAG, "accept: " + aBoolean);
            }
        });
```

```java
10-06 08:16:10.212 14043-14043/leavesc.hello.rxjavademo E/MainActivity: accept: false
```

## 2、takeWhile()

发射原始 `Observable`，直到指定的某个条件不成立的那一刻，它停止发射原始 `Observable`，并终止自己的 `Observable`

````java
Observable.just(1, 2, 3, 4, 5, 1, 2).takeWhile(new Predicate<Integer>() {
            @Override
            public boolean test(Integer integer) throws Exception {
                return integer < 4;
            }
        }).subscribe(new Consumer<Integer>() {
            @Override
            public void accept(Integer integer) throws Exception {
                Log.e(TAG, "accept: " + integer);
            }
        });
````

```java
10-06 14:03:42.110 20095-20095/leavesc.hello.rxjavademo E/MainActivity: accept: 1
10-06 14:03:42.110 20095-20095/leavesc.hello.rxjavademo E/MainActivity: accept: 2
10-06 14:03:42.110 20095-20095/leavesc.hello.rxjavademo E/MainActivity: accept: 3
```

## 3、skipWhile()

订阅原始的 `Observable`，但是忽略它的发射物，直到指定的某个条件变为 false 时才开始发射原始 Observable

```java
			Observable.just(1, 2, 4, 1, 3, 4, 5, 1, 5)
                .skipWhile(new Predicate<Integer>() {
                    @Override
                    public boolean test(Integer integer) throws Exception {
                        return integer < 3;
                    }
                })
                .subscribe(new Consumer<Integer>() {
                    @Override
                    public void accept(Integer integer) throws Exception {
                        Log.e(TAG, "integer " + integer);
                    }
                });
```

```java
10-06 13:59:40.583 19764-19764/leavesc.hello.rxjavademo E/MainActivity: integer 4
10-06 13:59:40.593 19764-19764/leavesc.hello.rxjavademo E/MainActivity: integer 1
10-06 13:59:40.593 19764-19764/leavesc.hello.rxjavademo E/MainActivity: integer 3
10-06 13:59:40.593 19764-19764/leavesc.hello.rxjavademo E/MainActivity: integer 4
10-06 13:59:40.593 19764-19764/leavesc.hello.rxjavademo E/MainActivity: integer 5
10-06 13:59:40.593 19764-19764/leavesc.hello.rxjavademo E/MainActivity: integer 1
10-06 13:59:40.593 19764-19764/leavesc.hello.rxjavademo E/MainActivity: integer 5
```

## 4、takeUntil()

用于设置一个条件，当事件满足此条件时，此事件会被发送，但之后的事件就不会被发送了

```java
Observable.just(1, 2, 4, 1, 3, 4, 5, 1, 5)
                .takeUntil(new Predicate<Integer>() {
                    @Override
                    public boolean test(Integer integer) throws Exception {
                        return integer > 3;
                    }
                })
                .subscribe(new Consumer<Integer>() {
                    @Override
                    public void accept(Integer integer) throws Exception {
                        Log.e(TAG, "integer " + integer);
                    }
                });
```

```java
10-06 08:54:24.833 17208-17208/? E/MainActivity: integer 1
10-06 08:54:24.833 17208-17208/? E/MainActivity: integer 2
10-06 08:54:24.833 17208-17208/? E/MainActivity: integer 4
```

## 5、skipUntil()

当 `skipUntil()` 中的 `Observable` 发送事件了，原始的 `Observable` 才会发送事件给观察者

```java
Observable.intervalRange(1, 6, 0, 1, TimeUnit.SECONDS)
                .skipUntil(Observable.intervalRange(10, 3, 1, 1, TimeUnit.SECONDS))
                .subscribe(new Observer<Long>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        Log.e(TAG, "onSubscribe");
                    }

                    @Override
                    public void onNext(Long along) {
                        Log.e(TAG, "onNext : " + along);
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e(TAG, "onError");
                    }

                    @Override
                    public void onComplete() {
                        Log.e(TAG, "onComplete");
                    }
                });
```

```java
10-06 08:51:16.926 16877-16877/leavesc.hello.rxjavademo E/MainActivity: onSubscribe
10-06 08:51:17.946 16877-16892/leavesc.hello.rxjavademo E/MainActivity: onNext : 2
10-06 08:51:18.936 16877-16892/leavesc.hello.rxjavademo E/MainActivity: onNext : 3
10-06 08:51:19.946 16877-16892/leavesc.hello.rxjavademo E/MainActivity: onNext : 4
10-06 08:51:20.936 16877-16892/leavesc.hello.rxjavademo E/MainActivity: onNext : 5
10-06 08:51:21.946 16877-16892/leavesc.hello.rxjavademo E/MainActivity: onNext : 6
10-06 08:51:21.946 16877-16892/leavesc.hello.rxjavademo E/MainActivity: onComplete
```

## 6、sequenceEqual()

判断两个 `Observable` 发送的事件是否相同，如果两个序列是相同的（相同的数据，相同的顺序，相同的终止状态），它就发射 true，否则发射 false

```java
        Observable.sequenceEqual(Observable.just(1, 2, 3), Observable.just(1, 2, 3))
                .subscribe(new Consumer<Boolean>() {
                    @Override
                    public void accept(Boolean aBoolean) throws Exception {
                        Log.e(TAG, "accept aBoolean : " + aBoolean);
                    }
                });
```

```java
10-06 08:46:59.369 16492-16492/leavesc.hello.rxjavademo E/MainActivity: accept aBoolean : true
```

## 7、contains()

判断事件序列中是否含有某个元素，如果有则返回 true，如果没有则返回 false

```java
        Observable.just(1, 2, 3, 4).contains(2).subscribe(new Consumer<Boolean>() {
            @Override
            public void accept(Boolean aBoolean) throws Exception {
                Log.e(TAG, "accept aBoolean : " + aBoolean);
            }
        });
```

```java
10-06 08:45:58.100 16386-16386/leavesc.hello.rxjavademo E/MainActivity: accept aBoolean : true
```

## 8、isEmpty()

判断事件序列是否为空

```java
        Observable.create(new ObservableOnSubscribe<Integer>() {
            @Override
            public void subscribe(ObservableEmitter<Integer> emitter) throws Exception {
                emitter.onComplete();
            }
        }).isEmpty().subscribe(new Consumer<Boolean>() {
            @Override
            public void accept(Boolean aBoolean) throws Exception {
                Log.e(TAG, "accept aBoolean: " + aBoolean);
            }
        });
```

```java
10-06 08:43:43.201 16278-16278/leavesc.hello.rxjavademo E/MainActivity: accept aBoolean: true
```

## 9、amb()

`amb()` 接收一个 `Observable` 集合，但是只会发送最先发送事件的 `Observable` 中的事件，不管发射的是一项数据还是一个 `onError` 或 `onCompleted` 通知，其余 `Observable` 将会被丢弃

```java
        List<Observable<Long>> list = new ArrayList<>();
        list.add(Observable.intervalRange(1, 3, 2, 1, TimeUnit.SECONDS));
        list.add(Observable.intervalRange(10, 3, 0, 1, TimeUnit.SECONDS));
        Observable.amb(list).subscribe(new Consumer<Long>() {
            @Override
            public void accept(Long aLong) throws Exception {
                Log.e(TAG, "accept: " + aLong);
            }
        });
```

```java
10-06 08:41:45.783 16053-16068/leavesc.hello.rxjavademo E/MainActivity: accept: 10
10-06 08:41:46.783 16053-16068/leavesc.hello.rxjavademo E/MainActivity: accept: 11
10-06 08:41:47.783 16053-16068/leavesc.hello.rxjavademo E/MainActivity: accept: 12
```

## 10、defaultIfEmpty()

如果 Observable 没有发射任何值，则可以利用这个方法发送一个默认值

```java
        Observable.create(new ObservableOnSubscribe<Integer>() {
            @Override
            public void subscribe(ObservableEmitter<Integer> emitter) throws Exception {
                emitter.onComplete();
            }
        }).defaultIfEmpty(100).subscribe(new Consumer<Integer>() {
            @Override
            public void accept(Integer integer) throws Exception {
                Log.e(TAG, "accept: " + integer);
            }
        });
```

```java
10-06 08:40:04.754 15945-15945/leavesc.hello.rxjavademo E/MainActivity: accept: 100
```

