import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../models/models.dart';
import '../providers/auth_provider.dart';
import '../services/api_service.dart';
import '../utils/constants.dart';
import '../utils/date_utils.dart';
import '../widgets/date_cell.dart';
import '../widgets/loading_widget.dart';
import '../widgets/time_slot_cell.dart';

class VenueDetailScreen extends StatefulWidget {
  final Venue venue;

  const VenueDetailScreen({super.key, required this.venue});

  @override
  State<VenueDetailScreen> createState() => _VenueDetailScreenState();
}

class _VenueDetailScreenState extends State<VenueDetailScreen> {
  final ApiService _apiService = ApiService();
  final List<DateTime> _availableDates = AppDateUtils.nextAvailableDates();
  late DateTime _selectedDate;
  List<TimeSlot> _timeSlots = [];
  bool _isLoading = true;
  bool _isSubmitting = false;
  String? _errorMessage;
  final Set<int> _selectedSlotIds = {};

  final int _maxSlots = 4;

  @override
  void initState() {
    super.initState();
    _selectedDate = _availableDates.first;
    _loadTimeSlots();
  }

  List<TimeSlot> get _morningSlots =>
      _timeSlots.where((s) => s.period == SlotPeriod.morning).toList();

  List<TimeSlot> get _afternoonSlots =>
      _timeSlots.where((s) => s.period == SlotPeriod.afternoon).toList();

