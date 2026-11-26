// ============================================================
// 1. 精确取自 ClassTrack 源码的 7 套音乐人专辑主题数据
// ============================================================
const ARTIST_THEMES = [
  {
    id: 'JAY_CHOU',
    displayName: '叶惠美·范特西',
    artistName: '周杰伦 (Jay Chou)',
    badgeText: 'JAY · FANTASY',
    genre: 'MANDOPOP',
    genreLabel: '华语',
    isDark: false,
    bg: '#FAF6EE',
    surface: '#FFFFFF',
    surfaceVariant: '#F2EBDC',
    primary: '#B45309',
    secondary: '#235347',
    tertiary: '#991B1B',
    text: '#211A14',
    muted: '#786B5E',
    border: '#E6DEC8',
    glassBg: 'rgba(255, 252, 246, 0.78)',
    glassBorder: 'rgba(255, 255, 255, 0.92)',
    tracks: ['#FEF3E2', '#E8F3EE', '#FCECEB', '#EDF1F8', '#F5EEF9', '#F4EFE6'],
    accents: ['#B45309', '#1E5643', '#B91C1C', '#2A4E82', '#6B3FA0', '#7C4D25'],
    spectrum: ['#B45309', '#235347', '#991B1B', '#D97706'],
    lyrics: [
      { song: '晴天 · 校园晴空', line: '故事的小黄花，从出生那年就飘着，童年的荡秋千，随记忆一直晃到现在。' },
      { song: '七里香 · 夏日诗篇', line: '窗外的麻雀在电线杆上多嘴，你说这一句很有夏天的感觉。' }
    ]
  },
  {
    id: 'DAVID_TAO',
    displayName: '黑色柳丁',
    artistName: '陶喆 (David Tao)',
    badgeText: 'TAO · SOUL R&B',
    genre: 'MANDOPOP',
    genreLabel: '华语',
    isDark: true,
    bg: '#0C0D12',
    surface: '#151720',
    surfaceVariant: '#1E2230',
    primary: '#FF6B1A',
    secondary: '#F59E0B',
    tertiary: '#38BDF8',
    text: '#F8F9FC',
    muted: '#949AB1',
    border: '#272C3F',
    glassBg: 'rgba(21, 23, 32, 0.72)',
    glassBorder: 'rgba(255, 255, 255, 0.20)',
    tracks: ['#331C12', '#2E2412', '#122738', '#231834', '#122C24', '#2E1622'],
    accents: ['#FF7A2E', '#FBBF24', '#38BDF8', '#A78BFA', '#34D399', '#FB7185'],
    spectrum: ['#FF6B1A', '#F59E0B', '#38BDF8', '#FB7185'],
    lyrics: [
      { song: '黑色柳丁 · 摇滚觉醒', line: '今天我心情有一点怪怪的，可是说不出到底为什么，好像有一些悲伤的征兆。' },
      { song: '普通朋友 · Acoustic R&B', line: '等待，我随时随地在等待，做你感情上的依赖，我没有任何的疑问，这是爱。' }
    ]
  },
  {
    id: 'TAYLOR_SWIFT',
    displayName: '午夜星辰',
    artistName: 'Taylor Swift',
    badgeText: 'SWIFT · MIDNIGHTS',
    genre: 'ENGLISH',
    genreLabel: '欧美',
    isDark: true,
    bg: '#0D1226',
    surface: '#151C38',
    surfaceVariant: '#1E284D',
    primary: '#F472B6',
    secondary: '#A78BFA',
    tertiary: '#38BDF8',
    text: '#F8F7FF',
    muted: '#9BA3C7',
    border: '#283463',
    glassBg: 'rgba(21, 28, 56, 0.72)',
    glassBorder: 'rgba(255, 255, 255, 0.22)',
    tracks: ['#321A36', '#221C42', '#142847', '#143036', '#33261A', '#341827'],
    accents: ['#F472B6', '#C084FC', '#60A5FA', '#2DD4BF', '#FBBF24', '#FB7185'],
    spectrum: ['#F472B6', '#A78BFA', '#38BDF8', '#2DD4BF'],
    lyrics: [
      { song: 'Lavender Haze · Midnights', line: 'Meet me at midnight, staring at the ceiling with you, oh you don\'t ever say too much.' },
      { song: 'Lover · Pastel Sky', line: 'Can I go where you go? Can we always be this close forever and ever?' }
    ]
  },
  {
    id: 'AESPA',
    displayName: '超新星金属',
    artistName: 'aespa (에스파)',
    badgeText: 'AESPA · SUPERNOVA',
    genre: 'KPOP',
    genreLabel: 'K-Pop',
    isDark: true,
    bg: '#090B10',
    surface: '#121521',
    surfaceVariant: '#1B2032',
    primary: '#C084FC',
    secondary: '#22D3EE',
    tertiary: '#F43F5E',
    text: '#F4F6FB',
    muted: '#8C96B2',
    border: '#262D45',
    glassBg: 'rgba(18, 21, 33, 0.72)',
    glassBorder: 'rgba(255, 255, 255, 0.22)',
    tracks: ['#26173E', '#102A36', '#331528', '#17223B', '#122E28', '#2A2136'],
    accents: ['#C084FC', '#22D3EE', '#FB7185', '#818CF8', '#34D399', '#E879F9'],
    spectrum: ['#C084FC', '#22D3EE', '#F43F5E', '#818CF8'],
    lyrics: [
      { song: 'Supernova · Hyperstellar', line: 'Event horizon, 사건의 지평선 너머로, Supernova! Nova, can\'t stop hyperstellar.' },
      { song: 'Whiplash · Think Fast', line: 'One look give \'em Whiplash! Beat drop with a big flash, 집중해 좀 더 Think fast.' }
    ]
  },
  {
    id: 'ZTMY',
    displayName: '真夜霓虹',
    artistName: 'ずっと真夜中でいいのに。',
    badgeText: 'ZTMY · MIDNIGHT',
    genre: 'JPOP',
    genreLabel: '日音',
    isDark: true,
    bg: '#0B0D14',
    surface: '#131722',
    surfaceVariant: '#1C2232',
    primary: '#10E898',
    secondary: '#FF477E',
    tertiary: '#8B5CF6',
    text: '#F3F5FA',
    muted: '#8A94AD',
    border: '#232A3E',
    glassBg: 'rgba(19, 23, 34, 0.72)',
    glassBorder: 'rgba(255, 255, 255, 0.22)',
    tracks: ['#1E1B3A', '#0F2E28', '#33182C', '#132842', '#332417', '#1B2436'],
    accents: ['#A78BFA', '#34D399', '#FB7185', '#38BDF8', '#FBBF24', '#818CF8'],
    spectrum: ['#10E898', '#FF477E', '#8B5CF6', '#38BDF8'],
    lyrics: [
      { song: '秒針を噛む · Track 01', line: 'このまま奪って隠して忘れたい、分かり合う○と×もなくて。' },
      { song: 'お勉強しといてよ · Study Mode', line: '理性的な感情論が綺麗で、また迷ってしまうけど。' }
    ]
  },
  {
    id: 'YORUSHIKA',
    displayName: '盗作之夏',
    artistName: 'ヨルシカ (Yorushika)',
    badgeText: 'YORUSHIKA · SUMMER',
    genre: 'JPOP',
    genreLabel: '日音',
    isDark: false,
    bg: '#F5F7FA',
    surface: '#FFFFFF',
    surfaceVariant: '#EEF2F7',
    primary: '#2563EB',
    secondary: '#0D9488',
    tertiary: '#E15B45',
    text: '#0F172A',
    muted: '#64748B',
    border: '#E2E8F0',
    glassBg: 'rgba(255, 255, 255, 0.80)',
    glassBorder: 'rgba(255, 255, 255, 0.94)',
    tracks: ['#EBF3FE', '#E6F7F4', '#FEF0ED', '#EEF0FF', '#FEF7E6', '#EAF6FA'],
    accents: ['#1D4ED8', '#0F766E', '#C2410C', '#4338CA', '#B45309', '#0369A1'],
    spectrum: ['#2563EB', '#0D9488', '#E15B45', '#38BDF8'],
    lyrics: [
      { song: '晴る · Sunny Breeze', line: '晴れに晴れ、花よ咲け、咲いて春のせい。' },
      { song: '春泥棒 · Spring Thief', line: '花見をしよう、散る花びらを拾うように時間を数えよう。' }
    ]
  },
  {
    id: 'FUJII_KAZE',
    displayName: '落日微风',
    artistName: '藤井 風 (Fujii Kaze)',
    badgeText: 'KAZE · GROOVE',
    genre: 'JPOP',
    genreLabel: '日音',
    isDark: false,
    bg: '#F9F6F0',
    surface: '#FFFFFF',
    surfaceVariant: '#F1ECE1',
    primary: '#D95338',
    secondary: '#2B6E4E',
    tertiary: '#C88A4B',
    text: '#1F1A16',
    muted: '#786E65',
    border: '#E6DFD3',
    glassBg: 'rgba(255, 252, 246, 0.80)',
    glassBorder: 'rgba(255, 255, 255, 0.94)',
    tracks: ['#FCEEE9', '#E7F3EC', '#FBF3E0', '#F2ECF8', '#E8F2F5', '#F5ECE4'],
    accents: ['#C84328', '#1E5E3F', '#9E6B14', '#5E3F7A', '#1F6378', '#7D4829'],
    spectrum: ['#D95338', '#2B6E4E', '#C88A4B', '#1F6378'],
    lyrics: [
      { song: '満ちてゆく · Overflowing', line: '手を放す、軽くなる、満ちてゆく。' },
      { song: 'きらり · Kirari Groove', line: '新しい日々も拙い過去も、全部がきらり。' }
    ]
  }
];

