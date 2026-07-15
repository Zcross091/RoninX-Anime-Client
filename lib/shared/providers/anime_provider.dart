import 'dart:convert';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:http/http.dart' as http;
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
