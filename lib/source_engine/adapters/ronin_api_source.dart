import 'dart:convert';
import 'package:http/http.dart' as http;
import 'package:supabase_flutter/supabase_flutter.dart';
import 'package:encrypt/encrypt.dart';
import 'package:html/parser.dart' show parse;
import '../../core/config/supabase_state.dart';

class RoninApiSource {
  static const String baseUrl = 'https://ronin-api-proxy.vercel.app';

  // AES Keys for GogoCDN
  static const String key = '37911490979715163134003223491201';
  static const String secondKey = '54674138327930866480207815084989';
  static const String iv = '3134003223491201';

  Future<List<Map<String, dynamic>>> searchEpisode(String query, String episode) async {
    // 1. Build title variants to search against
    final rawQuery = query.toLowerCase().trim();
    final slug = rawQuery
        .replaceAll(RegExp(r'[^\w\s]'), '') // strip punctuation
        .replaceAll(RegExp(r'\s+'), ' ')
        .trim();

    // ── Tier 1: Exact title match ──
    List<dynamic> dbData = [];
    if (SupabaseState.isInitialized) {
      dbData = await Supabase.instance.client
          .from('anime_links')
        .select()
        .eq('title', rawQuery)
        .eq('episode', int.tryParse(episode) ?? 0);
    }

    // ── Tier 2: Case-insensitive LIKE on cleaned slug ──
    if (dbData.isEmpty && SupabaseState.isInitialized) {
      dbData = await Supabase.instance.client
          .from('anime_links')
          .select()
          .ilike('title', '%$slug%')
          .eq('episode', int.tryParse(episode) ?? 0);
    }

    // ── Tier 3: Split into keywords, OR-search each word >3 characters ──
    if (dbData.isEmpty && SupabaseState.isInitialized) {
      final keywords = slug.split(' ').where((w) => w.length > 3).toList();
      if (keywords.isNotEmpty) {
        final orFilter = keywords.map((k) => 'title.ilike.%$k%').join(',');
        dbData = await Supabase.instance.client
            .from('anime_links')
            .select()
            .or(orFilter)
            .eq('episode', int.tryParse(episode) ?? 0);
      }
    }

    // ── Tier 4: Token-overlap scoring ──
    if (dbData.isNotEmpty) {
      final queryTokens = slug.split(' ').where((t) => t.isNotEmpty).toList();
      int _scoreRow(Map<String, dynamic> row) {
        final dbTitle = (row['title'] as String? ?? '').toLowerCase();
        return queryTokens.where((t) => dbTitle.contains(t)).length;
      }

      // Require at least 50% token overlap
      final minScore = (queryTokens.length * 0.5).ceil();
      final scored = dbData.where((r) => _scoreRow(r as Map<String, dynamic>) >= minScore).toList();

      if (scored.isEmpty) {
        dbData = [];
      } else {
        scored.sort((a, b) => _scoreRow(b as Map<String, dynamic>).compareTo(_scoreRow(a as Map<String, dynamic>)));
        final bestTitle = (scored.first as Map<String, dynamic>)['title'];
        dbData = scored.where((r) => (r as Map<String, dynamic>)['title'] == bestTitle).toList();
      }
    }

    // ── Cache Miss handling ──
    if (dbData.isEmpty) {
      // Fire-and-forget background trigger to Vercel API
      http.get(Uri.parse('$baseUrl/api/trigger-miner?title=${Uri.encodeComponent(query)}&episode=$episode'));

      throw Exception('Episode not found in cache. The background miner has been triggered! Please check back in a few minutes.');
    }

    return List<Map<String, dynamic>>.from(dbData);
  }

  Future<String?> decryptGogoStream(String iframeUrl) async {
    try {
      final uri = Uri.parse(iframeUrl);
      final id = uri.queryParameters['id'];
      if (id == null) return null;

      final response = await http.get(Uri.parse(iframeUrl), headers: {
        'Referer': 'https://anitaku.to/',
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
      });

      if (response.statusCode != 200) return null;

      final document = parse(response.body);
      final scriptElement = document.querySelector('script[data-name="episode"]');
      final cryptoValue = scriptElement?.attributes['data-value'];

      if (cryptoValue == null) return null;

      // 1. Decrypt Gogo token
      final decryptedToken = _decrypt(cryptoValue, key, iv);
      
      // 2. Encrypt the ID parameter
      final encryptedId = _encrypt(id, key, iv);

      // 3. Build ajax URL: id=encryptedId & alias=id & decryptedToken
      final ajaxBase = iframeUrl.split('/streaming.php').first;
      final ajaxUrl = '$ajaxBase/encrypt-ajax.php?id=$encryptedId&alias=$id&$decryptedToken';

      final ajaxResponse = await http.get(Uri.parse(ajaxUrl), headers: {
        'X-Requested-With': 'XMLHttpRequest',
        'Referer': iframeUrl,
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
      });

      if (ajaxResponse.statusCode != 200) return null;

      final ajaxData = json.decode(ajaxResponse.body);
      final encryptedData = ajaxData['data'] as String?;
      if (encryptedData == null) return null;

      // 4. Decrypt Ajax response data using secondKey
      final decryptedData = _decrypt(encryptedData, secondKey, iv);
      final sources = json.decode(decryptedData);

      if (sources['source'] != null && sources['source'].isNotEmpty) {
        return sources['source'][0]['file'];
      }
    } catch (e) {
      print('GogoCDN decrypt failed: $e');
    }
    return null;
  }

  String _encrypt(String text, String keyStr, String ivStr) {
    final key = Key.fromUtf8(keyStr);
    final iv = IV.fromUtf8(ivStr);
    final encrypter = Encrypter(AES(key, mode: AESMode.cbc, padding: 'PKCS7'));
    return encrypter.encrypt(text, iv: iv).base64;
  }

  String _decrypt(String text, String keyStr, String ivStr) {
    final key = Key.fromUtf8(keyStr);
    final iv = IV.fromUtf8(ivStr);
    final encrypter = Encrypter(AES(key, mode: AESMode.cbc, padding: 'PKCS7'));
    return encrypter.decrypt64(text, iv: iv);
  }
}
