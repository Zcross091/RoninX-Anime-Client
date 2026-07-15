class MangaPage {
  final int index;
  final String url;
  final Map<String, String>? headers;

  MangaPage({
    required this.index,
    required this.url,
    this.headers,
  });

  factory MangaPage.fromJson(Map<String, dynamic> json) {
    return MangaPage(
      index: json['index'] as int? ?? 0,
      url: json['url'] as String? ?? '',
      headers: (json['headers'] as Map<String, dynamic>?)?.map(
        (key, value) => MapEntry(key, value.toString()),
      ),
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'index': index,
      'url': url,
      'headers': headers,
    };
  }
}
