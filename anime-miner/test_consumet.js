const { ANIME } = require('@consumet/extensions');

async function testConsumet() {
    console.log("Initializing Hianime provider...");
    const hianime = new ANIME.Hianime();
    
    try {
        console.log("Searching for One Piece...");
        const searchResults = await hianime.search("One Piece");
        console.log("Search Results:", searchResults.results[0]);
        
        const animeId = searchResults.results[0].id;
        console.log("\nFetching Anime Info for:", animeId);
        const animeInfo = await hianime.fetchAnimeInfo(animeId);
        
        const firstEp = animeInfo.episodes[0];
        console.log(`\nFetching Stream Links for Ep ${firstEp.number} (${firstEp.id})...`);
        const streams = await hianime.fetchEpisodeSources(firstEp.id);
        
        console.log("\nStreams found:", streams.sources.length);
        const defaultStream = streams.sources.find(s => s.isM3U8) || streams.sources[0];
        console.log("Default Stream URL:", defaultStream.url);
        
    } catch(e) {
        console.error("Consumet test failed:", e);
    }
}
testConsumet();
