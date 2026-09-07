import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import '../models/models.dart';
import '../utils/constants.dart';

class VenueCard extends StatelessWidget {
  final Venue venue;
  final VoidCallback? onTap;

  const VenueCard({super.key, required this.venue, this.onTap});

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(AppColors.radius),
      child: Container(
        decoration: BoxDecoration(
          color: AppColors.card,
          borderRadius: BorderRadius.circular(AppColors.radius),
          boxShadow: [
            BoxShadow(
              color: Colors.black.withOpacity(0.06),
              blurRadius: 4,
              offset: const Offset(0, 2),
            ),
          ],
        ),
        clipBehavior: Clip.antiAlias,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            SizedBox(
              height: 120,
              child: _buildImage(),
            ),
            Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    venue.name,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: TextStyle(
                      fontSize: 16,
                      fontWeight: FontWeight.w600,
                      color: AppColors.text,
                    ),
                  ),
                  const SizedBox(height: 6),
                  Row(
                    children: [
                      Icon(Icons.people, size: 14, color: AppColors.muted),
                      const SizedBox(width: 4),
                      Text(
                        '${venue.capacity}人',
                        style: TextStyle(fontSize: 13, color: AppColors.muted),
                      ),
                      const SizedBox(width: 16),
                      Icon(Icons.square_foot, size: 14, color: AppColors.muted),
                      const SizedBox(width: 4),
                      Text(
                        '${venue.area}m²',
                        style: TextStyle(fontSize: 13, color: AppColors.muted),
                      ),
                    ],
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildImage() {
    final imageUrl = venue.imageUrl;
    if (imageUrl == null || imageUrl.isEmpty) {
      return _gradientPlaceholder();
    }

    return CachedNetworkImage(
      imageUrl: imageUrl,
      fit: BoxFit.cover,
      placeholder: (context, url) => Stack(
        alignment: Alignment.center,
        children: [
          _gradientPlaceholder(),
          CircularProgressIndicator(color: Colors.white.withOpacity(0.8)),
        ],
      ),
      errorWidget: (context, url, error) => _gradientPlaceholder(),
    );
  }

  Widget _gradientPlaceholder() {
    return Container(
      decoration: BoxDecoration(
        gradient: LinearGradient(
          colors: [
            AppColors.primary.withOpacity(0.7),
            AppColors.primary.withOpacity(0.3),
          ],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
      ),
      child: Center(
        child: Icon(
          Icons.account_balance,
          size: 40,
          color: Colors.white.withOpacity(0.8),
        ),
      ),
    );
  }
}
