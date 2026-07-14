import 'dart:convert';
import 'package:http/http.dart' as http;
import 'package:shonenx/core/utils/app_logger.dart';
import 'package:shonenx/shared/models/unified_episode.dart';
import 'package:shonenx/shared/models/unified_media.dart';
import 'package:shonenx/shared/models/video_server.dart';
import 'package:shonenx/shared/models/video_stream.dart';
import 'package:shonenx/source_engine/models/source_info.dart';
import 'package:shonenx/source_engine/providers/anime_source.dart';

class RoninApiSource extends AnimeSource {
  static const String baseUrl = 'https://ronin-api-proxy.vercel.app';
  final log = AppLogger.scope(RoninApiSource);

  @override
  SourceInfo get sourceInfo => const SourceInfo(
        id: 'ronin_api',
        name: 'Ronin API',
        type: SourceType.inbuilt,
        mediaType: MediaType.ANIME,
      );

  @override
  Future<List<UnifiedMedia>> search(
    String query,
    MediaType type, {
    int page = 1,
    bool isAdult = false,
    List<String> sort = const ['SEARCH_MATCH'],
    List<String> genres = const [],
    List<String> tags = const [],
  }) async {
    // Just return a dummy match so the matchmaker picks this query as the providerId
    return [
      UnifiedMedia(
        id: query,
        type: MediaType.ANIME,
        title: MediaTitle(english: query, romaji: query, native: query),
      )
    ];
  }

  @override
  Future<List<UnifiedMedia>> getTrending({int page = 1}) async {
    return [];
  }

  @override
  Future<UnifiedMedia> getDetails(String providerId, MediaType type) async {
    return UnifiedMedia(
      id: providerId,
      type: MediaType.ANIME,
      title: MediaTitle(english: providerId, romaji: providerId),
    );
  }

  @override
  Future<List<UnifiedEpisode>> getEpisodes(String animeId) async {
    final parts = animeId.split('::');
    final query = parts[0];
    final totalEpisodes = parts.length > 1 ? int.tryParse(parts[1]) ?? 200 : 200;

    return List.generate(
      totalEpisodes,
      (i) => UnifiedEpisode(
        id: '$query|${i + 1}',
        title: 'Episode ${i + 1}',
        number: (i + 1).toDouble(),
      ),
    );
  }

  @override
  Future<List<VideoServer>> getServers(String episodeId) async {
    return [
      VideoServer(id: 'ronin_db', name: 'Ronin Database'),
    ];
  }

  @override
  Future<List<VideoStream>> getSources(String episodeId, VideoServer server) async {
    final parts = episodeId.split('|');
    if (parts.length < 2) return [];

    final query = Uri.encodeComponent(parts[0]);
    final episode = parts[1];

    try {
      // 1. Check Supabase Cache First
      final dbResponse = await http.get(Uri.parse('$baseUrl/api/db?title=$query&episode=$episode'));
      if (dbResponse.statusCode == 200) {
        final List<dynamic> dbData = json.decode(dbResponse.body);
        if (dbData.isNotEmpty) {
          log.i('Cache hit for $query episode $episode!');
          return dbData.map((res) {
            return VideoStream(
              url: res['url'],
              quality: res['type'] ?? 'Auto',
            );
          }).toList();
        }
      }

      // 2. Cache Miss - Trigger Miner
      log.i('Cache miss for $query episode $episode. Triggering GitHub Action Miner...');
      await http.get(Uri.parse('$baseUrl/api/trigger-miner?title=$query&episode=$episode'));
      
      // 3. Inform the user
      throw Exception('Episode not found in cache. The Ronin Miner has been triggered in the background! Please check back in a few minutes.');
      
    } catch (e, st) {
      log.e('Failed to get sources from Ronin API', e, st);
      if (e.toString().contains('Ronin Miner')) {
        rethrow;
      }
    }
    return [];
  }
}
