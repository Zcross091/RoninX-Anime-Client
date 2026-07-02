const axios = require('axios');
const cheerio = require('cheerio');

async function testPlayer() {
    // These are the exact parameters found in the gogoanime.es HTML for Awajima Hyakkei Ep 12
    const data = {
        Blogger: "aEJSRmxXU2RvNmRaYVdHbUg1QTVCUHNPRHM4aGRmN3hET25xV2V6RlZIcTYvY1FGenE1QjR0UGpEUE0wM1ZGamtRWm9jWHMvOE1XMTFQS3lML3ZhbHRYMG5YcmxKeTdjRmFqOEV0YWRSbVk9",
        url2: "aEtKUFNZNUdLeGNqTlhWeThhdjNLdz09",
        url3: "aEtKUFNZNUdLeGNqTlhWeThhdjNLdz09",
        feature_image: "https://i3.wp.com/gogoanime.by/wp-content/uploads/2026/04/awajima-hyakkei.webp",
        user_agent: "axios/1.18.1",
        ref: "gogoanime.by",
        postId: "20354"
    };

    const qs = new URLSearchParams(data).toString();
    const iframeUrl = `https://9animetv.be/wp-content/plugins/video-player/includes/player/player.php?${qs}`;
    
    console.log("Constructed Iframe URL:", iframeUrl);
    
    try {
        const res = await axios.get(iframeUrl);
        console.log("Iframe HTML Status:", res.status);
        console.log("Iframe HTML Snippet:", res.data.substring(0, 300));
        
        // Let's see if the HTML contains the raw googlevideo link
        const regex = /https?:\/\/[^\s"'<>]+\.googlevideo\.com[^\s"'<>]+/gi;
        const matches = res.data.match(regex);
        if (matches) {
            console.log("✅ LIVE EXTRACTION SUCCESSFUL! Found raw video link in the iframe!");
        } else {
            console.log("No raw link found. The iframe might be doing more AJAX.");
        }
    } catch(e) {
        console.error("Failed:", e.message);
    }
}
testPlayer();
