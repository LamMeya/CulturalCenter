import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../providers/auth_provider.dart';
import '../providers/venue_provider.dart';
import '../utils/constants.dart';
import '../widgets/empty_widget.dart';
import '../widgets/error_widget.dart';
import '../widgets/loading_widget.dart';
import '../widgets/notification_banner.dart';
import '../widgets/venue_card.dart';
import 'venue_detail_screen.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      context.read<VenueProvider>().loadData();
    });
  }

  @override
  Widget build(BuildContext context) {
    final user = context.watch<AuthProvider>().user;
    final venueProvider = context.watch<VenueProvider>();

    return Scaffold(
      backgroundColor: AppColors.background,
      body: RefreshIndicator(
        color: AppColors.primary,
        backgroundColor: AppColors.card,
        onRefresh: () => venueProvider.loadData(),
        child: CustomScrollView(
          slivers: [
            SliverToBoxAdapter(
              child: _HomeHeader(nickname: user?.nickname),
            ),
            SliverToBoxAdapter(
              child: _buildNotification(venueProvider.activeNotification),
            ),
            const SliverToBoxAdapter(
              child: Padding(
                padding: EdgeInsets.fromLTRB(16, 20, 16, 12),
                child: Text(
                  '场馆列表',
                  style: TextStyle(
                    fontSize: 18,
                    fontWeight: FontWeight.w600,
                  ),
                ),
              ),
            ),
            _buildBody(venueProvider),
            const SliverPadding(padding: EdgeInsets.only(bottom: 20)),
          ],
        ),
      ),
    );
  }

  Widget _buildNotification(dynamic notification) {
    if (notification == null || notification.content == null) {
      return const SizedBox.shrink();
    }
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 12, 16, 0),
      child: NotificationBanner(notification: notification),
    );
  }

  Widget _buildBody(VenueProvider provider) {
    if (provider.isLoading && provider.venues.isEmpty) {
      return const SliverToBoxAdapter(
        child: LoadingWidget(message: '加载场馆中...'),
      );
    }

    if (provider.errorMessage != null && provider.venues.isEmpty) {
      return SliverToBoxAdapter(
        child: AppErrorWidget(
          message: provider.errorMessage!,
          onRetry: () => provider.loadData(),
        ),
      );
    }

    if (provider.venues.isEmpty) {
      return const SliverToBoxAdapter(
        child: EmptyWidget(
          icon: Icons.account_balance,
          title: '暂无可用场馆',
        ),
      );
    }

    return SliverPadding(
      padding: const EdgeInsets.symmetric(horizontal: 16),
      sliver: SliverGrid(
        gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
          crossAxisCount: 2,
          crossAxisSpacing: 12,
          mainAxisSpacing: 12,
          childAspectRatio: 0.82,
        ),
        delegate: SliverChildBuilderDelegate(
          (context, index) {
            final venue = provider.venues[index];
            return VenueCard(
              venue: venue,
              onTap: () => Navigator.push(
                context,
                MaterialPageRoute(
                  builder: (_) => VenueDetailScreen(venue: venue),
                ),
              ),
            );
          },
          childCount: provider.venues.length,
        ),
      ),
    );
  }
}

class _HomeHeader extends StatelessWidget {
  final String? nickname;

  const _HomeHeader({this.nickname});

  @override
  Widget build(BuildContext context) {
    return Container(
      color: AppColors.card,
      child: Column(
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 8, 16, 12),
            child: Row(
              children: [
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        '珠海文化中心',
                        style: TextStyle(
                          fontSize: 24,
                          fontWeight: FontWeight.bold,
                          color: AppColors.text,
                        ),
                      ),
                      if (nickname != null) ...[
                        const SizedBox(height: 4),
                        Text(
                          '你好，$nickname',
                          style: TextStyle(
                            fontSize: 14,
                            color: AppColors.muted,
                          ),
                        ),
                      ],
                    ],
                  ),
                ),
                CircleAvatar(
                  radius: 22,
                  backgroundColor: AppColors.accent.withOpacity(0.15),
                  child: Icon(Icons.local_fire_department,
                      color: AppColors.accent),
                ),
              ],
            ),
          ),
          Divider(color: AppColors.border, height: 1),
        ],
      ),
    );
  }
}
