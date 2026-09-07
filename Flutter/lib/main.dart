import 'package:flutter/material.dart';
import 'package:intl/date_symbol_data_local.dart';
import 'package:provider/provider.dart';
import 'providers/auth_provider.dart';
import 'providers/bookings_provider.dart';
import 'providers/profile_provider.dart';
import 'providers/team_provider.dart';
import 'providers/venue_provider.dart';
import 'screens/login_screen.dart';
import 'screens/main_tab_screen.dart';
import 'utils/constants.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await initializeDateFormatting('zh_CN', null);
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MultiProvider(
      providers: [
        ChangeNotifierProvider(create: (_) => AuthProvider()),
        ChangeNotifierProvider(create: (_) => VenueProvider()),
        ChangeNotifierProvider(create: (_) => TeamProvider()),
        ChangeNotifierProvider(create: (_) => BookingsProvider()),
        ChangeNotifierProvider(create: (_) => ProfileProvider()),
      ],
      child: MaterialApp(
        title: AppConstants.appName,
        debugShowCheckedModeBanner: false,
        theme: ThemeData(
          useMaterial3: true,
          colorScheme: ColorScheme.fromSeed(
            seedColor: AppColors.primary,
            brightness: Brightness.light,
          ),
          scaffoldBackgroundColor: AppColors.background,
          appBarTheme: AppBarTheme(
            backgroundColor: AppColors.background,
            foregroundColor: AppColors.text,
            elevation: 0,
            centerTitle: true,
            titleTextStyle: TextStyle(
              fontSize: 18,
              fontWeight: FontWeight.w600,
              color: AppColors.text,
            ),
          ),
          bottomNavigationBarTheme: BottomNavigationBarThemeData(
            backgroundColor: AppColors.card,
            selectedItemColor: AppColors.primary,
            unselectedItemColor: AppColors.muted,
            type: BottomNavigationBarType.fixed,
          ),
          elevatedButtonTheme: ElevatedButtonThemeData(
            style: ElevatedButton.styleFrom(
              backgroundColor: AppColors.primary,
              foregroundColor: Colors.white,
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(AppColors.radius),
              ),
            ),
          ),
          inputDecorationTheme: InputDecorationTheme(
            filled: true,
            fillColor: AppColors.card,
            border: OutlineInputBorder(
              borderRadius: BorderRadius.circular(AppColors.radius),
              borderSide: BorderSide.none,
            ),
          ),
        ),
        home: const AuthGate(),
      ),
    );
  }
}

class AuthGate extends StatelessWidget {
  const AuthGate({super.key});

  @override
  Widget build(BuildContext context) {
    final authProvider = context.watch<AuthProvider>();

    return AnimatedSwitcher(
      duration: const Duration(milliseconds: 300),
      child: authProvider.isLoggedIn
          ? const MainTabScreen(key: ValueKey('main'))
          : const LoginScreen(key: ValueKey('login')),
    );
  }
}
