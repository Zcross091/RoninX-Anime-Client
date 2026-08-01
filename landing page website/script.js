document.addEventListener('DOMContentLoaded', () => {
    const prefersReducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

    // ============================================================
    // SCROLL PROGRESS BAR
    // ============================================================
    const scrollProgress = document.getElementById('scroll-progress');
    if (scrollProgress) {
        window.addEventListener('scroll', () => {
            const scrollTop = window.scrollY;
            const docHeight = document.documentElement.scrollHeight - window.innerHeight;
            const progress = docHeight > 0 ? (scrollTop / docHeight) * 100 : 0;
            scrollProgress.style.width = progress + '%';
        }, { passive: true });
    }

    // ============================================================
    // 3D PARTICLE CANVAS SYSTEM
    // ============================================================
    const canvas = document.getElementById('particle-canvas');
    if (canvas && !prefersReducedMotion) {
        const ctx = canvas.getContext('2d');
        let particles = [];
        let mouseX = -1000;
        let mouseY = -1000;
        let animationId;

        const resize = () => {
            canvas.width = window.innerWidth;
            canvas.height = window.innerHeight;
        };
        resize();
        window.addEventListener('resize', resize);

        document.addEventListener('mousemove', (e) => {
            mouseX = e.clientX;
            mouseY = e.clientY;
        });

        document.addEventListener('mouseleave', () => {
            mouseX = -1000;
            mouseY = -1000;
        });

        const PARTICLE_COUNT = Math.min(80, Math.floor(window.innerWidth / 20));
        const CONNECTION_DISTANCE = 120;
        const MOUSE_RADIUS = 150;

        class Particle {
            constructor() {
                this.x = Math.random() * canvas.width;
                this.y = Math.random() * canvas.height;
                this.vx = (Math.random() - 0.5) * 0.5;
                this.vy = (Math.random() - 0.5) * 0.5;
                this.radius = Math.random() * 2 + 0.5;
                // Accent color palette
                const colors = [
                    'rgba(244, 63, 94,',   // rose
                    'rgba(251, 113, 133,',  // pink
                    'rgba(56, 189, 248,',   // blue
                    'rgba(168, 85, 247,',   // purple
                ];
                this.color = colors[Math.floor(Math.random() * colors.length)];
                this.opacity = Math.random() * 0.5 + 0.2;
            }

            update() {
                // Mouse repulsion
                const dx = this.x - mouseX;
                const dy = this.y - mouseY;
                const dist = Math.sqrt(dx * dx + dy * dy);
                if (dist < MOUSE_RADIUS && dist > 0) {
                    const force = (MOUSE_RADIUS - dist) / MOUSE_RADIUS;
                    this.vx += (dx / dist) * force * 0.8;
                    this.vy += (dy / dist) * force * 0.8;
                }

                // Damping
                this.vx *= 0.98;
                this.vy *= 0.98;

                this.x += this.vx;
                this.y += this.vy;

                // Wrap around screen
                if (this.x < -10) this.x = canvas.width + 10;
                if (this.x > canvas.width + 10) this.x = -10;
                if (this.y < -10) this.y = canvas.height + 10;
                if (this.y > canvas.height + 10) this.y = -10;
            }

            draw() {
                ctx.beginPath();
                ctx.arc(this.x, this.y, this.radius, 0, Math.PI * 2);
                ctx.fillStyle = this.color + this.opacity + ')';
                ctx.fill();

                // Glow
                ctx.beginPath();
                ctx.arc(this.x, this.y, this.radius * 3, 0, Math.PI * 2);
                ctx.fillStyle = this.color + (this.opacity * 0.15) + ')';
                ctx.fill();
            }
        }

        // Init particles
        for (let i = 0; i < PARTICLE_COUNT; i++) {
            particles.push(new Particle());
        }

        const drawConnections = () => {
            for (let i = 0; i < particles.length; i++) {
                for (let j = i + 1; j < particles.length; j++) {
                    const dx = particles[i].x - particles[j].x;
                    const dy = particles[i].y - particles[j].y;
                    const dist = Math.sqrt(dx * dx + dy * dy);

                    if (dist < CONNECTION_DISTANCE) {
                        const opacity = (1 - dist / CONNECTION_DISTANCE) * 0.15;
                        ctx.beginPath();
                        ctx.moveTo(particles[i].x, particles[i].y);
                        ctx.lineTo(particles[j].x, particles[j].y);
                        ctx.strokeStyle = `rgba(244, 63, 94, ${opacity})`;
                        ctx.lineWidth = 0.5;
                        ctx.stroke();
                    }
                }
            }
        };

        const animate = () => {
            ctx.clearRect(0, 0, canvas.width, canvas.height);
            particles.forEach(p => {
                p.update();
                p.draw();
            });
            drawConnections();
            animationId = requestAnimationFrame(animate);
        };

        animate();

        // Pause when not visible
        document.addEventListener('visibilitychange', () => {
            if (document.hidden) {
                cancelAnimationFrame(animationId);
            } else {
                animate();
            }
        });
    }

    // ============================================================
    // TYPING ANIMATION ON HERO HEADLINE
    // ============================================================
    if (!prefersReducedMotion) {
        const heroTitle = document.getElementById('hero-title');
        if (heroTitle) {
            const originalHTML = heroTitle.innerHTML;
            const cursor = heroTitle.querySelector('.typing-cursor');

            // The text is already in the DOM for SEO. We animate opacity of the h1 content.
            heroTitle.style.opacity = '0';
            
            setTimeout(() => {
                heroTitle.style.transition = 'opacity 0.6s ease';
                heroTitle.style.opacity = '1';
                
                // Remove cursor after a delay
                if (cursor) {
                    setTimeout(() => {
                        cursor.style.transition = 'opacity 0.5s ease';
                        cursor.style.opacity = '0';
                        setTimeout(() => cursor.remove(), 500);
                    }, 3000);
                }
            }, 300);
        }
    }

    // ============================================================
    // MOBILE MENU TOGGLE
    // ============================================================
    const menuToggle = document.getElementById('menu-toggle');
    const navLinks = document.getElementById('nav-links');

    if (menuToggle && navLinks) {
        menuToggle.addEventListener('click', () => {
            menuToggle.classList.toggle('active');
            navLinks.classList.toggle('active');
        });

        navLinks.querySelectorAll('a').forEach(link => {
            link.addEventListener('click', () => {
                menuToggle.classList.remove('active');
                navLinks.classList.remove('active');
            });
        });
    }

    // ============================================================
    // INTERACTIVE PHONE MOCKUP TAB SWITCHER
    // ============================================================
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

    // Auto-cycle tabs
    if (!prefersReducedMotion) {
        let tabIndex = 0;
        const tabNames = ['stream', 'manga', 'anilist'];
        setInterval(() => {
            tabIndex = (tabIndex + 1) % tabNames.length;
            const btn = document.querySelector(`.mockup-tab-btn[data-tab="${tabNames[tabIndex]}"]`);
            if (btn) btn.click();
        }, 5000);
    }

    // ============================================================
    // ANIMATED COUNTER ON SCROLL
    // ============================================================
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

    // ============================================================
    // 3D TILT EFFECT ON FEATURE CARDS (Holographic Glow)
    // ============================================================
    if (!prefersReducedMotion) {
        const tiltCards = document.querySelectorAll('[data-tilt]');

        tiltCards.forEach(card => {
            const glowEl = card.querySelector('.card-glow');

            card.addEventListener('mousemove', (e) => {
                const rect = card.getBoundingClientRect();
                const x = e.clientX - rect.left;
                const y = e.clientY - rect.top;
                const centerX = rect.width / 2;
                const centerY = rect.height / 2;

                const rotateX = ((y - centerY) / centerY) * -8;
                const rotateY = ((x - centerX) / centerX) * 8;

                card.style.transform = `perspective(800px) rotateX(${rotateX}deg) rotateY(${rotateY}deg) scale3d(1.02, 1.02, 1.02)`;

                // Holographic glow follows cursor
                if (glowEl) {
                    glowEl.style.background = `radial-gradient(circle at ${x}px ${y}px, rgba(244, 63, 94, 0.15) 0%, rgba(56, 189, 248, 0.05) 40%, transparent 70%)`;
                }
            });

            card.addEventListener('mouseleave', () => {
                card.style.transform = 'perspective(800px) rotateX(0deg) rotateY(0deg) scale3d(1, 1, 1)';
                if (glowEl) {
                    glowEl.style.background = 'transparent';
                }
            });
        });
    }

    // ============================================================
    // MAGNETIC HOVER BUTTONS
    // ============================================================
    if (!prefersReducedMotion) {
        const magneticBtns = document.querySelectorAll('.magnetic-btn');

        magneticBtns.forEach(btn => {
            btn.addEventListener('mousemove', (e) => {
                const rect = btn.getBoundingClientRect();
                const x = e.clientX - rect.left - rect.width / 2;
                const y = e.clientY - rect.top - rect.height / 2;

                btn.style.transform = `translate(${x * 0.2}px, ${y * 0.2}px)`;
            });

            btn.addEventListener('mouseleave', () => {
                btn.style.transform = 'translate(0px, 0px)';
            });
        });
    }

    // ============================================================
    // SCROLL-DRIVEN PARALLAX LAYERS
    // ============================================================
    if (!prefersReducedMotion) {
        const parallaxElements = document.querySelectorAll('[data-parallax-speed]');

        if (parallaxElements.length > 0) {
            let ticking = false;

            window.addEventListener('scroll', () => {
                if (!ticking) {
                    requestAnimationFrame(() => {
                        const scrollY = window.scrollY;
                        parallaxElements.forEach(el => {
                            const speed = parseFloat(el.getAttribute('data-parallax-speed'));
                            const yOffset = scrollY * speed;
                            el.style.transform = `translateY(${yOffset}px)`;
                        });
                        ticking = false;
                    });
                    ticking = true;
                }
            }, { passive: true });
        }
    }

    // ============================================================
    // 3D TECH STACK ORBIT POSITIONING
    // ============================================================
    const techOrbit = document.getElementById('tech-orbit');
    if (techOrbit) {
        const techItems = techOrbit.querySelectorAll('.tech-item');
        const container = techOrbit.parentElement;
        const radius = Math.min(container.offsetWidth, container.offsetHeight) / 2 - 50;

        techItems.forEach(item => {
            const angle = parseFloat(item.getAttribute('data-angle')) * (Math.PI / 180);
            const x = Math.cos(angle) * radius;
            const z = Math.sin(angle) * radius;
            item.style.transform = `translate3d(${x}px, 0, ${z}px)`;
        });

        // Reposition on resize
        window.addEventListener('resize', () => {
            const newRadius = Math.min(container.offsetWidth, container.offsetHeight) / 2 - 50;
            techItems.forEach(item => {
                const angle = parseFloat(item.getAttribute('data-angle')) * (Math.PI / 180);
                const x = Math.cos(angle) * newRadius;
                const z = Math.sin(angle) * newRadius;
                item.style.transform = `translate3d(${x}px, 0, ${z}px)`;
            });
        });
    }

    // ============================================================
    // INTERACTIVE DISCOVERY — LIVE SEARCH / ANILIST API
    // ============================================================
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

        items.forEach((anime, index) => {
            const card = document.createElement('div');
            card.className = 'demo-anime-card';
            card.style.animationDelay = `${index * 0.05}s`;
            
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

    // ============================================================
    // FAQ ACCORDION
    // ============================================================
    const faqItems = document.querySelectorAll('.faq-item');

    faqItems.forEach(item => {
        const question = item.querySelector('.faq-question');
        question.addEventListener('click', () => {
            const isOpen = item.classList.contains('open');
            
            faqItems.forEach(i => i.classList.remove('open'));

            if (!isOpen) {
                item.classList.add('open');
            }
        });
    });

    // ============================================================
    // INTERSECTION OBSERVER — SCROLL REVEAL
    // ============================================================
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

    // ============================================================
    // INTERACTIVE MOUSE TILT ON HERO PHONE
    // ============================================================
    const heroVisual = document.querySelector('.hero-visual');
    const mockupContainer = document.querySelector('.app-mockup-wrapper');

    if (heroVisual && mockupContainer && !prefersReducedMotion) {
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

    // ============================================================
    // HEADER HIDE/SHOW ON SCROLL
    // ============================================================
    let lastScrollY = 0;
    const header = document.querySelector('header');
    if (header) {
        window.addEventListener('scroll', () => {
            const currentScrollY = window.scrollY;
            if (currentScrollY > lastScrollY && currentScrollY > 100) {
                header.style.transform = 'translateY(-100%)';
                header.style.transition = 'transform 0.3s ease';
            } else {
                header.style.transform = 'translateY(0)';
            }
            lastScrollY = currentScrollY;
        }, { passive: true });
    }
});
