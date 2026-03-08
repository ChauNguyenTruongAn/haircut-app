import 'package:flutter/material.dart';
import 'package:flutter_local_notifications/flutter_local_notifications.dart';
import 'package:timezone/timezone.dart' as tz;
import 'package:timezone/data/latest.dart' as tz_data;
import 'package:haircut_app/models/appointment.dart';

class NotificationService {
  static final NotificationService _instance = NotificationService._internal();
  static NotificationService get instance => _instance;

  final FlutterLocalNotificationsPlugin flutterLocalNotificationsPlugin =
      FlutterLocalNotificationsPlugin();

  NotificationService._internal();

  Future<void> init() async {
    // Initialize timezone data
    tz_data.initializeTimeZones();

    // Initialize Android settings
    const AndroidInitializationSettings initializationSettingsAndroid =
        AndroidInitializationSettings('@mipmap/ic_launcher');

    // Initialize iOS settings
    final DarwinInitializationSettings initializationSettingsIOS =
        DarwinInitializationSettings(
      requestAlertPermission: true,
      requestBadgePermission: true,
      requestSoundPermission: true,
    );

    // Initialize settings
    final InitializationSettings initializationSettings =
        InitializationSettings(
      android: initializationSettingsAndroid,
      iOS: initializationSettingsIOS,
    );

    // Initialize plugin
    await flutterLocalNotificationsPlugin.initialize(
      initializationSettings,
      onDidReceiveNotificationResponse: _onNotificationTapped,
    );

    // Request permission for iOS
    final plugin =
        flutterLocalNotificationsPlugin.resolvePlatformSpecificImplementation<
            IOSFlutterLocalNotificationsPlugin>();
    if (plugin != null) {
      await plugin.requestPermissions(
        alert: true,
        badge: true,
        sound: true,
      );
    }
  }

  void _onNotificationTapped(NotificationResponse response) {
    // Handle notification tap
    debugPrint('Notification tapped: ${response.payload}');
    // Navigate to appointment details or other screens based on payload
  }

  Future<void> showNotification({
    required int id,
    required String title,
    required String body,
    String? payload,
  }) async {
    const AndroidNotificationDetails androidPlatformChannelSpecifics =
        AndroidNotificationDetails(
      'haircut_channel',
      'Haircut Appointments',
      channelDescription: 'Notifications for haircut appointments',
      importance: Importance.max,
      priority: Priority.high,
      showWhen: true,
    );

    const DarwinNotificationDetails iOSPlatformChannelSpecifics =
        DarwinNotificationDetails(
      presentAlert: true,
      presentBadge: true,
      presentSound: true,
    );

    const NotificationDetails platformChannelSpecifics = NotificationDetails(
      android: androidPlatformChannelSpecifics,
      iOS: iOSPlatformChannelSpecifics,
    );

    await flutterLocalNotificationsPlugin.show(
      id,
      title,
      body,
      platformChannelSpecifics,
      payload: payload,
    );
  }

  Future<void> scheduleAppointmentReminder(Appointment appointment) async {
    // Parse appointment time into hours and minutes
    final List<String> timeParts = appointment.startTime.split(':');
    final int hour = int.parse(timeParts[0]);
    final int minute = int.parse(timeParts[1]);

    // Create a DateTime for the appointment
    final DateTime appointmentDateTime = DateTime(
      appointment.date.year,
      appointment.date.month,
      appointment.date.day,
      hour,
      minute,
    );

    // Schedule a reminder 2 hours before appointment
    final DateTime reminderTime = appointmentDateTime.subtract(
      const Duration(hours: 2),
    );

    // Only schedule if reminder time is in the future
    if (reminderTime.isAfter(DateTime.now())) {
      final tz.TZDateTime scheduledDate = tz.TZDateTime.from(
        reminderTime,
        tz.local,
      );

      // Get services formatted as a string
      final String servicesText = appointment.services?.isNotEmpty == true
          ? appointment.services?.map((s) => s.name).join(', ') ??
              'Not specified'
          : 'Not specified';

      await flutterLocalNotificationsPlugin.zonedSchedule(
        appointment.id.hashCode ?? 0,
        'Upcoming Appointment Reminder',
        'You have an appointment for $servicesText at ${appointment.startTime}',
        scheduledDate,
        const NotificationDetails(
          android: AndroidNotificationDetails(
            'haircut_reminder_channel',
            'Appointment Reminders',
            channelDescription: 'Reminders for upcoming appointments',
            importance: Importance.max,
            priority: Priority.high,
          ),
          iOS: DarwinNotificationDetails(),
        ),
        androidScheduleMode: AndroidScheduleMode.exactAllowWhileIdle,
        matchDateTimeComponents: DateTimeComponents.time,
        payload: appointment.id.toString(),
      );
    }
  }

  Future<void> showAppointmentStatusNotification(
      Appointment appointment) async {
    String title;
    String body;

    switch (appointment.status) {
      case AppointmentStatus.CONFIRMED:
        title = 'Appointment Confirmed';
        body =
            'Your appointment on ${_formatDate(appointment.date)} at ${appointment.startTime} has been confirmed.';
        break;
      case AppointmentStatus.CANCELLED:
        title = 'Appointment Cancelled';
        body =
            'Your appointment on ${_formatDate(appointment.date)} at ${appointment.startTime} has been cancelled.';
        break;
      case AppointmentStatus.COMPLETED:
        title = 'Appointment Completed';
        body =
            'Thank you for your visit! Your appointment on ${_formatDate(appointment.date)} at ${appointment.startTime} has been marked as completed.';
        break;
      default:
        title = 'Appointment Update';
        body =
            'Your appointment on ${_formatDate(appointment.date)} at ${appointment.startTime} has been updated.';
    }

    await showNotification(
      id: appointment.id.hashCode ?? 0,
      title: title,
      body: body,
      payload: appointment.id.toString(),
    );
  }

  // Cancel a specific notification
  Future<void> cancelNotification(int id) async {
    await flutterLocalNotificationsPlugin.cancel(id);
  }

  // Cancel all notifications
  Future<void> cancelAllNotifications() async {
    await flutterLocalNotificationsPlugin.cancelAll();
  }

  // Helper method to format date
  String _formatDate(DateTime date) {
    return '${date.day}/${date.month}/${date.year}';
  }
}
