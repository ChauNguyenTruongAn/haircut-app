import 'dart:convert';
import 'dart:developer' as developer;
import 'package:http/http.dart' as http;
import 'package:haircut_app/models/barber.dart';
import 'package:haircut_app/models/haircut_service.dart';
import 'package:haircut_app/models/appointment.dart';
import 'package:haircut_app/models/user.dart';
import 'package:haircut_app/config/app_config.dart';
import '../services/token_service.dart';

class ApiService {
  final String baseUrl;
  final TokenService tokenService;

  ApiService({required this.baseUrl, required this.tokenService});

  // Helper method to get headers with authorization token
  Future<Map<String, String>> _getHeaders() async {
    final token = await tokenService.getAccessToken();
    final headers = {
      'Content-Type': 'application/json',
    };

    if (token != null) {
      headers['Authorization'] = 'Bearer $token';
    }

    return headers;
  }

  // Helper để xử lý JSON với UTF-8 đúng cách
  Map<String, dynamic> _decodeUtf8Json(http.Response response) {
    // Decode response body với UTF-8
    final String decodedBody = utf8.decode(response.bodyBytes);
    return json.decode(decodedBody) as Map<String, dynamic>;
  }

  List<dynamic> _decodeUtf8JsonList(http.Response response) {
    // Decode response body với UTF-8
    final String decodedBody = utf8.decode(response.bodyBytes);
    return json.decode(decodedBody) as List<dynamic>;
  }

  // Auth endpoints
  Future<Map<String, dynamic>> login(String email, String password) async {
    try {
      developer.log('API login attempt with email: $email', name: 'ApiService');
      final response = await http.post(
        Uri.parse('$baseUrl${AppConfig.loginEndpoint}'),
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode({
          'username': email,
          'password': password,
        }),
      );

      developer.log('Login response status: ${response.statusCode}',
          name: 'ApiService');
      developer.log('Login response body: ${response.body}',
          name: 'ApiService');

      if (response.statusCode == 200) {
        // Sử dụng UTF-8 decode
        final data = _decodeUtf8Json(response);

        // Save tokens to token service
        if (data.containsKey('accessToken')) {
          await tokenService.saveAccessToken(data['accessToken']);
          // Make token available in return value for AuthProvider
          data['token'] = data['accessToken'];
        }

        if (data.containsKey('refreshToken')) {
          await tokenService.saveRefreshToken(data['refreshToken']);
        }

        return data;
      } else {
        developer.log(
            'Login failed with status ${response.statusCode}: ${response.body}',
            name: 'ApiService');
        throw Exception('Failed to login: ${response.body}');
      }
    } catch (e) {
      developer.log('Login error: $e', name: 'ApiService');
      throw Exception('Login error: $e');
    }
  }

  // Refresh token method
  Future<bool> refreshToken() async {
    developer.log('=== Refresh token attempt ===', name: 'ApiService');
    developer.log(
        'Refresh token URL: $baseUrl${AppConfig.validateTokenEndpoint}',
        name: 'ApiService');

    try {
      final response = await http.post(
        Uri.parse('$baseUrl${AppConfig.validateTokenEndpoint}'),
        headers: {'Content-Type': 'application/json'},
        body: json.encode({
          'refreshToken': await tokenService.getRefreshToken(),
        }),
      );

      developer.log('Refresh token response status: ${response.statusCode}',
          name: 'ApiService');
      developer.log('Refresh token response body: ${response.body}',
          name: 'ApiService');

      if (response.statusCode == 200) {
        // Sử dụng UTF-8 decode
        final responseData = _decodeUtf8Json(response);

        // Save the new access token
        await tokenService.saveAccessToken(responseData['accessToken']);
        developer.log('New access token saved using tokenService',
            name: 'ApiService');

        return true;
      } else {
        developer.log(
            'ERROR: Failed to refresh token. Status: ${response.statusCode}, Body: ${response.body}',
            name: 'ApiService');
        throw Exception('Failed to refresh token: ${response.body}');
      }
    } catch (e) {
      developer.log('ERROR: Exception during token refresh: $e',
          name: 'ApiService');
      throw Exception('Token refresh error: $e');
    }
  }

