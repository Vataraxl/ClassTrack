// ============================================================
// 1. 精确取自 ClassTrack 源码的 7 套音乐人专辑主题与预置高校数据
// ============================================================
const ARTIST_THEMES = [
  {
    id: 'ZTMY',
    displayName: '真夜霓虹',
    artistName: 'ずっと真夜中でいいのに。',
    badgeText: 'ZTMY · MIDNIGHT',
    genreLabel: '日音',
    isDark: true,
    bg: '#0B0D14',
    surface: '#131722',
    surfaceVariant: '#1C2232',
    primary: '#10E898',
    secondary: '#FF477E',
    tertiary: '#8B5CF6',
    text: '#F3F5FA',
    border: '#232A3E',
    glassBg: 'rgba(19, 23, 34, 0.72)',
    glassBorder: 'rgba(255, 255, 255, 0.22)',
    tracks: ['#1E1B3A', '#0F2E28', '#33182C', '#132842', '#332417', '#1B2436'],
    accents: ['#A78BFA', '#34D399', '#FB7185', '#38BDF8', '#FBBF24', '#818CF8'],
    spectrum: ['#10E898', '#FF477E', '#8B5CF6'],
    lyrics: [
      { song: '秒針を噛む · Track 01', line: 'このまま奪って隠して忘れたい、分かり合う○と×もなくて。' },
      { song: 'お勉強しといてよ · Study Mode', line: '理性的な感情論が綺麗で、また迷ってしまうけど。' }
    ]
  },
  {
    id: 'JAY_CHOU',
    displayName: '叶惠美·范特西',
    artistName: '周杰伦 (Jay Chou)',
    badgeText: 'JAY · FANTASY',
    genreLabel: '华语',
    isDark: false,
    bg: '#FAF6EE',
    surface: '#FFFFFF',
    surfaceVariant: '#F2EBDC',
    primary: '#B45309',
    secondary: '#235347',
    tertiary: '#991B1B',
    text: '#211A14',
    border: '#E6DEC8',
    glassBg: 'rgba(255, 252, 246, 0.78)',
    glassBorder: 'rgba(255, 255, 255, 0.92)',
    tracks: ['#FEF3E2', '#E8F3EE', '#FCECEB', '#EDF1F8', '#F5EEF9', '#F4EFE6'],
    accents: ['#B45309', '#1E5643', '#B91C1C', '#2A4E82', '#6B3FA0', '#7C4D25'],
    spectrum: ['#B45309', '#235347', '#991B1B'],
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
    genreLabel: '华语',
    isDark: true,
    bg: '#0C0D12',
    surface: '#151720',
    surfaceVariant: '#1E2230',
    primary: '#FF6B1A',
    secondary: '#F59E0B',
    tertiary: '#38BDF8',
    text: '#F8F9FC',
    border: '#272C3F',
    glassBg: 'rgba(21, 23, 32, 0.72)',
    glassBorder: 'rgba(255, 255, 255, 0.20)',
    tracks: ['#331C12', '#2E2412', '#122738', '#231834', '#122C24', '#2E1622'],
    accents: ['#FF7A2E', '#FBBF24', '#38BDF8', '#A78BFA', '#34D399', '#FB7185'],
    spectrum: ['#FF6B1A', '#F59E0B', '#38BDF8'],
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
    genreLabel: '欧美',
    isDark: true,
    bg: '#0D1226',
    surface: '#151C38',
    surfaceVariant: '#1E284D',
    primary: '#F472B6',
    secondary: '#A78BFA',
    tertiary: '#38BDF8',
    text: '#F8F7FF',
    border: '#283463',
    glassBg: 'rgba(21, 28, 56, 0.72)',
    glassBorder: 'rgba(255, 255, 255, 0.22)',
    tracks: ['#321A36', '#221C42', '#142847', '#143036', '#33261A', '#341827'],
    accents: ['#F472B6', '#C084FC', '#60A5FA', '#2DD4BF', '#FBBF24', '#FB7185'],
    spectrum: ['#F472B6', '#A78BFA', '#38BDF8'],
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
    genreLabel: 'K-Pop',
    isDark: true,
    bg: '#090B10',
    surface: '#121521',
    surfaceVariant: '#1B2032',
    primary: '#C084FC',
    secondary: '#22D3EE',
    tertiary: '#F43F5E',
    text: '#F4F6FB',
    border: '#262D45',
    glassBg: 'rgba(18, 21, 33, 0.72)',
    glassBorder: 'rgba(255, 255, 255, 0.22)',
    tracks: ['#26173E', '#102A36', '#331528', '#17223B', '#122E28', '#2A2136'],
    accents: ['#C084FC', '#22D3EE', '#FB7185', '#818CF8', '#34D399', '#E879F9'],
    spectrum: ['#C084FC', '#22D3EE', '#F43F5E'],
    lyrics: [
      { song: 'Supernova · Hyperstellar', line: 'Event horizon, 사건의 지평선 너머로, Supernova! Nova, can\'t stop hyperstellar.' },
      { song: 'Whiplash · Think Fast', line: 'One look give \'em Whiplash! Beat drop with a big flash, 집중해 좀 더 Think fast.' }
    ]
  },
  {
    id: 'YORUSHIKA',
    displayName: '盗作之夏',
    artistName: 'ヨルシカ (Yorushika)',
    badgeText: 'YORUSHIKA · SUMMER',
    genreLabel: '日音',
    isDark: false,
    bg: '#F5F7FA',
    surface: '#FFFFFF',
    surfaceVariant: '#EEF2F7',
    primary: '#2563EB',
    secondary: '#0D9488',
    tertiary: '#E15B45',
    text: '#0F172A',
    border: '#E2E8F0',
    glassBg: 'rgba(255, 255, 255, 0.80)',
    glassBorder: 'rgba(255, 255, 255, 0.94)',
    tracks: ['#EBF3FE', '#E6F7F4', '#FEF0ED', '#EEF0FF', '#FEF7E6', '#EAF6FA'],
    accents: ['#1D4ED8', '#0F766E', '#C2410C', '#4338CA', '#B45309', '#0369A1'],
    spectrum: ['#2563EB', '#0D9488', '#E15B45'],
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
    genreLabel: '日音',
    isDark: false,
    bg: '#F9F6F0',
    surface: '#FFFFFF',
    surfaceVariant: '#F1ECE1',
    primary: '#D95338',
    secondary: '#2B6E4E',
    tertiary: '#C88A4B',
    text: '#1F1A16',
    border: '#E6DFD3',
    glassBg: 'rgba(255, 252, 246, 0.80)',
    glassBorder: 'rgba(255, 255, 255, 0.94)',
    tracks: ['#FCEEE9', '#E7F3EC', '#FBF3E0', '#F2ECF8', '#E8F2F5', '#F5ECE4'],
    accents: ['#C84328', '#1E5E3F', '#9E6B14', '#5E3F7A', '#1F6378', '#7D4829'],
    spectrum: ['#D95338', '#2B6E4E', '#C88A4B'],
    lyrics: [
      { song: '満ちてゆく · Overflowing', line: '手を放す、軽くなる、満ちてゆく。' },
      { song: 'きらり · Kirari Groove', line: '新しい日々も拙い過去も、全部がきらり。' }
    ]
  }
];

const ANTIGRAVITY_LIGHT_COLORS = ['#2c64ed', '#f84242', '#ffcf03'];
const ANTIGRAVITY_DARK_COLORS = ['#7189ff', '#3074f9', '#05070e'];

const UNIVERSITY_PORTALS = [
  ['对外经济贸易大学', '强智教务'],
  ['对外经贸 bkjw 新端', 'UIBE 本科教务'],
  ['北京林业大学', '强智教务'],
  ['中国农业大学', '强智/综合'],
  ['北京邮电大学', '正方/综合'],
  ['北京航空航天大学', '综合教务'],
  ['北京理工大学', '金智/综合'],
  ['中国人民大学', '金智/综合'],
  ['北京师范大学', '青果教务'],
  ['北京科技大学', '强智教务'],
  ['北京交通大学', '树维/URP'],
  ['浙江大学', '新正方教务'],
  ['武汉大学', '新正方/综合'],
  ['华中科技大学', '综合教务'],
  ['复旦大学', '复旦教务'],
  ['上海交通大学', '上交综合'],
  ['南京大学', '南大教务']
];

const WORKSHOP_TABS = {
  build: {
    btnText: '复制构建命令',
    code: `git clone https://github.com/Vataraxl/ClassTrack.git
cd ClassTrack
./gradlew assembleDebug`
  },
  theme: {
    btnText: '复制自定义主题 JSON',
    code: `{
  "themeName": "群青夜游",
  "artistName": "YOASOBI",
  "isDark": true,
  "primaryHex": "#38BDF8",
  "secondaryHex": "#F43F5E",
  "tertiaryHex": "#A855F7",
  "backgroundHex": "#0B1021"
}`
  },
  stack: {
    btnText: '复制技术栈清单',
    code: `Kotlin 2.3.20 · JVM 21 · AGP 9.0.1 · Gradle 9.1
Jetpack Compose (BOM 2026.03.01) · Material 3 · Navigation 3
minSdk 26 · compileSdk 36 · 100% 本地 SQLite`
  }
};

