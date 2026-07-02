const puppeteer = require('puppeteer-extra');
const StealthPlugin = require('puppeteer-extra-plugin-stealth');
puppeteer.use(StealthPlugin());

async function run() {
    const browser = await puppeteer.launch({ headless: false });
    const page = await browser.newPage();
    await page.goto('https://gogoanime.es/one-piece-episode-14', { waitUntil: 'domcontentloaded' });
    
    // Wait for the servers to load
    await new Promise(r => setTimeout(r, 3000));
    
    const types = await page.evaluate(() => {
        return Array.from(document.querySelectorAll('li[data-type]')).map(li => li.getAttribute('data-type'));
    });
    
    console.log("AVAILABLE SERVERS:", types);
    await browser.close();
}
run();
