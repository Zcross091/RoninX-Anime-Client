const cheerio = require('cheerio');
const fs = require('fs');

const html = fs.readFileSync('test_page_1.html', 'utf8');
const $ = cheerio.load(html);

const links = [];
$('a').each((i, el) => {
    const href = $(el).attr('href');
    if (href && href.includes('gogoanime.es')) {
        links.push(href);
    }
});

console.log(links.slice(0, 50).join('\n'));
