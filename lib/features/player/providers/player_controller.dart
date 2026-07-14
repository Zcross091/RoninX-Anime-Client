import 'dart:async';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:media_kit/media_kit.dart';
import 'package:media_kit_video/media_kit_video.dart';
import '../../../shared/providers/sync_providers.dart';

final playerControllerProvider = StateNotifierProvider.family<PlayerNotifier, PlayerState, String>((ref, mediaId) {
  return PlayerNotifier(ref, mediaId);
});

class PlayerState {
  final bool isLoading;
  final String? error;
  final List<String> availableQualities;
  final String? currentQuality;
  final List<String> availableSubtitles;
  final String? currentSubtitle;

  PlayerState({
    this.isLoading = true,
    this.error,
    this.availableQualities = const [],
    this.currentQuality,
    this.availableSubtitles = const [],
    this.currentSubtitle,
  });

  PlayerState copyWith({
    bool? isLoading,
    String? error,
    List<String>? availableQualities,
    String? currentQuality,
    List<String>? availableSubtitles,
    String? currentSubtitle,
  }) {
    return PlayerState(
      isLoading: isLoading ?? this.isLoading,
      error: error ?? this.error,
      availableQualities: availableQualities ?? this.availableQualities,
      currentQuality: currentQuality ?? this.currentQuality,
      availableSubtitles: availableSubtitles ?? this.availableSubtitles,
      currentSubtitle: currentSubtitle ?? this.currentSubtitle,
    );
  }
}

class PlayerNotifier extends StateNotifier<PlayerState> {
  final Ref ref;
  final String mediaId;
  late final Player player;
  late final VideoController controller;
  Timer? _progressTimer;

  PlayerNotifier(this.ref, this.mediaId) : super(PlayerState()) {
    player = Player();
    controller = VideoController(player);
    _initListeners();
  }

  void _initListeners() {
    player.stream.error.listen((error) {
      state = state.copyWith(error: error.toString(), isLoading: false);
    });

    player.stream.playing.listen((playing) {
      if (playing) {
        _startProgressTimer();
      } else {
        _stopProgressTimer();
      }
    });
  }

  Future<void> loadStream(String url, double episodeNumber) async {
    state = state.copyWith(isLoading: true, error: null);
    try {
      await player.open(Media(url));
      state = state.copyWith(isLoading: false);

      // Auto-save progress
      _startProgressTimer(episodeNumber: episodeNumber);
    } catch (e) {
      state = state.copyWith(isLoading: false, error: e.toString());
    }
  }

  void _startProgressTimer({double? episodeNumber}) {
    _progressTimer?.cancel();
    _progressTimer = Timer.periodic(const Duration(seconds: 15), (timer) {
      if (player.state.playing && player.state.duration > Duration.zero) {
        ref.read(watchHistoryProvider.notifier).updateProgress(
          mediaId: mediaId,
          episodeNumber: episodeNumber ?? 0,
          position: player.state.position,
          duration: player.state.duration,
        );
      }
    });
  }

  void _stopProgressTimer() {
    _progressTimer?.cancel();
  }

  @override
  void dispose() {
    _stopProgressTimer();
    player.dispose();
    super.dispose();
  }
}
