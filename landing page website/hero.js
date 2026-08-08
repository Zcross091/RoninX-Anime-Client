(() => {
  const reduceMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
  const revealItems = document.querySelectorAll("[data-reveal]");
  const parallaxItems = document.querySelectorAll("[data-parallax]");
  const hero = document.querySelector(".hero");
  const slashLayer = document.querySelector(".slash-layer");

  revealItems.forEach((item) => {
    if (reduceMotion) item.classList.add("is-visible");
  });

  if (!reduceMotion) {
    const revealObserver = new IntersectionObserver(
      (entries, observer) => {
        entries.forEach((entry) => {
          if (!entry.isIntersecting) return;
          entry.target.classList.add("is-visible");
          observer.unobserve(entry.target);
        });
      },
      {
        threshold: 0.14,
        rootMargin: "0px 0px -8% 0px",
      },
    );

    revealItems.forEach((item) => revealObserver.observe(item));
  }

  let ticking = false;

  const updateParallax = () => {
    const viewportCenter = window.innerHeight / 2;

    parallaxItems.forEach((item) => {
      const rect = item.getBoundingClientRect();
      const itemCenter = rect.top + rect.height / 2;
      const distanceFromCenter = itemCenter - viewportCenter;
      const amount = Number(item.dataset.parallax || 0.12);
      const y = Math.max(-26, Math.min(26, distanceFromCenter * amount * -0.14));
      item.style.setProperty("--parallax-y", `${y}px`);
    });

    ticking = false;
  };

  const requestParallaxUpdate = () => {
    if (ticking) return;
    ticking = true;
    window.requestAnimationFrame(updateParallax);
  };

  updateParallax();
  window.addEventListener("scroll", requestParallaxUpdate, { passive: true });
  window.addEventListener("resize", requestParallaxUpdate, { passive: true });

  if (reduceMotion || !hero || !slashLayer) return;

  let lastHoverSlash = 0;
  let lastPointer = null;

  const randomBetween = (min, max) => Math.random() * (max - min) + min;

  const createSlash = (x, y, { hover = false } = {}) => {
    const slash = document.createElement("span");
    const angle = randomBetween(-24, -8);

    slash.className = hover ? "slash slash--hover" : "slash";
    slash.style.left = `${x}px`;
    slash.style.top = `${y}px`;
    slash.style.setProperty("--slash-angle", `${angle}deg`);
    slashLayer.appendChild(slash);

    window.setTimeout(() => slash.remove(), hover ? 420 : 600);
    return slash;
  };

  const createSparks = (x, y, count = 12) => {
    const colors = ["#ffffff", "#ff78a7", "#8d6bff", "#5dbdff"];

    for (let index = 0; index < count; index += 1) {
      const spark = document.createElement("span");
      const angle = randomBetween(0, Math.PI * 2);
      const distance = randomBetween(30, 110);

      spark.className = "spark";
      spark.style.left = `${x + randomBetween(-8, 8)}px`;
      spark.style.top = `${y + randomBetween(-8, 8)}px`;
      spark.style.setProperty("--spark-x", `${Math.cos(angle) * distance}px`);
      spark.style.setProperty("--spark-y", `${Math.sin(angle) * distance}px`);
      spark.style.setProperty("--spark-size", `${randomBetween(2, 5)}px`);
      spark.style.setProperty("--spark-duration", `${randomBetween(430, 760)}ms`);
      spark.style.setProperty("--spark-color", colors[index % colors.length]);
      slashLayer.appendChild(spark);

      window.setTimeout(() => spark.remove(), 800);
    }
  };

  const getPointerPosition = (event) => ({
    x: event.clientX,
    y: event.clientY,
  });

  hero.addEventListener("pointermove", (event) => {
    if (event.pointerType === "touch") return;

    const now = performance.now();
    if (now - lastHoverSlash < 170) return;
    if (!lastPointer) {
      lastPointer = getPointerPosition(event);
      return;
    }

    const movement = Math.hypot(event.clientX - lastPointer.x, event.clientY - lastPointer.y);
    lastPointer = getPointerPosition(event);
    if (movement < 18) return;

    lastHoverSlash = now;
    createSlash(event.clientX, event.clientY, { hover: true });
  });

  hero.addEventListener("pointerleave", () => {
    lastPointer = null;
  });

  hero.addEventListener("pointerdown", (event) => {
    if (event.button !== undefined && event.button !== 0) return;

    const { x, y } = getPointerPosition(event);
    createSlash(x, y);
    createSparks(x, y, 16);
  });
  const filterButtons = document.querySelectorAll("[data-filter]");
  const titleCards = document.querySelectorAll("[data-category]");

  filterButtons.forEach((button) => {
    button.addEventListener("click", () => {
      const filter = button.dataset.filter;

      filterButtons.forEach((item) => {
        const isActive = item === button;
        item.classList.toggle("is-active", isActive);
        item.setAttribute("aria-selected", String(isActive));
      });

      titleCards.forEach((card) => {
        const shouldShow = filter === "all" || card.dataset.category === filter;
        card.classList.toggle("is-hidden", !shouldShow);
      });
    });
  });
  const githubStats = document.querySelector("[data-github-stats]");
  const releaseVersion = document.querySelector("[data-release-version]");
  const releaseDate = document.querySelector("[data-release-date]");
  const downloadCount = document.querySelector("[data-download-count]");
  const starCount = document.querySelector("[data-star-count]");
  const releaseStatus = document.querySelector("[data-release-status]");

  const GITHUB_REPO = "Zcross091/RoninX-Anime-Client";
  const GITHUB_API = `https://api.github.com/repos/${GITHUB_REPO}`;
  const GITHUB_CACHE_KEY = "roninx-github-stats-v1";
  const CACHE_TTL = 10 * 60 * 1000;

  const formatNumber = (value) => new Intl.NumberFormat("en", { notation: "compact", maximumFractionDigits: 1 }).format(value);
  const formatDate = (value) => new Intl.DateTimeFormat("en", { month: "short", day: "numeric", year: "numeric" }).format(new Date(value));

  const setReleaseError = () => {
    if (!githubStats) return;
    if (releaseVersion) releaseVersion.textContent = "Available";
    if (releaseDate) releaseDate.textContent = "View latest release";
    if (downloadCount) downloadCount.textContent = "—";
    if (starCount) starCount.textContent = "—";
    if (releaseStatus) {
      releaseStatus.textContent = "Live GitHub stats are temporarily unavailable. The download link still works.";
      releaseStatus.classList.add("is-error");
    }
  };

  const renderReleaseStats = ({ repo, releases }) => {
    const publishedReleases = releases.filter((release) => !release.draft && !release.prerelease);
    const latest = publishedReleases[0] || releases[0];
    const downloads = releases.reduce(
      (total, release) => total + (release.assets || []).reduce((assetTotal, asset) => assetTotal + (asset.download_count || 0), 0),
      0,
    );

    if (releaseVersion) releaseVersion.textContent = latest?.tag_name || latest?.name || "Latest";
    if (releaseDate) releaseDate.textContent = latest?.published_at ? formatDate(latest.published_at) : "Release available";
    if (downloadCount) downloadCount.textContent = formatNumber(downloads);
    if (starCount) starCount.textContent = formatNumber(repo.stargazers_count || 0);
    if (releaseStatus) {
      releaseStatus.textContent = "Live data from GitHub · Updated just now";
      releaseStatus.classList.add("is-ready");
    }
  };

  const loadGithubStats = async () => {
    if (!githubStats) return;

    try {
      const cached = JSON.parse(sessionStorage.getItem(GITHUB_CACHE_KEY) || "null");
      if (cached && Date.now() - cached.timestamp < CACHE_TTL) {
        renderReleaseStats(cached.data);
        return;
      }
    } catch {
      sessionStorage.removeItem(GITHUB_CACHE_KEY);
    }

    const controller = new AbortController();
    const timeout = window.setTimeout(() => controller.abort(), 7000);

    try {
      const [repoResponse, releasesResponse] = await Promise.all([
        fetch(GITHUB_API, { headers: { Accept: "application/vnd.github+json" }, signal: controller.signal }),
        fetch(`${GITHUB_API}/releases?per_page=100`, { headers: { Accept: "application/vnd.github+json" }, signal: controller.signal }),
      ]);

      if (!repoResponse.ok || !releasesResponse.ok) throw new Error("GitHub API request failed");

      const data = { repo: await repoResponse.json(), releases: await releasesResponse.json() };
      sessionStorage.setItem(GITHUB_CACHE_KEY, JSON.stringify({ timestamp: Date.now(), data }));
      renderReleaseStats(data);
    } catch (error) {
      setReleaseError();
    } finally {
      window.clearTimeout(timeout);
    }
  };

  loadGithubStats();
})();
