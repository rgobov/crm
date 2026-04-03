import 'package:flutter/material.dart';
import 'platform_utils_stub.dart'
    if (dart.library.js_interop) 'platform_utils_web.dart';

abstract class PlatformUtils {
  static PlatformUtils? _instance;

  static PlatformUtils get instance {
    _instance ??= getPlatformImplementation();
    return _instance!;
  }

  bool get isTelegramSupported;
  void ready();
  void expand();
  Color? get telegramButtonColor;
  bool get isTelegramDarkMode;
}
