import 'dart:convert';
import 'dart:io';
import 'package:http/http.dart' as http;
import 'package:path_provider/path_provider.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../security/crypto_utils.dart';

class SyncManager {
  static const String manifestUrl = 'https://raw.githubusercontent.com/Zcross091/RoninX-Anime-Client/main/extensions/latest.json'; // Placeholder URL
  static const String _versionKey = 'roninx_js_bundle_version';

  /// Synchronizes the JS extension bundle from the CI/CD pipeline
  static Future<String?> syncJsBundle() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final currentVersion = prefs.getString(_versionKey);

      // 1. Fetch latest.json manifest
      final manifestRes = await http.get(Uri.parse(manifestUrl));
      if (manifestRes.statusCode != 200) {
        print('Failed to fetch manifest. Status: ${manifestRes.statusCode}');
        return _getLocalBundle();
      }

      final manifest = json.decode(manifestRes.body);
      final remoteVersion = manifest['version'] as String;
      final bundleUrl = manifest['bundleUrl'] as String;
      final signatureBase64 = manifest['signature'] as String;

      // 2. Compare versions
      if (currentVersion == remoteVersion) {
        print('JS Bundle is up-to-date (v$currentVersion).');
        return _getLocalBundle();
      }

      print('New JS Bundle found: v$remoteVersion. Downloading...');

      // 3. Download the JS bundle
      final bundleRes = await http.get(Uri.parse(bundleUrl));
      if (bundleRes.statusCode != 200) {
        print('Failed to download JS bundle.');
        return _getLocalBundle();
      }

      final jsData = bundleRes.body;

      // 4. Verify Ed25519 signature
      final isValid = await CryptoUtils.verifySignature(jsData, signatureBase64);
      if (!isValid) {
        print('SECURITY ALERT: Invalid Ed25519 signature on JS bundle. Rejecting update.');
        return _getLocalBundle();
      }

      // 5. Save the valid bundle and update version
      await _saveLocalBundle(jsData);
      await prefs.setString(_versionKey, remoteVersion);
      print('JS Bundle successfully synced to v$remoteVersion');
      
      return jsData;
    } catch (e) {
      print('SyncManager error: $e');
      return _getLocalBundle();
    }
  }

  static Future<String?> _getLocalBundle() async {
    try {
      final directory = await getApplicationDocumentsDirectory();
      final file = File('${directory.path}/roninx_master.js');
      if (await file.exists()) {
        return await file.readAsString();
      }
    } catch (e) {
      print('Failed to read local bundle: $e');
    }
    return null;
  }

  static Future<void> _saveLocalBundle(String jsData) async {
    final directory = await getApplicationDocumentsDirectory();
    final file = File('${directory.path}/roninx_master.js');
    await file.writeAsString(jsData);
  }
}
