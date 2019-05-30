## 一、Git是什么
Git 与CVS、Subversion 一类的集中式版本控制工具不同，它采用了分布式版本库的作法，不需要服务器端软件，就可以运作版本控制，使得源代码的发布和交流极其方便。Git 的速度很快，这对于诸如 Linux 内核这样的大项目来说自然很重要，Git 最为出色的是它的合并追踪（merge tracing）能力

## 二、Git的配置
Git官网下载地址：https://git-scm.com/download/
Git完成默认配置安装后，在桌面点击鼠标右键，会有两个选项

Git  GUI Here 代表图形界面模式
Git Bash Here 代表命令行模式

![](http://upload-images.jianshu.io/upload_images/2552605-f67487a7e0461799?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

这里选择命令行模式

![](http://upload-images.jianshu.io/upload_images/2552605-4850bae68858d20e?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

之后需要先设置你的用户名和email地址作为个人标示，这是非常重要的，因为每次Git提交都需要使用该信息

```java
config --global user.name "用户名"
config --global user.email "邮箱地址"
```

--global 选项代表Git将使用该信息来处理你在系统中所做的一切操作，如果希望在一个特定的项目下使用不同的用户名或email地址，可以在该项目中运行该命令而不用--global选项

![](http://upload-images.jianshu.io/upload_images/2552605-c7dbbac5cafce8f2?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240) 

配置完成后可以用以下命令查看个人信息

```java
cat ~/.gitconfig
```

![](http://upload-images.jianshu.io/upload_images/2552605-3ea24abc85ee707d?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

## 三、仓库
### （1）创建仓库
GitHub是以仓库（Repositories）的概念来管理一个项目的，仓库可以理解为一个目录，该目录下发生的所有文件变化，例如文件创建、文件修改、文件删除等都可以被Git所跟踪记录
Git可以对每一个版本的修改进行记录，并保存各个版本号，在需要的时候可以对版本进行更迭回退

创建仓库的步骤也比较简单
这里选择在D盘的git文件夹下创建
![](http://upload-images.jianshu.io/upload_images/2552605-505eb155cdc836c7?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

```java
cd 命令用来打开文件夹
mkdir 命令用来创建文件夹
pwd 命令用来确定当前路径
```

这样，就可以看到D盘下生成了一个git文件夹了
之后，使用以下命令将git文件夹初始化为一个仓库

```java
git init
```

![](http://upload-images.jianshu.io/upload_images/2552605-4c1c7905df4fac76?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

然后，在git文件夹下就可以看到多出一个名为 **.git** 的文件夹了，需要设置系统**显示隐藏的项目**才可以看到

![](http://upload-images.jianshu.io/upload_images/2552605-5425d647ce40a80c?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

该文件夹用来记录版本变化的，不可以手动修改

### （2）向仓库提交文件
首先在git文件夹下新建一个test.txt文本文件，随便输入一些内容，我这里输入：11111111111
使用以下命令将之添加到暂存区中

```java
git add test.txt
```

![](http://upload-images.jianshu.io/upload_images/2552605-3b12e207e9ea48c1?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

再将文件提交到Git仓库

```java
git commit -m '版本说明'
```
![](http://upload-images.jianshu.io/upload_images/2552605-a8089903984c05da?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

然后再来查看当前状态，检查是否还有文件未提交

```java
git status
```

![](http://upload-images.jianshu.io/upload_images/2552605-44e668d41d657043?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

可以看到，提示说没有文件需要提交，工作目录是干净的

之后再来修改test.txt文件，多添加一行内容：22222222222
再来查看当前状态

![](http://upload-images.jianshu.io/upload_images/2552605-152febb396305520?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

提示说有文件被修改但还没被提交

查看有哪些内容被修改了，由此可以查看文本内容的前后变化

```java
git diff test.txt
```

![](http://upload-images.jianshu.io/upload_images/2552605-05995586d8e4151e?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

此时test.txt就相当于一个新版本了，确认文件修改完成后，可以再次向仓库提交文件，保存当前版本

![](http://upload-images.jianshu.io/upload_images/2552605-092e49024a4bd226?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

查看当前所有版本

```java
git log
```

![](http://upload-images.jianshu.io/upload_images/2552605-aa1411e73a990aa5?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

可以看到一共有两个版本号，提交者与提交时间都有记录

如果觉得以上信息太繁杂，可以用以下命令查看简短的版本信息

```java
$ git log --pretty=oneline

```

![](http://upload-images.jianshu.io/upload_images/2552605-5b36dfc37b46505a?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

## 四、版本回退
之前已经知道了如何提交文件并查看版本信息，这里再来学习如何回退到前一个版本
当前test.txt是第二个版本，文本内容应该是

```java
11111111111
22222222222
```

这时想要让它回退到第一个版本

```java
git reset  --hard HEAD^  回退到上一个版本
git reset  --hard HEAD^^ 回退到上上个版本
···
git reset  --hard HEAD~10 回退十个版本
```

![](http://upload-images.jianshu.io/upload_images/2552605-8d40656a463733b2?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

可以看到已经回退到了第一个版本
当中，“27d08d6”该字段代表简短型的版本号，由之前查看所有版本的图片可知，是完整版本号的前缀字段

打开test.txt文件，可以看到文本内容已经变成了

```javascript
11111111111
```
或者用以下命令查看文本内容

```java
cat test.txt
```
![](http://upload-images.jianshu.io/upload_images/2552605-c84237fb07cf722e?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

查看版本信息

![](http://upload-images.jianshu.io/upload_images/2552605-9d400796837d5897?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

可以看到只剩下一个版本信息了

如果回退版本后后悔了，想要恢复到第二个版本，可以通过以下命令恢复到指定版本号

```java
git reset  --hard 指定版本号
```

不过此时我们不知道第二个版本的版本号，可以先来查询

```java
git reflog
```

![](http://upload-images.jianshu.io/upload_images/2552605-6c0c492c43c4661d?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

可以看到一共记录了三次版本信息，两次修改内容，一次回退版本

则6789d5c就是我们需要的版本号，恢复之，并查看文本内容，可以看到内容又恢复到第二个版本了

![](http://upload-images.jianshu.io/upload_images/2552605-105f61541f634868?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

## 五、撤销操作
### （1）撤销修改操作
在test.txt文件中再添加一行数据，现在的内容应该是：

```java
11111111111
22222222222
33333333333
```
查看当前状态

![](http://upload-images.jianshu.io/upload_images/2552605-9d30aaf7b02bf904?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

提示说有修改的内容未提交
此时，如果想要撤销该修改，除了可以直接指定版本号进行回退外，也可以使用以下命令撤销在工作区的修改操作

```java
git checkout -- test.txt
```

![](http://upload-images.jianshu.io/upload_images/2552605-29307e556758f3f2?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

可以看到文本内容又恢复了

该命令可以分为两种情况
（1）在完成一次提交操作后，又对文件进行了修改操作，且此时文件还未添加到暂存区
此时执行该命令是恢复到上一次提交操作后的状态，即撤销在工作区进行的修改
（2）文件添加到了暂存区，此时进行了修改操作，且还未提交到仓库
此时执行该命令是撤销在添加到暂存区后进行的修改操作

### （2）撤销删除操作
在git文件夹下再新建一个one.txt文件，然后将之提交到仓库中

![](http://upload-images.jianshu.io/upload_images/2552605-2e7180cb231cfa7a?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

此时，one.txt文件就已经存在于版本库中了

然后再通过手动删除或者使用以下命令删除one.txt文件

```java
rm one.txt
```

![](http://upload-images.jianshu.io/upload_images/2552605-1b576579f3258f18?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

可以看到删除操作也被记录下来了，此时可以选择直接commit，提交本次修改，或者使用以下命令从版本库中恢复被删除的文件

```java
git checkout -- one.txt
```

![](http://upload-images.jianshu.io/upload_images/2552605-3a2bf0d61b5b5aeb?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

查看目录，可以看到文件已经恢复了

## 六、远程仓库
这里来尝试将本地仓库提交到GitHub上，需要用户先有一个GitHub账号
我的GitHub账号是：https://github.com/leavesC
现在也是啥都没有，之后会逐渐把自己之前做的东西上传上去的
或者是可以看我的CSDN博客：http://blog.csdn.net/new_one_object

在这之前需要先创建自己的创建SSH Key
如果之前已经创建过了，以下目录中就会含有两个密匙文件
id_rsa是私钥，id_rsa.pub是公钥

![](http://upload-images.jianshu.io/upload_images/2552605-f192091dbcf55852?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

如果没有，还需要先使用以下命令来创建

```java
ssh-keygen -t rsa -C "initobject@gmail.com"（自己的邮箱地址）
```
输入后可能需要连续敲几次回车，之后就生成key了

登录GitHub，进入**Settings**界面，点击**SSH and GPG keys**，新建一个SSH key

![](http://upload-images.jianshu.io/upload_images/2552605-32c16c2b7ca6fcc5?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

添加成功后界面应该是这样的

![](http://upload-images.jianshu.io/upload_images/2552605-24d01646685bcf1a?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)


此时，我们要做的是把在本地电脑的工程提交到GitHub，所以需要先在GitHub上建立一个仓库，工程名和描述可以随意写

![](http://upload-images.jianshu.io/upload_images/2552605-d7ee1e726efd1a78?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

点击“Create repository”按钮，就创建了一个Git仓库了

![](http://upload-images.jianshu.io/upload_images/2552605-c84c7976ab80ec1a?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

现在这个test仓库还是空的，GitHub提示我们可以通过命令行将该仓库与本地工程进行关联，然后推送本地工程文件

```java
…or push an existing repository from the command line
git remote add origin https://github.com/initobject/test.git
git push -u origin master
```
以上命令根据不同的用户名与仓库名而会有所不同
根据该提示我们可以先输入如下命令

```java
git remote add origin https://github.com/initobject/test.git
```
再输入

```java
git push -u origin master
```
会提示用户输入GitHub用户名与密码

![](http://upload-images.jianshu.io/upload_images/2552605-8819daf6e7314e57?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

![](http://upload-images.jianshu.io/upload_images/2552605-f23c4e831e1fbf65?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

有时候因为网络原因上传文件到GitHub会失败~~

此时，刷新GitHub网页，可以看到文件都已经上传成功了，可以下载下来与本地文件进行对比

![](http://upload-images.jianshu.io/upload_images/2552605-14f20773cbedca84?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

之后，本地每次进行了修改操作后，可以直接用以下命令将工程推送到远程仓库中

```java
git push origin master
```

以上是将本地仓库推送到远程仓库中，那么自然也可以将远程仓库克隆到本地仓库中
在GitHub上再新建一个仓库，命名为test2

![](http://upload-images.jianshu.io/upload_images/2552605-17e85a7cfb724a89?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

![](http://upload-images.jianshu.io/upload_images/2552605-49a8a05296245158?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

在本地输入如下命令就可以将test2工程克隆或者说下载到git文件夹下了

```java
https://github.com/initobject/tets2
```

![](http://upload-images.jianshu.io/upload_images/2552605-3e966f0af6fbf64c?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

## 七、创建与合并分支
到目前为止，本地工程中只包含有一条主分支，即master分支
可以用以下命令查看所有分支，当前分支名的前边会有一个星号

```java
git branch
```

![](http://upload-images.jianshu.io/upload_images/2552605-6b34e06f8f8ce4a1?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

创建一个新分支并命名为dev，并切换到dev分支

```java
git checkout -b dev
```

![](http://upload-images.jianshu.io/upload_images/2552605-54c4e1a1c34dd206?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

该命令表示创建并切换分支，相当于如下两条命令

```java
git branch dev 创建dev分支

git checkout dev 切换到dev分支
```

查看当前test.txt文件内容，并添加新的一行内容，再查看

![](http://upload-images.jianshu.io/upload_images/2552605-cfa179d58ceab4f5?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

在dev分支上向仓库提交文件

![](http://upload-images.jianshu.io/upload_images/2552605-1ae0e3b9f54b2673?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

切换到master分支，查看test.txt的内容，可以看到在master分支下文本内容并没有被改动，因为改动操作是在dev分支下进行的

```java
git checkout master
```

![](http://upload-images.jianshu.io/upload_images/2552605-2517647fc6fd6370?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

在master分支下，将dev分支的内容合并到master上，可以看到test.txt文件被改动了

```java
git merge dev
```

![](http://upload-images.jianshu.io/upload_images/2552605-8044903b48216b9c?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

之后，删除dev分支

```java
git branch -d dev
```

![](http://upload-images.jianshu.io/upload_images/2552605-9c4b7cc894442876?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)