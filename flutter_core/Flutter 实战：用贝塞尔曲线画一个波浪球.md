> 公众号：[字节数组](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/adbc507fc3704fd8955aae739a433db2~tplv-k3u1fbpfcp-zoom-1.image)
>
> 希望对你有所帮助 🤣🤣

当 flutter 的现有组件无法满足产品要求的 UI 效果时，我们就需要通过自绘组件的方式来进行实现了。本篇文章就来介绍如何用 flutter 自定义实现一个带文本的波浪球，效果如下所示：

![](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/c4a23268ff864004a6dda427b8d4f0a7~tplv-k3u1fbpfcp-zoom-1.image)

先来总结下 WaveLoadingWidget 的特点，这样才能归纳出实现该效果所需要的步骤：

1. widget 的主体是一个不规则的半圆形，顶部曲线以类似于波浪的形式从左往右上下起伏运行
2. 波浪球可以自定义颜色，此处以 waveColor 命名
3. 波浪球的起伏线将嵌入的文本分为上下两种颜色，上半部分颜色以 backgroundColor 命名，下半部分颜色以 foregroundColor 命名，文本的整体颜色一直在根据波浪的运行而动态变化中

虽然文本的整体颜色是在不断变化的，但只要能够绘制出其中一帧的图形，其动态效果就能通过不断改变波浪曲线的位置参数来实现，所以这里先把该 widget 当成静态的，先实现其静态效果即可

将绘制步骤拆解为以下几步：

1. 绘制颜色为 backgroundColor 的文本，将其绘制在 canvas 的最底层
2. 根据 widget 的宽高信息构建一个不超出范围的最大圆形路径 circlePath
3. 以 circlePath 的水平中间线作为波浪的基准起伏线，在起伏线的上边和下边分别用贝塞尔曲线绘制一段连续的波浪 path，将 path 的首尾两端以矩形的方式连接在一起，构成 wavePath，wavePath 的底部会与 circlePath 的最底部相交
4. 取 circlePath 和 wavePath 的交集 combinePath，用 waveColor 填充， 此时就得到了半圆形的球形波浪了
5. 利用 `canvas.clipPath(combinePath)` 方法裁切画布，再绘制颜色为 foregroundColor 的文本，此时绘制的 foregroundColor 文本只会显示 combinePath 范围内的部分，也即只会显示下半部分，使得两次不同时间绘制的文本重叠在了一起，从而得到了有不同颜色范围的文本
6. 利用 AnimationController 不断改变 wavePath 的起始点的 X 坐标，同时重新刷新 UI，从而得到波浪不断从左往右起伏运行的动态效果

现在就来一步步实现以上的绘制步骤吧

# 一、绘制 backgroundColor 文本

flutter 通过 CustomPainter 为开发者提供了自绘 UI 的入口，其内部的 `void paint(Canvas canvas, Size size)`  方法提供了画布 canvas 对象以及包含 widget 宽高信息的 size 对象

这里就来继承 CustomPainter 类，在 `paint` 方法中先来绘制颜色为 backgroundColor 的文本。flutter 的 canvas 对象没有提供直接 `drawText` 的 API，所以其绘制文本的步骤相对原生的自定义 View 要稍微麻烦一点

```dart
class _WaveLoadingPainter extends CustomPainter {
  final String text;

  final double fontSize;

  final double animatedValue;

  final Color backgroundColor;

  final Color foregroundColor;

  final Color waveColor;

  _WaveLoadingPainter({
    required this.text,
    required this.fontSize,
    required this.animatedValue,
    required this.backgroundColor,
    required this.foregroundColor,
    required this.waveColor,
  });

  @override
  void paint(Canvas canvas, Size size) {
    final side = min(size.width, size.height);
    _drawText(canvas: canvas, side: side, color: backgroundColor);
  }

  void _drawText(
      {required Canvas canvas, required double side, required Color color}) {
    ParagraphBuilder paragraphBuilder = ParagraphBuilder(ParagraphStyle(
      textAlign: TextAlign.center,
      fontStyle: FontStyle.normal,
      fontSize: fontSize,
    ));
    paragraphBuilder.pushStyle(ui.TextStyle(color: color));
    paragraphBuilder.addText(text);
    ParagraphConstraints pc = ParagraphConstraints(width: fontSize);
    Paragraph paragraph = paragraphBuilder.build()..layout(pc);
    canvas.drawParagraph(
      paragraph,
      Offset((side - paragraph.width) / 2.0, (side - paragraph.height) / 2.0),
    );
  }

  @override
  bool shouldRepaint(CustomPainter oldDelegate) {
    return animatedValue != (oldDelegate as _WaveLoadingPainter).animatedValue;
  }
}
```

