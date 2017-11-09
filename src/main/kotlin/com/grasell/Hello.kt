package com.grasell

import au.com.bytecode.opencsv.CSVReader
import kotlinx.collections.immutable.immutableListOf
import kotlinx.collections.immutable.toImmutableList
import java.io.FileReader
import kotlin.system.measureTimeMillis

fun main(args: Array<String>) {
    println("Hello, World")

    val csv = CSVReader(FileReader("DKSalaries.csv"))
    val firstLine = csv.readNext()
    val nameIndex = firstLine.indexOf("Name")
    val scoreIndex = firstLine.indexOf("Score")
    val costIndex = firstLine.indexOf("Salary")
    val positionIndex = firstLine.indexOf("Position")

    csv.readAll().map {
        Player(it[nameIndex], it[scoreIndex].toInt(), it[costIndex].toInt(), it[positionIndex])
    }

    val slotTypes = listOf(
            "QB",
            "RB",
            "WR",
            "TE",
            "DST"
    )

    val players = List(464) {
        Player("p" + it, hash(it), it, slotTypes.random(it))
    }.toImmutableList()

    val slots = listOf(
            Slot(setOf("QB"), 1),
            Slot(setOf("RB"), 2),
            Slot(setOf("WR"), 3),
            Slot(setOf("TE"), 1),
            Slot(setOf("FLEX"), 1),
            Slot(setOf("DST"), 1)
    )

    val budget = 50_000

    val time = measureTimeMillis {
        val solution = solveDraftsack(players, budget, slots)
        println(solution)
    }

    println("Running time: $time")
}

private fun hash(any: Any, times: Int = 0) = Math.abs( (any.toString() + times.toString()).hashCode() )

private fun <T> List<T>.random(seed: Any) = get(hash(seed) % size)