const axios = require('axios');

async function checkApi() {
    try {
        const res = await axios.get('https://api-consumet.vercel.app/anime/gogoanime/one-piece');
        console.log("Consumet Vercel Works! Episodes:", res.data.episodes.length);
        const epId = res.data.episodes[0].id;
        const watchRes = await axios.get(`https://api-consumet.vercel.app/anime/gogoanime/watch/${epId}`);
        console.log("Stream URL:", watchRes.data.sources[0].url);
    } catch(e) { console.log("Consumet Vercel Failed:", e.message); }
}
checkApi();
