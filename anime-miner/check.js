const puppeteer = require('puppeteer-extra');
const StealthPlugin = require('puppeteer-extra-plugin-stealth');
puppeteer.use(StealthPlugin());

async function check() {
  const browser = await puppeteer.launch({ headless: true });
  const page = await browser.newPage();
  
  await page.goto('https://gogoanime.es/awajima-hyakkei-episode-12-english-subbed/', { waitUntil: 'domcontentloaded' });
  const master = await page.evaluate(() => {
    const link = Array.from(document.querySelectorAll('a')).find(a => a.href && a.href.includes('/series/') && !a.href.includes('?'));
    return link ? link.href : null;
  });
  console.log('Master Series Link:', master);
  
  await browser.close();
}
check();
