import axios from 'axios';
import * as cheerio from 'cheerio';
import { createClient } from '@supabase/supabase-js';
import dotenv from 'dotenv';
import { fetchWithStealthBrowser } from './browserManager';

dotenv.config();

const supabaseUrl = process.env.SUPABASE_URL || "";
const supabaseKey = process.env.SUPABASE_KEY || "";
const supabase = (supabaseUrl && supabaseKey) ? createClient(supabaseUrl, supabaseKey) : null;

async function saveToSupabase(title: string, episode: number, type: string, url: string) {
    if (!supabase) return;
    const { error } = await supabase.from('anime_links').upsert(
        { title: title.toLowerCase().trim(), episode, type, url },
        { onConflict: 'title, episode, type' }
    );
    if (error) console.error("❌ Supabase Error:", error);
    else console.log(`✅ Cached to Supabase: [${title}] Ep ${episode} -> ${url}`);
}

export async function scrapeGogoanimeLight(query: string, epNum: number, domains: string[]): Promise<string | null> {
    const cleanQuery = query.toLowerCase().trim();
    const querySlug = cleanQuery.replace(/[^a-z0-9]+/g, '-').replace(/^-+|-+$/g, '');

    for (const domain of domains) {
        try {
            // ── Stage 1: Try direct episode URL prediction first for maximum speed ──
            const directEpUrl = `${domain}/${querySlug}-episode-${epNum}`;
            try {
                const epRes = await axios.get(directEpUrl, {
                    timeout: 5000,
                    headers: {
                        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36',
                        'Referer': `${domain}/`
                    }
                });
                const ep$ = cheerio.load(epRes.data);
                const iframe = ep$('.play-video iframe, div.anime_video_body iframe, iframe').attr('src');
                if (iframe) {
                    const videoUrl = iframe.startsWith('http') ? iframe : `https:${iframe}`;
                    console.log(`⚡ Instant Direct Gogo Match: [${query}] Ep ${epNum} -> ${videoUrl}`);
                    await saveToSupabase(query, epNum, "embed", videoUrl);
                    return videoUrl;
                }
            } catch (directErr: any) {
                // If 403 or blocked, fallback to Puppeteer Stealth
                if (directErr.response?.status === 403 || directErr.response?.status === 503) {
                    console.log(`⚠️ Stage 1 GET 403/503 on ${directEpUrl}. Triggering Puppeteer Stealth fallback...`);
                    const { iframeSrc } = await fetchWithStealthBrowser(directEpUrl, '.play-video iframe, div.anime_video_body iframe, iframe');
                    if (iframeSrc) {
                        const videoUrl = iframeSrc.startsWith('http') ? iframeSrc : `https:${iframeSrc}`;
                        console.log(`⚡ Stealth Browser Direct Match: [${query}] Ep ${epNum} -> ${videoUrl}`);
                        await saveToSupabase(query, epNum, "embed", videoUrl);
                        return videoUrl;
                    }
                }
            }

            // ── Stage 2: Search Gogoanime catalogue via HTTP ──
            const searchUrl = `${domain}/search.html?keyword=${encodeURIComponent(query)}`;
            let searchHtml = '';
            try {
                const res = await axios.get(searchUrl, {
                    timeout: 7000,
                    headers: {
                        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36',
                        'Referer': `${domain}/`
                    }
                });
                searchHtml = res.data;
            } catch (e: any) {
                console.log(`⚠️ Search HTTP failed on ${searchUrl} (${e.message}). Falling back to Stealth Browser search...`);
                const stealthRes = await fetchWithStealthBrowser(searchUrl, 'ul.items li p.name a');
                searchHtml = stealthRes.html;
            }

            if (!searchHtml) continue;

            const $ = cheerio.load(searchHtml);
            let chosenResult: cheerio.Cheerio<cheerio.Element> | null = null;
            $('ul.items li p.name a').each((_, el) => {
                const text = $(el).text().toLowerCase().trim();
                const href = $(el).attr('href') || '';
                if (text === cleanQuery || href.includes(querySlug)) {
                    chosenResult = $(el);
                }
            });

            if (!chosenResult || !chosenResult.length) {
                const first = $('ul.items li p.name a').first();
                if (first.length) chosenResult = first;
                else continue;
            }

            const categoryHref = chosenResult.attr('href') || '';
            const categoryUrl = categoryHref.startsWith('http')
                ? categoryHref
                : `${domain}${categoryHref.startsWith('/') ? '' : '/'}${categoryHref}`;
            const seriesSlug = categoryHref.replace('/category/', '').replace('/anime/', '').replace(/\/$/, '');

            // ── Stage 3: Query Gogo AJAX Engine for True Episode Links (Handles Ongoing Shows & Non-standard URLs) ──
            let exactEpisodeUrl: string | null = null;
            try {
                let catHtml = '';
                try {
                    const catRes = await axios.get(categoryUrl, {
                        timeout: 7000,
                        headers: {
                            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36',
                            'Referer': searchUrl
                        }
                    });
                    catHtml = catRes.data;
                } catch (catErr: any) {
                    const stealthCat = await fetchWithStealthBrowser(categoryUrl, '#movie_id');
                    catHtml = stealthCat.html;
                }

                if (catHtml) {
                    const cat$ = cheerio.load(catHtml);
                    const movieId = cat$('#movie_id').val() || cat$('input#movie_id').val() || '';
                    const aliasId = cat$('#alias_anime').val() || cat$('input#alias_anime').val() || seriesSlug;
                    const lastEpEl = cat$('ul#episode_page li a').last();
                    const epEnd = lastEpEl.attr('ep_end') || '9999';

                    if (movieId) {
                        const ajaxUrl = `https://ajax.gogocdn.net/ajax/load-list-episode?ep_start=0&ep_end=${epEnd}&id=${movieId}&default_ep=0&alias=${aliasId}`;
                        let ajaxHtml = '';
                        try {
                            const ajaxRes = await axios.get(ajaxUrl, {
                                timeout: 7000,
                                headers: {
                                    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36',
                                    'Referer': categoryUrl
                                }
                            });
                            ajaxHtml = ajaxRes.data;
                        } catch (ajaxErr: any) {
                            const stealthAjax = await fetchWithStealthBrowser(ajaxUrl, '#episode_related li a');
                            ajaxHtml = stealthAjax.html;
                        }

                        if (ajaxHtml) {
                            const ajax$ = cheerio.load(ajaxHtml);
                            ajax$('#episode_related li a').each((_, el) => {
                                const href = ajax$(el).attr('href')?.trim() || '';
                                const match = href.match(/-episode-(\d+(?:\.\d+)?)/i);
                                if (match && parseFloat(match[1]) === epNum) {
                                    exactEpisodeUrl = href.startsWith('http') ? href : `${domain}${href.startsWith('/') ? '' : '/'}${href}`;
                                }
                            });
                        }
                    }
                }
            } catch (ajaxPipelineErr: any) {
                console.warn(`⚠️ Category AJAX pipeline issue for ${seriesSlug}: ${ajaxPipelineErr.message}`);
            }

            // Fallback to speculative slug URL if AJAX pinpointing yields no match
            if (!exactEpisodeUrl) {
                exactEpisodeUrl = `${domain}/${seriesSlug}-episode-${epNum}`;
            }

            // ── Stage 4: Extract Stream Iframe Source ──
            let iframeUrl: string | undefined = undefined;

            try {
                const epRes = await axios.get(exactEpisodeUrl, {
                    timeout: 8000,
                    headers: {
                        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36',
                        'Referer': categoryUrl
                    }
                });
                const ep$ = cheerio.load(epRes.data);
                const iframe = ep$('.play-video iframe, div.anime_video_body iframe, iframe').attr('src');
                if (iframe) {
                    iframeUrl = iframe.startsWith('http') ? iframe : `https:${iframe}`;
                }
            } catch (epErr: any) {
                // Fallback to Stealth Browser for episode page
                console.log(`⚠️ Episode HTTP GET failed on ${exactEpisodeUrl}. Retrying via Stealth Browser...`);
                const stealthRes = await fetchWithStealthBrowser(exactEpisodeUrl, '.play-video iframe, div.anime_video_body iframe, iframe');
                if (stealthRes.iframeSrc) {
                    iframeUrl = stealthRes.iframeSrc.startsWith('http') ? stealthRes.iframeSrc : `https:${stealthRes.iframeSrc}`;
                }
            }

            if (iframeUrl) {
                console.log(`✅ Superior Gogo AJAX Match: [${query}] Ep ${epNum} -> ${iframeUrl}`);
                await saveToSupabase(query, epNum, "embed", iframeUrl);
                return iframeUrl;
            }
        } catch (e: any) {
            console.error(`Gogo Light failed on ${domain}: ${e.message}`);
        }
    }
    return null;
}
