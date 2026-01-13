import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:try_neuro/features/manager/data/manager_service.dart';
import 'package:try_neuro/features/manager/domain/wappi_settings_model.dart';
import 'package:try_neuro/service_locator.dart';

class WappiSettingsScreen extends StatefulWidget {
  const WappiSettingsScreen({super.key});

  @override
  State<WappiSettingsScreen> createState() => _WappiSettingsScreenState();
}

class _WappiSettingsScreenState extends State<WappiSettingsScreen> {
  final _managerService = sl<ManagerService>();
  final _formKey = GlobalKey<FormState>();

  late TextEditingController _apiKeyController;
  late TextEditingController _profileIdController;
  late TextEditingController _templateController;
  
  bool _isEnabled = false;
  String _messengerType = 'TELEGRAM';
  int _leadTimeMinutes = 1440; // 24 часа по умолчанию
  bool _isLoading = true;
  bool _isSaving = false;

  @override
  void initState() {
    super.initState();
    _apiKeyController = TextEditingController();
    _profileIdController = TextEditingController();
    _templateController = TextEditingController();
    _loadSettings();
  }

  Future<void> _loadSettings() async {
    try {
      final settings = await _managerService.getWappiSettings();
      setState(() {
        _apiKeyController.text = settings.apiKey;
        _profileIdController.text = settings.profileId;
        _templateController.text = settings.reminderTemplate;
        _isEnabled = settings.isEnabled;
        _messengerType = settings.messengerType;
        _leadTimeMinutes = settings.leadTimeMinutes;
        _isLoading = false;
      });
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Ошибка загрузки: $e')));
        setState(() => _isLoading = false);
      }
    }
  }

  void _showTimerPicker() {
    // Вычисляем текущие часы и минуты из общего количества минут
    int initialHours = _leadTimeMinutes ~/ 60;
    int initialMinutes = _leadTimeMinutes % 60;

    showModalBottomSheet(
      context: context,
      builder: (BuildContext context) {
        return Container(
          height: 300,
          color: Colors.white,
          child: Column(
            children: [
              Container(
                color: Colors.grey.shade100,
                padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    TextButton(onPressed: () => Navigator.pop(context), child: const Text('Отмена')),
                    const Text('Время напоминания', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
                    TextButton(
                      onPressed: () => Navigator.pop(context), 
                      child: const Text('Готово', style: TextStyle(fontWeight: FontWeight.bold))
                    ),
                  ],
                ),
              ),
              Expanded(
                child: CupertinoTimerPicker(
                  mode: CupertinoTimerPickerMode.hm,
                  initialTimerDuration: Duration(hours: initialHours, minutes: initialMinutes),
                  onTimerDurationChanged: (Duration duration) {
                    setState(() {
                      _leadTimeMinutes = duration.inMinutes;
                    });
                  },
                ),
              ),
            ],
          ),
        );
      },
    );
  }

  Future<void> _saveSettings() async {
    if (!_formKey.currentState!.validate()) return;

    setState(() => _isSaving = true);
    try {
      final settings = WappiSettings(
        apiKey: _apiKeyController.text.trim(),
        profileId: _profileIdController.text.trim(),
        isEnabled: _isEnabled,
        reminderTemplate: _templateController.text.trim(),
        messengerType: _messengerType,
        leadTimeMinutes: _leadTimeMinutes,
      );

      await _managerService.updateWappiSettings(settings);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Настройки сохранены')));
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Ошибка сохранения: $e')));
      }
    } finally {
      if (mounted) setState(() => _isSaving = false);
    }
  }

  String _formatLeadTime() {
    int hours = _leadTimeMinutes ~/ 60;
    int minutes = _leadTimeMinutes % 60;
    String res = '';
    if (hours > 0) res += '$hours ч. ';
    if (minutes > 0) res += '$minutes мин.';
    if (res.isEmpty) res = '0 мин.';
    return res;
  }

  @override
  void dispose() {
    _apiKeyController.dispose();
    _profileIdController.dispose();
    _templateController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Настройки уведомлений')),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : Form(
              key: _formKey,
              child: SingleChildScrollView(
                padding: const EdgeInsets.all(16.0),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    Card(
                      elevation: 0,
                      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12), side: BorderSide(color: Colors.grey.shade200)),
                      child: SwitchListTile(
                        title: const Text('Автоматические напоминания'),
                        subtitle: Text(_isEnabled ? 'Включены' : 'Выключены'),
                        value: _isEnabled,
                        onChanged: (val) => setState(() => _isEnabled = val),
                      ),
                    ),
                    const SizedBox(height: 16),
                    
                    ListTile(
                      tileColor: Colors.white,
                      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12), side: BorderSide(color: Colors.grey.shade200)),
                      title: const Text('Когда отправлять напоминание?'),
                      subtitle: Text('За ${_formatLeadTime()} до визита'),
                      trailing: const Icon(Icons.timer_outlined, color: Colors.blue),
                      onTap: _showTimerPicker,
                    ),
                    
                    const SizedBox(height: 24),
                    const Text('ТЕХНИЧЕСКИЕ НАСТРОЙКИ', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 12, color: Colors.grey)),
                    const SizedBox(height: 8),
                    TextFormField(
                      controller: _apiKeyController,
                      decoration: const InputDecoration(labelText: 'Wappi API Key', border: OutlineInputBorder()),
                      obscureText: true,
                    ),
                    const SizedBox(height: 16),
                    TextFormField(
                      controller: _profileIdController,
                      decoration: const InputDecoration(labelText: 'Wappi Profile ID', border: OutlineInputBorder()),
                    ),
                    const SizedBox(height: 16),
                    DropdownButtonFormField<String>(
                      value: _messengerType,
                      decoration: const InputDecoration(labelText: 'Мессенджер', border: OutlineInputBorder()),
                      items: const [
                        DropdownMenuItem(value: 'TELEGRAM', child: Text('Telegram')),
                        DropdownMenuItem(value: 'WHATSAPP', child: Text('WhatsApp')),
                      ],
                      onChanged: (val) => setState(() => _messengerType = val!),
                    ),
                    
                    const SizedBox(height: 24),
                    const Text('ШАБЛОН СООБЩЕНИЯ', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 12, color: Colors.grey)),
                    const SizedBox(height: 8),
                    TextFormField(
                      controller: _templateController,
                      maxLines: 5,
                      decoration: const InputDecoration(border: OutlineInputBorder(), hintText: 'Текст напоминания...'),
                    ),
                    const SizedBox(height: 8),
                    const Wrap(
                      spacing: 8,
                      children: [
                        _TagChip(tag: '{name}', label: 'Имя'),
                        _TagChip(tag: '{service}', label: 'Услуга'),
                        _TagChip(tag: '{date}', label: 'Дата'),
                        _TagChip(tag: '{time}', label: 'Время'),
                        _TagChip(tag: '{master}', label: 'Мастер'),
                      ],
                    ),
                    const SizedBox(height: 32),
                    ElevatedButton(
                      onPressed: _isSaving ? null : _saveSettings,
                      style: ElevatedButton.styleFrom(
                        padding: const EdgeInsets.symmetric(vertical: 16),
                        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                        elevation: 0,
                      ),
                      child: _isSaving 
                        ? const CircularProgressIndicator(color: Colors.white) 
                        : const Text('СОХРАНИТЬ', style: TextStyle(fontWeight: FontWeight.bold)),
                    ),
                  ],
                ),
              ),
            ),
    );
  }
}

class _TagChip extends StatelessWidget {
  final String tag;
  final String label;
  const _TagChip({required this.tag, required this.label});

  @override
  Widget build(BuildContext context) {
    return ActionChip(
      label: Text(tag, style: const TextStyle(fontSize: 11, fontWeight: FontWeight.bold, color: Colors.blue)),
      onPressed: () {},
      tooltip: label,
    );
  }
}
