require('dotenv').config();
const puppeteer = require('puppeteer-extra');
const StealthPlugin = require('puppeteer-extra-plugin-stealth');
const AdblockerPlugin = require('puppeteer-extra-plugin-adblocker');
const { createClient } = require('@supabase/supabase-js');

process.on('unhandledRejection', (reason, promise) => {
    if (reason && reason.name === 'TargetCloseError') return;
    if (reason && reason.message && reason.message.includes('Target closed')) return;
});

puppeteer.use(StealthPlugin());
puppeteer.use(AdblockerPlugin({ blockTrackers: true }));

const supabase = createClient(process.env.SUPABASE_URL, process.env.SUPABASE_KEY);

const BASE_URL = process.env.NINEANIME_URL; // https://9anime.tech

async function extractIframe(page, url) {
    let retries = 3;
    while(retries > 0) {
        try {
            await page.goto(url, { waitUntil: 'domcontentloaded', timeout: 30000 });
            await new Promise(r => setTimeout(r, 4000)); // Wait for JS/Iframes to load
            
            const iframeSrc = await page.evaluate(() => {
                const iframes = Array.from(document.querySelectorAll('iframe'));
                for (const frame of iframes) {
                    if (frame.src && !frame.src.includes('sharethis')) {
                        return frame.src;
                    }
                }
                return null;
            });
            
            return iframeSrc;
        } catch(e) {
            retries--;
            console.log(`      ⚠️ Retry extracting iframe (${retries} left)...`);
        }
    }
    return null;
}

async function mineNineAnime() {
    console.log(`🚀 Starting 9ANIME Iframe Miner targeting ${BASE_URL}...`);
    const browser = await puppeteer.launch({ headless: false });
    
    // For MVP, we will mine the recently added episodes on the homepage
    // In the future, this can be expanded to paginate through /browse/tv
    const page = await browser.newPage();
    
    try {
        await page.goto(`${BASE_URL}/home`, { waitUntil: 'domcontentloaded', timeout: 30000 });
        console.log(`   Scouting Timeline: ${BASE_URL}/home`);
        await new Promise(r => setTimeout(r, 3000));
        
        // Find all episode links on the homepage
        const episodeLinks = await page.evaluate(() => {
            return Array.from(document.querySelectorAll('a'))
                .map(a => a.href)
                .filter(href => href.includes('/watch/'))
                .filter((v, i, a) => a.indexOf(v) === i); // unique
        });
        
        console.log(`   Found ${episodeLinks.length} episodes directly on the timeline...`);
        
        for (const epLink of episodeLinks) {
            console.log(`   🎬 Mining episode: ${epLink}`);
            const epPage = await browser.newPage();
            
            const iframeSrc = await extractIframe(epPage, epLink);
            
            if (iframeSrc) {
                console.log(`      ✅ Secured Iframe: ${iframeSrc}`);
                
                // Parse Title and Ep Num from URL (e.g. /watch/one-piece-81553?ep=14 or just /watch/one-piece-81553)
                // This is a naive parse, can be improved.
                let titleSlug = epLink.split('/watch/')[1].split('?')[0];
                titleSlug = titleSlug.replace(/-\d+$/, ''); // Remove ID like -81553
                const title = titleSlug.replace(/-/g, ' ');
                
                const epMatch = epLink.match(/ep=(\d+)/);
                const epNum = epMatch ? parseInt(epMatch[1]) : 1;
                
                // Save to DB
                const { error } = await supabase.from('anime_links').upsert(
                    {
                        title: title,
                        episode: epNum,
                        type: 'sub',
                        url: iframeSrc
                    },
                    { onConflict: 'title, episode, type' }
                );
                
                if (error) console.log(`      ❌ DB Save Failed: ${error.message}`);
                else console.log(`      💾 Saved to Database!`);
            } else {
                console.log(`      ❌ Failed to find iframe.`);
            }
            
            try { await epPage.close(); } catch(e){}
        }
        
    } catch(e) {
        console.log("Failed to scrape timeline:", e.message);
    }
    
    console.log("🎉 9ANIME MINING COMPLETE!");
    await browser.close();
}

mineNineAnime();
