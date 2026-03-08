import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:haircut_app/providers/service_provider.dart';
import 'package:haircut_app/models/haircut_service.dart';

class ServiceFormScreen extends StatefulWidget {
  final HaircutService? service;

  const ServiceFormScreen({super.key, this.service});

  @override
  _ServiceFormScreenState createState() => _ServiceFormScreenState();
}

class _ServiceFormScreenState extends State<ServiceFormScreen> {
  final _formKey = GlobalKey<FormState>();
  late final TextEditingController _nameController;
  late final TextEditingController _descriptionController;
  late final TextEditingController _priceController;
  late final TextEditingController _durationController;
  late final TextEditingController _sortOrderController;
  late bool _isActive;

  @override
  void initState() {
    super.initState();
    _nameController = TextEditingController(text: widget.service?.name ?? '');
    _descriptionController =
        TextEditingController(text: widget.service?.description ?? '');
    _priceController =
        TextEditingController(text: widget.service?.basePrice.toString() ?? '');
    _durationController = TextEditingController(
        text: widget.service?.durationMinutes.toString() ?? '30');
    _sortOrderController = TextEditingController(
        text: widget.service?.sortOrder.toString() ?? '0');
    _isActive = widget.service?.isActive ?? true;
  }

  @override
  void dispose() {
    _nameController.dispose();
    _descriptionController.dispose();
    _priceController.dispose();
    _durationController.dispose();
    _sortOrderController.dispose();
    super.dispose();
  }

  Future<void> _saveService() async {
    if (_formKey.currentState!.validate()) {
      final serviceProvider =
          Provider.of<ServiceProvider>(context, listen: false);
      final service = HaircutService(
        id: widget.service?.id ?? 0,
        name: _nameController.text,
        description: _descriptionController.text,
        basePrice: double.parse(_priceController.text),
        durationMinutes: int.parse(_durationController.text),
        imageUrl: widget.service?.imageUrl ?? '',
        isActive: _isActive,
        sortOrder: int.parse(_sortOrderController.text),
      );

      try {
        if (widget.service == null) {
          await serviceProvider.createService(service);
        } else {
          await serviceProvider.updateService(service);
        }
        if (mounted) {
          Navigator.pop(context);
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: Text(widget.service == null
                  ? 'Đã tạo dịch vụ thành công'
                  : 'Đã cập nhật dịch vụ thành công'),
              backgroundColor: Colors.green,
            ),
          );
        }
      } catch (e) {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: Text('Lỗi: ${e.toString()}'),
              backgroundColor: Colors.red,
            ),
          );
        }
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(widget.service == null ? 'Thêm Dịch Vụ' : 'Sửa Dịch Vụ'),
        actions: [
          IconButton(
            icon: const Icon(Icons.save),
            onPressed: _saveService,
          ),
        ],
      ),
      body: Form(
        key: _formKey,
        child: ListView(
          padding: const EdgeInsets.all(16),
          children: [
            TextFormField(
              controller: _nameController,
              decoration: const InputDecoration(
                labelText: 'Tên dịch vụ',
                border: OutlineInputBorder(),
              ),
              validator: (value) {
                if (value == null || value.isEmpty) {
                  return 'Vui lòng nhập tên dịch vụ';
                }
                return null;
              },
            ),
            const SizedBox(height: 16),
            TextFormField(
              controller: _descriptionController,
              decoration: const InputDecoration(
                labelText: 'Mô tả',
                border: OutlineInputBorder(),
              ),
              maxLines: 3,
            ),
            const SizedBox(height: 16),
            TextFormField(
              controller: _priceController,
              decoration: const InputDecoration(
                labelText: 'Giá (đ)',
                border: OutlineInputBorder(),
              ),
              keyboardType: TextInputType.number,
              validator: (value) {
                if (value == null || value.isEmpty) {
                  return 'Vui lòng nhập giá';
                }
                if (double.tryParse(value) == null) {
                  return 'Vui lòng nhập số hợp lệ';
                }
                return null;
              },
            ),
            const SizedBox(height: 16),
            TextFormField(
              controller: _durationController,
              decoration: const InputDecoration(
                labelText: 'Thời gian (phút)',
                border: OutlineInputBorder(),
              ),
              keyboardType: TextInputType.number,
              validator: (value) {
                if (value == null || value.isEmpty) {
                  return 'Vui lòng nhập thời gian';
                }
                if (int.tryParse(value) == null) {
                  return 'Vui lòng nhập số hợp lệ';
                }
                return null;
              },
            ),
            const SizedBox(height: 16),
            TextFormField(
              controller: _sortOrderController,
              decoration: const InputDecoration(
                labelText: 'Thứ tự sắp xếp',
                border: OutlineInputBorder(),
              ),
              keyboardType: TextInputType.number,
              validator: (value) {
                if (value == null || value.isEmpty) {
                  return 'Vui lòng nhập thứ tự sắp xếp';
                }
                if (int.tryParse(value) == null) {
                  return 'Vui lòng nhập số hợp lệ';
                }
                return null;
              },
            ),
            const SizedBox(height: 16),
            SwitchListTile(
              title: const Text('Đang hoạt động'),
              value: _isActive,
              onChanged: (value) {
                setState(() {
                  _isActive = value;
                });
              },
            ),
          ],
        ),
      ),
    );
  }
}
