import 'dart:convert';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/bridge/js_engine.dart';
import '../../../core/models/media_item.dart';
import '../../../core/models/chapter.dart';
import '../../../core/models/manga_page.dart';
import '../../anime/providers/extension_provider.dart'; // To reuse jsEngineProvider

// A family provider for executing a search action on a specific JS manga extension source
final mangaSearchProvider = FutureProvider.autoDispose.family<List<MediaItem>, Map<String, dynamic>>((ref, params) async {
  final engine = ref.watch(jsEngineProvider);
  final sourceId = params['sourceId'] as String? ?? 'default_manga_source';
  final query = params['query'] as String;

  final result = await engine.executeAction('manga', sourceId, 'search', {'query': query});
  
  final List<dynamic> jsonList = jsonDecode(result.toString());
  return jsonList.map((e) => MediaItem.fromJson(e as Map<String, dynamic>)).toList();
});

// A family provider for fetching details & chapters of a specific manga
final mangaDetailsProvider = FutureProvider.autoDispose.family<Map<String, dynamic>, Map<String, String>>((ref, params) async {
  final engine = ref.watch(jsEngineProvider);
  final sourceId = params['sourceId']!;
  final url = params['url']!;

  final result = await engine.executeAction('manga', sourceId, 'details', {'url': url});
  final Map<String, dynamic> jsonMap = jsonDecode(result.toString());
  
  final mediaItem = MediaItem.fromJson(jsonMap['manga'] as Map<String, dynamic>);
  final chaptersList = (jsonMap['chapters'] as List<dynamic>?)
      ?.map((e) => Chapter.fromJson(e as Map<String, dynamic>))
      .toList() ?? [];

  return {
    'manga': mediaItem,
    'chapters': chaptersList,
  };
});

// A family provider for extracting manga pages for a specific chapter
final mangaPagesProvider = FutureProvider.autoDispose.family<List<MangaPage>, Map<String, String>>((ref, params) async {
  final engine = ref.watch(jsEngineProvider);
  final sourceId = params['sourceId']!;
  final url = params['url']!;

  final result = await engine.executeAction('manga', sourceId, 'extractPages', {'url': url});
  final List<dynamic> jsonList = jsonDecode(result.toString());
  
  return jsonList.map((e) => MangaPage.fromJson(e as Map<String, dynamic>)).toList();
});
