package com.grasell

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse

// =============================================================================
// Team data class tests
// =============================================================================
class TeamTest {

    @Test
    fun emptyTeamHasZeroScoreAndCost() {
        assertEquals(0, emptyTeam.score)
        assertEquals(0, emptyTeam.cost)
        assertEquals(0, emptyTeam.size)
        assertTrue(emptyTeam.players.isEmpty())
    }

    @Test
    fun withPlayerIncrementsSizeByOne() {
        val t1 = emptyTeam.withPlayer(Player("P1", 10, 3000, "QB"))
        assertEquals(1, t1.size)
        val t2 = t1.withPlayer(Player("P2", 15, 4000, "RB"))
        assertEquals(2, t2.size)
    }

    @Test
    fun withPlayerAccumulatesScore() {
        val team = emptyTeam
            .withPlayer(Player("P1", 10, 3000, "QB"))
            .withPlayer(Player("P2", 15, 4000, "RB"))
            .withPlayer(Player("P3", 7, 2000, "WR"))
        assertEquals(32, team.score)
    }

    @Test
    fun withPlayerAccumulatesCost() {
        val team = emptyTeam
            .withPlayer(Player("P1", 10, 3000, "QB"))
            .withPlayer(Player("P2", 15, 4000, "RB"))
        assertEquals(7000, team.cost)
    }

    @Test
    fun playersPropertyReturnsAllAddedPlayers() {
        val p1 = Player("P1", 10, 3000, "QB")
        val p2 = Player("P2", 15, 4000, "RB")
        val p3 = Player("P3", 7, 2000, "WR")
        val team = emptyTeam.withPlayer(p1).withPlayer(p2).withPlayer(p3)
        val names = team.players.map { it.name }.toSet()
        assertEquals(setOf("P1", "P2", "P3"), names)
    }

    @Test
    fun withPlayerDoesNotMutateOriginalTeam() {
        val t1 = emptyTeam.withPlayer(Player("P1", 10, 3000, "QB"))
        val t2 = t1.withPlayer(Player("P2", 15, 4000, "RB"))
        // t1 should be unchanged
        assertEquals(1, t1.size)
        assertEquals(10, t1.score)
        assertEquals(3000, t1.cost)
        // t2 is the new team
        assertEquals(2, t2.size)
        assertEquals(25, t2.score)
        assertEquals(7000, t2.cost)
    }

    @Test
    fun singlePlayerTeam() {
        val player = Player("Solo", 42, 9999, "TE")
        val team = emptyTeam.withPlayer(player)
        assertEquals(1, team.size)
        assertEquals(42, team.score)
        assertEquals(9999, team.cost)
        assertEquals("Solo", team.players[0].name)
    }
}

// =============================================================================
// Slot and Player model tests
// =============================================================================
class SlotTest {

    @Test
    fun fitsPlayerMatchingPosition() {
        val slot = Slot(setOf("QB"), 1)
        assertTrue(slot.fitsPlayer(Player("QB1", 10, 5000, "QB")))
    }

    @Test
    fun fitsPlayerNonMatchingPosition() {
        val slot = Slot(setOf("QB"), 1)
        assertFalse(slot.fitsPlayer(Player("RB1", 10, 5000, "RB")))
    }

    @Test
    fun flexSlotFitsMultiplePositions() {
        val flexSlot = Slot(setOf("WR", "RB", "TE"), 1)
        assertTrue(flexSlot.fitsPlayer(Player("WR1", 10, 5000, "WR")))
        assertTrue(flexSlot.fitsPlayer(Player("RB1", 10, 5000, "RB")))
        assertTrue(flexSlot.fitsPlayer(Player("TE1", 10, 5000, "TE")))
        assertFalse(flexSlot.fitsPlayer(Player("QB1", 10, 5000, "QB")))
        assertFalse(flexSlot.fitsPlayer(Player("DST1", 10, 5000, "DST")))
    }

    @Test
    fun slotWithZeroSizeStillReportsPosition() {
        val slot = Slot(setOf("QB"), 0)
        assertTrue(slot.fitsPlayer(Player("QB1", 10, 5000, "QB")))
    }

    @Test
    fun slotDataClassEquality() {
        val s1 = Slot(setOf("QB"), 2)
        val s2 = Slot(setOf("QB"), 2)
        assertEquals(s1, s2)
    }
}

class PlayerTest {

    @Test
    fun playerDataClassEquality() {
        val p1 = Player("Tom Brady", 20, 8000, "QB")
        val p2 = Player("Tom Brady", 20, 8000, "QB")
        assertEquals(p1, p2)
    }