// ============================================================
// 2. 原生 WebGL GLSL 粒子星环引擎 (1:1 复刻 MainParticlesComponent)
// ============================================================
const NOISE_GLSL = `
vec3 mod289(vec3 x) { return x - floor(x * (1.0 / 289.0)) * 289.0; }
vec4 mod289(vec4 x) { return x - floor(x * (1.0 / 289.0)) * 289.0; }
vec4 permute(vec4 x) { return mod289(((x * 34.0) + 1.0) * x); }
vec4 taylorInvSqrt(vec4 r) { return 1.79284291400159 - 0.85373472095314 * r; }
float snoise(vec3 v) {
  const vec2 C = vec2(1.0 / 6.0, 1.0 / 3.0);
  const vec4 D = vec4(0.0, 0.5, 1.0, 2.0);
  vec3 i = floor(v + dot(v, C.yyy));
  vec3 x0 = v - i + dot(i, C.xxx);
  vec3 g = step(x0.yzx, x0.xyz);
  vec3 l = 1.0 - g;
  vec3 i1 = min(g.xyz, l.zxy);
  vec3 i2 = max(g.xyz, l.zxy);
  vec3 x1 = x0 - i1 + C.xxx;
  vec3 x2 = x0 - i2 + C.yyy;
  vec3 x3 = x0 - D.yyy;
  i = mod289(i);
  vec4 p = permute(permute(permute(
    i.z + vec4(0.0, i1.z, i2.z, 1.0))
    + i.y + vec4(0.0, i1.y, i2.y, 1.0))
    + i.x + vec4(0.0, i1.x, i2.x, 1.0));
  float n_ = 0.142857142857;
  vec3 ns = n_ * D.wyz - D.xzx;
  vec4 j = p - 49.0 * floor(p * ns.z * ns.z);
  vec4 x_ = floor(j * ns.z);
  vec4 y_ = floor(j - 7.0 * x_);
  vec4 x = x_ * ns.x + ns.yyyy;
  vec4 y = y_ * ns.x + ns.yyyy;
  vec4 h = 1.0 - abs(x) - abs(y);
  vec4 b0 = vec4(x.xy, y.xy);
  vec4 b1 = vec4(x.zw, y.zw);
  vec4 s0 = floor(b0) * 2.0 + 1.0;
  vec4 s1 = floor(b1) * 2.0 + 1.0;
  vec4 sh = -step(h, vec4(0.0));
  vec4 a0 = b0.xzyw + s0.xzyw * sh.xxyy;
  vec4 a1 = b1.xzyw + s1.xzyw * sh.zzww;
  vec3 p0 = vec3(a0.xy, h.x);
  vec3 p1 = vec3(a0.zw, h.y);
  vec3 p2 = vec3(a1.xy, h.z);
  vec3 p3 = vec3(a1.zw, h.w);
  vec4 norm = taylorInvSqrt(vec4(dot(p0, p0), dot(p1, p1), dot(p2, p2), dot(p3, p3)));
  p0 *= norm.x; p1 *= norm.y; p2 *= norm.z; p3 *= norm.w;
  vec4 m = max(0.6 - vec4(dot(x0, x0), dot(x1, x1), dot(x2, x2), dot(x3, x3)), 0.0);
  m = m * m;
  return 42.0 * dot(m * m, vec4(dot(p0, x0), dot(p1, x1), dot(p2, x2), dot(p3, x3)));
}`;

const VERT_SRC = `
precision highp float;
attribute vec4 aParticle;
uniform float uAspect;
uniform float uPixelRatio;
uniform float uParticleScale;
varying vec2 vLocalPos;
varying float vScale;
varying float vVelocity;

void main() {
  vLocalPos = aParticle.xy;
  vScale = aParticle.z;
  vVelocity = aParticle.w;
  gl_Position = vec4(aParticle.x / uAspect, aParticle.y, 0.0, 1.0);
  gl_PointSize = clamp((vScale * 8.2) * (uPixelRatio * 0.65) * uParticleScale, 1.8, 28.0);
}`;

const FRAG_SRC = `
precision highp float;
varying vec2 vLocalPos;
varying float vScale;
varying float vVelocity;
uniform vec3 uColor1;
uniform vec3 uColor2;
uniform vec3 uColor3;
uniform vec2 uRingPos;
uniform float uTime;
uniform float uAlpha;
uniform float uIsLight;
${NOISE_GLSL}

float sdRoundBox(in vec2 p, in vec2 b, in vec4 r) {
  r.xy = (p.x > 0.0) ? r.xy : r.zw;
  r.x = (p.y > 0.0) ? r.x : r.y;
  vec2 q = abs(p) - b + r.x;
  return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r.x;
}

vec2 rotate2d(vec2 v, float a) {
  float s = sin(a);
  float c = cos(a);
  return mat2(c, s, -s, c) * v;
}

void main() {
  float noiseAngle = snoise(vec3(vLocalPos * 5.2 + vec2(18.4924, 72.9744), uTime * 0.65));
  float noiseColor = snoise(vec3(vLocalPos * 1.15 + vec2(74.664, 91.556), uTime * 0.35));
  noiseColor = (noiseColor + 1.0) * 0.5;

  float angle = atan(vLocalPos.y - uRingPos.y, vLocalPos.x - uRingPos.x);
  vec2 uv = gl_PointCoord.xy - vec2(0.5);
  uv.y *= -1.0;
  uv = rotate2d(uv, -angle + (noiseAngle * 0.45));

  float h = 0.72;
  float progress = smoothstep(0.05, 0.92, pow(noiseColor, 1.35));
  vec3 col = mix(
    mix(uColor1, uColor2, clamp(progress / h, 0.0, 1.0)),
    mix(uColor2, uColor3, clamp((progress - h) / (1.0 - h), 0.0, 1.0)),
    step(h, progress)
  );

  float rounded = sdRoundBox(uv, vec2(0.46, 0.19), vec4(0.22));
  rounded = smoothstep(0.11, 0.0, rounded);

  float a = uAlpha * rounded * smoothstep(0.08, 0.25, vScale);
  if (a < 0.015) discard;

  vec3 finalCol = clamp(col, 0.0, 1.0);
  if (uIsLight > 0.5) {
    finalCol = mix(finalCol * 1.1, finalCol * clamp(0.86 + vVelocity * 0.35, 0.86, 1.05), 0.25);
  }
  gl_FragColor = vec4(finalCol * a, a);
}`;

function hexToRgbNorm(hex) {
  const clean = hex.replace('#', '');
  const n = parseInt(clean.length === 3 ? clean.split('').map((c) => c + c).join('') : clean, 16);
  return [((n >> 16) & 255) / 255, ((n >> 8) & 255) / 255, (n & 255) / 255];
}

function smoothstep(edge0, edge1, x) {
  const t = Math.max(0, Math.min(1, (x - edge0) / (edge1 - edge0)));
  return t * t * (3 - 2 * t);
}

class WebGLParticleField {
  constructor(canvas, options = {}) {
    this.canvas = canvas;
    this.theme = options.theme || 'light'; // 'light' | 'dark'
    this.ringMode = options.ringMode || 'hero'; // 'hero' | 'halo' | 'split' | 'dark'
    this.colors = (options.colors || ANTIGRAVITY_LIGHT_COLORS).map(hexToRgbNorm);
    this.particleScale = options.particleScale || 1.0;

    this.dpr = Math.min(window.devicePixelRatio || 1, 2);
    this.width = 0;
    this.height = 0;
    this.aspect = 1;
    this.count = 0;
    this.refs = null;
    this.simData = null;

    this.pointer = { x: 0, y: 0, active: false };
    this.ringPos = { x: 0, y: 0 };
    this.time = 0;
    this.isVisible = true;
    this.running = false;
    this.rafId = 0;

    this.gl = canvas.getContext('webgl', {
      alpha: true,
      antialias: true,
      powerPreference: 'high-performance'
    });

    if (this.gl) {
      this.initGL();
    } else {
      this.ctx2d = canvas.getContext('2d');
    }

    this.resize();
    this.seedPoints();
    this.stepSimulation(0);
    this.draw();
  }

