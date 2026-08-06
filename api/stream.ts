import type { VercelRequest, VercelResponse } from "@vercel/node";
import { scrapeGogoanimeLight } from "../scrapers/gogoanimeLight";

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
