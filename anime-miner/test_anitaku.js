const puppeteer = require('puppeteer-extra');
const StealthPlugin = require('puppeteer-extra-plugin-stealth');
puppeteer.use(StealthPlugin());

async function run() {
    console.log("Checking the REAL Gogoanime (anitaku.to) for One Piece Ep 14...");
    const browser = await puppeteer.launch({ headless: false });
    const page = await browser.newPage();
    
    try {
        await page.goto('https://anitaku.to/one-piece-episode-14', { waitUntil: 'domcontentloaded', timeout: 30000 });
        console.log("Page loaded!");
        
        await new Promise(r => setTimeout(r, 3000));
        
        const servers = await page.evaluate(() => {
            return Array.from(document.querySelectorAll('.anime_muti_link ul li')).map(li => li.getAttribute('class'));
        });
        console.log("Servers found:", servers);
        
        const iframe = await page.evaluate(() => {
            return document.querySelector('div.play-video iframe')?.src;
        });
        console.log("Main iframe:", iframe);
        
    } catch(e) {
        console.error("Failed:", e.message);
    }
    
    await browser.close();
}
run();