const ANTIGRAVITY_SPECTRUM = ['#2c64ed', '#f84242', '#ffcf03', '#10e898'];
const DARK_CTA_SPECTRUM = ['#7189ff', '#3074f9', '#10e898'];

const UNIVERSITY_PORTALS = [
  { school: '对外经济贸易大学', system: '强智教务', category: '强智' },
  { school: '对外经贸 bkjw 新端', system: 'UIBE 本科教务', category: '综合' },
  { school: '北京林业大学', system: '强智教务', category: '强智' },
  { school: '中国农业大学', system: '强智/综合', category: '强智' },
  { school: '北京邮电大学', system: '正方/综合', category: '正方' },
  { school: '北京航空航天大学', system: '综合教务', category: '综合' },
  { school: '北京理工大学', system: '金智/综合', category: '综合' },
  { school: '中国人民大学', system: '金智/综合', category: '综合' },
  { school: '北京师范大学', system: '青果教务', category: '综合' },
  { school: '北京科技大学', system: '强智教务', category: '强智' },
  { school: '北京交通大学', system: '树维/URP', category: '综合' },
  { school: '浙江大学', system: '新正方教务', category: '正方' },
  { school: '武汉大学', system: '新正方/综合', category: '正方' },
  { school: '华中科技大学', system: '综合教务', category: '综合' },
  { school: '复旦大学', system: '复旦教务', category: '综合' },
  { school: '上海交通大学', system: '上交综合', category: '综合' },
  { school: '南京大学', system: '南大教务', category: '综合' }
];

