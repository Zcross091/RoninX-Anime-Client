const { ANIME } = require('@consumet/extensions');

async function testPahe() {
    console.log("Initializing AnimePahe provider...");
    const pahe = new ANIME.AnimePahe();

    try {
        console.log("Searching for 'One Piece'...");
        const searchResults = await pahe.search("One Piece");
        console.log("Search Results:", searchResults.results[0]);

        const animeId = searchResults.results[0].id;
        console.log("\nFetching details for:", animeId);
        const info = await pahe.fetchAnimeInfo(animeId);
        
        console.log(`\nEpisodes found: ${info.episodes.length}`);
        const firstEp = info.episodes[0];
        console.log(`Episode 1 details:`, firstEp);

        console.log(`\nFetching stream links for Episode 1 from AnimePahe...`);
        const sources = await pahe.fetchEpisodeSources(firstEp.id);
        console.log("Stream links found:", sources.sources.length);
        console.log("First stream:", sources.sources[0]);
    } catch(e) {
        console.error("AnimePahe failed:", e);
    }
}

testPahe();
