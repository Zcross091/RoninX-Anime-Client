import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:cached_network_image/cached_network_image.dart';
import 'package:go_router/go_router.dart';
import '../../../core/theme/app_theme.dart';
import '../../../shared/providers/sync_providers.dart';

class WatchlistScreen extends ConsumerWidget {
  const WatchlistScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final watchlist = ref.watch(watchListProvider);

    return Scaffold(
      appBar: AppBar(title: const Text('My Watchlist')),
      body: watchlist.isEmpty
          ? const Center(
              child: Text(
                'Your watchlist is empty.',
                style: TextStyle(color: Colors.grey, fontSize: 16),
              ),
            )
          : ListView.builder(
              padding: const EdgeInsets.all(16),
              itemCount: watchlist.length,
              itemBuilder: (context, index) {
                final item = watchlist[index];
                final title = item['title'] ?? 'Unknown Anime';
                final imageUrl = item['image'] ?? '';
                final episodeCount = item['ep_count'] ?? 0;
                final rating = item['score'] ?? '0.0';

                return Container(
                  margin: const EdgeInsets.only(bottom: 16),
                  height: 100,
                  decoration: BoxDecoration(
                    color: AppTheme.surface,
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: InkWell(
                    onTap: () => context.push('/detail/${item['media_id']}'),
                    borderRadius: BorderRadius.circular(12),
                    child: Row(
                      children: [
                        ClipRRect(
                          borderRadius: const BorderRadius.horizontal(left: Radius.circular(12)),
                          child: CachedNetworkImage(
                            imageUrl: imageUrl,
                            width: 80,
                            height: 100,
                            fit: BoxFit.cover,
                            errorWidget: (context, url, error) => Container(
                              width: 80,
                              color: AppTheme.surfaceLight,
                              child: const Icon(Icons.movie),
                            ),
                          ),
                        ),
                        const SizedBox(width: 16),
                        Expanded(
                          child: Column(
                            mainAxisAlignment: MainAxisAlignment.center,
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                title,
                                style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16),
                                maxLines: 1,
                                overflow: TextOverflow.ellipsis,
                              ),
                              const SizedBox(height: 4),
                              Text(
                                'Rating: $rating | Episodes: $episodeCount',
                                style: const TextStyle(color: Colors.grey, fontSize: 13),
                              ),
                            ],
                          ),
                        ),
                        IconButton(
                          icon: const Icon(Icons.delete_outline, color: Colors.grey),
                          onPressed: () {
                            ref.read(watchListProvider.notifier).toggleWatchList({
                              'media_id': item['media_id'],
                              'title': title,
                              'image': imageUrl,
                              'ep_count': episodeCount,
                              'score': rating,
                            });
                          },
                        ),
                        IconButton(
                          icon: const Icon(Icons.play_arrow_rounded, color: AppTheme.primaryRed),
                          onPressed: () => context.push('/detail/${item['media_id']}'),
                        ),
                      ],
                    ),
                  ),
                );
              },
            ),
    );
  }
}
