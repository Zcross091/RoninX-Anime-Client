import React, { useState, useEffect } from 'react';
import { Seal } from '../ui/Seal';

function AiringCountdown({ nextAiringEpisode }) {
  if (!nextAiringEpisode) return null;

  const [timeLeft, setTimeLeft] = useState('');

  useEffect(() => {
    const calculateTimeLeft = () => {
      const now = Math.floor(Date.now() / 1000);
      const diff = nextAiringEpisode.airingAt - now;

      if (diff <= 0) {
        setTimeLeft('Airing now / Aired');
        return;
      }

      const days = Math.floor(diff / (24 * 3600));
      const hours = Math.floor((diff % (24 * 3600)) / 3600);
      const minutes = Math.floor((diff % 3600) / 60);
      const seconds = diff % 60;

      let parts = [];
      if (days > 0) parts.push(`${days}d`);
      if (hours > 0 || days > 0) parts.push(`${hours}h`);
      if (minutes > 0 || hours > 0 || days > 0) parts.push(`${minutes}m`);
      parts.push(`${seconds}s`);

      setTimeLeft(parts.join(' '));
    };

    calculateTimeLeft();
    const interval = setInterval(calculateTimeLeft, 1000);
    return () => clearInterval(interval);
  }, [nextAiringEpisode]);

  return (
    <div className="bg-accent/10 border border-accent/20 rounded-xl p-3.5 text-center my-4 font-bold shadow-[0_0_15px_rgba(196,32,44,0.05)]">
      <div className="text-[10px] text-zinc-500 uppercase tracking-widest mb-1">
        Next Episode {nextAiringEpisode.episode} Releases In
      </div>
      <div className="text-[16px] text-accent font-display font-black tracking-wider uppercase animate-pulse">
        {timeLeft}
      </div>
    </div>
  );
}

export function PlayerSidebarRight({
  selectedAnime,
  isInWatchlist,
  toggleWatchlist,
  nextAiringEpisode,
}) {
  return (
    <div className="player-sidebar-right">
      <img src={selectedAnime.image} alt={selectedAnime.title} className="sidebar-right-cover" />
      <h3 className="sidebar-right-title">{selectedAnime.title}</h3>
      
      <div className="sidebar-right-meta">
        <span className="sidebar-right-badge">HD</span>
        <span className="sidebar-right-badge pink">Ep {selectedAnime.ep_count}</span>
        <span className="sidebar-right-badge">TV</span>
        <span className="flex items-center gap-1"><Seal score={selectedAnime.score || 'N/A'} /></span>
      </div>
      
      {nextAiringEpisode && <AiringCountdown nextAiringEpisode={nextAiringEpisode} />}

      <p className="sidebar-right-synopsis">
        {selectedAnime.synopsis || 'No synopsis available.'}
      </p>
      
      <p className="sidebar-right-synopsis mt-4" style={{fontSize:'0.75rem'}}>
        Ronin Anime is the best site to watch <strong>{selectedAnime.title}</strong> SUB online, or you can even watch <strong>{selectedAnime.title}</strong> DUB in HD quality.
      </p>

      <button
        onClick={() => toggleWatchlist(selectedAnime)}
        className={`flex items-center justify-center gap-2 border font-bold text-[13px] px-4 py-3 rounded-lg cursor-pointer transition-all hover:scale-[1.02] active:scale-[0.98] w-full mt-4 ${isInWatchlist(selectedAnime) ? 'bg-accent border-accent text-white shadow-[0_0_15px_rgba(196,32,44,0.3)]' : 'bg-white/5 border-white/10 hover:border-white/20 hover:bg-white/10 text-white'}`}
      >
        <span className="text-[15px] font-black">{isInWatchlist(selectedAnime) ? '✓' : '+'}</span>
        {isInWatchlist(selectedAnime) ? 'In Watchlist' : 'Add to Watchlist'}
      </button>
    </div>
  );
}
