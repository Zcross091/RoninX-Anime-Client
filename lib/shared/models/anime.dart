class Anime {
  final String id;
  final String title;
  final String? poster;
  final String? description;
  final String? type;
  final String? status;
  final List<String>? genres;

  Anime({
    required this.id,
    required this.title,
    this.poster,
    this.description,
    this.type,
    this.status,
    this.genres,
  });

  factory Anime.fromJson(Map<String, dynamic> json) {
    // Jikan API mapping
    return Anime(
      id: json['mal_id'].toString(),
      title: json['title'] ?? 'Unknown',
      poster: json['images']?['jpg']?['large_image_url'],
      description: json['synopsis'],
      type: json['type'],
      status: json['status'],
      genres: (json['genres'] as List?)?.map((e) => e['name'] as String).toList(),
    );
  }
}
