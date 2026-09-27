package com.naiwa.game

import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class GameTest {
    private fun game(vararg values: Int)=Game(Random(42)).apply { cells=IntArray(16) { values.getOrElse(it){0} } }
    @Test fun openingHasTwoValidTiles() { repeat(100) { val g=Game(Random(it)); g.reset(); assertEquals(2,g.cells.count { v->v!=0 }); assertTrue(g.cells.all { v->v==0||v==2||v==4 }) } }
    @Test fun fourEqualTilesMergeOnlyInPairs() { val g=game(2,2,2,2); val t=g.move(Direction.LEFT); assertEquals(4,g.cells[0]); assertEquals(4,g.cells[1]); assertEquals(8,t.gained); assertEquals(2,t.merged.size); assertEquals(3,g.cells.count { it!=0 }) }
    @Test fun newlyMergedTileCannotMergeAgain() { val g=game(2,2,4,0); val t=g.move(Direction.LEFT); assertEquals(4,g.cells[0]); assertEquals(4,g.cells[1]); assertEquals(4,t.gained) }
    @Test fun everyDirectionMergesAtLeadingEdge() { for(d in Direction.values()) { val g=game(); val positions=when(d) { Direction.LEFT->listOf(0,1,0); Direction.RIGHT->listOf(2,3,3); Direction.UP->listOf(0,4,0); Direction.DOWN->listOf(8,12,12) }; g.cells[positions[0]]=2; g.cells[positions[1]]=2; val t=g.move(d); assertEquals(4,g.cells[positions[2]]); assertEquals(setOf(positions[2]),t.merged) } }
    @Test fun invalidMoveDoesNotSpawn() { val g=game(2); val before=g.cells.copyOf(); assertFalse(g.move(Direction.LEFT).changed); assertArrayEquals(before,g.cells); assertEquals(0,g.score) }
    @Test fun slideWithoutMergeSpawnsOnce() { val g=game(0,2); val t=g.move(Direction.LEFT); assertTrue(t.changed); assertEquals(0,t.gained); assertTrue(t.merged.isEmpty()); assertEquals(2,g.cells.count { it!=0 }) }
    @Test fun winningStopsAt2048() { val g=game(1024,1024); val t=g.move(Direction.LEFT); assertTrue(g.won); assertEquals(2048,g.score); assertEquals(-1,t.spawned); assertFalse(g.move(Direction.RIGHT).changed); assertEquals(2048,g.cells.max()) }
    @Test fun fullBoardCanBeLostOrStillPlayable() { val g=game(2,4,2,4,4,2,4,2,2,4,2,4,4,2,4,2); assertTrue(g.lost); assertFalse(g.move(Direction.LEFT).changed); g.cells[1]=2; assertFalse(g.lost) }
    @Test fun seededSpawnsIncludeBothValuesNearExpectedRatio() { var fours=0; repeat(1000) { val g=Game(Random(it)); g.reset(); fours+=g.cells.count { v->v==4 } }; assertTrue(fours in 140..260) }
}
