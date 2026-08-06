import type { VercelRequest, VercelResponse } from "@vercel/node";
import axios from "axios";
import * as cheerio from "cheerio";

async function scrapeGogoanimeLight(query: string, epNum: number, domains: string[]): Promise<string | null> {
    const cleanQuery = query.toLowerCase().trim();
    const querySlug = cleanQuery.replace(/[^a-z0-9]+/g, "-").replace(/^-+|-+$/g, "");

    for (const domain of domains) {
        try {
            // Stage 1: Direct episode prediction
            const directEpUrl = `${domain}/${querySlug}-episode-${epNum}`;
            try {
                const epRes = await axios.get(directEpUrl, {
                    timeout: 5000,
                    headers: {
                        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36",
                        "Referer": `${domain}/`
                    }
                });
                const ep$ = cheerio.load(epRes.data);
                const iframe = ep$(".play-video iframe, div.anime_video_body iframe, iframe").attr("src");
                if (iframe) {
                    return iframe.startsWith("http") ? iframe : `https:${iframe}`;
                }
            } catch (e) {}

            // Stage 2: Search catalogue
            const searchUrl = `${domain}/search.html?keyword=${encodeURIComponent(query)}`;
            let searchHtml = "";
            try {
                const res = await axios.get(searchUrl, {
                    timeout: 7000,
                    headers: {
                        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36",
                        "Referer": `${domain}/`
                    }
                });
                searchHtml = res.data;
            } catch (e) {
                continue;
            }

            if (!searchHtml) continue;

            const $ = cheerio.load(searchHtml);
            let chosenResult: cheerio.Cheerio<cheerio.Element> | null = null;
            $("ul.items li p.name a").each((_, el) => {
                const text = $(el).text().toLowerCase().trim();
                const href = $(el).attr("href") || "";
                if (text === cleanQuery || href.includes(querySlug)) {
                    chosenResult = $(el);
                }
            });

            if (!chosenResult || !chosenResult.length) {
                const first = $("ul.items li p.name a").first();
                if (first.length) chosenResult = first;
                else continue;
            }

            const categoryHref = chosenResult.attr("href") || "";
            const categoryUrl = categoryHref.startsWith("http")
                ? categoryHref
                : `${domain}${categoryHref.startsWith("/") ? "" : "/"}${categoryHref}`;
            const seriesSlug = categoryHref.replace("/category/", "").replace("/anime/", "").replace(/\/$/, "");

            let exactEpisodeUrl: string | null = null;
            try {
                const catRes = await axios.get(categoryUrl, {
                    timeout: 7000,
                    headers: {
                        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36",
                        "Referer": searchUrl
                    }
                });
                const catHtml = catRes.data;

                if (catHtml) {
                    const cat$ = cheerio.load(catHtml);
                    const movieId = cat$("#movie_id").val() || cat$("input#movie_id").val() || "";
                    const aliasId = cat$("#alias_anime").val() || cat$("input#alias_anime").val() || seriesSlug;
                    const lastEpEl = cat$("ul#episode_page li a").last();
                    const epEnd = lastEpEl.attr("ep_end") || "9999";

                    if (movieId) {
                        const ajaxUrl = `https://ajax.gogocdn.net/ajax/load-list-episode?ep_start=0&ep_end=${epEnd}&id=${movieId}&default_ep=0&alias=${aliasId}`;
                        const ajaxRes = await axios.get(ajaxUrl, {
                            timeout: 7000,
                            headers: {
                                "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36",
                                "Referer": categoryUrl
                            }
                        });
                        const ajaxHtml = ajaxRes.data;

                        if (ajaxHtml) {
                            const ajax$ = cheerio.load(ajaxHtml);
                            ajax$("#episode_related li a").each((_, el) => {
                                const href = ajax$(el).attr("href")?.trim() || "";
                                const match = href.match(/-episode-(\d+(?:\.\d+)?)/i);
                                if (match && parseFloat(match[1]) === epNum) {
                                    exactEpisodeUrl = href.startsWith("http") ? href : `${domain}${href.startsWith("/") ? "" : "/"}${href}`;
                                }
                            });
                        }
                    }
                }
            } catch (e) {}

            if (!exactEpisodeUrl) {
                exactEpisodeUrl = `${domain}/${seriesSlug}-episode-${epNum}`;
            }

            try {
                const epRes = await axios.get(exactEpisodeUrl, {
                    timeout: 8000,
                    headers: {
                        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36",
                        "Referer": categoryUrl
                    }
                });
                const ep$ = cheerio.load(epRes.data);
                const iframe = ep$(".play-video iframe, div.anime_video_body iframe, iframe").attr("src");
                if (iframe) {
                    return iframe.startsWith("http") ? iframe : `https:${iframe}`;
                }
            } catch (e) {}
        } catch (e) {}
    }
    return null;
}

export default async function handler(req: VercelRequest, res: VercelResponse) {
    res.setHeader("Access-Control-Allow-Credentials", "true");
    res.setHeader("Access-Control-Allow-Origin", "*");
    res.setHeader("Access-Control-Allow-Methods", "GET,OPTIONS");

    if (req.method === "OPTIONS") {
        return res.status(200).end();
    }

    const title = (req.query.title as string) || "";
    const episode = parseInt((req.query.episode as string) || "1", 10);

    if (!title) {
        return res.status(400).json({ error: "Missing title query parameter" });
    }

    try {
        const defaultDomains = ["https://anitaku.pe", "https://gogoanime3.co", "https://gogoanime.or.at"];
        const domains = process.env.GOGO_DOMAINS
            ? process.env.GOGO_DOMAINS.split(",").map(d => d.trim())
            : defaultDomains;

        const streamUrl = await scrapeGogoanimeLight(title, episode, domains);

        if (streamUrl) {
            return res.status(200).json({
                status: "success",
                title,
                episode,
                url: streamUrl
            });
        } else {
            return res.status(404).json({
                status: "not_found",
                error: `Stream not found for ${title} Ep ${episode}`
            });
        }
    } catch (err: any) {
        return res.status(500).json({ error: err.message || "Internal Server Error" });
    }
}
