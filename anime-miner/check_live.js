const axios = require('axios');

async function checkLiveExtraction() {
    try {
        const { data: html } = await axios.get('https://gogoanime.es/awajima-hyakkei-episode-12-english-subbed/');
        
        // Find if the raw video link is embedded in the HTML script tags
        const regex = /https?:\/\/[^\s"'<>]+\.googlevideo\.com[^\s"'<>]+/gi;
        const matches = html.match(regex);
        
        if (matches) {
            console.log("Found Raw Link via Axios! Length:", matches.length);
            console.log("Sample:", matches[0]);
        } else {
            console.log("No raw link found directly in HTML. Searching for tokens or base64...");
            
            // Search for typical gogoanime player URLs
            const iframeRegex = /<iframe[^>]+src=["']([^"']+)["']/gi;
            const iframes = [...html.matchAll(iframeRegex)].map(m => m[1]);
            console.log("Found IFrames:", iframes);
        }
    } catch(e) { console.error("Axios failed:", e.message); }
}
checkLiveExtraction();