const WORKSHOP_TABS = {
  build: {
    btnText: '复制构建命令',
    code: `git clone https://github.com/Vataraxl/ClassTrack.git
cd ClassTrack
./gradlew assembleDebug
# 产物位于 app/build/outputs/apk/debug/app-debug.apk`
  },
  theme: {
    btnText: '复制自定义主题 JSON',
    code: `{
  "themeName": "群青夜游",
  "artistName": "YOASOBI / 自定义音乐人",
  "isDark": true,
  "primaryHex": "#38BDF8",
  "secondaryHex": "#F43F5E",
  "tertiaryHex": "#A855F7",
  "backgroundHex": "#0B1021",
  "trackBackgroundHexList": ["#172554", "#3B0764", "#4C0519", "#064E3B"]
}`
  },
  stack: {
    btnText: '复制技术栈清单',
    code: `Language: Kotlin 2.3.20 · JVM Toolchain 21
Build:    AGP 9.0.1 · Gradle 9.1 · minSdk 26 · targetSdk 36
UI:       Jetpack Compose (BOM 2026.03.01) · Material 3 · Navigation 3
Storage:  100% 本地 SQLite & SharedPreferences · kotlinx.serialization 1.8.0`
  }
};

// ============================================================
// 2. ParticleRingField 120Hz 胶囊粒子环引擎
// ============================================================
class ParticleRingField {
  constructor(canvas, options = {}) {
    this.canvas = canvas;
    this.ctx = canvas.getContext('2d');
    this.mode = options.mode || 'hero'; // 'hero' | 'ring' | 'dark'
    this.colors = options.colors || ANTIGRAVITY_SPECTRUM;
    this.particles = [];
    this.pointer = { x: 0.5, y: 0.5, active: false };
    this.ringCenter = { x: 0.5, y: 0.5 };
    this.width = 0;
    this.height = 0;
    this.dpr = 1;
    this.isVisible = true;
    this.running = false;
    this.rafId = 0;
    this.time = 0;

    this.resize();
    this.seedParticles();
    this.renderFrame(0);
  }

  setColors(colors) {
    this.colors = colors;
    for (let i = 0; i < this.particles.length; i++) {
      this.particles[i].color = colors[i % colors.length];
    }
    if (!this.running) {
      this.renderFrame(this.time);
    }
  }

  resize() {
    const rect = this.canvas.parentElement.getBoundingClientRect();
    this.dpr = Math.min(window.devicePixelRatio || 1, 2);
    this.width = Math.max(320, Math.round(rect.width));
    this.height = Math.max(320, Math.round(rect.height));
    this.canvas.width = Math.round(this.width * this.dpr);
    this.canvas.height = Math.round(this.height * this.dpr);
    this.ctx.setTransform(this.dpr, 0, 0, this.dpr, 0, 0);
  }

  seedParticles() {
    const isNarrow = this.width < 680;
    const cols = isNarrow ? 24 : 38;
    const rows = isNarrow ? 20 : 30;
    this.particles = [];

    for (let r = 0; r < rows; r++) {
      for (let c = 0; c < cols; c++) {
        // 确定性抖动网格模拟泊松盘均匀分布
        const seed1 = Math.sin(r * 12.9898 + c * 78.233) * 43758.5453;
        const seed2 = Math.sin(r * 39.346 + c * 11.135) * 24634.6345;
        const jx = seed1 - Math.floor(seed1) - 0.5;
        const jy = seed2 - Math.floor(seed2) - 0.5;
        const nx = (c + 0.5 + jx * 0.85) / cols;
        const ny = (r + 0.5 + jy * 0.85) / rows;
        const idx = r * cols + c;
        this.particles.push({
          nx,
          ny,
          phase: (seed1 - Math.floor(seed1)) * Math.PI * 2,
          color: this.colors[idx % this.colors.length]
        });
      }
    }
  }

  updatePointer(clientX, clientY) {
    const rect = this.canvas.getBoundingClientRect();
    if (!rect.width || !rect.height) return;
    this.pointer.x = Math.max(0.15, Math.min(0.85, (clientX - rect.left) / rect.width));
    this.pointer.y = Math.max(0.15, Math.min(0.85, (clientY - rect.top) / rect.height));
    this.pointer.active = true;
  }

