import 'dart:async';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:intl/intl.dart';
import 'package:intl/date_symbol_data_local.dart';
import 'package:telegram_web_app/telegram_web_app.dart';
import 'package:try_neuro/core/network/time_service.dart';
import 'package:try_neuro/core/offline/sync_service.dart';
import 'package:try_neuro/features/auth/login_screen.dart';
import 'package:try_neuro/service_locator.dart';

void main() {
  runZonedGuarded(() async {
    WidgetsFlutterBinding.ensureInitialized();
    setupServiceLocator();
    
    // --- ИНИЦИАЛИЗАЦИЯ TELEGRAM (ПОПЫТКА 1) ---
    if (kIsWeb) {
      try {
        final tg = TelegramWebApp.instance;
        if (tg.isSupported) {
          tg.ready();
          tg.expand();
        }
      } catch (e) {
        print('Initial TG error: $e');
      }
    }

    sl<SyncService>().start();
    await sl<TimeService>().sync();
    await initializeDateFormatting('ru_RU', null);
    Intl.defaultLocale = 'ru_RU';
    
    runApp(const MyApp());

  }, (error, stackTrace) {
    print('Caught error: $error');
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
    // --- ИНИЦИАЛИЗАЦИЯ TELEGRAM (ПОПЫТКА 2 - когда Flutter готов) ---
    if (kIsWeb) {
      WidgetsBinding.instance.addPostFrameCallback((_) {
        try {
          final tg = TelegramWebApp.instance;
          if (tg.isSupported) {
            tg.ready();
            print('Telegram Ready signal sent from FrameCallback');
          }
        } catch (e) {
          print('Post-frame TG error: $e');
        }
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final tg = TelegramWebApp.instance;
    final isTg = tg.isSupported;
    
    return MaterialApp(
      title: 'Try Neuro CRM',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(
          seedColor: (isTg && tg.themeParams.buttonColor != null) 
              ? tg.themeParams.buttonColor! 
              : Colors.blue,
          brightness: (isTg && tg.colorScheme == TelegramColorScheme.dark) 
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
