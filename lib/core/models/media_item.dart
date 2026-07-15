class MediaItem {
  final String id;
  final String title;
  final String? url;
  final String? coverUrl;
  final String? synopsis;
  final List<String>? genres;
  final String? status;
  final double? score;
  final bool isManga;

  MediaItem({
    required this.id,
    required this.title,
    this.url,
    this.coverUrl,
    this.synopsis,
    this.genres,
    this.status,
    this.score,
    this.isManga = false,
  });

  factory MediaItem.fromJson(Map<String, dynamic> json) {
    return MediaItem(
      id: json['id'] as String? ?? '',
      title: json['title'] as String? ?? '',
      url: json['url'] as String?,
      coverUrl: json['coverUrl'] as String?,
      synopsis: json['synopsis'] as String?,
      genres: (json['genres'] as List<dynamic>?)?.map((e) => e as String).toList(),
      status: json['status'] as String?,
      score: json['score'] != null ? (json['score'] as num).toDouble() : null,
      isManga: json['isManga'] as bool? ?? false,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'title': title,
      'url': url,
      'coverUrl': coverUrl,
      'synopsis': synopsis,
      'genres': genres,
      'status': status,
      'score': score,
      'isManga': isManga,
    };
  }
}