  initGL() {
    const gl = this.gl;
    const compile = (type, src) => {
      const s = gl.createShader(type);
      gl.shaderSource(s, src);
      gl.compileShader(s);
      if (!gl.getShaderParameter(s, gl.COMPILE_STATUS)) {
        throw new Error(gl.getShaderInfoLog(s));
      }
      return s;
    };
    const prog = gl.createProgram();
    gl.attachShader(prog, compile(gl.VERTEX_SHADER, VERT_SRC));
    gl.attachShader(prog, compile(gl.FRAGMENT_SHADER, FRAG_SRC));
    gl.linkProgram(prog);
    gl.useProgram(prog);

    this.program = prog;
    this.buffer = gl.createBuffer();
    this.locAttr = gl.getAttribLocation(prog, 'aParticle');
    this.uAspect = gl.getUniformLocation(prog, 'uAspect');
    this.uPixelRatio = gl.getUniformLocation(prog, 'uPixelRatio');
    this.uParticleScale = gl.getUniformLocation(prog, 'uParticleScale');
    this.uColor1 = gl.getUniformLocation(prog, 'uColor1');
    this.uColor2 = gl.getUniformLocation(prog, 'uColor2');
    this.uColor3 = gl.getUniformLocation(prog, 'uColor3');
    this.uRingPos = gl.getUniformLocation(prog, 'uRingPos');
    this.uTime = gl.getUniformLocation(prog, 'uTime');
    this.uAlpha = gl.getUniformLocation(prog, 'uAlpha');
    this.uIsLight = gl.getUniformLocation(prog, 'uIsLight');

    gl.enable(gl.BLEND);
    gl.blendFunc(gl.ONE, gl.ONE_MINUS_SRC_ALPHA);
  }

  setColors(hexList) {
    this.colors = hexList.slice(0, 3).map(hexToRgbNorm);
    if (!this.running) this.draw();
  }

  resize() {
    const parent = this.canvas.parentElement;
    if (!parent) return;
    const rect = parent.getBoundingClientRect();
    this.dpr = Math.min(window.devicePixelRatio || 1, 2);
    this.width = Math.max(280, Math.round(rect.width));
    this.height = Math.max(280, Math.round(rect.height));
    this.aspect = this.width / this.height;
    this.canvas.width = Math.round(this.width * this.dpr);
    this.canvas.height = Math.round(this.height * this.dpr);
    if (this.gl) {
      this.gl.viewport(0, 0, this.canvas.width, this.canvas.height);
    } else if (this.ctx2d) {
      this.ctx2d.setTransform(this.dpr, 0, 0, this.dpr, 0, 0);
    }
  }

  seedPoints() {
    const isNarrow = this.width < 700;
    const cols = this.ringMode === 'hero' || this.ringMode === 'dark'
      ? (isNarrow ? 34 : 76)
      : (isNarrow ? 28 : 48);
    const rows = this.ringMode === 'hero' || this.ringMode === 'dark'
      ? (isNarrow ? 38 : 48)
      : (isNarrow ? 28 : 42);

    this.count = cols * rows;
    this.refs = new Float32Array(this.count * 2);
    this.simData = new Float32Array(this.count * 4);

    const spanX = this.aspect * 1.12;
    const spanY = 1.12;
    let idx = 0;

    for (let r = 0; r < rows; r++) {
      for (let c = 0; c < cols; c++) {
        const s1 = Math.sin(r * 12.9898 + c * 78.233) * 43758.5453;
        const s2 = Math.sin(r * 39.346 + c * 11.135) * 24634.6345;
        const jx = s1 - Math.floor(s1) - 0.5;
        const jy = s2 - Math.floor(s2) - 0.5;

        const rx = (((c + 0.5 + jx * 0.9) / cols) * 2 - 1) * spanX;
        const ry = (((r + 0.5 + jy * 0.9) / rows) * 2 - 1) * spanY;

        this.refs[idx * 2] = rx;
        this.refs[idx * 2 + 1] = ry;
        this.simData[idx * 4] = rx;
        this.simData[idx * 4 + 1] = ry;
        this.simData[idx * 4 + 2] = 0.25;
        this.simData[idx * 4 + 3] = 0.5;
        idx++;
      }
    }

    if (this.gl) {
      const gl = this.gl;
      gl.bindBuffer(gl.ARRAY_BUFFER, this.buffer);
      gl.bufferData(gl.ARRAY_BUFFER, this.simData, gl.DYNAMIC_DRAW);
    }
  }

  updatePointer(clientX, clientY) {
    const rect = this.canvas.getBoundingClientRect();
    if (!rect.width || !rect.height) return;
    const nx = ((clientX - rect.left) / rect.width) * 2 - 1;
    const ny = -(((clientY - rect.top) / rect.height) * 2 - 1);
    this.pointer.x = nx * this.aspect * 0.55;
    this.pointer.y = ny * 0.55;
    this.pointer.active = true;
  }

  clearPointer() {
    this.pointer.active = false;
  }

  stepSimulation(t) {
    const idleX = Math.sin(t * 0.42) * 0.18;
    const idleY = Math.cos(t * 0.34) * 0.11;
    const tx = this.pointer.active ? this.pointer.x : idleX;
    const ty = this.pointer.active ? this.pointer.y : idleY;
    const lerp = this.pointer.active ? 0.032 : 0.015;

    this.ringPos.x += (tx - this.ringPos.x) * lerp;
    this.ringPos.y += (ty - this.ringPos.y) * lerp;

    const baseR = this.ringMode === 'halo' ? 0.48 : (this.ringMode === 'split' ? 0.48 : 0.56);
    const ringRadius = baseR + Math.sin(t * 0.95) * 0.06 + Math.cos(t * 2.6) * 0.035;
    const ringWidth = this.ringMode === 'halo' ? 0.24 : 0.27;
    const ringWidth2 = this.ringMode === 'halo' ? 0.12 : 0.11;
    const dispAmp = this.ringMode === 'halo' ? 0.18 : 0.11;

    for (let i = 0; i < this.count; i++) {
      const rx = this.refs[i * 2];
      const ry = this.refs[i * 2 + 1];

      const dx = rx - this.ringPos.x;
      const dy = ry - this.ringPos.y;
      const dist = Math.hypot(dx, dy) || 0.0001;

      const waveDispX = Math.sin(rx * 6.5 + t * 1.1) * 0.018 + Math.cos(ry * 12.0 - t * 0.7) * 0.007;
      const waveDispY = Math.cos(ry * 6.5 + t * 0.9) * 0.018 + Math.sin(rx * 12.0 + t * 0.7) * 0.007;

      const bandOuter = smoothstep(ringRadius - ringWidth * 1.7, ringRadius, dist) -
        smoothstep(ringRadius, ringRadius + ringWidth, dist);
      const bandSharp = smoothstep(ringRadius - ringWidth2 * 1.8, ringRadius, dist) -
        smoothstep(ringRadius, ringRadius + ringWidth2, dist);
      const innerSoft = this.ringMode === 'halo'
        ? 0.0
        : smoothstep(ringRadius + ringWidth2, ringRadius, dist) * 0.18;

      let targetScale = Math.pow(bandOuter, 1.8) * 0.85 + Math.pow(bandSharp, 2.2) * 1.45 + innerSoft;
      if (this.ringMode !== 'halo') {
        targetScale += 0.24; // 背景细微星尘基底
      } else if (dist < ringRadius * 0.56) {
        targetScale *= smoothstep(ringRadius * 0.42, ringRadius * 0.65, dist); // OAuth 弹窗中心留白
      }

      const prevScale = this.simData[i * 4 + 2];
      const nextScale = prevScale + (targetScale - prevScale) * 0.22;

      const pull = Math.pow(Math.max(0, bandSharp), 0.75) * dispAmp;
      const fx = rx + waveDispX + (dx / dist) * pull;
      const fy = ry + waveDispY + (dy / dist) * pull;

      this.simData[i * 4] = fx;
      this.simData[i * 4 + 1] = fy;
      this.simData[i * 4 + 2] = nextScale;
      this.simData[i * 4 + 3] = Math.min(1, nextScale * 0.65);
    }
  }

  draw() {
    if (this.gl) {
      const gl = this.gl;
      gl.useProgram(this.program);
      gl.clearColor(0, 0, 0, 0);
      gl.clear(gl.COLOR_BUFFER_BIT);

      gl.bindBuffer(gl.ARRAY_BUFFER, this.buffer);
      gl.bufferSubData(gl.ARRAY_BUFFER, 0, this.simData);
      gl.enableVertexAttribArray(this.locAttr);
      gl.vertexAttribPointer(this.locAttr, 4, gl.FLOAT, false, 0, 0);

      gl.uniform1f(this.uAspect, this.aspect);
      gl.uniform1f(this.uPixelRatio, this.dpr);
      gl.uniform1f(this.uParticleScale, this.particleScale);
      gl.uniform3fv(this.uColor1, this.colors[0]);
      gl.uniform3fv(this.uColor2, this.colors[1]);
      gl.uniform3fv(this.uColor3, this.colors[2]);
      gl.uniform2f(this.uRingPos, this.ringPos.x, this.ringPos.y);
      gl.uniform1f(this.uTime, this.time);
      gl.uniform1f(this.uAlpha, 1.0);
      gl.uniform1f(this.uIsLight, this.theme === 'light' ? 1.0 : 0.0);

      gl.drawArrays(gl.POINTS, 0, this.count);
      return;
    }

    // 极简 Canvas2D 降级
    const ctx = this.ctx2d;
    ctx.clearRect(0, 0, this.width, this.height);
    for (let i = 0; i < this.count; i += 2) {
      const x = (this.simData[i * 4] / this.aspect * 0.5 + 0.5) * this.width;
      const y = (-this.simData[i * 4 + 1] * 0.5 + 0.5) * this.height;
      const s = this.simData[i * 4 + 2];
      const [r, g, b] = this.colors[i % 3];
      ctx.fillStyle = `rgba(${Math.round(r * 255)},${Math.round(g * 255)},${Math.round(b * 255)},${Math.min(0.85, s * 0.55)})`;
      ctx.beginPath();
      ctx.arc(x, y, Math.max(1, s * 1.8), 0, Math.PI * 2);
      ctx.fill();
    }
  }

