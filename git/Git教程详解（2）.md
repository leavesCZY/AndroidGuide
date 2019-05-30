## 一、基础操作

这里需要先来配置一下**Git**的默认编辑器，在修改工程版本的注释和描述时需要用到
我的电脑是Windows系统，想要设置的编辑器是Notepad++
在目录`C:\Users\用户名`打开**.gitconfig**文件，向之添加下面一句配置口令，将文件路径修改为自己电脑对应的程序安装路径即可

```java
[core]
	editor = 'C:/Software/Notepad++/notepad++.exe' -multiInst -notabbar -nosession -noPlugin
```

---

还是先在D盘初始化一个仓库test，当前仓库为空

![](http://upload-images.jianshu.io/upload_images/2552605-936e90f1afcbc7ac?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

手动创建一个1.txt文件，将之添加到仓库中

```java
git add 1.txt
git commit -m '第一个版本，添加了1.txt文件'
```

![](http://upload-images.jianshu.io/upload_images/2552605-fa4665b7f74ebff3?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

再手动创建一个2.txt，将之添加到仓库中
```java
git add 2.txt
git commit -m '第二个版本，添加了2.txt文件'
```

![](http://upload-images.jianshu.io/upload_images/2552605-8519f4720b217f84?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

---

之前说过可以用

```java
git log
```

来查看所有版本信息，其实该命令也可以用来查看指定目录或者文件的日志
查看1.txt的日志信息

```java
git log 1.txt
```

![](http://upload-images.jianshu.io/upload_images/2552605-4df867cbc4550964?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

可以看到1.txt文件只保存有第一个版本时的日志信息，因为在第二个版本时它并没有改动

再来查看2.txt文件的日志信息

![](http://upload-images.jianshu.io/upload_images/2552605-cd5fa68520bc2004?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

同理，2.txt文件则只有第二个版本时的信息

---

前边说过

```java
git diff
```
命令可以查看文件的前后变化，准备来说，该命令是可以查看工作区、暂存区、最新提交之间的差别

向1.txt文件手动添加一行内容：111111111111，查看变化

![](http://upload-images.jianshu.io/upload_images/2552605-42a166e1f75a1614?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

因为此时1.txt文件还未提交到暂存区中，所以该命令查看的是工作区与最新提交之间状态的差别
“+”号表示的是添加的内容
“-”号表示的是删除的内容（这里并没有标示出来，因为只增添了内容）

先将1.txt文件添加到暂存区，再来查看变化

![](http://upload-images.jianshu.io/upload_images/2552605-3ec72a1ae7c24035?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

可以看到并没有提示文件差异，因为此时对比的是工作区与暂存区之间的差别，而此时文件内容是一样的

如果想要再查看工作区与最新提交之间差别，可以使用

```java
git diff head
```
![](http://upload-images.jianshu.io/upload_images/2552605-4277988cd6017b7d?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

最新提交的内容为空，可以看到对比最新提交还是多出了一行内容

将1.txt文件提交到仓库，并查看版本信息

![](http://upload-images.jianshu.io/upload_images/2552605-3e3e1e3ac3ddbcc3?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

## 二、分支操作
master是git默认创建的分支，在进行并行开发的过程中，往往就需要创建多个分支，让多人同时进行开发
不同的分支拥有不同的代码状态，同时进行完全不同的作业，等作业完成后就可以将分支合并

先创建并切换到feature-A分支

```java
git checkout -b feature-A
```

然后查看当前所有分支

```java
git branch
```

这两条命令上篇博文也讲到了

![](http://upload-images.jianshu.io/upload_images/2552605-73062cd111ea3655?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

可以使用以下命令切换回上一个分支，即master

```java
git checkout -
```

![](http://upload-images.jianshu.io/upload_images/2552605-3b0dbaef07045ed4?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

再切换回feature-A分支
当前1.txt的内容应该只有一行，再增添一行内容，现在完整的内容应该是：

```java
111111111111
222222222222
```

将1.txt文件提交到仓库

![](http://upload-images.jianshu.io/upload_images/2552605-17159f4934bf0946?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

再切换回master分支，查看1.txt文件内容

```java
git checkout master

cat 1.txt
```

![](http://upload-images.jianshu.io/upload_images/2552605-2f0fc5094a27fbec?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

可以看到feature-A分支下的修改对master下的文件并没有影响

再向1.txt文件添加一行内容，此时应为：

```java
111111111111
333333333333
```
再提交文件

![](http://upload-images.jianshu.io/upload_images/2552605-3aed8ca267c6e865?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

尝试在master分支下合并两个分支的内容

```java
git merge feature-A
```

![](http://upload-images.jianshu.io/upload_images/2552605-cd03fe6b0a4e7783?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

可以看到提示说：**自动合并失败，请解决冲突后再提交**
因为在两个分支下，1.txt文件的第二行都有内容，且内容不同，git不知道该如何合并
而且，此时打开1.txt文件，可以看到内容变成了

```java
111111111111
<<<<<<< HEAD
333333333333
=======
222222222222
>>>>>>> feature-A

```

提示了冲突的分支和冲突内容，需要我们手动修改该文件内容，然后再提交
将文件内容修改为

```java
111111111111
333333333333
222222222222

```
向仓库提交文件

![](http://upload-images.jianshu.io/upload_images/2552605-08967c6109bc154c?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

有时，采用图表形式输出提交日志看起来可能会比较直观
feature-A分支内容的改变以及合并操作也一目了然

```java
git log --graph
```

![](http://upload-images.jianshu.io/upload_images/2552605-4d96dfdd7f7c299b?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

## 三、修改操作
前边的版本号也比较乱了，这里在**Git**文件夹下再来重新建立一个新的仓库
然后手动建立一个1.txt文件，文本内容为，将之添加到仓库中

```java
init object git
```

![](http://upload-images.jianshu.io/upload_images/2552605-02bd3116c4be1163?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

此时，提交时的描述内容是“第一个版本，添加了1.txt文件”，如果想要修改它，可以使用如下命令

```java
git commit --amend
```
此时，Notepad++就会被启动，这样就可以来修改其描述内容了，以“#”符号注释的内容可以不用理会

![](http://upload-images.jianshu.io/upload_images/2552605-d8c916e20f464a09?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

将描述内容修改为

![](http://upload-images.jianshu.io/upload_images/2552605-0fb1f66eebe0b5d8?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

修改完成后关闭编辑器，确认保存即可
再来查询版本信息，可以看到描述内容已经修改了

![](http://upload-images.jianshu.io/upload_images/2552605-e32cc77b53683cc9?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

其实，在提交文件到仓库时，如果想要提交多行描述文本，可以不用加**-m**参数，即直接用

```java
git commit
```
这样也可以启动Notepad++，就可以输入多行文本了

---

此时，再将1.txt文件中的“**git**”单词删去，重新提交

![](http://upload-images.jianshu.io/upload_images/2552605-fd07a47c83d74534?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

再将“object”单词删去，重新提交

![](http://upload-images.jianshu.io/upload_images/2552605-ac1dd1c48ef92041?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

此时的版本号情况

![](http://upload-images.jianshu.io/upload_images/2552605-84d8e0d403a12ec4?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

这里只是删去了一个单词，就又多了一个版本号，这里来尝试将最新两个版本号合并

```java
git rebase -i head~2
```
此时，Notepad++会自动启动

![](http://upload-images.jianshu.io/upload_images/2552605-18c4d1d4788220d6?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

对比上图查询到的版本信息已经描述内容，可以知道前两行内容的意义

这里将**73b0cbc**的历史记录压缩到**e8d6634**中，即删除最新一条记录，将之合并到第二条，将前两行文本修改为

```java
pick e8d6634 删去了单词git
fixup 73b0cbc 删去了单词object
```
退出保存
再来查询当前版本信息

![](http://upload-images.jianshu.io/upload_images/2552605-331f6263b8f4198f?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

可以看到版本变成了两个，且最新一个版本号与之前两个均不一样，这个是合并后得来的

可此时最新版本号的描述内容也不太正确，因为被删除了的单词有两个，这里再来修改下

```java
git commit --amend
```
此时，Notepad++就会启动，将描述内容修改为

```java
删去了单词git和object
```

退出保存
再来查询版本号信息，已经被正确改动了

![](http://upload-images.jianshu.io/upload_images/2552605-7b33b9881463cd61?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

## 四、远程仓库操作

在GitHub上新建一个仓库，命名为GitTest，具体步骤可以参考我上一篇博文，注意不要生成README文件

![](http://upload-images.jianshu.io/upload_images/2552605-51a9ab0bec978fb9?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

在GitHub上创建的仓库路径为：https://github.com/initobject/GitTest.git

![](http://upload-images.jianshu.io/upload_images/2552605-980f77171ab0c8b2?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

现在再来将之设置成本地仓库的远程仓库

```java
git remote add origin https://github.com/initobject/GitTest.git
```
Git会自动将远程仓库的名称设置为origin

然后将本地仓库当前分支下的所有内容推送到远程仓库

```java
git push -u origin master
```
![](http://upload-images.jianshu.io/upload_images/2552605-10a763de0f552c22?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

如此执行后，当前分支的内容就会被推送到远程仓库origin的master分支
-u参数可以在推送的同时，将origin仓库的master分支设置为本地仓库当前分支的上游（upstream）。添加了这个参数，将来运行git pull命令从远程仓库获取内容时，本地仓库的这个分支就可以直接从origin的master分支获取内容，省去了另外添加参数的麻烦

刷新GitHub网页，可以看到代码已经上传到了仓库了
比如我的GitHub仓库：https://github.com/initobject/GitTest

![](http://upload-images.jianshu.io/upload_images/2552605-4b296b4a01c0ae9c?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

此外，也可以为远程仓库设置分支
现在本地创建一个feature-A分支

```java
git checkout -b feature-A
```
再将之push远程仓库并保持分支名称相同

```java
git push -u origin feature-A
```

![](http://upload-images.jianshu.io/upload_images/2552605-e88a9d5e5dac1254?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

刷新GitHub网页，可以看到多了一个新分支，而文本内容一样

![](http://upload-images.jianshu.io/upload_images/2552605-7f614976ddc12b23?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

---

现在重新创建一个新仓库Git_New，用来从GitHub获取之前提交的内容

![](http://upload-images.jianshu.io/upload_images/2552605-a77e6eef22499429?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

远程仓库的地址可以从这里获取

![](http://upload-images.jianshu.io/upload_images/2552605-c6626f75c8a7f879?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

```java
git clone https://github.com/initobject/GitTest.git
```

![](http://upload-images.jianshu.io/upload_images/2552605-983d9a82e543cc63?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

可以看到Git_New文件夹下也多了一个新目录GitTest，即是我们之前上传的

可以将所有文件提交到仓库中

```java
git add .  （符号“.”表示增加工作区所有文件到暂存区）
git commit -m '增添了GitTest文件夹'
```

![](http://upload-images.jianshu.io/upload_images/2552605-167f18e42f6b1578?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

因为GitTest仓库是从远程仓库clone而来的，此时，GitTest仓库的master分支是与GitHub远程仓库相的master分支在内容上是完全相同的

进入GitTest文件夹

```java
cd GitTest
```

之前，我为GitTest仓库设置了feature-A分支，这里再来试着将该分支获取至本地仓库，

```java
git checkout -b feature-A origin/feature-A
// feature-A 表示本地仓库要新建的分支名
// origin/feature-A 代表以origin为名的远程仓库的feature-A分支
```

![](http://upload-images.jianshu.io/upload_images/2552605-b186dd4df8ecc5c0?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

再来查看当前分支的相关信息

```java
git branch -a

// -a 加上该参数可以同时显示本地仓库与远程仓库的分支信息
```

![](http://upload-images.jianshu.io/upload_images/2552605-c866f39924de80a5?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)


切换回master分支，为1.txt文件增添一个单词：**object**
现在的内容应该是

```java
init object
```
再查看当前状态

![](http://upload-images.jianshu.io/upload_images/2552605-c235509c8e3a3b39?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

提示1.txt文件未提交
提交该文件，并推送到远程仓库

```
git push
```
推送前需要先输入GitHub账户名与密码

![](http://upload-images.jianshu.io/upload_images/2552605-556eef61c772d1b0?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

刷新GitHub网页，可以看到仓库的描述文本都被改变了

![](http://upload-images.jianshu.io/upload_images/2552605-3437b6c67332c25f?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

---

现在再回到一开始使用的**Git**仓库，即步骤三使用的仓库

重新打开该仓库的方法是进入**Git文件夹**，鼠标右键点击**Git Bash Here**即可

因此在之前我们在本地向远程仓库的master分支推送了新内容，本地的仓库是旧的的，这里再来获取远程仓库的最新内容

```java
git pull origin
```

![](http://upload-images.jianshu.io/upload_images/2552605-8d1f9a94b5507014?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

查看版本变化，可以看到最新版本号就是之前推送的

![](http://upload-images.jianshu.io/upload_images/2552605-b6e59d568681f38d?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

这样，本地仓库的内容也得到了更新
这种方式方便于进行多人合作开发，一人向远程仓库推送开发内容，另一人获取到本地，修改完成后再提交到远程仓库，也可以在不同分支下分开开发，如此循环反复直至软件培育完成