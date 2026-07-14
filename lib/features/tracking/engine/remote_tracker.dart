import 'package:roninx/features/tracking/engine/tracking_service.dart';
import 'package:roninx/core/network/auth/authenticator.dart';
import 'package:roninx/features/tracking/domain/models/tracker_profile.dart';
import 'package:roninx/source_engine/models/tracker_search_result.dart';
import 'package:roninx/shared/models/unified_media.dart';
import 'package:roninx/source_engine/models/paginated_result.dart';
import 'package:roninx/shared/providers/content_prefs_provider.dart';

abstract interface class RemoteTracker implements TrackingService {
  Authenticator get authenticator;

  Future<TrackerProfile> fetchProfile();

  Future<List<String>> fetchGenres();

  Future<List<String>> fetchTags();

  Future<List<TrackerSearchResult>> searchMedia(
    String query, {
    required MediaType type,
  });

  Future<PaginatedResult<UnifiedMedia>> getTrending({
    int page = 1,
    MediaType type = MediaType.ANIME,
    Duration? cacheDuration,
    AdultContentMode adultMode = AdultContentMode.safe,
  });

  Future<PaginatedResult<UnifiedMedia>> search(
    String query, {
    int page = 1,
    required MediaType type,
    List<String>? genres,
    List<String>? tags,
    Duration? cacheDuration,
    AdultContentMode adultMode = AdultContentMode.safe,
  });

  Future<UnifiedMedia> getDetails(String providerId, MediaType type);
}
