import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;
import 'package:shared_preferences/shared_preferences.dart';
import 'package:haircut_app/models/user.dart';
import 'package:haircut_app/config/app_config.dart';
import 'package:haircut_app/services/api_service.dart';
import 'dart:developer' as developer;
import 'package:haircut_app/services/token_service.dart';

class AuthProvider with ChangeNotifier {
  User? _currentUser;
  String? _accessToken;
  String? _refreshToken;
  bool _isLoading = false;
  String? _error;
  bool _isLoggedIn = false;
  final ApiService _apiService = ApiService(
      baseUrl: AppConfig.apiBaseUrl, tokenService: TokenService.instance);

  User? get currentUser => _currentUser;
  String? get token => _accessToken;
  bool get isLoading => _isLoading;
  String? get error => _error;
  bool get isAuthenticated => _accessToken != null;
  bool get isLoggedIn => _isLoggedIn;

  // Role-based checks
  bool get isAdmin =>
      isLoggedIn && _currentUser != null && _currentUser!.isAdmin;
  bool get isBarber =>
      isLoggedIn && _currentUser != null && _currentUser!.isBarber;
  bool get isUser =>
      _currentUser?.role.toUpperCase() == 'USER' ||
      (_currentUser != null && (_currentUser!.role.isEmpty));

  Future<bool> login(String email, String password) async {
    _isLoading = true;
    _error = null;
    notifyListeners();

    try {
      final response = await _apiService.login(email, password);

      if (response.containsKey('user') && response.containsKey('token')) {
        final user = User.fromJson(response['user']);
        _currentUser = user;
        _isLoggedIn = true;
        _isLoading = false;
        _error = null;
        notifyListeners();

        // Đăng nhập thành công, lưu thông tin người dùng
        await saveUserInfo(user);

        return true;
      } else {
        _error = 'Invalid response from server';
        _isLoading = false;
        notifyListeners();
        return false;
      }
    } catch (e) {
      _error = e.toString();
      _isLoading = false;
      notifyListeners();
      return false;
    }
  }

  Future<void> logout() async {
    _accessToken = null;
    _refreshToken = null;
    _currentUser = null;

    // Clear tokens using TokenService
    await TokenService.instance.clearTokens();

    // Clear user data
    final prefs = await SharedPreferences.getInstance();
    await prefs.remove('user');

    notifyListeners();
  }

  Future<bool> tryAutoLogin() async {
    final prefs = await SharedPreferences.getInstance();

    // Check if tokens exist using TokenService
    _accessToken = await TokenService.instance.getAccessToken();
    _refreshToken = await TokenService.instance.getRefreshToken();
    final userData = prefs.getString('user');

    if (_accessToken == null || userData == null) {
      return false;
    }

    _currentUser = User.fromJson(json.decode(userData));

    // Verify token validity and refresh if necessary
    try {
      // Thử sử dụng access token
      final response = await http.get(
        Uri.parse('${AppConfig.apiBaseUrl}${AppConfig.profileEndpoint}'),
        headers: {
          'Content-Type': 'application/json',
          'Authorization': 'Bearer $_accessToken',
        },
      );

      if (response.statusCode == 200) {
        // Token còn hợp lệ
        _currentUser = User.fromJson(json.decode(response.body));
        await _saveAuthData();
        notifyListeners();
        return true;
      } else {
        // Thử refresh token
        try {
          final success = await _apiService.refreshToken();
          if (success) {
            // Refresh successful, get new token from TokenService
            _accessToken = await TokenService.instance.getAccessToken();
            await _saveAuthData();
            notifyListeners();
            return true;
          } else {
            await logout();
            return false;
          }
        } catch (e) {
          // Refresh token không hợp lệ, đăng xuất
          await logout();
          return false;
        }
      }
    } catch (e) {
      _error = e.toString();
      return false;
    }
  }

