document.addEventListener('DOMContentLoaded', () => {
    // --- Mobile Menu Toggle ---
    const menuToggle = document.getElementById('menu-toggle');
    const navLinks = document.getElementById('nav-links');

    if (menuToggle && navLinks) {
        menuToggle.addEventListener('click', () => {
            menuToggle.classList.toggle('active');
            navLinks.classList.toggle('active');
        });

        // Close menu when clicking links
        navLinks.querySelectorAll('a').forEach(link => {
            link.addEventListener('click', () => {
                menuToggle.classList.remove('active');
                navLinks.classList.remove('active');
            });
        });
    }

    // --- Interactive Phone Mockup Tab Switcher ---
    const tabBtns = document.querySelectorAll('.mockup-tab-btn');
    const mockupScreens = document.querySelectorAll('.mockup-screen');

    tabBtns.forEach(btn => {
        btn.addEventListener('click', () => {
            const targetTab = btn.getAttribute('data-tab');

            tabBtns.forEach(b => b.classList.remove('active'));
            mockupScreens.forEach(s => s.classList.remove('active'));

            btn.classList.add('active');
            const targetScreen = document.getElementById(`screen-${targetTab}`);
            if (targetScreen) {
                targetScreen.classList.add('active');
            }
        });
    });

    // --- Animated Counter on Scroll ---
    const statNumbers = document.querySelectorAll('.stat-number');
    let animatedStats = false;

    const animateStats = () => {
        statNumbers.forEach(stat => {
            const target = parseInt(stat.getAttribute('data-target'), 10);
            const prefix = stat.getAttribute('data-prefix') || '';
            const suffix = stat.getAttribute('data-suffix') || '';
            let count = 0;
            const step = Math.max(1, Math.floor(target / 40));

            const updateCount = () => {
                count += step;
                if (count >= target) {
                    stat.textContent = `${prefix}${target}${suffix}`;
                } else {
                    stat.textContent = `${prefix}${count}${suffix}`;
                    requestAnimationFrame(updateCount);
                }
            };

            updateCount();
        });
    };

    const statsSection = document.querySelector('.stats-section');
    if (statsSection) {
        const statsObserver = new IntersectionObserver((entries) => {
            entries.forEach(entry => {
                if (entry.isIntersecting && !animatedStats) {
                    animatedStats = true;
                    animateStats();
                }
            });
        }, { threshold: 0.4 });
        statsObserver.observe(statsSection);
    }

    // --- Interactive Discovery Live Search / AniList API Integration ---
    const fallbackAnimeData = [
        { title: "Dragon Ball Super", category: "Popular", rating: "4.8", eps: "131/131 Sub|Dub", genre: "Action • Martial Arts", tag: "CLASSIC", image: "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx21175-1eEalZ0c8nK4.jpg" },
        { title: "Dragon Ball Z", category: "Top Rated", rating: "4.9", eps: "291/291 Sub|Dub", genre: "Action • Shounen", tag: "LEGENDARY", image: "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx225-2C75lO4p9K4f.png" },
        { title: "Solo Leveling", category: "Trending", rating: "4.9", eps: "12/12 Sub|Dub", genre: "Action • Fantasy", tag: "TRENDING", image: "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx151807-m1gXyP6n0w6n.jpg" },
        { title: "Jujutsu Kaisen Season 2", category: "Popular", rating: "4.9", eps: "23/23 Sub|Dub", genre: "Action • Supernatural", tag: "HOT", image: "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx145064-ee0d1B4yN8W6.jpg" },
        { title: "Demon Slayer: Hashira Training", category: "Trending", rating: "4.8", eps: "08/08 Sub|Dub", genre: "Action • Historical", tag: "NEW", image: "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx170942-5yqL3k7o4yG6.jpg" },
        { title: "Frieren: Beyond Journey's End", category: "Top Rated", rating: "4.95", eps: "28/28 Sub|Dub", genre: "Adventure • Fantasy", tag: "MUST WATCH", image: "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx154587-g6P26d9lE5y0.jpg" },
        { title: "Naruto Shippuden", category: "Popular", rating: "4.9", eps: "500/500 Sub|Dub", genre: "Action • Ninja", tag: "POPULAR", image: "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx1735-aKzR1e5d7N3O.jpg" },
        { title: "One Piece", category: "Popular", rating: "4.9", eps: "1100+ Eps", genre: "Action • Adventure", tag: "ONGOING", image: "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx21-YCDoj1ekBxIm.jpg" },
        { title: "Attack on Titan Final Season", category: "Top Rated", rating: "4.9", eps: "Final Chapter", genre: "Dark Fantasy • Military", tag: "COMPLETE", image: "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx131681-3c58Y6oY4N8O.jpg" },
        { title: "Bleach: Thousand-Year Blood War", category: "Trending", rating: "4.9", eps: "26/26 Sub|Dub", genre: "Action • Shounen", tag: "POPULAR", image: "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx114446-0b3W5p9iN6K2.jpg" },
        { title: "Chainsaw Man", category: "Popular", rating: "4.7", eps: "12/12 Sub|Dub", genre: "Action • Dark Fantasy", tag: "POPULAR", image: "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx127230-T5cR1n2m9K4f.jpg" },
        { title: "Spy x Family", category: "Popular", rating: "4.8", eps: "37/37 Sub|Dub", genre: "Comedy • Action", tag: "TOP", image: "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx140960-aK5jE6N1k4L2.jpg" },
        { title: "My Hero Academia Season 7", category: "Trending", rating: "4.7", eps: "Ongoing Sub|Dub", genre: "Action • Superhero", tag: "NEW SEASON", image: "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx163139-3y5O6p9N0w2m.jpg" },
        { title: "Hunter x Hunter (2011)", category: "Top Rated", rating: "4.9", eps: "148/148 Sub|Dub", genre: "Action • Adventure", tag: "MASTERPIECE", image: "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx11061-sP5P1d7N3O2m.jpg" },
        { title: "Death Note", category: "Top Rated", rating: "4.8", eps: "37/37 Sub|Dub", genre: "Psychological • Thriller", tag: "CLASSIC", image: "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx1535-aKzR1e5d7N3O.jpg" },
        { title: "Cyberpunk: Edgerunners", category: "Movies", rating: "4.8", eps: "10/10 Complete", genre: "Sci-Fi • Cyberpunk", tag: "1080P/60FPS", image: "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx120377-5yqL3k7o4yG6.jpg" }
    ];

    const searchInput = document.getElementById('anime-search');
    const filterBtns = document.querySelectorAll('.filter-btn');
    const animeGrid = document.getElementById('demo-anime-grid');

    const renderAnimeCards = (items) => {
        if (!animeGrid) return;
        animeGrid.innerHTML = '';

        if (!items || items.length === 0) {
            animeGrid.innerHTML = `
                <div class="no-results">
                    <span>🔍</span>
                    <p>No matching titles found. RoninX Android client searches 10,000+ live sources in-app.</p>
                </div>
            `;
            return;
        }

        items.forEach(anime => {
            const card = document.createElement('div');
            card.className = 'demo-anime-card';
            
            const imageHtml = anime.image ? `<img src="${anime.image}" alt="${anime.title}" loading="lazy" onerror="this.style.display='none'">` : '';

            card.innerHTML = `
                <div class="card-badge">${anime.tag || 'AVAILABLE'}</div>
                <div class="card-thumbnail">
                    ${imageHtml}
                    <div class="play-overlay" style="z-index: 2;"><span>▶</span></div>
                </div>
                <div class="card-info">
                    <h4>${anime.title}</h4>
                    <p class="genre">${anime.genre || 'Anime'}</p>
                    <div class="card-meta">
                        <span class="rating">⭐ ${anime.rating || '4.8'}</span>
                        <span class="eps">${anime.eps || 'HD Stream'}</span>
                    </div>
                </div>
            `;
            animeGrid.appendChild(card);
        });
    };

    let activeFilter = 'All';
    let searchQuery = '';
    let searchDebounceTimeout = null;

    // Fetch live search results from AniList GraphQL API
    const fetchAniListSearch = async (query) => {
        if (!animeGrid) return;
        
        animeGrid.innerHTML = `
            <div class="no-results">
                <span class="loading-spinner" style="font-size: 2rem; display: inline-block; animation: spin 1s infinite linear;">⌛</span>
                <p>Searching AniList live database for "${query}"...</p>
            </div>
        `;

        try {
            const gqlQuery = `
            query ($search: String) {
              Page(perPage: 12) {
                media(search: $search, type: ANIME, sort: [POPULARITY_DESC]) {
                  id
                  title {
                    english
                    romaji
                  }
                  coverImage {
                    large
                  }
                  averageScore
                  episodes
                  genres
                  format
                  status
                }
              }
            }`;

            const response = await fetch('https://graphql.anilist.co', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Accept': 'application/json',
                },
                body: JSON.stringify({
                    query: gqlQuery,
                    variables: { search: query }
                })
            });

            if (!response.ok) throw new Error('Network error');

            const result = await response.json();
            const mediaList = result?.data?.Page?.media || [];

            if (mediaList.length === 0) {
                filterFallbackAnime(query);
                return;
            }

            const apiAnime = mediaList.map(item => {
                const title = item.title.english || item.title.romaji || "Anime Title";
                const rating = item.averageScore ? (item.averageScore / 10).toFixed(1) : "4.8";
                const eps = item.episodes ? `${item.episodes} Eps` : (item.status || "Ongoing");
                const genre = item.genres && item.genres.length > 0 ? item.genres.slice(0, 2).join(' • ') : "Anime";
                const tag = item.format || "ONLINE";

                return {
                    title: title,
                    category: "Popular",
                    rating: rating,
                    eps: eps,
                    genre: genre,
                    tag: tag,
                    image: item.coverImage ? item.coverImage.large : ""
                };
            });

            renderAnimeCards(apiAnime);
        } catch (err) {
            console.warn("AniList API fallback to local index", err);
            filterFallbackAnime(query);
        }
    };

    const filterFallbackAnime = (query = searchQuery) => {
        const filtered = fallbackAnimeData.filter(item => {
            const matchesCategory = activeFilter === 'All' || item.category === activeFilter;
            const matchesSearch = !query || 
                                  item.title.toLowerCase().includes(query.toLowerCase()) || 
                                  item.genre.toLowerCase().includes(query.toLowerCase());
            
            // If user typed a search query, prioritize title matching over strict category filtering
            if (query && query.trim().length > 0) {
                return item.title.toLowerCase().includes(query.toLowerCase()) || 
                       item.genre.toLowerCase().includes(query.toLowerCase());
            }

            return matchesCategory && matchesSearch;
        });

        renderAnimeCards(filtered);
    };

    if (searchInput) {
        searchInput.addEventListener('input', (e) => {
            searchQuery = e.target.value.trim();

            if (searchDebounceTimeout) clearTimeout(searchDebounceTimeout);

            if (searchQuery.length >= 2) {
                searchDebounceTimeout = setTimeout(() => {
                    fetchAniListSearch(searchQuery);
                }, 350);
            } else {
                filterFallbackAnime();
            }
        });
    }

    filterBtns.forEach(btn => {
        btn.addEventListener('click', () => {
            filterBtns.forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
            activeFilter = btn.getAttribute('data-filter');

            if (searchQuery && searchQuery.length >= 2) {
                fetchAniListSearch(searchQuery);
            } else {
                filterFallbackAnime();
            }
        });
    });

    // Initial Render
    filterFallbackAnime();

    // --- FAQ Accordion ---
    const faqItems = document.querySelectorAll('.faq-item');

    faqItems.forEach(item => {
        const question = item.querySelector('.faq-question');
        question.addEventListener('click', () => {
            const isOpen = item.classList.contains('open');
            
            // Close all
            faqItems.forEach(i => i.classList.remove('open'));

            // Toggle clicked
            if (!isOpen) {
                item.classList.add('open');
            }
        });
    });

    // --- Intersection Observer for Scroll Reveal Animations ---
    const revealElements = document.querySelectorAll('.reveal');

    const revealObserver = new IntersectionObserver((entries) => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                entry.target.classList.add('active');
            }
        });
    }, {
        threshold: 0.15,
        rootMargin: '0px 0px -50px 0px'
    });

    revealElements.forEach(el => revealObserver.observe(el));

    // --- Interactive Mouse Tilt Effect on Hero Card ---
    const heroVisual = document.querySelector('.hero-visual');
    const mockupContainer = document.querySelector('.app-mockup-wrapper');

    if (heroVisual && mockupContainer) {
        heroVisual.addEventListener('mousemove', (e) => {
            const rect = heroVisual.getBoundingClientRect();
            const x = e.clientX - rect.left - rect.width / 2;
            const y = e.clientY - rect.top - rect.height / 2;

            const rotateX = (-y / rect.height) * 15;
            const rotateY = (x / rect.width) * 15;

            mockupContainer.style.transform = `perspective(1000px) rotateX(${rotateX}deg) rotateY(${rotateY}deg) scale3d(1.02, 1.02, 1.02)`;
        });

        heroVisual.addEventListener('mouseleave', () => {
            mockupContainer.style.transform = `perspective(1000px) rotateX(0deg) rotateY(0deg) scale3d(1, 1, 1)`;
        });
    }
});
