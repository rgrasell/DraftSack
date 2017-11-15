package com.grasell

import au.com.bytecode.opencsv.CSVReader
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

    val players = csv.readAll().map {
        Player(it[nameIndex].trim(), it[scoreIndex].toDouble().toInt(), it[costIndex].toInt(), it[positionIndex])
    }

    val slots = listOf(
            Slot(setOf("QB"), 1),
            Slot(setOf("RB"), 2),
            Slot(setOf("WR"), 3),
            Slot(setOf("TE"), 1),
            Slot(setOf("WR", "RB"), 1),
            Slot(setOf("DST"), 1)
    )

    val budget = 50_000

    val time = measureTimeMillis {
        val solution = solveDraftsack(players, budget, slots)
        solution?.players?.forEach { println(it) }
        println("total score: ${solution?.score}")
        println("total cost: ${solution?.cost}")
    }

    println("Running time: $time")
}