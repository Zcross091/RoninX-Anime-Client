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
    final attrs = json['attributes'] as Map<String, dynamic>? ?? {};
    final titles = attrs['titles'] as Map<String, dynamic>? ?? {};
    
    return Anime(
      id: json['id'].toString(),
      title: titles['en'] ?? titles['en_jp'] ?? attrs['canonicalTitle'] ?? 'Unknown',
      poster: attrs['posterImage']?['large'] ?? attrs['posterImage']?['original'],
      description: attrs['synopsis'],
      type: attrs['subtype'],
      status: attrs['status'],
      genres: [], // Kitsu uses relationships for genres, we can leave it empty or map later
    );
  }
}
