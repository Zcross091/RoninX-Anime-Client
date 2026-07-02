const puppeteer = require('puppeteer-extra');
const StealthPlugin = require('puppeteer-extra-plugin-stealth');
puppeteer.use(StealthPlugin());

async function run() {
    console.log("Launching browser to investigate 9anime.tech...");
    const browser = await puppeteer.launch({ headless: false });
    const page = await browser.newPage();
    
    try {
        await page.goto('https://9anime.tech/home', { waitUntil: 'domcontentloaded', timeout: 30000 });
        console.log("Page loaded!");
        
        await new Promise(r => setTimeout(r, 5000)); // wait for JS/Cloudflare
        
        const html = await page.content();
        const title = await page.title();
        console.log("Title:", title);
        
        // Save the HTML for analysis
        const fs = require('fs');
        fs.writeFileSync('9anime_test.html', html);
        console.log("Saved 9anime_test.html");
        
        // Find links
        const links = await page.evaluate(() => {
            return Array.from(document.querySelectorAll('a')).map(a => a.href).slice(0, 20);
        });
        console.log("Sample links:", links);
        
    } catch(e) {
        console.error("Failed:", e.message);
    }
    
    await browser.close();
}
run();
