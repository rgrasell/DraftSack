package com.grasell

import kotlinx.collections.immutable.immutableListOf
import kotlinx.collections.immutable.toImmutableList

fun main(args: Array<String>) {
    println("Hello, World")

    val slotTypes = listOf(
            "QB",
            "RB",
            "WR",
            "TE",
            "FLEX",
            "DST"
    )

    val players = List(464) {
        Player("p" + it, hash(it), it, immutableListOf(slotTypes.random(it), slotTypes.random(it + 1)))
    }.toImmutableList()

    val slots = listOf(
            Slot("QB", 1),
            Slot("RB", 2),
            Slot("WR", 3),
            Slot("TE", 1),
            Slot("FLEX", 1),
            Slot("DST", 1)
    )

    val solution = DraftSack().solve(players, hash(players), slots)
    println(solution)
}

private fun hash(any: Any, times: Int = 0) = Math.abs( (any.toString() + times.toString()).hashCode() )

private fun <T> List<T>.random(seed: Any) = get(hash(seed) % size)