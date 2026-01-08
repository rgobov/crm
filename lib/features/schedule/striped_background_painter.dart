import 'package:flutter/material.dart';

/// Виджет, который рисует диагональную штриховку.
class StripedBackground extends StatelessWidget {
  final Color backgroundColor;
  final Color stripeColor;

  const StripedBackground({
    super.key,
    this.backgroundColor = Colors.transparent,
    this.stripeColor = Colors.black12,
  });

  @override
  Widget build(BuildContext context) {
    // --- ИЗМЕНЕНИЕ ЗДЕСЬ: Добавляем ClipRect для обрезки ---
    return ClipRect(
      child: CustomPaint(
        painter: _StripePainter(stripeColor: stripeColor, backgroundColor: backgroundColor),
        child: Container(),
      ),
    );
  }
}

class _StripePainter extends CustomPainter {
  final Color stripeColor;
  final Color backgroundColor;
  
  _StripePainter({required this.stripeColor, required this.backgroundColor});

  @override
  void paint(Canvas canvas, Size size) {
    final backgroundPaint = Paint()..color = backgroundColor;
    canvas.drawRect(Rect.fromLTWH(0, 0, size.width, size.height), backgroundPaint);

    final stripePaint = Paint()
      ..color = stripeColor
      ..strokeWidth = 2.0
      ..style = PaintingStyle.stroke;

    for (double i = -size.height; i < size.width; i += 10) {
      canvas.drawLine(Offset(i, 0), Offset(i + size.height, size.height), stripePaint);
    }
  }

  @override
  bool shouldRepaint(_StripePainter oldDelegate) {
    return stripeColor != oldDelegate.stripeColor || backgroundColor != oldDelegate.backgroundColor;
  }
}
