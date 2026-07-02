const { createClient } = require('@supabase/supabase-js');

const supabaseUrl = 'https://knmbpwlraitujnzdzbfv.supabase.co';
const supabaseKey = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImtubWJwd2xyYWl0dWpuemR6YmZ2Iiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTc4Mjg5OTczMCwiZXhwIjoyMDk4NDc1NzMwfQ.LpFoKTThntmj6_cLIs4XB0kjTOBgB5w1Xlf_vBqKWYo';
const supabase = createClient(supabaseUrl, supabaseKey);

async function checkDb() {
    const { data, error } = await supabase
        .from('anime_links')
        .select('*')
        .order('created_at', { ascending: false })
        .limit(1);
    
    if (error) console.error("Error:", error);
    else console.log("LATEST DB ROW:", data);
}
checkDb();
