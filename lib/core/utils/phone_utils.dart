import 'package:flutter/services.dart';

class PhoneUtils {
  static String format(String? phone) {
    if (phone == null || phone.isEmpty) return '';
    String digits = phone.replaceAll(RegExp(r'[^0-9]'), '');
    if (digits.length == 11) {
      return '+${digits[0]} (${digits.substring(1, 4)}) ${digits.substring(4, 7)}-${digits.substring(7, 9)}-${digits.substring(9, 11)}';
    } else if (digits.length == 10) {
      return '+7 (${digits.substring(0, 3)}) ${digits.substring(3, 6)}-${digits.substring(6, 8)}-${digits.substring(8, 10)}';
    }
    return digits.isNotEmpty ? '+$digits' : '';
  }

  static String clean(String phone) {
    return phone.replaceAll(RegExp(r'[^0-9]'), '');
  }
}

class RussianPhoneInputFormatter extends TextInputFormatter {
  @override
  TextEditingValue formatEditUpdate(
    TextEditingValue oldValue,
    TextEditingValue newValue,
  ) {
    final String newText = newValue.text;
    final String oldText = oldValue.text;

    if (newText.isEmpty) {
      return const TextEditingValue(text: '', selection: TextSelection.collapsed(offset: 0));
    }

    // 1. Получаем "цифровой скелет" нового ввода
    String digits = newText.replaceAll(RegExp(r'[^0-9]'), '');

    // 2. Если пользователь нажал Backspace, и курсор стоял сразу после разделителя
    if (newText.length < oldText.length) {
      final int selectionEnd = newValue.selection.end;
      // Проверяем, не удалил ли пользователь разделитель. 
      // Если да - удаляем цифру перед этим разделителем.
      if (selectionEnd > 0 && selectionEnd < oldText.length) {
         final String charDeleted = oldText[selectionEnd];
         if (RegExp(r'[^0-9]').hasMatch(charDeleted)) {
            // Пользователь пытался стереть разделитель. 
            // Нужно найти позицию последней цифры ПЕРЕД курсором и убрать её.
            int digitsBefore = oldText.substring(0, selectionEnd).replaceAll(RegExp(r'[^0-9]'), '').length;
            if (digitsBefore > 0) {
               String oldDigits = oldText.replaceAll(RegExp(r'[^0-9]'), '');
               digits = oldDigits.substring(0, digitsBefore - 1) + oldDigits.substring(digitsBefore);
            }
         }
      }
    }

    // Ограничение 11 цифр и авто-замена 8 -> 7
    if (digits.length > 11) digits = digits.substring(0, 11);
    if (digits.startsWith('8')) digits = '7' + digits.substring(1);

    // 3. Накладываем цифры на шаблон
    final StringBuffer res = StringBuffer();
    const template = '+# (###) ###-##-##';
    int digitIdx = 0;
    int templateIdx = 0;

    while (templateIdx < template.length && digitIdx < digits.length) {
      if (template[templateIdx] == '#') {
        res.write(digits[digitIdx]);
        digitIdx++;
      } else {
        res.write(template[templateIdx]);
      }
      templateIdx++;
    }

    final String resultText = res.toString();

    // 4. УМНЫЙ РАСЧЕТ КУРСОРA
    // Считаем, сколько цифр было в новом вводе ДО курсора
    int digitsBeforeCursor = newValue.text.substring(0, newValue.selection.end).replaceAll(RegExp(r'[^0-9]'), '').length;
    
    // Если мы удаляли и попали на разделитель, корректируем счетчик
    if (newText.length < oldText.length && digitsBeforeCursor > digits.length) {
      digitsBeforeCursor = digits.length;
    }

    // Ищем позицию в отформатированной строке, которая соответствует этому количеству цифр
    int finalCursorPos = 0;
    int currentDigitsCount = 0;
    for (int i = 0; i < resultText.length; i++) {
      if (RegExp(r'[0-9]').hasMatch(resultText[i])) {
        currentDigitsCount++;
      }
      finalCursorPos = i + 1;
      if (currentDigitsCount == digitsBeforeCursor) break;
    }

    // Если цифр нет, курсор в начало
    if (digits.isEmpty) finalCursorPos = 0;

    return TextEditingValue(
      text: resultText,
      selection: TextSelection.collapsed(offset: finalCursorPos),
    );
  }
}
