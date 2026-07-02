const puppeteer = require('puppeteer-extra');
const StealthPlugin = require('puppeteer-extra-plugin-stealth');
puppeteer.use(StealthPlugin());

async function check() {
  const browser = await puppeteer.launch({ headless: true });
  const page = await browser.newPage();
  
  await page.goto('https://gogoanime.es/awajima-hyakkei-episode-12-english-subbed/', { waitUntil: 'domcontentloaded' });
  const html = await page.evaluate(() => {
    const videoBody = document.querySelector('.anime_video_body');
    return videoBody ? videoBody.innerHTML : 'Not found';
  });
  console.log('Video Body HTML:\n', html);
  
  await browser.close();
}
check();
