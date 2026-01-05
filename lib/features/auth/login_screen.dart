import 'package:flutter/material.dart';
import 'package:try_neuro/core/config/app_config.dart';
import 'package:try_neuro/features/admin/admin_dashboard_screen.dart';
import 'package:try_neuro/features/auth/data/auth_service.dart';
import 'package:try_neuro/features/auth/domain/user_model.dart';
import 'package:try_neuro/features/auth/register_company_screen.dart';
import 'package:try_neuro/features/manager/manager_home_screen.dart';
import 'package:try_neuro/features/staff/employee_home_screen.dart';
import 'package:try_neuro/service_locator.dart';

class LoginScreen extends StatefulWidget {
  const LoginScreen({super.key});

  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  final _emailController = TextEditingController(
    text: AppConfig.isProduction ? null : 'forts1@e1.ru',
  );
  final _passwordController = TextEditingController(
    text: AppConfig.isProduction ? null : 'qwerty',
  );

  final _authService = sl<AuthService>(); 
  bool _isLoading = false;

  Future<void> _login() async {
    setState(() {
      _isLoading = true;
    });
    FocusScope.of(context).unfocus();

    final user = await _authService.login(
      _emailController.text.trim(),
      _passwordController.text,
    );

    setState(() {
      _isLoading = false;
    });

    if (user != null && mounted) {
      Widget homeScreen;
      switch (user.role) {
        case UserRole.admin:
          homeScreen = const AdminDashboardScreen();
          break;
        case UserRole.manager:
          homeScreen = const ManagerHomeScreen();
          break;
        case UserRole.employee:
          homeScreen = EmployeeHomeScreen(user: user);
          break;
        default:
          homeScreen = const ManagerHomeScreen();
      }
      Navigator.pushReplacement(
        context,
        MaterialPageRoute(builder: (context) => homeScreen),
      );
    } else if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Неверный email или пароль'),
          backgroundColor: Colors.red,
        ),
      );
    }
  }

  void _navigateToRegister() {
    Navigator.push(
      context,
      MaterialPageRoute(builder: (context) => const RegisterCompanyScreen()),
    );
  }

  @override
  void dispose() {
    _emailController.dispose();
    _passwordController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Вход в CRM'),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            const SizedBox(height: 60.0),
            const Text(
              'Добро пожаловать!',
              style: TextStyle(fontSize: 24, fontWeight: FontWeight.bold),
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 32.0),
            TextField(
              controller: _emailController,
              decoration: const InputDecoration(
                labelText: 'Email',
                border: OutlineInputBorder(),
              ),
              keyboardType: TextInputType.emailAddress,
              enabled: !_isLoading,
            ),
            const SizedBox(height: 16),
            TextField(
              controller: _passwordController,
              decoration: const InputDecoration(
                labelText: 'Пароль',
                border: OutlineInputBorder(),
              ),
              obscureText: true,
              enabled: !_isLoading,
            ),
            const SizedBox(height: 24),
            _isLoading
                ? const Center(child: CircularProgressIndicator())
                : ElevatedButton(
                    onPressed: _login,
                    style: ElevatedButton.styleFrom(
                      minimumSize: const Size(double.infinity, 50),
                    ),
                    child: const Text('Войти'),
                  ),
            const SizedBox(height: 16),
            TextButton(
              onPressed: _navigateToRegister,
              child: const Text('Зарегистрировать компанию'),
            ),
          ],
        ),
      ),
    );
  }
}
