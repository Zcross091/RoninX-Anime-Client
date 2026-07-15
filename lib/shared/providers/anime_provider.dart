import 'dart:convert';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:http/http.dart' as http;
import 'package:html/parser.dart' show parse;
import '../models/anime.dart';
import '../models/episode.dart';
import 'dart:async';

// Throttling is no longer aggressively required for Kitsu, but we keep a relaxed version just in case
DateTime _lastRequestTime = DateTime.now();
Completer<void>? _throttleCompleter;

Future<void> _throttleRequest() async {
  while (_throttleCompleter != null) {
    await _throttleCompleter!.future;
  }
  _throttleCompleter = Completer<void>();
  final now = DateTime.now();
  final diff = now.difference(_lastRequestTime);
  if (diff.inMilliseconds < 200) {
    await Future.delayed(Duration(milliseconds: 200 - diff.inMilliseconds));
  }
  _lastRequestTime = DateTime.now();
  final completer = _throttleCompleter;
  _throttleCompleter = null;
  completer?.complete();
}

final animeListProvider = FutureProvider.family<List<Anime>, String>((ref, type) async {
  await _throttleRequest();
  String url = 'https://kitsu.io/api/edge/anime?sort=-userCount';
  if (type == 'airing') {
    url = 'https://kitsu.io/api/edge/anime?filter[status]=current&sort=-userCount';
  } else if (type == 'upcoming') {
    url = 'https://kitsu.io/api/edge/anime?filter[status]=upcoming&sort=-userCount';
  }
  
  final response = await http.get(Uri.parse(url), headers: {'Accept': 'application/vnd.api+json'});
  if (response.statusCode == 200) {
    final data = json.decode(response.body);
    return (data['data'] as List).map((e) => Anime.fromJson(e)).toList();
  }
  throw Exception('Failed to load anime');
});

final mangaListProvider = FutureProvider.family<List<Anime>, String>((ref, type) async {
  await _throttleRequest();
  String url = 'https://kitsu.io/api/edge/manga?sort=-userCount';
  if (type == 'manga') {
    url = 'https://kitsu.io/api/edge/manga?filter[subtype]=manga&sort=-userCount';
  } else if (type == 'novels') {
    url = 'https://kitsu.io/api/edge/manga?filter[subtype]=novel&sort=-userCount';
  }
  
  final response = await http.get(Uri.parse(url), headers: {'Accept': 'application/vnd.api+json'});
  if (response.statusCode == 200) {
    final data = json.decode(response.body);
    return (data['data'] as List).map((e) => Anime.fromJson(e)).toList();
  }
  throw Exception('Failed to load manga');
});

final animeSearchProvider = FutureProvider.family<List<Anime>, String>((ref, query) async {
  if (query.isEmpty) return [];
  await _throttleRequest();
  final response = await http.get(
    Uri.parse('https://kitsu.io/api/edge/anime?filter[text]=${Uri.encodeComponent(query)}'),
    headers: {'Accept': 'application/vnd.api+json'},
  );
  if (response.statusCode == 200) {
    final data = json.decode(response.body);
    return (data['data'] as List).map((e) => Anime.fromJson(e)).toList();
  }
  return [];
});

final mangaSearchProvider = FutureProvider.family<List<Anime>, String>((ref, query) async {
  if (query.isEmpty) return [];
  await _throttleRequest();
  final response = await http.get(
    Uri.parse('https://kitsu.io/api/edge/manga?filter[text]=${Uri.encodeComponent(query)}'),
    headers: {'Accept': 'application/vnd.api+json'},
  );
  if (response.statusCode == 200) {
    final data = json.decode(response.body);
    return (data['data'] as List).map((e) => Anime.fromJson(e)).toList();
  }
  return [];
});

final genreAnimeProvider = FutureProvider.family<List<Anime>, String>((ref, genreName) async {
  await _throttleRequest();
  final response = await http.get(
    Uri.parse('https://kitsu.io/api/edge/anime?filter[categories]=${Uri.encodeComponent(genreName)}&sort=-userCount'),
    headers: {'Accept': 'application/vnd.api+json'},
  );
  if (response.statusCode == 200) {
    final data = json.decode(response.body);
    return (data['data'] as List).map((e) => Anime.fromJson(e)).toList();
  }
  return [];
});

final animeDetailProvider = FutureProvider.family<Anime, String>((ref, id) async {
  await _throttleRequest();
  final response = await http.get(
    Uri.parse('https://kitsu.io/api/edge/anime/$id'),
    headers: {'Accept': 'application/vnd.api+json'},
  );
  if (response.statusCode == 200) {
    final data = json.decode(response.body);
    return Anime.fromJson(data['data']);
  }
  throw Exception('Failed to load anime details');
});

final animeEpisodesProvider = FutureProvider.family<List<Episode>, String>((ref, id) async {
  await _throttleRequest();
  final response = await http.get(
    Uri.parse('https://kitsu.io/api/edge/anime/$id'),
    headers: {'Accept': 'application/vnd.api+json'},
  );
  if (response.statusCode == 200) {
    final data = json.decode(response.body);
    final count = data['data']['attributes']['episodeCount'] as int? ?? 12;
    return List.generate(count, (index) => Episode(number: index + 1));
  }
  return List.generate(12, (index) => Episode(number: index + 1));
});

// --- Manga Providers ---

final mangaDetailProvider = FutureProvider.family<Anime, String>((ref, id) async {
  await _throttleRequest();
  final response = await http.get(
    Uri.parse('https://kitsu.io/api/edge/manga/$id'),
    headers: {'Accept': 'application/vnd.api+json'},
  );
  if (response.statusCode == 200) {
    final data = json.decode(response.body);
    return Anime.fromJson(data['data']);
  }
  throw Exception('Failed to load manga details');
});

final mangaChaptersProvider = FutureProvider.family<List<Episode>, String>((ref, id) async {
  await _throttleRequest();
  final response = await http.get(
    Uri.parse('https://kitsu.io/api/edge/manga/$id'),
    headers: {'Accept': 'application/vnd.api+json'},
  );
  if (response.statusCode == 200) {
    final data = json.decode(response.body);
    final count = data['data']['attributes']['chapterCount'] as int? ?? 500;
    return List.generate(count, (index) => Episode(number: index + 1));
  }
  return List.generate(100, (index) => Episode(number: index + 1));
});

// Scrapes/queries Manganato to fetch pages for a given manga and chapter number
final mangaPagesProvider = FutureProvider.family<List<String>, Map<String, String>>((ref, params) async {
  final title = params['title']!;
  final chapter = params['chapter']!;

  try {
    // 1. Search Manganato for the manga
    final searchUrl = 'https://manganato.com/search/story/${title.toLowerCase().replaceAll(RegExp(r'[^\w\s]'), '').replaceAll(' ', '_')}';
    final searchRes = await http.get(Uri.parse(searchUrl)).timeout(const Duration(seconds: 10));
    if (searchRes.statusCode == 200) {
      final doc = parse(searchRes.body);
      final firstResult = doc.querySelector('.search-story-item a.item-img');
      final mangaUrl = firstResult?.attributes['href'];

      if (mangaUrl != null) {
        // 2. Fetch manga page to list chapters
        final mangaPageRes = await http.get(Uri.parse(mangaUrl)).timeout(const Duration(seconds: 10));
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
            }).timeout(const Duration(seconds: 10));
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