    @Test
    fun playerFieldsAccessible() {
        val p = Player("Test Player", 15, 6000, "WR")
        assertEquals("Test Player", p.name)
        assertEquals(15, p.score)
        assertEquals(6000, p.cost)
        assertEquals("WR", p.position)
    }
}

// =============================================================================
// cullPlayers tests
// =============================================================================
class CullPlayersTest {

    @Test
    fun filtersZeroScorePlayers() {
        val players = listOf(
            Player("Good", 20, 5000, "QB"),
            Player("Zero", 0, 3000, "QB")
        )
        val culled = cullPlayers(players, listOf(Slot(setOf("QB"), 1)))
        assertEquals(1, culled.size)
        assertEquals("Good", culled[0].name)
    }

    @Test
    fun filtersNegativeScorePlayers() {
        val players = listOf(
            Player("Good", 15, 4000, "RB"),
            Player("Bad", -5, 3000, "RB")
        )
        val culled = cullPlayers(players, listOf(Slot(setOf("RB"), 1)))
        assertEquals(1, culled.size)
        assertEquals("Good", culled[0].name)
    }

    @Test
    fun keepsBestPlayersPerCostGroup() {
        val players = listOf(
            Player("WR-A", 20, 5000, "WR"),
            Player("WR-B", 25, 5000, "WR"),
            Player("WR-C", 15, 5000, "WR")
        )
        val culled = cullPlayers(players, listOf(Slot(setOf("WR"), 1)))
        assertEquals(1, culled.size)
        assertEquals("WR-B", culled[0].name)
    }

    @Test
    fun keepsMorePlayersWhenMoreSlots() {
        val players = listOf(
            Player("WR-A", 20, 5000, "WR"),
            Player("WR-B", 25, 5000, "WR"),
            Player("WR-C", 15, 5000, "WR")
        )
        // 3 WR slots: keep all 3 from same cost group
        val culled = cullPlayers(players, listOf(Slot(setOf("WR"), 3)))
        assertEquals(3, culled.size)
    }

    @Test
    fun differentCostGroupsKeptSeparately() {
        val players = listOf(
            Player("Cheap-A", 10, 3000, "RB"),
            Player("Cheap-B", 8, 3000, "RB"),
            Player("Mid-A", 18, 5000, "RB"),
            Player("Mid-B", 15, 5000, "RB"),
            Player("Expensive-A", 25, 8000, "RB")
        )
        // 1 RB slot: keep best from each cost group
        val culled = cullPlayers(players, listOf(Slot(setOf("RB"), 1)))
        val names = culled.map { it.name }.toSet()
        assertEquals(setOf("Cheap-A", "Mid-A", "Expensive-A"), names)
    }

    @Test
    fun flexSlotCountsTowardPositionCapacity() {
        val players = listOf(
            Player("RB-A", 20, 5000, "RB"),
            Player("RB-B", 18, 5000, "RB"),
            Player("RB-C", 15, 5000, "RB")
        )
        // 1 RB slot + 1 FLEX (WR/RB) = 2 total RB capacity
        val slots = listOf(
            Slot(setOf("RB"), 1),
            Slot(setOf("WR", "RB"), 1)
        )
        val culled = cullPlayers(players, slots)
        assertEquals(2, culled.size) // only top 2 from same cost group
    }

    @Test
    fun playersWithNoMatchingSlotFiltered() {
        val players = listOf(
            Player("QB1", 20, 5000, "QB"),
            Player("K1", 5, 3000, "K") // no K slot
        )
        val culled = cullPlayers(players, listOf(Slot(setOf("QB"), 1)))
        assertEquals(1, culled.size)
        assertEquals("QB1", culled[0].name)
    }

    @Test
    fun allPlayersFilteredReturnsEmpty() {
        val players = listOf(
            Player("Zero", 0, 5000, "QB"),
            Player("Negative", -3, 3000, "QB")
        )
        val culled = cullPlayers(players, listOf(Slot(setOf("QB"), 1)))
        assertTrue(culled.isEmpty())
    }

    @Test
    fun emptyPlayerListReturnsEmpty() {
        val culled = cullPlayers(emptyList(), listOf(Slot(setOf("QB"), 1)))
        assertTrue(culled.isEmpty())
    }

