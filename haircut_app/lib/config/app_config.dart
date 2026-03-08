class AppConfig {
  static const String apiBaseUrl =
      'https://ab8b-2402-9d80-3db-8d1f-b405-18b9-5099-bd92.ngrok-free.app';

  // API endpoints
  static const String loginEndpoint = '/auth/login';
  static const String registerEndpoint = '/auth/register';
  static const String validateTokenEndpoint = '/auth/refresh';
  static const String profileEndpoint = '/users';
  static const String changePasswordEndpoint = '/auth/password';

  static const String barbersEndpoint = '/api/barbers';
  static const String servicesEndpoint = '/services';
  static const String appointmentsEndpoint = '/appointments';
  static const String usersEndpoint = '/users';
}
