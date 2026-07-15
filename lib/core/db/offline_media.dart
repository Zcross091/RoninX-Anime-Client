import 'package:isar/isar.dart';

part 'offline_media.g.dart';

@collection
class OfflineMedia {
  Id id = Isar.autoIncrement; // you can also use id = null to auto increment

  @Index(unique: true, replace: true)
  late String sourceMediaId; // E.g., 'mangapill_123'

  late String title;
  
  String? coverUrl;
  
  bool isManga = false;

  late String localPath; // The directory where the media is stored

  // Store metadata as JSON strings for simplicity
  String? metadataJson;

  DateTime downloadDate = DateTime.now();
}

@collection
class OfflineTask {
  Id id = Isar.autoIncrement;

  late String mediaId; // Reference to OfflineMedia
  
  late String chapterOrEpisodeId;

  late String sourceUrl; // The remote URL to download
  
  late String localFileName; // e.g., 'episode_1.mp4' or 'page_1.jpg'

  @enumerated
  DownloadStatus status = DownloadStatus.pending;

  double progress = 0.0;
}

enum DownloadStatus {
  pending,
  downloading,
  completed,
  failed,
}
