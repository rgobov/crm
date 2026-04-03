import 'package:flutter/material.dart';
import 'platform_utils.dart';

class PlatformImplementation extends PlatformUtils {
  @override
  bool get isTelegramSupported => false;

  @override
  void ready() {}

  @override
  void expand() {}

  @override
  Color? get telegramButtonColor => null;

  @override
  bool get isTelegramDarkMode => false;
}

PlatformUtils getPlatformImplementation() => PlatformImplementation();