  Future<void> register(
      String username, String email, String password, String name) async {
    final response = await http.post(
      Uri.parse('$baseUrl${AppConfig.registerEndpoint}'),
      headers: {'Content-Type': 'application/json'},
      body: json.encode({
        'username': username,
        'email': email,
        'password': password,
        'name': name,
      }),
    );

    if (response.statusCode != 201) {
      throw Exception('Failed to register: ${response.body}');
    }
  }

  // Get user profile
  Future<User> getUserProfile(String userId) async {
    try {
      final response = await http.get(
        Uri.parse('$baseUrl${AppConfig.profileEndpoint}/$userId'),
        headers: await _getHeaders(),
      );
      if (response.statusCode == 200) {
        return User.fromJson(_decodeUtf8Json(response));
      } else {
        throw Exception('Failed to get user profile: ${response.body}');
      }
    } catch (e) {
      developer.log('Error getting user profile: $e', name: 'ApiService');
      rethrow;
    }
  }

  // Update user profile
  Future<User> updateUserProfile(
      String userId, Map<String, dynamic> updateData) async {
    try {
      final response = await http.put(
        Uri.parse('$baseUrl/users/info/$userId'),
        headers: await _getHeaders(),
        body: jsonEncode(updateData),
      );
      if (response.statusCode == 200) {
        return User.fromJson(_decodeUtf8Json(response));
      } else {
        throw Exception('Failed to update user profile: ${response.body}');
      }
    } catch (e) {
      developer.log('Error updating user profile: $e', name: 'ApiService');
      rethrow;
    }
  }

  Future<void> changePassword(
      String token, String currentPassword, String newPassword) async {
    final response = await http.put(
      Uri.parse('$baseUrl${AppConfig.changePasswordEndpoint}'),
      headers: {
        'Content-Type': 'application/json',
        'Authorization': 'Bearer $token',
      },
      body: json.encode({
        'currentPassword': currentPassword,
        'newPassword': newPassword,
      }),
    );

    if (response.statusCode != 200) {
      throw Exception('Failed to change password: ${response.body}');
    }
  }

  Future<void> logout() async {
    await tokenService.clearTokens();
  }

  // Barber endpoints
  Future<List<Barber>> getBarbers() async {
    final headers = await _getHeaders();
    final response = await http.get(
      Uri.parse('$baseUrl${AppConfig.barbersEndpoint}'),
      headers: headers,
    );

    if (response.statusCode == 200) {
      final List<dynamic> data = _decodeUtf8JsonList(response);
      return data.map((json) => Barber.fromJson(json)).toList();
    } else {
      throw Exception('Failed to load barbers: ${response.body}');
    }
  }

  Future<Barber> getBarber(int id) async {
    final headers = await _getHeaders();
    final response = await http.get(
      Uri.parse('$baseUrl${AppConfig.barbersEndpoint}/$id'),
      headers: headers,
    );

    if (response.statusCode == 200) {
      return Barber.fromJson(_decodeUtf8Json(response));
    } else {
      throw Exception('Failed to load barber: ${response.body}');
    }
  }

  Future<Barber> createBarber(Barber barber) async {
    try {
      // Chuẩn bị dữ liệu request
      final requestBody = barber.toJson();
      developer.log('Creating barber with data: ${json.encode(requestBody)}',
          name: 'ApiService');

      // Tạo URL path
      final path = AppConfig.barbersEndpoint;
      developer.log('Create barber path: $path', name: 'ApiService');

      // Thực hiện request trực tiếp, bỏ qua hoàn toàn http package
      final response =
          await _executeDirectRequest('POST', path, body: requestBody);

      // Xử lý response
      if (response.statusCode == 201 || response.statusCode == 200) {
        return Barber.fromJson(_decodeUtf8Json(response));
      } else {
        final errorMessage = 'Failed to create barber: ${response.body}';
        developer.log(errorMessage, name: 'ApiService');
        throw Exception(errorMessage);
      }
    } catch (e) {
      developer.log('Exception in createBarber: $e', name: 'ApiService');
      throw Exception('Error creating barber: $e');
    }
  }

