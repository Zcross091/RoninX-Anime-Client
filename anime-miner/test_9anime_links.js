const cheerio = require('cheerio');
const fs = require('fs');
const html = fs.readFileSync('9anime_test.html', 'utf8');
const $ = cheerio.load(html);

const links = new Set();
$('a').each((i, el) => {
    const href = $(el).attr('href');
    if (href && !href.includes('/genre/') && href !== '#' && href.length > 2) {
        links.add(href);
    }
});

console.log(Array.from(links).slice(0, 30));
