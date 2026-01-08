import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:try_neuro/core/session/session_service.dart';
import 'package:try_neuro/features/auth/domain/user_model.dart';
import 'package:try_neuro/features/manager/data/manager_service.dart';
import 'package:try_neuro/features/schedule/domain/appointment_comment_model.dart';
import 'package:try_neuro/features/staff/data/employee_service.dart';
import 'package:try_neuro/service_locator.dart';

class CommentThreadScreen extends StatefulWidget {
  final String appointmentId;

  const CommentThreadScreen({super.key, required this.appointmentId});

  @override
  State<CommentThreadScreen> createState() => _CommentThreadScreenState();
}

class _CommentThreadScreenState extends State<CommentThreadScreen> {
  final _sessionService = sl<SessionService>();
  final _managerService = sl<ManagerService>();
  final _employeeService = sl<EmployeeService>();
  final _textController = TextEditingController();
  
  late Future<List<AppointmentComment>> _commentsFuture;
  User? _currentUser;
  bool _isSending = false;

  @override
  void initState() {
    super.initState();
    _loadData();
  }

  void _loadData() {
    setState(() {
      _commentsFuture = _fetchComments();
    });
  }

  Future<List<AppointmentComment>> _fetchComments() async {
    _currentUser ??= await _sessionService.getCurrentUser();
    if (_currentUser?.role == UserRole.manager) {
      return _managerService.getComments(widget.appointmentId);
    } else {
      return _employeeService.getComments(widget.appointmentId);
    }
  }

  Future<void> _addComment() async {
    final text = _textController.text.trim();
    if (text.isEmpty || _isSending) return;

    setState(() => _isSending = true);

    try {
      if (_currentUser?.role == UserRole.manager) {
        await _managerService.addComment(widget.appointmentId, text);
      } else {
        await _employeeService.addComment(widget.appointmentId, text);
      }
      _textController.clear();
      _loadData(); // Перезагружаем комментарии
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Ошибка отправки: ${e.toString()}')));
      }
    } finally {
      if (mounted) {
        setState(() => _isSending = false);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Комментарии к записи')),
      body: Column(
        children: [
          Expanded(
            child: FutureBuilder<List<AppointmentComment>>(
              future: _commentsFuture,
              builder: (context, snapshot) {
                if (snapshot.connectionState == ConnectionState.waiting) {
                  return const Center(child: CircularProgressIndicator());
                }
                if (snapshot.hasError) {
                  return Center(child: Text('Ошибка загрузки: ${snapshot.error}'));
                }
                final comments = snapshot.data ?? [];
                if (comments.isEmpty) {
                  return const Center(child: Text('Комментариев пока нет.'));
                }
                return ListView.builder(
                  padding: const EdgeInsets.all(8.0),
                  itemCount: comments.length,
                  itemBuilder: (context, index) {
                    final comment = comments[index];
                    final isMe = comment.authorId == _currentUser?.staffId;
                    return _buildCommentBubble(comment, isMe);
                  },
                );
              },
            ),
          ),
          _buildMessageInput(),
        ],
      ),
    );
  }

  Widget _buildCommentBubble(AppointmentComment comment, bool isMe) {
    final align = isMe ? CrossAxisAlignment.end : CrossAxisAlignment.start;
    final color = isMe ? Colors.blue.shade100 : Colors.grey.shade200;

    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4.0),
      child: Column(
        crossAxisAlignment: align,
        children: [
          Container(
            constraints: BoxConstraints(maxWidth: MediaQuery.of(context).size.width * 0.75),
            padding: const EdgeInsets.symmetric(horizontal: 12.0, vertical: 8.0),
            decoration: BoxDecoration(color: color, borderRadius: BorderRadius.circular(16)),
            child: Text(comment.text),
          ),
          const SizedBox(height: 2),
          Text(
            '${comment.authorName} • ${DateFormat.Hm().format(comment.createdAt.toLocal())}',
            style: Theme.of(context).textTheme.bodySmall,
          ),
        ],
      ),
    );
  }

  // --- ИЗМЕНЕНИЕ ЗДЕСЬ ---
  Widget _buildMessageInput() {
    return Container(
      padding: const EdgeInsets.all(8.0),
      decoration: BoxDecoration(color: Theme.of(context).scaffoldBackgroundColor, boxShadow: [BoxShadow(blurRadius: 4, color: Colors.black.withOpacity(0.1))]),
      child: Row(
        children: [
          Expanded(
            child: TextField(
              controller: _textController,
              decoration: const InputDecoration(
                hintText: 'Введите комментарий...', 
                border: InputBorder.none,
                counterText: "", // Убираем стандартный счетчик, чтобы не мешал
              ),
              maxLines: 3,
              minLines: 1,
              maxLength: 500, // <<< ДОБАВЛЯЕМ ОГРАНИЧЕНИЕ
            ),
          ),
          IconButton(
            icon: _isSending ? const SizedBox(width: 24, height: 24, child: CircularProgressIndicator(strokeWidth: 2)) : const Icon(Icons.send),
            onPressed: _addComment,
          ),
        ],
      ),
    );
  }
}
