import fs from 'fs';
import path from 'path';
import { scrapeGogoanimeLight } from './gogoanimeLight';

const defaultDomains = [
    "https://anitaku.pe",
    "https://gogoanime3.co",
    "https://gogoanime.or.at"
];

async function run() {
    const args = process.argv.slice(2);
    if (args.length < 2) {
        console.error("Usage: ts-node scrapers/mine.ts <anime_title> <episode_number>");
        process.exit(1);
    }

    const title = args[0].trim();
    const epNum = parseInt(args[1], 10) || 1;

    console.log(`🚀 Starting GitHub Runner Mining Job for: [${title}] Episode ${epNum}...`);

    const domains = (process.env.GOGO_DOMAINS ? process.env.GOGO_DOMAINS.split(',').map(d => d.trim()) : defaultDomains);
    const videoUrl = await scrapeGogoanimeLight(title, epNum, domains);

    const cacheDir = path.join(__dirname, '..', 'cache');
    if (!fs.existsSync(cacheDir)) {
        fs.mkdirSync(cacheDir, { recursive: true });
    }

    const payload = {
        title: title.toLowerCase().trim(),
        episode: epNum,
        url: videoUrl,
        timestamp: Date.now(),
        status: videoUrl ? "success" : "not_found"
    };

    const cacheFile = path.join(cacheDir, 'latest_stream.json');
    fs.writeFileSync(cacheFile, JSON.stringify(payload, null, 2), 'utf-8');

    if (videoUrl) {
        console.log(`🎉 Successfully mined stream URL for [${title}] Ep ${epNum}: ${videoUrl}`);
    } else {
        console.log(`❌ Failed to mine stream URL for [${title}] Ep ${epNum}. Output saved as not_found.`);
    }
}

run().catch(err => {
    console.error("Fatal Mining Error:", err);
    process.exit(1);
});
