import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:intl/intl.dart';
import 'package:haircut_app/providers/auth_provider.dart';
import 'package:haircut_app/providers/service_provider.dart';
import 'package:haircut_app/providers/barber_provider.dart';
import 'package:haircut_app/providers/appointment_provider.dart';
import 'package:haircut_app/models/appointment.dart';
import 'package:haircut_app/screens/profile_screen.dart';
import 'package:haircut_app/screens/service_management_screen.dart';
import 'package:haircut_app/screens/barber_management_screen.dart';
import 'package:haircut_app/screens/appointment_management_screen.dart';
import 'package:fl_chart/fl_chart.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  _HomeScreenState createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  int _selectedIndex = 0;
  bool _isLoading = true;
  final NumberFormat _currencyFormatter = NumberFormat('#,###', 'vi_VN');
  double _totalRevenue = 0.0;
  Map<String, double> _revenueByService = {};
  Map<String, int> _appointmentCountByStatus = {};
  DateTime _startDate = DateTime.now().subtract(const Duration(days: 30));
  DateTime _endDate = DateTime.now();
  String _selectedPeriod = 'month'; // month, quarter, year, custom

  String formatPrice(double price) {
    return '${_currencyFormatter.format(price)}đ';
  }

  @override
  void initState() {
    super.initState();
    _updateDateRange(); // Tự động cập nhật khoảng thời gian khi khởi tạo
  }

  void _updateDateRange() {
    final now = DateTime.now();
    switch (_selectedPeriod) {
      case 'month':
        _startDate = DateTime(now.year, now.month, 1);
        _endDate = DateTime(now.year, now.month + 1, 0);
        break;
      case 'quarter':
        final quarter = ((now.month - 1) ~/ 3) * 3 + 1;
        _startDate = DateTime(now.year, quarter, 1);
        _endDate = DateTime(now.year, quarter + 3, 0);
        break;
      case 'year':
        _startDate = DateTime(now.year, 1, 1);
        _endDate = DateTime(now.year, 12, 31);
        break;
    }
    _loadData(); // Tự động load dữ liệu sau khi cập nhật khoảng thời gian
  }

  Future<void> _loadData() async {
    if (!mounted) return;

    setState(() {
      _isLoading = true;
    });

    try {
      await Provider.of<ServiceProvider>(context, listen: false)
          .fetchServices();
      await Provider.of<BarberProvider>(context, listen: false).fetchBarbers();

      // Lấy thống kê doanh thu
      final response =
          await Provider.of<AppointmentProvider>(context, listen: false)
              .getRevenueStatistics(_startDate, _endDate);

      if (mounted) {
        setState(() {
          // Xử lý dữ liệu từ API
          final totalRevenue = response['totalRevenue'] as double? ?? 0.0;
          final revenueByService =
              Map<String, double>.from(response['revenueByService'] ?? {});
          final appointmentCountByStatus = Map<String, int>.from(
              (response['appointmentCountByStatus'] as Map<String, dynamic>?)
                      ?.map((key, value) => MapEntry(key, value as int)) ??
                  {});

          // Cập nhật state
          _totalRevenue = totalRevenue;
          _revenueByService = revenueByService;
          _appointmentCountByStatus = appointmentCountByStatus;

          // Debug log
          print('Total Revenue: $_totalRevenue');
          print('Revenue by Service: $_revenueByService');
          print('Appointment Count by Status: $_appointmentCountByStatus');
        });
      }
    } catch (e) {
      print("Error loading data: $e");
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Lỗi khi tải dữ liệu: ${e.toString()}'),
            backgroundColor: Colors.red,
          ),
        );
      }
    } finally {
      if (mounted) {
        setState(() {
          _isLoading = false;
        });
      }
    }
  }

  Future<void> _selectDateRange() async {
    final DateTimeRange? picked = await showDateRangePicker(
      context: context,
      firstDate: DateTime(2020),
      lastDate: DateTime(2030),
      initialDateRange: DateTimeRange(start: _startDate, end: _endDate),
    );
    if (picked != null) {
      setState(() {
        _startDate = picked.start;
        _endDate = picked.end;
        _selectedPeriod = 'custom';
      });
      _loadData();
    }
  }

  @override
  Widget build(BuildContext context) {
    final authProvider = Provider.of<AuthProvider>(context);
    final isAdmin = authProvider.isAdmin;
    final isBarber = authProvider.isBarber;
    final bool showManagement = isAdmin || isBarber;

    final List<Widget> screens = [
      _buildHomeTab(),
      const ServiceManagementScreen(),
      const AppointmentManagementScreen(),
      if (showManagement) const BarberManagementScreen(),
      ProfileScreen(),
    ];

    return Scaffold(
      body: IndexedStack(
        index: _selectedIndex,
        children: screens,
      ),
      bottomNavigationBar: BottomNavigationBar(
        type: BottomNavigationBarType.fixed,
        currentIndex: _selectedIndex,
        onTap: (index) {
          if (!showManagement && index > 2) {
            setState(() {
              _selectedIndex = index - 1;
            });
          } else {
            setState(() {
              _selectedIndex = index;
            });
          }
        },
        selectedItemColor: Theme.of(context).primaryColor,
        unselectedItemColor: Colors.grey,
        items: [
          const BottomNavigationBarItem(
            icon: Icon(Icons.home),
            label: 'Trang chủ',
          ),
          const BottomNavigationBarItem(
            icon: Icon(Icons.cut),
            label: 'Dịch vụ',
          ),
          const BottomNavigationBarItem(
            icon: Icon(Icons.calendar_today),
            label: 'Đặt lịch',
          ),
          if (showManagement)
            const BottomNavigationBarItem(
              icon: Icon(Icons.business),
              label: 'Quản lý',
            ),
          const BottomNavigationBarItem(
            icon: Icon(Icons.person),
            label: 'Tài khoản',
          ),
        ],
      ),
    );
  }

  Widget _buildHomeTab() {
    if (_isLoading) {
      return const Center(
        child: CircularProgressIndicator(),
      );
    }

    // Tính toán tổng số lịch hẹn
    final totalAppointments =
        _appointmentCountByStatus.values.fold(0, (sum, count) => sum + count);
    final confirmedAppointments = _appointmentCountByStatus['CONFIRMED'] ?? 0;
    final cancelledAppointments = _appointmentCountByStatus['CANCELLED'] ?? 0;

    return SingleChildScrollView(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Header với banner
          Container(
            height: 210,
            width: double.infinity,
            decoration: BoxDecoration(
              color: Theme.of(context).primaryColor,
              borderRadius: const BorderRadius.only(
                bottomLeft: Radius.circular(25),
                bottomRight: Radius.circular(25),
              ),
            ),
            padding: const EdgeInsets.all(20),
            child: SafeArea(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  const Text(
                    'Quản lý tiệm tóc',
                    style: TextStyle(
                      color: Colors.white,
                      fontSize: 24,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                  const SizedBox(height: 10),
                  const Text(
                    'Đặt lịch hẹn ngay',
                    style: TextStyle(
                      color: Colors.white,
                      fontSize: 16,
                    ),
                  ),
                  const SizedBox(height: 20),
                  ElevatedButton(
                    onPressed: () {
                      setState(() {
                        _selectedIndex = 2;
                      });
                    },
                    style: ElevatedButton.styleFrom(
                      backgroundColor: Colors.white,
                      foregroundColor: Theme.of(context).primaryColor,
                      padding: const EdgeInsets.symmetric(
                          horizontal: 20, vertical: 10),
                    ),
                    child: const Text('Đặt Lịch Ngay'),
                  ),
                ],
              ),
            ),
          ),

          // Bộ lọc thời gian
          Padding(
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text(
                  'Thống Kê Doanh Thu',
                  style: TextStyle(
                    fontSize: 20,
                    fontWeight: FontWeight.bold,
                  ),
                ),
                const SizedBox(height: 16),
                Row(
                  children: [
                    Expanded(
                      child: DropdownButtonFormField<String>(
                        value: _selectedPeriod,
                        decoration: const InputDecoration(
                          border: OutlineInputBorder(),
                          contentPadding: EdgeInsets.symmetric(horizontal: 12),
                        ),
                        items: const [
                          DropdownMenuItem(
                            value: 'month',
                            child: Text('Tháng này'),
                          ),
                          DropdownMenuItem(
                            value: 'quarter',
                            child: Text('Quý này'),
                          ),
                          DropdownMenuItem(
                            value: 'year',
                            child: Text('Năm này'),
                          ),
                          DropdownMenuItem(
                            value: 'custom',
                            child: Text('Tùy chọn'),
                          ),
                        ],
                        onChanged: (value) {
                          if (value != null) {
                            setState(() {
                              _selectedPeriod = value;
                            });
                            if (value == 'custom') {
                              _selectDateRange();
                            } else {
                              _updateDateRange();
                            }
                          }
                        },
                      ),
                    ),
                    const SizedBox(width: 16),
                    if (_selectedPeriod == 'custom')
                      TextButton.icon(
                        onPressed: _selectDateRange,
                        icon: const Icon(Icons.calendar_today),
                        label: Text(
                          '${DateFormat('dd/MM/yyyy').format(_startDate)} - ${DateFormat('dd/MM/yyyy').format(_endDate)}',
                        ),
                      ),
                  ],
                ),
              ],
            ),
          ),

          // Thẻ thống kê
          Padding(
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    _buildStatCard(
                      context,
                      title: 'Tổng doanh thu',
                      value: formatPrice(_totalRevenue),
                      icon: Icons.monetization_on,
                      color: Colors.purple,
                    ),
                    const SizedBox(width: 16),
                    _buildStatCard(
                      context,
                      title: 'Lịch hẹn',
                      value: totalAppointments.toString(),
                      icon: Icons.calendar_today,
                      color: Colors.blue,
                    ),
                  ],
                ),
                const SizedBox(height: 16),
                Row(
                  children: [
                    _buildStatCard(
                      context,
                      title: 'Đã xác nhận',
                      value: confirmedAppointments.toString(),
                      icon: Icons.check_circle,
                      color: Colors.green,
                    ),
                    const SizedBox(width: 16),
                    _buildStatCard(
                      context,
                      title: 'Đã hủy',
                      value: cancelledAppointments.toString(),
                      icon: Icons.cancel,
                      color: Colors.red,
                    ),
                  ],
                ),
              ],
            ),
          ),

          // Biểu đồ doanh thu theo dịch vụ
          Padding(
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text(
                  'Doanh Thu Theo Dịch Vụ',
                  style: TextStyle(
                    fontSize: 20,
                    fontWeight: FontWeight.bold,
                  ),
                ),
                const SizedBox(height: 16),
                SizedBox(
                  height: 300,
                  child: _revenueByService.isEmpty
                      ? const Center(child: Text('Không có dữ liệu'))
                      : PieChart(
                          PieChartData(
                            sections: _revenueByService.entries.map((entry) {
                              final serviceName = entry.key;
                              final revenue = entry.value;
                              final percentage = (revenue / _totalRevenue * 100)
                                  .toStringAsFixed(1);

                              return PieChartSectionData(
                                value: revenue,
                                title: '$percentage%\n${serviceName}',
                                color: _getColorForService(serviceName),
                                radius: 100,
                                titleStyle: const TextStyle(
                                  fontSize: 12,
                                  fontWeight: FontWeight.bold,
                                  color: Colors.white,
                                ),
                              );
                            }).toList(),
                            sectionsSpace: 2,
                            centerSpaceRadius: 40,
                          ),
                        ),
                ),
                const SizedBox(height: 16),
                // Hiển thị danh sách dịch vụ bên dưới biểu đồ
                ..._revenueByService.entries.map((entry) {
                  final serviceName = entry.key;
                  final revenue = entry.value;
                  final percentage =
                      (revenue / _totalRevenue * 100).toStringAsFixed(1);

                  return Padding(
                    padding: const EdgeInsets.symmetric(vertical: 4),
                    child: Row(
                      children: [
                        Container(
                          width: 16,
                          height: 16,
                          decoration: BoxDecoration(
                            color: _getColorForService(serviceName),
                            shape: BoxShape.circle,
                          ),
                        ),
                        const SizedBox(width: 8),
                        Expanded(
                          child: Text(
                            serviceName,
                            style: const TextStyle(fontSize: 14),
                          ),
                        ),
                        Text(
                          '${formatPrice(revenue)} ($percentage%)',
                          style: const TextStyle(
                            fontSize: 14,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                      ],
                    ),
                  );
                }).toList(),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildStatCard(
    BuildContext context, {
    required String title,
    required String value,
    required IconData icon,
    required Color color,
  }) {
    return Expanded(
      child: Container(
        height: 111,
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: color.withOpacity(0.1),
          borderRadius: BorderRadius.circular(16),
          border: Border.all(color: color.withOpacity(0.3)),
        ),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Icon(icon, color: color),
            const Spacer(),
            Text(
              value,
              style: TextStyle(
                fontWeight: FontWeight.bold,
                fontSize: 20,
                color: color,
              ),
            ),
            const SizedBox(height: 4),
            Text(
              title,
              style: TextStyle(
                color: Colors.grey[700],
                fontSize: 14,
              ),
            ),
          ],
        ),
      ),
    );
  }

  Color _getColorForService(String serviceName) {
    final colors = [
      Colors.blue,
      Colors.green,
      Colors.orange,
      Colors.purple,
      Colors.red,
      Colors.teal,
      Colors.indigo,
      Colors.amber,
    ];
    return colors[serviceName.hashCode % colors.length];
  }
}
