import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:cached_network_image/cached_network_image.dart';
import 'package:go_router/go_router.dart';
import '../../../core/theme/app_theme.dart';
import '../../../shared/providers/anime_provider.dart';

class DetailScreen extends ConsumerWidget {
  final String id;
  const DetailScreen({super.key, required this.id});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final animeDetail = ref.watch(animeDetailProvider(id));
    final episodes = ref.watch(animeEpisodesProvider(id));

    return Scaffold(
      body: animeDetail.when(
        data: (anime) => CustomScrollView(
          slivers: [
            SliverAppBar(
              expandedHeight: 400,
              pinned: true,
              flexibleSpace: FlexibleSpaceBar(
                background: CachedNetworkImage(
                  imageUrl: anime.poster ?? '',
                  fit: BoxFit.cover,
                  colorFilter: ColorFilter.mode(Colors.black.withOpacity(0.3), BlendMode.darken),
                ),
              ),
            ),
            SliverToBoxAdapter(
              child: Padding(
                padding: const EdgeInsets.all(16.0),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(anime.title, style: const TextStyle(fontSize: 24, fontWeight: FontWeight.bold)),
                    const SizedBox(height: 8),
                    Wrap(
                      spacing: 8,
                      children: [
                        if (anime.type != null) _infoChip(anime.type!),
                        if (anime.status != null) _infoChip(anime.status!),
                        if (anime.genres != null) ...anime.genres!.take(3).map((g) => _infoChip(g)),
                      ],
                    ),
                    const SizedBox(height: 16),
                    Text(
                      anime.description ?? 'No description available.',
                      style: const TextStyle(color: Colors.grey, height: 1.5),
                    ),
                    const SizedBox(height: 24),
                    const Text('Episodes', style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
                    const SizedBox(height: 12),
                    episodes.when(
                      data: (epList) => GridView.builder(
                        shrinkWrap: true,
                        physics: const NeverScrollableScrollPhysics(),
                        gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                          crossAxisCount: 5,
                          crossAxisSpacing: 10,
                          mainAxisSpacing: 10,
                        ),
                        itemCount: epList.length,
                        itemBuilder: (context, index) {
                          final ep = epList[index];
                          return GestureDetector(
                            onTap: () => context.push('/player', extra: {
                              'title': '${anime.title} - Episode ${ep.number}',
                              'animeTitle': anime.title,
                              'episode': ep.number.toString(),
                            }),
                            child: Container(
                              decoration: BoxDecoration(
                                color: AppTheme.surface,
                                borderRadius: BorderRadius.circular(8),
                                border: Border.all(color: AppTheme.surfaceLight),
                              ),
                              alignment: Alignment.center,
                              child: Text('${ep.number}'),
                            ),
                          );
                        },
                      ),
                      loading: () => const Center(child: CircularProgressIndicator()),
                      error: (e, s) => Center(child: Text('Error loading episodes: $e')),
                    ),
                  ],
                ),
              ),
            ),
          ],
        ),
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (e, s) => Scaffold(appBar: AppBar(), body: Center(child: Text('Error: $e'))),
      ),
      floatingActionButton: animeDetail.when(
        data: (anime) {
          final watchlist = ref.watch(watchListProvider);
          final isInWatchlist = watchlist.any((e) => e['media_id'] == anime.id);

          return FloatingActionButton.extended(
            onPressed: () {
              ref.read(watchListProvider.notifier).toggleWatchList({
                'media_id': anime.id,
                'title': anime.title,
                'image': anime.poster,
                'ep_count': 0, // Fallback placeholder
                'score': '0.0', // Fallback placeholder
              });
              ScaffoldMessenger.of(context).showSnackBar(
                SnackBar(
                  content: Text(
                    isInWatchlist
                        ? 'Removed from Watchlist'
                        : 'Added to Watchlist',
                  ),
                  duration: const Duration(seconds: 2),
                ),
              );
            },
            backgroundColor: AppTheme.primaryRed,
            label: Text(
              isInWatchlist ? 'In Watchlist' : 'Add to Watchlist',
              style: const TextStyle(color: Colors.white),
            ),
            icon: Icon(
              isInWatchlist ? Icons.bookmark_added : Icons.bookmark_add,
              color: Colors.white,
            ),
          );
        },
        loading: () => const SizedBox.shrink(),
        error: (e, s) => const SizedBox.shrink(),
      ),
    );
  }

  Widget _infoChip(String label) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      decoration: BoxDecoration(
        color: AppTheme.surface,
        borderRadius: BorderRadius.circular(4),
      ),
      child: Text(label, style: const TextStyle(fontSize: 12, color: Colors.grey)),
    );
  }
}