  Future<void> _loadTimeSlots() async {
    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });

    try {
      final slots = await _apiService.fetchTimeSlots(
        widget.venue.id,
        AppDateUtils.formatApiDate(_selectedDate),
      );
      setState(() {
        _timeSlots = slots;
        _isLoading = false;
      });
    } catch (e) {
      setState(() {
        _timeSlots = [];
        _errorMessage = e.toString();
        _isLoading = false;
      });
    }
  }

  void _onSlotTap(TimeSlot slot) {
    if (!slot.isOpen || slot.isBooked) return;

    setState(() {
      if (_selectedSlotIds.contains(slot.id)) {
        _selectedSlotIds.remove(slot.id);
      } else {
        if (_selectedSlotIds.length >= _maxSlots) return;
        _selectedSlotIds.add(slot.id);
      }
    });
  }

  Future<void> _submitBooking() async {
    if (_selectedSlotIds.isEmpty) return;

    final teamId = context.read<AuthProvider>().user?.teamId;
    if (teamId == null) {
      _showNoTeamDialog();
      return;
    }

    setState(() => _isSubmitting = true);

    try {
      await _apiService.createBooking(
        venueId: widget.venue.id,
        teamId: teamId,
        timeSlotIds: _selectedSlotIds.toList(),
      );
      if (mounted) _showSuccessDialog();
    } catch (e) {
      setState(() => _errorMessage = e.toString());
    } finally {
      if (mounted) setState(() => _isSubmitting = false);
    }
  }

  void _showNoTeamDialog() {
    showDialog(
      context: context,
      builder: (_) => AlertDialog(
        title: const Text('请先加入团队'),
        content: const Text('预约场地需要先加入一个团队。'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('确定'),
          ),
        ],
      ),
    );
  }

  void _showSuccessDialog() {
    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (_) => AlertDialog(
        title: const Text('预约成功'),
        content: const Text('您的场地预约申请已提交，请等待审核结果。'),
        actions: [
          TextButton(
            onPressed: () {
              Navigator.pop(context);
              Navigator.pop(context);
            },
            child: const Text('确定'),
          ),
        ],
      ),
    );
  }

  void _showErrorDialog(String message) {
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

  @override
  Widget build(BuildContext context) {
    if (_errorMessage != null) {
      final message = _errorMessage;
      _errorMessage = null;
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (message != null) _showErrorDialog(message);
      });
    }

    return Scaffold(
      backgroundColor: AppColors.background,
      body: Stack(
        children: [
          CustomScrollView(
            slivers: [
              SliverToBoxAdapter(child: _heroSection()),
              SliverToBoxAdapter(child: _infoSection()),
              SliverToBoxAdapter(child: _dateSelectionSection()),
              SliverToBoxAdapter(child: _timeSlotSection()),
              const SliverPadding(padding: EdgeInsets.only(bottom: 100)),
            ],
          ),
          Positioned(
            left: 0,
            right: 0,
            bottom: 0,
            child: _bottomBar(),
          ),
        ],
      ),
    );
  }

  Widget _heroSection() {
    return Container(
      height: 200,
      decoration: BoxDecoration(
        gradient: LinearGradient(
          colors: [AppColors.primary, AppColors.primaryDark],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
      ),
      child: Stack(
        alignment: Alignment.bottomLeft,
        children: [
          Positioned(
            right: 20,
            top: 40,
            child: Icon(
              Icons.account_balance,
              size: 80,
              color: Colors.white.withOpacity(0.2),
            ),
          ),
          Container(
            width: double.infinity,
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              gradient: LinearGradient(
                colors: [
                  Colors.black.withOpacity(0.4),
                  Colors.transparent,
                ],
                begin: Alignment.bottomCenter,
                end: Alignment.topCenter,
              ),
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              mainAxisSize: MainAxisSize.min,
              children: [
                Text(
                  widget.venue.name,
                  style: const TextStyle(
                    fontSize: 22,
                    fontWeight: FontWeight.bold,
                    color: Colors.white,
                  ),
                ),
                const SizedBox(height: 8),
                Row(
                  children: [
                    Icon(Icons.people, size: 14, color: Colors.white70),
                    const SizedBox(width: 4),
                    Text(
                      '${widget.venue.capacity}人',
                      style: const TextStyle(fontSize: 13, color: Colors.white70),
                    ),
                    const SizedBox(width: 16),
                    Icon(Icons.square_foot, size: 14, color: Colors.white70),
                    const SizedBox(width: 4),
                    Text(
                      '${widget.venue.area}m²',
                      style: const TextStyle(fontSize: 13, color: Colors.white70),
                    ),
                    const SizedBox(width: 16),
                    Expanded(
                      child: Row(
                        children: [
                          Icon(Icons.location_on,
                              size: 14, color: Colors.white70),
                          const SizedBox(width: 4),
                          Expanded(
                            child: Text(
                              widget.venue.address,
                              maxLines: 1,
                              overflow: TextOverflow.ellipsis,
                              style: const TextStyle(
                                  fontSize: 13, color: Colors.white70),
                            ),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _infoSection() {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(16),
      color: AppColors.card,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            widget.venue.description,
            style: TextStyle(
              fontSize: 14,
              color: AppColors.text,
              height: 1.5,
            ),
          ),
          if (widget.venue.facilities.isNotEmpty) ...[
            const SizedBox(height: 12),
            SizedBox(
              height: 32,
              child: ListView.separated(
                scrollDirection: Axis.horizontal,
                itemCount: widget.venue.facilities.length,
                separatorBuilder: (_, __) => const SizedBox(width: 8),
                itemBuilder: (context, index) {
                  final facility = widget.venue.facilities[index];
                  return Container(
                    padding:
                        const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
                    decoration: BoxDecoration(
                      color: AppColors.primary.withOpacity(0.08),
                      borderRadius: BorderRadius.circular(8),
                    ),
                    child: Row(
                      children: [
                        Icon(
                          _facilityIcon(facility),
                          size: 12,
                          color: AppColors.primary,
                        ),
                        const SizedBox(width: 4),
                        Text(
                          facility,
                          style: TextStyle(
                            fontSize: 12,
                            color: AppColors.primary,
                          ),
                        ),
                      ],
                    ),
                  );
                },
              ),
            ),
          ],
        ],
      ),
    );
  }

  IconData _facilityIcon(String name) {
    final lower = name.toLowerCase();
    if (lower.contains('空调') || lower.contains('ac')) return Icons.ac_unit;
    if (lower.contains('投影') || lower.contains('projector'))
      return Icons.tv;
    if (lower.contains('音响') || lower.contains('audio'))
      return Icons.speaker;
    if (lower.contains('wifi')) return Icons.wifi;
    if (lower.contains('停车') || lower.contains('parking'))
      return Icons.local_parking;
    if (lower.contains('舞台') || lower.contains('stage'))
      return Icons.theater_comedy;
    return Icons.label;
  }

  Widget _dateSelectionSection() {
    return Padding(
      padding: const EdgeInsets.only(top: 16, bottom: 4),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Padding(
            padding: EdgeInsets.symmetric(horizontal: 16),
            child: Text(
              '选择日期',
              style: TextStyle(
                fontSize: 16,
                fontWeight: FontWeight.w600,
              ),
            ),
          ),
          const SizedBox(height: 12),
          SizedBox(
            height: 80,
            child: ListView.builder(
              scrollDirection: Axis.horizontal,
              padding: const EdgeInsets.symmetric(horizontal: 16),
              itemCount: _availableDates.length,
              itemBuilder: (context, index) {
                final date = _availableDates[index];
                return Padding(
                  padding: const EdgeInsets.only(right: 10),
                  child: DateCell(
                    date: date,
                    isSelected: AppDateUtils.isSameDay(date, _selectedDate),
                    onTap: () {
                      setState(() {
                        _selectedDate = date;
                        _selectedSlotIds.clear();
                      });
                      _loadTimeSlots();
                    },
                  ),
                );
              },
            ),
          ),
        ],
      ),
    );
  }

  Widget _timeSlotSection() {
    return Padding(
      padding: const EdgeInsets.only(top: 16, bottom: 16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16),
            child: Row(
              children: [
                const Text(
                  '选择时间段',
                  style: TextStyle(
                    fontSize: 16,
                    fontWeight: FontWeight.w600,
                  ),
                ),
                const Spacer(),
                if (_selectedSlotIds.isNotEmpty)
                  Container(
                    padding:
                        const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                    decoration: BoxDecoration(
                      color: AppColors.primary.withOpacity(0.1),
                      borderRadius: BorderRadius.circular(10),
                    ),
                    child: Text(
                      '已选 ${_selectedSlotIds.length}/$_maxSlots 个',
                      style: TextStyle(
                        fontSize: 13,
                        color: AppColors.primary,
                      ),
                    ),
                  ),
              ],
            ),
          ),
          const SizedBox(height: 12),
          if (_isLoading)
            const Center(
              child: Padding(
                padding: EdgeInsets.symmetric(vertical: 24),
                child: LoadingWidget(message: '加载时间段...'),
              ),
            )
          else if (_timeSlots.isEmpty)
            Center(
              child: Padding(
                padding: const EdgeInsets.symmetric(vertical: 24),
                child: Text(
                  '该日期暂无可用时间段',
                  style: TextStyle(fontSize: 14, color: AppColors.muted),
                ),
              ),
            )
          else ...[
            if (_morningSlots.isNotEmpty) _periodSection('上午', _morningSlots),
            if (_afternoonSlots.isNotEmpty)
              _periodSection('下午', _afternoonSlots),
          ],
        ],
      ),
    );
  }

  Widget _periodSection(String title, List<TimeSlot> slots) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
            child: Text(
              title,
              style: TextStyle(
                fontSize: 14,
                fontWeight: FontWeight.w500,
                color: AppColors.muted,
              ),
            ),
          ),
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16),
            child: GridView.builder(
              shrinkWrap: true,
              physics: const NeverScrollableScrollPhysics(),
              gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                crossAxisCount: 2,
                crossAxisSpacing: 10,
                mainAxisSpacing: 10,
                childAspectRatio: 2.8,
              ),
              itemCount: slots.length,
              itemBuilder: (context, index) {
                final slot = slots[index];
                return TimeSlotCell(
                  slot: slot,
                  isSelected: _selectedSlotIds.contains(slot.id),
                  onTap: () => _onSlotTap(slot),
                );
              },
            ),
          ),
        ],
      ),
    );
  }

  Widget _bottomBar() {
    final enabled = _selectedSlotIds.isNotEmpty && !_isSubmitting;

    return Container(
      decoration: BoxDecoration(
        color: AppColors.card,
        border: Border(top: BorderSide(color: AppColors.border)),
      ),
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
      child: SafeArea(
        child: Row(
          children: [
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                mainAxisSize: MainAxisSize.min,
                children: [
                  Text(
                    '已选 ${_selectedSlotIds.length} 个时段',
                    style: TextStyle(fontSize: 14, color: AppColors.text),
                  ),
                  Text(
                    AppDateUtils.formatDisplayDate(_selectedDate),
                    style: TextStyle(fontSize: 12, color: AppColors.muted),
                  ),
                ],
              ),
            ),
            GestureDetector(
              onTap: enabled ? _submitBooking : null,
              child: Container(
                padding:
                    const EdgeInsets.symmetric(horizontal: 28, vertical: 14),
                decoration: BoxDecoration(
                  color: enabled
                      ? AppColors.primary
                      : AppColors.muted.withOpacity(0.4),
                  borderRadius: BorderRadius.circular(AppColors.radius),
                ),
                child: _isSubmitting
                    ? const SizedBox(
                        width: 18,
                        height: 18,
                        child: CircularProgressIndicator(
                          color: Colors.white,
                          strokeWidth: 2,
                        ),
                      )
                    : const Text(
                        '确认预约',
                        style: TextStyle(
                          fontSize: 16,
                          fontWeight: FontWeight.w600,
                          color: Colors.white,
                        ),
                      ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