  clearPointer() {
    this.pointer.active = false;
  }

  start() {
    if (this.running) return;
    this.running = true;
    let lastTs = performance.now();
    const tick = (now) => {
      if (!this.running) return;
      const dt = Math.min(0.05, (now - lastTs) / 1000);
      lastTs = now;
      if (this.isVisible) {
        this.time += dt;
        this.renderFrame(this.time);
      }
      this.rafId = requestAnimationFrame(tick);
    };
    this.rafId = requestAnimationFrame(tick);
  }

  stop() {
    this.running = false;
    if (this.rafId) {
      cancelAnimationFrame(this.rafId);
      this.rafId = 0;
    }
  }

  renderFrame(t) {
    const ctx = this.ctx;
    const w = this.width;
    const h = this.height;
    ctx.clearRect(0, 0, w, h);

    // 环心惯性追随指针或李萨如飘浮
    const idleX = 0.5 + Math.sin(t * 0.35) * 0.04;
    const idleY = 0.5 + Math.cos(t * 0.27) * 0.03;
    const targetX = this.pointer.active ? this.pointer.x : idleX;
    const targetY = this.pointer.active ? this.pointer.y : idleY;
    this.ringCenter.x += (targetX - this.ringCenter.x) * 0.045;
    this.ringCenter.y += (targetY - this.ringCenter.y) * 0.045;

    const minDim = Math.min(w, h);
    const cx = this.ringCenter.x * w;
    const cy = this.ringCenter.y * h;
    const baseRadius = this.mode === 'ring' ? minDim * 0.32 : minDim * 0.35;
    const ringRadius = baseRadius + minDim * (0.025 * Math.sin(t * 1.1) + 0.014 * Math.cos(t * 2.3));
    const ringSpread = this.mode === 'ring' ? minDim * 0.14 : minDim * 0.19;

    ctx.lineCap = 'round';

    for (let i = 0; i < this.particles.length; i++) {
      const p = this.particles[i];
      const waveX = Math.sin(t * 0.8 + p.ny * 6.0 + p.phase) * 9;
      const waveY = Math.cos(t * 0.75 + p.nx * 6.0 + p.phase) * 9;

      let px = p.nx * w + waveX;
      let py = p.ny * h + waveY;

      const dx = px - cx;
      const dy = py - cy;
      const dist = Math.hypot(dx, dy) || 1;
      const diff = dist - ringRadius;

      // 高斯环带强度：中心留空，环带明亮
      const band = Math.exp(-(diff * diff) / (2 * ringSpread * ringSpread));
      const innerHole = this.mode === 'ring'
        ? Math.min(1, Math.max(0, (dist - minDim * 0.17) / (minDim * 0.09)))
        : Math.min(1, Math.max(0.18, (dist - minDim * 0.13) / (minDim * 0.14)));

      const strength = band * innerHole;
      if (strength < 0.08) {
        if (this.mode !== 'ring' && (i % 5 === 0)) {
          ctx.globalAlpha = this.mode === 'dark' ? 0.16 : 0.12;
          ctx.strokeStyle = p.color;
          ctx.lineWidth = 1.6;
          ctx.beginPath();
          ctx.moveTo(px, py);
          ctx.lineTo(px + 1.5, py + 1.5);
          ctx.stroke();
        }
        continue;
      }

      // 磁力微吸扯位移
      const pull = (diff / dist) * band * -12;
      px += (dx / dist) * pull;
      py += (dy / dist) * pull;

      const angle = Math.atan2(dy, dx) + Math.sin(t * 0.9 + p.phase) * 0.28;
      const dashLen = 3.5 + strength * 7.5;
      const lineW = 1.8 + strength * 1.4;
      const ux = Math.cos(angle) * (dashLen * 0.5);
      const uy = Math.sin(angle) * (dashLen * 0.5);

      ctx.globalAlpha = (this.mode === 'dark' ? 0.88 : 0.78) * strength;
      ctx.strokeStyle = p.color;
      ctx.lineWidth = lineW;
      ctx.beginPath();
      ctx.moveTo(px - ux, py - uy);
      ctx.lineTo(px + ux, py + uy);
      ctx.stroke();
    }

    ctx.globalAlpha = 1;
  }
}

// ============================================================
// 3. 初始化页面交互逻辑
// ============================================================
const reducedMotionQuery = window.matchMedia('(prefers-reduced-motion: reduce)');
let userPausedMotion = reducedMotionQuery.matches;

const heroField = new ParticleRingField(document.getElementById('hero-canvas'), {
  mode: 'hero',
  colors: ANTIGRAVITY_SPECTRUM
});
const workshopField = new ParticleRingField(document.getElementById('workshop-canvas'), {
  mode: 'ring',
  colors: ANTIGRAVITY_SPECTRUM
});
const darkField = new ParticleRingField(document.getElementById('dark-canvas'), {
  mode: 'dark',
  colors: DARK_CTA_SPECTRUM
});

const allFields = [heroField, workshopField, darkField];

function syncMotionLoop() {
  const toggleBtn = document.getElementById('motion-toggle');
  if (userPausedMotion) {
    allFields.forEach((f) => f.stop());
    toggleBtn.textContent = '播放动效';
  } else {
    allFields.forEach((f) => f.start());
    toggleBtn.textContent = '暂停动效';
  }
}

