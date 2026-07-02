const axios = require('axios');
const fs = require('fs');

async function testPlayer() {
    const finalIframeUrl = "https://9animetv.be/wp-content/plugins/video-player/includes/player/n-bg/player.php?Blogger=aEJSRmxXU2RvNmRaYVdHbUg1QTVCUHNPRHM4aGRmN3hET25xV2V6RlZIcTYvY1FGenE1QjR0UGpEUE0wM1ZGamtRWm9jWHMvOE1XMTFQS3lML3ZhbHRYMG5YcmxKeTdjRmFqOEV0YWRSbVk9&feature_image=https%3A%2F%2Fi3.wp.com%2Fgogoanime.by%2Fwp-content%2Fuploads%2F2026%2F04%2Fawajima-hyakkei.webp&ref=gogoanime.by";
    
    try {
        const res = await axios.get(finalIframeUrl);
        fs.writeFileSync('final_iframe.html', res.data);
        console.log("Dumped to final_iframe.html");
    } catch(e) {
        console.error("Failed:", e.message);
    }
}
testPlayer();
