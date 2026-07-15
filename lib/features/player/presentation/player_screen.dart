import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:media_kit/media_kit.dart';
import 'package:media_kit_video/media_kit_video.dart';
import 'package:wakelock_plus/wakelock_plus.dart';
import 'package:flutter/services.dart';
import '../../../core/theme/app_theme.dart';
import '../../../shared/providers/stream_provider.dart';

class PlayerScreen extends ConsumerStatefulWidget {
  final String title;
  final String? streamUrl; // Optional direct URL
  final String? animeTitle; // For resolution
  final String? episode; // For resolution

  const PlayerScreen({
    super.key,
    required this.title,
    this.streamUrl,
    this.animeTitle,
    this.episode,
  });

  @override
  ConsumerState<PlayerScreen> createState() => _PlayerScreenState();
}

class _PlayerScreenState extends ConsumerState<PlayerScreen> {
  late final Player player = Player();
  late final VideoController controller = VideoController(player);
  bool _isInitialized = false;
  bool _isInitializing = false;
  String? _error;

  @override
  void initState() {
    super.initState();
    WakelockPlus.enable();
    SystemChrome.setEnabledSystemUIMode(SystemUiMode.immersiveSticky);
    SystemChrome.setPreferredOrientations([
      DeviceOrientation.landscapeLeft,
      DeviceOrientation.landscapeRight,
    ]);

    if (widget.streamUrl != null && widget.streamUrl!.isNotEmpty) {
      _initPlayer(widget.streamUrl!);
    }
  }

  Future<void> _initPlayer(String url) async {
    if (_isInitializing) return;
    _isInitializing = true;
    try {
      await player.open(Media(url));
      if (mounted) {
        setState(() {
          _isInitialized = true;
          _isInitializing = false;
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _error = e.toString();
          _isInitializing = false;
        });
      }
    }
  }

  @override
  void dispose() {
    WakelockPlus.disable();
    SystemChrome.setEnabledSystemUIMode(SystemUiMode.edgeToEdge);
    SystemChrome.setPreferredOrientations([
      DeviceOrientation.portraitUp,
    ]);
    player.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    // If no direct URL, resolve it
    if (widget.streamUrl == null && widget.animeTitle != null && widget.episode != null) {
      final streamAsync = ref.watch(streamResolverProvider({
        'title': widget.animeTitle!,
        'episode': widget.episode!,
      }));

      return streamAsync.when(
        data: (url) {
          if (url != null && !_isInitialized && !_isInitializing && _error == null) {
            WidgetsBinding.instance.addPostFrameCallback((_) {
              if (mounted) _initPlayer(url);
            });
          }
          if (url == null) return _buildError('No stream found for this episode.');
          return _buildPlayerUI();
        },
        loading: () => _buildLoading(),
        error: (e, s) => _buildError(e.toString()),
      );
    }

    return _buildPlayerUI();
  }

