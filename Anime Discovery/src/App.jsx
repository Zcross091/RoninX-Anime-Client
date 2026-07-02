import React, { useState, useEffect } from 'react';
import { createClient } from '@supabase/supabase-js';
import './index.css';

const SUPABASE_URL = import.meta.env.VITE_SUPABASE_URL;
const SUPABASE_KEY = import.meta.env.VITE_SUPABASE_ANON_KEY;
const supabase = createClient(SUPABASE_URL, SUPABASE_KEY);

function App() {
  const [searchTerm, setSearchTerm] = useState('');
  const [searchResults, setSearchResults] = useState([]);
  const [isSearching, setIsSearching] = useState(false);
  const [selectedAnime, setSelectedAnime] = useState(null);
  const [isPlaying, setIsPlaying] = useState(false);
  
  const [activeEpisode, setActiveEpisode] = useState(1);
  const [streamUrl, setStreamUrl] = useState(null);
  const [isIframe, setIsIframe] = useState(false);
  const [isLoadingStream, setIsLoadingStream] = useState(false);
  const [streamError, setStreamError] = useState(false);
  const [isMagnet, setIsMagnet] = useState(false);
  
  const [activeTab, setActiveTab] = useState('discover');

  // Example trending anime for the initial view
  const trendingAnime = [
    { title: "One Piece", image: "https://cdn.myanimelist.net/images/anime/6/73245l.jpg", ep_count: 1143 },
    { title: "Solo Leveling", image: "https://cdn.myanimelist.net/images/anime/1105/138421l.jpg", ep_count: 12 },
    { title: "Jujutsu Kaisen", image: "https://cdn.myanimelist.net/images/anime/1171/109222l.jpg", ep_count: 47 },
    { title: "Attack on Titan", image: "https://cdn.myanimelist.net/images/anime/10/47347l.jpg", ep_count: 89 }
  ];

  const handleSearch = async (e) => {
    e.preventDefault();
    if (!searchTerm.trim()) return;
    
    setIsSearching(true);
    setSearchResults([]);
    
    try {
      // Using Jikan API for metadata search
      const res = await fetch(`https://api.jikan.moe/v4/anime?q=${encodeURIComponent(searchTerm)}&limit=8`);
      const data = await res.json();
      
      const mappedResults = data.data.map(anime => ({
        title: anime.title_english || anime.title,
        image: anime.images.jpg.large_image_url,
        ep_count: anime.episodes || 12,
        score: anime.score
      }));
      
      setSearchResults(mappedResults);
    } catch (err) {
      console.error(err);
    } finally {
      setIsSearching(false);
    }
  };

  const openAnime = (anime) => {
    setSelectedAnime(anime);
    setIsPlaying(false);
    setActiveEpisode(null);
  };

  const closePlayer = () => {
    setSelectedAnime(null);
    setIsPlaying(false);
    setActiveEpisode(null);
    setStreamUrl(null);
    setStreamError(false);
    setIsMagnet(false);
  };

  const handleEpisodeChange = (ep) => {
    setActiveEpisode(ep);
    setIsPlaying(true);
    fetchStream(selectedAnime.title, ep);
  };

  const fetchStream = async (title, epNum) => {
    setIsLoadingStream(true);
    setStreamError(false);
    setIsIframe(false);
    setIsMagnet(false);
    setStreamUrl(null);

    try {
      const searchTitle = title.toLowerCase().replace(/[^a-z0-9]+/g, ' ').trim();
      
      const { data: dbResList, error } = await supabase
        .from('anime_links')
        .select('url')
        .ilike('title', `%${searchTitle}%`)
        .eq('episode', parseInt(epNum));
        
      if (error || !dbResList || dbResList.length === 0) {
        throw new Error("Stream not found");
      }
      
      const dbRes = dbResList[0];
      
      let finalUrl = dbRes.url;
      let iframeFlag = false;

      // Handle Gogoanime JSON tokens
      try {
          const parsedJson = JSON.parse(dbRes.url);
          if (parsedJson && parsedJson.bloggerToken) {
              const qs = new URLSearchParams({
                  Blogger: parsedJson.bloggerToken,
                  feature_image: parsedJson.featureImage || '',
                  ref: "gogoanime.by"
              }).toString();
              finalUrl = `https://9animetv.be/wp-content/plugins/video-player/includes/player/n-bg/player.php?${qs}`;
              iframeFlag = true;
          }
      } catch (e) {
          // Not JSON, it's either a raw iframe or a magnet link
          if (!finalUrl.startsWith('magnet:')) {
            iframeFlag = true;
          }
      }
      
      if (finalUrl.startsWith('magnet:')) {
        setIsMagnet(true);
        setStreamUrl(finalUrl);
      } else {
        setIsIframe(iframeFlag);
        setStreamUrl(finalUrl);
      }
    } catch(err) {
      setStreamError(true);
    } finally {
      setIsLoadingStream(false);
    }
  };

  return (
    <div className="app-container">
      <header className="header">
        <div className="logo" style={{cursor: 'pointer'}} onClick={() => setActiveTab('discover')}>Animetize</div>
        <nav className="main-nav">
          <button 
            className={`nav-btn ${activeTab === 'discover' ? 'active' : ''}`}
            onClick={() => setActiveTab('discover')}
          >
            Discover
          </button>
          <button 
            className={`nav-btn ${activeTab === 'mylist' ? 'active' : ''}`}
            onClick={() => setActiveTab('mylist')}
          >
            My List
          </button>
        </nav>
      </header>

      <main className="main-content">
        {activeTab === 'discover' ? (
          <>
            <section className="hero">
              <h1>Discover & Stream <br/><span style={{color: 'var(--accent-color)'}}>True P2P Anime</span></h1>
          <p>The ultimate decentralized network. Access tens of thousands of anime directly through peer-to-peer magnet streams and high-speed offshore nodes.</p>
          
          <div className="search-container">
            <form onSubmit={handleSearch}>
              <input 
                type="text" 
                className="search-input" 
                placeholder="Search for an anime (e.g., One Piece)..." 
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
              />
              <button type="submit" className="search-button">Search</button>
            </form>
          </div>
        </section>

        <section className="results-section">
          {isSearching ? (
            <div className="loading">Connecting to nodes...</div>
          ) : searchResults.length > 0 ? (
            <div className="results-grid">
              {searchResults.map((anime, idx) => (
                <div className="anime-card" key={idx} onClick={() => openAnime(anime)}>
                  <img src={anime.image} alt={anime.title} className="card-image" />
                  <div className="card-content">
                    <h3 className="card-title">{anime.title}</h3>
                    <div className="card-meta">
                      <span className="badge">TV</span>
                      <span>★ {anime.score || 'N/A'}</span>
                      <span>{anime.ep_count} Episodes</span>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <>
              <h2 style={{marginTop: '2rem', marginBottom: '1rem'}}>Trending Now</h2>
              <div className="results-grid">
                {trendingAnime.map((anime, idx) => (
                  <div className="anime-card" key={idx} onClick={() => openAnime(anime)}>
                    <img src={anime.image} alt={anime.title} className="card-image" />
                    <div className="card-content">
                      <h3 className="card-title">{anime.title}</h3>
                      <div className="card-meta">
                        <span className="badge">TV</span>
                        <span>{anime.ep_count} Episodes</span>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </>
          )}
        </section>
          </>
        ) : (
          <section className="mylist-section" style={{textAlign: 'center', padding: '6rem 2rem'}}>
            <div className="p2p-icon" style={{fontSize: '5rem', marginBottom: '1rem', color: 'var(--text-secondary)'}}>📚</div>
            <h2 style={{fontSize: '2.5rem', marginBottom: '1rem'}}>Your List is Empty</h2>
            <p style={{color: 'var(--text-secondary)', fontSize: '1.2rem', marginBottom: '2rem'}}>
              Start exploring the decentralized swarm and add your favorite series here.
            </p>
            <button 
              className="magnet-btn" 
              onClick={() => setActiveTab('discover')}
              style={{cursor: 'pointer', border: 'none'}}
            >
              Start Discovering
            </button>
          </section>
        )}
      </main>

      {/* Premium Video Player Modal */}
      {selectedAnime && (
        <div className="player-overlay" onClick={closePlayer}>
          <div className="player-container" onClick={(e) => e.stopPropagation()}>
            <div className="player-header">
              <h2>{selectedAnime.title} {activeEpisode ? `- Episode ${activeEpisode}` : ''}</h2>
              <button className="close-btn" onClick={closePlayer}>✕</button>
            </div>
            
            {isPlaying ? (
              <div className="video-wrapper">
                {isLoadingStream ? (
                  <div className="p2p-state">
                    <h3 className="loading">Decrypting nodes...</h3>
                  </div>
                ) : streamError ? (
                  <div className="p2p-state error-state">
                    <div className="p2p-icon">⚠️</div>
                    <h3>Stream Not Found</h3>
                    <p>Our miners haven't archived this episode yet. Please check back later.</p>
                  </div>
                ) : isMagnet ? (
                  <div className="p2p-state" style={{maxWidth: '600px', margin: '0 auto', padding: '2rem'}}>
                  <div className="p2p-icon" style={{fontSize: '3rem', marginBottom: '1rem'}}>⚡</div>
                  <h3 style={{fontSize: '1.8rem', marginBottom: '1rem'}}>Decentralized Stream Ready</h3>
                  <p style={{marginBottom: '2rem', color: 'var(--text-secondary)', lineHeight: '1.6'}}>
                    To bypass browser sandbox limits and stream ultra high-quality `.mkv` files without ads or buffering, you must use a dedicated P2P client.
                  </p>
                  
                  <div style={{display: 'flex', flexDirection: 'column', gap: '1rem', alignItems: 'center'}}>
                    <a 
                      href={streamUrl} 
                      target="_blank" 
                      rel="noopener noreferrer" 
                      className="magnet-btn"
                      style={{width: '100%', padding: '1rem', fontSize: '1.2rem', fontWeight: 'bold'}}
                    >
                      ▶️ Launch WebTorrent
                    </a>
                    
                    <button 
                      className="magnet-btn" 
                      style={{background: 'rgba(255,255,255,0.05)', boxShadow: 'none', width: '100%'}}
                      onClick={() => {
                        navigator.clipboard.writeText(streamUrl);
                        alert("Magnet link copied! You can paste this directly into WebTorrent Desktop.");
                      }}
                    >
                      📋 Copy Magnet Link
                    </button>
                  </div>
                  
                  <div style={{marginTop: '2rem', padding: '1.5rem', background: 'rgba(255,100,100,0.1)', borderRadius: '12px', border: '1px solid rgba(255,100,100,0.2)'}}>
                    <h4 style={{color: '#ff6b6b', marginBottom: '0.5rem'}}>Nothing happened?</h4>
                    <p style={{fontSize: '0.9rem', color: 'var(--text-secondary)'}}>
                      If the button above didn't open an app, you don't have a supported P2P player installed. <br/><br/>
                      <a href="https://webtorrent.io/desktop/" target="_blank" rel="noopener noreferrer" style={{color: '#fff', textDecoration: 'underline'}}>
                        Download WebTorrent Desktop
                      </a> 
                      &nbsp;(Free & Open Source) to start streaming instantly.
                    </p>
                  </div>
                </div>
                ) : streamUrl && isIframe ? (
                  <iframe src={streamUrl} allowFullScreen allow="autoplay; fullscreen" title="Anime Player"></iframe>
                ) : streamUrl ? (
                  <video src={streamUrl} controls autoPlay></video>
                ) : null}
              </div>
            ) : (
              <div style={{display: 'flex', gap: '2rem', padding: '2rem', background: '#0a0a0a'}}>
                 <img src={selectedAnime.image} alt={selectedAnime.title} style={{width: '200px', borderRadius: '12px', boxShadow: '0 10px 30px rgba(0,0,0,0.5)'}} />
                 <div style={{display: 'flex', flexDirection: 'column', justifyContent: 'center'}}>
                    <h3 style={{fontSize: '2rem', marginBottom: '1rem'}}>{selectedAnime.title}</h3>
                    <div className="card-meta" style={{marginBottom: '1.5rem', fontSize: '1rem'}}>
                       <span className="badge">TV</span>
                       <span>★ {selectedAnime.score || 'N/A'}</span>
                       <span>{selectedAnime.ep_count} Episodes</span>
                    </div>
                    <p style={{color: 'var(--text-secondary)', fontSize: '1.1rem', lineHeight: '1.6', maxWidth: '500px'}}>
                      Welcome to the decentralized network. Select an episode from the list below to connect to the P2P swarm and begin streaming instantly.
                    </p>
                 </div>
              </div>
            )}
            
            <div className="episode-selector">
              {Array.from({length: selectedAnime.ep_count || 24}, (_, i) => i + 1).map(ep => (
                <button 
                  key={ep} 
                  className={`ep-btn ${ep === activeEpisode ? 'active' : ''}`}
                  onClick={() => handleEpisodeChange(ep)}
                >
                  Ep {ep}
                </button>
              ))}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default App;
