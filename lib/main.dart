import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:intl/date_symbol_data_local.dart';
import 'package:try_neuro/core/offline/sync_service.dart';
import 'package:try_neuro/features/auth/login_screen.dart';
import 'package:try_neuro/service_locator.dart';

void main() {
  // ПРАВИЛЬНАЯ СТРУКТУРА
  runZonedGuarded(() async {
    // Вся асинхронная инициализация должна быть ВНУТРИ runZonedGuarded
    WidgetsFlutterBinding.ensureInitialized();
    setupServiceLocator();
    sl<SyncService>().start();
    await initializeDateFormatting('ru_RU', null);
    
    runApp(const MyApp());

  }, (error, stackTrace) {
    print('Caught error: $error');
    print(stackTrace);
  });
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
