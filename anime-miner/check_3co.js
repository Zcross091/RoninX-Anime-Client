const axios = require('axios');

async function check() {
    try {
        const { data } = await axios.get('https://gogoanime3.co/');
        console.log("gogoanime3.co HTML snippet:", data.substring(0, 300));
    } catch(e) {
        console.log("gogoanime3.co is DEAD:", e.message);
    }
}
check();
