class StreamSource {
  final String url;
  final String quality; // e.g., '1080p', '720p', 'default'
  final bool isM3U8;
  final Map<String, String>? headers;

  StreamSource({
    required this.url,
    required this.quality,
    this.isM3U8 = false,
    this.headers,
  });

  factory StreamSource.fromJson(Map<String, dynamic> json) {
    return StreamSource(
      url: json['url'] as String? ?? '',
      quality: json['quality'] as String? ?? 'default',
      isM3U8: json['isM3U8'] as bool? ?? false,
      headers: (json['headers'] as Map<String, dynamic>?)?.map(
        (key, value) => MapEntry(key, value.toString()),
      ),
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'url': url,
      'quality': quality,
      'isM3U8': isM3U8,
      'headers': headers,
    };
  }
}

class SubtitleTrack {
  final String url;
  final String language; // e.g., 'English', 'Spanish'
  final String? format; // 'ass', 'srt', 'vtt'

  SubtitleTrack({
    required this.url,
    required this.language,
    this.format,
  });

  factory SubtitleTrack.fromJson(Map<String, dynamic> json) {
    return SubtitleTrack(
      url: json['url'] as String? ?? '',
      language: json['language'] as String? ?? 'Unknown',
      format: json['format'] as String?,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'url': url,
      'language': language,
      'format': format,
    };
  }
}

class VideoExtraction {
  final List<StreamSource> sources;
  final List<SubtitleTrack> subtitles;

  VideoExtraction({
    required this.sources,
    this.subtitles = const [],
  });

  factory VideoExtraction.fromJson(Map<String, dynamic> json) {
    return VideoExtraction(
      sources: (json['sources'] as List<dynamic>?)
              ?.map((e) => StreamSource.fromJson(e as Map<String, dynamic>))
              .toList() ??
          [],
      subtitles: (json['subtitles'] as List<dynamic>?)
              ?.map((e) => SubtitleTrack.fromJson(e as Map<String, dynamic>))
              .toList() ??
          [],
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'sources': sources.map((e) => e.toJson()).toList(),
      'subtitles': subtitles.map((e) => e.toJson()).toList(),
    };
  }
}
