import test from 'node:test';
import assert from 'node:assert/strict';
import { emptyBoard, spawn, newGame, move, isLost, validSave } from './engine.mjs';

function board(...values) { return Array.from({ length: 16 }, (_, i) => values[i] || 0); }

test('opening creates two 2/4 tiles', () => {
  for (let i = 1; i < 100; i++) {
    let n = 0;
    const result = newGame(() => (n++ * i % 997) / 997);
    assert.equal(result.filter(Boolean).length, 2);
    assert.ok(result.every(value => [0, 2, 4].includes(value)));
  }
});
test('spawn chooses 4 at 10 percent threshold', () => {
  assert.equal(spawn(emptyBoard(), () => .09).board[1], 4);
  assert.equal(spawn(emptyBoard(), () => .12).board[1], 2);
});
test('four equal tiles merge as two pairs, only once', () => {
  const result = move(board(2, 2, 2, 2), 'left');
  assert.deepEqual(result.board.slice(0, 4), [4, 4, 0, 0]);
  assert.equal(result.gained, 8);
  assert.deepEqual(result.merged, [0, 1]);
});
test('newly merged tile does not merge again', () => {
  const result = move(board(2, 2, 4), 'left');
  assert.deepEqual(result.board.slice(0, 4), [4, 4, 0, 0]);
  assert.equal(result.gained, 4);
});
test('all directions merge on leading edge', () => {
  const cases = [['left', 0, 1, 0], ['right', 2, 3, 3], ['up', 0, 4, 0], ['down', 8, 12, 12]];
  for (const [direction, a, b, destination] of cases) {
    const initial = emptyBoard(); initial[a] = 2; initial[b] = 2;
    const result = move(initial, direction);
    assert.equal(result.board[destination], 4);
    assert.deepEqual(result.merged, [destination]);
  }
});
test('invalid slide leaves board unchanged', () => {
  const initial = board(2);
  const result = move(initial, 'left');
  assert.equal(result.changed, false);
  assert.deepEqual(result.board, initial);
});
test('win stops at 2048 and produces no 4096', () => {
  const result = move(board(1024, 1024), 'left');
  assert.equal(result.board[0], 2048);
  assert.equal(result.gained, 2048);
  assert.equal(move(result.board, 'right').changed, false);
});
test('loss requires full board and no neighbors', () => {
  const initial = board(2,4,2,4,4,2,4,2,2,4,2,4,4,2,4,2);
  assert.equal(isLost(initial), true);
  assert.equal(move(initial, 'left').changed, false);
  initial[1] = 2;
  assert.equal(isLost(initial), false);
});
test('saved data must have a valid board', () => {
  assert.equal(validSave({board: board(2), score: 0}), true);
  assert.equal(validSave({board: board(3), score: 0}), false);
  assert.equal(validSave({board: emptyBoard(), score: 0}), false);
});