  start() {
    if (this.running) return;
    this.running = true;
    let last = performance.now();
    const tick = (now) => {
      if (!this.running) return;
      const dt = Math.min(0.05, (now - last) / 1000);
      last = now;
      if (this.isVisible) {
        this.time += dt;
        this.stepSimulation(this.time);
        this.draw();
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
}

// ============================================================
// 2b. 50/50 悬停聚拢变形粒子场 (1:1 复刻 MorphingParticlesComponent)
// ============================================================
const MORPH_VERT_SRC = `
precision highp float;
attribute vec4 aData; // x, y, scale, morph
uniform float uAspect;
uniform float uPixelRatio;
varying float vScale;
varying float vMorph;
varying vec2 vPos;

void main() {
  vScale = aData.z;
  vMorph = aData.w;
  vPos = aData.xy;
  gl_Position = vec4(aData.x / uAspect, aData.y, 0.0, 1.0);
  float basePt = mix(2.0, 2.7, vMorph);
  gl_PointSize = clamp((vScale * basePt + 0.8) * uPixelRatio, 1.5, 8.0);
}`;

const MORPH_FRAG_SRC = `
precision highp float;
varying float vScale;
varying float vMorph;
varying vec2 vPos;

void main() {
  vec2 uv = gl_PointCoord.xy - vec2(0.5);
  float d = length(uv);
  float circle = smoothstep(0.5, 0.34, d);
  if (circle < 0.02) discard;

  vec3 idleCol   = vec3(0.26, 0.28, 0.32);    // 深炭灰微点
  vec3 flyCol    = vec3(1.0, 0.275, 0.255);   // #FF4641 珊瑚红跃迁
  vec3 lockBlue  = vec3(0.204, 0.420, 0.945); // #346BF1 鲜亮电光蓝主色
  vec3 tintCyan  = vec3(0.220, 0.741, 0.973); // #38BDF8 局部冰晶青高光
  vec3 tintIndig = vec3(0.357, 0.353, 0.969); // #5B5AF7 局部霓虹靛微光

  float wave = clamp(0.5 + 0.35 * vPos.x + 0.35 * vPos.y, 0.0, 1.0);
  vec3 tintCol = mix(tintIndig, tintCyan, wave);
  vec3 lockCol = mix(lockBlue, tintCol, 0.16);

  float mid = smoothstep(0.08, 0.45, vMorph);
  float end = smoothstep(0.40, 0.78, vMorph);
  vec3 col = mix(mix(idleCol, flyCol, mid), lockCol, end);

  float a = circle * clamp(mix(0.68, 0.95, vMorph) * smoothstep(0.04, 0.32, vScale), 0.0, 1.0);
  if (a < 0.015) discard;
  gl_FragColor = vec4(col * a, a);
}`;

function drawRoundedRectPath(ctx, x, y, w, h, r) {
  const rad = Math.min(r, w * 0.5, h * 0.5);
  ctx.beginPath();
  ctx.moveTo(x + rad, y);
  ctx.lineTo(x + w - rad, y);
  ctx.quadraticCurveTo(x + w, y, x + w, y + rad);
  ctx.lineTo(x + w, y + h - rad);
  ctx.quadraticCurveTo(x + w, y + h, x + w - rad, y + h);
  ctx.lineTo(x + rad, y + h);
  ctx.quadraticCurveTo(x, y + h, x, y + h - rad);
  ctx.lineTo(x, y + rad);
  ctx.quadraticCurveTo(x, y, x + rad, y);
  ctx.closePath();
}

function sampleMorphTargets(shapeType, aspect) {
  const size = 500;
  const off = document.createElement('canvas');
  off.width = size;
  off.height = size;
  const ctx = off.getContext('2d');
  ctx.clearRect(0, 0, size, size);
  ctx.strokeStyle = '#000000';
  ctx.fillStyle = '#000000';
  ctx.lineCap = 'round';
  ctx.lineJoin = 'round';
  const isCompact = aspect < 0.95;

  if (shapeType === 'vinyl') {
    // ClassTrack 黑胶唱盘 + 双重声槽弧线 + 右侧唱臂唱头 + 左上跃动八分音符 ♪
    // 中心安全留白区避开文案与按钮；窄屏启用紧凑外扩几何避免擦碰标题
    const cx = 232;
    const cy = 250;

    // 1. 黑胶外盘环
    ctx.lineWidth = 17;
    ctx.beginPath();
    ctx.arc(cx, cy, isCompact ? 196 : 194, 0, Math.PI * 2);
    ctx.stroke();

    // 2. 上下双重声槽弧线
    if (isCompact) {
      ctx.lineWidth = 9;
      ctx.beginPath();
      ctx.arc(cx, cy, 166, -Math.PI * 0.70, -Math.PI * 0.30);
      ctx.stroke();
      ctx.beginPath();
      ctx.arc(cx, cy, 166, Math.PI * 0.30, Math.PI * 0.70);
      ctx.stroke();

      ctx.lineWidth = 7;
      ctx.beginPath();
      ctx.arc(cx, cy, 144, -Math.PI * 0.62, -Math.PI * 0.38);
      ctx.stroke();
      ctx.beginPath();
      ctx.arc(cx, cy, 144, Math.PI * 0.38, Math.PI * 0.62);
      ctx.stroke();
    } else {
      ctx.lineWidth = 9;
      ctx.beginPath();
      ctx.arc(cx, cy, 156, -Math.PI * 0.76, -Math.PI * 0.24);
      ctx.stroke();
      ctx.beginPath();
      ctx.arc(cx, cy, 156, Math.PI * 0.24, Math.PI * 0.76);
      ctx.stroke();

      ctx.lineWidth = 7;
      ctx.beginPath();
      ctx.arc(cx, cy, 126, -Math.PI * 0.69, -Math.PI * 0.31);
      ctx.stroke();
      ctx.beginPath();
      ctx.arc(cx, cy, 126, Math.PI * 0.31, Math.PI * 0.69);
      ctx.stroke();
    }

    // 3. 右侧唱机枢轴底座 + 折线唱臂 + 倾斜唱头
    ctx.lineWidth = 10;
    ctx.beginPath();
    ctx.arc(456, 78, 20, 0, Math.PI * 2);
    ctx.stroke();
    ctx.beginPath();
    ctx.arc(456, 78, 6, 0, Math.PI * 2);
    ctx.fill();

    ctx.lineWidth = 13;
    ctx.beginPath();
    ctx.moveTo(456, 102);
    ctx.lineTo(452, 296);
    ctx.lineTo(414, 372);
    ctx.stroke();

    // 唱头外壳块
    ctx.lineWidth = 22;
    ctx.beginPath();
    ctx.moveTo(421, 358);
    ctx.lineTo(408, 384);
    ctx.stroke();

    // 4. 左上角八分音符 ♪（位于盘环左上方外侧）
    ctx.save();
    ctx.translate(54, 102);
    ctx.rotate(-0.42);
    ctx.beginPath();
    ctx.ellipse(0, 0, 17, 12, 0, 0, Math.PI * 2);
    ctx.fill();
    ctx.restore();

    ctx.lineWidth = 11;
    ctx.beginPath();
    ctx.moveTo(67, 98);
    ctx.lineTo(67, 42);
    ctx.quadraticCurveTo(96, 50, 99, 75);
    ctx.stroke();
  } else {
    // ClassTrack 监听头戴式耳机 + 顶部课节状态胶囊 + 底部七柱课程节律频谱
    const cx = 250;
    const cy = isCompact ? 232 : 240;

    // 1. 头梁弧线
    ctx.lineWidth = 18;
    ctx.beginPath();
    ctx.arc(cx, cy, isCompact ? 196 : 192, -Math.PI * 0.93, -Math.PI * 0.07);
    ctx.stroke();

    // 头梁内衬减压垫细弧
    ctx.lineWidth = 8;
    ctx.beginPath();
    ctx.arc(cx, cy, isCompact ? 172 : 168, -Math.PI * 0.72, -Math.PI * 0.28);
    ctx.stroke();

    // 2. 顶部实时课节状态胶囊
    const capY = isCompact ? 82 : 106;
    const capCy = capY + 13;
    ctx.lineWidth = 9;
    drawRoundedRectPath(ctx, 182, capY, 136, isCompact ? 26 : 28, 13);
    ctx.stroke();
    ctx.beginPath();
    ctx.arc(202, capCy, 6.5, 0, Math.PI * 2);
    ctx.fill();
    ctx.lineWidth = 6.5;
    ctx.beginPath();
    ctx.moveTo(220, capCy);
    ctx.lineTo(298, capCy);
    ctx.stroke();

    // 3. 左右全包耳式耳罩 + 衔接臂 + 外侧声波弧
    if (isCompact) {
      ctx.lineWidth = 14;
      ctx.beginPath();
      ctx.moveTo(58, 192);
      ctx.lineTo(62, 206);
      ctx.moveTo(442, 192);
      ctx.lineTo(438, 206);
      ctx.stroke();

      drawRoundedRectPath(ctx, 40, 202, 34, 124, 17);
      ctx.fill();
      drawRoundedRectPath(ctx, 426, 202, 34, 124, 17);
      ctx.fill();

      ctx.lineWidth = 9;
      ctx.beginPath();
      ctx.arc(64, 264, 46, Math.PI * 0.79, Math.PI * 1.21);
      ctx.stroke();
      ctx.beginPath();
      ctx.arc(436, 264, 46, -Math.PI * 0.21, Math.PI * 0.21);
      ctx.stroke();
    } else {
      ctx.lineWidth = 14;
      ctx.beginPath();
      ctx.moveTo(62, 198);
      ctx.lineTo(66, 212);
      ctx.moveTo(438, 198);
      ctx.lineTo(434, 212);
      ctx.stroke();

      drawRoundedRectPath(ctx, 48, 206, 36, 128, 18);
      ctx.fill();
      drawRoundedRectPath(ctx, 416, 206, 36, 128, 18);
      ctx.fill();

      ctx.lineWidth = 9;
      ctx.beginPath();
      ctx.arc(70, 270, 50, Math.PI * 0.79, Math.PI * 1.21);
      ctx.stroke();
      ctx.beginPath();
      ctx.arc(430, 270, 50, -Math.PI * 0.21, Math.PI * 0.21);
      ctx.stroke();
    }

    // 4. 底部 7 柱课程频谱柱
    const eqBars = isCompact
      ? [
          [148, 424, 456],
          [182, 402, 460],
          [216, 390, 464],
          [250, 410, 454],
          [284, 388, 464],
          [318, 402, 460],
          [352, 424, 456]
        ]
      : [
          [148, 412, 448],
          [182, 388, 452],
          [216, 372, 456],
          [250, 396, 446],
          [284, 370, 456],
          [318, 388, 452],
          [352, 412, 448]
        ];
    ctx.lineWidth = 13;
    for (const [bx, byTop, byBottom] of eqBars) {
      ctx.beginPath();
      ctx.moveTo(bx, byTop);
      ctx.lineTo(bx, byBottom);
      ctx.stroke();
    }
  }

  const img = ctx.getImageData(0, 0, size, size).data;
  const targets = [];
  const span = isCompact ? Math.min(aspect * 0.96, 0.86) : Math.min(aspect * 0.92, 0.90);
  const halfSpanX = span;
  const halfSpanY = span;
  const step = 2;

  for (let y = 0; y < size; y += step) {
    for (let x = 0; x < size; x += step) {
      if (img[(y * size + x) * 4 + 3] > 90) {
        const jx = (Math.sin(x * 12.9 + y * 78.2) * 43758.5) % 1;
        const jy = (Math.sin(x * 39.3 + y * 11.1) * 24634.6) % 1;
        const nx = (((x + (jx - 0.5) * 1.6) / size) * 2 - 1) * halfSpanX;
        const ny = -((((y + (jy - 0.5) * 1.6) / size) * 2 - 1) * halfSpanY);
        targets.push([nx, ny]);
      }
    }
  }
  return targets;
}

class MorphingParticleField {
  constructor(canvas, shapeType) {
    this.canvas = canvas;
    this.shapeType = shapeType;
    this.dpr = Math.min(window.devicePixelRatio || 1, 2);
    this.width = 0;
    this.height = 0;
    this.aspect = 1;
    this.count = 0;
    this.isHovering = false;
    this.hoverT = 0;
    this.isVisible = true;
    this.running = false;
    this.rafId = 0;
    this.time = 0;

    this.gl = canvas.getContext('webgl', {
      alpha: true,
      antialias: true,
      powerPreference: 'high-performance'
    });

    if (this.gl) {
      this.initGL();
    } else {
      this.ctx2d = canvas.getContext('2d');
    }

    this.resize();
    this.seedPoints();
    this.step(0.016);
    this.draw();
  }

  initGL() {
    const gl = this.gl;
    const compile = (type, src) => {
      const s = gl.createShader(type);
      gl.shaderSource(s, src);
      gl.compileShader(s);
      if (!gl.getShaderParameter(s, gl.COMPILE_STATUS)) {
        throw new Error(gl.getShaderInfoLog(s));
      }
      return s;
    };
    const prog = gl.createProgram();
    gl.attachShader(prog, compile(gl.VERTEX_SHADER, MORPH_VERT_SRC));
    gl.attachShader(prog, compile(gl.FRAGMENT_SHADER, MORPH_FRAG_SRC));
    gl.linkProgram(prog);
    gl.useProgram(prog);

    this.program = prog;
    this.buffer = gl.createBuffer();
    this.locAttr = gl.getAttribLocation(prog, 'aData');
    this.uAspect = gl.getUniformLocation(prog, 'uAspect');
    this.uPixelRatio = gl.getUniformLocation(prog, 'uPixelRatio');

    gl.enable(gl.BLEND);
    gl.blendFunc(gl.ONE, gl.ONE_MINUS_SRC_ALPHA);
  }

  resize() {
    const parent = this.canvas.parentElement;
    if (!parent) return;
    const rect = parent.getBoundingClientRect();
    this.dpr = Math.min(window.devicePixelRatio || 1, 2);
    this.width = Math.max(280, Math.round(rect.width));
    this.height = Math.max(320, Math.round(rect.height));
    this.aspect = this.width / this.height;
    this.canvas.width = Math.round(this.width * this.dpr);
    this.canvas.height = Math.round(this.height * this.dpr);
    if (this.gl) {
      this.gl.viewport(0, 0, this.canvas.width, this.canvas.height);
    } else if (this.ctx2d) {
      this.ctx2d.setTransform(this.dpr, 0, 0, this.dpr, 0, 0);
    }
  }

  seedPoints() {
    const isNarrow = this.width < 520;
    const cols = isNarrow ? 40 : 50;
    const rows = isNarrow ? 42 : 48;
    this.count = cols * rows;

    const targets = sampleMorphTargets(this.shapeType, this.aspect);
    for (let i = targets.length - 1; i > 0; i--) {
      const r = Math.abs(Math.sin(i * 19.31 + 7.13)) * 10000;
      const j = Math.floor((r - Math.floor(r)) * (i + 1));
      const tmp = targets[i];
      targets[i] = targets[j];
      targets[j] = tmp;
    }

    this.refs = new Float32Array(this.count * 2);
    this.targets = new Float32Array(this.count * 2);
    this.seeds = new Float32Array(this.count * 2);
    this.isMorpher = new Uint8Array(this.count);
    this.simData = new Float32Array(this.count * 4);

    const spanX = this.aspect * 0.98;
    const spanY = 0.96;
    let idx = 0;

    for (let r = 0; r < rows; r++) {
      for (let c = 0; c < cols; c++) {
        const s1 = Math.sin(r * 17.13 + c * 91.7) * 43758.5453;
        const s2 = Math.sin(r * 41.89 + c * 23.4) * 24634.6345;
        const f1 = s1 - Math.floor(s1);
        const f2 = s2 - Math.floor(s2);

        const rx = (((c + 0.5 + (f1 - 0.5) * 0.9) / cols) * 2 - 1) * spanX;
        const ry = (((r + 0.5 + (f2 - 0.5) * 0.9) / rows) * 2 - 1) * spanY;

        this.refs[idx * 2] = rx;
        this.refs[idx * 2 + 1] = ry;
        this.seeds[idx * 2] = f1;
        this.seeds[idx * 2 + 1] = f2;

        // 窄屏按 65%（13/20）、桌面按 55%（11/20）粒子锁入目标轮廓，兼顾清晰度与干净背景
        const morphShare = isNarrow ? ((idx * 7) % 20 < 13) : ((idx * 7) % 20 < 11);
        const active = targets.length > 0 && morphShare;
        this.isMorpher[idx] = active ? 1 : 0;
        const tgt = targets[idx % Math.max(1, targets.length)] || [rx, ry];
        this.targets[idx * 2] = tgt[0];
        this.targets[idx * 2 + 1] = tgt[1];

        this.simData[idx * 4] = rx;
        this.simData[idx * 4 + 1] = ry;
        this.simData[idx * 4 + 2] = 0.65;
        this.simData[idx * 4 + 3] = 0;
        idx++;
      }
    }

    if (this.gl) {
      const gl = this.gl;
      gl.bindBuffer(gl.ARRAY_BUFFER, this.buffer);
      gl.bufferData(gl.ARRAY_BUFFER, this.simData, gl.DYNAMIC_DRAW);
    }
  }

  setHover(hover) {
    this.isHovering = hover;
    if (!this.running) {
      this.hoverT = hover ? 1 : 0;
      this.step(0.016);
      this.draw();
    }
  }

  step(dt, morphDt = dt) {
    const t = this.time;
    const goal = this.isHovering ? 1 : 0;
    const speed = 1.9; // ~0.52s 完成聚拢或散开
    if (this.hoverT < goal) {
      this.hoverT = Math.min(1, this.hoverT + morphDt * speed);
    } else if (this.hoverT > goal) {
      this.hoverT = Math.max(0, this.hoverT - morphDt * speed);
    }

    for (let i = 0; i < this.count; i++) {
      const s1 = this.seeds[i * 2];
      const s2 = this.seeds[i * 2 + 1];
      const rx = this.refs[i * 2];
      const ry = this.refs[i * 2 + 1];

      // 错落起步时间（0..0.28）后极速 cubic-out 锁定
      const localM = Math.max(0, Math.min(1, (this.hoverT - s1 * 0.25) / 0.75));
      const inv = 1 - localM;
      const eased = 1 - inv * inv * inv;

      const morphable = this.isMorpher[i] === 1;
      const tx = morphable ? this.targets[i * 2] : rx;
      const ty = morphable ? this.targets[i * 2 + 1] : ry;

      const driftX = Math.sin(t * 0.7 + s1 * 20) * (0.012 * (1 - eased) + 0.002 * eased);
      const driftY = Math.cos(t * 0.65 + s2 * 20) * (0.012 * (1 - eased) + 0.002 * eased);

      const px = rx + (tx - rx) * (morphable ? eased : 0) + driftX;
      const py = ry + (ty - ry) * (morphable ? eased : 0) + driftY;

      const lifeEnd = 2.8 + s2 * 1.4;
      const life = ((s1 * 10 + t * 0.5) % lifeEnd) / lifeEnd;
      const idleBreath = smoothstep(0.02, 0.35, life) - smoothstep(0.55, 1.0, life);
      const scale = morphable
        ? idleBreath * 0.6 * (1 - eased) + 1.05 * eased + 0.22
        : (idleBreath * 0.58 + 0.16) * (1 - this.hoverT * 0.94);

      this.simData[i * 4] = px;
      this.simData[i * 4 + 1] = py;
      this.simData[i * 4 + 2] = scale;
      this.simData[i * 4 + 3] = morphable ? localM : 0;
    }
  }

  draw() {
    if (this.gl) {
      const gl = this.gl;
      gl.useProgram(this.program);
      gl.clearColor(0, 0, 0, 0);
      gl.clear(gl.COLOR_BUFFER_BIT);

      gl.bindBuffer(gl.ARRAY_BUFFER, this.buffer);
      gl.bufferSubData(gl.ARRAY_BUFFER, 0, this.simData);
      gl.enableVertexAttribArray(this.locAttr);
      gl.vertexAttribPointer(this.locAttr, 4, gl.FLOAT, false, 0, 0);

      gl.uniform1f(this.uAspect, this.aspect);
      gl.uniform1f(this.uPixelRatio, this.dpr);
      gl.drawArrays(gl.POINTS, 0, this.count);
      return;
    }

    const ctx = this.ctx2d;
    ctx.clearRect(0, 0, this.width, this.height);
    for (let i = 0; i < this.count; i++) {
      const x = (this.simData[i * 4] / this.aspect * 0.5 + 0.5) * this.width;
      const y = (-this.simData[i * 4 + 1] * 0.5 + 0.5) * this.height;
      const m = this.simData[i * 4 + 3];
      ctx.fillStyle = m > 0.55 ? 'rgba(52,107,241,0.9)' : 'rgba(66,70,80,0.65)';
      ctx.beginPath();
      ctx.arc(x, y, m > 0.55 ? 2.2 : 1.3, 0, Math.PI * 2);
      ctx.fill();
    }
  }

  start() {
    if (this.running) return;
    this.running = true;
    let last = performance.now();
    const tick = (now) => {
      if (!this.running) return;
      const elapsed = (now - last) / 1000;
      const dt = Math.min(0.05, elapsed);
      const morphDt = Math.min(0.25, elapsed);
      last = now;
      if (this.isVisible) {
        this.time += dt;
        this.step(dt, morphDt);
        this.draw();
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
}

// ============================================================
// 3. 初始化全部 WebGL 粒子场、视口暂停与 Hero 滚动淡出
// ============================================================
const reducedMotionQuery = window.matchMedia('(prefers-reduced-motion: reduce)');
let userPausedMotion = reducedMotionQuery.matches;

const heroField = new WebGLParticleField(document.getElementById('hero-canvas'), {
  theme: 'light',
  ringMode: 'hero',
  colors: ANTIGRAVITY_LIGHT_COLORS,
  particleScale: 0.92
});

const morphLeftField = new MorphingParticleField(document.getElementById('morph-left-canvas'), 'vinyl');
const morphRightField = new MorphingParticleField(document.getElementById('morph-right-canvas'), 'headphones');

const darkField = new WebGLParticleField(document.getElementById('dark-canvas'), {
  theme: 'dark',
  ringMode: 'dark',
  colors: ANTIGRAVITY_DARK_COLORS,
  particleScale: 0.96
});

const dialogField = new WebGLParticleField(document.getElementById('dialog-canvas'), {
  theme: 'light',
  ringMode: 'halo',
  colors: ANTIGRAVITY_LIGHT_COLORS,
  particleScale: 0.95
});
dialogField.isVisible = false;

const allFields = [heroField, morphLeftField, morphRightField, darkField, dialogField];
const lightFields = [heroField, dialogField];

let startBouncerWave = () => {};
let stopBouncerWave = () => {};

function syncMotionLoop() {
  const toggleBtn = document.getElementById('motion-toggle');
  if (userPausedMotion) {
    allFields.forEach((f) => f.stop());
    stopBouncerWave();
    toggleBtn.textContent = '播放动效';
  } else {
    allFields.forEach((f) => f.start());
    startBouncerWave();
    toggleBtn.textContent = '暂停动效';
  }
}

syncMotionLoop();

document.getElementById('motion-toggle').addEventListener('click', () => {
  userPausedMotion = !userPausedMotion;
  syncMotionLoop();
});

const canvasObserver = new IntersectionObserver((entries) => {
  for (const entry of entries) {
    const field = allFields.find((f) => f.canvas === entry.target);
    if (field && field !== dialogField) {
      field.isVisible = entry.isIntersecting;
    }
  }
}, { rootMargin: '80px 0px', threshold: 0 });

[heroField, morphLeftField, morphRightField, darkField].forEach((f) => canvasObserver.observe(f.canvas));

window.addEventListener('resize', () => {
  allFields.forEach((f) => {
    f.resize();
    f.seedPoints();
    if (f.stepSimulation) f.stepSimulation(f.time);
    else f.step(0.016);
    f.draw();
  });
}, { passive: true });

// 鼠标引力交互 & 50/50 悬停聚拢变形绑定
const bindPointer = (el, field) => {
  el.addEventListener('pointermove', (e) => field.updatePointer(e.clientX, e.clientY), { passive: true });
  el.addEventListener('pointerleave', () => field.clearPointer());
};
bindPointer(document.querySelector('.hero-section'), heroField);
bindPointer(document.querySelector('.dark-cta-card'), darkField);
bindPointer(document.getElementById('workshop-dialog-stage'), dialogField);

const bindMorphCol = (selector, morphField) => {
  const col = document.querySelector(selector);
  const coarseQuery = window.matchMedia('(pointer: coarse)');
  let manualToggled = false;

  col.addEventListener('pointerenter', (e) => {
    if (e.pointerType !== 'touch' && !coarseQuery.matches) {
      morphField.setHover(true);
    }
  });
  col.addEventListener('pointerleave', (e) => {
    if (e.pointerType !== 'touch' && !coarseQuery.matches) {
      morphField.setHover(false);
    }
  });
  col.addEventListener('click', (e) => {
    if (coarseQuery.matches && !e.target.closest('a, button')) {
      manualToggled = true;
      morphField.setHover(!morphField.isHovering);
    }
  });

  const colObserver = new IntersectionObserver((entries) => {
    if (!coarseQuery.matches) return;
    for (const entry of entries) {
      if (entry.intersectionRatio >= 0.52 && !manualToggled) {
        morphField.setHover(true);
      } else if (entry.intersectionRatio < 0.12) {
        manualToggled = false;
        morphField.setHover(false);
      }
    }
  }, { threshold: [0.12, 0.52] });
  colObserver.observe(col);
};
bindMorphCol('[data-morph-shape="vinyl"]', morphLeftField);
bindMorphCol('[data-morph-shape="headphones"]', morphRightField);

// Hero 粒子随向下滚动渐隐（100% 对标 antigravity.google ScrollTrigger）
const heroSectionEl = document.querySelector('.hero-section');
const heroCanvasEl = document.getElementById('hero-canvas');
window.addEventListener('scroll', () => {
  const rect = heroSectionEl.getBoundingClientRect();
  const progress = Math.max(0, Math.min(1, -rect.top / Math.max(1, rect.height * 0.78)));
  heroCanvasEl.style.opacity = String(1 - progress);
}, { passive: true });

// Header 收缩 & Scroll Reveal 降级
if (!CSS.supports('(animation-timeline: scroll()) and (animation-range: 0% 100%)')) {
  const header = document.getElementById('site-header');
  const onScrollHeader = () => header.classList.toggle('header--scrolled', window.scrollY > 40);
  onScrollHeader();
  window.addEventListener('scroll', onScrollHeader, { passive: true });
}

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
  }, { threshold: 0.1 });
  revealEls.forEach((el) => revealObserver.observe(el));
}

// ============================================================
// 4. 26 枚圆盘行进正弦波 + 滚动横向视差 + 宣言流光打字机
// ============================================================
const BOUNCER_PATHS = [
  '<circle cx="12" cy="12" r="9"/><circle cx="12" cy="12" r="2.5"/>',
  '<path d="M3 12h3l3-6 4 12 3-6h5"/>',
  '<rect x="4" y="5" width="16" height="14" rx="4"/><path d="M8 10h8M8 14h5"/>',
  '<rect x="3" y="8" width="18" height="8" rx="4"/><circle cx="8" cy="12" r="1.5" fill="currentColor"/>',
  '<path d="M9 18V5l11-2v13"/><circle cx="6" cy="18" r="2.5"/><circle cx="17" cy="16" r="2.5"/>',
  '<rect x="5" y="11" width="14" height="9" rx="2"/><path d="M8 11V7a4 4 0 0 1 8 0v4"/>',
  '<circle cx="12" cy="13" r="7"/><path d="M12 10v3l2 2M9 3h6"/>',
  '<path d="M4 6h16v12H4z"/><path d="M4 10h16M10 6v12"/>',
  '<rect x="7" y="6" width="10" height="12" rx="2"/><path d="M9 6V3h6v3M9 18v3h6v-3"/>',
  '<path d="M12 3l2.4 5.6L20 11l-5.6 2.4L12 19l-2.4-5.6L4 11l5.6-2.4z"/>',
  '<polyline points="4 17 10 11 4 5"/><line x1="12" y1="19" x2="20" y2="19"/>'
];

const bouncerSection = document.querySelector('.bouncer-section');
const bouncerStrip = document.getElementById('bouncer-strip');
const bouncerDiscs = [];

for (let i = 0; i < 26; i++) {
  const li = document.createElement('li');
  li.className = 'bouncer-item';
  const disc = document.createElement('div');
  disc.className = 'bouncer-disc';
  disc.innerHTML = `<svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">${BOUNCER_PATHS[i % BOUNCER_PATHS.length]}</svg>`;
  li.appendChild(disc);
  bouncerStrip.appendChild(li);
  bouncerDiscs.push(disc);
}

let bouncerOffsets = [];
function measureBouncerOffsets() {
  const listLeft = bouncerStrip.getBoundingClientRect().left;
  bouncerOffsets = bouncerDiscs.map((d) => {
    const r = d.getBoundingClientRect();
    return r.left - listLeft + r.width / 2;
  });
}
measureBouncerOffsets();
window.addEventListener('resize', measureBouncerOffsets, { passive: true });

let bouncerWaveVisible = true;
let bouncerWavePhase = 0;
let bouncerRaf = 0;
let bouncerLastTime = performance.now();

function applyBouncerWave(phasePx) {
  const wavelength = Math.max(960, window.innerWidth);
  const amp = window.innerWidth < 640 ? 22 : 35;
  for (let i = 0; i < bouncerDiscs.length; i++) {
    const cx = bouncerOffsets[i] ?? i * 106;
    const angle = ((cx + phasePx) / wavelength) * Math.PI * 2;
    const y = Math.sin(angle) * amp;
    bouncerDiscs[i].style.transform = `translate3d(0, ${y.toFixed(2)}px, 0)`;
  }
}

applyBouncerWave(0);

startBouncerWave = () => {
  if (bouncerRaf) return;
  bouncerLastTime = performance.now();
  const loop = (now) => {
    const dt = Math.min(0.05, (now - bouncerLastTime) / 1000);
    bouncerLastTime = now;
    if (bouncerWaveVisible && !userPausedMotion) {
      const wavelength = Math.max(960, window.innerWidth);
      bouncerWavePhase = (bouncerWavePhase + (dt / 7.0) * wavelength) % wavelength;
      applyBouncerWave(bouncerWavePhase);
    }
    bouncerRaf = requestAnimationFrame(loop);
  };
  bouncerRaf = requestAnimationFrame(loop);
};

stopBouncerWave = () => {
  if (bouncerRaf) {
    cancelAnimationFrame(bouncerRaf);
    bouncerRaf = 0;
  }
};

if (!userPausedMotion) startBouncerWave();

const bouncerObserver = new IntersectionObserver((entries) => {
  for (const e of entries) bouncerWaveVisible = e.isIntersecting;
}, { rootMargin: '100px 0px', threshold: 0 });
bouncerObserver.observe(bouncerSection);

// 滚动水平视差（相对视口中心对称滑动，桌面 250px、手机 150px 行程）
const updateBouncerScrub = () => {
  const rect = bouncerSection.getBoundingClientRect();
  const vh = window.innerHeight || 900;
  const progress = Math.max(0, Math.min(1, (vh - rect.top) / (vh + rect.height)));
  const maxShift = window.innerWidth < 640 ? 75 : 125;
  const shiftX = maxShift - progress * (maxShift * 2);
  bouncerStrip.style.transform = `translate3d(calc(-50% + ${shiftX.toFixed(1)}px), 0, 0)`;
};
updateBouncerScrub();
window.addEventListener('scroll', updateBouncerScrub, { passive: true });

// 宣言流光竖线打字机入场
const statementEl = document.querySelector('.editorial-statement');
const statementTextEl = document.getElementById('statement-text');
const statementCaretEl = document.getElementById('statement-caret');
const fullStatement = statementTextEl.getAttribute('data-full-text') || statementTextEl.textContent;
let statementTyped = false;

const statementObserver = new IntersectionObserver((entries) => {
  for (const entry of entries) {
    if (!entry.isIntersecting || statementTyped) continue;
    statementTyped = true;
    statementObserver.disconnect();
    if (reducedMotionQuery.matches || userPausedMotion) {
      statementTextEl.textContent = fullStatement;
      statementCaretEl.classList.add('is-done');
      return;
    }
    let idx = 0;
    statementTextEl.textContent = '';
    statementCaretEl.classList.remove('is-done');
    const typeTimer = setInterval(() => {
      idx++;
      statementTextEl.textContent = fullStatement.slice(0, idx);
      if (idx >= fullStatement.length) {
        clearInterval(typeTimer);
        setTimeout(() => statementCaretEl.classList.add('is-done'), 600);
      }
    }, 24);
  }
}, { rootMargin: '0px 0px -18% 0px', threshold: 0.1 });
statementObserver.observe(statementEl);

// ============================================================
// 5. 预览台色点切换、材质切换、歌词切换与 28 点阵交互
// ============================================================
let currentTheme = ARTIST_THEMES[0];
let currentLyricIndex = 0;

const theaterFrame = document.getElementById('theater-frame');
const phoneShell = document.getElementById('phone-shell');
const previewThemeBar = document.getElementById('preview-theme-bar');

function buildPreviewThemeDots() {
  previewThemeBar.innerHTML = '';
  ARTIST_THEMES.forEach((theme) => {
    const btn = document.createElement('button');
    btn.type = 'button';
    btn.className = `theme-dot-btn${theme.id === currentTheme.id ? ' is-active' : ''}`;
    btn.setAttribute('aria-label', `切换到 ${theme.displayName}`);
    btn.innerHTML = `
      <span class="theme-dot-swatch" style="background: ${theme.primary};"></span>
      <span class="theme-dot-name">${theme.displayName}</span>
    `;
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
  phoneShell.style.setProperty('--phone-text', found.text);
  phoneShell.style.setProperty('--phone-border', found.border);
  phoneShell.style.setProperty('--phone-glass-bg', found.glassBg);
  phoneShell.style.setProperty('--phone-glass-border', found.glassBorder);

  document.querySelectorAll('.course-block').forEach((block) => {
    const slot = Number(block.getAttribute('data-slot')) || 0;
    block.style.setProperty('--course-bg', found.tracks[slot % found.tracks.length]);
    block.style.setProperty('--course-accent', found.accents[slot % found.accents.length]);
  });

  updateLyricBar();
  buildPreviewThemeDots();

  if (syncParticles) {
    lightFields.forEach((f) => f.setColors(found.spectrum));
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
    phoneShell.setAttribute('data-ui-style', btn.getAttribute('data-ui-mode'));
    document.querySelectorAll('[data-ui-mode]').forEach((b) => b.classList.toggle('is-active', b === btn));
  });
});

let notifDismissed = false;
const arrivedBtn = document.getElementById('arrived-btn');
const dotNotifCard = document.getElementById('dot-notif-card');
arrivedBtn.addEventListener('click', () => {
  notifDismissed = !notifDismissed;
  if (notifDismissed) {
    dotNotifCard.classList.add('is-dismissed');
    document.getElementById('notif-title').textContent = '已收起提醒';
    document.getElementById('notif-sub').textContent = '下节课前 15 分钟自动唤醒';
    arrivedBtn.textContent = '恢复';
    renderDotMatrix(28);
  } else {
    dotNotifCard.classList.remove('is-dismissed');
    document.getElementById('notif-title').textContent = '物理实验长课';
    document.getElementById('notif-sub').textContent = '09:50–12:15 · 实验楼 401';
    arrivedBtn.textContent = '已到教室';
    renderDotMatrix(18);
  }
});

const cursorPill = document.getElementById('cursor-pill');
theaterFrame.addEventListener('pointermove', (e) => {
  if (e.pointerType === 'touch' || reducedMotionQuery.matches) return;
  const rect = theaterFrame.getBoundingClientRect();
  cursorPill.style.opacity = '1';
  cursorPill.style.transform = `translate3d(${e.clientX - rect.left + 16}px, ${e.clientY - rect.top + 16}px, 0)`;
}, { passive: true });
theaterFrame.addEventListener('pointerleave', () => {
  cursorPill.style.opacity = '0';
});

renderDotMatrix(18);
applyTheme('ZTMY', false);

// ============================================================
// 6. 极简黑胶画廊 Carousel（遵循 modern-web-guidance）
// ============================================================
const track = document.getElementById('vinyl-track');
const markersNav = document.getElementById('carousel-markers');
const statusRegion = document.getElementById('carousel-status');
const prevBtn = document.getElementById('carousel-prev');
const nextBtn = document.getElementById('carousel-next');

let settledIndex = 0;
let pendingIndex = null;

function buildVinylGallery() {
  track.innerHTML = '';
  markersNav.innerHTML = '';

  ARTIST_THEMES.forEach((theme, i) => {
    const slide = document.createElement('article');
    slide.className = 'vinyl-slide';
    slide.setAttribute('role', 'group');
    slide.setAttribute('aria-roledescription', 'slide');
    slide.setAttribute('aria-label', `第 ${i + 1} 张，共 ${ARTIST_THEMES.length} 张`);

    const btn = document.createElement('button');
    btn.type = 'button';
    btn.className = 'vinyl-cover-btn';
    btn.innerHTML = `
      <div class="vinyl-cover-box" style="background: linear-gradient(145deg, ${theme.surface}, ${theme.surfaceVariant}); color: ${theme.text};">
        <span style="font-size: 11px; font-family: var(--font-mono); color: ${theme.primary};">${theme.badgeText}</span>
        <div class="vinyl-cover-disc" style="--disc-accent: ${theme.primary};" aria-hidden="true"></div>
      </div>
      <div>
        <div class="vinyl-title">${theme.displayName}</div>
        <div class="vinyl-sub">${theme.artistName} · ${theme.genreLabel}</div>
      </div>
    `;
    btn.addEventListener('click', () => {
      applyTheme(theme.id, true);
      document.getElementById('preview').scrollIntoView({
        behavior: reducedMotionQuery.matches ? 'auto' : 'smooth',
        block: 'start'
      });
    });
    slide.appendChild(btn);
    track.appendChild(slide);

    const markerBtn = document.createElement('button');
    markerBtn.type = 'button';
    markerBtn.className = 'carousel-marker-btn';
    markerBtn.setAttribute('aria-label', `跳转到第 ${i + 1} 张专辑`);
    if (i === 0) markerBtn.setAttribute('aria-current', 'true');
    markerBtn.innerHTML = '<span class="marker-pill"></span>';
    markerBtn.addEventListener('click', () => scrollToSlide(i));
    markersNav.appendChild(markerBtn);
  });
}

function updateMarkers(activeIdx) {
  markersNav.querySelectorAll('.carousel-marker-btn').forEach((b, i) => {
    if (i === activeIdx) b.setAttribute('aria-current', 'true');
    else b.removeAttribute('aria-current');
  });
}

function nearestSlideIndex() {
  const slides = Array.from(track.children);
  const trackCenter = track.scrollLeft + track.clientWidth / 2;
  let best = 0;
  let minD = Infinity;
  slides.forEach((s, i) => {
    const d = Math.abs((s.offsetLeft + s.offsetWidth / 2) - trackCenter);
    if (d < minD) { minD = d; best = i; }
  });
  return best;
}

function scrollToSlide(targetIdx) {
  const slides = Array.from(track.children);
  if (!slides.length) return;
  const wrapped = (targetIdx + slides.length) % slides.length;
  pendingIndex = wrapped;
  updateMarkers(wrapped);
  const s = slides[wrapped];
  track.scrollTo({
    left: Math.max(0, s.offsetLeft - (track.clientWidth - s.offsetWidth) / 2),
    behavior: reducedMotionQuery.matches ? 'auto' : 'smooth'
  });
}

prevBtn.addEventListener('click', () => scrollToSlide((pendingIndex ?? nearestSlideIndex()) - 1));
nextBtn.addEventListener('click', () => scrollToSlide((pendingIndex ?? nearestSlideIndex()) + 1));

let rAFScheduled = false;
track.addEventListener('scroll', () => {
  if (pendingIndex !== null || rAFScheduled) return;
  rAFScheduled = true;
  requestAnimationFrame(() => {
    rAFScheduled = false;
    updateMarkers(nearestSlideIndex());
  });
}, { passive: true });

function handleScrollSettled() {
  const idx = nearestSlideIndex();
  pendingIndex = null;
  updateMarkers(idx);
  if (idx !== settledIndex) {
    settledIndex = idx;
    const title = track.children[idx]?.querySelector('.vinyl-title')?.textContent || '';
    statusRegion.textContent = `当前第 ${idx + 1} 张，共 ${ARTIST_THEMES.length} 张：${title}`;
  }
}

if ('onscrollend' in window) {
  track.addEventListener('scrollend', handleScrollSettled);
} else {
  let tId = 0;
  track.addEventListener('scroll', () => {
    clearTimeout(tId);
    tId = setTimeout(handleScrollSettled, 140);
  }, { passive: true });
}

buildVinylGallery();

// ============================================================
// 7. 弹窗控制（公告条、移动抽屉、17 所高校清单、OAuth 构建工坊）
// ============================================================
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
  if (e.target === mobileDrawer || e.target.tagName === 'A') mobileDrawer.close();
});

const portalsDialog = document.getElementById('portals-dialog');
const portalsList = document.getElementById('portals-list');
UNIVERSITY_PORTALS.forEach(([school, sys]) => {
  const row = document.createElement('div');
  row.className = 'portal-item';
  row.innerHTML = `<span>${school}</span><em>${sys}</em>`;
  portalsList.appendChild(row);
});
document.getElementById('open-portals-btn').addEventListener('click', () => portalsDialog.showModal());
document.getElementById('portals-dialog-close').addEventListener('click', () => portalsDialog.close());
portalsDialog.addEventListener('click', (e) => { if (e.target === portalsDialog) portalsDialog.close(); });

const workshopDialog = document.getElementById('workshop-dialog');
const openWorkshopModal = () => {
  workshopDialog.showModal();
  dialogField.isVisible = true;
  dialogField.resize();
  dialogField.seedPoints();
  dialogField.stepSimulation(dialogField.time);
  dialogField.draw();
};
document.getElementById('open-workshop-btn').addEventListener('click', openWorkshopModal);
document.getElementById('dark-open-workshop').addEventListener('click', openWorkshopModal);
document.getElementById('mobile-open-workshop').addEventListener('click', () => {
  mobileDrawer.close();
  openWorkshopModal();
});
document.getElementById('workshop-dialog-close').addEventListener('click', () => workshopDialog.close());
workshopDialog.addEventListener('close', () => { dialogField.isVisible = false; });
workshopDialog.addEventListener('click', (e) => { if (e.target === workshopDialog) workshopDialog.close(); });

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
