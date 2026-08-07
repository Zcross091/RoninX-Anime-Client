import type { VercelRequest, VercelResponse } from "@vercel/node";
import axios from "axios";

export default async function handler(req: VercelRequest, res: VercelResponse) {
    res.setHeader("Access-Control-Allow-Credentials", "true");
    res.setHeader("Access-Control-Allow-Origin", "*");
    res.setHeader("Access-Control-Allow-Methods", "GET,OPTIONS");

    if (req.method === "OPTIONS") {
        return res.status(200).end();
    }

    const title = (req.query.title as string) || "";
    const rawChapter = parseInt((req.query.chapter as string) || "1", 10);
    const chapter = isNaN(rawChapter) || rawChapter < 1 ? 1 : rawChapter;

    if (!title) {
        return res.status(400).json({ error: "Missing title query parameter" });
    }

    try {
        const searchUrl = `https://consumet-api-v2.vercel.app/manga/mangapill/${encodeURIComponent(title)}`;
        const searchRes = await axios.get(searchUrl, { timeout: 7000 });
        const searchData = searchRes.data;
        const results = searchData?.results || [];

        let chosenMangaId = "";
        const cleanTitle = title.toLowerCase().trim();
        for (const item of results) {
            const itemTitle = (item.title || "").toLowerCase().trim();
            if (itemTitle.includes(cleanTitle) || cleanTitle.includes(itemTitle)) {
                chosenMangaId = item.id;
                break;
            }
        }

        if (!chosenMangaId && results.length > 0) {
            chosenMangaId = results[0].id;
        }

        if (!chosenMangaId) {
            return res.status(404).json({ error: `Manga not found for: ${title}` });
        }

        const infoUrl = `https://consumet-api-v2.vercel.app/manga/mangapill/info?id=${encodeURIComponent(chosenMangaId)}`;
        const infoRes = await axios.get(infoUrl, { timeout: 7000 });
        const chapters = infoRes.data?.chapters || [];

        let chosenChapterId = "";
        for (const ch of chapters) {
            const chNum = parseFloat(ch.chapter || "0");
            if (chNum === chapter) {
                chosenChapterId = ch.id;
                break;
            }
        }

        if (!chosenChapterId && chapters.length > 0) {
            chosenChapterId = chapters[chapters.length - 1].id;
        }

        if (!chosenChapterId) {
            return res.status(404).json({ error: `Chapter ${chapter} not found for: ${title}` });
        }

        const readUrl = `https://consumet-api-v2.vercel.app/manga/mangapill/read?chapterId=${encodeURIComponent(chosenChapterId)}`;
        const readRes = await axios.get(readUrl, { timeout: 7000 });
        const rawPages = readRes.data || [];

        const pageUrls: string[] = rawPages.map((p: any) => p.img).filter(Boolean);

        if (pageUrls.length > 0) {
            return res.status(200).json({
                status: "success",
                title,
                chapter,
                totalPages: pageUrls.length,
                pages: pageUrls
            });
        } else {
            return res.status(404).json({ error: `No pages found for ${title} Chapter ${chapter}` });
        }
    } catch (err: any) {
        return res.status(500).json({ error: err.message || "Internal Server Error" });
    }
}
