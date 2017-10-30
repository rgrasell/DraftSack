package com.grasell

import java.util.*
import kotlin.NoSuchElementException

class TripleArray<T>(val xLen: Int, val yLen: Int, val zLen: Int, initialValue: () -> T) {
    val data = MutableList(xLen * yLen * zLen) { initialValue() }

    operator fun set(x: Int, y: Int, z: Int, t: T) {
        data[toIndex(x, y, z)] = t
    }

    operator fun get(x: Int, y: Int, z: Int): T {
        return data[toIndex(x, y, z)]
    }

    private fun toIndex(x: Int, y: Int, z: Int) = xLen * yLen * z + xLen * y + x
}


class NArray<T>(private val dimensions: List<Int>, private val initialValue: () -> T) {
    val data = MutableList(dimensions.stream().reduce{ left, right -> left * right }.get()) { initialValue() }
    val workaroundData = mutableMapOf<List<Int>, T>()


    operator fun set(coords: List<Int>, t: T) {
        checkArity(coords)
        workaroundData[coords] = t
    }

    operator fun get(coords: List<Int>): T {
        checkArity(coords)
        return workaroundData[coords] ?: initialValue()
    }

    private fun checkArity(coords: List<Int>) {
        if (coords.size != dimensions.size) throw NoSuchElementException("Input doesn't match dimensions of the array.")
    }

    fun forEach(action: (List<Int>) -> Unit) {
        NotImplementedError("Back to work.")
    }

//    operator fun set(coords: List<Int>, t: T) {
//        data[toIndex(coords)] = t
//    }
//
//    operator fun get(coords: List<Int>): T {
//        return data[toIndex(coords)]
//    }
//
//    private fun toIndex(coords: List<Int>): Int {
//        var flattenedCoord = 0
//        dimensions.forEachIndexed { index, value ->
//            flattenedCoord += value + ()
//        }
//    }
}