    @Test
    fun multiplePositionsMultipleSlots() {
        val players = listOf(
            Player("QB1", 20, 5000, "QB"),
            Player("QB2", 18, 5000, "QB"),
            Player("RB1", 22, 6000, "RB"),
            Player("RB2", 19, 6000, "RB"),
            Player("RB3", 15, 6000, "RB"),
            Player("WR1", 25, 7000, "WR")
        )
        val slots = listOf(
            Slot(setOf("QB"), 1),
            Slot(setOf("RB"), 2),
            Slot(setOf("WR"), 1)
        )
        val culled = cullPlayers(players, slots)
        val names = culled.map { it.name }.toSet()
        // QB: 1 slot, keep 1 per cost group -> QB1
        // RB: 2 slots, keep 2 per cost group -> RB1, RB2
        // WR: 1 slot, keep 1 per cost group -> WR1
        assertEquals(setOf("QB1", "RB1", "RB2", "WR1"), names)
    }
}

// =============================================================================
// solveDraftsack edge case tests
// =============================================================================
class SolveDraftsackEdgeCaseTest {

    @Test
    fun noPlayersReturnsEmptyTeam() {
        val result = solveDraftsack(emptyList(), 50000, listOf(Slot(setOf("QB"), 1)))
        assertNotNull(result)
        assertEquals(0, result.score)
        assertTrue(result.players.isEmpty())
    }

    @Test
    fun singlePlayerFits() {
        val result = solveDraftsack(
            listOf(Player("QB1", 20, 5000, "QB")),
            50000,
            listOf(Slot(setOf("QB"), 1))
        )
        assertNotNull(result)
        assertEquals(20, result.score)
        assertEquals(5000, result.cost)
    }

    @Test
    fun singlePlayerOverBudget() {
        val result = solveDraftsack(
            listOf(Player("QB1", 20, 60000, "QB")),
            50000,
            listOf(Slot(setOf("QB"), 1))
        )
        assertNotNull(result)
        assertEquals(0, result.score)
    }

    @Test
    fun singlePlayerWrongPosition() {
        val result = solveDraftsack(
            listOf(Player("QB1", 20, 5000, "QB")),
            50000,
            listOf(Slot(setOf("RB"), 1))
        )
        assertNotNull(result)
        assertEquals(0, result.score)
    }

    @Test
    fun exactBudgetMatch() {
        val result = solveDraftsack(
            listOf(Player("QB1", 20, 50000, "QB")),
            50000,
            listOf(Slot(setOf("QB"), 1))
        )
        assertNotNull(result)
        assertEquals(20, result.score)
        assertEquals(50000, result.cost)
    }

    @Test
    fun oneOverBudget() {
        val result = solveDraftsack(
            listOf(Player("QB1", 20, 50001, "QB")),
            50000,
            listOf(Slot(setOf("QB"), 1))
        )
        assertNotNull(result)
        assertEquals(0, result.score)
    }

    @Test
    fun zeroBudgetNoPlayers() {
        val result = solveDraftsack(
            listOf(Player("QB1", 20, 5000, "QB")),
            0,
            listOf(Slot(setOf("QB"), 1))
        )
        assertNotNull(result)
        assertEquals(0, result.score)
    }

    @Test
    fun allPlayersZeroScore() {
        val result = solveDraftsack(
            listOf(
                Player("QB1", 0, 5000, "QB"),
                Player("RB1", 0, 4000, "RB")
            ),
            50000,
            listOf(Slot(setOf("QB"), 1), Slot(setOf("RB"), 1))
        )
        assertNotNull(result)
        assertEquals(0, result.score)
    }

    @Test
    fun allPlayersWrongPosition() {
        val result = solveDraftsack(
            listOf(
                Player("QB1", 20, 5000, "QB"),
                Player("QB2", 15, 4000, "QB")
            ),
            50000,
            listOf(Slot(setOf("RB"), 1), Slot(setOf("WR"), 1))
        )
        assertNotNull(result)
        assertEquals(0, result.score)
    }

    @Test
    fun allPlayersOverBudget() {
        val result = solveDraftsack(
            listOf(
                Player("QB1", 20, 60000, "QB"),
                Player("RB1", 15, 70000, "RB")
            ),
            50000,
            listOf(Slot(setOf("QB"), 1), Slot(setOf("RB"), 1))
        )
        assertNotNull(result)
        assertEquals(0, result.score)
    }

    @Test
    fun choosesHigherScoreOverLowerCost() {
        val result = solveDraftsack(
            listOf(
                Player("High", 30, 8000, "QB"),
                Player("Low", 10, 3000, "QB")
            ),
            50000,
            listOf(Slot(setOf("QB"), 1))
        )
        assertNotNull(result)
        assertEquals(30, result.score)
        assertEquals("High", result.players[0].name)
    }

