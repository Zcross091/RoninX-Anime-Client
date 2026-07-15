import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:cached_network_image/cached_network_image.dart';
import 'package:go_router/go_router.dart';
import '../../../core/theme/app_theme.dart';
import '../../../shared/providers/anime_provider.dart';
import '../../../shared/providers/sync_providers.dart';

class DetailScreen extends ConsumerWidget {
  final String id;
  final bool isManga;
  
  const DetailScreen({
    super.key,
    required this.id,
    this.isManga = false,
  });

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final detailAsync = ref.watch(isManga ? mangaDetailProvider(id) : animeDetailProvider(id));
    final episodesAsync = ref.watch(isManga ? mangaChaptersProvider(id) : animeEpisodesProvider(id));

    return Scaffold(
      body: detailAsync.when(
        data: (anime) => CustomScrollView(
          slivers: [
            SliverAppBar(
              expandedHeight: 400,
              pinned: true,
              flexibleSpace: FlexibleSpaceBar(
                background: CachedNetworkImage(
                  imageUrl: anime.poster ?? '',
                  fit: BoxFit.cover,
                  color: Colors.black.withOpacity(0.3),
                  colorBlendMode: BlendMode.darken,
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
                    Text(
                      isManga ? 'Chapters' : 'Episodes',
                      style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                    ),
                    const SizedBox(height: 12),
                    episodesAsync.when(
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
                          final displayNum = ep.number.toInt().toString();
                          return GestureDetector(
                            onTap: () {
                              if (isManga) {
                                context.push('/reader', extra: {
                                  'title': '${anime.title} - Chapter $displayNum',
                                  'chapter': displayNum,
                                  'mangaTitle': anime.title,
                                });
                              } else {
                                context.push('/player', extra: {
                                  'title': '${anime.title} - Episode $displayNum',
                                  'animeTitle': anime.title,
                                  'episode': displayNum,
                                });
                              }
                            },
                            child: Container(
                              decoration: BoxDecoration(
                                color: AppTheme.surface,
                                borderRadius: BorderRadius.circular(8),
                                border: Border.all(color: AppTheme.surfaceLight),
                              ),
                              alignment: Alignment.center,
                              child: Text(displayNum),
                            ),
                          );
                        },
                      ),
                      loading: () => const Center(child: CircularProgressIndicator()),
                      error: (e, s) => Center(child: Text('Error loading contents: $e')),
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
      floatingActionButton: detailAsync.when(
        data: (anime) => Consumer(
          builder: (context, ref, child) {
            final watchList = ref.watch(watchListProvider);
            final isAdded = watchList.any((item) => item['media_id'] == anime.id);

            return FloatingActionButton(
              backgroundColor: AppTheme.primaryRed,
              child: Icon(isAdded ? Icons.bookmark : Icons.bookmark_border, color: Colors.white),
              onPressed: () async {
                final session = ref.read(supabaseAuthProvider);
                if (session == null) {
                  ScaffoldMessenger.of(context).showSnackBar(
                    const SnackBar(content: Text('Please log in first to manage your watchlist.')),
                  );
                  return;
                }
                await ref.read(watchListProvider.notifier).toggleWatchList({
                  'media_id': anime.id,
                  'title': anime.title,
                  'poster': anime.poster,
                  'type': isManga ? 'manga' : 'anime',
                });
              },
            );
          },
        ),
        loading: () => null,
        error: (e, s) => null,
      ),
    );
  }

  Widget _infoChip(String label) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
      decoration: BoxDecoration(
        color: AppTheme.surface,
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: AppTheme.surfaceLight),
      ),
      child: Text(
        label,
        style: const TextStyle(fontSize: 12, color: Colors.white70),
      ),
    );
  }
}
