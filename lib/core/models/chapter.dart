class Chapter {
  final String id;
  final String mediaId;
  final String? title;
  final double number;
  final String? url;
  final DateTime? releaseDate;

  Chapter({
    required this.id,
    required this.mediaId,
    this.title,
    required this.number,
    this.url,
    this.releaseDate,
  });

  factory Chapter.fromJson(Map<String, dynamic> json) {
    return Chapter(
      id: json['id'] as String? ?? '',
      mediaId: json['mediaId'] as String? ?? '',
      title: json['title'] as String?,
      number: (json['number'] as num?)?.toDouble() ?? 0.0,
      url: json['url'] as String?,
      releaseDate: json['releaseDate'] != null ? DateTime.tryParse(json['releaseDate']) : null,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'mediaId': mediaId,
      'title': title,
      'number': number,
      'url': url,
      'releaseDate': releaseDate?.toIso8601String(),
    };
  }
}
