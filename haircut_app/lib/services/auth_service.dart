import 'dart:convert';
import 'package:flutter/foundation.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:haircut_app/models/user.dart';
import 'package:haircut_app/services/api_service.dart';
import 'package:http/http.dart' as http;
import 'package:haircut_app/config/app_config.dart';
import 'package:haircut_app/services/token_service.dart';

class AuthService extends ChangeNotifier {
  User? _currentUser;
  String? _token;
  final ApiService _apiService = ApiService(
      baseUrl: AppConfig.apiBaseUrl, tokenService: TokenService.instance);
  bool _isLoading = false;

  User? get currentUser => _currentUser;
  String? get token => _token;
  bool get isLoggedIn => _token != null;
  bool get isLoading => _isLoading;

  AuthService() {
    _loadFromPrefs();
  }

  Future<void> _loadFromPrefs() async {
    _isLoading = true;
    notifyListeners();

    final prefs = await SharedPreferences.getInstance();
    final storedToken = prefs.getString('auth_token');
    final storedUser = prefs.getString('user');

    if (storedToken != null && storedUser != null) {
      _token = storedToken;
      _currentUser = User.fromJson(json.decode(storedUser));
    }

    _isLoading = false;
    notifyListeners();
  }

  Future<void> _saveToPrefs() async {
    final prefs = await SharedPreferences.getInstance();

    if (_token != null && _currentUser != null) {
      await prefs.setString('auth_token', _token!);
      await prefs.setString('user', json.encode(_currentUser!.toJson()));
    } else {
      await prefs.remove('auth_token');
      await prefs.remove('user');
    }
  }

  Future<bool> login(String username, String password) async {
    try {
      _isLoading = true;
      notifyListeners();

      final response = await _apiService.login(username, password);
      _token = response['token'];

      if (_token != null) {
        _currentUser = await _apiService.getUserProfile(_token!);
        await _saveToPrefs();
        _isLoading = false;
        notifyListeners();
        return true;
      }

      _isLoading = false;
      notifyListeners();
      return false;
    } catch (e) {
      _isLoading = false;
      notifyListeners();
      rethrow;
    }
  }

  Future<bool> register(
      String username, String email, String password, String name) async {
    try {
      _isLoading = true;
      notifyListeners();

      await _apiService.register(username, email, password, name);
      return await login(username, password);
    } catch (e) {
      _isLoading = false;
      notifyListeners();
      rethrow;
    }
  }

  Future<void> logout() async {
    _token = null;
    _currentUser = null;
    await _saveToPrefs();
    notifyListeners();
  }

  Future<void> refreshUserProfile() async {
    if (_token != null) {
      try {
        _currentUser = await _apiService.getUserProfile(_token!);
        await _saveToPrefs();
        notifyListeners();
      } catch (e) {
        if (kDebugMode) {
          print('Error refreshing user profile: $e');
        }
      }
    }
  }

  Future<bool> updateProfile(String name, String email) async {
    try {
      _isLoading = true;
      notifyListeners();

      if (_token == null) return false;

      final response = await http.put(
        Uri.parse('${_apiService.baseUrl}/users/profile'),
        headers: {
          'Content-Type': 'application/json',
          'Authorization': 'Bearer $_token',
        },
        body: jsonEncode({
          'name': name,
          'email': email,
        }),
      );

      if (response.statusCode == 200) {
        await refreshUserProfile();
        _isLoading = false;
        notifyListeners();
        return true;
      } else {
        _isLoading = false;
        notifyListeners();
        return false;
      }
    } catch (e) {
      _isLoading = false;
      notifyListeners();
      rethrow;
    }
  }

  Future<bool> changePassword(
      String currentPassword, String newPassword) async {
    try {
      _isLoading = true;
      notifyListeners();

      if (_token == null) return false;

      await _apiService.changePassword(_token!, currentPassword, newPassword);

      _isLoading = false;
      notifyListeners();
      return true;
    } catch (e) {
      _isLoading = false;
      notifyListeners();
      rethrow;
    }
  }
}
