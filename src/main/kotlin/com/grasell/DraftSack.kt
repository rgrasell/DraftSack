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
        var result: Team? = null

        // If we skip this player
        val playersWithoutCurrent = constraint.players.pop()
        val teamFromSkippingPlayer = solveRecursive(constraint.copy(players = playersWithoutCurrent), memoization, numPlayersCallback, memoizedSizeCallback)

        // Try putting this player into the slots they fit
        val currentPlayer = constraint.players.last()
        val budgetAfterPlayer = constraint.budget - currentPlayer.cost
        // Find intersection between slots in our constraint and slots the player fits
        val teamFromSlottingPlayer = constraint.slots.asSequence()
            .filter { it.size > 0 && it.fitsPlayer(currentPlayer) }
            .map {
                val newSlots = copySlots(constraint.slots, it)
                val potentialTeam = solveRecursive(Constraint(newSlots, playersWithoutCurrent, budgetAfterPlayer), memoization, numPlayersCallback, memoizedSizeCallback)
                potentialTeam?.withPlayer(currentPlayer)
        }.maxBy { it?.score ?: -1 }

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

    val maxPossiblePerPosition = slots.asSequence()
            .flatMap { slot -> slot.positionsAllowed.asSequence()
                    .map { position -> Pair(position, slot.size) } }
            .groupingBy { it.first }
            .fold(0) { accum, element -> accum + element.second }

    val bestPlayers = players.asSequence()
            .filter { it.score > 0 }
            .groupBy { Pair(it.position, it.cost) }.asSequence()
            .flatMap { it.value.asSequence().sortedByDescending { it.score }.take(maxPossiblePerPosition[it.key.first]!!) }
            .toList().toImmutableList()

    return bestPlayers
}

data class Constraint(val slots: ImmutableList<Slot>, val players: ImmutableList<Player>, val budget: Int)

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

