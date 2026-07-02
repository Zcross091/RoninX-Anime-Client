const puppeteer = require('puppeteer-extra');
const StealthPlugin = require('puppeteer-extra-plugin-stealth');
puppeteer.use(StealthPlugin());

async function check() {
    console.log("Launching Stealth Puppeteer...");
    const browser = await puppeteer.launch({ headless: true });
    const page = await browser.newPage();
    
    let m3u8 = null;

    await page.setRequestInterception(true);
    page.on('request', req => {
        const url = req.url();
        // Console log all intercepted requests to see if we even reach the streaming iframe!
        if (url.includes('streaming.php') || url.includes('embtaku') || url.includes('goload')) {
            console.log("   --> Iframe Request:", url);
        }

        if (url.includes('.m3u8')) {
            console.log("   --> CAUGHT M3U8:", url);
            m3u8 = url;
            req.abort(); // Stop it from downloading the video
        } else if (['image', 'font'].includes(req.resourceType())) {
            req.abort(); // Only abort images and fonts to save bandwidth, let scripts run!
        } else {
            req.continue();
        }
    });

    console.log("Navigating to gogoanime3.co episode...");
    await page.goto('https://gogoanime3.co/one-piece-episode-1100', { waitUntil: 'domcontentloaded', timeout: 30000 });
    
    console.log("Waiting for video to decrypt...");
    let ticks = 0;
    while (!m3u8 && ticks < 40) {
        await new Promise(r => setTimeout(r, 250));
        ticks++;
    }

    await browser.close();

    if (m3u8) {
        console.log("✅ THE M3U8 IS ALIVE AND WORKING:", m3u8);
    } else {
        console.log("❌ Could not find m3u8 on gogoanime3.co");
    }
}
check();
