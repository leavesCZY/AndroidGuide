> 公众号：[字节数组](https://upload-images.jianshu.io/upload_images/2552605-57915be42c4f6a82.jpg)
>
> 希望对你有所帮助 🤣🤣

flutter 中的自定义 Widget 算作是 flutter 体系中比较高阶的知识点之一了，相当于原生开发中的自定义 View，以我个人的感受来说，自定义 widget 的难度要低于自定义 View，不过由于当前 flutter 的开源库还不算多丰富，所以有些效果还是需要开发者自己动手来实现，而本篇文章就来介绍如何用 flutter 来实现一个带文本的波浪球 Widget，实现的的效果如下所示：

![](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/6ecd9c54e5e049708a4b990bca004998~tplv-k3u1fbpfcp-zoom-1.image)

源代码点击这里下载：https://github.com/leavesCZY/flutter_do

先来总结下该 WaveLoadingWidget 的特点，这样才能归纳出实现该效果所需的步骤

1. widget 的主体是一个不规则的半圆，顶部以类似于波浪的形式从左往右上下波动运行
2. 球形波浪可以自定义颜色，此处以 waveColor 命名
3. 波浪的起伏线将嵌入的文本分为上下两种颜色，上边的文本颜色以 backgroundColor 命名，下边的文本颜色以 foregroundColor 命名，文本的颜色一直在动态变化中

虽然波浪是不断运动的，但只要能够绘制出其中一帧的图形，其动态效果就能通过不断改变波浪的位置参数来完成，所以这里先把该 widget 当成静态的，先实现其静态效果即可

将绘制步骤拆解为以下几步：

1. 绘制颜色为 backgroundColor 的文本，将其绘制在 canvas 的最底层
2. 根据 widget 的宽高信息构建一个不超出范围的最大圆形路径 circlePath
3. 以 circlePath 的水平中间线作为波浪的起伏线，在起伏线的上边和下边分别利用贝塞尔曲线绘制一段连续的波浪 path，将 path 的首尾两端以矩形的形式连接在一起，构成 wavePath，wavePath 的底部会与 circlePath 的底部相交于一点
4. 取 circlePath 和 wavePath 的交集 targetPath，用 waveColor 填充， 此时就得到了半圆形的球形波浪了
5. 利用 `canvas.clipPath(targetPath)` 方法裁切画布，再绘制颜色为 foregroundColor 的文本，此时绘制的 foregroundColor 文本只会显示 targetPath 范围内的部分，从而使两次不同时间绘制的文本重叠在了一起，得到了有不同颜色范围的文本
6. 利用 flutter 动画不断改变 wavePath 的起始点的 X 坐标，同时重新绘制 UI，从而得到波浪不断从左往右前进的效果

现在就来一步步实现以上的绘制步骤吧

# 一、初始化画笔

flutter 通过抽象类 `CustomPainter` 为开发者提供了自绘 UI 的入口，其内部的抽象方法 `void paint(Canvas canvas, Size size)` 提供了**画布对象 canvas** 以及包含 widget **宽高信息的 size 对象**

此处就来继承 CustomPainter 类，初始化画笔对象以及各个配置参数（要绘制的文本，颜色值等）

```dart
class WaveLoadingPainter extends CustomPainter {
  //如果外部没有指定颜色值，则使用此默认颜色值
  static final Color defaultColor = Colors.lightBlue;

  //画笔对象
  var _paint = Paint();

  //圆形路径
  Path _circlePath = Path();

  //波浪路径
  Path _wavePath = Path();

  //要显示的文本
  final String text;

  //字体大小
  final double fontSize;

  final Color backgroundColor;

  final Color foregroundColor;

  final Color waveColor;

  WaveLoadingPainter(
      {this.text,
      this.fontSize,
      this.backgroundColor,
      this.foregroundColor,
      this.waveColor}) {
    _paint
      ..isAntiAlias = true
      ..style = PaintingStyle.fill
      ..strokeWidth = 3
      ..color = waveColor ?? defaultColor;
  }

  @override
  void paint(Canvas canvas, Size size) {
    
  }

  @override
  bool shouldRepaint(CustomPainter oldDelegate) {
    return true;
  }
}

```

# 二、绘制 backgroundColor 文本

flutter 的 canvas 对象没有提供直接 `drawText` 的 API，其绘制文本的步骤相对原生的自定义 View 要比较麻烦

```dart
@override
  void paint(Canvas canvas, Size size) {
    double side = min(size.width, size.height);
    double radius = side / 2.0;

    _drawText(canvas: canvas, side: side, colors: backgroundColor);
      
    ···
  }

  void _drawText({Canvas canvas, double side, Color colors}) {
    ParagraphBuilder pb = ParagraphBuilder(ParagraphStyle(
      textAlign: TextAlign.center,
      fontStyle: FontStyle.normal,
      fontSize: fontSize ?? 0,
    ));
    pb.pushStyle(ui.TextStyle(color: colors ?? defaultColor));
    pb.addText(text);
    ParagraphConstraints pc = ParagraphConstraints(width: fontSize ?? 0);
    Paragraph paragraph = pb.build()..layout(pc);
    canvas.drawParagraph(
        paragraph,
        Offset(
            (side - paragraph.width) / 2.0, (side - paragraph.height) / 2.0));
  }
```

![](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/fd485927e396495aabba9f6bd00eb673~tplv-k3u1fbpfcp-zoom-1.image)

# 三、构建圆形路径 circlePath

取 widget 的宽和高的最小值作为圆的直径大小，以此构建出一个不超出 widget 范围的最大圆形路径

```dart
 @override
  void paint(Canvas canvas, Size size) {
    double side = min(size.width, size.height);
    double radius = side / 2.0;

    _drawText(canvas: canvas, side: side, colors: backgroundColor);
      
    _circlePath.reset();
    _circlePath.addArc(Rect.fromLTWH(0, 0, side, side), 0, 2 * pi);

    ···
  }
```

# 四、绘制波浪线

此处波浪的宽度和高度就根据一个固定的比例值来求值，以 _circlePath 的中间分隔线作为水平线，在水平线上下根据贝塞尔曲线绘制出连续的波浪线

```dart
  @override
  void paint(Canvas canvas, Size size) {
    double side = min(size.width, size.height);
    double radius = side / 2.0;

    _drawText(canvas: canvas, side: side, colors: backgroundColor);

    _circlePath.reset();
    _circlePath.addArc(Rect.fromLTWH(0, 0, side, side), 0, 2 * pi);

    double waveWidth = side * 0.8;
    double waveHeight = side / 6;
    _wavePath.reset();
    _wavePath.moveTo(-waveWidth, radius);
    for (double i = -waveWidth; i < side; i += waveWidth) {
      _wavePath.relativeQuadraticBezierTo(
          waveWidth / 4, -waveHeight, waveWidth / 2, 0);
      _wavePath.relativeQuadraticBezierTo(
          waveWidth / 4, waveHeight, waveWidth / 2, 0);
    }

    //为了方便读者理解，这里把路径绘制出来，实际上不需要
    canvas.drawPath(_wavePath, _paint);

  }
```

![](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/36663f142ac640179a8823bf091a4e1c~tplv-k3u1fbpfcp-zoom-1.image)

此时绘制的曲线还处于非闭合状态，需要将 _wavePath 的首尾两端连接起来，这样才可以和 _circlePath 做交集

```dart
    _wavePath.relativeLineTo(0, radius);
    _wavePath.lineTo(-waveWidth, side);
    _wavePath.close();
```

_wavePath 闭合后，此时绘制出来的图形就如下所示

![](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/b6377be8ef3846c58ab291f8d396ea0d~tplv-k3u1fbpfcp-zoom-1.image)

# 五、取交集

_circlePath 和  _wavePath 的交集就是一个半圆形波浪了

```dart
    var combine = Path.combine(PathOperation.intersect, _circlePath, _wavePath);
    canvas.drawPath(combine, _paint);

    //为了方便读者理解，这里把路径绘制出来，实际上不需要
    canvas.drawPath(combine, _paint);
```



![](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/a01e65ed24844720b455ceb69ac0a74e~tplv-k3u1fbpfcp-zoom-1.image)

# 六、绘制 foregroundColor 文本

文本的颜色是分为上下两部分的，foregroundColor 颜色的文本不需要显示上半部分，所以在绘制 foregroundColor 文本的时候需要把上半部分文本给裁切掉，使两次不同时间绘制的文本重叠在了一起，得到了有不同颜色范围的文本

```dart
    canvas.clipPath(combine);
    _drawText(canvas: canvas, side: side, colors: foregroundColor);
```

![](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/babf80b896ce4a56bb20bbfc53da6966~tplv-k3u1fbpfcp-zoom-1.image)

# 七、添加动画

现在已经绘制好了单独一帧时的效果图了，可以考虑使 widget 动起来了

只要不断改变贝塞尔曲线的起始点坐标，使之不断从左往右移动，就可以营造出波浪从左往右前进的效果了。WaveLoadingPainter 只负责根据外部传入的动画值 **animatedValue** 来绘制 UI，构造 animatedValue 的逻辑则由外部的 _WaveLoadingWidgetState 进行处理，这里规定 animatedValue 的值是从 0 递增到 1，在开始构建 _wavePath 前只需要移动其起始坐标点即可

```dart
 @override
  void paint(Canvas canvas, Size size) {
    double side = min(size.width, size.height);
    double radius = side / 2.0;

    _drawText(canvas: canvas, side: side, colors: backgroundColor);

    _circlePath.reset();
    _circlePath.addArc(Rect.fromLTWH(0, 0, side, side), 0, 2 * pi);

    double waveWidth = side * 0.8;
    double waveHeight = side / 6;
    _wavePath.reset();
    _wavePath.moveTo((animatedValue - 1) * waveWidth, radius);
    for (double i = -waveWidth; i < side; i += waveWidth) {
      _wavePath.relativeQuadraticBezierTo(
          waveWidth / 4, -waveHeight, waveWidth / 2, 0);
      _wavePath.relativeQuadraticBezierTo(
          waveWidth / 4, waveHeight, waveWidth / 2, 0);
    }
    _wavePath.relativeLineTo(0, radius);
    _wavePath.lineTo(-waveWidth, side);
    _wavePath.close();

    var combine = Path.combine(PathOperation.intersect, _circlePath, _wavePath);
    canvas.drawPath(combine, _paint);

    canvas.clipPath(combine);
    _drawText(canvas: canvas, side: side, colors: foregroundColor);
  }
```

```dart
class _WaveLoadingWidgetState extends State<WaveLoadingWidget>
    with SingleTickerProviderStateMixin {
  final String text;

  final double fontSize;

  final Color backgroundColor;

  final Color foregroundColor;

  final Color waveColor;

  AnimationController controller;

  Animation<double> animation;

  _WaveLoadingWidgetState(
      {@required this.text,
      @required this.fontSize,
      @required this.backgroundColor,
      @required this.foregroundColor,
      @required this.waveColor});

  @override
  void initState() {
    super.initState();
    controller =
        AnimationController(duration: const Duration(seconds: 1), vsync: this);
    controller.addStatusListener((status) {
      switch (status) {
        case AnimationStatus.dismissed:
          print("dismissed");
          break;
        case AnimationStatus.forward:
          print("forward");
          break;
        case AnimationStatus.reverse:
          print("reverse");
          break;
        case AnimationStatus.completed:
          print("completed");
          break;
      }
    });

    animation = Tween(
      begin: 0.0,
      end: 1.0,
    ).animate(controller)
      ..addListener(() {
        setState(() => {});
      });
    controller.repeat();
  }

  @override
  void dispose() {
    controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return CustomPaint(
      painter: WaveLoadingPainter(
        text: text,
        fontSize: fontSize,
        animatedValue: animation.value,
        backgroundColor: backgroundColor,
        foregroundColor: foregroundColor,
        waveColor: waveColor,
      ),
    );
  }
}
```

# 八、使用

之后只要将 WaveLoadingPainter 包裹到 StatefulWidget 中即可，在 StatefulWidget 中开放可以自定义配置的参数就可以了

```dart
class WaveLoadingWidget extends StatefulWidget {
  final String text;

  final double fontSize;

  final Color backgroundColor;

  final Color foregroundColor;

  final Color waveColor;

  WaveLoadingWidget(
      {@required this.text,
      @required this.fontSize,
      @required this.backgroundColor,
      @required this.foregroundColor,
      @required this.waveColor}) {
    assert(text != null && text.length == 1);
    assert(fontSize != null && fontSize > 0);
  }

  @override
  _WaveLoadingWidgetState createState() => _WaveLoadingWidgetState(
        text: text,
        fontSize: fontSize,
        backgroundColor: backgroundColor,
        foregroundColor: foregroundColor,
        waveColor: waveColor,
      );
}
```

使用方式就类似于一般的系统 widget 

```dart
		Container(
            width: 300,
            height: 300,
            child: WaveLoadingWidget(
              text: "锲",
              fontSize: 215,
              backgroundColor: Colors.lightBlue,
              foregroundColor: Colors.white,
              waveColor: Colors.lightBlue,
            ),
          ),
          Container(
            width: 250,
            height: 250,
            child: WaveLoadingWidget(
              text: "而",
              fontSize: 175,
              backgroundColor: Colors.indigoAccent,
              foregroundColor: Colors.white,
              waveColor: Colors.indigoAccent,
            ),
          ),
```



源代码点击这里下载：https://github.com/leavesCZY/flutter_do

此外该项目也提供了 N 多个常用 Widget 和自定义 Widget 的使用及实现方法，涵盖了系统 Widget 、布局容器、动画、高阶功能、自定义 Widget 等内容，欢迎 star