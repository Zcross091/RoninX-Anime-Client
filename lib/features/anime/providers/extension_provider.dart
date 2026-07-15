import 'dart:convert';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/bridge/js_engine.dart';
import '../../../core/models/media_item.dart';
import '../../../core/models/episode.dart';
import '../../../core/models/stream_source.dart';

final jsEngineProvider = Provider<JSEngine>((ref) {
  final engine = JSEngine();
  ref.onDispose(() => engine.dispose());
  return engine;
});

// A family provider for executing a search action on a specific JS extension source
final extensionSearchProvider = FutureProvider.autoDispose.family<List<MediaItem>, Map<String, dynamic>>((ref, params) async {
  final engine = ref.watch(jsEngineProvider);
  final sourceId = params['sourceId'] as String? ?? 'default_anime_source';
  final query = params['query'] as String;

  final result = await engine.executeAction('anime', sourceId, 'search', {'query': query});
  
  // Assuming the JS script returns a JSON string of a list
  final List<dynamic> jsonList = jsonDecode(result.toString());
  return jsonList.map((e) => MediaItem.fromJson(e as Map<String, dynamic>)).toList();
});

// A family provider for fetching details & episodes of a specific anime
final extensionDetailsProvider = FutureProvider.autoDispose.family<Map<String, dynamic>, Map<String, String>>((ref, params) async {
  final engine = ref.watch(jsEngineProvider);
  final sourceId = params['sourceId']!;
  final url = params['url']!;

  final result = await engine.executeAction('anime', sourceId, 'details', {'url': url});
  final Map<String, dynamic> jsonMap = jsonDecode(result.toString());
  
  final mediaItem = MediaItem.fromJson(jsonMap['anime'] as Map<String, dynamic>);
  final episodesList = (jsonMap['episodes'] as List<dynamic>?)
      ?.map((e) => Episode.fromJson(e as Map<String, dynamic>))
      .toList() ?? [];

  return {
    'anime': mediaItem,
    'episodes': episodesList,
  };
});

// A family provider for extracting video streams for a specific episode
final extensionVideoExtractorProvider = FutureProvider.autoDispose.family<VideoExtraction, Map<String, String>>((ref, params) async {
  final engine = ref.watch(jsEngineProvider);
  final sourceId = params['sourceId']!;
  final url = params['url']!;

  final result = await engine.executeAction('anime', sourceId, 'extractVideo', {'url': url});
  final Map<String, dynamic> jsonMap = jsonDecode(result.toString());
  
  return VideoExtraction.fromJson(jsonMap);
});