  Future<Barber> updateBarber(
      int barberId, Map<String, dynamic> barberData) async {
    try {
      // Log update request data
      developer.log('Updating barber with ID: $barberId', name: 'ApiService');
      developer.log('Update data: ${json.encode(barberData)}',
          name: 'ApiService');

      // Create URL path
      final path = '${AppConfig.barbersEndpoint}/$barberId';
      developer.log('Update barber path: $path', name: 'ApiService');

      // Execute request directly
      final response =
          await _executeDirectRequest('PUT', path, body: barberData);

      // Process response
      if (response.statusCode == 200) {
        return Barber.fromJson(_decodeUtf8Json(response));
      } else {
        final errorMessage = 'Failed to update barber: ${response.body}';
        developer.log(errorMessage, name: 'ApiService');
        throw Exception(errorMessage);
      }
    } catch (e) {
      developer.log('Exception in updateBarber: $e', name: 'ApiService');
      throw Exception('Error updating barber: $e');
    }
  }

  Future<void> deleteBarber(int id) async {
    try {
      // Tạo URL path
      final path = '${AppConfig.barbersEndpoint}/$id';
      developer.log('Deleting barber with ID: $id', name: 'ApiService');
      developer.log('Delete barber path: $path', name: 'ApiService');

      // Thực hiện request trực tiếp, bỏ qua hoàn toàn http package
      final response = await _executeDirectRequest('DELETE', path);

      // Xử lý response
      if (response.statusCode != 204 && response.statusCode != 200) {
        final errorMessage = 'Failed to delete barber: ${response.body}';
        developer.log(errorMessage, name: 'ApiService');
        throw Exception(errorMessage);
      }
    } catch (e) {
      developer.log('Exception in deleteBarber: $e', name: 'ApiService');
      throw Exception('Error deleting barber: $e');
    }
  }

  // Haircut Service endpoints
  Future<List<HaircutService>> getServices() async {
    final headers = await _getHeaders();
    final response = await http.get(
      Uri.parse('$baseUrl${AppConfig.servicesEndpoint}'),
      headers: headers,
    );

    if (response.statusCode == 200) {
      final List<dynamic> data = _decodeUtf8JsonList(response);
      return data.map((json) => HaircutService.fromJson(json)).toList();
    } else {
      throw Exception('Failed to load services: ${response.body}');
    }
  }

  Future<List<HaircutService>> getServicesByBarber(int barberId) async {
    final headers = await _getHeaders();
    final response = await http.get(
      Uri.parse('$baseUrl${AppConfig.barbersEndpoint}/$barberId/services'),
      headers: headers,
    );

    if (response.statusCode == 200) {
      final List<dynamic> data = _decodeUtf8JsonList(response);
      return data.map((json) => HaircutService.fromJson(json)).toList();
    } else {
      throw Exception('Failed to load services for barber: ${response.body}');
    }
  }

  Future<HaircutService> getService(int id) async {
    final headers = await _getHeaders();
    final response = await http.get(
      Uri.parse('$baseUrl${AppConfig.servicesEndpoint}/$id'),
      headers: headers,
    );

    if (response.statusCode == 200) {
      return HaircutService.fromJson(_decodeUtf8Json(response));
    } else {
      throw Exception('Failed to load service: ${response.body}');
    }
  }

