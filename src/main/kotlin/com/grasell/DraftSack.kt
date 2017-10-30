package com.grasell

class DraftSack {

    fun solve(players: List<Player>, budget: Int, slots: List<Slot>): Team? {
        val memoization = mutableMapOf<Constraint, Team?>()

        return solveRecursive(Constraint(slots, players, budget), memoization)
    }

    private fun solveRecursive(constraint: Constraint, memoization: MutableMap<Constraint, Team?>): Team? {
        if (constraint.isInvalid) return null

        val memoizedResult = memoization[constraint]
        if (memoizedResult != null) return memoizedResult

        // Actually calculate it.  Darn.
        var result: Team? = null

        // If we skip this player
        val playersWithoutCurrent = constraint.players.toMutableList()
        playersWithoutCurrent.removeAt(playersWithoutCurrent.lastIndex)
        val teamFromSkippingPlayer = solveRecursive(constraint.copy(players = playersWithoutCurrent), memoization)

        // Try putting this player into the slots they fit
        val currentPlayer = constraint.players.last()
        val budgetAfterPlayer = constraint.budget - currentPlayer.cost
        // Find intersection between slots in our constraint and slots the player fits
        val slotFits = constraint.slots.filter{ it.size > 0 }.filter{ it.fitsPlayer(currentPlayer) }
        val teamFromSlottingPlayer = slotFits.map{it.slotType}.map {
            val newSlots = copySlots(constraint.slots, it)
            solveRecursive(Constraint(newSlots, playersWithoutCurrent, budgetAfterPlayer), memoization)
        }.maxBy { it?.score ?: -1 }

        result = getBest(teamFromSkippingPlayer, teamFromSlottingPlayer)

        memoization[constraint] = result

        return result
    }
}

private fun getBest(t1: Team?, t2: Team?): Team? {
    if (t1 == null) return t2
    if (t2 == null) return t1

    if (t1.score > t2.score) {
        return t2
    }

    return t2
}

private fun copySlots(slots: List<Slot>, typeToDecrement: String): List<Slot> {
    return slots.map {
        if (it.slotType == typeToDecrement) {
            it.copy(size = it.size-1)
        } else {
            it
        }
    }
}

data class Constraint(val slots: List<Slot>, val players: List<Player>, val budget: Int) {
    val isInvalid
    get() = slots.isEmpty() || players.isEmpty() || budget < 0
}


data class Team(val slots: List<Slot>, val players: List<Player>, val budget: Int) {
    val score
    get() = players.stream().mapToInt{ it.score }.sum()

    val isValid
    get() = players.stream().mapToInt{ it.cost }.sum() <= budget

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

data class Player(val name: String, val score: Int, val cost: Int, val slotTypes: List<String>)

