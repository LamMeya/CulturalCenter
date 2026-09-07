import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../providers/auth_provider.dart';
import '../providers/bookings_provider.dart';
import '../utils/constants.dart';
import '../widgets/booking_card.dart';
import '../widgets/empty_widget.dart';
import '../widgets/error_widget.dart';
import '../widgets/loading_widget.dart';

class BookingsScreen extends StatefulWidget {
  const BookingsScreen({super.key});

  @override
  State<BookingsScreen> createState() => _BookingsScreenState();
}

class _BookingsScreenState extends State<BookingsScreen> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) => _loadData());
  }

  void _loadData() {
    final userId = context.read<AuthProvider>().user?.id;
    if (userId != null) {
      context.read<BookingsProvider>().loadBookings(userId);
    }
  }

  @override
  Widget build(BuildContext context) {
    final user = context.watch<AuthProvider>().user;
    final provider = context.watch<BookingsProvider>();

    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        backgroundColor: AppColors.background,
        elevation: 0,
        title: Text('我的预约', style: TextStyle(color: AppColors.text)),
        iconTheme: IconThemeData(color: AppColors.text),
      ),
      body: RefreshIndicator(
        color: AppColors.primary,
        backgroundColor: AppColors.card,
        onRefresh: () async {
          final userId = context.read<AuthProvider>().user?.id;
          if (userId != null) {
            await context.read<BookingsProvider>().loadBookings(userId);
          }
        },
        child: _buildBody(provider, user?.nickname ?? '--'),
      ),
    );
  }

  Widget _buildBody(BookingsProvider provider, String bookerName) {
    if (provider.isLoading && provider.bookings.isEmpty) {
      return const Center(
        child: LoadingWidget(message: '加载预约记录...'),
      );
    }

    if (provider.errorMessage != null && provider.bookings.isEmpty) {
      return Center(
        child: AppErrorWidget(
          message: provider.errorMessage!,
          onRetry: () => _loadData(),
        ),
      );
    }

    if (provider.bookings.isEmpty) {
      return const Center(
        child: EmptyWidget(
          icon: Icons.calendar_month,
          title: '暂无预约记录',
          subtitle: '去首页选择场馆开始预约吧',
        ),
      );
    }

    return ListView.builder(
      padding: const EdgeInsets.all(16),
      itemCount: provider.bookings.length,
      itemBuilder: (context, index) {
        final booking = provider.bookings[index];
        return Padding(
          padding: const EdgeInsets.only(bottom: 12),
          child: BookingCard(
            booking: booking,
            bookerName: bookerName,
            canCancel: provider.canCancel(booking),
            onCancel: () => _showCancelDialog(provider, booking),
          ),
        );
      },
    );
  }

  Future<void> _showCancelDialog(BookingsProvider provider, dynamic booking) async {
    final confirm = await showDialog<bool>(
      context: context,
      builder: (_) => AlertDialog(
        title: const Text('取消预约'),
        content: const Text('确定要取消这次预约吗？'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('再想想'),
          ),
          TextButton(
            onPressed: () => Navigator.pop(context, true),
            child: const Text('确定取消', style: TextStyle(color: Colors.red)),
          ),
        ],
      ),
    );

    if (confirm == true) {
      final userId = context.read<AuthProvider>().user?.id;
      if (userId != null) {
        await provider.cancelBooking(userId: userId, booking: booking);
        if (provider.errorMessage != null && mounted) {
          _showError(provider.errorMessage!);
          provider.clearError();
        }
      }
    }
  }

  void _showError(String message) {
    showDialog(
      context: context,
      builder: (_) => AlertDialog(
        title: const Text('错误'),
        content: Text(message),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('确定'),
          ),
        ],
      ),
    );
  }
}