syncMotionLoop();

document.getElementById('motion-toggle').addEventListener('click', () => {
  userPausedMotion = !userPausedMotion;
  syncMotionLoop();
});

// IntersectionObserver 控制画布视口启停（零后台功耗）
const canvasObserver = new IntersectionObserver((entries) => {
  for (const entry of entries) {
    const field = allFields.find((f) => f.canvas === entry.target);
    if (field) {
      field.isVisible = entry.isIntersecting;
    }
  }
}, { threshold: 0.01 });

allFields.forEach((f) => canvasObserver.observe(f.canvas));

window.addEventListener('resize', () => {
  allFields.forEach((f) => {
    f.resize();
    f.seedParticles();
    f.renderFrame(f.time);
  });
}, { passive: true });

// 指针追踪粒子环中心
document.querySelector('.hero-section').addEventListener('pointermove', (e) => heroField.updatePointer(e.clientX, e.clientY), { passive: true });
document.querySelector('.hero-section').addEventListener('pointerleave', () => heroField.clearPointer());
document.querySelector('.workshop-section').addEventListener('pointermove', (e) => workshopField.updatePointer(e.clientX, e.clientY), { passive: true });
document.querySelector('.workshop-section').addEventListener('pointerleave', () => workshopField.clearPointer());
document.querySelector('.dark-cta-card').addEventListener('pointermove', (e) => darkField.updatePointer(e.clientX, e.clientY), { passive: true });
document.querySelector('.dark-cta-card').addEventListener('pointerleave', () => darkField.clearPointer());

// Header 滚动收缩降级监听（当不支持 scroll-driven animations 时）
if (!CSS.supports('(animation-timeline: scroll()) and (animation-range: 0% 100%)')) {
  const header = document.getElementById('site-header');
  const onScrollHeader = () => {
    header.classList.toggle('header--scrolled', window.scrollY > 40);
  };
  onScrollHeader();
  window.addEventListener('scroll', onScrollHeader, { passive: true });
}

// 滚动入场动画降级（当不支持 view() 且无 reduced-motion 时）
if (!CSS.supports('(animation-timeline: view()) and (animation-range: entry)') && !reducedMotionQuery.matches) {
  const revealEls = document.querySelectorAll('.reveal-on-scroll');
  revealEls.forEach((el) => el.classList.add('js-reveal-pending'));
  const revealObserver = new IntersectionObserver((entries) => {
    for (const entry of entries) {
      if (entry.isIntersecting) {
        entry.target.classList.add('is-visible');
        revealObserver.unobserve(entry.target);
      }
    }
  }, { threshold: 0.12 });
  revealEls.forEach((el) => revealObserver.observe(el));
}

// 公告条关闭 & 移动端抽屉菜单
document.getElementById('announcement-close').addEventListener('click', () => {
  document.getElementById('announcement-bar').hidden = true;
});

const mobileDrawer = document.getElementById('mobile-drawer');
const mobileMenuBtn = document.getElementById('mobile-menu-btn');
mobileMenuBtn.addEventListener('click', () => {
  mobileDrawer.showModal();
  mobileMenuBtn.setAttribute('aria-expanded', 'true');
});
document.getElementById('mobile-drawer-close').addEventListener('click', () => mobileDrawer.close());
mobileDrawer.addEventListener('close', () => mobileMenuBtn.setAttribute('aria-expanded', 'false'));
mobileDrawer.addEventListener('click', (e) => {
  if (e.target === mobileDrawer || e.target.tagName === 'A') {
    mobileDrawer.close();
  }
});

// ============================================================
// 4. 交互预览台 + 7 套专辑色板 + 28 点阵通知联动
// ============================================================
let currentTheme = ARTIST_THEMES.find((t) => t.id === 'ZTMY');
let currentUiStyle = 'glass';
let currentLyricIndex = 0;

const theaterFrame = document.getElementById('theater-frame');
const phoneShell = document.getElementById('phone-shell');
const previewThemeBar = document.getElementById('preview-theme-bar');

function buildPreviewThemeButtons() {
  previewThemeBar.innerHTML = '';
  ARTIST_THEMES.forEach((theme) => {
    const btn = document.createElement('button');
    btn.type = 'button';
    btn.className = `theme-pill-select${theme.id === currentTheme.id ? ' is-active' : ''}`;
    btn.textContent = theme.displayName;
    btn.addEventListener('click', () => applyTheme(theme.id, true));
    previewThemeBar.appendChild(btn);
  });
}

function renderDotMatrix(litCount = 18) {
  const matrix = document.getElementById('dot-matrix');
  matrix.innerHTML = '';
  for (let i = 0; i < 28; i++) {
    const dot = document.createElement('span');
    dot.className = 'dot-cell';
    if (i < litCount) dot.classList.add('is-lit');
    if (i === litCount - 1) dot.classList.add('is-head');
    matrix.appendChild(dot);
  }
}

