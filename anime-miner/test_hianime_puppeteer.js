const puppeteer = require('puppeteer-extra');
const StealthPlugin = require('puppeteer-extra-plugin-stealth');
puppeteer.use(StealthPlugin());

async function testHiAnime() {
    console.log("Launching browser...");
    const browser = await puppeteer.launch({
        headless: true,
        args: ['--no-sandbox', '--disable-setuid-sandbox']
    });

    const page = await browser.newPage();
    const query = "Naruto";
    const url = `https://hianime.to/search?keyword=${encodeURIComponent(query)}`;

    console.log(`Navigating to ${url}...`);
    try {
        await page.goto(url, { waitUntil: 'domcontentloaded', timeout: 30000 });
        
        console.log("Extracting search results...");
        const results = await page.evaluate(() => {
            const items = Array.from(document.querySelectorAll('.film_list-wrap .flw-item'));
            return items.map(item => {
                const titleEl = item.querySelector('.film-name a');
                const href = titleEl ? titleEl.href : null;
                const title = titleEl ? titleEl.innerText : null;
                return { title, href };
            });
        });

        console.log("Results found:", results.slice(0, 3));

        if (results.length > 0) {
            const firstAnime = results[0];
            console.log(`Navigating to watch page of: ${firstAnime.title} (${firstAnime.href})`);
            
            // hianime urls look like https://hianime.to/naruto-637
            // We need to convert it to a watch URL: https://hianime.to/watch/naruto-637
            const watchUrl = firstAnime.href.replace('hianime.to/', 'hianime.to/watch/');
            console.log(`Watch URL: ${watchUrl}`);

            await page.goto(watchUrl, { waitUntil: 'domcontentloaded', timeout: 30000 });
            
            // Wait for player container to load
            await page.waitForSelector('#iframe-embed', { timeout: 10000 }).catch(() => {});
            
            const iframeSrc = await page.evaluate(() => {
                const iframe = document.querySelector('#iframe-embed');
                return iframe ? iframe.src : null;
            });

            console.log("Iframe Source URL:", iframeSrc);
        }
    } catch(e) {
        console.error("HiAnime Puppeteer failed:", e.message);
    } finally {
        await browser.close();
        console.log("Browser closed.");
    }
}

testHiAnime();