![](https://upload-images.jianshu.io/upload_images/2552605-5e9e2e5f2c1399a4.png)

# 二、构建 circlePath

取 widget 的宽度和高度的最小值作为圆的直径大小，以此构建出一个不超出 widget 范围的最大圆形路径 circlePath

```dart
  @override
  void paint(Canvas canvas, Size size) {
    final side = min(size.width, size.height);
    _drawText(canvas: canvas, side: side, color: backgroundColor);

    final circlePath = Path();
    circlePath.addArc(Rect.fromLTWH(0, 0, side, side), 0, 2 * pi);
  }
```

# 三、绘制波浪线

波浪的宽度和高度就根据一个固定的比例值来求值，以 circlePath 的中间分隔线作为水平线，在水平线的上下根据贝塞尔曲线绘制出连续的波浪线

```dart
  @override
  void paint(Canvas canvas, Size size) {
    final side = min(size.width, size.height);
    _drawText(canvas: canvas, side: side, color: backgroundColor);

    final circlePath = Path();
    circlePath.addArc(Rect.fromLTWH(0, 0, side, side), 0, 2 * pi);

    final waveWidth = side * 0.8;
    final waveHeight = side / 6;
    final wavePath = Path();
    final radius = side / 2.0;
    wavePath.moveTo(-waveWidth, radius);
    for (double i = -waveWidth; i < side; i += waveWidth) {
      wavePath.relativeQuadraticBezierTo(
          waveWidth / 4, -waveHeight, waveWidth / 2, 0);
      wavePath.relativeQuadraticBezierTo(
          waveWidth / 4, waveHeight, waveWidth / 2, 0);
    }
    //为了方便读者理解，这里把 wavePath 绘制出来，实际上不需要
    final paint = Paint()
      ..isAntiAlias = true
      ..style = PaintingStyle.fill
      ..strokeWidth = 3
      ..color = waveColor;
    canvas.drawPath(wavePath, paint);
  }
```

![](https://upload-images.jianshu.io/upload_images/2552605-89927e62ad3d25fe.png)

此时绘制的曲线还处于非闭合状态，需要将 wavePath 的首尾两端连接起来，这样后面才可以和 circlePath 取交集

```dart
wavePath.relativeLineTo(0, radius);
wavePath.lineTo(-waveWidth, side);
wavePath.close();
//为了方便读者理解，这里把 wavePath 绘制出来，实际上不需要
final paint = Paint()
  ..isAntiAlias = true
  ..style = PaintingStyle.fill
  ..strokeWidth = 3
  ..color = waveColor;
canvas.drawPath(wavePath, paint);
```

wavePath 闭合后，此时半圆的颜色就会铺满了

![](https://upload-images.jianshu.io/upload_images/2552605-4cb68f341f58a5f1.png)

# 四、取交集

取 circlePath 和  wavePath 的交集，就得到一个半圆形波浪球了

```dart
final paint = Paint()
  ..isAntiAlias = true
  ..style = PaintingStyle.fill
  ..strokeWidth = 3
  ..color = waveColor;
final combinePath = Path.combine(PathOperation.intersect, circlePath, wavePath);
canvas.drawPath(combinePath, paint);
```

![](https://upload-images.jianshu.io/upload_images/2552605-745ad48c90fff2c3.png)

# 五、绘制 foregroundColor 文本

文本的颜色是分为上下两部分的，上半部分颜色为 backgroundColor，下半部分为 foregroundColor。在第一步的时候已经绘制了颜色为 backgroundColor 的文本了，foregroundColor 文本不需要显示上半部分，所以在绘制 foregroundColor 文本之前需要先把绘制区域限定在 combinePath 内，使得两次不同时间绘制的文本重叠在了一起，从而得到有不同颜色范围的文本

```dart
canvas.clipPath(combinePath);
_drawText(canvas: canvas, side: side, color: foregroundColor);
```

![](https://upload-images.jianshu.io/upload_images/2552605-e2df4fd5f288ffdd.png)

# 六、添加动画

现在已经绘制好静态时的效果了，可以考虑如何使 widget 动起来了

要实现动态效果也很简单，只要不断改变贝塞尔曲线的起始点坐标，使之不断从左往右移动，就可以营造出波浪从左往右前进的效果了。_WaveLoadingPainter 根据外部传入的动画值 animatedValue 来设置 wavePath 的起始坐标点即可，生成 animatedValue 的逻辑和其它绘制参数均由 _WaveLoadingState 来提供

```dart
class _WaveLoadingState extends State<WaveLoading>
    with SingleTickerProviderStateMixin {
  String get _text => widget.text;

  double get _fontSize => widget.fontSize;

  Color get _backgroundColor => widget.backgroundColor;

  Color get _foregroundColor => widget.foregroundColor;

  Color get _waveColor => widget.waveColor;

  late AnimationController _controller;

  late Animation<double> _animation;

  @override
  void initState() {
    super.initState();
    _controller = AnimationController(
        duration: const Duration(milliseconds: 700), vsync: this);
    _animation = Tween(
      begin: 0.0,
      end: 1.0,
    ).animate(_controller)
      ..addListener(() {
        setState(() => {});
      });
    _controller.repeat();
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return RepaintBoundary(
      child: CustomPaint(
        painter: _WaveLoadingPainter(
          text: _text,
          fontSize: _fontSize,
          animatedValue: _animation.value,
          backgroundColor: _backgroundColor,
          foregroundColor: _foregroundColor,
          waveColor: _waveColor,
        ),
      ),
    );
  }
}

```

_WaveLoadingPainter 根据 animatedValue 来设置 wavePath 的起始坐标点

```kotlin
wavePath.moveTo((animatedValue - 1) * waveWidth, radius);
```

# 七、使用

最后将 _WaveLoadingState 包裹到 StatefulWidget 中，在 StatefulWidget 中开放可以自定义配置的参数就可以了

```dart
class WaveLoading extends StatefulWidget {
  final String text;

  final double fontSize;

  final Color backgroundColor;

  final Color foregroundColor;

  final Color waveColor;

  WaveLoading({
    Key? key,
    required this.text,
    required this.fontSize,
    required this.backgroundColor,
    required this.foregroundColor,
    required this.waveColor,
  }) : super(key: key) {
    assert(text.isNotEmpty && fontSize > 0);
  }

  @override
  State<StatefulWidget> createState() {
    return _WaveLoadingState();
  }
}
```

使用方式：

```dart
SizedBox(
	width: 300,
	height: 300,
	child: WaveLoading(
  		text: "開",
  		fontSize: 210,
  		backgroundColor: Colors.lightBlue,
  		foregroundColor: Colors.white,
  		waveColor: Colors.lightBlue,
)
```


源代码看这里：[WaveLoadingWidget](https://github.com/leavesCZY/WaveLoadingWidget)