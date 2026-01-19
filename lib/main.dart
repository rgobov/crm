import 'dart:async';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:intl/intl.dart';
import 'package:intl/date_symbol_data_local.dart';
import 'package:try_neuro/core/network/time_service.dart';
import 'package:try_neuro/core/offline/sync_service.dart';
import 'package:try_neuro/features/auth/login_screen.dart';
import 'package:try_neuro/service_locator.dart';
import 'package:try_neuro/core/utils/platform_utils.dart'; // Наш новый враппер

void main() {
  runZonedGuarded(() async {
    WidgetsFlutterBinding.ensureInitialized();
    setupServiceLocator();
    
    // --- ИНИЦИАЛИЗАЦИЯ TELEGRAM ЧЕРЕЗ ВРАППЕР ---
    final platform = PlatformUtils.instance;
    if (platform.isTelegramSupported) {
      try {
        platform.ready();
        platform.expand();
      } catch (e) {
        debugPrint('Initial TG error: $e');
      }
    }

    sl<SyncService>().start();
    await sl<TimeService>().sync();
    await initializeDateFormatting('ru_RU', null);
    Intl.defaultLocale = 'ru_RU';
    
    runApp(const MyApp());

  }, (error, stackTrace) {
    debugPrint('Caught error: $error');
  });
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
    // Повторный вызов ready через враппер
    WidgetsBinding.instance.addPostFrameCallback((_) {
      final platform = PlatformUtils.instance;
      if (platform.isTelegramSupported) {
        platform.ready();
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    final platform = PlatformUtils.instance;
    final isTg = platform.isTelegramSupported;
    
    return MaterialApp(
      title: 'Try Neuro CRM',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(
          seedColor: (isTg && platform.telegramButtonColor != null) 
              ? platform.telegramButtonColor! 
              : Colors.blue,
          brightness: (isTg && platform.isTelegramDarkMode)
              ? Brightness.dark 
              : Brightness.light,
        ),
        useMaterial3: true,
      ),
      localizationsDelegates: const [
        GlobalMaterialLocalizations.delegate,
        GlobalWidgetsLocalizations.delegate,
        GlobalCupertinoLocalizations.delegate,
      ],
      supportedLocales: const [
        Locale('ru', 'RU'),
      ],
      home: const LoginScreen(),
    );
  }
}
