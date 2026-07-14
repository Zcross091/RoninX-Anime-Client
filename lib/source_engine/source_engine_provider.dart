import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:roninx/features/tracking/domain/models/tracker_type.dart';
import 'package:roninx/source_engine/models/source_info.dart';
import 'package:roninx/source_engine/providers/anime_source.dart';
import 'package:roninx/source_engine/providers/manga_source.dart';
import 'package:roninx/source_engine/adapters/ronin_api_source.dart';
import 'package:roninx/features/tracking/engine/remote_tracker.dart';
import 'package:roninx/features/tracking/providers/tracker_registry.dart';
import 'package:roninx/features/discovery/providers/discovery_prefs_provider.dart';

final metadataSourceProvider = Provider<RemoteTracker>((ref) {
  final prefs = ref.watch(discoveryPrefsProvider);
  final targetTrackerId = prefs.metadataTrackerId;

  if (targetTrackerId != null) {
    final targetType = TrackerType.tryFromId(targetTrackerId);
    if (targetType != null) {
      final trackers = ref.watch(availableTrackersProvider);
      try {
        final targetTracker = trackers.firstWhere((t) => t.type == targetType);
        if (targetTracker is RemoteTracker) {
          return targetTracker;
        }
      } catch (_) {}
    }
  }

  final primary = ref.watch(primaryTrackerProvider);
  if (primary is RemoteTracker) return primary;

  final trackers = ref.watch(availableTrackersProvider);
  return trackers.firstWhere((t) => t is RemoteTracker) as RemoteTracker;
}, name: 'metadataSourceProvider');

final animeSourceProvider = Provider.family<AnimeSource, SourceInfo>((ref, info) {
  return RoninApiSource();
}, name: 'animeSourceProvider');

final mangaSourceProvider = Provider.family<MangaSource, SourceInfo>((ref, info) {
  // Return RoninApiSource as a dummy or create a RoninMangaSource later if needed.
  throw UnimplementedError('Manga not yet implemented for Ronin API');
}, name: 'mangaSourceProvider');
