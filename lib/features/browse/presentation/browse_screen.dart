import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:cached_network_image/cached_network_image.dart';
import 'package:go_router/go_router.dart';
import '../../../core/theme/app_theme.dart';
import '../../../shared/providers/anime_provider.dart';
import '../../../shared/models/anime.dart';

class BrowseScreen extends ConsumerStatefulWidget {
  final bool isManga;
  const BrowseScreen({super.key, this.isManga = false});

  @override
  ConsumerState<BrowseScreen> createState() => _BrowseScreenState();
}

class _BrowseScreenState extends ConsumerState<BrowseScreen> {
  final TextEditingController _searchController = TextEditingController();
  String _query = '';
  String? _selectedGenre; // Track active selected genre

  @override
  Widget build(BuildContext context) {
    final searchResults = ref.watch(widget.isManga ? mangaSearchProvider(_query) : animeSearchProvider(_query));
    final genreResults = _selectedGenre != null ? ref.watch(genreAnimeProvider(_selectedGenre!)) : null;

    final genres = [
      'Action',
      'Adventure',
      'Comedy',
      'Drama',
      'Fantasy',
      'Horror',
      'Mystery',
      'Romance',
      'Sci-Fi',
      'Slice of Life',
      'Sports',
      'Thriller'
    ];

    return Scaffold(
      appBar: AppBar(
        title: Text(_selectedGenre ?? (widget.isManga ? 'Browse Manga' : 'Browse Anime')),
        leading: _selectedGenre != null
            ? IconButton(
                icon: const Icon(Icons.arrow_back),
                onPressed: () => setState(() => _selectedGenre = null),
              )
            : null,
      ),
      body: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            if (_selectedGenre == null)
              TextField(
                controller: _searchController,
                onSubmitted: (value) {
                  setState(() {
                    _query = value;
                  });
                },
                decoration: InputDecoration(
                  hintText: widget.isManga ? 'Search manga...' : 'Search anime...',
                  prefixIcon: const Icon(Icons.search),
                  suffixIcon: _query.isNotEmpty
                      ? IconButton(
                          icon: const Icon(Icons.clear),
                          onPressed: () {
                            _searchController.clear();
                            setState(() => _query = '');
                          })
                      : null,
                  filled: true,
                  fillColor: AppTheme.surface,
                  border: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(12),
                    borderSide: BorderSide.none,
                  ),
                ),
              ),
            if (_selectedGenre == null && _query.isEmpty) const SizedBox(height: 24),
            
            // 1. Show Genre Results Grid
            if (_selectedGenre != null) ...[
              Expanded(
                child: genreResults!.when(
                  data: (results) => GridView.builder(
                    gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                      crossAxisCount: 3,
                      childAspectRatio: 0.6,
                      crossAxisSpacing: 12,
                      mainAxisSpacing: 12,
                    ),
                    itemCount: results.length,
                    itemBuilder: (context, index) {
                      final anime = results[index];
                      return _SearchCard(anime: anime, isManga: widget.isManga);
                    },
                  ),
                  loading: () => const Center(child: CircularProgressIndicator()),
                  error: (e, s) => Center(child: Text('Error: $e')),
                ),
              ),
            ]
            // 2. Show Genre Grid Selection List
            else if (_query.isEmpty) ...[
              const Text('Genres', style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
              const SizedBox(height: 16),
              Expanded(
                child: GridView.builder(
                  gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                    crossAxisCount: 2,
                    childAspectRatio: 2.5,
                    crossAxisSpacing: 12,
                    mainAxisSpacing: 12,
                  ),
                  itemCount: genres.length,
                  itemBuilder: (context, index) {
                    final name = genres[index];
                    return InkWell(
                      onTap: () {
                        setState(() {
                          _selectedGenre = name;
                        });
                      },
                      borderRadius: BorderRadius.circular(12),
                      child: Container(
                        decoration: BoxDecoration(
                          color: AppTheme.surface,
                          borderRadius: BorderRadius.circular(12),
                          border: Border.all(color: AppTheme.surfaceLight),
                        ),
                        alignment: Alignment.center,
                        child: Text(name, style: const TextStyle(fontWeight: FontWeight.w500)),
                      ),
                    );
                  },
                ),
              ),
            ]
            // 3. Show Text Search Results Grid
            else ...[
              Text('Results for "$_query"', style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
              const SizedBox(height: 16),
              Expanded(
                child: searchResults.when(
                  data: (results) => GridView.builder(
                    gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                      crossAxisCount: 3,
                      childAspectRatio: 0.6,
                      crossAxisSpacing: 12,
                      mainAxisSpacing: 12,
                    ),
                    itemCount: results.length,
                    itemBuilder: (context, index) {
                      final anime = results[index];
                      return _SearchCard(anime: anime, isManga: widget.isManga);
                    },
                  ),
                  loading: () => const Center(child: CircularProgressIndicator()),
                  error: (e, s) => Center(child: Text('Error: $e')),
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }
}

class _SearchCard extends StatelessWidget {
  final Anime anime;
  final bool isManga;
  const _SearchCard({required this.anime, this.isManga = false});

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: () => context.push('/detail/${anime.id}?manga=$isManga'),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Expanded(
            child: ClipRRect(
              borderRadius: BorderRadius.circular(8),
              child: CachedNetworkImage(
                imageUrl: anime.poster ?? '',
                fit: BoxFit.cover,
                placeholder: (context, url) => Container(color: AppTheme.surfaceLight),
                errorWidget: (context, url, error) => const Icon(Icons.error),
              ),
            ),
          ),
          const SizedBox(height: 8),
          Text(
            anime.title,
            maxLines: 2,
            overflow: TextOverflow.ellipsis,
            style: const TextStyle(fontSize: 12),
          ),
        ],
      ),
    );
  }
}
