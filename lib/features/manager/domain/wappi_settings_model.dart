class WappiSettings {
  final String? id;
  final String? tenantId;
  final String apiKey;
  final String profileId;
  final bool isEnabled;
  final String reminderTemplate;
  final String messengerType;
  final int leadTimeMinutes;

  WappiSettings({
    this.id,
    this.tenantId,
    required this.apiKey,
    required this.profileId,
    required this.isEnabled,
    required this.reminderTemplate,
    required this.messengerType,
    required this.leadTimeMinutes,
  });

  factory WappiSettings.fromJson(Map<String, dynamic> json) {
    return WappiSettings(
      id: json['id'],
      tenantId: json['tenantId'],
      apiKey: json['apiKey'] ?? '',
      profileId: json['profileId'] ?? '',
      // Используем ключ 'enabled', как того ожидает бэкенд
      isEnabled: json['enabled'] ?? false,
      reminderTemplate: json['reminderTemplate'] ?? '',
      messengerType: json['messengerType'] ?? 'TELEGRAM',
      leadTimeMinutes: json['leadTimeMinutes'] ?? 1440,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'tenantId': tenantId,
      'apiKey': apiKey,
      'profileId': profileId,
      // --- ИСПРАВЛЕНИЕ: Ключ должен быть 'enabled' ---
      'enabled': isEnabled,
      'reminderTemplate': reminderTemplate,
      'messengerType': messengerType,
      'leadTimeMinutes': leadTimeMinutes,
    };
  }
}
