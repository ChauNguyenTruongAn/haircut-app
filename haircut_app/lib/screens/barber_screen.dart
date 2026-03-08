import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:haircut_app/providers/barber_provider.dart';

class BarberScreen extends StatefulWidget {
  const BarberScreen({super.key});

  @override
  _BarberScreenState createState() => _BarberScreenState();
}

class _BarberScreenState extends State<BarberScreen> {
  @override
  void initState() {
    super.initState();
    Future.microtask(() =>
        Provider.of<BarberProvider>(context, listen: false).fetchBarbers());
  }

  @override
  Widget build(BuildContext context) {
    final barberProvider = Provider.of<BarberProvider>(context);

    final barbers = barberProvider.barbers;
    final isLoading = barberProvider.isLoading;

    return Scaffold(
        // ... UI code ...
        );
  }

  @override
  void dispose() {
    super.dispose();
  }
}
