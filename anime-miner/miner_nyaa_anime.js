require('dotenv').config();
const puppeteer = require('puppeteer-extra');
const StealthPlugin = require('puppeteer-extra-plugin-stealth');
const { createClient } = require('@supabase/supabase-js');

puppeteer.use(StealthPlugin());

const supabase = createClient(process.env.SUPABASE_URL, process.env.SUPABASE_KEY);

const NYAA_ANIME_URL = "https://nyaa.si/?c=1_2"; // Anime - English Translated

async function mineNyaaAnime() {
    console.log(`🚀 Starting NYAA ANIME Miner...`);
    const browser = await puppeteer.launch({ headless: false, args: ['--window-size=800,600'] });
    const page = await browser.newPage();
    
    try {
        let currentPage = 1;
        while (currentPage <= 5) { // Mine first 5 pages
            console.log(`\n======================================`);
            console.log(`📺 Scouting Nyaa Anime Timeline: Page ${currentPage}`);
            console.log(`======================================\n`);
            
            await page.goto(`${NYAA_ANIME_URL}&p=${currentPage}`, { waitUntil: 'domcontentloaded', timeout: 30000 });
            
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
            
            console.log(`   Found ${torrents.length} anime torrents on page ${currentPage}...`);
            
            for (const t of torrents) {
                // Try to parse title and episode
                // E.g. "[SubsPlease] One Piece - 1100 (1080p) [XXXXXX].mkv" -> "One Piece", "1100"
                let title = t.rawTitle;
                let epNum = 1;
                
                // Remove bracket tags at start and end
                title = title.replace(/^\[.*?\]/, '').replace(/\[.*?\]$/, '').replace(/\(.*?\)/g, '').trim();
                
                // Match " - 1100 " or " Episode 1100 " or " 1100 "
                const epMatch = title.match(/(?:- |episode |ep )?(\d+)(?:\s|$|v\d+)/i);
                if (epMatch) {
                    epNum = parseInt(epMatch[1]);
                    title = title.replace(epMatch[0], '').trim();
                }
                
                // Clean up trailing hyphens or dots
                title = title.replace(/-$/, '').replace(/\.$/, '').replace(/_/, ' ').trim();
                title = title.toLowerCase();
                
                console.log(`   🎬 Extracted: ${title} | Ep ${epNum}`);
                
                const { error } = await supabase.from('anime_links').upsert(
                    {
                        title: title,
                        episode: epNum,
                        type: 'sub', // English Translated category
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
    
    console.log("🎉 NYAA ANIME MINING COMPLETE!");
    await browser.close();
}

mineNyaaAnime();
