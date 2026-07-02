const axios = require('axios');
const fs = require('fs');

async function dump() {
    try {
        const { data } = await axios.get('https://gogoanime.es/awajima-hyakkei-episode-12-english-subbed/');
        fs.writeFileSync('gogoanime.html', data);
        console.log("Dumped to gogoanime.html");
    } catch(e) {
        console.error(e.message);
    }
}
dump();