function applyTheme(themeId, syncParticles = false) {
  const found = ARTIST_THEMES.find((t) => t.id === themeId);
  if (!found) return;
  currentTheme = found;
  currentLyricIndex = 0;

  theaterFrame.setAttribute('data-preview-tone', found.isDark ? 'dark' : 'light');
  theaterFrame.style.setProperty('--preview-primary', found.primary);
  theaterFrame.style.setProperty('--preview-secondary', found.secondary);
  theaterFrame.style.setProperty('--preview-tertiary', found.tertiary);

  phoneShell.setAttribute('data-tone', found.isDark ? 'dark' : 'light');
  phoneShell.style.setProperty('--phone-bg', found.bg);
  phoneShell.style.setProperty('--phone-surface', found.surface);
  phoneShell.style.setProperty('--phone-surface-variant', found.surfaceVariant);
  phoneShell.style.setProperty('--phone-text', found.text);
  phoneShell.style.setProperty('--phone-border', found.border);
  phoneShell.style.setProperty('--phone-glass-bg', found.glassBg);
  phoneShell.style.setProperty('--phone-glass-border', found.glassBorder);

  document.getElementById('phone-album-badge').textContent = found.badgeText;

  document.querySelectorAll('.course-block').forEach((block) => {
    const slot = Number(block.getAttribute('data-slot')) || 0;
    block.style.setProperty('--course-bg', found.tracks[slot % found.tracks.length]);
    block.style.setProperty('--course-accent', found.accents[slot % found.accents.length]);
  });

  updateLyricBar();
  buildPreviewThemeButtons();

  if (syncParticles) {
    heroField.setColors(found.spectrum);
    workshopField.setColors(found.spectrum);
    darkField.setColors(found.spectrum);
    document.querySelectorAll('.spectrum-chip').forEach((chip) => {
      chip.classList.toggle('is-active', chip.getAttribute('data-spectrum') === found.id);
    });
  }
}

function updateLyricBar() {
  const lyric = currentTheme.lyrics[currentLyricIndex % currentTheme.lyrics.length];
  document.getElementById('lyric-song').textContent = `♪ ${lyric.song}`;
  document.getElementById('lyric-line').textContent = lyric.line;
}

document.getElementById('lyric-bar').addEventListener('click', () => {
  currentLyricIndex = (currentLyricIndex + 1) % currentTheme.lyrics.length;
  updateLyricBar();
});

document.querySelectorAll('[data-ui-mode]').forEach((btn) => {
  btn.addEventListener('click', () => {
    currentUiStyle = btn.getAttribute('data-ui-mode');
    phoneShell.setAttribute('data-ui-style', currentUiStyle);
    document.getElementById('phone-material-pill').textContent = currentUiStyle === 'glass' ? 'LIQUID GLASS' : 'CLASSIC PURE';
    document.querySelectorAll('[data-ui-mode]').forEach((b) => b.classList.toggle('is-active', b === btn));
  });
});

// 28 点阵通知卡「已到教室」交互演示
const arrivedBtn = document.getElementById('arrived-btn');
const dotNotifCard = document.getElementById('dot-notif-card');
let notifDismissed = false;
arrivedBtn.addEventListener('click', () => {
  notifDismissed = !notifDismissed;
  if (notifDismissed) {
    dotNotifCard.classList.add('is-dismissed');
    document.getElementById('notif-title').textContent = '已收起提醒 · 祝上课愉快';
    document.getElementById('notif-sub').textContent = '状态栏胶囊已静默 · 下节课前 15 分钟自动唤醒';
    arrivedBtn.textContent = '恢复预览';
    renderDotMatrix(28);
  } else {
    dotNotifCard.classList.remove('is-dismissed');
    document.getElementById('notif-title').textContent = '正在进行 · 物理实验长课';
    document.getElementById('notif-sub').textContent = '09:50–12:15 · 实验楼 401 · 进度 64%';
    arrivedBtn.textContent = '已到教室';
    renderDotMatrix(18);
  }
});

// Hero 光谱快捷切换条
document.querySelectorAll('.spectrum-chip').forEach((chip) => {
  chip.addEventListener('click', () => {
    const key = chip.getAttribute('data-spectrum');
    document.querySelectorAll('.spectrum-chip').forEach((c) => c.classList.toggle('is-active', c === chip));
    if (key === 'antigravity') {
      heroField.setColors(ANTIGRAVITY_SPECTRUM);
      workshopField.setColors(ANTIGRAVITY_SPECTRUM);
      darkField.setColors(DARK_CTA_SPECTRUM);
    } else {
      applyTheme(key, true);
    }
  });
});

// 预览剧场悬浮胶囊光标跟随
const cursorPill = document.getElementById('cursor-pill');
theaterFrame.addEventListener('pointermove', (e) => {
  if (reducedMotionQuery.matches) return;
  const rect = theaterFrame.getBoundingClientRect();
  const x = e.clientX - rect.left + 16;
  const y = e.clientY - rect.top + 16;
  cursorPill.style.opacity = '1';
  cursorPill.style.transform = `translate3d(${x}px, ${y}px, 0)`;
}, { passive: true });
theaterFrame.addEventListener('pointerleave', () => {
  cursorPill.style.opacity = '0';
});

renderDotMatrix(18);
applyTheme('ZTMY', false);

