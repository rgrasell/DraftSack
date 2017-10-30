package com.grasell

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.immutableListOf
import kotlinx.collections.immutable.immutableSetOf
import kotlinx.collections.immutable.*

fun main(args: Array<String>) {
    println("Hello, World")

    val l = immutableListOf(1, 2, 3)
    val s = immutableSetOf(1, 2, 3)

    println(l.pop())
    println(l + 5)
    println(s)
}


fun <T> ImmutableList<T>.pop() = removeAt(lastIndex)
