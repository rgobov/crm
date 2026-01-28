import 'dart:async';
import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:intl/intl.dart';
import 'package:intl/date_symbol_data_local.dart';
import 'package:try_neuro/core/network/time_service.dart';
import 'package:try_neuro/core/offline/sync_service.dart';
import 'package:try_neuro/features/auth/login_screen.dart';
import 'package:try_neuro/service_locator.dart';
import 'package:try_neuro/core/utils/platform_utils.dart';
import 'package:flutter/foundation.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  setupServiceLocator();
  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  @override
  void initState() {
    super.initState();
    _initApp();
  }

  Future<void> _initApp() async {
    await initializeDateFormatting('ru_RU', null);
    Intl.defaultLocale = 'ru_RU';
    
    if (kIsWeb && PlatformUtils.instance.isTelegramSupported) {
      PlatformUtils.instance.ready();
      // Убрали принудительный expand() здесь, чтобы не блокировать клавиатуру
    }
    
    sl<SyncService>().start();
    sl<TimeService>().sync().catchError((e) => debugPrint('Sync error: $e'));
  }

  @override
  Widget build(BuildContext context) {
    final Color primaryColor = PlatformUtils.instance.telegramButtonColor ?? Colors.blue;
    final bool isDark = PlatformUtils.instance.isTelegramDarkMode;

    return MaterialApp(
      title: '999 CRM',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        useMaterial3: true,
        brightness: isDark ? Brightness.dark : Brightness.light,
        colorScheme: ColorScheme.fromSeed(
          seedColor: primaryColor,
          brightness: isDark ? Brightness.dark : Brightness.light,
        ),
        // ВАЖНО ДЛЯ КЛАВИАТУРЫ: отключаем автоматическое прокручивание
        visualDensity: VisualDensity.adaptivePlatformDensity,
        elevatedButtonTheme: ElevatedButtonThemeData(
          style: ElevatedButton.styleFrom(
            backgroundColor: primaryColor,
            foregroundColor: Colors.white,
            minimumSize: const Size(double.infinity, 50),
            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
          ),
        ),
      ),
      localizationsDelegates: const [
        GlobalMaterialLocalizations.delegate,
        GlobalWidgetsLocalizations.delegate,
        GlobalCupertinoLocalizations.delegate,
      ],
      supportedLocales: const [Locale('ru', 'RU')],
      home: const LoginScreen(),
    );
  }
}
