const { ANIME } = require('@consumet/extensions');

async function testAll() {
    const gogo = new ANIME.Gogoanime();
    const hianime = new ANIME.Hianime();

    console.log("--- TESTING GOGOANIME ---");
    try {
        const results = await gogo.search("Naruto");
        console.log("Gogo Search Top Result:", results.results[0]);
        const info = await gogo.fetchAnimeInfo(results.results[0].id);
        console.log(`Gogo Episodes count: ${info.episodes.length}`);
        const sources = await gogo.fetchEpisodeSources(info.episodes[0].id);
        console.log("Gogo Ep 1 Source URL:", sources.sources[0].url);
    } catch(e) {
        console.error("Gogo failed:", e.message);
    }

    console.log("\n--- TESTING HIANIME ---");
    try {
        const results = await hianime.search("Naruto");
        console.log("HiAnime Search Top Result:", results.results[0]);
        const info = await hianime.fetchAnimeInfo(results.results[0].id);
        console.log(`HiAnime Episodes count: ${info.episodes.length}`);
        const sources = await hianime.fetchEpisodeSources(info.episodes[0].id);
        console.log("HiAnime Ep 1 Source URL:", sources.sources[0].url);
    } catch(e) {
        console.error("HiAnime failed:", e.message);
    }
}

testAll();
