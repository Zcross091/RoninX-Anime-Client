require('dotenv').config();
const puppeteer = require('puppeteer-extra');
const StealthPlugin = require('puppeteer-extra-plugin-stealth');
const { createClient } = require('@supabase/supabase-js');

puppeteer.use(StealthPlugin());

const supabase = createClient(process.env.SUPABASE_URL, process.env.SUPABASE_KEY);

const NYAA_MANGA_URL = "https://nyaa.si/?c=3_1"; // Literature - English Translated

async function mineNyaaManga() {
    console.log(`🚀 Starting NYAA MANGA Miner...`);
    const browser = await puppeteer.launch({ headless: false, args: ['--window-size=800,600'] });
    const page = await browser.newPage();
    
    try {
        let currentPage = 1;
        while (currentPage <= 5) { // Mine first 5 pages
            console.log(`\n======================================`);
            console.log(`📚 Scouting Nyaa Manga Timeline: Page ${currentPage}`);
            console.log(`======================================\n`);
            
            await page.goto(`${NYAA_MANGA_URL}&p=${currentPage}`, { waitUntil: 'domcontentloaded', timeout: 30000 });
            
            const torrents = await page.evaluate(() => {
                const rows = Array.from(document.querySelectorAll('table.torrent-list tbody tr'));
                return rows.map(row => {
                    const titleEl = row.querySelector('td[colspan="2"] a:not(.comments)');
                    const links = row.querySelectorAll('td.text-center a');
                    
                    let magnet = '';
                    for (const link of links) {
                        if (link.href && link.href.startsWith('magnet:')) {
                            magnet = link.href;
                            break;
                        }
                    }
                    
                    return {
                        rawTitle: titleEl ? titleEl.title || titleEl.innerText : '',
                        magnet: magnet
                    };
                }).filter(t => t.rawTitle && t.magnet);
            });
            
            console.log(`   Found ${torrents.length} manga torrents on page ${currentPage}...`);
            
            for (const t of torrents) {
                // Try to parse title and chapter/volume
                let title = t.rawTitle;
                let chapterNum = 1;
                
                // E.g. "[Group] Manga Title - c015 (v02) [Digital]"
                title = title.replace(/^\[.*?\]/, '').replace(/\[.*?\]$/, '').replace(/\(.*?\)/g, '').trim();
                
                // Match " - c015 ", " ch 15 ", " v02 ", " volume 2 "
                const chMatch = title.match(/(?:- )?(?:c|ch|chapter|v|vol|volume)\s?(\d+)/i);
                if (chMatch) {
                    chapterNum = parseInt(chMatch[1]);
                    title = title.replace(chMatch[0], '').trim();
                }
                
                title = title.replace(/-$/, '').replace(/\.$/, '').replace(/_/, ' ').trim();
                title = title.toLowerCase();
                
                console.log(`   📖 Extracted: ${title} | Ch/Vol ${chapterNum}`);
                
                const { error } = await supabase.from('anime_links').upsert(
                    {
                        title: title,
                        episode: chapterNum,
                        type: 'manga', // Manga category
                        url: t.magnet
                    },
                    { onConflict: 'title, episode, type' }
                );
                
                if (error) {
                    console.log(`      ❌ DB Save Failed: ${error.message}`);
                } else {
                    console.log(`      💾 Saved Magnet to Database!`);
                }
            }
            
            currentPage++;
            await new Promise(r => setTimeout(r, 3000));
        }
        
    } catch(e) {
        console.log("Failed to scrape timeline:", e.message);
    }
    
    console.log("🎉 NYAA MANGA MINING COMPLETE!");
    await browser.close();
}

mineNyaaManga();
