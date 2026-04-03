import 'package:flutter/material.dart';
import 'package:telegram_web_app/telegram_web_app.dart';
import 'platform_utils.dart';

class PlatformImplementation extends PlatformUtils {
  @override
  bool get isTelegramSupported => TelegramWebApp.instance.isSupported;

  @override
  void ready() => TelegramWebApp.instance.ready();

  @override
  void expand() => TelegramWebApp.instance.expand();

  @override
  Color? get telegramButtonColor => TelegramWebApp.instance.themeParams.buttonColor;

  @override
  bool get isTelegramDarkMode => TelegramWebApp.instance.colorScheme == TelegramColorScheme.dark;
}

PlatformUtils getPlatformImplementation() => PlatformImplementation();
