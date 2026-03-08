import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:intl/intl.dart';
import 'package:table_calendar/table_calendar.dart';
import 'package:haircut_app/models/appointment.dart';
import 'package:haircut_app/models/barber.dart';
import 'package:haircut_app/models/haircut_service.dart';
import 'package:haircut_app/providers/appointment_provider.dart';
import 'package:haircut_app/providers/barber_provider.dart';
import 'package:haircut_app/providers/service_provider.dart';
import 'package:haircut_app/providers/auth_provider.dart';

class AppointmentManagementScreen extends StatefulWidget {
  const AppointmentManagementScreen({super.key});

  @override
  State<AppointmentManagementScreen> createState() =>
      _AppointmentManagementScreenState();
}

class _AppointmentManagementScreenState
    extends State<AppointmentManagementScreen> {
  DateTime _selectedDay = DateTime.now();
  DateTime _focusedDay = DateTime.now();
  bool _isInit = false;
  CalendarFormat _calendarFormat = CalendarFormat.month;

  @override
  void initState() {
    super.initState();

    // Di chuyển các cuộc gọi fetch vào initState
    Future.microtask(() {
      final barberProvider =
          Provider.of<BarberProvider>(context, listen: false);
      final serviceProvider =
          Provider.of<ServiceProvider>(context, listen: false);
      final appointmentProvider =
          Provider.of<AppointmentProvider>(context, listen: false);

      barberProvider.fetchBarbers();
      serviceProvider.fetchServices();
      appointmentProvider.fetchAppointmentsByDate(_selectedDay);
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
    final appointmentProvider = Provider.of<AppointmentProvider>(context);
    final authProvider = Provider.of<AuthProvider>(context);
    final isAdmin = authProvider.isAdmin;

    return Scaffold(
      appBar: AppBar(
        title: const Text('Quản lý Lịch hẹn'),
        actions: [
          IconButton(
            icon: const Icon(Icons.add),
            onPressed: () {
              _showAppointmentDialog(context);
            },
          ),
        ],
      ),
      body: Column(
        children: [
          // Calendar section with fixed height to prevent overflow
          SizedBox(
            height: 340, // Fixed height for calendar
            child: TableCalendar(
              firstDay: DateTime.now().subtract(const Duration(days: 365)),
              lastDay: DateTime.now().add(const Duration(days: 365)),
              focusedDay: _focusedDay,
              selectedDayPredicate: (day) {
                return isSameDay(_selectedDay, day);
              },
              onDaySelected: (selectedDay, focusedDay) {
                setState(() {
                  _selectedDay = selectedDay;
                  _focusedDay = focusedDay;
                });
                appointmentProvider.fetchAppointmentsByDate(selectedDay);
              },
              calendarFormat: _calendarFormat,
              onFormatChanged: (format) {
                setState(() {
                  _calendarFormat = format;
                });
              },
              calendarStyle: CalendarStyle(
                markersMaxCount: 3,
                markerDecoration: const BoxDecoration(
                  color: Colors.redAccent,
                  shape: BoxShape.circle,
                ),
              ),
              headerStyle: const HeaderStyle(
                formatButtonVisible: true,
                titleCentered: true,
              ),
            ),
          ),

          // Appointments list section
          Expanded(
            child: Container(
              padding: const EdgeInsets.only(
                  bottom: 20), // Added bottom padding to prevent overflow
              child: appointmentProvider.isLoading
                  ? const Center(child: CircularProgressIndicator())
                  : appointmentProvider.error != null
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
                                'Lỗi: ${appointmentProvider.error}',
                                style: const TextStyle(
                                  fontSize: 16,
                                  fontWeight: FontWeight.bold,
                                ),
                                textAlign: TextAlign.center,
                              ),
                              const SizedBox(height: 16),
                              ElevatedButton(
                                onPressed: () {
                                  appointmentProvider
                                      .fetchAppointmentsByDate(_selectedDay);
                                },
                                child: const Text('Thử lại'),
                              ),
                            ],
                          ),
                        )
                      : appointmentProvider.appointments.isEmpty
                          ? Center(
                              child: Text(
                                'Không có lịch hẹn cho ngày ${DateFormat.yMd().format(_selectedDay)}',
                                style: const TextStyle(fontSize: 16),
                              ),
                            )
                          : ListView.builder(
                              itemCount:
                                  appointmentProvider.appointments.length,
                              padding: const EdgeInsets.symmetric(
                                horizontal: 16,
                                vertical: 8,
                              ),
                              itemBuilder: (ctx, index) {
                                final appointment =
                                    appointmentProvider.appointments[index];
                                return _buildAppointmentItem(
                                    context, appointment);
                              },
                            ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildAppointmentItem(BuildContext context, Appointment appointment) {
    final statusColor = _getStatusColor(appointment.status);

    // Chuyển đổi trạng thái sang tiếng Việt
    String getStatusText(AppointmentStatus status) {
      switch (status) {
        case AppointmentStatus.BOOKED:
          return 'Đã đặt';
        case AppointmentStatus.CONFIRMED:
          return 'Đã xác nhận';
        case AppointmentStatus.CANCELLED:
          return 'Đã hủy';
        case AppointmentStatus.COMPLETED:
          return 'Hoàn thành';
        default:
          return status.toString().split('.').last;
      }
    }

    return Card(
      margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(12),
        side: BorderSide(
          color: statusColor.withOpacity(0.5),
          width: 1,
        ),
      ),
      child: ExpansionTile(
        leading: CircleAvatar(
          backgroundColor: statusColor.withOpacity(0.2),
          foregroundColor: statusColor,
          child: const Icon(Icons.event),
        ),
        title: Text(
          appointment.customerName.isNotEmpty
              ? appointment.customerName
              : 'Khách hàng',
          style: const TextStyle(fontWeight: FontWeight.bold),
          overflow: TextOverflow.ellipsis,
        ),
        subtitle: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const SizedBox(height: 4),
            Text(
              'Thời gian: ${appointment.startTime} - ${appointment.endTime}',
              overflow: TextOverflow.ellipsis,
            ),
            Text(
              'Trạng thái: ${getStatusText(appointment.status)}',
              style: TextStyle(
                color: statusColor,
                fontWeight: FontWeight.w600,
              ),
              overflow: TextOverflow.ellipsis,
            ),
          ],
        ),
        children: [
          Padding(
            padding: const EdgeInsets.all(16.0),
            child: SingleChildScrollView(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  if (appointment.barber != null)
                    _buildInfoRow('Barber:', appointment.barber!.name),
                  _buildInfoRow('Điện thoại:', appointment.customerPhone),
                  if (appointment.customerEmail.isNotEmpty)
                    _buildInfoRow('Email:', appointment.customerEmail),

                  // Phần dịch vụ
                  const SizedBox(height: 8),
                  const Text(
                    'Dịch vụ:',
                    style: TextStyle(fontWeight: FontWeight.bold),
                  ),
                  const SizedBox(height: 4),
                  if (appointment.services != null &&
                      appointment.services!.isNotEmpty)
                    Container(
                      constraints: BoxConstraints(maxHeight: 120),
                      child: SingleChildScrollView(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: appointment.services!
                              .map((service) => Padding(
                                    padding: const EdgeInsets.only(
                                        left: 16.0, bottom: 2.0),
                                    child: Text(
                                      '• ${service.name} (${service.basePrice.toStringAsFixed(2)}đ)',
                                    ),
                                  ))
                              .toList(),
                        ),
                      ),
                    )
                  else if (appointment.serviceIds.isNotEmpty)
                    Padding(
                      padding: const EdgeInsets.only(left: 16.0),
                      child: Text(
                          '${appointment.serviceIds.length} dịch vụ đã chọn'),
                    )
                  else
                    Padding(
                      padding: const EdgeInsets.only(left: 16.0),
                      child: Text('Không có thông tin dịch vụ',
                          style: TextStyle(
                              color: Colors.grey[600],
                              fontStyle: FontStyle.italic)),
                    ),

                  const SizedBox(height: 8),
                  _buildInfoRow('Tổng tiền:',
                      '${appointment.totalPrice.toStringAsFixed(2)}đ'),
                  if (appointment.notes?.isNotEmpty ?? false) ...[
                    const SizedBox(height: 8),
                    const Text(
                      'Ghi chú:',
                      style: TextStyle(fontWeight: FontWeight.bold),
                    ),
                    const SizedBox(height: 4),
                    Container(
                      width: double.infinity,
                      padding: const EdgeInsets.all(8),
                      decoration: BoxDecoration(
                        color: Colors.grey[100],
                        borderRadius: BorderRadius.circular(8),
                      ),
                      child: Text(appointment.notes!),
                    ),
                  ],
                  const SizedBox(height: 16),
                  SingleChildScrollView(
                    scrollDirection: Axis.horizontal,
                    child: Row(
                      mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                      children: [
                        if (appointment.status == AppointmentStatus.BOOKED)
                          _buildActionButton(
                            context,
                            'Xác nhận',
                            Icons.check_circle,
                            Colors.green,
                            () => _changeAppointmentStatus(
                              context,
                              appointment,
                              AppointmentStatus.CONFIRMED,
                            ),
                          ),
                        if (appointment.status == AppointmentStatus.BOOKED ||
                            appointment.status == AppointmentStatus.CONFIRMED)
                          _buildActionButton(
                            context,
                            'Hủy',
                            Icons.cancel,
                            Colors.red,
                            () => _changeAppointmentStatus(
                              context,
                              appointment,
                              AppointmentStatus.CANCELLED,
                            ),
                          ),
                        if (appointment.status == AppointmentStatus.CONFIRMED)
                          _buildActionButton(
                            context,
                            'Hoàn thành',
                            Icons.done_all,
                            Colors.blue,
                            () => _changeAppointmentStatus(
                              context,
                              appointment,
                              AppointmentStatus.COMPLETED,
                            ),
                          ),
                        _buildActionButton(
                          context,
                          'Sửa',
                          Icons.edit,
                          Colors.orange,
                          () => _showAppointmentDialog(context, appointment),
                        ),
                        _buildActionButton(
                          context,
                          'Xóa',
                          Icons.delete,
                          Colors.red[700]!,
                          () => _confirmDelete(context, appointment),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildInfoRow(String label, String value) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4.0),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 80,
            child: Text(
              label,
              style: const TextStyle(fontWeight: FontWeight.bold),
            ),
          ),
          Expanded(
            child: Text(
              value.isEmpty ? 'Không có' : value,
              style: value.isEmpty
                  ? TextStyle(
                      color: Colors.grey[500], fontStyle: FontStyle.italic)
                  : null,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildActionButton(
    BuildContext context,
    String label,
    IconData icon,
    Color color,
    VoidCallback onPressed,
  ) {
    return InkWell(
      onTap: onPressed,
      borderRadius: BorderRadius.circular(8),
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 8.0, vertical: 4.0),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(icon, color: color),
            const SizedBox(height: 4),
            Text(
              label,
              style: TextStyle(color: color, fontSize: 12),
            ),
          ],
        ),
      ),
    );
  }

  Color _getStatusColor(AppointmentStatus status) {
    switch (status) {
      case AppointmentStatus.BOOKED:
        return Colors.orange;
      case AppointmentStatus.CONFIRMED:
        return Colors.green;
      case AppointmentStatus.CANCELLED:
        return Colors.red;
      case AppointmentStatus.COMPLETED:
        return Colors.blue;
      default:
        return Colors.grey;
    }
  }

  void _changeAppointmentStatus(
    BuildContext context,
    Appointment appointment,
    AppointmentStatus newStatus,
  ) {
    // If cancelling, ask for a reason
    if (newStatus == AppointmentStatus.CANCELLED) {
      final TextEditingController reasonController = TextEditingController();
      showDialog(
        context: context,
        builder: (ctx) => AlertDialog(
          title: const Text('Lý do hủy lịch hẹn'),
          content: TextField(
            controller: reasonController,
            decoration: const InputDecoration(
              hintText: 'Nhập lý do hủy lịch hẹn',
              border: OutlineInputBorder(),
            ),
            maxLines: 3,
          ),
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
                _processStatusChange(
                    context, appointment, newStatus, reasonController.text);
              },
              child: const Text('Xác nhận'),
            ),
          ],
        ),
      );
    } else {
      _processStatusChange(context, appointment, newStatus, null);
    }
  }

  void _processStatusChange(
    BuildContext context,
    Appointment appointment,
    AppointmentStatus newStatus,
    String? reason,
  ) {
    // Show loading dialog
    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (BuildContext context) {
        return const AlertDialog(
          content: Row(
            children: [
              CircularProgressIndicator(),
              SizedBox(width: 20),
              Text("Đang cập nhật trạng thái..."),
            ],
          ),
        );
      },
    );

    // Call the API to change status
    Provider.of<AppointmentProvider>(context, listen: false)
        .changeAppointmentStatus(appointment.id, newStatus, reason: reason)
        .then((updatedAppointment) {
      // Close loading dialog
      Navigator.of(context).pop();

      if (updatedAppointment != null) {
        // Show success message
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Đã cập nhật trạng thái thành công'),
            backgroundColor: Colors.green,
            duration: const Duration(seconds: 2),
          ),
        );

        // Refresh appointments for the current date
        Provider.of<AppointmentProvider>(context, listen: false)
            .fetchAppointmentsByDate(_selectedDay);
      } else {
        // Show error message
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Không thể cập nhật trạng thái'),
            backgroundColor: Colors.red,
            duration: const Duration(seconds: 2),
          ),
        );
      }
    });
  }

  void _confirmDelete(BuildContext context, Appointment appointment) {
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Xác nhận xóa'),
        content: Text(
            'Bạn có chắc chắn muốn xóa lịch hẹn của ${appointment.customerName}?'),
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
              Provider.of<AppointmentProvider>(context, listen: false)
                  .deleteAppointment(appointment.id);
            },
            style: TextButton.styleFrom(foregroundColor: Colors.red),
            child: const Text('Xóa'),
          ),
        ],
      ),
    );
  }

  void _showAppointmentDialog(BuildContext context,
      [Appointment? appointment]) {
    final formKey = GlobalKey<FormState>();
    final barberProvider = Provider.of<BarberProvider>(context, listen: false);
    final serviceProvider =
        Provider.of<ServiceProvider>(context, listen: false);

    final nameController =
        TextEditingController(text: appointment?.customerName ?? '');
    final phoneController =
        TextEditingController(text: appointment?.customerPhone ?? '');
    final emailController =
        TextEditingController(text: appointment?.customerEmail ?? '');
    final notesController =
        TextEditingController(text: appointment?.notes ?? '');

    // Initialize with the appointment date or the currently selected day
    DateTime date = appointment?.date ?? _selectedDay;
    String startTime = appointment?.startTime ?? '10:00';
    int selectedBarberId = appointment?.barberId ?? 0;
    List<int> selectedServiceIds =
        List<int>.from(appointment?.serviceIds ?? []);

    // For new appointments, default to BOOKED, otherwise use the current status
    AppointmentStatus status = appointment?.status ?? AppointmentStatus.BOOKED;

    // Ánh xạ trạng thái lịch hẹn sang tiếng Việt
    String getStatusDisplayName(AppointmentStatus status) {
      switch (status) {
        case AppointmentStatus.BOOKED:
          return 'Đã đặt';
        case AppointmentStatus.CONFIRMED:
          return 'Đã xác nhận';
        case AppointmentStatus.CANCELLED:
          return 'Đã hủy';
        case AppointmentStatus.COMPLETED:
          return 'Hoàn thành';
        default:
          return status.toString().split('.').last;
      }
    }

    // Create a StatefulBuilder to handle state changes inside the dialog
    showDialog(
      context: context,
      builder: (BuildContext context) {
        return StatefulBuilder(
          builder: (context, setState) {
            return Dialog(
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(16),
              ),
              child: Container(
                width: double.infinity,
                constraints: BoxConstraints(
                  maxHeight: MediaQuery.of(context).size.height * 0.85,
                  maxWidth: 500,
                ),
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    // Dialog header
                    Container(
                      padding: const EdgeInsets.all(16),
                      decoration: BoxDecoration(
                        color: Theme.of(context).primaryColor,
                        borderRadius: const BorderRadius.only(
                          topLeft: Radius.circular(16),
                          topRight: Radius.circular(16),
                        ),
                      ),
                      child: Row(
                        children: [
                          Expanded(
                            child: Text(
                              appointment == null
                                  ? 'Thêm lịch hẹn'
                                  : 'Sửa lịch hẹn',
                              style: const TextStyle(
                                fontSize: 18,
                                fontWeight: FontWeight.bold,
                                color: Colors.white,
                              ),
                            ),
                          ),
                          IconButton(
                            icon: const Icon(Icons.close, color: Colors.white),
                            onPressed: () {
                              Navigator.of(context).pop();
                            },
                          ),
                        ],
                      ),
                    ),

                    // Dialog content - scrollable
                    Expanded(
                      child: SingleChildScrollView(
                        padding: const EdgeInsets.all(16),
                        child: Form(
                          key: formKey,
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              // Customer information section
                              const Text(
                                'Thông tin khách hàng',
                                style: TextStyle(
                                  fontSize: 16,
                                  fontWeight: FontWeight.bold,
                                ),
                              ),
                              const SizedBox(height: 8),
                              TextFormField(
                                controller: nameController,
                                decoration: const InputDecoration(
                                  labelText: 'Tên khách hàng',
                                  border: OutlineInputBorder(),
                                ),
                                validator: (value) {
                                  if (value == null || value.isEmpty) {
                                    return 'Vui lòng nhập tên';
                                  }
                                  return null;
                                },
                              ),
                              const SizedBox(height: 12),
                              TextFormField(
                                controller: phoneController,
                                decoration: const InputDecoration(
                                  labelText: 'Số điện thoại',
                                  border: OutlineInputBorder(),
                                ),
                                validator: (value) {
                                  if (value == null || value.isEmpty) {
                                    return 'Vui lòng nhập số điện thoại';
                                  }
                                  return null;
                                },
                              ),
                              const SizedBox(height: 12),
                              TextFormField(
                                controller: emailController,
                                decoration: const InputDecoration(
                                  labelText: 'Email (Không bắt buộc)',
                                  border: OutlineInputBorder(),
                                ),
                              ),

                              const SizedBox(height: 24),

                              // Appointment time section
                              const Text(
                                'Thời gian lịch hẹn',
                                style: TextStyle(
                                  fontSize: 16,
                                  fontWeight: FontWeight.bold,
                                ),
                              ),
                              const SizedBox(height: 8),
                              Card(
                                elevation: 0,
                                color: Colors.grey.shade100,
                                child: Padding(
                                  padding: const EdgeInsets.all(8.0),
                                  child: Column(
                                    children: [
                                      ListTile(
                                        leading:
                                            const Icon(Icons.calendar_today),
                                        title: const Text('Ngày'),
                                        subtitle: Text(
                                          DateFormat(
                                                  'EEEE, dd/MM/yyyy', 'vi_VN')
                                              .format(date),
                                          style: const TextStyle(
                                              fontWeight: FontWeight.bold),
                                        ),
                                        onTap: () async {
                                          final pickedDate =
                                              await showDatePicker(
                                            context: context,
                                            initialDate: date,
                                            firstDate: DateTime.now(),
                                            lastDate: DateTime.now()
                                                .add(const Duration(days: 365)),
                                          );
                                          if (pickedDate != null) {
                                            setState(() {
                                              date = pickedDate;
                                            });
                                          }
                                        },
                                      ),
                                      const Divider(),
                                      ListTile(
                                        leading: const Icon(Icons.access_time),
                                        title: const Text('Thời gian'),
                                        subtitle: Text(
                                          startTime,
                                          style: const TextStyle(
                                              fontWeight: FontWeight.bold),
                                        ),
                                        onTap: () async {
                                          final TimeOfDay initialTime =
                                              TimeOfDay(
                                            hour: int.parse(
                                                startTime.split(':')[0]),
                                            minute: int.parse(
                                                startTime.split(':')[1]),
                                          );
                                          final pickedTime =
                                              await showTimePicker(
                                            context: context,
                                            initialTime: initialTime,
                                          );
                                          if (pickedTime != null) {
                                            setState(() {
                                              startTime =
                                                  '${pickedTime.hour.toString().padLeft(2, '0')}:${pickedTime.minute.toString().padLeft(2, '0')}';
                                            });
                                          }
                                        },
                                      ),
                                    ],
                                  ),
                                ),
                              ),

                              const SizedBox(height: 24),

                              // Barber selection
                              const Text(
                                'Chọn Barber',
                                style: TextStyle(
                                  fontSize: 16,
                                  fontWeight: FontWeight.bold,
                                ),
                              ),
                              const SizedBox(height: 8),
                              DropdownButtonFormField<int>(
                                decoration: const InputDecoration(
                                  labelText: 'Barber',
                                  border: OutlineInputBorder(),
                                ),
                                value: selectedBarberId != 0
                                    ? selectedBarberId
                                    : null,
                                hint: const Text('Chọn barber'),
                                validator: (value) {
                                  if (value == null) {
                                    return 'Vui lòng chọn barber';
                                  }
                                  return null;
                                },
                                items: barberProvider.barbers
                                    .where((barber) =>
                                        barber.isActive &&
                                        barber.isAvailableForBooking)
                                    .map((barber) => DropdownMenuItem<int>(
                                          value: barber.id,
                                          child: Text(barber.name),
                                        ))
                                    .toList(),
                                onChanged: (value) {
                                  setState(() {
                                    selectedBarberId = value!;
                                  });
                                },
                              ),

                              const SizedBox(height: 24),

                              // Services selection
                              const Text(
                                'Chọn dịch vụ',
                                style: TextStyle(
                                  fontSize: 16,
                                  fontWeight: FontWeight.bold,
                                ),
                              ),
                              const SizedBox(height: 8),

                              // Service selection list in a card
                              Card(
                                elevation: 0,
                                color: Colors.grey.shade100,
                                child: Padding(
                                  padding: const EdgeInsets.all(8.0),
                                  child: Column(
                                    children: [
                                      ...serviceProvider.services
                                          .where((service) => service.isActive)
                                          .map((service) => CheckboxListTile(
                                                title: Text(service.name),
                                                subtitle: Text(
                                                    '${service.basePrice.toStringAsFixed(2)}đ | ${service.durationMinutes} phút'),
                                                value: selectedServiceIds
                                                    .contains(service.id),
                                                onChanged: (bool? value) {
                                                  setState(() {
                                                    if (value == true) {
                                                      selectedServiceIds
                                                          .add(service.id);
                                                    } else {
                                                      selectedServiceIds
                                                          .remove(service.id);
                                                    }
                                                  });
                                                },
                                              )),
                                    ],
                                  ),
                                ),
                              ),

                              if (selectedServiceIds.isEmpty)
                                Padding(
                                  padding: const EdgeInsets.all(8.0),
                                  child: Text(
                                    'Vui lòng chọn ít nhất một dịch vụ',
                                    style: TextStyle(
                                        color: Colors.red.shade700,
                                        fontSize: 14),
                                  ),
                                ),

                              const SizedBox(height: 24),

                              // Notes
                              const Text(
                                'Ghi chú',
                                style: TextStyle(
                                  fontSize: 16,
                                  fontWeight: FontWeight.bold,
                                ),
                              ),
                              const SizedBox(height: 8),
                              TextFormField(
                                controller: notesController,
                                decoration: const InputDecoration(
                                  labelText: 'Ghi chú (Không bắt buộc)',
                                  border: OutlineInputBorder(),
                                  alignLabelWithHint: true,
                                ),
                                maxLines: 3,
                              ),

                              // Status selection for existing appointments
                              if (appointment != null) ...[
                                const SizedBox(height: 24),
                                const Text(
                                  'Trạng thái',
                                  style: TextStyle(
                                    fontSize: 16,
                                    fontWeight: FontWeight.bold,
                                  ),
                                ),
                                const SizedBox(height: 8),
                                DropdownButtonFormField<AppointmentStatus>(
                                  decoration: const InputDecoration(
                                    labelText: 'Trạng thái',
                                    border: OutlineInputBorder(),
                                  ),
                                  value: status,
                                  items: AppointmentStatus.values
                                      .map((status) =>
                                          DropdownMenuItem<AppointmentStatus>(
                                            value: status,
                                            child: Text(
                                                getStatusDisplayName(status)),
                                          ))
                                      .toList(),
                                  onChanged: (value) {
                                    setState(() {
                                      status = value!;
                                    });
                                  },
                                ),
                              ],

                              const SizedBox(height: 16),
                            ],
                          ),
                        ),
                      ),
                    ),

                    // Dialog actions
                    Container(
                      padding: const EdgeInsets.all(16),
                      decoration: BoxDecoration(
                        color: Colors.grey.shade100,
                        borderRadius: const BorderRadius.only(
                          bottomLeft: Radius.circular(16),
                          bottomRight: Radius.circular(16),
                        ),
                      ),
                      child: Row(
                        mainAxisAlignment: MainAxisAlignment.end,
                        children: [
                          TextButton(
                            onPressed: () {
                              Navigator.of(context).pop();
                            },
                            child: const Text('Hủy'),
                          ),
                          const SizedBox(width: 16),
                          ElevatedButton(
                            style: ElevatedButton.styleFrom(
                              backgroundColor: Theme.of(context).primaryColor,
                              foregroundColor: Colors.white,
                              padding: const EdgeInsets.symmetric(
                                  horizontal: 24, vertical: 12),
                            ),
                            onPressed: () {
                              if (formKey.currentState!.validate() &&
                                  selectedServiceIds.isNotEmpty) {
                                final appointmentProvider =
                                    Provider.of<AppointmentProvider>(context,
                                        listen: false);

                                // Calculate the total price of all selected services
                                double totalPrice = 0;
                                for (final serviceId in selectedServiceIds) {
                                  final service =
                                      serviceProvider.services.firstWhere(
                                    (s) => s.id == serviceId,
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
                                  totalPrice += service.basePrice;
                                }

                                // Calculate end time based on the total duration of services
                                int totalDurationMinutes = serviceProvider
                                    .calculateTotalDuration(selectedServiceIds);
                                String endTime = _calculateEndTime(
                                    startTime, totalDurationMinutes);

                                // Create or update appointment
                                final authProvider = Provider.of<AuthProvider>(
                                    context,
                                    listen: false);
                                final userId = authProvider.currentUser?.id;

                                // Check if user is logged in
                                if (userId == null) {
                                  ScaffoldMessenger.of(context).showSnackBar(
                                    const SnackBar(
                                      content:
                                          Text('Bạn cần đăng nhập để đặt lịch'),
                                      backgroundColor: Colors.red,
                                      duration: Duration(seconds: 3),
                                    ),
                                  );
                                  return;
                                }

                                final newAppointment = Appointment(
                                  id: appointment?.id ?? 0,
                                  customerName: nameController.text,
                                  customerPhone: phoneController.text,
                                  customerEmail: emailController.text,
                                  date: date,
                                  startTime: startTime,
                                  endTime: endTime,
                                  status: status,
                                  notes: notesController.text,
                                  totalPrice: totalPrice,
                                  barberId: selectedBarberId,
                                  customerId: userId,
                                  serviceIds: selectedServiceIds,
                                  createdAt:
                                      appointment?.createdAt ?? DateTime.now(),
                                  isReminderSent:
                                      appointment?.isReminderSent ?? false,
                                );

                                if (appointment == null) {
                                  // Show loading indicator
                                  showDialog(
                                    context: context,
                                    barrierDismissible: false,
                                    builder: (BuildContext context) {
                                      return const AlertDialog(
                                        content: Row(
                                          children: [
                                            CircularProgressIndicator(),
                                            SizedBox(width: 20),
                                            Text("Đang tạo lịch hẹn..."),
                                          ],
                                        ),
                                      );
                                    },
                                  );

                                  // Create appointment with error handling
                                  appointmentProvider
                                      .createAppointment(newAppointment)
                                      .then((createdAppointment) {
                                    // Close loading dialog
                                    Navigator.of(context).pop();
                                    // Close appointment form dialog
                                    Navigator.of(context).pop();

                                    if (createdAppointment != null) {
                                      // Refresh to ensure the created appointment is shown
                                      appointmentProvider
                                          .fetchAppointmentsByDate(date);

                                      // Show success message
                                      ScaffoldMessenger.of(context)
                                          .showSnackBar(
                                        const SnackBar(
                                          content: Text(
                                              'Đã tạo lịch hẹn thành công'),
                                          backgroundColor: Colors.green,
                                          duration: Duration(seconds: 2),
                                        ),
                                      );
                                    } else {
                                      // Show error message
                                      ScaffoldMessenger.of(context)
                                          .showSnackBar(
                                        SnackBar(
                                          content: Text(
                                              'Lỗi: ${appointmentProvider.error ?? "Không thể tạo lịch hẹn"}'),
                                          backgroundColor: Colors.red,
                                          duration: const Duration(seconds: 4),
                                        ),
                                      );
                                    }
                                  });
                                } else {
                                  // Show loading indicator for update
                                  showDialog(
                                    context: context,
                                    barrierDismissible: false,
                                    builder: (BuildContext context) {
                                      return const AlertDialog(
                                        content: Row(
                                          children: [
                                            CircularProgressIndicator(),
                                            SizedBox(width: 20),
                                            Text("Đang cập nhật lịch hẹn..."),
                                          ],
                                        ),
                                      );
                                    },
                                  );

                                  // Update appointment with error handling
                                  appointmentProvider
                                      .updateAppointment(newAppointment)
                                      .then((updatedAppointment) {
                                    // Close loading dialog
                                    Navigator.of(context).pop();
                                    // Close appointment form dialog
                                    Navigator.of(context).pop();

                                    if (updatedAppointment != null) {
                                      // Refresh to ensure the updated appointment is shown
                                      appointmentProvider
                                          .fetchAppointmentsByDate(date);

                                      // Show success message
                                      ScaffoldMessenger.of(context)
                                          .showSnackBar(
                                        const SnackBar(
                                          content: Text(
                                              'Đã cập nhật lịch hẹn thành công'),
                                          backgroundColor: Colors.green,
                                          duration: Duration(seconds: 2),
                                        ),
                                      );
                                    } else {
                                      // Show error message
                                      ScaffoldMessenger.of(context)
                                          .showSnackBar(
                                        SnackBar(
                                          content: Text(
                                              'Lỗi: ${appointmentProvider.error ?? "Không thể cập nhật lịch hẹn"}'),
                                          backgroundColor: Colors.red,
                                          duration: const Duration(seconds: 4),
                                        ),
                                      );
                                    }
                                  });
                                }
                              }
                            },
                            child: const Text('Lưu',
                                style: TextStyle(fontSize: 16)),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
            );
          },
        );
      },
    );
  }

  String _calculateEndTime(String startTime, int durationMinutes) {
    final List<String> timeParts = startTime.split(':');
    final int startHour = int.parse(timeParts[0]);
    final int startMinute = int.parse(timeParts[1]);

    final DateTime now = DateTime.now();
    final DateTime startDateTime = DateTime(
      now.year,
      now.month,
      now.day,
      startHour,
      startMinute,
    );

    final DateTime endDateTime =
        startDateTime.add(Duration(minutes: durationMinutes));

    final String endHour = endDateTime.hour.toString().padLeft(2, '0');
    final String endMinute = endDateTime.minute.toString().padLeft(2, '0');

    return '$endHour:$endMinute';
  }
}