    @Test
    fun budgetForcesLowerScorePlayer() {
        val result = solveDraftsack(
            listOf(
                Player("Expensive", 30, 60000, "QB"),
                Player("Affordable", 10, 3000, "QB")
            ),
            50000,
            listOf(Slot(setOf("QB"), 1))
        )
        assertNotNull(result)
        assertEquals(10, result.score)
        assertEquals("Affordable", result.players[0].name)
    }

    @Test
    fun multiSlotSamePositionPicksBest() {
        val players = listOf(
            Player("WR1", 20, 5000, "WR"),
            Player("WR2", 15, 4000, "WR"),
            Player("WR3", 10, 3000, "WR")
        )
        val result = solveDraftsack(players, 50000, listOf(Slot(setOf("WR"), 2)))
        assertNotNull(result)
        assertEquals(35, result.score) // WR1 + WR2
        assertEquals(2, result.players.size)
    }

    @Test
    fun fewerPlayersThanSlots() {
        val players = listOf(Player("WR1", 20, 5000, "WR"))
        val result = solveDraftsack(players, 50000, listOf(Slot(setOf("WR"), 3)))
        assertNotNull(result)
        assertEquals(20, result.score)
        assertEquals(1, result.players.size)
    }

    @Test
    fun resultNeverExceedsBudget() {
        val players = listOf(
            Player("QB1", 30, 9000, "QB"),
            Player("QB2", 20, 3000, "QB"),
            Player("RB1", 25, 8000, "RB"),
            Player("RB2", 15, 4000, "RB"),
            Player("WR1", 22, 7000, "WR"),
            Player("WR2", 12, 3000, "WR")
        )
        val slots = listOf(
            Slot(setOf("QB"), 1),
            Slot(setOf("RB"), 1),
            Slot(setOf("WR"), 1)
        )
        val result = solveDraftsack(players, 15000, slots)
        assertNotNull(result)
        assertTrue(result.cost <= 15000, "Cost ${result.cost} exceeds budget 15000")
    }

    @Test
    fun deterministic() {
        val players = listOf(
            Player("QB1", 20, 5000, "QB"),
            Player("RB1", 18, 4000, "RB"),
            Player("RB2", 15, 3500, "RB"),
            Player("WR1", 22, 4500, "WR")
        )
        val slots = listOf(
            Slot(setOf("QB"), 1),
            Slot(setOf("RB"), 1),
            Slot(setOf("WR", "RB"), 1)
        )
        val r1 = solveDraftsack(players, 50000, slots)
        val r2 = solveDraftsack(players, 50000, slots)
        assertNotNull(r1)
        assertNotNull(r2)
        assertEquals(r1.score, r2.score)
        assertEquals(r1.cost, r2.cost)
        assertEquals(r1.players.map { it.name }.toSet(), r2.players.map { it.name }.toSet())
    }
}

// =============================================================================
// Budget constraint and tradeoff tests
// =============================================================================
class BudgetTradeoffTest {

    @Test
    fun budgetConstraintForcesSuboptimalIndividualChoices() {
        val players = listOf(
            Player("Expensive QB", 30, 9000, "QB"),
            Player("Cheap QB", 20, 3000, "QB"),
            Player("Expensive RB", 25, 8000, "RB"),
            Player("Cheap RB", 15, 4000, "RB")
        )
        val slots = listOf(Slot(setOf("QB"), 1), Slot(setOf("RB"), 1))
        // Budget 12000: best is CheapQB(20)+ExpRB(25)=45 at 11000
        val result = solveDraftsack(players, 12000, slots)
        assertNotNull(result)
        assertEquals(45, result.score)
        assertTrue(result.cost <= 12000)
    }

    @Test
    fun tightBudgetWithManyPlayers() {
        val players = listOf(
            Player("QB1", 30, 9000, "QB"),
            Player("QB2", 25, 7000, "QB"),
            Player("QB3", 20, 5000, "QB"),
            Player("RB1", 28, 8500, "RB"),
            Player("RB2", 22, 6500, "RB"),
            Player("RB3", 18, 4500, "RB"),
            Player("RB4", 12, 3000, "RB")
        )
        val slots = listOf(Slot(setOf("QB"), 1), Slot(setOf("RB"), 2))
        // Budget 18000: must find best combo within budget
        val result = solveDraftsack(players, 18000, slots)
        assertNotNull(result)
        assertTrue(result.cost <= 18000, "Cost ${result.cost} exceeds budget 18000")
        // QB3(5000) + RB1(8500) + RB2(6500) = 20000 - over
        // QB3(5000) + RB1(8500) + RB3(4500) = 18000 -> score 20+28+18=66
        // QB3(5000) + RB2(6500) + RB3(4500) = 16000 -> score 20+22+18=60
        // QB2(7000) + RB3(4500) + RB4(3000) = 14500 -> score 25+18+12=55
        // QB3(5000) + RB1(8500) + RB4(3000) = 16500 -> score 20+28+12=60
        assertEquals(66, result.score)
    }

