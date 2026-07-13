import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shonenx/source_engine/models/source_info.dart';
import 'package:shonenx/shared/models/unified_media.dart';

final availableAnimeSourcesProvider = FutureProvider<List<SourceInfo>>((ref) async {
  return [
    const SourceInfo(
      id: 'ronin_api',
      name: 'Ronin API',
      type: SourceType.inbuilt,
      mediaType: MediaType.ANIME,
    )
  ];
}, name: 'availableAnimeSourcesProvider');

final availableMangaSourcesProvider = FutureProvider<List<SourceInfo>>((ref) async {
  return [];
}, name: 'availableMangaSourcesProvider');

final availableNovelSourcesProvider = FutureProvider<List<SourceInfo>>((ref) async {
  return [];
}, name: 'availableNovelSourcesProvider');

final allAvailableSourcesProvider = FutureProvider<List<SourceInfo>>((ref) async {
  final animeSources = await ref.watch(availableAnimeSourcesProvider.future);
  final mangaSources = await ref.watch(availableMangaSourcesProvider.future);
  final novelSources = await ref.watch(availableNovelSourcesProvider.future);
  return [...animeSources, ...mangaSources, ...novelSources];
}, name: 'allAvailableSourcesProvider');
