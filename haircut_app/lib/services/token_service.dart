import 'package:shared_preferences/shared_preferences.dart';

class TokenService {
  static const String ACCESS_TOKEN_KEY = 'access_token';
  static const String REFRESH_TOKEN_KEY = 'refresh_token';

  // Singleton instance
  static final TokenService instance = TokenService._internal();

  factory TokenService() {
    return instance;
  }

  TokenService._internal();

  Future<String?> getAccessToken() async {
    final prefs = await SharedPreferences.getInstance();
    return prefs.getString(ACCESS_TOKEN_KEY);
  }

  Future<bool> saveAccessToken(String token) async {
    final prefs = await SharedPreferences.getInstance();
    return prefs.setString(ACCESS_TOKEN_KEY, token);
  }

  Future<String?> getRefreshToken() async {
    final prefs = await SharedPreferences.getInstance();
    return prefs.getString(REFRESH_TOKEN_KEY);
  }

  Future<bool> saveRefreshToken(String token) async {
    final prefs = await SharedPreferences.getInstance();
    return prefs.setString(REFRESH_TOKEN_KEY, token);
  }

  Future<bool> clearTokens() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.remove(ACCESS_TOKEN_KEY);
    return prefs.remove(REFRESH_TOKEN_KEY);
  }
}
