const { createClient } = require('@supabase/supabase-js');
const axios = require('axios');

const supabaseUrl = 'https://knmbpwlraitujnzdzbfv.supabase.co';
const supabaseKey = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImtubWJwd2xyYWl0dWpuemR6YmZ2Iiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTc4Mjg5OTczMCwiZXhwIjoyMDk4NDc1NzMwfQ.LpFoKTThntmj6_cLIs4XB0kjTOBgB5w1Xlf_vBqKWYo';
const supabase = createClient(supabaseUrl, supabaseKey);

async function checkLinks() {
    console.log("Fetching links from Supabase...");
    const { data, error } = await supabase.from('anime_links').select('*').limit(20);
    
    if (error) {
        console.error("Supabase error:", error);
        return;
    }

    if (!data || data.length === 0) {
        console.log("No links found in Supabase! The database is empty.");
        return;
    }

    console.log(`Found ${data.length} links to test. Testing now...`);

    let working = 0;
    let failed = 0;

    for (const item of data) {
        try {
            console.log(`\nTesting: ${item.title} Ep ${item.episode}`);
            console.log(`URL: ${item.url}`);
            
            // Send an HTTP GET request to the stream URL to see if it responds with a video playlist
            const response = await axios.get(item.url, { timeout: 10000 });
            
            if (response.status === 200 && (response.data.includes('#EXTM3U') || response.headers['content-type'].includes('video'))) {
                console.log(`✅ Success! The stream is alive and contains valid video data.`);
                working++;
            } else {
                console.log(`❌ Failed! The server responded but didn't provide a valid video playlist.`);
                failed++;
            }
        } catch (err) {
            console.log(`❌ Failed! Could not connect to the stream: ${err.message}`);
            failed++;
        }
    }

    console.log(`\n=============================`);
    console.log(`Summary: ${working} working | ${failed} failed`);
    console.log(`=============================`);
}

checkLinks();
