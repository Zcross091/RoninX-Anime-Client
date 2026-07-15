import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../source_engine/adapters/ronin_api_source.dart';

final roninApiSourceProvider = Provider((ref) => RoninApiSource());

final streamResolverProvider = FutureProvider.family<String?, Map<String, String>>((ref, params) async {
  final source = ref.read(roninApiSourceProvider);
  final title = params['title']!;
  final episode = params['episode']!;

  try {
    final searchResults = await source.searchEpisode(title, episode);
    if (searchResults.isNotEmpty) {
      final iframeUrl = searchResults.first['url'] as String?;
      if (iframeUrl != null && (iframeUrl.contains('streaming.php') || iframeUrl.contains('embed'))) {
        return await source.decryptGogoStream(iframeUrl);
      }
      return iframeUrl; // Return direct link if not GogoCDN
    }
  } catch (e) {
    rethrow;
  }
  return null;
});
