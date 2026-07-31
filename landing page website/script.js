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

    // --- Interactive Discovery Live Search / Filter ---
    const animeData = [
        { title: "Solo Leveling", category: "Trending", rating: "4.9", eps: "12/12 Sub|Dub", genre: "Action • Fantasy", tag: "TRENDING" },
        { title: "Jujutsu Kaisen Season 2", category: "Popular", rating: "4.9", eps: "23/23 Sub|Dub", genre: "Action • Supernatural", tag: "HOT" },
        { title: "Demon Slayer: Hashira Training", category: "Trending", rating: "4.8", eps: "08/08 Sub|Dub", genre: "Action • Historical", tag: "NEW" },
        { title: "Frieren: Beyond Journey's End", category: "Top Rated", rating: "4.95", eps: "28/28 Sub|Dub", genre: "Adventure • Fantasy", tag: "MUST WATCH" },
        { title: "Chainsaw Man", category: "Popular", rating: "4.7", eps: "12/12 Sub|Dub", genre: "Action • Dark Fantasy", tag: "POPULAR" },
        { title: "One Piece", category: "Popular", rating: "4.9", eps: "1100+ Eps", genre: "Action • Adventure", tag: "ONGOING" },
        { title: "Attack on Titan Final Season", category: "Top Rated", rating: "4.9", eps: "Final Chapter", genre: "Dark Fantasy • Military", tag: "COMPLETE" },
        { title: "Spy x Family", category: "Popular", rating: "4.8", eps: "37/37 Sub|Dub", genre: "Comedy • Action", tag: "TOP" },
        { title: "Kaguya-sama: Love is War", category: "Top Rated", rating: "4.9", eps: "37/37 Sub|Dub", genre: "Romance • Comedy", tag: "RATED #1" },
        { title: "Bleach: Thousand-Year Blood War", category: "Trending", rating: "4.9", eps: "26/26 Sub|Dub", genre: "Action • Shounen", tag: "POPULAR" },
        { title: "Vinland Saga", category: "Top Rated", rating: "4.9", eps: "48/48 Sub|Dub", genre: "Drama • Historical", tag: "MASTERPIECE" },
        { title: "Cyberpunk: Edgerunners", category: "Movies", rating: "4.8", eps: "10/10 Complete", genre: "Sci-Fi • Cyberpunk", tag: "1080P/60FPS" }
    ];

    const searchInput = document.getElementById('anime-search');
    const filterBtns = document.querySelectorAll('.filter-btn');
    const animeGrid = document.getElementById('demo-anime-grid');

    const renderAnimeCards = (items) => {
        if (!animeGrid) return;
        animeGrid.innerHTML = '';

        if (items.length === 0) {
            animeGrid.innerHTML = `
                <div class="no-results">
                    <span>🔍</span>
                    <p>No matching titles found in demo index. RoninX client searches 10,000+ live sources in-app.</p>
                </div>
            `;
            return;
        }

        items.forEach(anime => {
            const card = document.createElement('div');
            card.className = 'demo-anime-card';
            card.innerHTML = `
                <div class="card-badge">${anime.tag}</div>
                <div class="card-thumbnail">
                    <div class="play-overlay"><span>▶</span></div>
                </div>
                <div class="card-info">
                    <h4>${anime.title}</h4>
                    <p class="genre">${anime.genre}</p>
                    <div class="card-meta">
                        <span class="rating">⭐ ${anime.rating}</span>
                        <span class="eps">${anime.eps}</span>
                    </div>
                </div>
            `;
            animeGrid.appendChild(card);
        });
    };

    let activeFilter = 'All';
    let searchQuery = '';

    const filterAnime = () => {
        const filtered = animeData.filter(item => {
            const matchesCategory = activeFilter === 'All' || item.category === activeFilter;
            const matchesSearch = item.title.toLowerCase().includes(searchQuery.toLowerCase()) || 
                                  item.genre.toLowerCase().includes(searchQuery.toLowerCase());
            return matchesCategory && matchesSearch;
        });
        renderAnimeCards(filtered);
    };

    if (searchInput) {
        searchInput.addEventListener('input', (e) => {
            searchQuery = e.target.value;
            filterAnime();
        });
    }

    filterBtns.forEach(btn => {
        btn.addEventListener('click', () => {
            filterBtns.forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
            activeFilter = btn.getAttribute('data-filter');
            filterAnime();
        });
    });

    // Initial Render
    renderAnimeCards(animeData);

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
