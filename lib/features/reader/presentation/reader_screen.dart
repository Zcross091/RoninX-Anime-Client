import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/services.dart';
import '../../../core/models/manga_page.dart';
import '../providers/manga_extension_provider.dart';
import '../../../core/theme/app_theme.dart';

enum ReadingMode { vertical, horizontal }

class ReaderScreen extends ConsumerStatefulWidget {
  final String title;
  final String sourceId;
  final String chapterUrl;
  final ReadingMode initialMode;
  final String? chapter;
  final String? mangaTitle;

  const ReaderScreen({
    super.key,
    required this.title,
    required this.sourceId,
    required this.chapterUrl,
    this.initialMode = ReadingMode.vertical,
    this.chapter,
    this.mangaTitle,
  });

  @override
  ConsumerState<ReaderScreen> createState() => _ReaderScreenState();
}

class _ReaderScreenState extends ConsumerState<ReaderScreen> {
  late ReadingMode _readingMode;
  late final PageController _pageController;
  final ScrollController _scrollController = ScrollController();
  bool _showControls = true;

  @override
  void initState() {
    super.initState();
    _readingMode = widget.initialMode;
    _pageController = PageController();
    SystemChrome.setEnabledSystemUIMode(SystemUiMode.immersiveSticky);
  }

  @override
  void dispose() {
    _pageController.dispose();
    _scrollController.dispose();
    SystemChrome.setEnabledSystemUIMode(SystemUiMode.edgeToEdge);
    super.dispose();
  }

  void _toggleControls() {
    setState(() => _showControls = !_showControls);
  }

  void _precacheNextImages(BuildContext context, List<MangaPage> pages, int currentIndex) {
    // Pre-cache next 3 images
    for (int i = 1; i <= 3; i++) {
      if (currentIndex + i < pages.length) {
        precacheImage(
          CachedNetworkImageProvider(
            pages[currentIndex + i].url,
            headers: pages[currentIndex + i].headers,
          ),
          context,
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final pagesAsync = ref.watch(mangaPagesProvider({
      'sourceId': widget.sourceId,
      'url': widget.chapterUrl,
    }));

    return Scaffold(
      backgroundColor: Colors.black,
      body: Stack(
        children: [
          GestureDetector(
            onTap: _toggleControls,
            child: pagesAsync.when(
              data: (pages) {
                if (pages.isEmpty) {
                  return const Center(child: Text('No pages found', style: TextStyle(color: Colors.white)));
                }

                if (_readingMode == ReadingMode.vertical) {
                  return InteractiveViewer(
                    minScale: 1.0,
                    maxScale: 3.5,
                    child: ListView.builder(
                      controller: _scrollController,
                      itemCount: pages.length,
                      padding: EdgeInsets.zero,
                      cacheExtent: 5000, // Pre-cache massive areas ahead for vertical scroll
                      itemBuilder: (context, index) {
                        _precacheNextImages(context, pages, index);
                        return CachedNetworkImage(
                          imageUrl: pages[index].url,
                          httpHeaders: pages[index].headers,
                          fit: BoxFit.fitWidth,
                          width: double.infinity,
                          placeholder: (context, url) => Container(
                            height: 400,
                            color: Colors.grey[900],
                            child: const Center(child: CircularProgressIndicator(color: AppTheme.primaryRed)),
                          ),
                          errorWidget: (context, url, error) => Container(
                            height: 400,
                            color: Colors.grey[900],
                            child: const Center(child: Icon(Icons.broken_image, color: AppTheme.primaryRed, size: 36)),
                          ),
                        );
                      },
                    ),
                  );
                } else {
                  return PageView.builder(
                    controller: _pageController,
                    itemCount: pages.length,
                    onPageChanged: (index) => _precacheNextImages(context, pages, index),
                    itemBuilder: (context, index) {
                      return InteractiveViewer(
                        child: CachedNetworkImage(
                          imageUrl: pages[index].url,
                          httpHeaders: pages[index].headers,
                          fit: BoxFit.contain,
                          placeholder: (context, url) => const Center(
                            child: CircularProgressIndicator(color: AppTheme.primaryRed),
                          ),
                          errorWidget: (context, url, error) => const Center(
                            child: Icon(Icons.broken_image, color: AppTheme.primaryRed, size: 36),
                          ),
                        ),
                      );
                    },
                  );
                }
              },
              loading: () => const Center(child: CircularProgressIndicator(color: AppTheme.primaryRed)),
              error: (e, s) => Center(child: Text(e.toString(), style: const TextStyle(color: Colors.white))),
            ),
          ),
          
          if (_showControls) _buildTopBar(),
          if (_showControls) _buildBottomBar(),
        ],
      ),
    );
  }

  Widget _buildTopBar() {
    return Positioned(
      top: 0,
      left: 0,
      right: 0,
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 24),
        decoration: const BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topCenter,
            end: Alignment.bottomCenter,
            colors: [Colors.black87, Colors.transparent],
          ),
        ),
        child: SafeArea(
          bottom: false,
          child: Row(
            children: [
              IconButton(
                icon: const Icon(Icons.arrow_back, color: Colors.white),
                onPressed: () => Navigator.of(context).pop(),
              ),
              Expanded(
                child: Text(
                  widget.title,
                  style: const TextStyle(color: Colors.white, fontSize: 18, fontWeight: FontWeight.bold),
                  overflow: TextOverflow.ellipsis,
                ),
              ),
              IconButton(
                icon: Icon(
                  _readingMode == ReadingMode.vertical ? Icons.view_day : Icons.view_carousel,
                  color: Colors.white,
                ),
                onPressed: () {
                  setState(() {
                    _readingMode = _readingMode == ReadingMode.vertical
                        ? ReadingMode.horizontal
                        : ReadingMode.vertical;
                  });
                },
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildBottomBar() {
    // In horizontal mode, show a simple page slider. In vertical mode, scroll progress.
    return Positioned(
      bottom: 0,
      left: 0,
      right: 0,
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 24),
        decoration: const BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.bottomCenter,
            end: Alignment.topCenter,
            colors: [Colors.black87, Colors.transparent],
          ),
        ),
        child: SafeArea(
          top: false,
          child: Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              IconButton(
                icon: const Icon(Icons.skip_previous, color: Colors.white),
                onPressed: () {}, // Navigate previous chapter
              ),
              const Text(
                'Reading',
                style: TextStyle(color: Colors.white70, fontSize: 16),
              ),
              IconButton(
                icon: const Icon(Icons.skip_next, color: Colors.white),
                onPressed: () {}, // Navigate next chapter
              ),
            ],
          ),
        ),
      ),
    );
  }
}