    @Test
    fun preferHighTotalOverSingleHighPlayer() {
        // Even though one player has highest individual score,
        // picking two medium-score players might be better total
        val players = listOf(
            Player("Star", 40, 9000, "WR"),
            Player("MedA", 25, 4000, "WR"),
            Player("MedB", 22, 4000, "WR")
        )
        // Budget 9000, 2 WR slots
        // Star alone: 40 at 9000
        // MedA + MedB: 47 at 8000
        val result = solveDraftsack(players, 9000, listOf(Slot(setOf("WR"), 2)))
        assertNotNull(result)
        assertEquals(47, result.score)
    }
}

// =============================================================================
// FLEX slot and multi-position tests
// =============================================================================
class FlexSlotTest {

    @Test
    fun flexSlotUsedForBestCombination() {
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
        // QB1(20) + RB1(18) + WR1(22) = 60
        assertEquals(60, result.score)
        assertEquals(3, result.players.size)
    }

    @Test
    fun flexSlotCanTakeEitherPosition() {
        // Only one player per position, FLEX could take either
        val players = listOf(
            Player("RB1", 25, 5000, "RB"),
            Player("WR1", 20, 4000, "WR")
        )
        val slots = listOf(Slot(setOf("WR", "RB"), 1))
        val result = solveDraftsack(players, 50000, slots)
        assertNotNull(result)
        // Should pick RB1 (higher score) for the FLEX slot
        assertEquals(25, result.score)
    }

    @Test
    fun flexDoesNotDoubleCountSlots() {
        // RB slot + FLEX(WR/RB): should only fit 2 RBs total, not 3
        val players = listOf(
            Player("RB1", 20, 5000, "RB"),
            Player("RB2", 18, 4000, "RB"),
            Player("RB3", 15, 3000, "RB")
        )
        val slots = listOf(
            Slot(setOf("RB"), 1),
            Slot(setOf("WR", "RB"), 1)
        )
        val result = solveDraftsack(players, 50000, slots)
        assertNotNull(result)
        assertEquals(2, result.players.size) // max 2 slots total
        assertEquals(38, result.score) // RB1 + RB2
    }

    @Test
    fun multipleFlexSlots() {
        val players = listOf(
            Player("RB1", 20, 5000, "RB"),
            Player("WR1", 22, 4500, "WR"),
            Player("TE1", 15, 3500, "TE")
        )
        val slots = listOf(
            Slot(setOf("WR", "RB", "TE"), 1),
            Slot(setOf("WR", "RB", "TE"), 1)
        )
        val result = solveDraftsack(players, 50000, slots)
        assertNotNull(result)
        // Pick top 2: WR1(22) + RB1(20) = 42
        assertEquals(42, result.score)
        assertEquals(2, result.players.size)
    }

    @Test
    fun flexSlotNotUsedIfNotBeneficial() {
        val players = listOf(
            Player("QB1", 30, 5000, "QB"),
            Player("RB1", 10, 8000, "RB")
        )
        val slots = listOf(
            Slot(setOf("QB"), 1),
            Slot(setOf("WR", "RB"), 1)
        )
        // Budget only supports one: QB1 is better value
        val result = solveDraftsack(players, 10000, slots)
        assertNotNull(result)
        // QB1(30) at 5000 + RB1(10) at 8000 = 13000 over budget
        // Just QB1 = 30
        assertEquals(30, result.score)
    }
}

// =============================================================================
// Realistic DraftKings-style integration tests
// =============================================================================
class IntegrationTest {

    private val realisticSlots = listOf(
        Slot(setOf("QB"), 1),
        Slot(setOf("RB"), 2),
        Slot(setOf("WR"), 3),
        Slot(setOf("TE"), 1),
        Slot(setOf("WR", "RB", "TE"), 1), // FLEX
        Slot(setOf("DST"), 1)
    )

