import 'package:flutter/material.dart';
import 'bookings_screen.dart';
import 'home_screen.dart';
import 'profile_screen.dart';
import 'team_screen.dart';
import '../utils/constants.dart';

class MainTabScreen extends StatefulWidget {
  const MainTabScreen({super.key});

  @override
  State<MainTabScreen> createState() => _MainTabScreenState();
}

class _MainTabScreenState extends State<MainTabScreen> {
  int _currentIndex = 0;

  final List<Widget> _pages = const [
    HomeScreen(),
    TeamScreen(),
    BookingsScreen(),
    ProfileScreen(),
  ];

  final List<_NavItem> _items = const [
    _NavItem(label: '首页', icon: Icons.home_filled),
    _NavItem(label: '团队', icon: Icons.people),
    _NavItem(label: '预约', icon: Icons.calendar_month),
    _NavItem(label: '我的', icon: Icons.person),
  ];

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: IndexedStack(
        index: _currentIndex,
        children: _pages,
      ),
      bottomNavigationBar: BottomNavigationBar(
        currentIndex: _currentIndex,
        onTap: (index) => setState(() => _currentIndex = index),
        type: BottomNavigationBarType.fixed,
        backgroundColor: AppColors.card,
        selectedItemColor: AppColors.primary,
        unselectedItemColor: AppColors.muted,
        selectedLabelStyle: const TextStyle(fontSize: 11),
        unselectedLabelStyle: const TextStyle(fontSize: 11),
        items: _items
            .map(
              (item) => BottomNavigationBarItem(
                icon: Icon(item.icon),
                label: item.label,
              ),
            )
            .toList(),
      ),
    );
  }
}

class _NavItem {
  final String label;
  final IconData icon;

  const _NavItem({required this.label, required this.icon});
}
