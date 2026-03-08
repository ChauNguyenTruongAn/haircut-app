import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:haircut_app/models/haircut_service.dart';
import 'package:haircut_app/providers/service_provider.dart';
import 'package:haircut_app/providers/auth_provider.dart';
import 'package:haircut_app/screens/service_form_screen.dart';
import 'package:intl/intl.dart';

class ServiceManagementScreen extends StatefulWidget {
  const ServiceManagementScreen({super.key});

  @override
  State<ServiceManagementScreen> createState() =>
      _ServiceManagementScreenState();
}

class _ServiceManagementScreenState extends State<ServiceManagementScreen> {
  final bool _isInit = false;
  final NumberFormat _currencyFormatter = NumberFormat('#,###', 'vi_VN');

  String formatPrice(double price) {
    return '${_currencyFormatter.format(price)}đ';
  }

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      Provider.of<ServiceProvider>(context, listen: false).fetchServices();
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Quản Lý Dịch Vụ'),
        actions: [
          IconButton(
            icon: const Icon(Icons.add),
            onPressed: () {
              Navigator.push(
                context,
                MaterialPageRoute(
                  builder: (context) => const ServiceFormScreen(),
                ),
              );
            },
          ),
        ],
      ),
      body: Consumer<ServiceProvider>(
        builder: (context, serviceProvider, child) {
          if (serviceProvider.isLoading) {
            return const Center(child: CircularProgressIndicator());
          }

          if (serviceProvider.error != null) {
            return Center(
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Text(
                    serviceProvider.error!,
                    style: const TextStyle(color: Colors.red),
                  ),
                  const SizedBox(height: 16),
                  ElevatedButton(
                    onPressed: () {
                      serviceProvider.fetchServices();
                    },
                    child: const Text('Thử lại'),
                  ),
                ],
              ),
            );
          }

          final services = serviceProvider.getSortedServices();

          if (services.isEmpty) {
            return const Center(
              child: Text('Chưa có dịch vụ nào'),
            );
          }

          return RefreshIndicator(
            onRefresh: () => serviceProvider.fetchServices(),
            child: ListView.builder(
              padding: const EdgeInsets.all(16),
              itemCount: services.length,
              itemBuilder: (context, index) {
                final service = services[index];
                return Card(
                  margin: const EdgeInsets.only(bottom: 16),
                  child: ListTile(
                    contentPadding: const EdgeInsets.all(16),
                    leading: CircleAvatar(
                      backgroundColor:
                          Theme.of(context).primaryColor.withOpacity(0.1),
                      child: Icon(
                        _getIconForService(service.name),
                        color: Theme.of(context).primaryColor,
                      ),
                    ),
                    title: Text(
                      service.name,
                      style: const TextStyle(
                        fontWeight: FontWeight.bold,
                        fontSize: 16,
                      ),
                    ),
                    subtitle: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        if (service.description.isNotEmpty)
                          Padding(
                            padding: const EdgeInsets.only(top: 4),
                            child: Text(
                              service.description,
                              style: TextStyle( 
                                color: Colors.grey[600],
                                fontSize: 14,
                              ),
                            ),
                          ),
                        const SizedBox(height: 8),
                        Row(
                          children: [
                            Icon(
                              Icons.access_time,
                              size: 16,
                              color: Colors.grey[600],
                            ),
                            const SizedBox(width: 2),
                            Text(
                              '${service.durationMinutes} phút',
                              style: TextStyle(
                                color: Colors.grey[600],
                                fontSize: 14,
                              ),
                            ),
                            const SizedBox(width: 8),
                            Icon(
                              Icons.attach_money,
                              size: 16,
                              color: Colors.grey[600],
                            ),
                            const SizedBox(width: 0),
                            Text(
                              formatPrice(service.basePrice),
                              style: TextStyle(
                                color: Colors.grey[600],
                                fontSize: 14,
                              ),
                            ),
                          ],
                        ),
                      ],
                    ),
                    trailing: Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        IconButton(
                          icon: const Icon(Icons.edit),
                          onPressed: () {
                            Navigator.push(
                              context,
                              MaterialPageRoute(
                                builder: (context) => ServiceFormScreen(
                                  service: service,
                                ),
                              ),
                            );
                          },
                        ),
                        IconButton(
                          icon: const Icon(Icons.delete),
                          onPressed: () {
                            _showDeleteConfirmationDialog(context, service);
                          },
                        ),
                      ],
                    ),
                  ),
                );
              },
            ),
          );
        },
      ),
    );
  }

  void _showDeleteConfirmationDialog(
      BuildContext context, HaircutService service) {
    final scaffoldMessenger = ScaffoldMessenger.of(context);
    showDialog(
      context: context,
      builder: (dialogContext) => AlertDialog(
        title: const Text('Xác nhận xóa'),
        content: Text('Bạn có chắc chắn muốn xóa dịch vụ "${service.name}"?'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(dialogContext),
            child: const Text('Hủy'),
          ),
          TextButton(
            onPressed: () async {
              Navigator.pop(dialogContext);
              final success =
                  await Provider.of<ServiceProvider>(context, listen: false)
                      .deleteService(service.id);
              if (success) {
                scaffoldMessenger.showSnackBar(
                  const SnackBar(
                    content: Text('Đã xóa dịch vụ thành công'),
                    backgroundColor: Colors.green,
                  ),
                );
              }
            },
            child: const Text('Xóa'),
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
}
