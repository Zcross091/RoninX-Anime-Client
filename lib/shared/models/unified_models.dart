class UnifiedMedia {
  final String id;
  final Map<String, String> title; // english, romaji, native
  final String? image;
  final int episodesCount;
  final double score;
  final String? synopsis;
  final List<String> genres;

  UnifiedMedia({
    required this.id,
    required this.title,
    this.image,
    required this.episodesCount,
    required this.score,
    this.synopsis,
    required this.genres,
  });

  factory UnifiedMedia.fromJikan(Map<String, dynamic> json) {
    return UnifiedMedia(
      id: json['mal_id'].toString(),
      title: {
        'english': json['title_english'] ?? json['title'] ?? 'Unknown',
        'romaji': json['title'] ?? 'Unknown',
        'native': json['title_japanese'] ?? 'Unknown',
      },
      image: json['images']?['jpg']?['large_image_url'],
      episodesCount: json['episodes'] ?? 0,
      score: (json['score'] as num?)?.toDouble() ?? 0.0,
      synopsis: json['synopsis'],
      genres: (json['genres'] as List?)?.map((e) => e['name'] as String).toList() ?? [],
    );
  }
}

class UnifiedEpisode {
  final String id; // title|number
  final String title;
  final double number;

  UnifiedEpisode({
    required this.id,
    required this.title,
    required this.number,
  });
}

class VideoServer {
  final String id;
  final String name;

  VideoServer({
    required this.id,
    required this.name,
  });
}

class VideoStream {
  final String url;
  final String quality;
  final Map<String, String> headers;

  VideoStream({
    required this.url,
    required this.quality,
    required this.headers,
  });
}
