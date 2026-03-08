import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:haircut_app/providers/auth_provider.dart';
import 'dart:developer' as developer;

class LoginScreen extends StatefulWidget {
  const LoginScreen({super.key});

  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  final TextEditingController _usernameController = TextEditingController();
  final TextEditingController _passwordController = TextEditingController();
  final _formKey = GlobalKey<FormState>();
  bool _isLoading = false;
  bool _passwordVisible = false;

  @override
  void dispose() {
    _usernameController.dispose();
    _passwordController.dispose();
    super.dispose();
  }

  void _login() async {
    if (_formKey.currentState!.validate()) {
      setState(() {
        _isLoading = true;
      });

      developer.log('=== LoginScreen._login ===', name: 'LoginScreen');
      developer.log('Starting login process...', name: 'LoginScreen');
      developer.log('Username: ${_usernameController.text.trim()}',
          name: 'LoginScreen');

      try {
        final authProvider = Provider.of<AuthProvider>(context, listen: false);
        developer.log('Calling authProvider.login...', name: 'LoginScreen');

        final success = await authProvider.login(
          _usernameController.text.trim(),
          _passwordController.text,
        );

        if (!mounted) return;

        developer.log('Login result: ${success ? 'Success' : 'Failed'}',
            name: 'LoginScreen');
        if (success) {
          developer.log('Navigating to home screen...', name: 'LoginScreen');
          // Navigate to home
          Navigator.pushReplacementNamed(context, '/home');
        } else {
          developer.log('Showing error snackbar. Error: ${authProvider.error}',
              name: 'LoginScreen');
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: Text(
                  'Đăng nhập thất bại: ${authProvider.error ?? 'Vui lòng kiểm tra thông tin đăng nhập'}'),
              backgroundColor: Colors.red,
            ),
          );
        }
      } catch (e) {
        developer.log('ERROR: Exception during login: $e', name: 'LoginScreen');
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Lỗi: ${e.toString()}'),
            backgroundColor: Colors.red,
          ),
        );
      } finally {
        if (mounted) {
          setState(() {
            _isLoading = false;
          });
        }
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(20.0),
          child: Center(
            child: SingleChildScrollView(
              child: Form(
                key: _formKey,
                child: Column(
                  mainAxisAlignment: MainAxisAlignment.center,
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    // Logo and title
                    Column(
                      children: [
                        Icon(
                          Icons.content_cut,
                          size: 80,
                          color: Theme.of(context).primaryColor,
                        ),
                        const SizedBox(height: 16),
                        Text(
                          'Quản lý tiệm tóc',
                          style: Theme.of(context).textTheme.displayMedium,
                          textAlign: TextAlign.center,
                        ),
                        const SizedBox(height: 8),
                      
                      ],
                    ),
                    const SizedBox(height: 40),

                    // Username field
                    TextFormField(
                      controller: _usernameController,
                      decoration: const InputDecoration(
                        labelText: 'Username',
                        prefixIcon: Icon(Icons.person),
                        border: OutlineInputBorder(),
                      ),
                      validator: (value) {
                        if (value == null || value.isEmpty) {
                          return 'Please enter your username';
                        }
                        return null;
                      },
                    ),
                    const SizedBox(height: 16),

                    // Password field
                    TextFormField(
                      controller: _passwordController,
                      obscureText: !_passwordVisible,
                      decoration: InputDecoration(
                        labelText: 'Password',
                        prefixIcon: const Icon(Icons.lock),
                        border: const OutlineInputBorder(),
                        suffixIcon: IconButton(
                          icon: Icon(
                            _passwordVisible
                                ? Icons.visibility
                                : Icons.visibility_off,
                          ),
                          onPressed: () {
                            setState(() {
                              _passwordVisible = !_passwordVisible;
                            });
                          },
                        ),
                      ),
                      validator: (value) {
                        if (value == null || value.isEmpty) {
                          return 'Please enter your password';
                        }
                        return null;
                      },
                    ),
                    const SizedBox(height: 24),

                    // Login button
                    ElevatedButton(
                      onPressed: _isLoading ? null : _login,
                      style: ElevatedButton.styleFrom(
                        padding: const EdgeInsets.symmetric(vertical: 16),
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(8),
                        ),
                      ),
                      child: _isLoading
                          ? const SizedBox(
                              height: 20,
                              width: 20,
                              child: CircularProgressIndicator(
                                strokeWidth: 2,
                                color: Colors.white,
                              ),
                            )
                          : const Text(
                              'Đăng nhập',
                              style: TextStyle(fontSize: 16),
                            ),
                    ),
                    const SizedBox(height: 20),

                    // For demo purposes - admin login
                    Padding(
                      padding: const EdgeInsets.only(top: 16.0),
                      child: Column(
                        children: [
                          TextButton(
                            onPressed: () {
                              _usernameController.text = 'admin';
                              _passwordController.text = '123';
                            },
                            child: const Text('Use Admin Account'),
                          ),
                        
                        ],
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}
