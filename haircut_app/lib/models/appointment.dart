import 'package:haircut_app/models/barber.dart';
import 'package:haircut_app/models/haircut_service.dart';

enum AppointmentStatus { BOOKED, CONFIRMED, CANCELLED, COMPLETED }

class Appointment {
  final int id;
  final String customerName;
  final String customerPhone;
  final String customerEmail;
  final DateTime date;
  final String startTime;
  final String endTime;
  final AppointmentStatus status;
  final String? notes;
  final double totalPrice;
  final int barberId;
  final int? customerId;
  final List<int> serviceIds;
  final Barber? barber;
  final List<HaircutService>? services;
  final DateTime createdAt;
  final DateTime? updatedAt;
  final bool isReminderSent;
  final String? senderId;

  Appointment({
    required this.id,
    required this.customerName,
    required this.customerPhone,
    required this.customerEmail,
    required this.date,
    required this.startTime,
    required this.endTime,
    required this.status,
    required this.notes,
    required this.totalPrice,
    required this.barberId,
    this.customerId,
    required this.serviceIds,
    this.barber,
    this.services,
    required this.createdAt,
    this.updatedAt,
    required this.isReminderSent,
    this.senderId,
  });

  factory Appointment.fromJson(Map<String, dynamic> json) {
    // Log để debug dữ liệu nhận từ server
    print('Parsing appointment JSON: ${json.keys}');

    // Xử lý danh sách dịch vụ
    List<int> serviceIds = [];
    List<HaircutService>? servicesList;

    try {
      // Kiểm tra và xử lý trường services từ backend
      if (json['services'] != null) {
        print('Services data type: ${json['services'].runtimeType}');

        if (json['services'] is List) {
          // Trường hợp services là một danh sách các đối tượng
          final servicesList = json['services'] as List;
          print('Services list length: ${servicesList.length}');

          serviceIds = servicesList
              .map<int>((service) {
                if (service is Map<String, dynamic>) {
                  return service['id'] as int;
                } else if (service is int) {
                  return service;
                }
                return 0; // Fallback value
              })
              .where((id) => id > 0)
              .toList();

          // Bỏ servicesList null nếu không có dịch vụ hợp lệ
          if (serviceIds.isEmpty) {
            print('No valid service IDs found in services list');
          }
        } else if (json['services'] is Set) {
          // Trường hợp services là một Set
          serviceIds = List<int>.from(json['services']);
        } else {
          print('Unexpected services type: ${json['services'].runtimeType}');
          serviceIds = [];
        }
      } else if (json['serviceIds'] != null) {
        // Nếu backend trả về dưới dạng serviceIds
        if (json['serviceIds'] is List) {
          serviceIds = List<int>.from(json['serviceIds']);
        } else if (json['serviceIds'] is Set) {
          serviceIds = List<int>.from(json['serviceIds']);
        }
      }

      // Xử lý chi tiết dịch vụ
      if (json['serviceDetails'] != null && json['serviceDetails'] is List) {
        final details = json['serviceDetails'] as List;
        if (details.isNotEmpty) {
          servicesList = [];

          for (var detail in details) {
            if (detail is Map<String, dynamic> && detail.containsKey('name')) {
              try {
                final service = HaircutService.fromJson(detail);
                servicesList.add(service);
              } catch (e) {
                print('Error parsing service detail: $e');
              }
            }
          }

          // Nếu không có dịch vụ hợp lệ, đặt servicesList = null
          if (servicesList.isEmpty) {
            servicesList = null;
          }
        }
      } else if (json['services'] is List) {
        final services = json['services'] as List;
        if (services.isNotEmpty && services.first is Map<String, dynamic>) {
          servicesList = [];

          for (var service in services) {
            if (service is Map<String, dynamic> &&
                service.containsKey('name')) {
              try {
                final serviceObj = HaircutService.fromJson(service);
                servicesList.add(serviceObj);
              } catch (e) {
                print('Error parsing service object: $e');
              }
            }
          }

          if (servicesList.isEmpty) {
            servicesList = null;
          }
        }
      }
    } catch (e) {
      print('Error processing services: $e');
      serviceIds = [];
      servicesList = null;
    }

    // Xử lý trạng thái
    AppointmentStatus status = AppointmentStatus.BOOKED;
    if (json['status'] != null) {
      final statusStr = json['status'].toString().toUpperCase();
      try {
        status = AppointmentStatus.values.firstWhere(
          (e) => e.toString().split('.').last == statusStr,
          orElse: () => AppointmentStatus.BOOKED,
        );
      } catch (e) {
        print('Error parsing appointment status: $e');
      }
    }

    // Xử lý các trường text
    String customerName = '';
    String customerPhone = '';
    String customerEmail = '';
    String notes = '';

    try {
      customerName = json['customerName']?.toString() ?? '';
      customerPhone = json['customerPhone']?.toString() ?? '';
      customerEmail = json['customerEmail']?.toString() ?? '';
      notes = json['notes']?.toString() ?? '';
    } catch (e) {
      print('Error parsing text fields: $e');
    }

    // Xử lý ngày giờ
    DateTime date = DateTime.now();
    String startTime = '';
    String endTime = '';
    DateTime createdAt = DateTime.now();
    DateTime? updatedAt;

    try {
      if (json['date'] != null) {
        date = DateTime.parse(json['date'].toString());
      }

      startTime = json['startTime']?.toString() ?? '';
      endTime = json['endTime']?.toString() ?? '';

      if (json['createdAt'] != null) {
        createdAt = DateTime.parse(json['createdAt'].toString());
      }

      if (json['updatedAt'] != null) {
        updatedAt = DateTime.parse(json['updatedAt'].toString());
      }
    } catch (e) {
      print('Error parsing date/time fields: $e');
    }

    // Xử lý các trường số
    int id = 0;
    int barberId = 0;
    int? customerId;
    double totalPrice = 0.0;
    bool isReminderSent = false;

    try {
      id = json['id'] is int ? json['id'] : 0;
      barberId = json['barberId'] is int ? json['barberId'] : 0;

      if (json['userId'] != null) {
        customerId = json['userId'] is int ? json['userId'] : null;
      } else if (json['customerId'] != null) {
        customerId = json['customerId'] is int ? json['customerId'] : null;
      }

      if (json['totalPrice'] != null) {
        if (json['totalPrice'] is int) {
          totalPrice = (json['totalPrice'] as int).toDouble();
        } else if (json['totalPrice'] is double) {
          totalPrice = json['totalPrice'];
        } else {
          try {
            totalPrice = double.parse(json['totalPrice'].toString());
          } catch (e) {
            totalPrice = 0.0;
          }
        }
      }

      isReminderSent = json['isReminderSent'] == true;
    } catch (e) {
      print('Error parsing numeric fields: $e');
    }

    // Xử lý barber
    Barber? barber;
    try {
      if (json['barber'] != null && json['barber'] is Map<String, dynamic>) {
        barber = Barber.fromJson(json['barber']);
      }
    } catch (e) {
      print('Error parsing barber: $e');
    }

    return Appointment(
      id: id,
      customerName: customerName,
      customerPhone: customerPhone,
      customerEmail: customerEmail,
      date: date,
      startTime: startTime,
      endTime: endTime,
      status: status,
      notes: notes,
      totalPrice: totalPrice,
      barberId: barberId,
      customerId: customerId,
      serviceIds: serviceIds,
      barber: barber,
      services: servicesList,
      createdAt: createdAt,
      updatedAt: updatedAt,
      isReminderSent: isReminderSent,
      senderId: json['senderId']?.toString(),
    );
  }

