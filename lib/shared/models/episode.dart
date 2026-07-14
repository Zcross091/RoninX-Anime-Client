class Episode {
  final int number;
  final String? title;
  final String? filler;
  final String? recap;

  Episode({
    required this.number,
    this.title,
    this.filler,
    this.recap,
  });

  factory Episode.fromJson(Map<String, dynamic> json) {
    return Episode(
      number: json['mal_id'] ?? 0,
      title: json['title'],
      filler: json['filler'].toString(),
      recap: json['recap'].toString(),
    );
  }
}
