import 'dart:io';
import 'package:http/http.dart' as http;
import 'package:isar/isar.dart';
import 'package:path_provider/path_provider.dart';
import 'offline_media.dart';

class DownloadManager {
  static final DownloadManager _instance = DownloadManager._internal();
  factory DownloadManager() => _instance;

  DownloadManager._internal();

  Isar? _isar;

  Future<void> init() async {
    if (_isar != null) return;
    final dir = await getApplicationDocumentsDirectory();
    _isar = await Isar.open(
      [OfflineMediaSchema, OfflineTaskSchema],
      directory: dir.path,
    );
  }

  Isar get isar {
    if (_isar == null) throw Exception('DownloadManager not initialized');
    return _isar!;
  }

  Future<void> enqueueTask(String mediaId, String chapterId, String url, String fileName) async {
    final task = OfflineTask()
      ..mediaId = mediaId
      ..chapterOrEpisodeId = chapterId
      ..sourceUrl = url
      ..localFileName = fileName
      ..status = DownloadStatus.pending;

    await isar.writeTxn(() async {
      await isar.offlineTasks.put(task);
    });

    _processQueue(); // Fire and forget
  }

  bool _isProcessing = false;

  Future<void> _processQueue() async {
    if (_isProcessing) return;
    _isProcessing = true;

    try {
      while (true) {
        final pendingTask = await isar.offlineTasks
            .filter()
            .statusEqualTo(DownloadStatus.pending)
            .findFirst();

        if (pendingTask == null) break;

        // Mark as downloading
        pendingTask.status = DownloadStatus.downloading;
        await isar.writeTxn(() async => await isar.offlineTasks.put(pendingTask));

        // Attempt download
        final success = await _downloadFile(pendingTask.sourceUrl, pendingTask.localFileName);

        pendingTask.status = success ? DownloadStatus.completed : DownloadStatus.failed;
        await isar.writeTxn(() async => await isar.offlineTasks.put(pendingTask));
      }
    } finally {
      _isProcessing = false;
    }
  }

  Future<bool> _downloadFile(String url, String fileName) async {
    try {
      final dir = await getApplicationDocumentsDirectory();
      final file = File('${dir.path}/downloads/$fileName');
      
      if (!await file.parent.exists()) {
        await file.parent.create(recursive: true);
      }

      final response = await http.get(Uri.parse(url));
      if (response.statusCode == 200) {
        await file.writeAsBytes(response.bodyBytes);
        return true;
      }
      return false;
    } catch (e) {
      print('Download failed: $e');
      return false;
    }
  }
}
