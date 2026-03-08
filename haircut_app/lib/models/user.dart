
class User {
  final int id;
  final String username;
  final String name;
  final String email;
  final String role;
  final String avatar;
  final String phoneNumber;
  final bool isActive;

  User({
    required this.id,
    required this.username,
    required this.name,
    required this.email,
    required this.role,
    this.avatar = '',
    this.phoneNumber = '',
    this.isActive = true,
  });

  factory User.fromJson(Map<String, dynamic> json) {
    return User(
      id: json['id'] ?? 0,
      username: json['username'] ?? '',
      name: json['name'] ?? '',
      email: json['email'] ?? '',
      role: json['role'] ?? 'User',
      avatar: json['avatar'] ?? '',
      phoneNumber: json['phoneNumber'] ?? '',
      isActive: json['isActive'] ?? true,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'username': username,
      'name': name,
      'email': email,
      'role': role,
      'avatar': avatar,
      'phoneNumber': phoneNumber,
      'isActive': isActive,
    };
  }

  User copyWith({
    int? id,
    String? username,
    String? name,
    String? email,
    String? role,
    String? avatar,
    String? phoneNumber,
    bool? isActive,
  }) {
    return User(
      id: id ?? this.id,
      username: username ?? this.username,
      name: name ?? this.name,
      email: email ?? this.email,
      role: role ?? this.role,
      avatar: avatar ?? this.avatar,
      phoneNumber: phoneNumber ?? this.phoneNumber,
      isActive: isActive ?? this.isActive,
    );
  }

  bool get isAdmin => role.toLowerCase() == 'admin';
  bool get isBarber => role.toLowerCase().contains('barber');
}
