import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:media_kit/media_kit.dart';
import 'package:screen_brightness/screen_brightness.dart';

class PlayerGestureLayer extends StatefulWidget {
  final Player player;
  final VoidCallback onSingleTap;

  const PlayerGestureLayer({
    super.key,
    required this.player,
    required this.onSingleTap,
  });

  @override
  State<PlayerGestureLayer> createState() => _PlayerGestureLayerState();
}

class _PlayerGestureLayerState extends State<PlayerGestureLayer> {
  // Ghost Scrubbing State
  Duration? _scrubPosition;
  Duration? _startScrubPosition;
  double _dragStartX = 0.0;
  bool _isScrubbing = false;

  // Volume & Brightness State
  double _startVolume = 0.0;
  double _startBrightness = 0.0;
  bool _isAdjustingVolume = false;
  bool _isAdjustingBrightness = false;
  
  // Indicator Display
  String? _indicatorText;
  IconData? _indicatorIcon;
  bool _showIndicator = false;

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: widget.onSingleTap,
      onDoubleTapDown: _handleDoubleTap,
      onHorizontalDragStart: _onHorizontalDragStart,
      onHorizontalDragUpdate: _onHorizontalDragUpdate,
      onHorizontalDragEnd: _onHorizontalDragEnd,
      onVerticalDragStart: _onVerticalDragStart,
      onVerticalDragUpdate: _onVerticalDragUpdate,
      onVerticalDragEnd: _onVerticalDragEnd,
      child: Container(
        color: Colors.transparent,
        child: Stack(
          children: [
            // Ghost Scrubbing UI overlay
            if (_isScrubbing && _scrubPosition != null)
              Center(
                child: Container(
                  padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 12),
                  decoration: BoxDecoration(
                    color: Colors.black.withOpacity(0.7),
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: Text(
                    _formatDuration(_scrubPosition!),
                    style: const TextStyle(
                      color: Colors.white,
                      fontSize: 32,
                      fontWeight: FontWeight.bold,
                      letterSpacing: 2.0,
                    ),
                  ),
                ),
              ),
              
            // Brightness / Volume Indicator overlay
            if (_showIndicator && _indicatorText != null)
              Center(
                child: Container(
                  padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 16),
                  decoration: BoxDecoration(
                    color: Colors.black.withOpacity(0.7),
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Icon(_indicatorIcon, color: Colors.white, size: 48),
                      const SizedBox(height: 12),
                      Text(
                        _indicatorText!,
                        style: const TextStyle(
                          color: Colors.white,
                          fontSize: 24,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                    ],
                  ),
                ),
              ),
          ],
        ),
      ),
    );
  }

  void _handleDoubleTap(TapDownDetails details) {
    final width = MediaQuery.of(context).size.width;
    if (details.localPosition.dx < width / 3) {
      // Seek Backward
      widget.player.seek(widget.player.state.position - const Duration(seconds: 10));
      _showTempIndicator(Icons.replay_10, '-10s');
    } else if (details.localPosition.dx > width * 2 / 3) {
      // Seek Forward
      widget.player.seek(widget.player.state.position + const Duration(seconds: 10));
      _showTempIndicator(Icons.forward_10, '+10s');
    } else {
      widget.player.playOrPause();
    }
  }

  // --- Horizontal Drag (Ghost Scrubbing) ---

  void _onHorizontalDragStart(DragStartDetails details) {
    if (_isAdjustingVolume || _isAdjustingBrightness) return;
    _isScrubbing = true;
    _startScrubPosition = widget.player.state.position;
    _dragStartX = details.globalPosition.dx;
    HapticFeedback.lightImpact();
    setState(() {});
  }

  void _onHorizontalDragUpdate(DragUpdateDetails details) {
    if (!_isScrubbing) return;
    
    final width = MediaQuery.of(context).size.width;
    final dx = details.globalPosition.dx - _dragStartX;
    
    // 90 seconds max scrub per full screen width drag
    final secondsDelta = (dx / width) * 90; 
    
    var newPos = _startScrubPosition! + Duration(seconds: secondsDelta.toInt());
    if (newPos < Duration.zero) newPos = Duration.zero;
    if (newPos > widget.player.state.duration) newPos = widget.player.state.duration;
    
    if (_scrubPosition?.inSeconds != newPos.inSeconds) {
      if (newPos.inSeconds % 10 == 0) { // Haptic tick every 10 seconds crossed
        HapticFeedback.selectionClick();
      }
      setState(() {
        _scrubPosition = newPos;
      });
    }
  }

  void _onHorizontalDragEnd(DragEndDetails details) {
    if (_isScrubbing && _scrubPosition != null) {
      widget.player.seek(_scrubPosition!);
      HapticFeedback.mediumImpact();
    }
    setState(() {
      _isScrubbing = false;
      _scrubPosition = null;
    });
  }

  // --- Vertical Drag (Brightness / Volume) ---

  Future<void> _onVerticalDragStart(DragStartDetails details) async {
    if (_isScrubbing) return;
    final width = MediaQuery.of(context).size.width;
    if (details.globalPosition.dx < width / 2) {
      // Left side: Brightness
      _isAdjustingBrightness = true;
      try {
        _startBrightness = await ScreenBrightness().current;
      } catch (e) {
        _startBrightness = 0.5; // fallback
      }
    } else {
      // Right side: Volume
      _isAdjustingVolume = true;
      _startVolume = widget.player.state.volume;
    }
  }

  void _onVerticalDragUpdate(DragUpdateDetails details) {
    if (_isScrubbing) return;
    final height = MediaQuery.of(context).size.height;
    // Dragging UP is negative dx, so we negate to make UP = positive increment
    final delta = -(details.primaryDelta! / height); 

    if (_isAdjustingBrightness) {
      _startBrightness = (_startBrightness + delta).clamp(0.0, 1.0);
      try {
        ScreenBrightness().setScreenBrightness(_startBrightness);
      } catch (e) {
        // Platform not supported or error
      }
      setState(() {
        _indicatorIcon = Icons.brightness_6;
        _indicatorText = '${(_startBrightness * 100).toInt()}%';
        _showIndicator = true;
      });
    } else if (_isAdjustingVolume) {
      _startVolume = (_startVolume + (delta * 100)).clamp(0.0, 100.0);
      widget.player.setVolume(_startVolume);
      setState(() {
        _indicatorIcon = Icons.volume_up;
        _indicatorText = '${_startVolume.toInt()}%';
        _showIndicator = true;
      });
    }
  }

  void _onVerticalDragEnd(DragEndDetails details) {
    _isAdjustingBrightness = false;
    _isAdjustingVolume = false;
    Future.delayed(const Duration(milliseconds: 800), () {
      if (mounted && !_isAdjustingBrightness && !_isAdjustingVolume) {
        setState(() {
          _showIndicator = false;
        });
      }
    });
  }

  void _showTempIndicator(IconData icon, String text) {
    setState(() {
      _indicatorIcon = icon;
      _indicatorText = text;
      _showIndicator = true;
    });
    Future.delayed(const Duration(milliseconds: 800), () {
      if (mounted) setState(() => _showIndicator = false);
    });
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
