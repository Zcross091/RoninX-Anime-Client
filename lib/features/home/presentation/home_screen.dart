import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:cached_network_image/cached_network_image.dart';
import 'package:go_router/go_router.dart';
import '../../../core/theme/app_theme.dart';
import '../../../shared/providers/anime_provider.dart';
import '../../../shared/models/anime.dart';
import '../../../shared/providers/sync_providers.dart';

class HomeScreen extends ConsumerWidget {
  const HomeScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return Scaffold(
      body: CustomScrollView(
        slivers: [
          SliverAppBar(
            floating: true,
            title: Text(
              'RONINX',
              style: TextStyle(
                color: AppTheme.primaryRed,
                fontWeight: FontWeight.bold,
                letterSpacing: 2,
              ),
            ),
            actions: [
              IconButton(icon: const Icon(Icons.search), onPressed: () => context.push('/browse')),
              Consumer(
                builder: (context, ref, child) {
                  final session = ref.watch(supabaseAuthProvider);
                  final isLoggedIn = session != null;
                  return IconButton(
                    icon: Icon(
                      isLoggedIn ? Icons.account_circle : Icons.person_outline,
                      color: isLoggedIn ? AppTheme.primaryRed : Colors.white,
                    ),
                    onPressed: () => _showAuthDialog(context, ref, isLoggedIn),
                  );
                },
              ),
            ],
          ),
          SliverToBoxAdapter(
            child: _buildHeroCarousel(context, ref),
          ),
          _buildSection(ref, 'Trending Now', 'airing'),
          _buildSection(ref, 'Top Airing', 'upcoming'),
          _buildSection(ref, 'Popular', 'bypopularity'),
          const SliverPadding(padding: EdgeInsets.only(bottom: 100)),
        ],
      ),
      bottomNavigationBar: _buildBottomNav(context),
    );
  }

  Widget _buildSection(WidgetRef ref, String title, String filter) {
    final animeList = ref.watch(animeListProvider(filter));

    return SliverMainAxisGroup(
      slivers: [
        SliverPadding(
          padding: const EdgeInsets.fromLTRB(16, 24, 16, 12),
          sliver: SliverToBoxAdapter(
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text(title, style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
                TextButton(onPressed: () {}, child: const Text('See All')),
              ],
            ),
          ),
        ),
        SliverToBoxAdapter(
          child: SizedBox(
            height: 220,
            child: animeList.when(
              data: (data) => ListView.builder(
                scrollDirection: Axis.horizontal,
                padding: const EdgeInsets.symmetric(horizontal: 16),
                itemCount: data.length,
                itemBuilder: (context, index) {
                  final anime = data[index];
                  return _AnimeCard(anime: anime);
                },
              ),
              loading: () => const Center(child: CircularProgressIndicator()),
              error: (e, s) => Center(child: Text('Error: $e')),
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildHeroCarousel(BuildContext context, WidgetRef ref) {
    final trending = ref.watch(animeListProvider('airing'));

    return trending.when(
      data: (data) {
        if (data.isEmpty) return const SizedBox.shrink();
        final anime = data.first;
        return GestureDetector(
          onTap: () => context.push('/detail/${anime.id}'),
          child: Container(
            height: 250,
            margin: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              borderRadius: BorderRadius.circular(16),
              image: DecorationImage(
                image: CachedNetworkImageProvider(anime.poster ?? ''),
                fit: BoxFit.cover,
                colorFilter: ColorFilter.mode(Colors.black.withOpacity(0.4), BlendMode.darken),
              ),
            ),
            child: Stack(
              children: [
                Positioned(
                  bottom: 20,
                  left: 20,
                  right: 20,
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        anime.title,
                        style: const TextStyle(fontSize: 24, fontWeight: FontWeight.bold, color: Colors.white),
                        maxLines: 2,
                        overflow: TextOverflow.ellipsis,
                      ),
                      const SizedBox(height: 8),
                      ElevatedButton(
                        onPressed: () => context.push('/detail/${anime.id}'),
                        style: ElevatedButton.styleFrom(backgroundColor: AppTheme.primaryRed),
                        child: const Text('Watch Now', style: TextStyle(color: Colors.white)),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
        );
      },
      loading: () => Container(height: 250, margin: const EdgeInsets.all(16), color: AppTheme.surface),
      error: (e, s) => const SizedBox.shrink(),
    );
  }

  Widget _buildBottomNav(BuildContext context) {
    return Container(
      color: Colors.black,
      child: SafeArea(
        child: Padding(
          padding: const EdgeInsets.symmetric(vertical: 8),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.spaceAround,
            children: [
              _navItem(Icons.home, 'Home', true, () {}),
              _navItem(Icons.explore, 'Browse', false, () => context.push('/browse')),
              _navItem(Icons.bookmark, 'Watchlist', false, () => context.push('/watchlist')),
              _navItem(Icons.settings, 'Settings', false, () {}),
            ],
          ),
        ),
      ),
    );
  }

  Widget _navItem(IconData icon, String label, bool isActive, VoidCallback onTap) {
    return GestureDetector(
      onTap: onTap,
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(icon, color: isActive ? AppTheme.primaryRed : Colors.grey),
          Text(label, style: TextStyle(color: isActive ? AppTheme.primaryRed : Colors.grey, fontSize: 12)),
        ],
      ),
    );
  }
}

class _AnimeCard extends StatelessWidget {
  final Anime anime;
  const _AnimeCard({required this.anime});

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: () => context.push('/detail/${anime.id}'),
      child: Container(
        width: 140,
        margin: const EdgeInsets.only(right: 12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Expanded(
              child: ClipRRect(
                borderRadius: BorderRadius.circular(8),
                child: CachedNetworkImage(
                  imageUrl: anime.poster ?? '',
                  fit: BoxFit.cover,
                  placeholder: (context, url) => Container(color: AppTheme.surfaceLight),
                  errorWidget: (context, url, error) => const Icon(Icons.error),
                ),
              ),
            ),
            const SizedBox(height: 8),
            Text(
              anime.title,
              maxLines: 2,
              overflow: TextOverflow.ellipsis,
              style: const TextStyle(fontSize: 14),
            ),
          ],
        ),
      ),
    );
  }
}

void _showAuthDialog(BuildContext context, WidgetRef ref, bool isLoggedIn) {
  if (isLoggedIn) {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Account Details'),
        content: Text('Logged in as: ${ref.read(supabaseAuthProvider)?.user.email}'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('Cancel'),
          ),
          ElevatedButton(
            style: ElevatedButton.styleFrom(backgroundColor: AppTheme.primaryRed),
            onPressed: () async {
              await ref.read(supabaseAuthProvider.notifier).signOut();
              if (context.mounted) Navigator.pop(context);
            },
            child: const Text('Logout', style: TextStyle(color: Colors.white)),
          ),
        ],
      ),
    );
  } else {
    final emailController = TextEditingController();
    final passwordController = TextEditingController();

    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Login / Sign Up'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            TextField(
              controller: emailController,
              decoration: const InputDecoration(labelText: 'Email'),
            ),
            TextField(
              controller: passwordController,
              obscureText: true,
              decoration: const InputDecoration(labelText: 'Password'),
            ),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('Cancel'),
          ),
          ElevatedButton(
            style: ElevatedButton.styleFrom(backgroundColor: AppTheme.primaryRed),
            onPressed: () async {
              try {
                await ref.read(supabaseAuthProvider.notifier).signIn(
                      emailController.text.trim(),
                      passwordController.text.trim(),
                    );
                if (context.mounted) Navigator.pop(context);
              } catch (_) {
                try {
                  await ref.read(supabaseAuthProvider.notifier).signUp(
                        emailController.text.trim(),
                        passwordController.text.trim(),
                      );
                  if (context.mounted) Navigator.pop(context);
                } catch (e) {
                  if (context.mounted) {
                    ScaffoldMessenger.of(context).showSnackBar(
                      SnackBar(content: Text('Auth error: ${e.toString()}')),
                    );
                  }
                }
              }
            },
            child: const Text('Login / Register', style: TextStyle(color: Colors.white)),
          ),
        ],
      ),
    );
  }
}
