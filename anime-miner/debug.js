const puppeteer = require('puppeteer-extra');
const StealthPlugin = require('puppeteer-extra-plugin-stealth');
puppeteer.use(StealthPlugin());
const cheerio = require('cheerio');

(async () => { 
    const browser = await puppeteer.launch({headless: 'new'}); 
    const page = await browser.newPage(); 
    await page.goto('https://gogoanime.by/?page=1', {waitUntil: 'domcontentloaded'}); 
    const html = await page.content(); 
    const $ = cheerio.load(html); 
    console.log('Listing classes on main episode containers:'); 
    $('article, li, div.item').slice(0, 10).each((i, el) => console.log($(el).attr('class'))); 
    console.log('Looking for links...'); 
    $('a[href*="episode"]').slice(0, 15).each((i, el) => console.log($(el).attr('href'))); 
    await browser.close(); 
})();
