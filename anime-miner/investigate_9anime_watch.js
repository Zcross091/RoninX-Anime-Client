const puppeteer = require('puppeteer-extra');
const StealthPlugin = require('puppeteer-extra-plugin-stealth');
puppeteer.use(StealthPlugin());

async function run() {
    console.log("Investigating 9anime.tech/watch/one-piece-81553...");
    const browser = await puppeteer.launch({ headless: false });
    const page = await browser.newPage();
    
    try {
        await page.goto('https://9anime.tech/watch/one-piece-81553?ep=14', { waitUntil: 'domcontentloaded', timeout: 30000 });
        console.log("Page loaded!");
        
        await new Promise(r => setTimeout(r, 5000));
        
        const html = await page.content();
        const fs = require('fs');
        fs.writeFileSync('9anime_watch.html', html);
        console.log("Saved 9anime_watch.html");
        
        // Find iframes
        const iframes = await page.evaluate(() => {
            return Array.from(document.querySelectorAll('iframe')).map(i => i.src);
        });
        console.log("Iframes found:", iframes);
        
    } catch(e) {
        console.error("Failed:", e.message);
    }
    
    await browser.close();
}
run();
