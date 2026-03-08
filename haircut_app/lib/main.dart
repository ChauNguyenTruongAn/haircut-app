import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:intl/intl.dart';
import 'package:intl/date_symbol_data_local.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:haircut_app/screens/home_screen.dart';
import 'package:haircut_app/screens/login_screen.dart';
import 'package:haircut_app/screens/barber_management_screen.dart';
import 'package:haircut_app/screens/service_management_screen.dart';
import 'package:haircut_app/screens/appointment_management_screen.dart';
import 'package:haircut_app/screens/profile_screen.dart';
import 'package:haircut_app/screens/splash_screen.dart';
import 'package:haircut_app/screens/register_screen.dart';
import 'package:haircut_app/providers/auth_provider.dart';
import 'package:haircut_app/providers/barber_provider.dart';
import 'package:haircut_app/providers/service_provider.dart';
import 'package:haircut_app/providers/appointment_provider.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  initializeDateFormatting('vi_VN', null).then((_) {
    runApp(const MyApp());
  });
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MultiProvider(
      providers: [
        ChangeNotifierProvider(create: (ctx) => AuthProvider()),
        ChangeNotifierProvider(create: (ctx) => BarberProvider()),
        ChangeNotifierProvider(create: (ctx) => ServiceProvider()),
        ChangeNotifierProvider(create: (ctx) => AppointmentProvider()),
      ],
      child: Consumer<AuthProvider>(
        builder: (ctx, auth, _) => MaterialApp(
          debugShowCheckedModeBanner: false,
          title: 'Haircut Booking',
          localizationsDelegates: const [
            GlobalMaterialLocalizations.delegate,
            GlobalWidgetsLocalizations.delegate,
            GlobalCupertinoLocalizations.delegate,
          ],
          supportedLocales: const [
            Locale('vi', 'VN'),
            Locale('en', 'US'),
          ],
          locale: const Locale('vi', 'VN'),
          theme: ThemeData(
            primarySwatch: Colors.blue,
            primaryColor: const Color(0xFF2A3990),
            visualDensity: VisualDensity.adaptivePlatformDensity,
            fontFamily: 'Roboto',
          ),
          home: auth.isLoading
              ? const SplashScreen()
              : auth.currentUser != null
                  ? const HomeScreen()
                  : const LoginScreen(),
          routes: {
            '/login': (ctx) => const LoginScreen(),
            '/register': (ctx) => const RegisterScreen(),
            '/home': (ctx) => const HomeScreen(),
            '/profile': (ctx) => const ProfileScreen(),
          },
        ),
      ),
    );
  }
}
