require('dotenv').config();
const puppeteer = require('puppeteer-extra');
const StealthPlugin = require('puppeteer-extra-plugin-stealth');
const AdblockerPlugin = require('puppeteer-extra-plugin-adblocker');
const { createClient } = require('@supabase/supabase-js');
const fs = require('fs');

process.on('unhandledRejection', (reason, promise) => {
    if (reason && reason.name === 'TargetCloseError') {
        // Ignore Puppeteer internal TargetCloseErrors caused by rapidly closing tabs
    } else if (reason && reason.message && reason.message.includes('Target closed')) {
        // Ignore
    } else {
        console.error('Unhandled Rejection at:', promise, 'reason:', reason);
    }
});

puppeteer.use(StealthPlugin());
puppeteer.use(AdblockerPlugin({ blockTrackers: true }));

// Supabase Connection
const supabaseUrl = process.env.SUPABASE_URL;
const supabaseKey = process.env.SUPABASE_KEY;
const supabase = createClient(supabaseUrl, supabaseKey);

const BASE_URL = "https://gogoanime.es";
const VISITED_FILE = './visitedSeries.json';

// Load visited series from disk so we can stop and resume safely
let visitedSeries = [];
if (fs.existsSync(VISITED_FILE)) {
    try { visitedSeries = JSON.parse(fs.readFileSync(VISITED_FILE, 'utf8')); } catch(e){}
}

function saveVisited(seriesUrl) {
    if (!visitedSeries.includes(seriesUrl)) {
        visitedSeries.push(seriesUrl);
        fs.writeFileSync(VISITED_FILE, JSON.stringify(visitedSeries, null, 2));
    }
}