  Future<bool> register(
      String name, String username, String email, String password) async {
    _setLoading(true);
    _error = null;

    try {
      final response = await http.post(
        Uri.parse('${AppConfig.apiBaseUrl}${AppConfig.registerEndpoint}'),
        headers: {'Content-Type': 'application/json'},
        body: json.encode({
          'name': name,
          'username': username,
          'email': email,
          'password': password,
        }),
      );

      if (response.statusCode == 201) {
        // Tự động đăng nhập sau khi đăng ký
        return await login(username, password);
      } else {
        final data = json.decode(response.body);
        _error = data['message'] ?? 'Registration failed';
        _setLoading(false);
        notifyListeners();
        return false;
      }
    } catch (e) {
      _error = e.toString();
      _setLoading(false);
      notifyListeners();
      return false;
    }
  }

  Future<bool> updateProfile(String name, String email) async {
    if (_currentUser == null || _accessToken == null) {
      _error = 'Not authenticated';
      return false;
    }

    _setLoading(true);
    _error = null;

    try {
      final response = await http.put(
        Uri.parse('${AppConfig.apiBaseUrl}${AppConfig.profileEndpoint}'),
        headers: {
          'Content-Type': 'application/json',
          'Authorization': 'Bearer $_accessToken',
        },
        body: json.encode({
          'name': name,
          'email': email,
        }),
      );

      if (response.statusCode == 200) {
        final data = json.decode(response.body);
        _currentUser = User.fromJson(data);

        // Update shared preferences
        await _saveAuthData();

        _setLoading(false);
        notifyListeners();
        return true;
      } else {
        final data = json.decode(response.body);
        _error = data['message'] ?? 'Update failed';
        _setLoading(false);
        notifyListeners();
        return false;
      }
    } catch (e) {
      _error = e.toString();
      _setLoading(false);
      notifyListeners();
      return false;
    }
  }

  Future<bool> changePassword(
      String currentPassword, String newPassword) async {
    if (_currentUser == null || _accessToken == null) {
      _error = 'Not authenticated';
      return false;
    }

    _setLoading(true);
    _error = null;

    try {
      final response = await http.put(
        Uri.parse('${AppConfig.apiBaseUrl}${AppConfig.changePasswordEndpoint}'),
        headers: {
          'Content-Type': 'application/json',
          'Authorization': 'Bearer $_accessToken',
        },
        body: json.encode({
          'currentPassword': currentPassword,
          'newPassword': newPassword,
        }),
      );

      if (response.statusCode == 200) {
        _setLoading(false);
        notifyListeners();
        return true;
      } else {
        final data = json.decode(response.body);
        _error = data['message'] ?? 'Password change failed';
        _setLoading(false);
        notifyListeners();
        return false;
      }
    } catch (e) {
      _error = e.toString();
      _setLoading(false);
      notifyListeners();
      return false;
    }
  }

  // Helper methods
  void _setLoading(bool value) {
    _isLoading = value;
    notifyListeners();
  }

  Future<void> _saveAuthData() async {
    final prefs = await SharedPreferences.getInstance();

    // No need to save tokens here as they're already saved in TokenService
    // Only save user data
    if (_currentUser != null) {
      await prefs.setString('user', json.encode(_currentUser!.toJson()));
    }
  }

  void clearError() {
    _error = null;
    notifyListeners();
  }

  // Save user info to shared preferences
  Future<void> saveUserInfo(User user) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setInt('userId', user.id);
    await prefs.setString('username', user.username);
    await prefs.setString('name', user.name);
    await prefs.setString('email', user.email);
    await prefs.setString('role', user.role);
  }

  // Load user info from shared preferences
  Future<void> loadUserInfo() async {
    final prefs = await SharedPreferences.getInstance();
    final userId = prefs.getInt('userId');
    final username = prefs.getString('username');
    final name = prefs.getString('name');
    final email = prefs.getString('email');
    final role = prefs.getString('role');

    if (userId != null &&
        username != null &&
        name != null &&
        email != null &&
        role != null) {
      _currentUser = User(
        id: userId,
        username: username,
        name: name,
        email: email,
        role: role,
      );
      _isLoggedIn = true;
      notifyListeners();
    }
  }
}
