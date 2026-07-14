import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

class AppTheme {
  static const Color primaryRed = Color(0xFFC4202C);
  static const Color background = Color(0xFF000000);
  static const Color surface = Color(0xFF0F0F0F);
  static const Color surfaceLight = Color(0xFF141414);

  static ThemeData get darkTheme {
    return ThemeData(
      useMaterial3: true,
      brightness: Brightness.dark,
      scaffoldBackgroundColor: background,
      primaryColor: primaryRed,
      colorScheme: const ColorScheme.dark(
        primary: primaryRed,
        onPrimary: Colors.white,
        surface: surface,
        onSurface: Colors.white,
        background: background,
        onBackground: Colors.white,
      ),
      textTheme: GoogleFonts.poppinsTextTheme(ThemeData.dark().textTheme).apply(
        bodyColor: Colors.white,
        displayColor: Colors.white,
      ),
      cardTheme: CardThemeData(
        color: surface,
        elevation: 0,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      ),
      appBarTheme: const AppBarTheme(
        backgroundColor: background,
        elevation: 0,
        centerTitle: true,
      ),
    );
  }
}
