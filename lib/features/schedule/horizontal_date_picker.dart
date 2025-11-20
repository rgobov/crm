
import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

class HorizontalDatePicker extends StatefulWidget {
  final Function(DateTime) onDateSelected;
  final DateTime initialDate;

  const HorizontalDatePicker({
    super.key,
    required this.onDateSelected,
    required this.initialDate,
  });

  @override
  State<HorizontalDatePicker> createState() => _HorizontalDatePickerState();
}

class _HorizontalDatePickerState extends State<HorizontalDatePicker> {
  late DateTime _selectedDate;
  late ScrollController _scrollController;

  @override
  void initState() {
    super.initState();
    _selectedDate = widget.initialDate;
    _scrollController = ScrollController();
    // Прокручиваем к выбранной дате после построения
    WidgetsBinding.instance.addPostFrameCallback((_) {
      _scrollToDate(_selectedDate);
    });
  }

  @override
  void dispose() {
    _scrollController.dispose();
    super.dispose();
  }

  void _scrollToDate(DateTime date) {
    // Простая логика прокрутки: находим индекс (смещение от -50)
    // Индекс "сегодня" = 50.
    // Разница в днях:
    final diff = date.difference(DateTime.now()).inDays;
    final index = 50 + diff;
    // Ширина элемента (60) + отступы (8) = 68
    if (index >= 0 && index < 100) {
      final offset = (index * 68.0) - MediaQuery.of(context).size.width / 2 + 34;
      _scrollController.animateTo(
        offset,
        duration: const Duration(milliseconds: 300),
        curve: Curves.easeInOut,
      );
    }
  }

  bool _isSameDay(DateTime a, DateTime b) {
    return a.year == b.year && a.month == b.month && a.day == b.day;
  }

  @override
  Widget build(BuildContext context) {
    final today = DateTime.now();

    return Container(
      height: 90, // Немного увеличили высоту
      decoration: BoxDecoration(
        color: Colors.grey.shade50, // Легкий фон
        border: Border(
          top: BorderSide(color: Colors.grey.shade300), // Верхняя граница
        ),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.05),
            blurRadius: 4,
            offset: const Offset(0, -2),
          ),
        ],
      ),
      child: ListView.builder(
        controller: _scrollController,
        scrollDirection: Axis.horizontal,
        itemCount: 100,
        padding: const EdgeInsets.symmetric(horizontal: 8),
        itemBuilder: (context, index) {
          final date = DateTime.now().add(Duration(days: index - 50));
          final isSelected = _isSameDay(date, _selectedDate);
          final isToday = _isSameDay(date, today);
          final isWeekend = date.weekday == DateTime.saturday || date.weekday == DateTime.sunday;

          return GestureDetector(
            onTap: () {
              setState(() {
                _selectedDate = date;
              });
              widget.onDateSelected(date);
              _scrollToDate(date);
            },
            child: Container(
              width: 60,
              margin: const EdgeInsets.symmetric(horizontal: 4, vertical: 8),
              decoration: BoxDecoration(
                color: isSelected ? Theme.of(context).primaryColor : Colors.white,
                borderRadius: BorderRadius.circular(12),
                border: Border.all(
                  color: isSelected 
                      ? Theme.of(context).primaryColor 
                      : isToday 
                          ? Theme.of(context).primaryColor.withOpacity(0.5) // Рамка для сегодня
                          : Colors.transparent,
                  width: isToday && !isSelected ? 1.5 : 1,
                ),
                boxShadow: isSelected
                    ? [
                        BoxShadow(
                          color: Theme.of(context).primaryColor.withOpacity(0.4),
                          blurRadius: 6,
                          offset: const Offset(0, 3),
                        )
                      ]
                    : null,
              ),
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Text(
                    DateFormat.E('ru_RU').format(date).toUpperCase(),
                    style: TextStyle(
                      color: isSelected 
                          ? Colors.white 
                          : isWeekend 
                              ? Colors.red.shade400 // Выходные красным
                              : Colors.grey.shade600,
                      fontSize: 11,
                      fontWeight: FontWeight.w500,
                    ),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    date.day.toString(),
                    style: TextStyle(
                      color: isSelected 
                          ? Colors.white 
                          : isToday
                              ? Theme.of(context).primaryColor // Сегодня синим
                              : Colors.black87,
                      fontSize: 18,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                  if (isToday && !isSelected)
                    Container(
                      margin: const EdgeInsets.only(top: 4),
                      width: 4,
                      height: 4,
                      decoration: BoxDecoration(
                        color: Theme.of(context).primaryColor,
                        shape: BoxShape.circle,
                      ),
                    ),
                ],
              ),
            ),
          );
        },
      ),
    );
  }
}