// ============================================================
// 5. 高校教务系统标签墙筛选
// ============================================================
const portalWall = document.getElementById('portal-wall');
function renderPortals(filter) {
  portalWall.innerHTML = '';
  const items = filter === 'all'
    ? UNIVERSITY_PORTALS
    : UNIVERSITY_PORTALS.filter((p) => p.category === filter);
  items.forEach((p) => {
    const chip = document.createElement('span');
    chip.className = 'portal-chip';
    chip.innerHTML = `<span>${p.school}</span><em>${p.system}</em>`;
    portalWall.appendChild(chip);
  });
}

document.querySelectorAll('.portal-filter-btn').forEach((btn) => {
  btn.addEventListener('click', () => {
    document.querySelectorAll('.portal-filter-btn').forEach((b) => b.classList.toggle('is-active', b === btn));
    renderPortals(btn.getAttribute('data-system'));
  });
});
renderPortals('all');

// ============================================================
// 6. 唱片柜 Carousel 轮播控制（严格遵循 modern-web-guidance carousel）
// ============================================================
const track = document.getElementById('vinyl-track');
const markersNav = document.getElementById('carousel-markers');
const statusRegion = document.getElementById('carousel-status');
const prevBtn = document.getElementById('carousel-prev');
const nextBtn = document.getElementById('carousel-next');

let visibleSlides = [];
let settledIndex = 0;
let pendingIndex = null;

function buildVinylSlides() {
  track.innerHTML = '';
  ARTIST_THEMES.forEach((theme, idx) => {
    const slide = document.createElement('article');
    slide.className = 'vinyl-slide';
    slide.setAttribute('role', 'group');
    slide.setAttribute('aria-roledescription', 'slide');
    slide.setAttribute('data-genre', theme.genre);
    slide.setAttribute('data-theme-id', theme.id);
    slide.setAttribute('aria-label', `第 ${idx + 1} 张，共 ${ARTIST_THEMES.length} 张`);

    const lyric = theme.lyrics[0];
    const sleeveTextColor = theme.isDark ? theme.text : '#211A14';
    slide.innerHTML = `
      <div>
        <div class="vinyl-artwork">
          <div class="vinyl-sleeve" style="background: linear-gradient(145deg, ${theme.surface}, ${theme.surfaceVariant}); color: ${sleeveTextColor}; border: 1px solid ${theme.border};">
            <span style="font-size: 11px; font-family: var(--font-mono); color: ${theme.primary}; font-weight: 700;">${theme.badgeText}</span>
            <div>
              <div style="font-size: 17px; font-weight: 700; line-height: 1.2;">${theme.displayName}</div>
              <div style="font-size: 12px; opacity: 0.78; margin-top: 2px;">${theme.artistName}</div>
            </div>
          </div>
          <div class="vinyl-disc-peek" style="--disc-accent: ${theme.primary};" aria-hidden="true"></div>
        </div>
        <div class="vinyl-swatches" style="margin-top: 16px;">
          <span class="swatch-dot" style="background: ${theme.bg};" title="背景色"></span>
          <span class="swatch-dot" style="background: ${theme.primary};" title="主色调"></span>
          <span class="swatch-dot" style="background: ${theme.secondary};" title="副色调"></span>
          <span class="swatch-dot" style="background: ${theme.tertiary};" title="点缀色"></span>
          <span style="font-size: 12px; color: var(--ink-subtle); margin-left: auto;">${theme.genreLabel} · ${theme.isDark ? '暗夜' : '浅色'}</span>
        </div>
        <p class="vinyl-quote" style="margin-top: 12px;">「${lyric.line}」</p>
      </div>
      <div>
        <button type="button" class="pill-btn pill-btn--secondary pill-btn--compact apply-album-btn" data-theme-id="${theme.id}" style="width: 100%;">
          应用至预览台与粒子星环
        </button>
      </div>
    `;
    track.appendChild(slide);
  });

  track.querySelectorAll('.apply-album-btn').forEach((btn) => {
    btn.addEventListener('click', () => {
      applyTheme(btn.getAttribute('data-theme-id'), true);
      document.getElementById('preview').scrollIntoView({
        behavior: reducedMotionQuery.matches ? 'auto' : 'smooth',
        block: 'start'
      });
    });
  });

  syncVisibleSlides();
}

function syncVisibleSlides() {
  visibleSlides = Array.from(track.children).filter((el) => !el.hidden);
  visibleSlides.forEach((slide, i) => {
    slide.setAttribute('aria-label', `第 ${i + 1} 张，共 ${visibleSlides.length} 张`);
  });

  markersNav.innerHTML = '';
  visibleSlides.forEach((slide, i) => {
    const markerBtn = document.createElement('button');
    markerBtn.type = 'button';
    markerBtn.className = 'carousel-marker-btn';
    markerBtn.setAttribute('aria-label', `跳转到第 ${i + 1} 张专辑主题`);
    if (i === 0) markerBtn.setAttribute('aria-current', 'true');
    markerBtn.innerHTML = '<span class="marker-pill"></span>';
    markerBtn.addEventListener('click', () => scrollToSlide(i));
    markersNav.appendChild(markerBtn);
  });

  settledIndex = 0;
  pendingIndex = null;
  track.scrollTo({ left: 0, behavior: 'auto' });
  updateMarkers(0);
}

