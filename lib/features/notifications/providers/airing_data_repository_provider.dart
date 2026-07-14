import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:roninx/core/network/http_client.dart';
import 'package:roninx/features/notifications/data/anilist_airing_repository.dart';
import 'package:roninx/features/notifications/data/mal_airing_repository.dart';
import 'package:roninx/features/notifications/data/airing_data_repository.dart';
import 'package:roninx/features/tracking/domain/models/tracker_type.dart';

final airingDataRepositoryProvider = Provider.family<AiringDataRepository, TrackerType>((ref, trackerType) {
  final http = HTTP();
  if (trackerType == TrackerType.myanimelist) {
    return MALAiringRepository(http);
  }
  return AniListAiringRepository(http);
});
