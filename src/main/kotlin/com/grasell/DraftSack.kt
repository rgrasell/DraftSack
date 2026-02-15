package com.grasell

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.immutableListOf
import kotlinx.collections.immutable.toImmutableList

val emptyTeam = Team(immutableListOf())


fun solveDraftsack(players: List<Player>, budget: Int, slots: List<Slot>, numPlayersCallback: (Int) -> Unit = {}, memoizedSizeCallback: (Int) -> Unit = {}): Team? {
    val memoization = mutableMapOf<Constraint, Team?>()
    val culledPlayerList = cullPlayers(players, slots)
    val bestSoFar = BestTracker()

    return solveRecursive(Constraint(slots.toImmutableList(), culledPlayerList, budget), memoization, bestSoFar, numPlayersCallback, memoizedSizeCallback)
}

// Tracks best score found so far for pruning
class BestTracker(var bestScore: Int = 0)

private fun solveRecursive(constraint: Constraint, memoization: MutableMap<Constraint, Team?>, bestSoFar: BestTracker, numPlayersCallback: (Int) -> Unit, memoizedSizeCallback: (Int) -> Unit): Team? {
    if (constraint.budget < 0) return null
    if (constraint.players.isEmpty()) return emptyTeam // no players -> empty team

    // Early termination: if upper bound can't beat current best, prune this branch
    if (constraint.upperBound() <= bestSoFar.bestScore) {
        return null
    }

    numPlayersCallback(constraint.players.size)

    return memoization.getOrPut(constraint) {
        // Actually calculate it.  What a drag.
        var result: Team?

        // If we skip this player
        val playersWithoutCurrent = constraint.players.pop()
        val teamFromSkippingPlayer = solveRecursive(constraint.copy(players = playersWithoutCurrent), memoization, bestSoFar, numPlayersCallback, memoizedSizeCallback)

        // Try putting this player into the slots they fit
        val currentPlayer = constraint.players.last()
        val budgetAfterPlayer = constraint.budget - currentPlayer.cost
        // Find intersection between slots in our constraint and slots the player fits
        val teamFromSlottingPlayer = constraint.slots.asSequence()
            .filter { it.size > 0 && it.fitsPlayer(currentPlayer) }
            .map {
                val newSlots = copySlots(constraint.slots, it)
                val potentialTeam = solveRecursive(Constraint(newSlots, playersWithoutCurrent, budgetAfterPlayer), memoization, bestSoFar, numPlayersCallback, memoizedSizeCallback)
                potentialTeam?.withPlayer(currentPlayer)
        }.maxBy { it?.score ?: -1 }

        result = getBest(teamFromSkippingPlayer, teamFromSlottingPlayer)

        if (result == null) result = emptyTeam // Best we can do is an empty team :(

        // Update best score found so far
        result?.score?.let { score ->
            if (score > bestSoFar.bestScore) {
                bestSoFar.bestScore = score
            }
        }

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

    // Step 1: Remove dominated players (same position, higher/equal cost, lower score)
    val nonDominatedPlayers = players.asSequence()
            .filter { it.score > 0 }
            .groupBy { it.position }
            .flatMap { (_, positionPlayers) ->
                val sorted = positionPlayers.sortedWith(compareBy({ it.cost }, { -it.score }))
                val nonDominated = mutableListOf<Player>()
                var maxScoreSoFar = Int.MIN_VALUE

                for (player in sorted) {
                    if (player.score > maxScoreSoFar) {
                        nonDominated.add(player)
                        maxScoreSoFar = player.score
                    }
                }
                nonDominated
            }

    // Step 2: Keep only top N players per (position, cost) combination
    val bestPlayers = nonDominatedPlayers
            .groupBy { Pair(it.position, it.cost) }.asSequence()
            .flatMap { it.value.asSequence().sortedByDescending { it.score }.take(maxPossiblePerPosition[it.key.first]!!) }
            .sortedByDescending { it.valueRatio() }
            .toList().toImmutableList()

    return bestPlayers
}

// Calculate value per dollar ratio for player ranking
private fun Player.valueRatio(): Double = if (cost > 0) score.toDouble() / cost else 0.0

data class Constraint(val slots: ImmutableList<Slot>, val players: ImmutableList<Player>, val budget: Int) : Comparable<Constraint> {
    // Cache hashCode for faster memoization lookups
    private val cachedHashCode by lazy {
        var result = slots.hashCode()
        result = 31 * result + players.hashCode()
        result = 31 * result + budget
        result
    }

    override fun hashCode(): Int = cachedHashCode

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

    // Calculate upper bound for pruning (optimistic estimate of best possible score)
    fun upperBound(): Int {
        val totalSlots = slots.sumOf { it.size }
        return players.asSequence()
                .take(minOf(totalSlots, players.size))
                .sumOf { it.score }
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

