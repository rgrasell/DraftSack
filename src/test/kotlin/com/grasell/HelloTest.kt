package com.grasell

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DraftSackTest {

    @Test
    fun noPlayersReturnsEmptyTeam() {
        val slots = listOf(Slot(setOf("QB"), 1))
        val result = solveDraftsack(emptyList(), 50000, slots)
        assertNotNull(result)
        assertEquals(0, result.score)
        assertTrue(result.players.isEmpty())
    }

    @Test
    fun singlePlayerThatFits() {
        val players = listOf(Player("Tom Brady", 20, 5000, "QB"))
        val slots = listOf(Slot(setOf("QB"), 1))
        val result = solveDraftsack(players, 50000, slots)
        assertNotNull(result)
        assertEquals(20, result.score)
        assertEquals(5000, result.cost)
        assertEquals(1, result.players.size)
        assertEquals("Tom Brady", result.players[0].name)
    }

    @Test
    fun singlePlayerOverBudget() {
        val players = listOf(Player("Tom Brady", 20, 60000, "QB"))
        val slots = listOf(Slot(setOf("QB"), 1))
        val result = solveDraftsack(players, 50000, slots)
        assertNotNull(result)
        assertEquals(0, result.score)
        assertTrue(result.players.isEmpty())
    }

    @Test
    fun singlePlayerWrongPosition() {
        val players = listOf(Player("Tom Brady", 20, 5000, "QB"))
        val slots = listOf(Slot(setOf("RB"), 1))
        val result = solveDraftsack(players, 50000, slots)
        assertNotNull(result)
        assertEquals(0, result.score)
        assertTrue(result.players.isEmpty())
    }

    @Test
    fun chooseBestOfTwoPlayers() {
        val players = listOf(
            Player("Good QB", 25, 5000, "QB"),
            Player("Bad QB", 10, 5000, "QB")
        )
        val slots = listOf(Slot(setOf("QB"), 1))
        val result = solveDraftsack(players, 50000, slots)
        assertNotNull(result)
        assertEquals(25, result.score)
        assertEquals("Good QB", result.players[0].name)
    }

    @Test
    fun multipleSlotsSamePosition() {
        val players = listOf(
            Player("WR1", 20, 5000, "WR"),
            Player("WR2", 15, 4000, "WR"),
            Player("WR3", 10, 3000, "WR")
        )
        val slots = listOf(Slot(setOf("WR"), 2))
        val result = solveDraftsack(players, 50000, slots)
        assertNotNull(result)
        assertEquals(35, result.score) // WR1 + WR2
        assertEquals(9000, result.cost)
        assertEquals(2, result.players.size)
    }

    @Test
    fun budgetConstraintForcesSuboptimalChoice() {
        val players = listOf(
            Player("Expensive QB", 30, 9000, "QB"),
            Player("Cheap QB", 20, 3000, "QB"),
            Player("Expensive RB", 25, 8000, "RB"),
            Player("Cheap RB", 15, 4000, "RB")
        )
        val slots = listOf(
            Slot(setOf("QB"), 1),
            Slot(setOf("RB"), 1)
        )
        // Budget of 12000: can't pick both expensive (17000)
        // Best combo: Expensive QB (30) + Cheap RB (15) = 45 for 13000 - over budget
        // Next: Cheap QB (20) + Expensive RB (25) = 45 for 11000 - fits!
        // Or: Expensive QB (30) + Cheap RB (15) doesn't fit
        // Cheap QB (20) + Expensive RB (25) = 45 for 11000
        // Cheap QB (20) + Cheap RB (15) = 35 for 7000
        val result = solveDraftsack(players, 12000, slots)
        assertNotNull(result)
        assertEquals(45, result.score) // Cheap QB + Expensive RB
        assertTrue(result.cost <= 12000)
    }

    @Test
    fun flexSlotOptimization() {
        val players = listOf(
            Player("QB1", 20, 5000, "QB"),
            Player("RB1", 18, 4000, "RB"),
            Player("RB2", 15, 3500, "RB"),
            Player("WR1", 22, 4500, "WR")
        )
        val slots = listOf(
            Slot(setOf("QB"), 1),
            Slot(setOf("RB"), 1),
            Slot(setOf("WR", "RB"), 1) // FLEX
        )
        val result = solveDraftsack(players, 50000, slots)
        assertNotNull(result)
        // Should pick QB1(20) + RB1(18) + WR1(22) in flex = 60
        // Or QB1(20) + WR1(22) in flex is not valid since WR needs own slot
        // Actually: QB1 in QB slot, RB1 in RB slot, WR1 in FLEX (WR fits FLEX)
        // Or: QB1 in QB slot, RB1 in RB slot, RB2 in FLEX
        // QB1(20) + RB1(18) + WR1(22) = 60 if WR fits FLEX
        // QB1(20) + RB1(18) + RB2(15) = 53
        // Best: 60
        assertEquals(60, result.score)
    }

    @Test
    fun zeroScorePlayersFiltered() {
        val players = listOf(
            Player("Good QB", 20, 5000, "QB"),
            Player("Zero QB", 0, 3000, "QB")
        )
        val slots = listOf(Slot(setOf("QB"), 1))
        val culled = cullPlayers(players, slots)
        assertEquals(1, culled.size)
        assertEquals("Good QB", culled[0].name)
    }

    @Test
    fun cullPlayerKeepsBestPerCostGroup() {
        val players = listOf(
            Player("WR-A", 20, 5000, "WR"),
            Player("WR-B", 25, 5000, "WR"),
            Player("WR-C", 15, 5000, "WR")
        )
        // Only 1 WR slot, so keep only the best at each cost
        val slots = listOf(Slot(setOf("WR"), 1))
        val culled = cullPlayers(players, slots)
        assertEquals(1, culled.size)
        assertEquals("WR-B", culled[0].name)
    }

    @Test
    fun negativeScorePlayersFiltered() {
        val players = listOf(
            Player("Good RB", 15, 4000, "RB"),
            Player("Bad RB", -5, 3000, "RB")
        )
        val slots = listOf(Slot(setOf("RB"), 1))
        val culled = cullPlayers(players, slots)
        assertEquals(1, culled.size)
        assertEquals("Good RB", culled[0].name)
    }

    @Test
    fun teamScoreAndCostTrackedCorrectly() {
        val team = emptyTeam
            .withPlayer(Player("P1", 10, 3000, "QB"))
            .withPlayer(Player("P2", 15, 4000, "RB"))
        assertEquals(25, team.score)
        assertEquals(7000, team.cost)
        assertEquals(2, team.size)
        assertEquals(2, team.players.size)
    }
}