    @Test
    fun fullRosterWithExactlyEnoughPlayers() {
        val players = listOf(
            Player("QB1", 20, 7000, "QB"),
            Player("RB1", 18, 6000, "RB"),
            Player("RB2", 15, 5500, "RB"),
            Player("WR1", 22, 7500, "WR"),
            Player("WR2", 19, 6500, "WR"),
            Player("WR3", 16, 5000, "WR"),
            Player("TE1", 14, 4500, "TE"),
            Player("DST1", 8, 3000, "DST")
        )
        val result = solveDraftsack(players, 50000, realisticSlots)
        assertNotNull(result)
        // All 8 players fit within budget (total cost = 45000)
        // 7 slots filled (RB x2 + WR x3 + QB + TE + DST) but we have 8 players and 9 slots
        // FLEX can take WR/RB/TE - no extra player so 8 filled at most
        val totalCost = players.sumBy { it.cost }
        assertTrue(totalCost <= 50000)
        assertEquals(8, result.players.size)
    }

    @Test
    fun fullRosterOptimizesWithinBudget() {
        val players = listOf(
            Player("QB-Star", 28, 8000, "QB"),
            Player("QB-Budget", 18, 5000, "QB"),
            Player("RB-Star", 24, 7500, "RB"),
            Player("RB-Mid", 20, 6000, "RB"),
            Player("RB-Budget", 14, 4000, "RB"),
            Player("WR-Star", 26, 8000, "WR"),
            Player("WR-Mid1", 20, 5500, "WR"),
            Player("WR-Mid2", 18, 5000, "WR"),
            Player("WR-Budget", 12, 3500, "WR"),
            Player("TE-Star", 16, 5000, "TE"),
            Player("TE-Budget", 10, 3000, "TE"),
            Player("DST1", 9, 3500, "DST"),
            Player("DST2", 7, 2500, "DST")
        )
        val result = solveDraftsack(players, 50000, realisticSlots)
        assertNotNull(result)
        assertTrue(result.cost <= 50000, "Cost ${result.cost} exceeds budget 50000")
        assertTrue(result.score > 0)
        // 9 slots total (QB:1 + RB:2 + WR:3 + TE:1 + FLEX:1 + DST:1)
        val totalSlots = realisticSlots.sumBy { it.size }
        assertTrue(result.players.size <= totalSlots,
            "Players ${result.players.size} exceeds slot capacity $totalSlots")
    }

    @Test
    fun missingPositionResultsInPartialRoster() {
        // No DST players available
        val players = listOf(
            Player("QB1", 20, 5000, "QB"),
            Player("RB1", 18, 4000, "RB"),
            Player("RB2", 15, 3500, "RB"),
            Player("WR1", 22, 5000, "WR"),
            Player("WR2", 19, 4000, "WR"),
            Player("WR3", 16, 3500, "WR"),
            Player("TE1", 14, 3000, "TE")
        )
        val result = solveDraftsack(players, 50000, realisticSlots)
        assertNotNull(result)
        // Should still pick best available (no DST to fill that slot)
        assertTrue(result.players.none { it.position == "DST" })
    }

    @Test
    fun nineIndividualSlotsMemoizationCorrectness() {
        // Regression: slots defined as 9 individual Slot objects (size=1 each)
        // Previously caused memoization key collision with bit-packed Long encoding
        val individualSlots = listOf(
            Slot(setOf("QB"), 1),
            Slot(setOf("RB"), 1),
            Slot(setOf("RB"), 1),
            Slot(setOf("WR"), 1),
            Slot(setOf("WR"), 1),
            Slot(setOf("WR"), 1),
            Slot(setOf("TE"), 1),
            Slot(setOf("WR", "RB", "TE"), 1), // FLEX
            Slot(setOf("DST"), 1)
        )
        val players = listOf(
            Player("QB1", 22, 7000, "QB"),
            Player("RB1", 20, 6500, "RB"),
            Player("RB2", 17, 5500, "RB"),
            Player("RB3", 12, 4000, "RB"),
            Player("WR1", 24, 7500, "WR"),
            Player("WR2", 19, 5500, "WR"),
            Player("WR3", 15, 4500, "WR"),
            Player("WR4", 11, 3500, "WR"),
            Player("TE1", 14, 4500, "TE"),
            Player("TE2", 10, 3000, "TE"),
            Player("DST1", 8, 3000, "DST")
        )
        val totalSlots = individualSlots.sumBy { it.size }
        val result = solveDraftsack(players, 50000, individualSlots)
        assertNotNull(result)
        assertTrue(result.cost <= 50000, "Cost ${result.cost} exceeds budget 50000")
        assertTrue(result.players.size <= totalSlots,
            "Players ${result.players.size} exceeds slot capacity $totalSlots")
        // Verify no duplicates
        val names = result.players.map { it.name }
        assertEquals(names.size, names.toSet().size, "Duplicate players: $names")
        // Verify score/cost consistency
        assertEquals(result.players.sumBy { it.score }, result.score)
        assertEquals(result.players.sumBy { it.cost }, result.cost)
    }

