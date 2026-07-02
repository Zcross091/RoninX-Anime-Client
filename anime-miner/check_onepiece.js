const { createClient } = require('@supabase/supabase-js');

const supabaseUrl = 'https://knmbpwlraitujnzdzbfv.supabase.co';
const supabaseKey = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImtubWJwd2xyYWl0dWpuemR6YmZ2Iiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTc4Mjg5OTczMCwiZXhwIjoyMDk4NDc1NzMwfQ.LpFoKTThntmj6_cLIs4XB0kjTOBgB5w1Xlf_vBqKWYo';
const supabase = createClient(supabaseUrl, supabaseKey);

async function check() {
    const { data, error } = await supabase
        .from('anime_links')
        .select('*')
        .ilike('title', '%one piece%');
        
    console.log(`Found ${data ? data.length : 0} rows for one piece`);
    if (data && data.length > 0) {
        console.log("Sample title in DB:", data[0].title);
        console.log("Episodes available:", data.map(d => d.episode).sort((a,b)=>a-b).join(', '));
    }
}
check();
