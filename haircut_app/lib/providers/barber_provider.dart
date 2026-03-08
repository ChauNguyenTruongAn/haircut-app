import 'package:flutter/foundation.dart';
import 'package:haircut_app/models/barber.dart';
import 'package:haircut_app/services/api_service.dart';
import 'package:haircut_app/config/app_config.dart';
import 'package:haircut_app/services/token_service.dart';
import 'dart:developer' as developer;

class BarberProvider with ChangeNotifier {
  final ApiService _apiService = ApiService(
      baseUrl: AppConfig.apiBaseUrl, tokenService: TokenService.instance);
  List<Barber> _barbers = [];
  bool _isLoading = false;
  String? _error;

  List<Barber> get barbers => _barbers;
  bool get isLoading => _isLoading;
  String? get error => _error;

  // Get all barbers
  Future<void> fetchBarbers() async {
    _isLoading = true;
    _error = null;
    notifyListeners();

    try {
      _barbers = await _apiService.getBarbers();
      _isLoading = false;
      notifyListeners();
    } catch (e) {
      _isLoading = false;
      _error = e.toString();
      developer.log('Error fetching barbers: $_error', name: 'BarberProvider');
      notifyListeners();
    }
  }

  // Get a specific barber
  Future<Barber?> fetchBarber(int id) async {
    _isLoading = true;
    _error = null;
    notifyListeners();

    try {
      final barber = await _apiService.getBarber(id);
      _isLoading = false;
      notifyListeners();
      return barber;
    } catch (e) {
      _isLoading = false;
      _error = e.toString();
      developer.log('Error fetching barber $id: $_error',
          name: 'BarberProvider');
      notifyListeners();
      return null;
    }
  }

  // Create a new barber
  Future<Barber?> createBarber(Barber barber) async {
    _isLoading = true;
    _error = null;
    notifyListeners();

    try {
      // Log request details
      developer.log('Creating barber: ${barber.name}', name: 'BarberProvider');
      developer.log(
          'Working hours: ${barber.startWorkingHour} - ${barber.endWorkingHour}',
          name: 'BarberProvider');

      final newBarber = await _apiService.createBarber(barber);
      _barbers.add(newBarber);
      _isLoading = false;
      notifyListeners();
      return newBarber;
    } catch (e) {
      _isLoading = false;
      _error = _parseError(e);
      developer.log('Error creating barber: $_error', name: 'BarberProvider');
      notifyListeners();
      return null;
    }
  }

  // Update an existing barber
  Future<Barber?> updateBarber(Barber barber) async {
    _isLoading = true;
    _error = null;
    notifyListeners();

    try {
      // Prepare the update data with only the fields we want to update
      final Map<String, dynamic> updateData = {
        'name': barber.name,
        'email': barber.email,
        'phone': barber.phone,
        'bio': barber.bio,
        'position': barber.position,
        'startWorkingHour': barber.startWorkingHour,
        'endWorkingHour': barber.endWorkingHour,
        'isActive': barber.isActive,
        'isAvailableForBooking': barber.isAvailableForBooking,
      };

      // Only include serviceIds if not empty
      if (barber.serviceIds.isNotEmpty) {
        updateData['serviceIds'] = barber.serviceIds;
      }

      // Only include avatarUrl if it's not an example URL
      if (barber.avatarUrl.isNotEmpty &&
          !barber.avatarUrl.contains('example.com')) {
        updateData['avatarUrl'] = barber.avatarUrl;
      }

      // Log request details
      developer.log('Updating barber: ${barber.name} (ID: ${barber.id})',
          name: 'BarberProvider');
      developer.log('Update data: $updateData', name: 'BarberProvider');

      final updatedBarber =
          await _apiService.updateBarber(barber.id, updateData);

      // Update the barber in the list
      final index = _barbers.indexWhere((b) => b.id == barber.id);
      if (index != -1) {
        _barbers[index] = updatedBarber;
        _isLoading = false;
        notifyListeners();
        return updatedBarber;
      } else {
        // If barber not found in list, refresh the entire list
        await fetchBarbers();
        _isLoading = false;
        notifyListeners();
        return updatedBarber;
      }
    } catch (e) {
      _isLoading = false;
      _error = _parseError(e);
      developer.log('Error updating barber: $_error', name: 'BarberProvider');
      notifyListeners();
      return null;
    }
  }

  // Delete a barber
  Future<bool> deleteBarber(int id) async {
    _isLoading = true;
    _error = null;
    notifyListeners();

    try {
      await _apiService.deleteBarber(id);
      _barbers.removeWhere((barber) => barber.id == id);
      _isLoading = false;
      notifyListeners();
      return true;
    } catch (e) {
      _isLoading = false;
      _error = _parseError(e);
      developer.log('Error deleting barber: $_error', name: 'BarberProvider');
      notifyListeners();
      return false;
    }
  }

  // Helper to parse error messages
  String _parseError(dynamic error) {
    String errorMessage = error.toString();

    // Check for common LocalTime errors
    if (errorMessage.contains('java.time.LocalTime')) {
      return 'Định dạng thời gian không hợp lệ. Vui lòng sử dụng định dạng HH:MM';
    }

    // Check if it's an internal server error
    if (errorMessage.contains('500') ||
        errorMessage.contains('Internal Server Error')) {
      return 'Lỗi máy chủ, vui lòng thử lại sau';
    }

    return errorMessage;
  }

  // Get active barbers only
  List<Barber> getActiveBarbers() {
    return _barbers.where((barber) => barber.isActive).toList();
  }

  // Get available barbers (active and available for booking)
  List<Barber> getAvailableBarbers() {
    return _barbers
        .where((barber) => barber.isActive && barber.isAvailableForBooking)
        .toList();
  }

  // Get barbers by service
  List<Barber> getBarbersByService(int serviceId) {
    return _barbers
        .where((barber) =>
            barber.isActive &&
            barber.isAvailableForBooking &&
            barber.serviceIds.contains(serviceId))
        .toList();
  }
}
