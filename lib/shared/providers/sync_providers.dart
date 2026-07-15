import 'dart:convert';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:supabase_flutter/supabase_flutter.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../models/unified_models.dart';
import '../../core/config/supabase_state.dart';

// --- Supabase Auth Provider ---
final supabaseAuthProvider = StateNotifierProvider<AuthNotifier, Session?>((ref) {
  return AuthNotifier();
});

class AuthNotifier extends StateNotifier<Session?> {
  AuthNotifier() : super(SupabaseState.isInitialized ? Supabase.instance.client.auth.currentSession : null) {
    if (SupabaseState.isInitialized) {
      Supabase.instance.client.auth.onAuthStateChange.listen((data) {
        state = data.session;
      });
    }
  }

  Future<void> signIn(String email, String password) async {
    if (!SupabaseState.isInitialized) return;
    await Supabase.instance.client.auth.signInWithPassword(email: email, password: password);
  }

  Future<void> signUp(String email, String password) async {
    if (!SupabaseState.isInitialized) return;
    await Supabase.instance.client.auth.signUp(email: email, password: password);
  }

  Future<void> signOut() async {
    if (!SupabaseState.isInitialized) return;
    await Supabase.instance.client.auth.signOut();
  }
}

// --- Watch List Provider ---
final watchListProvider = StateNotifierProvider<WatchListNotifier, List<Map<String, dynamic>>>((ref) {
  return WatchListNotifier(ref);
});

class WatchListNotifier extends StateNotifier<List<Map<String, dynamic>>> {
  final Ref ref;
  WatchListNotifier(this.ref) : super([]) {
    _loadWatchList();
  }

  Future<void> _loadWatchList() async {
    final session = ref.read(supabaseAuthProvider);
    if (SupabaseState.isInitialized && session != null) {
      final response = await Supabase.instance.client
          .from('user_watch_history')
          .select()
          .eq('user_id', session.user.id);
      state = List<Map<String, dynamic>>.from(response);
    } else {
      final prefs = await SharedPreferences.getInstance();
      final localData = prefs.getString('local_watchlist') ?? '[]';
      state = List<Map<String, dynamic>>.from(json.decode(localData));
    }
  }

  Future<void> toggleWatchList(Map<String, dynamic> item) async {
    final session = ref.read(supabaseAuthProvider);
    final exists = state.any((e) => e['media_id'] == item['media_id']);

    if (SupabaseState.isInitialized && session != null) {
      if (exists) {
        await Supabase.instance.client
            .from('user_watch_history')
            .delete()
            .eq('user_id', session.user.id)
            .eq('media_id', item['media_id']);
      } else {
        await Supabase.instance.client
            .from('user_watch_history')
            .insert({
              ...item,
              'user_id': session.user.id,
              'status': 'watching',
            });
      }
      _loadWatchList();
    } else {
      final prefs = await SharedPreferences.getInstance();
      if (exists) {
        state = state.where((e) => e['media_id'] != item['media_id']).toList();
      } else {
        state = [...state, item];
      }
      prefs.setString('local_watchlist', json.encode(state));
    }
  }
}

// --- Watch History Provider (Playback Progress) ---
final watchHistoryProvider = StateNotifierProvider<WatchHistoryNotifier, Map<String, dynamic>>((ref) {
  return WatchHistoryNotifier(ref);
});

class WatchHistoryNotifier extends StateNotifier<Map<String, dynamic>> {
  final Ref ref;
  WatchHistoryNotifier(this.ref) : super({});

  Future<void> updateProgress({
    required String mediaId,
    required double episodeNumber,
    required Duration position,
    required Duration duration,
  }) async {
    final session = ref.read(supabaseAuthProvider);
    final progress = duration.inSeconds > 0 ? (position.inSeconds / duration.inSeconds) * 100 : 0.0;

    final data = {
      'media_id': mediaId,
      'episode_number': episodeNumber,
      'position': position.inSeconds,
      'duration': duration.inSeconds,
      'progress': progress,
      'last_watched': DateTime.now().toIso8601String(),
    };

    if (SupabaseState.isInitialized && session != null) {
      await Supabase.instance.client
          .from('user_watch_history')
          .upsert({
            ...data,
            'user_id': session.user.id,
          });
    } else {
      final prefs = await SharedPreferences.getInstance();
      final history = Map<String, dynamic>.from(json.decode(prefs.getString('local_history') ?? '{}'));
      history[mediaId] = data;
      prefs.setString('local_history', json.encode(history));
    }
    state = {...state, mediaId: data};
  }
}