  Widget _buildLoading() {
    return Scaffold(
      backgroundColor: Colors.black,
      appBar: AppBar(
        backgroundColor: Colors.transparent,
        elevation: 0,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back, color: Colors.white),
          onPressed: () => Navigator.of(context).pop(),
        ),
      ),
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            const CircularProgressIndicator(color: AppTheme.primaryRed),
            const SizedBox(height: 16),
            Text('Resolving Stream...', style: const TextStyle(color: Colors.white)),
          ],
        ),
      ),
    );
  }

  Widget _buildError(String message) {
    return Scaffold(
      backgroundColor: Colors.black,
      body: Center(
        child: Padding(
          padding: const EdgeInsets.all(24.0),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              const Icon(Icons.error_outline, color: AppTheme.primaryRed, size: 64),
              const SizedBox(height: 16),
              Text(
                message,
                textAlign: TextAlign.center,
                style: const TextStyle(color: Colors.white, fontSize: 16),
              ),
              const SizedBox(height: 24),
              ElevatedButton(
                onPressed: () => Navigator.of(context).pop(),
                style: ElevatedButton.styleFrom(backgroundColor: AppTheme.primaryRed),
                child: const Text('Go Back'),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildPlayerUI() {
    if (_error != null) return _buildError(_error!);

    return Scaffold(
      backgroundColor: Colors.black,
      body: Stack(
        children: [
          Center(
            child: Video(
              controller: controller,
              controls: NoVideoControls,
            ),
          ),
          _buildControlsOverlay(),
          if (!_isInitialized) const Center(child: CircularProgressIndicator(color: AppTheme.primaryRed)),
        ],
      ),
    );
  }

  Widget _buildControlsOverlay() {
    return Column(
      children: [
        _buildTopBar(),
        const Spacer(),
        _buildCenterControls(),
        const Spacer(),
        _buildBottomBar(),
      ],
    );
  }

  Widget _buildTopBar() {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: const BoxDecoration(
        gradient: LinearGradient(
          begin: Alignment.topCenter,
          end: Alignment.bottomCenter,
          colors: [Colors.black54, Colors.transparent],
        ),
      ),
      child: Row(
        children: [
          IconButton(
            icon: const Icon(Icons.arrow_back, color: Colors.white),
            onPressed: () => Navigator.of(context).pop(),
          ),
          Expanded(
            child: Text(
              widget.title,
              style: const TextStyle(color: Colors.white, fontSize: 18),
              overflow: TextOverflow.ellipsis,
            ),
          ),
          IconButton(icon: const Icon(Icons.settings, color: Colors.white), onPressed: () {}),
        ],
      ),
    );
  }

  Widget _buildCenterControls() {
    return Row(
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        IconButton(
          iconSize: 48,
          icon: const Icon(Icons.replay_10, color: Colors.white),
          onPressed: () => player.seek(player.state.position - const Duration(seconds: 10)),
        ),
        const SizedBox(width: 32),
        StreamBuilder(
          stream: player.stream.playing,
          builder: (context, snapshot) {
            final playing = snapshot.data ?? false;
            return IconButton(
              iconSize: 64,
              icon: Icon(playing ? Icons.pause_circle : Icons.play_circle, color: AppTheme.primaryRed),
              onPressed: () => player.playOrPause(),
            );
          },
        ),
        const SizedBox(width: 32),
        IconButton(
          iconSize: 48,
          icon: const Icon(Icons.forward_10, color: Colors.white),
          onPressed: () => player.seek(player.state.position + const Duration(seconds: 10)),
        ),
      ],
    );
  }

  Widget _buildBottomBar() {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: const BoxDecoration(
        gradient: LinearGradient(
          begin: Alignment.bottomCenter,
          end: Alignment.topCenter,
          colors: [Colors.black54, Colors.transparent],
        ),
      ),
      child: Column(
        children: [
          StreamBuilder(
            stream: player.stream.position,
            builder: (context, snapshot) {
              final position = snapshot.data ?? Duration.zero;
              final duration = player.state.duration;
              return SliderTheme(
                data: SliderTheme.of(context).copyWith(
                  activeTrackColor: AppTheme.primaryRed,
                  thumbColor: AppTheme.primaryRed,
                  overlayColor: AppTheme.primaryRed.withOpacity(0.2),
                ),
                child: Slider(
                  value: position.inSeconds.toDouble(),
                  max: duration.inSeconds.toDouble() > 0 ? duration.inSeconds.toDouble() : 1.0,
                  onChanged: (v) => player.seek(Duration(seconds: v.toInt())),
                ),
              );
            },
          ),
          Row(
            children: [
              StreamBuilder(
                stream: player.stream.position,
                builder: (context, snapshot) {
                  final position = snapshot.data ?? Duration.zero;
                  return Text(_formatDuration(position), style: const TextStyle(color: Colors.white));
                },
              ),
              const Spacer(),
              StreamBuilder(
                stream: player.stream.duration,
                builder: (context, snapshot) {
                  final duration = snapshot.data ?? Duration.zero;
                  return Text(_formatDuration(duration), style: const TextStyle(color: Colors.white));
                },
              ),
            ],
          ),
        ],
      ),
    );
  }

  String _formatDuration(Duration d) {
    String twoDigits(int n) => n.toString().padLeft(2, "0");
    String twoDigitMinutes = twoDigits(d.inMinutes.remainder(60));
    String twoDigitSeconds = twoDigits(d.inSeconds.remainder(60));
    final hours = d.inHours;
    if (hours > 0) {
      return "${twoDigits(hours)}:$twoDigitMinutes:$twoDigitSeconds";
    }
    return "$twoDigitMinutes:$twoDigitSeconds";
  }
}
