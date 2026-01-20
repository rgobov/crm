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
  // Максимально стандартный запуск без лишних оберток
  WidgetsFlutterBinding.ensureInitialized();
  setupServiceLocator();
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    // Фоновые задачи запускаем через минимальную задержку
    Timer.run(() async {
      await initializeDateFormatting('ru_RU', null);
      Intl.defaultLocale = 'ru_RU';
      
      if (kIsWeb && PlatformUtils.instance.isTelegramSupported) {
        PlatformUtils.instance.ready();
      }
      
      sl<SyncService>().start();
      sl<TimeService>().sync().catchError((e) => debugPrint('Sync error: $e'));
    });

    return MaterialApp(
      title: 'Try Neuro CRM',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        useMaterial3: true,
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.blue),
      ),
      localizationsDelegates: const [
        GlobalMaterialLocalizations.delegate,
        GlobalWidgetsLocalizations.delegate,
        GlobalCupertinoLocalizations.delegate,
      ],
      supportedLocales: const [Locale('ru', 'RU')],
      home: const LoginScreen(), // Сразу идем на логин
    );
  }
}
