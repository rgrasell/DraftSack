package com.grasell

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.immutableListOf
import kotlinx.collections.immutable.toImmutableList

val emptyTeam = Team(immutableListOf())


fun solveDraftsack(players: List<Player>, budget: Int, slots: List<Slot>): Team? {
    val memoization = mutableMapOf<Constraint, Team?>()

    return solveRecursive(Constraint(slots.toImmutableList(), players.toImmutableList(), budget), memoization)
}

private fun solveRecursive(constraint: Constraint, memoization: MutableMap<Constraint, Team?>): Team? {
    if (constraint.budget < 0) return null
    if (constraint.players.isEmpty()) return emptyTeam // no players -> empty team
    println("Players: ${constraint.players.size}")

    val memoizedResult = memoization[constraint]
    if (memoizedResult != null) {
        return memoizedResult
    }

    // Actually calculate it.  What a drag.
    var result: Team? = null

    // If we skip this player
    val playersWithoutCurrent = constraint.players.pop()
    val teamFromSkippingPlayer = solveRecursive(constraint.copy(players = playersWithoutCurrent), memoization)

    // Try putting this player into the slots they fit
    val currentPlayer = constraint.players.last()
    val budgetAfterPlayer = constraint.budget - currentPlayer.cost
    // Find intersection between slots in our constraint and slots the player fits
    val slotFits = constraint.slots.asSequence().filter{ it.size > 0 && it.fitsPlayer(currentPlayer) }
    val teamFromSlottingPlayer = slotFits.map{ it.slotType }.map {
        val newSlots = copySlots(constraint.slots, it)
        val potentialTeam = solveRecursive(Constraint(newSlots, playersWithoutCurrent, budgetAfterPlayer), memoization)
        potentialTeam?.withPlayer(currentPlayer)
    }.maxBy { it?.score ?: -1 }

    result = getBest(teamFromSkippingPlayer, teamFromSlottingPlayer)

    if (result == null) result = emptyTeam // Best we can do is an empty team :(

    memoization[constraint] = result
    println("memoized table now at ${memoization.size}")

    return result
}

private fun getBest(t1: Team?, t2: Team?): Team? {
    if (t1 == null) return t2
    if (t2 == null) return t1

    if (t1.score > t2.score) {
        return t1
    }

    return t2
}

private fun copySlots(slots: List<Slot>, typeToDecrement: String): ImmutableList<Slot> {
    return slots.map {
        if (it.slotType == typeToDecrement) {
            it.copy(size = it.size-1)
        } else {
            it
        }
    }.toImmutableList()
}

data class Constraint(val slots: ImmutableList<Slot>, val players: ImmutableList<Player>, val budget: Int)

fun <T> ImmutableList<T>.pop() = removeAt(lastIndex)

data class Team(val players: ImmutableList<Player>) {
    val score
    get() = players.asSequence().map { it.score }.sum()

    fun withPlayer(player: Player) = copy(players = players.add(player))

//    fun withPlayer(player: Player) {
//        val potentialSlots = slots.intersect(player.slotTypes)
//
//        var bestSolution = this
//        for (replacedSlot in potentialSlots) {
//            val newSolution = Team(slots, )
//        }
//    }
}

data class Slot(val slotType: String, val size: Int) {
    fun fitsPlayer(player: Player) = player.slotTypes.contains(slotType)
}

data class Player(val name: String, val score: Int, val cost: Int, val slotTypes: ImmutableList<String>)