    @Test
    fun manyPlayersPerPosition() {
        val players = mutableListOf<Player>()
        // 10 QBs, 15 RBs, 15 WRs, 10 TEs, 5 DSTs
        for (i in 1..10) players.add(Player("QB$i", 30 - i, 8000 - i * 200, "QB"))
        for (i in 1..15) players.add(Player("RB$i", 25 - i % 10, 7000 - i * 150, "RB"))
        for (i in 1..15) players.add(Player("WR$i", 28 - i % 10, 7500 - i * 150, "WR"))
        for (i in 1..10) players.add(Player("TE$i", 18 - i, 5000 - i * 200, "TE"))
        for (i in 1..5) players.add(Player("DST$i", 12 - i, 4000 - i * 300, "DST"))

        val result = solveDraftsack(players, 50000, realisticSlots)
        assertNotNull(result)
        assertTrue(result.cost <= 50000)
        assertTrue(result.score > 0)
        // Should fill 9 slots
        assertEquals(9, result.players.size)
    }
}

// =============================================================================
// Callback and memoization behavior tests
// =============================================================================
class CallbackTest {

    @Test
    fun numPlayersCallbackInvoked() {
        var callbackCount = 0
        val players = listOf(
            Player("QB1", 20, 5000, "QB"),
            Player("RB1", 15, 4000, "RB")
        )
        val slots = listOf(Slot(setOf("QB"), 1), Slot(setOf("RB"), 1))
        solveDraftsack(players, 50000, slots,
            numPlayersCallback = { callbackCount++ })
        assertTrue(callbackCount > 0, "numPlayersCallback should be invoked at least once")
    }

    @Test
    fun memoizedSizeCallbackInvoked() {
        var callbackCount = 0
        var lastMemoSize = 0
        val players = listOf(
            Player("QB1", 20, 5000, "QB"),
            Player("RB1", 15, 4000, "RB")
        )
        val slots = listOf(Slot(setOf("QB"), 1), Slot(setOf("RB"), 1))
        solveDraftsack(players, 50000, slots,
            memoizedSizeCallback = { size -> callbackCount++; lastMemoSize = size })
        assertTrue(callbackCount > 0, "memoizedSizeCallback should be invoked")
        assertTrue(lastMemoSize > 0, "Memoization table should have entries")
    }

    @Test
    fun numPlayersCallbackReceivesPositiveValues() {
        val values = mutableListOf<Int>()
        val players = listOf(
            Player("QB1", 20, 5000, "QB"),
            Player("RB1", 15, 4000, "RB"),
            Player("WR1", 18, 4500, "WR")
        )
        val slots = listOf(
            Slot(setOf("QB"), 1),
            Slot(setOf("RB"), 1),
            Slot(setOf("WR"), 1)
        )
        solveDraftsack(players, 50000, slots,
            numPlayersCallback = { values.add(it) })
        assertTrue(values.all { it > 0 }, "All callback values should be positive")
    }
}

// =============================================================================
// Correctness invariant tests
// =============================================================================
class CorrectnessInvariantTest {

    @Test
    fun resultPlayerCountNeverExceedsTotalSlots() {
        val players = listOf(
            Player("QB1", 20, 5000, "QB"),
            Player("QB2", 18, 4500, "QB"),
            Player("RB1", 22, 6000, "RB"),
            Player("RB2", 19, 5000, "RB"),
            Player("WR1", 25, 7000, "WR"),
            Player("WR2", 21, 5500, "WR"),
            Player("WR3", 17, 4000, "WR")
        )
        val slots = listOf(
            Slot(setOf("QB"), 1),
            Slot(setOf("RB"), 1),
            Slot(setOf("WR"), 2)
        )
        val totalSlotCapacity = slots.sumBy { it.size }
        val result = solveDraftsack(players, 50000, slots)
        assertNotNull(result)
        assertTrue(result.players.size <= totalSlotCapacity,
            "Players ${result.players.size} exceeds slot capacity $totalSlotCapacity")
    }

    @Test
    fun resultPlayersMatchAvailableSlotPositions() {
        val players = listOf(
            Player("QB1", 20, 5000, "QB"),
            Player("RB1", 18, 4000, "RB"),
            Player("WR1", 22, 4500, "WR")
        )
        val slots = listOf(
            Slot(setOf("QB"), 1),
            Slot(setOf("RB"), 1),
            Slot(setOf("WR"), 1)
        )
        val result = solveDraftsack(players, 50000, slots)
        assertNotNull(result)
        val allowedPositions = slots.flatMap { it.positionsAllowed }.toSet()
        for (player in result.players) {
            assertTrue(player.position in allowedPositions,
                "Player position ${player.position} not in allowed $allowedPositions")
        }
    }

