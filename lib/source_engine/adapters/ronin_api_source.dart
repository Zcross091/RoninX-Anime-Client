import 'dart:async';
import 'dart:convert';
import 'package:http/http.dart' as http;
import 'package:flutter_inappwebview/flutter_inappwebview.dart';
import 'package:encrypt/encrypt.dart' as enc;
import 'package:html/parser.dart' as hp;
import 'package:supabase_flutter/supabase_flutter.dart';
import 'package:roninx/core/utils/app_logger.dart';
import 'package:roninx/shared/models/unified_episode.dart';
import 'package:roninx/shared/models/unified_media.dart';
import 'package:roninx/shared/models/video_server.dart';
import 'package:roninx/shared/models/video_stream.dart';
import 'package:roninx/source_engine/models/source_info.dart';
import 'package:roninx/source_engine/providers/anime_source.dart';

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

    final query = parts[0];
    final episode = parts[1];

    try {
      // 1. Build title variants to search against
      final rawQuery = query.toLowerCase().trim();
      final slug = rawQuery
          .replaceAll(RegExp(r'[^\w\s]'), '')   // strip punctuation
          .replaceAll(RegExp(r'\s+'), ' ')
          .trim();

      log.i('Fuzzy searching Supabase for: "$rawQuery" (slug: "$slug") episode $episode');

      // ── Tier 1: Exact title match ──────────────────────────────────────────
      List<dynamic> dbData = await Supabase.instance.client
          .from('anime_links')
          .select()
          .eq('title', rawQuery)
          .eq('episode', int.tryParse(episode) ?? 0);

      // ── Tier 2: Case-insensitive LIKE on cleaned slug ──────────────────────
      if (dbData.isEmpty) {
        log.i('Tier-1 miss. Trying slug ilike: "$slug"');
        dbData = await Supabase.instance.client
            .from('anime_links')
            .select()
            .ilike('title', '%$slug%')
            .eq('episode', int.tryParse(episode) ?? 0);
      }

      // ── Tier 3: Split into keywords, OR-search each significant word ────────
      if (dbData.isEmpty) {
        final keywords = slug
            .split(' ')
            .where((w) => w.length > 3)   // skip short stop-words
            .toList();

        if (keywords.isNotEmpty) {
          log.i('Tier-2 miss. Trying keyword OR search: $keywords');
          // Build an OR filter matching any keyword in the title
          final orFilter = keywords
              .map((k) => 'title.ilike.%$k%')
              .join(',');
          dbData = await Supabase.instance.client
              .from('anime_links')
              .select()
              .or(orFilter)
              .eq('episode', int.tryParse(episode) ?? 0);
        }
      }

      // ── Tier 4: Local token-overlap scoring (pick best fuzzy match) ─────────
      if (dbData.isNotEmpty) {
        // Score every DB row by counting how many query tokens appear in the DB title
        final queryTokens = slug.split(' ').where((t) => t.isNotEmpty).toList();

        int _scoreRow(Map<String, dynamic> row) {
          final dbTitle = (row['title'] as String? ?? '').toLowerCase();
          return queryTokens.where((t) => dbTitle.contains(t)).length;
        }

        // Keep only rows with at least 50% token overlap to avoid garbage matches
        final minScore = (queryTokens.length * 0.5).ceil();
        final scored = dbData
            .where((r) => _scoreRow(r as Map<String, dynamic>) >= minScore)
            .toList();

        if (scored.isEmpty) {
          log.w('All fuzzy matches failed the 50% token overlap threshold. Treating as cache miss.');
          dbData = [];
        } else {
          // Sort descending by score so best match is first
          scored.sort((a, b) =>
              _scoreRow(b as Map<String, dynamic>)
                  .compareTo(_scoreRow(a as Map<String, dynamic>)));
          final bestTitle = (scored.first as Map<String, dynamic>)['title'];
          log.i('Best fuzzy match: "$bestTitle" (${_scoreRow(scored.first as Map<String, dynamic>)}/${queryTokens.length} tokens)');
          // Use only rows that share the best matching title to keep results coherent
          dbData = scored
              .where((r) => (r as Map<String, dynamic>)['title'] == bestTitle)
              .toList();
        }
      }

      if (dbData.isNotEmpty) {
        log.i('Cache hit for "$rawQuery" episode $episode!');
        
        final List<VideoStream> streams = [];
        for (final res in dbData) {
          String url = res['url'];
          final type = res['type'] ?? 'Auto';
          
          if (type == 'http') {
            try {
              // Check if it's an m3u8 directly
              if (!url.contains('.m3u8')) {
                log.i('Resolving M3U8 locally via GogoCDN decrypter: $url');
                final resolvedUrl = await _resolveGogoCDNLocally(url);
                if (resolvedUrl != null) {
                  url = resolvedUrl;
                } else {
                  log.i('Resolving M3U8 from iframe via backend resolver: $url');
                  final serverResolvedUrl = await _resolveM3u8ViaServer(url);
                  if (serverResolvedUrl != null) {
                    url = serverResolvedUrl;
                  } else {
                    log.w('Backend resolver failed. Falling back to local Webview extraction for $url');
                    final extractedUrl = await _extractM3u8WithWebview(url);
                    if (extractedUrl != null) {
                      url = extractedUrl;
                    }
                  }
                }
              }
            } catch (e) {
              log.e('Error during M3U8 extraction', e);
            }
          }
          
          // Only add HTTP streams if they have been successfully resolved to an m3u8 playlist
          if (type == 'http' && !url.contains('.m3u8')) {
            log.w('Skipping stream $url because it failed to resolve to a playable m3u8 link.');
            continue;
          }

          final Map<String, String> headers = {};
          if (url.startsWith('http')) {
            try {
              final uri = Uri.parse(url);
              headers['Referer'] = '${uri.scheme}://${uri.host}/';
              headers['User-Agent'] = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36';
            } catch (_) {}
          }

          streams.add(VideoStream(
            url: url,
            quality: type,
            headers: headers.isNotEmpty ? headers : null,
          ));
        }

        // Prioritize HTTP streams over Torrent magnet links so they are selected by default
        streams.sort((a, b) {
          final aIsTorrent = a.url.startsWith('magnet:') || a.quality.toLowerCase() == 'torrent';
          final bIsTorrent = b.url.startsWith('magnet:') || b.quality.toLowerCase() == 'torrent';
          if (aIsTorrent && !bIsTorrent) return 1;
          if (!aIsTorrent && bIsTorrent) return -1;
          return 0;
        });

        if (streams.isEmpty) {
          throw Exception('No playable video streams were resolved. The background miner has been triggered to fetch working links.');
        }

        return streams;
      }

      // 2. Cache Miss - Trigger Miner
      log.i('Cache miss for $query episode $episode. Triggering GitHub Action Miner...');
      await http.get(Uri.parse('$baseUrl/api/trigger-miner?title=${Uri.encodeComponent(query)}&episode=$episode'));
      
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

  Future<String?> _extractM3u8WithWebview(String url) async {
    final completer = Completer<String?>();
    HeadlessInAppWebView? headlessWebView;
    Timer? timeout;

    headlessWebView = HeadlessInAppWebView(
      initialUrlRequest: URLRequest(url: WebUri(url)),
      initialSettings: InAppWebViewSettings(
        javaScriptEnabled: true,
        useShouldInterceptRequest: true,
        mediaPlaybackRequiresUserGesture: false,
        domStorageEnabled: true,
        databaseEnabled: true,
        userAgent: "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
      ),
      shouldInterceptRequest: (controller, request) async {
        final reqUrl = request.url.toString();
        if (reqUrl.contains('.m3u8')) {
          if (!completer.isCompleted) {
            completer.complete(reqUrl);
          }
        }
        return null;
      },
    );

    timeout = Timer(const Duration(seconds: 15), () {
      if (!completer.isCompleted) completer.complete(null);
    });

    try {
      await headlessWebView.run();
    } catch (_) {}

    final m3u8Url = await completer.future;
    timeout.cancel();
    try {
      await headlessWebView.dispose();
    } catch (_) {}
    
    return m3u8Url;
  }

  Future<String?> _resolveM3u8ViaServer(String iframeUrl) async {
    try {
      final response = await http.get(
        Uri.parse('$baseUrl/api/resolve?url=${Uri.encodeComponent(iframeUrl)}'),
      ).timeout(const Duration(seconds: 8));

      if (response.statusCode == 200) {
        final Map<String, dynamic> data = json.decode(response.body);
        final List<dynamic>? results = data['results'];
        if (results != null && results.isNotEmpty) {
          final bestStream = results.firstWhere(
            (s) => s['url'] != null && s['url'].toString().contains('.m3u8'),
            orElse: () => results.first,
          );
          return bestStream['url']?.toString();
        }
      }
    } catch (e) {
      log.e('Server-side resolve failed: $e');
    }
    return null;
  }

  Future<String?> _resolveGogoCDNLocally(String iframeUrl) async {
    try {
      final videoUrl = Uri.parse(iframeUrl);
      final id = videoUrl.queryParameters['id'];
      if (id == null) return null;

      final res = await http.get(
        videoUrl,
        headers: {
          'Referer': 'https://anitaku.to/',
          'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
        },
      ).timeout(const Duration(seconds: 8));

      if (res.statusCode != 200) return null;

      final document = hp.parse(res.body);
      final script = document.querySelector("script[data-name='episode']");
      final scriptValue = script?.attributes['data-value'];
      if (scriptValue == null) return null;

      // Cryptographic keys matching TypeScript backend
      final key = enc.Key.fromUtf8('37911490979715163134003223491201');
      final secondKey = enc.Key.fromUtf8('54674138327930866480207815084989');
      final iv = enc.IV.fromUtf8('3134003223491201');

      final encrypter = enc.Encrypter(enc.AES(key, mode: enc.AESMode.cbc, padding: 'PKCS7'));
      final secondEncrypter = enc.Encrypter(enc.AES(secondKey, mode: enc.AESMode.cbc, padding: 'PKCS7'));

      // 1. Decrypt data-value token
      final decryptedToken = encrypter.decrypt(enc.Encrypted.fromBase64(scriptValue), iv: iv);

      // 2. Encrypt ID
      final encryptedId = encrypter.encrypt(id, iv: iv).base64;

      // 3. Make AJAX Request to decrypt-ajax
      final ajaxUrl = '${videoUrl.scheme}://${videoUrl.host}/encrypt-ajax.php?'
          'id=${Uri.encodeComponent(encryptedId)}&alias=$id&$decryptedToken';

      final ajaxRes = await http.get(
        Uri.parse(ajaxUrl),
        headers: {
          'X-Requested-With': 'XMLHttpRequest',
          'Referer': iframeUrl,
          'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
        },
      ).timeout(const Duration(seconds: 8));

      if (ajaxRes.statusCode != 200) return null;

      final ajaxData = jsonDecode(ajaxRes.body);
      final encryptedData = ajaxData['data'];
      if (encryptedData == null) return null;

      // 4. Decrypt AJAX Response
      final decryptedAjaxStr = secondEncrypter.decrypt(enc.Encrypted.fromBase64(encryptedData), iv: iv);
      final decryptedAjax = jsonDecode(decryptedAjaxStr);

      final List<dynamic>? sourceList = decryptedAjax['source'];
      if (sourceList != null && sourceList.isNotEmpty) {
        final bestStream = sourceList.firstWhere(
          (s) => s['file'] != null && s['file'].toString().contains('.m3u8'),
          orElse: () => sourceList.first,
        );
        return bestStream['file']?.toString();
      }
    } catch (e) {
      log.e('Local GogoCDN resolution failed: $e');
    }
    return null;
  }
}