  Map<String, dynamic> toJson() {
    // Convert serviceIds list to a Map<String, Integer> where quantity is 1 for each service
    Map<String, dynamic> json = {
      'id': id,
      'customerName': customerName,
      'customerPhone': customerPhone,
      'customerEmail': customerEmail,
      // Match the field names expected by the API
      'appointmentDate': date.toIso8601String().split('T')[0],
      'appointmentTime': startTime,
      'appointmentTimeEnd': endTime,
      'status': status.toString().split('.').last,
      'note': notes, // Note the field name change from 'notes' to 'note'
      'totalPrice': totalPrice,
      'barberId': barberId,
    };

    // Create services map with string keys to ensure proper JSON encoding
    Map<String, int> servicesMap = {};
    for (int serviceId in serviceIds) {
      servicesMap[serviceId.toString()] = 1;
    }
    json['services'] = servicesMap;

    // Only add userId if customerId is not null
    if (customerId != null) {
      json['userId'] = customerId;
    }

    return json;
  }

  Appointment copyWith({
    int? id,
    String? customerName,
    String? customerPhone,
    String? customerEmail,
    DateTime? date,
    String? startTime,
    String? endTime,
    AppointmentStatus? status,
    String? notes,
    double? totalPrice,
    int? barberId,
    int? customerId,
    List<int>? serviceIds,
    Barber? barber,
    List<HaircutService>? services,
    DateTime? createdAt,
    DateTime? updatedAt,
    bool? isReminderSent,
    String? senderId,
  }) {
    return Appointment(
      id: id ?? this.id,
      customerName: customerName ?? this.customerName,
      customerPhone: customerPhone ?? this.customerPhone,
      customerEmail: customerEmail ?? this.customerEmail,
      date: date ?? this.date,
      startTime: startTime ?? this.startTime,
      endTime: endTime ?? this.endTime,
      status: status ?? this.status,
      notes: notes ?? this.notes,
      totalPrice: totalPrice ?? this.totalPrice,
      barberId: barberId ?? this.barberId,
      customerId: customerId ?? this.customerId,
      serviceIds: serviceIds ?? this.serviceIds,
      barber: barber ?? this.barber,
      services: services ?? this.services,
      createdAt: createdAt ?? this.createdAt,
      updatedAt: updatedAt ?? this.updatedAt,
      isReminderSent: isReminderSent ?? this.isReminderSent,
      senderId: senderId ?? this.senderId,
    );
  }
}
