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

// Scrapes/queries MangaDex or Manganato to fetch pages for a given manga and chapter number
final mangaPagesProvider = FutureProvider.family<List<String>, Map<String, String>>((ref, params) async {
  final title = params['title']!;
  final chapter = params['chapter']!;

  // 1. Try MangaDex First (API-based, stable)
  try {
    final searchUrl = 'https://api.mangadex.org/manga?title=${Uri.encodeComponent(title)}&limit=1';
    final searchRes = await http.get(Uri.parse(searchUrl), headers: {
      'User-Agent': 'RoninXClient/1.0.0 (contact@roninx.app)',
      'Accept': 'application/json',
    }).timeout(const Duration(seconds: 5));

    if (searchRes.statusCode == 200) {
      final searchData = json.decode(searchRes.body);
      if (searchData['data'] != null && searchData['data'].isNotEmpty) {
        final mangaId = searchData['data'][0]['id'] as String;

        // Fetch chapter feed matching chapter number
        final feedUrl = 'https://api.mangadex.org/chapter?manga=$mangaId&chapter=$chapter&translatedLanguage[]=en&limit=1';
        final feedRes = await http.get(Uri.parse(feedUrl), headers: {
          'User-Agent': 'RoninXClient/1.0.0 (contact@roninx.app)',
          'Accept': 'application/json',
        }).timeout(const Duration(seconds: 5));

        if (feedRes.statusCode == 200) {
          final feedData = json.decode(feedRes.body);
          if (feedData['data'] != null && feedData['data'].isNotEmpty) {
            final chapterId = feedData['data'][0]['id'] as String;
            final hash = feedData['data'][0]['attributes']['hash'] as String;
            final pages = List<String>.from(feedData['data'][0]['attributes']['data'] as List);

            // Fetch At-Home server host URL
            final serverUrl = 'https://api.mangadex.org/at-home/server/$chapterId';
            final serverRes = await http.get(Uri.parse(serverUrl), headers: {
              'User-Agent': 'RoninXClient/1.0.0 (contact@roninx.app)',
              'Accept': 'application/json',
            }).timeout(const Duration(seconds: 5));

            if (serverRes.statusCode == 200) {
              final serverData = json.decode(serverRes.body);
              final host = serverData['baseUrl'] as String;
              return pages.map((page) => '$host/data/$hash/$page').toList();
            }
          }
        }
      }
    }
  } catch (_) {}

  // 2. Fall back to Manganato scraping (HTML scraping)
  try {
    final searchUrl = 'https://manganato.com/search/story/${title.toLowerCase().replaceAll(RegExp(r'[^\w\s]'), '').replaceAll(' ', '_')}';
    final searchRes = await http.get(Uri.parse(searchUrl)).timeout(const Duration(seconds: 5));
    if (searchRes.statusCode == 200) {
      final doc = parse(searchRes.body);
      final firstResult = doc.querySelector('.search-story-item a.item-img');
      final mangaUrl = firstResult?.attributes['href'];

      if (mangaUrl != null) {
        final mangaPageRes = await http.get(Uri.parse(mangaUrl)).timeout(const Duration(seconds: 5));
        if (mangaPageRes.statusCode == 200) {
          final mangaDoc = parse(mangaPageRes.body);
          final chapters = mangaDoc.querySelectorAll('.chapter-name');

          String? chapterUrl;
          for (final chap in chapters) {
            final text = chap.text.toLowerCase();
            if (text.contains('chapter $chapter') || text.endsWith(' $chapter')) {
              chapterUrl = chap.attributes['href'];
              break;
            }
          }

          chapterUrl ??= chapters.isNotEmpty ? chapters.first.attributes['href'] : null;

          if (chapterUrl != null) {
            final chapPageRes = await http.get(Uri.parse(chapterUrl), headers: {
              'Referer': 'https://manganato.com/',
            }).timeout(const Duration(seconds: 5));
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

  // 3. Fallback placeholder images if all scraper/APIs fail
  return List.generate(
    10,
    (i) => 'https://placehold.co/600x800/000000/FFFFFF/png?text=Page+${i + 1}+(Offline+Scraper+Fallback)',
  );
});