async function runMiner() {
    console.log("🚀 Starting OFFENSIVE Deep-Space Anime Miner...");
    
    const browser = await puppeteer.launch({
        headless: false,
        args: [
            '--no-sandbox',
            '--disable-setuid-sandbox',
            '--disable-dev-shm-usage',
            '--disable-accelerated-2d-canvas',
            '--disable-gpu'
        ]
    });

    let currentPage = 1;

    while (true) {
        console.log(`\n======================================`);
        console.log(`📑 Tracking Backwards in Time: Page ${currentPage}`);
        console.log(`======================================\n`);

        const page = await browser.newPage();
        const pageUrl = `${process.env.GOGOANIME_URL}/page/${currentPage}/`;
        
        console.log(`   Scouting Timeline: ${pageUrl}`);
        try {
            if (currentPage === 1) {
                await page.goto(process.env.GOGOANIME_URL, { waitUntil: 'domcontentloaded', timeout: 30000 });
            } else {
                await page.goto(pageUrl, { waitUntil: 'domcontentloaded', timeout: 30000 });
            }
        } catch (e) {
            console.log("   ⚠️ Reached end of time or timed out.");
            await page.close();
            break;
        }

        // Extract series links from the homepage feed
        const seriesLinks = await page.evaluate(() => {
            return Array.from(document.querySelectorAll('a'))
                .map(a => a.href)
                .filter(h => h && h.includes('/series/') && !h.includes('?type=Movie'));
        });

        // Filter for unique URLs only
        const uniqueSeries = [...new Set(seriesLinks)];

        await page.close();

        if (uniqueSeries.length === 0) {
            console.log(`   🛑 No series found on page ${currentPage}. End of the line.`);
            break;
        }

        console.log(`   Found ${uniqueSeries.length} master series directly on the timeline...`);

        // Mine each Master Series
        for (let f = 0; f < uniqueSeries.length; f++) {
            const masterSeriesUrl = uniqueSeries[f];

            if (visitedSeries.includes(masterSeriesUrl)) {
                console.log(`   ✅ Master Series ${masterSeriesUrl} already mined! Skipping.`);
                continue;
            }

            console.log(`   🔥 NEW ANIME DETECTED: ${masterSeriesUrl}`);
            console.log(`   ⚔️ Initiating Offensive Deep Mine...`);

            const seriesPage = await browser.newPage();
            let episodeLinks = [];
            
            try {
                await seriesPage.goto(masterSeriesUrl, { waitUntil: 'domcontentloaded', timeout: 30000 });
                episodeLinks = await seriesPage.evaluate(() => {
                    // Extract all episode links on the series page
                    // We target the main body to avoid sidebar recent releases
                    const body = document.querySelector('.anime_info_body, .anime_video_body, #episode_page, .main_body') || document;
                    const links = Array.from(body.querySelectorAll('a'))
                        .map(a => a.href)
                        .filter(h => h && h.includes('-episode-') && !h.includes('/category/') && !h.includes('/series/'));
                    return [...new Set(links)];
                });
            } catch(e) {
                console.log(`   ⚠️ Failed to load Master Series page.`);
            }
            
            await seriesPage.close();

            if (episodeLinks.length === 0) {
                console.log("   ❌ No episodes found on Master Series page. Moving on.");
                // Mark visited anyway to avoid infinite retries
                saveVisited(masterSeriesUrl);
                continue;
            }

            // Gogoanime usually lists newest first. Reverse to mine Oldest -> Newest.
            episodeLinks = episodeLinks.reverse();

            console.log(`   ✨ Stripping ${episodeLinks.length} episodes from oldest to newest!`);

            for (let e = 0; e < episodeLinks.length; e++) {
                const epLink = episodeLinks[e];
                
                let epTitle = "unknown";
                let epNum = 0;
                try {
                    const slug = epLink.split('/').filter(Boolean).pop();
                    const parts = slug.split('-episode-');
                    if (parts.length === 2) {
                        epTitle = parts[0].replace(/-/g, ' ');
                        const numMatch = parts[1].match(/^(\d+)/);
                        epNum = numMatch ? parseInt(numMatch[1]) : 1;
                    }
                } catch(err){}

                console.log(`      🎬 [Ep ${e+1}/${episodeLinks.length}] Mining ${epTitle} Ep ${epNum}...`);
                
                const epPage = await browser.newPage();
                
                // Disable loading images/fonts to speed up scraping
                await epPage.setRequestInterception(true);
                epPage.on('request', req => {
                    const rt = req.resourceType();
                    if (['image', 'stylesheet', 'font', 'media'].includes(rt)) {
                        req.abort();
                    } else {
                        req.continue();
                    }
                });

                try {
                    await epPage.goto(epLink, { waitUntil: 'domcontentloaded', timeout: 25000 });
                    
                    let waitTicks = 0;
                    let extracted = { bloggerToken: null };
                    while (waitTicks < 3) {
                        extracted = await epPage.evaluate(() => {
                            const li = document.querySelector('li[data-type="Blogger"]');
                            const article = document.querySelector('article');
                            const img = document.querySelector('meta[property="og:image"]');
                            
                            return {
                                bloggerToken: li ? li.getAttribute('data-encrypted-url1') : null,
                                postId: article ? article.id.replace('post-', '') : null,
                                featureImage: img ? img.content : null
                            };
                        });
                        if (extracted.bloggerToken) break;
                        await new Promise(r => setTimeout(r, 2000));
                        waitTicks++;
                    }

                    if (extracted.bloggerToken) {
                        console.log(`         ✅ Stream Tokens Secured! Saving...`);
                        try {
                            const iframeData = JSON.stringify({
                                bloggerToken: extracted.bloggerToken,
                                postId: extracted.postId || '',
                                featureImage: extracted.featureImage || ''
                            });
                            
                            await supabase.from('anime_links').upsert({ 
                                title: epTitle, 
                                episode: epNum, 
                                type: epLink.includes('dubbed') ? 'dub' : 'sub', 
                                url: iframeData 
                            }, { onConflict: 'title, episode, type' });
                        } catch (e) {
                            console.error("         ❌ Supabase Error:", e.message);
                        }
                    } else {
                        console.log(`         ❌ Failed to crack stream tokens.`);
                    }
                } catch (err) {
                    console.log(`         ⚠️ Network drop: ${err.message}`);
                }

                try {
                    await epPage.close();
                } catch (e) {
                    console.log(`         ⚠️ Failed to close page gracefully.`);
                }
            }

            // Successfully mined the whole series! Lock it in so we don't do it again.
            console.log(`   🛡️ Successfully archived ${masterSeriesUrl}!`);
            saveVisited(masterSeriesUrl);
        }

        currentPage++;
    }

    console.log("\n🎉 OFFENSIVE DEEP MINING COMPLETE! WE REACHED THE END OF TIME!");
    await browser.close();
}

runMiner();
