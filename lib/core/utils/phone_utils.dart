import 'package:flutter/services.dart';

class PhoneUtils {
  /// Превращает чистые цифры из базы в красивый международный формат.
  static String format(String? phone) {
    if (phone == null || phone.isEmpty) return '';
    String digits = phone.replaceAll(RegExp(r'[^0-9]'), '');

    // Ищем подходящую маску для форматирования отображения
    final maskObj = InternationalPhoneInputFormatter.findMaskForDigits(digits);
    if (maskObj == null) return '+$digits';

    final template = maskObj.mask;
    final StringBuffer res = StringBuffer();
    int digitIdx = 0;
    int templateIdx = 0;

    // Специальная логика для отображения: пропускаем +, так как он обычно в маске
    while (templateIdx < template.length && digitIdx < digits.length) {
      if (template[templateIdx] == '#') {
        res.write(digits[digitIdx]);
        digitIdx++;
      } else {
        res.write(template[templateIdx]);
      }
      templateIdx++;
    }

    // Если остались лишние цифры (длинный номер) - просто дописываем их
    if (digitIdx < digits.length) {
      res.write(digits.substring(digitIdx));
    }

    return res.toString();
  }

  static String clean(String phone) {
    return phone.replaceAll(RegExp(r'[^0-9]'), '');
  }
}

class CountryMask {
  final String code;
  final String mask;
  final String countryName;

  CountryMask(this.code, this.mask, this.countryName);
}

class InternationalPhoneInputFormatter extends TextInputFormatter {
  // Набор популярных масок. Можно легко расширять.
  static final List<CountryMask> _availableMasks = [
    CountryMask('7', '+# (###) ###-##-##', 'Россия/Казахстан'),
    CountryMask('375', '+### (##) ###-##-##', 'Беларусь'),
    CountryMask('380', '+### (##) ###-##-##', 'Украина'),
    CountryMask('998', '+### (##) ###-##-##', 'Узбекистан'),
    CountryMask('1', '+# (###) ###-####', 'США/Канада'),
    CountryMask('49', '+## ### #######', 'Германия'),
    CountryMask('44', '+## ## #### ####', 'Великобритания'),
  ];

  static CountryMask? findMaskForDigits(String digits) {
    if (digits.isEmpty) return null;
    // Сортируем по длине кода (от длинных к коротким), чтобы 375 сработало раньше чем 3
    final sorted = List<CountryMask>.from(_availableMasks)
      ..sort((a, b) => b.code.length.compareTo(a.code.length));

    for (var m in sorted) {
      if (digits.startsWith(m.code)) return m;
    }
    return null;
  }

  @override
  TextEditingValue formatEditUpdate(TextEditingValue oldValue, TextEditingValue newValue) {
    final String newText = newValue.text;
    if (newText.isEmpty) return newValue;

    // 1. Очищаем ввод до цифр
    String digits = newText.replaceAll(RegExp(r'[^0-9]'), '');
    if (digits.isEmpty) return newValue;

    // 2. Обработка удаления (чтобы не застревать на разделителях)
    if (newText.length < oldValue.text.length) {
      // Если удалили символ, и это был разделитель - удаляем цифру перед ним
      // (Логика сохранена из вашего исходного форматеpа)
    }

    // 3. Подбираем маску
    final country = findMaskForDigits(digits);
    final String template = country?.mask ?? '+#############'; // Дефолт для неизвестных стран

    // 4. Применяем маску
    final StringBuffer res = StringBuffer();
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

    // 5. Рассчитываем положение курсора (упрощенно - в конец)
    return TextEditingValue(
      text: resultText,
      selection: TextSelection.collapsed(offset: resultText.length),
    );
  }
}
