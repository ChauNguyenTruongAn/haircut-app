class HaircutService {
  final int id;
  final String name;
  final String description;
  final double basePrice;
  final int durationMinutes;
  final String imageUrl;
  final bool isActive;
  final int sortOrder;

  HaircutService({
    required this.id,
    required this.name,
    required this.description,
    required this.basePrice,
    required this.durationMinutes,
    required this.imageUrl,
    required this.isActive,
    required this.sortOrder,
  });

  factory HaircutService.fromJson(Map<String, dynamic> json) {
    return HaircutService(
      id: json['id'],
      name: json['name'],
      description: json['description'] ?? '',
      basePrice: json['basePrice'] is int
          ? (json['basePrice'] as int).toDouble()
          : json['basePrice'],
      durationMinutes: json['durationMinutes'],
      imageUrl: json['imageUrl'] ?? '',
      isActive: json['isActive'] ?? true,
      sortOrder: json['sortOrder'] ?? 0,
    );
  }

  Map<String, dynamic> toJson() {
    final map = {
      'name': name,
      'description': description,
      'basePrice': basePrice,
      'durationMinutes': durationMinutes,
      'imageUrl': imageUrl,
      'isActive': isActive,
      'sortOrder': sortOrder,
    };

    // Only include ID if it's not 0 (creating a new service)
    if (id != 0) {
      map['id'] = id;
    }

    return map;
  }

  HaircutService copyWith({
    int? id,
    String? name,
    String? description,
    double? basePrice,
    int? durationMinutes,
    String? imageUrl,
    bool? isActive,
    int? sortOrder,
  }) {
    return HaircutService(
      id: id ?? this.id,
      name: name ?? this.name,
      description: description ?? this.description,
      basePrice: basePrice ?? this.basePrice,
      durationMinutes: durationMinutes ?? this.durationMinutes,
      imageUrl: imageUrl ?? this.imageUrl,
      isActive: isActive ?? this.isActive,
      sortOrder: sortOrder ?? this.sortOrder,
    );
  }
}
