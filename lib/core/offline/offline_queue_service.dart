
import 'dart:convert';
import 'dart:io';

import 'package:path_provider/path_provider.dart';

// Модель для одной операции в очереди
class OfflineOperation {
  final String type; // e.g., 'add_contact', 'delete_appointment'
  final Map<String, dynamic> data;

  OfflineOperation({required this.type, required this.data});

  Map<String, dynamic> toJson() => {'type': type, 'data': data};

  factory OfflineOperation.fromJson(Map<String, dynamic> json) {
    return OfflineOperation(
      type: json['type'],
      data: json['data'],
    );
  }
}

// Сервис для управления очередью офлайн-операций
class OfflineQueueService {
  static const _fileName = 'offline_queue.json';

  Future<File> get _localFile async {
    final directory = await getApplicationDocumentsDirectory();
    return File('${directory.path}/$_fileName');
  }

  Future<List<OfflineOperation>> getQueue() async {
    try {
      final file = await _localFile;
      if (!await file.exists()) {
        return [];
      }
      final contents = await file.readAsString();
      if (contents.isEmpty) {
        return [];
      }
      final List<dynamic> jsonList = jsonDecode(contents);
      return jsonList.map((json) => OfflineOperation.fromJson(json)).toList();
    } catch (e) {
      // Если файл поврежден, считаем его пустым
      return [];
    }
  }

  Future<void> addToQueue(OfflineOperation operation) async {
    final queue = await getQueue();
    queue.add(operation);
    final file = await _localFile;
    await file.writeAsString(jsonEncode(queue.map((op) => op.toJson()).toList()));
  }

  Future<void> clearQueue() async {
    final file = await _localFile;
    if (await file.exists()) {
      await file.delete();
    }
  }
}
