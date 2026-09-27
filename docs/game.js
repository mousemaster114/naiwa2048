import { newGame, move, spawn, isLost, validSave, LIMIT } from './engine.mjs';

const $ = selector => document.querySelector(selector);
const boardEl = $('#board'), tilesEl = $('#tiles'), scoreEl = $('#score'), bestEl = $('#best');
const soundButton = $('#sound'), restartDialog = $('#restart-dialog'), endDialog = $('#end-dialog');
const status = $('#live-status');
const soundFiles = ['./assets/merge-one.mp3', './assets/merge-two.mp3'];
const mergePlayers = soundFiles.map(file => { const audio = new Audio(file); audio.preload = 'auto'; return audio; });
const endPlayer = new Audio('./assets/end.mp3');
endPlayer.preload = 'auto';
const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)');
let state, busy = false, lastMergePlayer = null, endShown = false;

function safeNumber(value) { return Number.isSafeInteger(value) && value >= 0 ? value : 0; }
function load() {
  try {
    const data = JSON.parse(localStorage.getItem('naiwa2048-v1'));
    if (validSave(data)) return { board: data.board, score: data.score, best: Math.max(safeNumber(data.best), data.score), sound: data.sound !== false };
  } catch { /* Browser storage may be unavailable. */ }
  return { board: newGame(), score: 0, best: 0, sound: true };
}
function save() { try { localStorage.setItem('naiwa2048-v1', JSON.stringify(state)); } catch { /* Keep the current session playable. */ } }
function pauseAudio(player) { if (player) { player.pause(); player.currentTime = 0; } }
function stopAudio() { mergePlayers.forEach(pauseAudio); pauseAudio(endPlayer); lastMergePlayer = null; }
function playMerge() {
  if (!state.sound) return;
  pauseAudio(lastMergePlayer);
  lastMergePlayer = mergePlayers[Math.floor(Math.random() * mergePlayers.length)];
  lastMergePlayer.currentTime = 0;
  lastMergePlayer.play().catch(() => {});
}
function playEnd() {
  pauseAudio(lastMergePlayer);
  if (!state.sound) return;
  endPlayer.currentTime = 0;
  endPlayer.play().catch(() => {});
}
function metrics() {
  const gap = parseFloat(getComputedStyle($('.board-background')).paddingLeft) || 9;
  return { gap, size: (boardEl.clientWidth - 5 * gap) / 4 };
}
function setPosition(node, index) {
  const { gap, size } = metrics();
  node.style.setProperty('--x', `${gap + index % 4 * (size + gap)}px`);
  node.style.setProperty('--y', `${gap + Math.floor(index / 4) * (size + gap)}px`);
}
function tile(value, index, animation = '') {
  const node = document.createElement('div');
  node.className = `tile ${animation}`;
  node.dataset.value = value;
  node.dataset.digits = String(value).length;
  node.innerHTML = `<strong>${value}</strong>`;
  setPosition(node, index);
  return node;
}
function render({ merged = [], spawned = -1 } = {}) {
  tilesEl.replaceChildren(...state.board.flatMap((value, index) => value ? [tile(value, index, index === spawned ? 'new' : merged.includes(index) ? 'pop' : '')] : []));
  scoreEl.textContent = String(state.score);
  bestEl.textContent = String(state.best);
  soundButton.textContent = state.sound ? '♫  声音 · 开' : '♩  声音 · 关';
  soundButton.setAttribute('aria-pressed', String(state.sound));
}
function showEnd(won, sound) {
  if (endShown || endDialog.open || document.hidden) return;
  endShown = true;
  $('#end-title').textContent = won ? '恭喜你合成了大奶蛋' : '差一点点，再来一局！';
  $('#end-score').textContent = `本局得分  ${state.score}`;
  $('#end-picture').hidden = !won;
  const confetti = $('#confetti');
  confetti.replaceChildren();
  if (won && !reduceMotion.matches) {
    for (let i = 0; i < 38; i++) {
      const piece = document.createElement('i');
      piece.style.setProperty('--x', `${Math.random() * 100}%`);
      piece.style.setProperty('--r', `${Math.random() * 360}deg`);
      piece.style.setProperty('--delay', `${Math.random() * .35}s`);
      piece.style.setProperty('--duration', `${1.1 + Math.random() * .9}s`);
      confetti.append(piece);
    }
  }
  endDialog.showModal();
  status.textContent = won ? '恭喜你合成了大奶蛋' : '游戏结束，再来一局';
  if (won && sound) playEnd();
}
function doMove(direction) {
  if (busy || restartDialog.open || endDialog.open || state.board.includes(LIMIT) || isLost(state.board)) return;
  const turn = move(state.board, direction);
  if (!turn.changed) return;
  busy = true;
  const moving = turn.paths.map(path => tile(path.value, path.from));
  tilesEl.replaceChildren(...moving);
  requestAnimationFrame(() => requestAnimationFrame(() => turn.paths.forEach((path, i) => setPosition(moving[i], path.to))));
  const delay = reduceMotion.matches ? 0 : 150;
  window.setTimeout(() => {
    state.board = turn.board;
    state.score += turn.gained;
    state.best = Math.max(state.best, state.score);
    if (turn.gained > 0) {
      playMerge();
      const fly = $('#score-fly'); fly.textContent = `+${turn.gained}`;
      fly.classList.remove('fly'); void fly.offsetWidth; fly.classList.add('fly');
    }
    let spawned = -1;
    const won = state.board.includes(LIMIT);
    if (!won) {
      const result = spawn(state.board);
      state.board = result.board;
      spawned = result.index;
    }
    save();
    render({ merged: turn.merged, spawned });
    busy = false;
    if (won || isLost(state.board)) window.setTimeout(() => showEnd(won, won), reduceMotion.matches ? 0 : 180);
    else status.textContent = turn.gained ? `合成得分 ${turn.gained}，当前得分 ${state.score}` : `当前得分 ${state.score}`;
  }, delay);
}
function reset() {
  stopAudio();
  restartDialog.close(); endDialog.close(); endShown = false; busy = false;
  state.board = newGame(); state.score = 0;
  save(); render(); boardEl.focus(); status.textContent = '新的一局开始了';
}

