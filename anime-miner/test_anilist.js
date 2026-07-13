const { META } = require('@consumet/extensions');

async function testAnilist() {
    console.log("Initializing META.Anilist...");
    const anilist = new META.Anilist();

    try {
        console.log("Searching for 'One Piece'...");
        const searchResults = await anilist.search("One Piece");
        console.log("Search Results:", searchResults.results[0]);

        const animeId = searchResults.results[0].id;
        console.log("\nFetching details for AniList ID:", animeId);
        const info = await anilist.fetchAnimeInfo(animeId);
        
        console.log(`\nEpisodes found: ${info.episodes.length}`);
        const firstEp = info.episodes[0];
        console.log(`Episode 1 details:`, firstEp);

        console.log(`\nFetching stream links for Episode 1...`);
        const sources = await anilist.fetchEpisodeSources(firstEp.id);
        console.log("Stream links found:", sources.sources.length);
        console.log("First stream:", sources.sources[0]);
    } catch(e) {
        console.error("Anilist Meta provider failed:", e);
    }
}

testAnilist();
