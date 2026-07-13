const { META, ANIME } = require('@consumet/extensions');

async function testAnilistZoro() {
    console.log("Initializing META.Anilist with Hianime provider...");
    // Let's pass the Hianime provider to the Anilist constructor
    const hianime = new ANIME.Hianime();
    const anilist = new META.Anilist(hianime);

    try {
        console.log("Searching for 'One Piece'...");
        const searchResults = await anilist.search("One Piece");
        const animeId = searchResults.results[0].id;
        
        console.log("\nFetching details for AniList ID:", animeId);
        const info = await anilist.fetchAnimeInfo(animeId);
        
        const firstEp = info.episodes[0];
        console.log(`Episode 1 details:`, firstEp);

        console.log(`\nFetching stream links for Episode 1 from HiAnime...`);
        const sources = await anilist.fetchEpisodeSources(firstEp.id);
        console.log("Stream links found:", sources.sources.length);
        console.log("First stream:", sources.sources[0]);
    } catch(e) {
        console.error("Anilist with Hianime failed:", e);
    }
}

testAnilistZoro();
