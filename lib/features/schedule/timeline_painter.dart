import 'package:flutter/material.dart';

// Упрощаем: теперь это просто отрисовщик, а не StatefulWidget
class TimelinePainter extends CustomPainter {
  final TimeOfDay timeNow;
  final double hourHeight;

  TimelinePainter({required this.timeNow, required this.hourHeight});

  @override
  void paint(Canvas canvas, Size size) {
    final paint = Paint()
      ..color = Colors.red
      ..strokeWidth = 1.5;

    // Вычисляем вертикальную позицию линии
    final topPosition = (timeNow.hour + timeNow.minute / 60.0) * hourHeight;

    // Рисуем красную линию через всю область
    canvas.drawLine(Offset(0, topPosition), Offset(size.width, topPosition), paint);

    // Рисуем кружок в начале линии для акцента
    canvas.drawCircle(Offset(0, topPosition), 4.0, paint);
  }

  @override
  bool shouldRepaint(covariant TimelinePainter oldDelegate) {
    // Перерисовываем, только если изменилось время
    return oldDelegate.timeNow != timeNow;
  }
}