function updateMarkers(activeIdx) {
  const buttons = markersNav.querySelectorAll('.carousel-marker-btn');
  buttons.forEach((b, i) => {
    if (i === activeIdx) {
      b.setAttribute('aria-current', 'true');
    } else {
      b.removeAttribute('aria-current');
    }
  });
}

function nearestSlideIndex() {
  if (!visibleSlides.length) return 0;
  const trackCenter = track.scrollLeft + track.clientWidth / 2;
  let bestIdx = 0;
  let bestDist = Infinity;
  visibleSlides.forEach((slide, i) => {
    const slideCenter = slide.offsetLeft + slide.offsetWidth / 2;
    const d = Math.abs(slideCenter - trackCenter);
    if (d < bestDist) {
      bestDist = d;
      bestIdx = i;
    }
  });
  return bestIdx;
}

function scrollToSlide(targetIdx) {
  if (!visibleSlides.length) return;
  const wrapped = (targetIdx + visibleSlides.length) % visibleSlides.length;
  pendingIndex = wrapped;
  updateMarkers(wrapped);

  const slide = visibleSlides[wrapped];
  const targetLeft = slide.offsetLeft - (track.clientWidth - slide.offsetWidth) / 2;
  track.scrollTo({
    left: Math.max(0, targetLeft),
    behavior: reducedMotionQuery.matches ? 'auto' : 'smooth'
  });
}

prevBtn.addEventListener('click', () => {
  const base = pendingIndex !== null ? pendingIndex : nearestSlideIndex();
  scrollToSlide(base - 1);
});

nextBtn.addEventListener('click', () => {
  const base = pendingIndex !== null ? pendingIndex : nearestSlideIndex();
  scrollToSlide(base + 1);
});

let rAFScheduled = false;
track.addEventListener('scroll', () => {
  if (pendingIndex !== null || rAFScheduled) return;
  rAFScheduled = true;
  requestAnimationFrame(() => {
    rAFScheduled = false;
    const idx = nearestSlideIndex();
    updateMarkers(idx);
  });
}, { passive: true });

function handleScrollSettled() {
  const idx = nearestSlideIndex();
  pendingIndex = null;
  updateMarkers(idx);
  if (idx !== settledIndex && visibleSlides[idx]) {
    settledIndex = idx;
    const title = visibleSlides[idx].querySelector('div[style*="font-weight: 700"]')?.textContent || '';
    statusRegion.textContent = `当前展示第 ${idx + 1} 张，共 ${visibleSlides.length} 张：${title}`;
  }
}

if ('onscrollend' in window) {
  track.addEventListener('scrollend', handleScrollSettled);
} else {
  let scrollDebounce = 0;
  track.addEventListener('scroll', () => {
    clearTimeout(scrollDebounce);
    scrollDebounce = setTimeout(handleScrollSettled, 140);
  }, { passive: true });
}

document.querySelectorAll('.genre-btn').forEach((btn) => {
  btn.addEventListener('click', () => {
    const genre = btn.getAttribute('data-genre');
    document.querySelectorAll('.genre-btn').forEach((b) => b.classList.toggle('is-active', b === btn));
    Array.from(track.children).forEach((slide) => {
      slide.hidden = genre !== 'ALL' && slide.getAttribute('data-genre') !== genre;
    });
    syncVisibleSlides();
  });
});

buildVinylSlides();

// ============================================================
// 7. OAuth-Callback 同款代码工坊 Tab 与绿色复制确认反馈
// ============================================================
const workshopCode = document.getElementById('workshop-code');
const workshopCopyBtn = document.getElementById('workshop-copy-btn');
let activeTabKey = 'build';
let copyTimer = 0;

document.querySelectorAll('.workshop-tab').forEach((tabBtn) => {
  tabBtn.addEventListener('click', () => {
    activeTabKey = tabBtn.getAttribute('data-tab');
    const data = WORKSHOP_TABS[activeTabKey];
    workshopCode.textContent = data.code;
    workshopCopyBtn.textContent = data.btnText;
    workshopCopyBtn.classList.remove('copied');

    document.querySelectorAll('.workshop-tab').forEach((b) => {
      const active = b === tabBtn;
      b.classList.toggle('is-active', active);
      b.setAttribute('aria-selected', active ? 'true' : 'false');
    });
  });
});

workshopCopyBtn.addEventListener('click', async () => {
  const text = WORKSHOP_TABS[activeTabKey].code;
  try {
    await navigator.clipboard.writeText(text);
  } catch {
    const ta = document.createElement('textarea');
    ta.value = text;
    ta.style.position = 'fixed';
    ta.style.opacity = '0';
    document.body.appendChild(ta);
    ta.select();
    document.execCommand('copy');
    document.body.removeChild(ta);
  }
  clearTimeout(copyTimer);
  workshopCopyBtn.classList.add('copied');
  workshopCopyBtn.textContent = '✓ 已复制到剪贴板';
  copyTimer = setTimeout(() => {
    workshopCopyBtn.classList.remove('copied');
    workshopCopyBtn.textContent = WORKSHOP_TABS[activeTabKey].btnText;
  }, 2200);
});
