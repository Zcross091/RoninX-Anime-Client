import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:roninx/shared/providers/database_provider.dart';
import 'package:roninx/features/tracking/domain/models/tracker_type.dart';
import 'package:roninx/features/tracking/engine/trackers/local/local_tracker.dart';
import 'package:roninx/features/tracking/engine/trackers/mal/mal_tracker.dart';
import 'package:roninx/features/tracking/engine/tracking_service.dart';
import 'package:roninx/features/tracking/engine/trackers/anilist/anilist_tracker.dart';
import 'package:roninx/features/tracking/engine/trackers/kitsu/kitsu_tracker.dart';
import 'package:roninx/features/tracking/providers/tracking_prefs_provider.dart';

import 'package:roninx/features/tracking/providers/tracker_profile_provider.dart';
import 'package:roninx/shared/models/unified_media.dart';

final availableTrackersProvider = Provider<List<TrackingService>>(
  (ref) => [
    AnilistTracker(ref),
    MalTracker(ref),
    KitsuTracker(ref),
    LocalTracker(ref.watch(databaseProvider)),
  ],
);

final primaryTrackerProvider = Provider<TrackingService>((ref) {
  final preferredType = ref.watch(
    trackingPrefsProvider.select((s) => s.primaryTracker),
  );

  if (preferredType == TrackerType.local) {
    return preferredType.getTracker(ref);
  }

  final profiles = ref.watch(trackerProfileProvider);
  if (profiles.containsKey(preferredType)) {
    return preferredType.getTracker(ref);
  }

  // Preferred is cloud but not logged in. Find next logged in cloud tracker.
  final loggedInCloudTypes = profiles.keys
      .where((t) => t != TrackerType.local)
      .toList();
  if (loggedInCloudTypes.isNotEmpty) {
    return loggedInCloudTypes.first.getTracker(ref);
  }

  // Fallback to local
  return TrackerType.local.getTracker(ref);
});

final activeTrackersProvider =
    Provider.family<List<TrackingService>, MediaType>((ref, mediaType) {
      final availableTrackers = ref.watch(availableTrackersProvider);
      return availableTrackers
          .where((t) => t.supportsMediaType(mediaType))
          .toList();
    });
