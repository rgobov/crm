import 'dart:async';
import 'package:flutter/foundation.dart';
import 'dart:js' as js;

class KeyboardUtils {
  static void forceShowKeyboard() {
    if (!kIsWeb) return;
    
    try {
      // Вызываем JS функцию, которую мы добавили в index.html
      js.context.callMethod('triggerMobileKeyboard');
    } catch (e) {
      debugPrint("Keyboard hack failed: $e");
    }
  }

  /// Помощник для полей ввода: вызывает хак и забирает фокус обратно через 100мс
  static void onTextFieldTap(FocusNode focusNode) {
    if (!kIsWeb) return;
    
    forceShowKeyboard();
    
    Timer(const Duration(milliseconds: 100), () {
      if (focusNode.canRequestFocus) {
        focusNode.requestFocus();
      }
    });
  }
}
