import 'dart:convert';
import 'package:flutter_js/flutter_js.dart';
import 'package:http/http.dart' as http;
import 'sync_manager.dart';

class JSEngine {
  static final JSEngine _instance = JSEngine._internal();
  factory JSEngine() => _instance;

  JSEngine._internal();

  JavascriptRuntime? _engine;
  bool _isInitialized = false;

  /// Initializes the JS Sandbox with the latest bundle
  Future<void> initialize() async {
    if (_isInitialized) return;

    // 1. Sync & Load the latest JS bundle
    final jsBundle = await SyncManager.syncJsBundle();
    if (jsBundle == null) {
      print('Warning: No JS bundle available to initialize engine.');
      return;
    }

    // 2. Instantiate flutter_js runtime
    _engine = getJavascriptRuntime();

    // 3. Expose the Bridge: JS -> Dart network interception
    _engine!.onMessage('fetchHtml', (dynamic args) async {
      try {
        final url = args as String;
        return await _fetchHtml(url);
      } catch (e) {
        return {"success": false, "error": e.toString()};
      }
    });

    _engine!.onMessage('flareFetch', (dynamic args) async {
      try {
        final url = args as String;
        return await _flareFetch(url);
      } catch (e) {
        return {"success": false, "error": e.toString()};
      }
    });

    // 4. Evaluate the JS bundle
    try {
      _engine!.evaluate(jsBundle);
      _isInitialized = true;
      print('JS Engine successfully initialized with flutter_js.');
    } catch (e) {
      print('Failed to evaluate JS bundle: $e');
    }
  }

  /// Executes an extension action defined in the JS bundle
  /// e.g. executeAction("anime", "HiAnime", "search", {"query": "Naruto"})
  Future<dynamic> executeAction(String category, String sourceId, String action, Map<String, dynamic> params) async {
    if (!_isInitialized) {
      await initialize();
    }
    
    if (_engine == null) throw Exception('JS Engine failed to initialize.');

    try {
      final jsonParams = jsonEncode(params);
      final script = '''
        // The master bundle should expose a global RoninX or similar router
        RoninX.route("$category", "$sourceId", "$action", $jsonParams);
      ''';
      
      print("🟡 Requesting video extraction/action: category=$category, source=$sourceId, action=$action");
      final JsEvalResult result = _engine!.evaluate(script);
      print("🟢 JS Engine Returned: ${result.stringResult}");
      
      // flutter_js returns a JsEvalResult. We can parse stringResult as JSON
      return jsonDecode(result.stringResult);
    } catch (e) {
      print('🔴 Error executing JS action $action on $sourceId: $e');
      rethrow;
    }
  }

  Future<String> _fetchHtml(String url) async {
    // Pure native Dart HTTP request. JS engine has NO direct network access.
    try {
      print("🟡 Bridge fetchHtml requesting: $url");
      final response = await http.get(Uri.parse(url), headers: {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
        'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8',
      }).timeout(const Duration(seconds: 10));
      return response.body;
    } catch (e) {
      print('🔴 Bridge fetchHtml failed: $e');
      throw Exception('fetchHtml failed: $e');
    }
  }

  Future<String> _flareFetch(String url) async {
    // Route through Phase 3: Anti-Bot Proxy Server
    try {
      final proxyUrl = Uri.parse('http://127.0.0.1:8191/v1'); // Flaresolverr endpoint
      final payload = {
        "cmd": "request.get",
        "url": url,
        "maxTimeout": 60000
      };
      
      final response = await http.post(
        proxyUrl,
        headers: {
          'Content-Type': 'application/json',
          'X-Ronin-Proxy-Key': 'SECRET_KEY_PLACEHOLDER'
        },
        body: jsonEncode(payload),
      ).timeout(const Duration(seconds: 65));

      final jsonRes = jsonDecode(response.body);
      if (jsonRes['solution'] != null) {
        return jsonRes['solution']['response'] as String;
      }
      throw Exception('No solution from FlareSolverr');
    } catch (e) {
      print('Bridge flareFetch failed: $e');
      throw Exception('flareFetch failed: $e');
    }
  }

  void dispose() {
    _engine?.dispose();
    _isInitialized = false;
  }
}