  Future<HaircutService> createService(HaircutService service) async {
    final headers = await _getHeaders();
    try {
      // Log request data for debugging
      final requestBody = json.encode(service.toJson());
      developer.log('Creating service with data: $requestBody',
          name: 'ApiService');

      final response = await http.post(
        Uri.parse('$baseUrl${AppConfig.servicesEndpoint}'),
        headers: headers,
        body: requestBody,
      );

      developer.log('Create service response status: ${response.statusCode}',
          name: 'ApiService');
      developer.log('Create service response body: ${response.body}',
          name: 'ApiService');

      if (response.statusCode == 201 || response.statusCode == 200) {
        return HaircutService.fromJson(_decodeUtf8Json(response));
      } else {
        final errorMessage = 'Failed to create service: ${response.body}';
        developer.log(errorMessage, name: 'ApiService');
        throw Exception(errorMessage);
      }
    } catch (e) {
      developer.log('Exception in createService: $e', name: 'ApiService');
      throw Exception('Error creating service: $e');
    }
  }

  Future<HaircutService> updateService(HaircutService service) async {
    final headers = await _getHeaders();
    final response = await http.put(
      Uri.parse('$baseUrl${AppConfig.servicesEndpoint}/${service.id}'),
      headers: headers,
      body: json.encode(service.toJson()),
    );

    if (response.statusCode == 200) {
      return HaircutService.fromJson(_decodeUtf8Json(response));
    } else {
      throw Exception('Failed to update service: ${response.body}');
    }
  }

  Future<void> deleteService(int id) async {
    final headers = await _getHeaders();
    final response = await http.delete(
      Uri.parse('$baseUrl${AppConfig.servicesEndpoint}/$id'),
      headers: headers,
    );

    if (response.statusCode != 200) {
      throw Exception('Failed to delete service: ${response.body}');
    }
  }

  // Appointment endpoints
  Future<List<Appointment>> getAppointments() async {
    final headers = await _getHeaders();
    final response = await http.get(
      Uri.parse('$baseUrl${AppConfig.appointmentsEndpoint}'),
      headers: headers,
    );

    if (response.statusCode == 200) {
      final List<dynamic> data = _decodeUtf8JsonList(response);
      return data.map((json) => Appointment.fromJson(json)).toList();
    } else {
      throw Exception('Failed to load appointments: ${response.body}');
    }
  }

  Future<List<Appointment>> getAppointmentsByDate(DateTime date) async {
    final headers = await _getHeaders();
    final formattedDate =
        "${date.year}-${date.month.toString().padLeft(2, '0')}-${date.day.toString().padLeft(2, '0')}";
    final response = await http.get(
      Uri.parse(
          '$baseUrl${AppConfig.appointmentsEndpoint}/date/$formattedDate'),
      headers: headers,
    );

    if (response.statusCode == 200) {
      final List<dynamic> data = _decodeUtf8JsonList(response);
      return data.map((json) => Appointment.fromJson(json)).toList();
    } else {
      throw Exception('Failed to load appointments by date: ${response.body}');
    }
  }

  Future<Appointment> getAppointment(int id) async {
    final headers = await _getHeaders();
    final response = await http.get(
      Uri.parse('$baseUrl${AppConfig.appointmentsEndpoint}/$id'),
      headers: headers,
    );

    if (response.statusCode == 200) {
      return Appointment.fromJson(_decodeUtf8Json(response));
    } else {
      throw Exception('Failed to load appointment: ${response.body}');
    }
  }