const slots = document.createDocumentFragment();
for (let i = 0; i < 16; i++) { const slot = document.createElement('div'); slot.className = 'slot'; slots.append(slot); }
$('.board-background').append(slots);
state = load(); render();
if (state.board.includes(LIMIT) || isLost(state.board)) window.setTimeout(() => showEnd(state.board.includes(LIMIT), false), 100);

let startX = 0, startY = 0;
boardEl.addEventListener('pointerdown', event => { startX = event.clientX; startY = event.clientY; boardEl.setPointerCapture(event.pointerId); });
boardEl.addEventListener('pointerup', event => {
  const dx = event.clientX - startX, dy = event.clientY - startY;
  if (Math.max(Math.abs(dx), Math.abs(dy)) < 24) return;
  doMove(Math.abs(dx) > Math.abs(dy) ? dx > 0 ? 'right' : 'left' : dy > 0 ? 'down' : 'up');
});
window.addEventListener('keydown', event => {
  const direction = { ArrowLeft: 'left', ArrowRight: 'right', ArrowUp: 'up', ArrowDown: 'down' }[event.key];
  if (!direction || restartDialog.open || endDialog.open) return;
  event.preventDefault(); doMove(direction);
});
window.addEventListener('resize', () => { if (!busy) render(); });
document.addEventListener('visibilitychange', () => {
  if (document.hidden) stopAudio();
  else if ((state.board.includes(LIMIT) || isLost(state.board)) && !endShown) showEnd(state.board.includes(LIMIT), false);
});
soundButton.addEventListener('click', () => { state.sound = !state.sound; if (!state.sound) stopAudio(); save(); render(); });
$('#restart').addEventListener('click', () => { if (!busy) restartDialog.showModal(); });
$('#cancel-restart').addEventListener('click', () => restartDialog.close());
$('#confirm-restart').addEventListener('click', reset);
$('#play-again').addEventListener('click', reset);
endDialog.addEventListener('cancel', event => event.preventDefault());
