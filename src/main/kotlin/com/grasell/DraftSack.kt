package com.grasell

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.immutableListOf
import kotlinx.collections.immutable.toImmutableList

val emptyTeam = Team(immutableListOf())


fun solveDraftsack(players: List<Player>, budget: Int, slots: List<Slot>, numPlayersCallback: (Int) -> Unit = {}, memoizedSizeCallback: (Int) -> Unit = {}): Team? {
    val memoization = mutableMapOf<Constraint, Team?>()
    val culledPlayerList = cullPlayers(players, slots)

    return solveRecursive(Constraint(slots.toImmutableList(), culledPlayerList, budget), memoization, numPlayersCallback, memoizedSizeCallback)
}

private fun solveRecursive(constraint: Constraint, memoization: MutableMap<Constraint, Team?>, numPlayersCallback: (Int) -> Unit, memoizedSizeCallback: (Int) -> Unit): Team? {
    if (constraint.budget < 0) return null
    if (constraint.players.isEmpty()) return emptyTeam // no players -> empty team
    numPlayersCallback(constraint.players.size)

    return memoization.getOrPut(constraint) {
        // Actually calculate it.  What a drag.
        var result: Team?

        // If we skip this player
        val playersWithoutCurrent = constraint.players.pop()
        val currentPlayer = constraint.players.last()
        val budgetAfterPlayer = constraint.budget - currentPlayer.cost

        // Try putting this player into the slots they fit first (more restrictive)
        var teamFromSlottingPlayer: Team? = null
        var bestSlotTeamScore = -1

        for (slot in constraint.slots) {
            if (slot.size > 0 && slot.fitsPlayer(currentPlayer)) {
                val newSlots = copySlots(constraint.slots, slot)
                val potentialTeam = solveRecursive(Constraint(newSlots, playersWithoutCurrent, budgetAfterPlayer), memoization, numPlayersCallback, memoizedSizeCallback)
                if (potentialTeam != null) {
                    val teamWithPlayer = potentialTeam.withPlayer(currentPlayer)
                    if (teamWithPlayer.score > bestSlotTeamScore) {
                        bestSlotTeamScore = teamWithPlayer.score
                        teamFromSlottingPlayer = teamWithPlayer
                    }
                }
            }
        }

        // If we skip this player
        val teamFromSkippingPlayer = solveRecursive(constraint.copy(players = playersWithoutCurrent), memoization, numPlayersCallback, memoizedSizeCallback)

        result = getBest(teamFromSkippingPlayer, teamFromSlottingPlayer)

        if (result == null) result = emptyTeam // Best we can do is an empty team :(

        memoizedSizeCallback(memoization.size)

        result
    }
}

private fun getBest(t1: Team?, t2: Team?): Team? {
    if (t1 == null) return t2
    if (t2 == null) return t1

    if (t1.score > t2.score) {
        return t1
    }

    return t2
}

private fun copySlots(slots: List<Slot>, slotToDecrement: Slot): ImmutableList<Slot> {
    return slots.map {
        if (it == slotToDecrement) {
            it.copy(size = it.size-1)
        } else {
            it
        }
    }.toImmutableList()
}

fun cullPlayers(players: List<Player>, slots: List<Slot>): ImmutableList<Player> {

    val maxPossiblePerPosition = mutableMapOf<String, Int>()
    for (slot in slots) {
        for (position in slot.positionsAllowed) {
            maxPossiblePerPosition[position] = (maxPossiblePerPosition[position] ?: 0) + slot.size
        }
    }

    val bestPlayers = mutableListOf<Player>()
    players.asSequence()
            .filter { it.score > 0 }
            .groupBy { it.position }
            .forEach { (position, positionPlayers) ->
                val maxForPosition = maxPossiblePerPosition[position] ?: 0
                bestPlayers.addAll(positionPlayers.sortedByDescending { it.score }.take(maxForPosition))
            }

    return bestPlayers.toImmutableList()
}

data class Constraint(val slots: ImmutableList<Slot>, val players: ImmutableList<Player>, val budget: Int) : Comparable<Constraint> {
    override fun compareTo(other: Constraint): Int {
        this.slots.asSequence().zip(other.slots.asSequence())
                .forEach { (left, right) ->
                    if (left.size < right.size) return -1
                    if (left.size > right.size) return 1
                }

        if (this.players.size < other.players.size) return -1
        if (this.players.size > other.players.size) return 1

        if (this.budget < other.budget) return -1
        if (this.budget > other.budget) return 1

        return 0
    }

}

fun <T> ImmutableList<T>.pop() = removeAt(lastIndex)

data class Team(val players: ImmutableList<Player>) {
    val score
        get() = players.asSequence().map { it.score }.sum()

    val cost
        get() = players.asSequence().map { it.cost }.sum()

    fun withPlayer(player: Player) = copy(players = players.add(player))
}

data class Slot(val positionsAllowed: Set<String>, val size: Int) {
    fun fitsPlayer(player: Player) = positionsAllowed.contains(player.position)
}

// TODO: Allow score types that aren't integers
data class Player(val name: String, val score: Int, val cost: Int, val position: String)