  Future<Appointment> createAppointment(Appointment appointment) async {
    final headers = await _getHeaders();
    try {
      // Convert service IDs to a map with string keys for proper JSON encoding
      Map<String, int> servicesMap = {};
      for (int serviceId in appointment.serviceIds) {
        servicesMap[serviceId.toString()] = 1;
      }

      // Format the request to match the backend API expectations
      Map<String, dynamic> requestData = {
        'userId': appointment.customerId,
        'barberId': appointment.barberId,
        'appointmentDate': appointment.date.toIso8601String().split('T')[0],
        'appointmentTime': appointment.startTime,
        'appointmentTimeEnd': appointment.endTime,
        'status': appointment.status.toString().split('.').last,
        'note': appointment.notes,
        'services': servicesMap,
        'senderId': appointment.senderId,
      };

      // Log request data for debugging
      developer.log(
          'Creating appointment with data: ${json.encode(requestData)}',
          name: 'ApiService');

      final response = await http.post(
        Uri.parse('$baseUrl${AppConfig.appointmentsEndpoint}'),
        headers: headers,
        body: json.encode(requestData),
      );

      developer.log('Create appointment response: ${response.statusCode}',
          name: 'ApiService');
      developer.log('Response body: ${response.body}', name: 'ApiService');

      if (response.statusCode == 201 || response.statusCode == 200) {
        return Appointment.fromJson(_decodeUtf8Json(response));
      } else {
        final errorMessage = 'Failed to create appointment: ${response.body}';
        developer.log(errorMessage, name: 'ApiService');
        throw Exception(errorMessage);
      }
    } catch (e) {
      developer.log('Exception in createAppointment: $e', name: 'ApiService');
      throw Exception('Error creating appointment: $e');
    }
  }

  Future<Appointment> updateAppointment(Appointment appointment) async {
    final headers = await _getHeaders();
    try {
      // Convert service IDs to a map with string keys for proper JSON encoding
      Map<String, int> servicesMap = {};
      for (int serviceId in appointment.serviceIds) {
        servicesMap[serviceId.toString()] = 1;
      }

      // Format the request to match the backend API expectations
      Map<String, dynamic> requestData = {
        'userId': appointment.customerId,
        'barberId': appointment.barberId,
        'appointmentDate': appointment.date.toIso8601String().split('T')[0],
        'appointmentTime': appointment.startTime,
        'appointmentTimeEnd': appointment.endTime,
        'status': appointment.status.toString().split('.').last,
        'note': appointment.notes,
        'services': servicesMap, // Use string keys for proper JSON encoding
      };

      // Log request data for debugging
      developer.log(
          'Updating appointment with data: ${json.encode(requestData)}',
          name: 'ApiService');

      final response = await http.put(
        Uri.parse(
            '$baseUrl${AppConfig.appointmentsEndpoint}/${appointment.id}'),
        headers: headers,
        body: json.encode(requestData),
      );

      developer.log('Update appointment response: ${response.statusCode}',
          name: 'ApiService');
      developer.log('Response body: ${response.body}', name: 'ApiService');

      if (response.statusCode == 200) {
        return Appointment.fromJson(_decodeUtf8Json(response));
      } else {
        final errorMessage = 'Failed to update appointment: ${response.body}';
        developer.log(errorMessage, name: 'ApiService');
        throw Exception(errorMessage);
      }
    } catch (e) {
      developer.log('Exception in updateAppointment: $e', name: 'ApiService');
      throw Exception('Error updating appointment: $e');
    }
  }

  Future<void> deleteAppointment(int id) async {
    final headers = await _getHeaders();
    final response = await http.delete(
      Uri.parse('$baseUrl${AppConfig.appointmentsEndpoint}/$id'),
      headers: headers,
    );

    if (response.statusCode != 204) {
      throw Exception('Failed to delete appointment: ${response.body}');
    }
  }

  Future<Appointment> changeAppointmentStatus(int id, AppointmentStatus status,
      {String? reason}) async {
    final headers = await _getHeaders();
    final Map<String, dynamic> requestBody = {
      'status': status.toString().split('.').last,
    };

    // Add reason if provided (especially important for CANCELLED status)
    if (reason != null && reason.isNotEmpty) {
      requestBody['reason'] = reason;
    }

    final response = await http.put(
      Uri.parse('$baseUrl${AppConfig.appointmentsEndpoint}/$id/status'),
      headers: headers,
      body: json.encode(requestBody),
    );

    if (response.statusCode == 200) {
      return Appointment.fromJson(_decodeUtf8Json(response));
    } else {
      throw Exception('Failed to change appointment status: ${response.body}');
    }
  }

