import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:cached_network_image/cached_network_image.dart';
import '../../../core/theme/app_theme.dart';
import '../../../shared/providers/anime_provider.dart';

class ReaderScreen extends ConsumerWidget {
  final String title;
  final String chapter;
  final String mangaTitle;

  const ReaderScreen({
    super.key,
    required this.title,
    required this.chapter,
    required this.mangaTitle,
  });

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final pagesAsync = ref.watch(mangaPagesProvider({
      'title': mangaTitle,
      'chapter': chapter,
    }));

    return Scaffold(
      backgroundColor: Colors.black,
      appBar: AppBar(
        title: Column(
          crossAxisAlignment: CrossAxisAlignment.center,
          children: [
            Text(title, style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
            Text('Chapter $chapter', style: const TextStyle(fontSize: 12, color: Colors.grey)),
          ],
        ),
        backgroundColor: Colors.black87,
        elevation: 0,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back, color: Colors.white),
          onPressed: () => Navigator.of(context).pop(),
        ),
      ),
      body: pagesAsync.when(
        data: (pages) => InteractiveViewer(
          minScale: 1.0,
          maxScale: 3.5,
          child: ListView.builder(
            itemCount: pages.length,
            padding: EdgeInsets.zero,
            itemBuilder: (context, index) {
              return CachedNetworkImage(
                imageUrl: pages[index],
                fit: BoxFit.contain,
                httpHeaders: const {
                  'Referer': 'https://manganato.com/',
                },
                placeholder: (context, url) => Container(
                  height: 400,
                  color: Colors.black,
                  alignment: Alignment.center,
                  child: const CircularProgressIndicator(color: AppTheme.primaryRed),
                ),
                errorWidget: (context, url, error) => Container(
                  height: 150,
                  color: AppTheme.surface,
                  alignment: Alignment.center,
                  child: const Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Icon(Icons.broken_image, color: Colors.grey, size: 36),
                      SizedBox(height: 8),
                      Text('Failed to load page image', style: TextStyle(color: Colors.grey)),
                    ],
                  ),
                ),
              );
            },
          ),
        ),
        loading: () => const Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              CircularProgressIndicator(color: AppTheme.primaryRed),
              SizedBox(height: 16),
              Text('Scraping Pages...', style: TextStyle(color: Colors.white)),
            ],
          ),
        ),
        error: (e, s) => Center(
          child: Padding(
            padding: const EdgeInsets.all(24.0),
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                const Icon(Icons.error_outline, color: AppTheme.primaryRed, size: 64),
                const SizedBox(height: 16),
                Text('Error: $e', style: const TextStyle(color: Colors.white), textAlign: TextAlign.center),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
