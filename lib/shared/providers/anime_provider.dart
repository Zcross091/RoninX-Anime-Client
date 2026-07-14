import 'dart:convert';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:http/http.dart' as http;
import '../models/anime.dart';
import '../models/episode.dart';

final animeListProvider = FutureProvider.family<List<Anime>, String>((ref, type) async {
  final response = await http.get(Uri.parse('https://api.jikan.moe/v4/top/anime?filter=$type'));
  if (response.statusCode == 200) {
    final data = json.decode(response.body);
    return (data['data'] as List).map((e) => Anime.fromJson(e)).toList();
  }
  throw Exception('Failed to load anime');
});

final animeSearchProvider = FutureProvider.family<List<Anime>, String>((ref, query) async {
  if (query.isEmpty) return [];
  final response = await http.get(Uri.parse('https://api.jikan.moe/v4/anime?q=$query'));
  if (response.statusCode == 200) {
    final data = json.decode(response.body);
    return (data['data'] as List).map((e) => Anime.fromJson(e)).toList();
  }
  return [];
});

final animeDetailProvider = FutureProvider.family<Anime, String>((ref, id) async {
  final response = await http.get(Uri.parse('https://api.jikan.moe/v4/anime/$id'));
  if (response.statusCode == 200) {
    final data = json.decode(response.body);
    return Anime.fromJson(data['data']);
  }
  throw Exception('Failed to load anime details');
});

final animeEpisodesProvider = FutureProvider.family<List<Episode>, String>((ref, id) async {
  final response = await http.get(Uri.parse('https://api.jikan.moe/v4/anime/$id/episodes'));
  if (response.statusCode == 200) {
    final data = json.decode(response.body);
    return (data['data'] as List).map((e) => Episode.fromJson(e)).toList();
  }
  return List.generate(24, (index) => Episode(number: index + 1)); // Fallback if Jikan has no episode list
});
