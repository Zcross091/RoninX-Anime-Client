import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../features/home/presentation/home_screen.dart';
import '../../features/browse/presentation/browse_screen.dart';
import '../../features/detail/presentation/detail_screen.dart';
import '../../features/player/presentation/player_screen.dart';
import '../../features/watchlist/presentation/watchlist_screen.dart';
import '../../features/reader/presentation/reader_screen.dart';

final routerProvider = Provider<GoRouter>((ref) {
  return GoRouter(
    initialLocation: '/',
    routes: [
      GoRoute(
        path: '/',
        builder: (context, state) => const HomeScreen(),
      ),
      GoRoute(
        path: '/browse',
        builder: (context, state) {
          final isManga = state.uri.queryParameters['manga'] == 'true';
          return BrowseScreen(isManga: isManga);
        },
      ),
      GoRoute(
        path: '/detail/:id',
        builder: (context, state) {
          final isManga = state.uri.queryParameters['manga'] == 'true';
          return DetailScreen(id: state.pathParameters['id']!, isManga: isManga);
        },
      ),
      GoRoute(
        path: '/player',
        builder: (context, state) {
          final extra = state.extra as Map<String, dynamic>?;
          return PlayerScreen(
            title: extra?['title'] ?? 'Unknown',
            streamUrl: extra?['streamUrl'],
            animeTitle: extra?['animeTitle'],
            episode: extra?['episode'],
          );
        },
      ),
      GoRoute(
        path: '/reader',
        builder: (context, state) {
          final extra = state.extra as Map<String, dynamic>?;
          return ReaderScreen(
            title: extra?['title'] ?? 'Chapter',
            chapter: extra?['chapter'] ?? '1',
            mangaTitle: extra?['mangaTitle'] ?? '',
            sourceId: extra?['sourceId'] ?? 'default_manga_source',
            chapterUrl: extra?['chapterUrl'] ?? '',
          );
        },
      ),
      GoRoute(
        path: '/watchlist',
        builder: (context, state) => const WatchlistScreen(),
      ),
    ],
  );
});
