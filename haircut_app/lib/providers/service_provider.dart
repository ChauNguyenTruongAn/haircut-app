import 'package:flutter/foundation.dart';
import 'package:haircut_app/models/haircut_service.dart';
import 'package:haircut_app/services/api_service.dart';
import 'package:haircut_app/config/app_config.dart';
import 'package:haircut_app/services/token_service.dart';
import 'package:http/http.dart' as http;
import 'dart:convert';

class ServiceProvider with ChangeNotifier {
  final ApiService _apiService = ApiService(
      baseUrl: AppConfig.apiBaseUrl, tokenService: TokenService.instance);
  List<HaircutService> _services = [];
  bool _isLoading = false;
  String? _error;

  List<HaircutService> get services => _services;
  bool get isLoading => _isLoading;
  String? get error => _error;

  // Get all services
  Future<void> fetchServices() async {
    if (_isLoading) return; // Tránh gọi nhiều lần cùng lúc

    _isLoading = true;
    _error = null;
    // Không gọi notifyListeners() ở đây

    try {
      _services = await _apiService.getServices();
      _isLoading = false;
      notifyListeners();
    } catch (e) {
      _isLoading = false;
      _error = e.toString();
      notifyListeners();
    }
  }

  // Get a specific service
  Future<HaircutService?> fetchService(int id) async {
    _isLoading = true;
    _error = null;
    notifyListeners();

    try {
      final service = await _apiService.getService(id);
      _isLoading = false;
      notifyListeners();
      return service;
    } catch (e) {
      _isLoading = false;
      _error = e.toString();
      notifyListeners();
      return null;
    }
  }

  // Create a new service
  Future<HaircutService?> createService(HaircutService service) async {
    _isLoading = true;
    _error = null;
    notifyListeners();

    try {
      final newService = await _apiService.createService(service);
      _services.add(newService);
      _isLoading = false;
      notifyListeners();
      return newService;
    } catch (e) {
      _isLoading = false;
      _error = e.toString();
      notifyListeners();
      return null;
    }
  }

  // Update an existing service
  Future<HaircutService?> updateService(HaircutService service) async {
    _isLoading = true;
    _error = null;
    notifyListeners();

    try {
      final updatedService = await _apiService.updateService(service);
      final index = _services.indexWhere((s) => s.id == service.id);
      if (index != -1) {
        _services[index] = updatedService;
      }
      _isLoading = false;
      notifyListeners();
      return updatedService;
    } catch (e) {
      _isLoading = false;
      _error = e.toString();
      notifyListeners();
      return null;
    }
  }

  // Delete a service
  Future<bool> deleteService(int id) async {
    _isLoading = true;
    _error = null;
    notifyListeners();

    try {
      final token = await TokenService.instance.getAccessToken();
      final response = await http.delete(
        Uri.parse('${AppConfig.apiBaseUrl}${AppConfig.servicesEndpoint}/$id'),
        headers: {
          'Authorization': 'Bearer $token',
          'Content-Type': 'application/json',
        },
      );

      if (response.statusCode == 200) {
        // Xóa service khỏi danh sách local
        _services.removeWhere((service) => service.id == id);
        _isLoading = false;
        notifyListeners();
        return true;
      } else {
        final errorMessage = response.body;
        _error = errorMessage;
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

  // Get active services only
  List<HaircutService> getActiveServices() {
    return _services.where((service) => service.isActive).toList();
  }

  // Get services sorted by sort order
  List<HaircutService> getSortedServices() {
    final sortedServices = List<HaircutService>.from(_services);
    sortedServices.sort((a, b) => a.sortOrder.compareTo(b.sortOrder));
    return sortedServices;
  }

  // Get services by duration (less than or equal to specified minutes)
  List<HaircutService> getServicesByMaxDuration(int maxMinutes) {
    return _services
        .where((service) =>
            service.isActive && service.durationMinutes <= maxMinutes)
        .toList();
  }

  // Calculate total duration of multiple services
  int calculateTotalDuration(List<int> serviceIds) {
    int totalDuration = 0;
    for (final id in serviceIds) {
      final service = _services.firstWhere(
        (service) => service.id == id,
        orElse: () => HaircutService(
          id: 0,
          name: '',
          description: '',
          basePrice: 0,
          durationMinutes: 0,
          imageUrl: '',
          isActive: false,
          sortOrder: 0,
        ),
      );

      if (service.id != 0) {
        totalDuration += service.durationMinutes;
      }
    }
    return totalDuration;
  }

  // Calculate total price of multiple services
  double calculateTotalPrice(List<int> serviceIds) {
    double totalPrice = 0;
    for (final id in serviceIds) {
      final service = _services.firstWhere(
        (service) => service.id == id,
        orElse: () => HaircutService(
          id: 0,
          name: '',
          description: '',
          basePrice: 0,
          durationMinutes: 0,
          imageUrl: '',
          isActive: false,
          sortOrder: 0,
        ),
      );

      if (service.id != 0) {
        totalPrice += service.basePrice;
      }
    }
    return totalPrice;
  }
}
