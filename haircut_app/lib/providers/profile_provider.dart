import 'package:flutter/foundation.dart';
import 'package:haircut_app/models/user.dart';
import 'package:haircut_app/services/api_service.dart';
import 'package:haircut_app/config/app_config.dart';
import 'package:haircut_app/services/token_service.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'dart:developer' as developer;

class ProfileProvider with ChangeNotifier {
  User? _user;
  bool _isLoading = false;
  String? _error;

  User? get user => _user;
  bool get isLoading => _isLoading;
  String? get error => _error;

  final ApiService _apiService = ApiService(
      baseUrl: AppConfig.apiBaseUrl, tokenService: TokenService.instance);

  Future<void> loadUserProfile() async {
    _isLoading = true;
    _error = null;
    notifyListeners();

    try {
      final prefs = await SharedPreferences.getInstance();
      final userId = prefs.getInt('userId');
      if (userId == null) {
        throw Exception('User ID not found');
      }
      _user = await _apiService.getUserProfile(userId.toString());
    } catch (e) {
      _error = _parseError(e);
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  Future<void> updateProfile(
      String name, String email, String phoneNumber) async {
    _isLoading = true;
    _error = null;
    notifyListeners();

    try {
      final prefs = await SharedPreferences.getInstance();
      final userId = prefs.getInt('userId');
      if (userId == null) {
        throw Exception('User ID not found');
      }

      final updateData = {
        'email': email,
        'sdt': phoneNumber,
        'hoTen': name,
      };

      _user =
          await _apiService.updateUserProfile(userId.toString(), updateData);
    } catch (e) {
      _error = _parseError(e);
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  String _parseError(dynamic error) {
    if (error is Exception) {
      return error.toString();
    }
    return 'An unexpected error occurred';
  }
}