  // Helper method để tạo request thủ công, tránh vấn đề Content-Type charset
  Future<http.Request> _createRequest(String method, String path,
      {Map<String, dynamic>? body}) async {
    final uri = Uri.parse('$baseUrl$path');

    var request = http.Request(method, uri);

    // Xóa bỏ mọi header mặc định có thể được thêm vào
    request.headers.clear();

    // Thiết lập headers theo cách thủ công
    request.headers['Content-Type'] = 'application/json';
    request.headers['Accept'] = 'application/json';

    // Thêm token nếu có
    final token = await tokenService.getAccessToken();
    if (token != null) {
      request.headers['Authorization'] = 'Bearer $token';
    }

    // Log header để debug
    developer.log('Request headers: ${request.headers}', name: 'ApiService');

    // Đặt body nếu có
    if (body != null) {
      // Encode body thành JSON và set
      request.body = json.encode(body);

      // Log để debug
      developer.log('Request body: ${request.body}', name: 'ApiService');
    }

    return request;
  }

  // Helper method để thực hiện request và trả về response
  Future<http.Response> _executeRequest(http.Request request) async {
    // Tạo client mới để kiểm soát hoàn toàn quá trình kết nối
    var client = http.Client();
    try {
      // Tạo http.BaseRequest từ http.Request để kiểm soát quá trình gửi
      var baseRequest = http.Request(request.method, request.url);

      // Copy headers và body từ request gốc mà không cho phép package thêm charset
      baseRequest.headers.addAll(request.headers);
      if (request.body.isNotEmpty) {
        baseRequest.body = request.body;
      }

      // Thực hiện request và nhận response stream
      final streamedResponse = await client.send(baseRequest);

      // Chuyển đổi stream response thành http.Response
      final response = await http.Response.fromStream(streamedResponse);

      return response;
    } finally {
      // Đảm bảo client luôn được đóng sau khi thực hiện xong
      client.close();
    }
  }

  Future<http.Response> _executeDirectRequest(String method, String path,
      {Map<String, dynamic>? body}) async {
    try {
      // Sử dụng http.Client trực tiếp thay vì HttpClient để tránh lỗi ký tự Unicode
      var client = http.Client();

      // Tạo URL
      final uri = Uri.parse('$baseUrl$path');
      developer.log('Direct request to: $uri', name: 'ApiService');

      // Chuẩn bị headers
      var headers = <String, String>{
        'Content-Type': 'application/json',
        'Accept': 'application/json',
      };

      // Thêm token nếu có
      final token = await tokenService.getAccessToken();
      if (token != null) {
        headers['Authorization'] = 'Bearer $token';
      }

      // Log headers
      developer.log('Request headers: $headers', name: 'ApiService');

      // Chuẩn bị body
      String? bodyString;
      if (body != null) {
        bodyString = json.encode(body);
        developer.log('Request body: $bodyString', name: 'ApiService');
      }

      // Thực hiện request dựa vào phương thức
      http.Response response;

      switch (method) {
        case 'GET':
          response = await client.get(uri, headers: headers);
          break;
        case 'POST':
          response = await client.post(uri, headers: headers, body: bodyString);
          break;
        case 'PUT':
          response = await client.put(uri, headers: headers, body: bodyString);
          break;
        case 'DELETE':
          response = await client.delete(uri, headers: headers);
          break;
        default:
          throw Exception('Unsupported HTTP method: $method');
      }

      // Log response
      developer.log('Response status: ${response.statusCode}',
          name: 'ApiService');
      developer.log('Response body: ${response.body}', name: 'ApiService');

      // Đóng client
      client.close();

      return response;
    } catch (e) {
      developer.log('Error in direct request: $e', name: 'ApiService');
      throw Exception('Error in direct request: $e');
    }
  }
}
