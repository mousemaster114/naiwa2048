export const SIZE = 4;
export const LIMIT = 2048;

export function emptyBoard() { return Array(16).fill(0); }

export function spawn(board, random = Math.random) {
  const empty = board.flatMap((value, index) => value === 0 ? [index] : []);
  if (empty.length === 0) return { board: [...board], index: -1 };
  const index = empty[Math.floor(random() * empty.length)];
  const next = [...board];
  next[index] = random() < .1 ? 4 : 2;
  return { board: next, index };
}

export function newGame(random = Math.random) {
  const first = spawn(emptyBoard(), random);
  return spawn(first.board, random).board;
}

export function isLost(board) {
  if (board.includes(0) || board.includes(LIMIT)) return false;
  return board.every((value, index) =>
    (index % SIZE === SIZE - 1 || board[index + 1] !== value) &&
    (Math.floor(index / SIZE) === SIZE - 1 || board[index + SIZE] !== value));
}

export function move(board, direction) {
  if (board.includes(LIMIT) || isLost(board)) return { changed: false, board: [...board], paths: [], merged: [], gained: 0 };
  const next = emptyBoard(), paths = [], merged = [];
  let gained = 0;
  for (let line = 0; line < SIZE; line++) {
    const positions = Array.from({ length: SIZE }, (_, step) => {
      if (direction === 'left') return line * SIZE + step;
      if (direction === 'right') return line * SIZE + SIZE - 1 - step;
      if (direction === 'up') return step * SIZE + line;
      if (direction === 'down') return (SIZE - 1 - step) * SIZE + line;
      throw new Error(`Unknown direction: ${direction}`);
    });
    const occupied = positions.filter(index => board[index] !== 0);
    let read = 0, write = 0;
    while (read < occupied.length) {
      const from = occupied[read], to = positions[write++], value = board[from];
      paths.push({ from, to, value });
      if (read + 1 < occupied.length && board[occupied[read + 1]] === value) {
        paths.push({ from: occupied[read + 1], to, value });
        next[to] = value * 2;
        gained += next[to];
        merged.push(to);
        read += 2;
      } else {
        next[to] = value;
        read++;
      }
    }
  }
  const changed = next.some((value, index) => value !== board[index]);
  return { changed, board: changed ? next : [...board], paths: changed ? paths : [], merged, gained };
}

export function validSave(data) {
  if (!data || !Array.isArray(data.board) || data.board.length !== 16 || !Number.isSafeInteger(data.score) || data.score < 0) return false;
  return data.board.every(value => Number.isInteger(value) && (value === 0 || value >= 2 && value <= LIMIT && (value & value - 1) === 0)) && data.board.some(Boolean);
}
