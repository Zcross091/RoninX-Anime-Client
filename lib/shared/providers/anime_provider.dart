import 'dart:convert';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:http/http.dart' as http;
import 'package:html/parser.dart' show parse;
import '../models/anime.dart';
import '../models/episode.dart';

// Simple global lock/delay list to throttle Jikan API calls (limit: 3 requests/sec)
DateTime _lastRequestTime = DateTime.now();
Future<void> _throttleJikan() async {
  final now = DateTime.now();
  final diff = now.difference(_lastRequestTime);
  if (diff.inMilliseconds < 400) {
    await Future.delayed(Duration(milliseconds: 400 - diff.inMilliseconds));
  }
  _lastRequestTime = DateTime.now();
}

// Genre mapping for Jikan API
final Map<String, int> jikanGenreIds = {
  'Action': 1,
  'Adventure': 2,
  'Comedy': 4,
  'Drama': 8,
  'Fantasy': 10,
  'Horror': 14,
  'Mystery': 7,
  'Romance': 22,
  'Sci-Fi': 24,
  'Slice of Life': 36,
  'Sports': 30,
  'Thriller': 41,
};

final animeListProvider = FutureProvider.family<List<Anime>, String>((ref, type) async {
  await _throttleJikan();
  final response = await http.get(Uri.parse('https://api.jikan.moe/v4/top/anime?filter=$type'));
  if (response.statusCode == 200) {
    final data = json.decode(response.body);
    return (data['data'] as List).map((e) => Anime.fromJson(e)).toList();
  }
  throw Exception('Failed to load anime');
});

final mangaListProvider = FutureProvider.family<List<Anime>, String>((ref, type) async {
  await _throttleJikan();
  final response = await http.get(Uri.parse('https://api.jikan.moe/v4/top/manga?filter=$type'));
  if (response.statusCode == 200) {
    final data = json.decode(response.body);
    // Reuse Anime model for Manga representation
    return (data['data'] as List).map((e) => Anime.fromJson(e)).toList();
  }
  throw Exception('Failed to load manga');
});

final animeSearchProvider = FutureProvider.family<List<Anime>, String>((ref, query) async {
  if (query.isEmpty) return [];
  await _throttleJikan();
  final response = await http.get(Uri.parse('https://api.jikan.moe/v4/anime?q=$query'));
  if (response.statusCode == 200) {
    final data = json.decode(response.body);
    return (data['data'] as List).map((e) => Anime.fromJson(e)).toList();
  }
  return [];
});

final mangaSearchProvider = FutureProvider.family<List<Anime>, String>((ref, query) async {
  if (query.isEmpty) return [];
  await _throttleJikan();
  final response = await http.get(Uri.parse('https://api.jikan.moe/v4/manga?q=$query'));
  if (response.statusCode == 200) {
    final data = json.decode(response.body);
    return (data['data'] as List).map((e) => Anime.fromJson(e)).toList();
  }
  return [];
});

final genreAnimeProvider = FutureProvider.family<List<Anime>, String>((ref, genreName) async {
  final genreId = jikanGenreIds[genreName];
  if (genreId == null) return [];
  await _throttleJikan();
  final response = await http.get(Uri.parse('https://api.jikan.moe/v4/anime?genres=$genreId&order_by=popularity'));
  if (response.statusCode == 200) {
    final data = json.decode(response.body);
    return (data['data'] as List).map((e) => Anime.fromJson(e)).toList();
  }
  return [];
});

final animeDetailProvider = FutureProvider.family<Anime, String>((ref, id) async {
  await _throttleJikan();
  final response = await http.get(Uri.parse('https://api.jikan.moe/v4/anime/$id'));
  if (response.statusCode == 200) {
    final data = json.decode(response.body);
    return Anime.fromJson(data['data']);
  }
  throw Exception('Failed to load anime details');
});

final animeEpisodesProvider = FutureProvider.family<List<Episode>, String>((ref, id) async {
  await _throttleJikan();
  final response = await http.get(Uri.parse('https://api.jikan.moe/v4/anime/$id/episodes'));
  if (response.statusCode == 200) {
    final data = json.decode(response.body);
    return (data['data'] as List).map((e) => Episode.fromJson(e)).toList();
  }
  return List.generate(24, (index) => Episode(number: index + 1));
});

// --- Manga Providers ---

final mangaDetailProvider = FutureProvider.family<Anime, String>((ref, id) async {
  await _throttleJikan();
  final response = await http.get(Uri.parse('https://api.jikan.moe/v4/manga/$id'));
  if (response.statusCode == 200) {
    final data = json.decode(response.body);
    return Anime.fromJson(data['data']);
  }
  throw Exception('Failed to load manga details');
});

final mangaChaptersProvider = FutureProvider.family<List<Episode>, String>((ref, id) async {
  // Jikan manga details endpoint to extract chapter count
  await _throttleJikan();
  final response = await http.get(Uri.parse('https://api.jikan.moe/v4/manga/$id'));
  if (response.statusCode == 200) {
    final data = json.decode(response.body);
    final chaptersCount = data['data']['chapters'] as int? ?? 50; // Fallback to 50 chapters
    return List.generate(
      chaptersCount,
      (index) => Episode(number: (index + 1).toDouble()),
    );
  }
  return List.generate(50, (index) => Episode(number: (index + 1).toDouble()));
});

// Scrapes/queries Manganato to fetch pages for a given manga and chapter number
final mangaPagesProvider = FutureProvider.family<List<String>, Map<String, String>>((ref, params) async {
  final title = params['title']!;
  final chapter = params['chapter']!;

  try {
    // 1. Search Manganato for the manga
    final searchUrl = 'https://manganato.com/search/story/${title.toLowerCase().replaceAll(RegExp(r'[^\w\s]'), '').replaceAll(' ', '_')}';
    final searchRes = await http.get(Uri.parse(searchUrl));
    if (searchRes.statusCode == 200) {
      final doc = parse(searchRes.body);
      final firstResult = doc.querySelector('.search-story-item a.item-img');
      final mangaUrl = firstResult?.attributes['href'];

      if (mangaUrl != null) {
        // 2. Fetch manga page to list chapters
        final mangaPageRes = await http.get(Uri.parse(mangaUrl));
        if (mangaPageRes.statusCode == 200) {
          final mangaDoc = parse(mangaPageRes.body);
          final chapters = mangaDoc.querySelectorAll('.chapter-name');
          
          // Match the closest chapter number
          String? chapterUrl;
          for (final chap in chapters) {
            final text = chap.text.toLowerCase();
            if (text.contains('chapter $chapter') || text.endsWith(' $chapter')) {
              chapterUrl = chap.attributes['href'];
              break;
            }
          }

          // Fallback to first chapter match if not found
          chapterUrl ??= chapters.isNotEmpty ? chapters.first.attributes['href'] : null;

          if (chapterUrl != null) {
            // 3. Fetch chapter page to scrape image links
            final chapPageRes = await http.get(Uri.parse(chapterUrl), headers: {
              'Referer': 'https://manganato.com/',
            });
            if (chapPageRes.statusCode == 200) {
              final chapDoc = parse(chapPageRes.body);
              final imgs = chapDoc.querySelectorAll('.container-chapter-reader img');
              return imgs.map((img) => img.attributes['src']!).where((src) => src.isNotEmpty).toList();
            }
          }
        }
      }
    }
  } catch (_) {}

  // Fallback placeholder images if scraping fails
  return List.generate(
    10,
    (i) => 'https://placehold.co/600x800/000000/FFFFFF/png?text=Page+${i + 1}+(Offline+Scraper+Fallback)',
  );
});
