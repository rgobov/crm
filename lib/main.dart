
import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:intl/date_symbol_data_local.dart';
import 'package:try_neuro/core/offline/sync_service.dart';
import 'package:try_neuro/features/auth/login_screen.dart';
import 'package:try_neuro/service_locator.dart';

// 1. Делаем функцию main асинхронной
void main() async {
  // 2. Эта строка - КЛЮЧ К РЕШЕНИЮ.
  // Она гарантирует, что все сервисы Flutter будут готовы к работе.
  WidgetsFlutterBinding.ensureInitialized();

  // Теперь безопасно инициализируем сервисы
  setupServiceLocator();

  // Запускаем сервис синхронизации
  sl<SyncService>().start();

  // Инициализируем данные для локализации дат
  await initializeDateFormatting('ru_RU', null);

  runZonedGuarded(() {
      runApp(const MyApp());
    },
    (error, stackTrace) {
      print('Caught error: $error');
      print(stackTrace);
    },
  );
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'CRM',
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.blue),
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
