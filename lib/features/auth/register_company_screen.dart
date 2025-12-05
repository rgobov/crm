
import 'package:flutter/material.dart';
import 'package:try_neuro/features/auth/data/auth_service.dart';
import 'package:try_neuro/service_locator.dart';

class RegisterCompanyScreen extends StatefulWidget {
  const RegisterCompanyScreen({super.key});

  @override
  State<RegisterCompanyScreen> createState() => _RegisterCompanyScreenState();
}

class _RegisterCompanyScreenState extends State<RegisterCompanyScreen> {
  final _formKey = GlobalKey<FormState>();
  final _companyNameController = TextEditingController();
  final _addressController = TextEditingController();
  final _emailController = TextEditingController();
  final _passwordController = TextEditingController();
  
  final AuthService _authService = sl<AuthService>();
  bool _isLoading = false;

  Future<void> _register() async {
    if (!_formKey.currentState!.validate()) return;

    setState(() => _isLoading = true);

    try {
      final success = await _authService.registerCompany(
        companyName: _companyNameController.text,
        address: _addressController.text,
        adminEmail: _emailController.text,
        adminPassword: _passwordController.text,
      );

      setState(() => _isLoading = false);

      if (success && mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Компания успешно зарегистрирована! Теперь войдите.')),
        );
        Navigator.pop(context); // Возвращаемся на экран логина
      } else if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Ошибка регистрации. Возможно, email уже занят.')),
        );
      }
    } catch (e) {
      setState(() => _isLoading = false);
       if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Ошибка: $e')),
        );
      }
    }
  }

  @override
  void dispose() {
    _companyNameController.dispose();
    _addressController.dispose();
    _emailController.dispose();
    _passwordController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Регистрация компании')),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16.0),
        child: Form(
          key: _formKey,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              const Text(
                'Создайте аккаунт для вашей организации',
                style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                textAlign: TextAlign.center,
              ),
              const SizedBox(height: 24),
              TextFormField(
                controller: _companyNameController,
                decoration: const InputDecoration(labelText: 'Название компании', border: OutlineInputBorder()),
                validator: (v) => v == null || v.isEmpty ? 'Введите название' : null,
              ),
              const SizedBox(height: 16),
              TextFormField(
                controller: _addressController,
                decoration: const InputDecoration(labelText: 'Адрес', border: OutlineInputBorder()),
              ),
              const SizedBox(height: 16),
              const Divider(),
              const SizedBox(height: 16),
              TextFormField(
                controller: _emailController,
                decoration: const InputDecoration(labelText: 'Email администратора', border: OutlineInputBorder()),
                validator: (v) => v == null || !v.contains('@') ? 'Введите корректный email' : null,
              ),
              const SizedBox(height: 16),
              TextFormField(
                controller: _passwordController,
                decoration: const InputDecoration(labelText: 'Пароль', border: OutlineInputBorder()),
                obscureText: true,
                validator: (v) => v == null || v.length < 6 ? 'Минимум 6 символов' : null,
              ),
              const SizedBox(height: 24),
              _isLoading
                  ? const Center(child: CircularProgressIndicator())
                  : ElevatedButton(
                      onPressed: _register,
                      child: const Padding(
                        padding: EdgeInsets.all(12.0),
                        child: Text('Зарегистрировать', style: TextStyle(fontSize: 16)),
                      ),
                    ),
            ],
          ),
        ),
      ),
    );
  }
}
