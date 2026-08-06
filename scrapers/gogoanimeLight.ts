import axios from 'axios';
import * as cheerio from 'cheerio';
import { createClient } from '@supabase/supabase-js';
import dotenv from 'dotenv';

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
            // ── Stage 1: Direct episode URL prediction (~200ms) ──
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
                // Continue to search catalogue
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
                continue;
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

            // ── Stage 3: Query Gogo AJAX Engine for Episode Link ──
            let exactEpisodeUrl: string | null = null;
            try {
                const catRes = await axios.get(categoryUrl, {
                    timeout: 7000,
                    headers: {
                        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36',
                        'Referer': searchUrl
                    }
                });
                const catHtml = catRes.data;

                if (catHtml) {
                    const cat$ = cheerio.load(catHtml);
                    const movieId = cat$('#movie_id').val() || cat$('input#movie_id').val() || '';
                    const aliasId = cat$('#alias_anime').val() || cat$('input#alias_anime').val() || seriesSlug;
                    const lastEpEl = cat$('ul#episode_page li a').last();
                    const epEnd = lastEpEl.attr('ep_end') || '9999';

                    if (movieId) {
                        const ajaxUrl = `https://ajax.gogocdn.net/ajax/load-list-episode?ep_start=0&ep_end=${epEnd}&id=${movieId}&default_ep=0&alias=${aliasId}`;
                        const ajaxRes = await axios.get(ajaxUrl, {
                            timeout: 7000,
                            headers: {
                                'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36',
                                'Referer': categoryUrl
                            }
                        });
                        const ajaxHtml = ajaxRes.data;

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
                // Ignore
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
