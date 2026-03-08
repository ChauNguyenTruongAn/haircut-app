import 'package:flutter/foundation.dart';
import 'package:haircut_app/models/appointment.dart';
import 'package:haircut_app/services/api_service.dart';
import 'package:haircut_app/config/app_config.dart';
import 'package:haircut_app/services/token_service.dart';
import 'package:http/http.dart' as http;
import 'dart:convert';
import 'package:intl/intl.dart';

class AppointmentProvider with ChangeNotifier {
  final String baseUrl = AppConfig.apiBaseUrl;
  final TokenService _tokenService = TokenService.instance;
  String? _token;
  final ApiService _apiService = ApiService(
      baseUrl: AppConfig.apiBaseUrl, tokenService: TokenService.instance);
  List<Appointment> _appointments = [];
  bool _isLoading = false;
  String? _error;
  DateTime _selectedDate = DateTime.now();

  AppointmentProvider() {
    _loadToken();
  }

  Future<void> _loadToken() async {
    _token = await _tokenService.getAccessToken();
  }

  String get token => _token ?? '';

  List<Appointment> get appointments => _appointments;
  bool get isLoading => _isLoading;
  String? get error => _error;
  DateTime get selectedDate => _selectedDate;

  // Set selected date
  void setSelectedDate(DateTime date) {
    _selectedDate = date;
    notifyListeners();
    // After updating the date, fetch appointments for the new date
    fetchAppointmentsByDate(date);
  }

  // Get all appointments
  Future<void> fetchAppointments() async {
    _isLoading = true;
    _error = null;
    notifyListeners();

    try {
      final response = await http.get(
        Uri.parse('$baseUrl/api/appointments'),
        headers: {
          'Authorization': 'Bearer $token',
          'Content-Type': 'application/json',
        },
      );

      if (response.statusCode == 200) {
        final List<dynamic> data = json.decode(response.body);
        _appointments = data.map((json) => Appointment.fromJson(json)).toList();
        _isLoading = false;
        notifyListeners();
      } else {
        throw Exception('Failed to load appointments');
      }
    } catch (e) {
      _isLoading = false;
      _error = e.toString();
      notifyListeners();
    }
  }

  // Get appointments by date
  Future<void> fetchAppointmentsByDate(DateTime date) async {
    _isLoading = true;
    _error = null;
    notifyListeners();

    try {
      _appointments = await _apiService.getAppointmentsByDate(date);
      _isLoading = false;
      notifyListeners();
    } catch (e) {
      _isLoading = false;
      _error = e.toString();
      notifyListeners();
    }
  }

  // Get a specific appointment
  Future<Appointment?> fetchAppointment(int id) async {
    _isLoading = true;
    _error = null;
    notifyListeners();

    try {
      final appointment = await _apiService.getAppointment(id);
      _isLoading = false;
      notifyListeners();
      return appointment;
    } catch (e) {
      _isLoading = false;
      _error = e.toString();
      notifyListeners();
      return null;
    }
  }

  // Create a new appointment
  Future<Appointment?> createAppointment(Appointment appointment) async {
    _isLoading = true;
    _error = null;
    notifyListeners();

    try {
      final newAppointment = await _apiService.createAppointment(appointment);
      _appointments.add(newAppointment);
      _isLoading = false;
      notifyListeners();
      return newAppointment;
    } catch (e) {
      _isLoading = false;
      _error = e.toString();
      notifyListeners();
      return null;
    }
  }

  // Update an existing appointment
  Future<Appointment?> updateAppointment(Appointment appointment) async {
    _isLoading = true;
    _error = null;
    notifyListeners();

    try {
      final updatedAppointment =
          await _apiService.updateAppointment(appointment);
      final index = _appointments.indexWhere((a) => a.id == appointment.id);
      if (index != -1) {
        _appointments[index] = updatedAppointment;
      }
      _isLoading = false;
      notifyListeners();
      return updatedAppointment;
    } catch (e) {
      _isLoading = false;
      _error = e.toString();
      notifyListeners();
      return null;
    }
  }

  // Delete an appointment
  Future<bool> deleteAppointment(int id) async {
    _isLoading = true;
    _error = null;
    notifyListeners();

    try {
      await _apiService.deleteAppointment(id);
      _appointments.removeWhere((appointment) => appointment.id == id);
      _isLoading = false;
      notifyListeners();
      return true;
    } catch (e) {
      _isLoading = false;
      _error = e.toString();
      notifyListeners();
      return false;
    }
  }

  // Change appointment status
  Future<Appointment?> changeAppointmentStatus(int id, AppointmentStatus status,
      {String? reason}) async {
    _isLoading = true;
    _error = null;
    notifyListeners();

    try {
      final updatedAppointment =
          await _apiService.changeAppointmentStatus(id, status, reason: reason);
      final index = _appointments.indexWhere((a) => a.id == id);
      if (index != -1) {
        _appointments[index] = updatedAppointment;
      }
      _isLoading = false;
      notifyListeners();
      return updatedAppointment;
    } catch (e) {
      _isLoading = false;
      _error = e.toString();
      notifyListeners();
      return null;
    }
  }

  // Get appointments for a specific barber
  List<Appointment> getAppointmentsByBarber(int barberId) {
    return _appointments
        .where((appointment) => appointment.barberId == barberId)
        .toList();
  }

  // Get appointments by status
  List<Appointment> getAppointmentsByStatus(AppointmentStatus status) {
    return _appointments
        .where((appointment) => appointment.status == status)
        .toList();
  }

  // Get appointments sorted by time
  List<Appointment> getAppointmentsSortedByTime() {
    final sortedAppointments = List<Appointment>.from(_appointments);
    sortedAppointments.sort((a, b) {
      final dateComparison = a.date.compareTo(b.date);
      if (dateComparison != 0) return dateComparison;

      return a.startTime.compareTo(b.startTime);
    });
    return sortedAppointments;
  }

  Future<Map<String, dynamic>> getRevenueStatistics(
      DateTime startDate, DateTime endDate) async {
    try {
      final response = await http.get(
        Uri.parse(
            '$baseUrl/api/bookings/revenue?startDate=${DateFormat('yyyy-MM-dd').format(startDate)}&endDate=${DateFormat('yyyy-MM-dd').format(endDate)}'),
        headers: {
          'Authorization': 'Bearer $token',
          'Content-Type': 'application/json; charset=utf-8',
          'Accept': 'application/json; charset=utf-8',
        },
      );

      if (response.statusCode == 200) {
        // Decode response body with UTF-8
        final String decodedBody = utf8.decode(response.bodyBytes);
        final data = json.decode(decodedBody);

        // Convert service names to proper UTF-8
        final Map<String, double> revenueByService = {};
        (data['revenueByService'] as Map<String, dynamic>)
            .forEach((key, value) {
          revenueByService[key] = (value as num).toDouble();
        });

        return {
          'totalRevenue': data['totalRevenue']?.toDouble() ?? 0.0,
          'revenueByService': revenueByService,
          'appointmentCountByStatus': data['appointmentCountByStatus'] ?? {},
        };
      } else {
        throw Exception('Failed to load revenue statistics');
      }
    } catch (e) {
      print('Error fetching revenue statistics: $e');
      return {
        'totalRevenue': 0.0,
        'revenueByService': {},
        'appointmentCountByStatus': {},
      };
    }
  }
}