    @Test
    fun teamScoreEqualsSumOfPlayerScores() {
        val players = listOf(
            Player("QB1", 20, 5000, "QB"),
            Player("RB1", 18, 4000, "RB"),
            Player("WR1", 22, 4500, "WR")
        )
        val slots = listOf(
            Slot(setOf("QB"), 1),
            Slot(setOf("RB"), 1),
            Slot(setOf("WR"), 1)
        )
        val result = solveDraftsack(players, 50000, slots)
        assertNotNull(result)
        assertEquals(result.players.sumBy { it.score }, result.score)
    }

    @Test
    fun teamCostEqualsSumOfPlayerCosts() {
        val players = listOf(
            Player("QB1", 20, 5000, "QB"),
            Player("RB1", 18, 4000, "RB"),
            Player("WR1", 22, 4500, "WR")
        )
        val slots = listOf(
            Slot(setOf("QB"), 1),
            Slot(setOf("RB"), 1),
            Slot(setOf("WR"), 1)
        )
        val result = solveDraftsack(players, 50000, slots)
        assertNotNull(result)
        assertEquals(result.players.sumBy { it.cost }, result.cost)
    }

    @Test
    fun noDuplicatePlayersInResult() {
        val players = listOf(
            Player("QB1", 20, 5000, "QB"),
            Player("RB1", 18, 4000, "RB"),
            Player("RB2", 15, 3500, "RB"),
            Player("WR1", 22, 4500, "WR"),
            Player("WR2", 19, 4000, "WR")
        )
        val slots = listOf(
            Slot(setOf("QB"), 1),
            Slot(setOf("RB"), 2),
            Slot(setOf("WR", "RB"), 1)
        )
        val result = solveDraftsack(players, 50000, slots)
        assertNotNull(result)
        val names = result.players.map { it.name }
        assertEquals(names.size, names.toSet().size, "Duplicate players found: $names")
    }

    @Test
    fun resultIsOptimalAmongBruteForceForSmallInput() {
        // Brute force all combinations and verify solver finds the best
        val players = listOf(
            Player("QB1", 20, 7000, "QB"),
            Player("QB2", 15, 4000, "QB"),
            Player("RB1", 18, 6000, "RB"),
            Player("RB2", 12, 3000, "RB")
        )
        val slots = listOf(Slot(setOf("QB"), 1), Slot(setOf("RB"), 1))
        val budget = 10000

        // Brute force: try all QB+RB combos
        val qbs = players.filter { it.position == "QB" }
        val rbs = players.filter { it.position == "RB" }
        var bestBruteScore = 0
        for (qb in qbs) {
            for (rb in rbs) {
                if (qb.cost + rb.cost <= budget) {
                    val total = qb.score + rb.score
                    if (total > bestBruteScore) bestBruteScore = total
                }
            }
        }

        val result = solveDraftsack(players, budget, slots)
        assertNotNull(result)
        assertEquals(bestBruteScore, result.score,
            "Solver score ${result.score} != brute force $bestBruteScore")
    }

    @Test
    fun resultIsOptimalBruteForceThreePositions() {
        val players = listOf(
            Player("QB1", 22, 8000, "QB"),
            Player("QB2", 16, 5000, "QB"),
            Player("RB1", 20, 7000, "RB"),
            Player("RB2", 14, 4000, "RB"),
            Player("WR1", 24, 7500, "WR"),
            Player("WR2", 13, 3500, "WR")
        )
        val slots = listOf(
            Slot(setOf("QB"), 1),
            Slot(setOf("RB"), 1),
            Slot(setOf("WR"), 1)
        )
        val budget = 16000

        // Brute force all QB+RB+WR combos
        val qbs = players.filter { it.position == "QB" }
        val rbs = players.filter { it.position == "RB" }
        val wrs = players.filter { it.position == "WR" }
        var bestBrute = 0
        for (qb in qbs) for (rb in rbs) for (wr in wrs) {
            if (qb.cost + rb.cost + wr.cost <= budget) {
                val s = qb.score + rb.score + wr.score
                if (s > bestBrute) bestBrute = s
            }
        }

        val result = solveDraftsack(players, budget, slots)
        assertNotNull(result)
        assertEquals(bestBrute, result.score)
    }
}
