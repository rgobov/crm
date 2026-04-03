import 'dart:async';
import 'package:flutter/foundation.dart';
import 'package:stomp_dart_client/stomp_dart_client.dart';
import 'package:try_neuro/core/config/app_config.dart';
import 'package:try_neuro/core/session/session_service.dart';
import 'package:try_neuro/service_locator.dart';

class WebSocketService {
  final _sessionService = sl<SessionService>();
  StompClient? _client;
  
  final _scheduleUpdateController = StreamController<String>.broadcast();
  Stream<String> get scheduleUpdates => _scheduleUpdateController.stream;

  void init() async {
    final user = await _sessionService.getCurrentUser();
    final tenantId = user?.tenantId;
    
    if (tenantId == null) {
      debugPrint('WS Error: Cannot initialize without tenantId');
      return;
    }

    // --- ОБНОВЛЕННАЯ ЛОГИКА ОПРЕДЕЛЕНИЯ URL ---
    String serverUrl;
    if (AppConfig.isProduction) {
      serverUrl = AppConfig.productionUrl;
    } else {
      if (!kIsWeb && defaultTargetPlatform == TargetPlatform.android) {
        // Учитываем флаг мобильного теста для WebSocket
        serverUrl = AppConfig.isMobileTest 
            ? AppConfig.developmentUrlAndroidDevice 
            : AppConfig.developmentUrlAndroidEmulator;
      } else {
        serverUrl = AppConfig.developmentUrlDefault;
      }
    }

    // Заменяем http на ws (или https на wss) и добавляем точку входа /ws
    final String wsUrl = serverUrl.replaceFirst('http', 'ws') + '/ws';

    _client = StompClient(
      config: StompConfig(
        url: wsUrl,
        onConnect: (frame) => _onConnect(frame, tenantId),
        onWebSocketError: (dynamic error) => debugPrint('WS Connection Error: $error'),
        onStompError: (frame) => debugPrint('STOMP Protocol Error: ${frame.body}'),
        reconnectDelay: const Duration(seconds: 5),
      ),
    );

    _client?.activate();
  }

  void _onConnect(StompFrame frame, String tenantId) {
    debugPrint('WebSocket Connected for Tenant: $tenantId');

    _client?.subscribe(
      destination: '/topic/schedule/$tenantId',
      callback: (frame) {
        if (frame.body == 'refresh') {
          debugPrint('WS: New update signal received from server');
          _scheduleUpdateController.add('refresh');
        }
      },
    );
  }

  void dispose() {
    _client?.deactivate();
    _scheduleUpdateController.close();
  }
}
