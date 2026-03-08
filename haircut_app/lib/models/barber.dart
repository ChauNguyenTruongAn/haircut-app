class Barber {
  final int id;
  final String name;
  final String email;
  final String phone;
  final String bio;
  final String position;
  final String avatarUrl;
  final bool isActive;
  final bool isAvailableForBooking;
  final String startWorkingHour;
  final String endWorkingHour;
  final List<int> serviceIds; // IDs of services the barber can perform

  Barber({
    required this.id,
    required this.name,
    required this.email,
    required this.phone,
    required this.bio,
    required this.position,
    required this.avatarUrl,
    required this.isActive,
    required this.isAvailableForBooking,
    required this.startWorkingHour,
    required this.endWorkingHour,
    required this.serviceIds,
  });

  factory Barber.fromJson(Map<String, dynamic> json) {
    // Log để debug
    print('Parsing barber: ${json.keys.toList()}');

    // Xử lý trường serviceIds
    List<int> serviceIds = [];
    try {
      if (json['services'] != null) {
        if (json['services'] is List) {
          // Trường hợp trả về dạng danh sách đối tượng service
          serviceIds = (json['services'] as List)
              .map((service) => service is Map
                  ? service['id'] as int
                  : (service is int ? service : 0))
              .where((id) => id > 0)
              .toList();
        } else if (json['services'] is Map) {
          // Trường hợp trả về dạng object với các key là serviceId
          serviceIds = (json['services'] as Map)
              .keys
              .map((key) => int.tryParse(key.toString()) ?? 0)
              .where((id) => id > 0)
              .toList();
        }
      } else if (json['serviceIds'] != null && json['serviceIds'] is List) {
        // Trường hợp backend trả về dưới dạng serviceIds
        serviceIds = List<int>.from(json['serviceIds']);
      }
    } catch (e) {
      print('Error parsing service IDs: $e');
    }

    // Xử lý thời gian làm việc
    String startWorkingHour = '09:00';
    String endWorkingHour = '17:00';

    if (json['startWorkingHour'] != null) {
      if (json['startWorkingHour'] is String) {
        startWorkingHour = json['startWorkingHour'];
      } else if (json['startWorkingHour'] is Map) {
        startWorkingHour =
            '${json['startWorkingHour']['hour']}:${json['startWorkingHour']['minute']}';
      }
    }

    if (json['endWorkingHour'] != null) {
      if (json['endWorkingHour'] is String) {
        endWorkingHour = json['endWorkingHour'];
      } else if (json['endWorkingHour'] is Map) {
        endWorkingHour =
            '${json['endWorkingHour']['hour']}:${json['endWorkingHour']['minute']}';
      }
    }

    return Barber(
      id: json['id'] is int
          ? json['id']
          : int.tryParse(json['id'].toString()) ?? 0,
      name: json['name']?.toString() ?? '',
      email: json['email']?.toString() ?? '',
      phone: json['phone']?.toString() ?? '',
      bio: json['bio']?.toString() ?? '',
      position: json['position']?.toString() ?? '',
      avatarUrl: json['avatarUrl']?.toString() ?? '',
      isActive: json['isActive'] is bool ? json['isActive'] : false,
      isAvailableForBooking: json['isAvailableForBooking'] is bool
          ? json['isAvailableForBooking']
          : false,
      startWorkingHour: startWorkingHour,
      endWorkingHour: endWorkingHour,
      serviceIds: serviceIds,
    );
  }

  Map<String, dynamic> toJson() {
    // Ensure time format is HH:MM
    String validateTimeFormat(String time) {
      // If the format is already HH:MM, return as is
      if (RegExp(r'^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$').hasMatch(time)) {
        // Ensure double-digit hours
        final parts = time.split(':');
        final hour = parts[0].padLeft(2, '0');
        final minute = parts[1].padLeft(2, '0');
        return '$hour:$minute';
      }
      // Default to 09:00 if invalid format
      return '09:00';
    }

    // Tạo JSON cơ bản, bỏ qua ID nếu đang tạo mới (ID = 0)
    final Map<String, dynamic> json = {
      'name': name,
      'email': email,
      'phone': phone,
      'bio': bio,
      'position': position,
      'isActive': isActive,
      'isAvailableForBooking': isAvailableForBooking,
      'startWorkingHour': validateTimeFormat(startWorkingHour),
      'endWorkingHour': validateTimeFormat(endWorkingHour),
    };

    // Chỉ thêm avatarUrl nếu không phải URL example.com
    if (avatarUrl.isNotEmpty && !avatarUrl.contains('example.com')) {
      json['avatarUrl'] = avatarUrl;
    }

    // Chỉ gửi serviceIds nếu không phải là danh sách rỗng
    if (serviceIds.isNotEmpty) {
      json['serviceIds'] = serviceIds;
    }

    // Chỉ thêm ID nếu không phải tạo mới
    if (id != 0) {
      json['id'] = id;
    }

    return json;
  }

  Barber copyWith({
    int? id,
    String? name,
    String? email,
    String? phone,
    String? bio,
    String? position,
    String? avatarUrl,
    bool? isActive,
    bool? isAvailableForBooking,
    String? startWorkingHour,
    String? endWorkingHour,
    List<int>? serviceIds,
  }) {
    return Barber(
      id: id ?? this.id,
      name: name ?? this.name,
      email: email ?? this.email,
      phone: phone ?? this.phone,
      bio: bio ?? this.bio,
      position: position ?? this.position,
      avatarUrl: avatarUrl ?? this.avatarUrl,
      isActive: isActive ?? this.isActive,
      isAvailableForBooking:
          isAvailableForBooking ?? this.isAvailableForBooking,
      startWorkingHour: startWorkingHour ?? this.startWorkingHour,
      endWorkingHour: endWorkingHour ?? this.endWorkingHour,
      serviceIds: serviceIds ?? this.serviceIds,
    );
  }
}
