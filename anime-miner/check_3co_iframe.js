const puppeteer = require('puppeteer-extra');
const StealthPlugin = require('puppeteer-extra-plugin-stealth');
puppeteer.use(StealthPlugin());

async function check() {
    console.log("Launching Stealth Puppeteer for gogoanime3...");
    const browser = await puppeteer.launch({ headless: true });
    const page = await browser.newPage();
    
    console.log("Navigating to episode...");
    await page.goto('https://gogoanime3.co/one-piece-episode-1100', { waitUntil: 'domcontentloaded', timeout: 30000 });
    
    console.log("Handling GDPR/Cloudflare...");
    await new Promise(r => setTimeout(r, 2000)); // wait for GDPR
    await page.evaluate(() => {
        const btns = document.querySelectorAll('button, p, span, a');
        for (let b of btns) {
            if (b.innerText && (b.innerText.includes('Consent') || b.innerText.includes('Accept'))) {
                b.click();
            }
        }
    });
    
    await new Promise(r => setTimeout(r, 3000)); // wait for video to load

    const iframeSrc = await page.evaluate(() => {
        const iframe = document.querySelector('.play-video iframe');
        return iframe ? iframe.src : null;
    });

    console.log("Iframe Src after GDPR:", iframeSrc);
    await browser.close();
}
check();
