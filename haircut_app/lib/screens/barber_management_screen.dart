import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:haircut_app/models/barber.dart';
import 'package:haircut_app/providers/barber_provider.dart';
import 'package:haircut_app/providers/auth_provider.dart';
import 'package:haircut_app/providers/service_provider.dart';
import 'package:haircut_app/models/haircut_service.dart';

class BarberManagementScreen extends StatefulWidget {
  const BarberManagementScreen({super.key});

  @override
  State<BarberManagementScreen> createState() => _BarberManagementScreenState();
}

class _BarberManagementScreenState extends State<BarberManagementScreen> {
  bool _isInit = false;

  @override
  void initState() {
    super.initState();
    Future.microtask(() async {
      await Provider.of<BarberProvider>(context, listen: false).fetchBarbers();
      await Provider.of<ServiceProvider>(context, listen: false)
          .fetchServices();
    });
  }

  @override
  void didChangeDependencies() {
    if (!_isInit) {
      _isInit = true;
    }
    super.didChangeDependencies();
  }

  @override
  Widget build(BuildContext context) {
    final barberProvider = Provider.of<BarberProvider>(context);
    final isAdmin = Provider.of<AuthProvider>(context).isAdmin;

    return Scaffold(
      appBar: AppBar(
        title: const Text('Quản lý Barber'),
      ),
      body: SafeArea(
        child: barberProvider.isLoading
            ? const Center(child: CircularProgressIndicator())
            : barberProvider.error != null
                ? Center(
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        const Icon(
                          Icons.error_outline,
                          color: Colors.red,
                          size: 60,
                        ),
                        const SizedBox(height: 16),
                        Text(
                          'Lỗi: ${barberProvider.error}',
                          style: Theme.of(context).textTheme.titleMedium,
                          textAlign: TextAlign.center,
                        ),
                        const SizedBox(height: 16),
                        ElevatedButton(
                          onPressed: () {
                            barberProvider.fetchBarbers();
                          },
                          child: const Text('Thử lại'),
                        ),
                      ],
                    ),
                  )
                : barberProvider.barbers.isEmpty
                    ? const Center(
                        child: Text('Chưa có barber nào'),
                      )
                    : ListView.builder(
                        itemCount: barberProvider.barbers.length,
                        itemBuilder: (ctx, index) {
                          final barber = barberProvider.barbers[index];
                          return _buildBarberItem(context, barber, isAdmin);
                        },
                      ),
      ),
      floatingActionButton: isAdmin
          ? FloatingActionButton(
              onPressed: () {
                _showBarberDialog(context);
              },
              child: const Icon(Icons.add),
            )
          : null,
    );
  }

  Widget _buildBarberItem(BuildContext context, Barber barber, bool isAdmin) {
    String? safeAvatarUrl = barber.avatarUrl;
    bool isValidAvatarUrl = safeAvatarUrl.isNotEmpty &&
        !safeAvatarUrl.contains('example.com');

    return Card(
      margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      child: ListTile(
        leading: CircleAvatar(
          backgroundColor: Theme.of(context).primaryColor,
          backgroundImage: isValidAvatarUrl
              ? NetworkImage(safeAvatarUrl) as ImageProvider
              : null,
          child: !isValidAvatarUrl
              ? Text(
                  barber.name.isNotEmpty
                      ? barber.name.substring(0, 1).toUpperCase()
                      : 'B',
                  style: const TextStyle(color: Colors.white),
                )
              : null,
        ),
        title: Text(barber.name),
        subtitle: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(barber.position),
            Text(
                'Giờ làm việc: ${barber.startWorkingHour} - ${barber.endWorkingHour}'),
          ],
        ),
        trailing: isAdmin
            ? Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  IconButton(
                    icon: const Icon(Icons.edit, color: Colors.blue),
                    onPressed: () {
                      _showBarberDialog(context, barber);
                    },
                  ),
                  IconButton(
                    icon: const Icon(Icons.delete, color: Colors.red),
                    onPressed: () {
                      _confirmDelete(context, barber);
                    },
                  ),
                ],
              )
            : null,
        onTap: () {
          // Show barber details
          _showBarberDetails(context, barber);
        },
      ),
    );
  }

  void _showBarberDetails(BuildContext context, Barber barber) {
    // Lấy danh sách dịch vụ từ provider
    final serviceProvider =
        Provider.of<ServiceProvider>(context, listen: false);
    final allServices = serviceProvider.services;

    // Lọc ra các dịch vụ mà barber có thể thực hiện
    final barberServices = allServices
        .where((service) => barber.serviceIds.contains(service.id))
        .toList();

    // Kiểm tra URL ảnh
    String? safeAvatarUrl = barber.avatarUrl;
    bool isValidAvatarUrl = safeAvatarUrl.isNotEmpty &&
        !safeAvatarUrl.contains('example.com');

    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        title: Text(barber.name),
        content: SingleChildScrollView(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            mainAxisSize: MainAxisSize.min,
            children: [
              Center(
                child: CircleAvatar(
                  radius: 50,
                  backgroundColor:
                      Theme.of(context).primaryColor.withOpacity(0.8),
                  backgroundImage: isValidAvatarUrl
                      ? NetworkImage(safeAvatarUrl) as ImageProvider
                      : null,
                  child: !isValidAvatarUrl
                      ? Text(
                          barber.name.isNotEmpty
                              ? barber.name.substring(0, 1).toUpperCase()
                              : 'B',
                          style: TextStyle(
                            color: Colors.white,
                            fontSize: 40,
                            fontWeight: FontWeight.bold,
                          ),
                        )
                      : null,
                ),
              ),
              const SizedBox(height: 16),
              Text('Vị trí: ${barber.position}'),
              const SizedBox(height: 8),
              Text('Email: ${barber.email}'),
              const SizedBox(height: 8),
              Text('Điện thoại: ${barber.phone}'),
              const SizedBox(height: 8),
              Text(
                  'Giờ làm việc: ${barber.startWorkingHour} - ${barber.endWorkingHour}'),
              const SizedBox(height: 8),
              Text(
                  'Trạng thái: ${barber.isActive ? "Hoạt động" : "Không hoạt động"}'),
              const SizedBox(height: 8),
              Text(
                  'Nhận đặt lịch: ${barber.isAvailableForBooking ? "Có" : "Không"}'),
              const SizedBox(height: 16),
              Text('Giới thiệu:',
                  style: Theme.of(context).textTheme.titleMedium),
              const SizedBox(height: 8),
              Text(barber.bio),
              const SizedBox(height: 16),
              Text('Dịch vụ thực hiện:',
                  style: Theme.of(context).textTheme.titleMedium),
              const SizedBox(height: 8),
              if (barberServices.isEmpty)
                Text('Không có dịch vụ nào',
                    style: TextStyle(fontStyle: FontStyle.italic))
              else
                Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: barberServices.map((service) {
                    return Padding(
                      padding: const EdgeInsets.only(bottom: 8.0),
                      child: Row(
                        children: [
                          Icon(
                            _getIconForService(service.name),
                            size: 20,
                            color: Theme.of(context).primaryColor,
                          ),
                          SizedBox(width: 8),
                          Expanded(
                            child: Text(
                              '${service.name} (${service.basePrice.toStringAsFixed(0)}đ, ${service.durationMinutes} phút)',
                              style: TextStyle(
                                fontWeight: FontWeight.w500,
                              ),
                            ),
                          ),
                        ],
                      ),
                    );
                  }).toList(),
                ),
            ],
          ),
        ),
        actions: [
          TextButton(
            onPressed: () {
              Navigator.of(ctx).pop();
            },
            child: const Text('Đóng'),
          ),
        ],
      ),
    );
  }

  IconData _getIconForService(String serviceName) {
    final name = serviceName.toLowerCase();
    if (name.contains('cut') || name.contains('trim') || name.contains('cắt')) {
      return Icons.content_cut;
    } else if (name.contains('color') ||
        name.contains('dye') ||
        name.contains('nhuộm')) {
      return Icons.color_lens;
    } else if (name.contains('style') ||
        name.contains('perm') ||
        name.contains('uốn')) {
      return Icons.brush;
    } else if (name.contains('wash') ||
        name.contains('shampoo') ||
        name.contains('gội')) {
      return Icons.water_drop;
    } else if (name.contains('beard') ||
        name.contains('shave') ||
        name.contains('râu')) {
      return Icons.face;
    } else {
      return Icons.spa;
    }
  }

  void _confirmDelete(BuildContext context, Barber barber) {
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Xác nhận xóa'),
        content: Text('Bạn có chắc muốn xóa ${barber.name}?'),
        actions: [
          TextButton(
            onPressed: () {
              Navigator.of(ctx).pop();
            },
            child: const Text('Hủy'),
          ),
          TextButton(
            onPressed: () {
              Navigator.of(ctx).pop();
              Provider.of<BarberProvider>(context, listen: false)
                  .deleteBarber(barber.id);
            },
            style: TextButton.styleFrom(foregroundColor: Colors.red),
            child: const Text('Xóa'),
          ),
        ],
      ),
    );
  }

  void _showBarberDialog(BuildContext context, [Barber? barber]) {
    final formKey = GlobalKey<FormState>();
    final nameController = TextEditingController(text: barber?.name ?? '');
    final emailController = TextEditingController(text: barber?.email ?? '');
    final phoneController = TextEditingController(text: barber?.phone ?? '');
    final bioController = TextEditingController(text: barber?.bio ?? '');
    final positionController =
        TextEditingController(text: barber?.position ?? '');
    final avatarUrlController =
        TextEditingController(text: barber?.avatarUrl ?? '');
    final startWorkingHourController =
        TextEditingController(text: barber?.startWorkingHour ?? '09:00');
    final endWorkingHourController =
        TextEditingController(text: barber?.endWorkingHour ?? '17:00');

    bool isActive = barber?.isActive ?? true;
    bool isAvailableForBooking = barber?.isAvailableForBooking ?? true;
    bool isSaving = false;
    String? errorMessage;

    // Danh sách dịch vụ đã chọn
    List<int> selectedServiceIds = List.from(barber?.serviceIds ?? []);

    // Kiểm tra định dạng thời gian hợp lệ (format HH:MM)
    bool isValidTimeFormat(String time) {
      final pattern = RegExp(r'^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$');
      return pattern.hasMatch(time);
    }

    // Hiển thị time picker
    Future<void> selectTime(TextEditingController controller) async {
      TimeOfDay initialTime = TimeOfDay(
        hour: int.tryParse(controller.text.split(':')[0]) ?? 9,
        minute: int.tryParse(controller.text.split(':')[1]) ?? 0,
      );

      final TimeOfDay? pickedTime = await showTimePicker(
        context: context,
        initialTime: initialTime,
      );

      if (pickedTime != null) {
        final hour = pickedTime.hour.toString().padLeft(2, '0');
        final minute = pickedTime.minute.toString().padLeft(2, '0');
        controller.text = '$hour:$minute';
      }
    }

    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (ctx) => StatefulBuilder(
        builder: (context, setState) {
          // Lấy danh sách dịch vụ từ provider
          final serviceProvider = Provider.of<ServiceProvider>(context);
          final services = serviceProvider.services;

          return Dialog(
            child: ConstrainedBox(
              constraints: BoxConstraints(
                maxHeight: MediaQuery.of(context).size.height * 0.8,
                maxWidth: MediaQuery.of(context).size.width * 0.9,
              ),
              child: Padding(
                padding: const EdgeInsets.all(16.0),
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Text(
                      barber == null ? 'Thêm Barber' : 'Sửa Barber',
                      style: Theme.of(context).textTheme.titleLarge,
                    ),
                    const SizedBox(height: 16),
                    Expanded(
                      child: SingleChildScrollView(
                        child: Form(
                          key: formKey,
                          child: Column(
                            mainAxisSize: MainAxisSize.min,
                            children: [
                              if (errorMessage != null)
                                Container(
                                  padding: const EdgeInsets.all(8),
                                  margin: const EdgeInsets.only(bottom: 16),
                                  decoration: BoxDecoration(
                                    color: Colors.red.shade50,
                                    border:
                                        Border.all(color: Colors.red.shade200),
                                    borderRadius: BorderRadius.circular(4),
                                  ),
                                  child: Row(
                                    children: [
                                      Icon(Icons.error_outline,
                                          color: Colors.red),
                                      SizedBox(width: 8),
                                      Expanded(
                                        child: Text(
                                          errorMessage!,
                                          style: TextStyle(
                                              color: Colors.red.shade800),
                                        ),
                                      ),
                                    ],
                                  ),
                                ),
                              TextFormField(
                                controller: nameController,
                                decoration:
                                    const InputDecoration(labelText: 'Tên'),
                                validator: (value) {
                                  if (value == null || value.isEmpty) {
                                    return 'Vui lòng nhập tên';
                                  }
                                  return null;
                                },
                              ),
                              TextFormField(
                                controller: emailController,
                                decoration:
                                    const InputDecoration(labelText: 'Email'),
                                validator: (value) {
                                  if (value == null || value.isEmpty) {
                                    return 'Vui lòng nhập email';
                                  }
                                  return null;
                                },
                              ),
                              TextFormField(
                                controller: phoneController,
                                decoration: const InputDecoration(
                                    labelText: 'Điện thoại'),
                                validator: (value) {
                                  if (value == null || value.isEmpty) {
                                    return 'Vui lòng nhập số điện thoại';
                                  }
                                  return null;
                                },
                              ),
                              TextFormField(
                                controller: positionController,
                                decoration:
                                    const InputDecoration(labelText: 'Vị trí'),
                              ),
                              TextFormField(
                                controller: avatarUrlController,
                                decoration: const InputDecoration(
                                  labelText:
                                      'URL Ảnh đại diện (để trống nếu không có)',
                                  hintText: 'https://...',
                                ),
                              ),
                              TextFormField(
                                controller: bioController,
                                decoration: const InputDecoration(
                                    labelText: 'Giới thiệu'),
                                maxLines: 3,
                              ),
                              Row(
                                children: [
                                  Expanded(
                                    child: TextFormField(
                                      controller: startWorkingHourController,
                                      decoration: const InputDecoration(
                                        labelText: 'Giờ bắt đầu',
                                        hintText: 'HH:MM',
                                      ),
                                      readOnly: true,
                                      onTap: () => selectTime(
                                          startWorkingHourController),
                                      validator: (value) {
                                        if (value == null || value.isEmpty) {
                                          return 'Bắt buộc';
                                        }
                                        if (!isValidTimeFormat(value)) {
                                          return 'Định dạng HH:MM';
                                        }
                                        return null;
                                      },
                                    ),
                                  ),
                                  const SizedBox(width: 16),
                                  Expanded(
                                    child: TextFormField(
                                      controller: endWorkingHourController,
                                      decoration: const InputDecoration(
                                        labelText: 'Giờ kết thúc',
                                        hintText: 'HH:MM',
                                      ),
                                      readOnly: true,
                                      onTap: () => selectTime(
                                          endWorkingHourController),
                                      validator: (value) {
                                        if (value == null || value.isEmpty) {
                                          return 'Bắt buộc';
                                        }
                                        if (!isValidTimeFormat(value)) {
                                          return 'Định dạng HH:MM';
                                        }
                                        return null;
                                      },
                                    ),
                                  ),
                                ],
                              ),
                              const SizedBox(height: 16),
                              SwitchListTile(
                                title: const Text('Hoạt động'),
                                value: isActive,
                                onChanged: (value) {
                                  setState(() {
                                    isActive = value;
                                  });
                                },
                              ),
                              SwitchListTile(
                                title: const Text('Nhận đặt lịch'),
                                value: isAvailableForBooking,
                                onChanged: (value) {
                                  setState(() {
                                    isAvailableForBooking = value;
                                  });
                                },
                              ),

                              // Phần chọn dịch vụ
                              const SizedBox(height: 16),
                              Align(
                                alignment: Alignment.centerLeft,
                                child: Text(
                                  'Dịch vụ có thể thực hiện:',
                                  style:
                                      Theme.of(context).textTheme.titleMedium,
                                ),
                              ),
                              const SizedBox(height: 8),

                              if (serviceProvider.isLoading)
                                Center(child: CircularProgressIndicator())
                              else if (services.isEmpty)
                                Text('Không có dịch vụ nào',
                                    style:
                                        TextStyle(fontStyle: FontStyle.italic))
                              else
                                SizedBox(
                                  height: 200,
                                  child: Container(
                                    decoration: BoxDecoration(
                                      border: Border.all(
                                          color: Colors.grey.shade300),
                                      borderRadius: BorderRadius.circular(8),
                                    ),
                                    child: ListView.builder(
                                      shrinkWrap: true,
                                      physics: ClampingScrollPhysics(),
                                      itemCount: services.length,
                                      itemBuilder: (context, index) {
                                        final service = services[index];
                                        return CheckboxListTile(
                                          title: Text(service.name),
                                          subtitle: Text(
                                              '${service.basePrice.toStringAsFixed(0)}đ - ${service.durationMinutes} phút'),
                                          value: selectedServiceIds
                                              .contains(service.id),
                                          onChanged: (bool? selected) {
                                            setState(() {
                                              if (selected == true) {
                                                if (!selectedServiceIds
                                                    .contains(service.id)) {
                                                  selectedServiceIds
                                                      .add(service.id);
                                                }
                                              } else {
                                                selectedServiceIds.removeWhere(
                                                    (id) => id == service.id);
                                              }
                                            });
                                          },
                                        );
                                      },
                                    ),
                                  ),
                                ),
                            ],
                          ),
                        ),
                      ),
                    ),
                    const SizedBox(height: 16),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.end,
                      children: [
                        TextButton(
                          onPressed: isSaving
                              ? null
                              : () {
                                  Navigator.of(ctx).pop();
                                },
                          child: const Text('Hủy'),
                        ),
                        const SizedBox(width: 8),
                        isSaving
                            ? Container(
                                margin: const EdgeInsets.only(left: 16),
                                width: 24,
                                height: 24,
                                child:
                                    CircularProgressIndicator(strokeWidth: 2),
                              )
                            : TextButton(
                                onPressed: () async {
                                  if (formKey.currentState!.validate()) {
                                    setState(() {
                                      isSaving = true;
                                      errorMessage = null;
                                    });

                                    try {
                                      final barberProvider =
                                          Provider.of<BarberProvider>(context,
                                              listen: false);

                                      // Create or update barber
                                      final newBarber = Barber(
                                        id: barber?.id ?? 0,
                                        name: nameController.text,
                                        email: emailController.text,
                                        phone: phoneController.text,
                                        bio: bioController.text,
                                        position: positionController.text,
                                        avatarUrl: avatarUrlController.text
                                                .contains('example.com')
                                            ? '' // Nếu là URL example.com, thì đặt thành chuỗi rỗng
                                            : avatarUrlController.text,
                                        isActive: isActive,
                                        isAvailableForBooking:
                                            isAvailableForBooking,
                                        startWorkingHour:
                                            startWorkingHourController.text,
                                        endWorkingHour:
                                            endWorkingHourController.text,
                                        serviceIds: selectedServiceIds,
                                      );

                                      if (barber == null) {
                                        await barberProvider
                                            .createBarber(newBarber);
                                        ScaffoldMessenger.of(context)
                                            .showSnackBar(
                                          SnackBar(
                                            content: Text(
                                                'Đã tạo ${newBarber.name} thành công'),
                                            backgroundColor: Colors.green,
                                          ),
                                        );
                                      } else {
                                        await barberProvider
                                            .updateBarber(newBarber);
                                        ScaffoldMessenger.of(context)
                                            .showSnackBar(
                                          SnackBar(
                                            content: Text(
                                                'Đã cập nhật ${newBarber.name} thành công'),
                                            backgroundColor: Colors.green,
                                          ),
                                        );
                                      }

                                      Navigator.of(ctx).pop();
                                    } catch (e) {
                                      setState(() {
                                        isSaving = false;
                                        errorMessage = e.toString();
                                      });
                                    }
                                  }
                                },
                                child: const Text('Lưu'),
                              ),
                      ],
                    ),
                  ],
                ),
              ),
            ),
          );
        },
      ),
    );
  }
}
