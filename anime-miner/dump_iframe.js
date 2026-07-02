const axios = require('axios');
const fs = require('fs');

async function testPlayer() {
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
    
    try {
        const res = await axios.get(iframeUrl);
        fs.writeFileSync('iframe.html', res.data);
        console.log("Dumped to iframe.html");
    } catch(e) {
        console.error("Failed:", e.message);
    }
}
testPlayer();
