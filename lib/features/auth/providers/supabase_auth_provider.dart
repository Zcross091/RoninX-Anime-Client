import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:supabase_flutter/supabase_flutter.dart';

final supabaseAuthProvider = NotifierProvider<SupabaseAuthNotifier, User?>(
  SupabaseAuthNotifier.new,
);

class SupabaseAuthNotifier extends Notifier<User?> {
  @override
  User? build() {
    Supabase.instance.client.auth.onAuthStateChange.listen((data) {
      state = data.session?.user;
    });
    return Supabase.instance.client.auth.currentUser;
  }

  Future<void> login(String email, String password) async {
    await Supabase.instance.client.auth.signInWithPassword(
      email: email,
      password: password,
    );
  }

  Future<void> register(String email, String password) async {
    await Supabase.instance.client.auth.signUp(
      email: email,
      password: password,
    );
  }

  Future<void> logout() async {
    await Supabase.instance.client.auth.signOut();
  }
}
