package com.grasell

fun <A, B> List<A>.logicalMap(mapper: (A) -> B): List<B> {
    return LogicalMappedList(this, mapper)
}

class LogicalMappedList<A, B>(private val backing: List<A>, private val mapper: (A) -> B) : List<B> {
    override val size: Int
        get() = backing.size

    override fun contains(element: B): Boolean {
        for (mappedElement in this) {
            if (mappedElement == element) return true
        }

        return false
    }

    override fun containsAll(elements: Collection<B>): Boolean {
        // TODO: performance enhancements.  O(n^2) no bueno
        elements.forEach {
            if (! contains(it)) return false
        }

        return true
    }

    override fun get(index: Int) = mapper(backing[index])

    override fun indexOf(element: B): Int {
        this.forEachIndexed { index, b -> if (element == b) return index }
        return -1
    }

    override fun isEmpty() = backing.isEmpty()

    override fun iterator() = MappingIterator(backing.iterator(), mapper)

    override fun lastIndexOf(element: B) = indexOfLast { it == element }

    override fun listIterator() = MappingListIterator(backing.listIterator(), mapper)

    override fun listIterator(index: Int) = MappingListIterator(backing.listIterator(index), mapper)

    override fun subList(fromIndex: Int, toIndex: Int) = LogicalMappedList(backing.subList(fromIndex, toIndex), mapper)
}

open class MappingIterator<A, out B>(private val backingIterator: Iterator<A>, private val mapper: (A) -> (B)) : Iterator<B> {
    override fun hasNext() = backingIterator.hasNext()

    override fun next() = mapper(backingIterator.next())
}

class MappingListIterator<A, out B>(private val backingIterator: ListIterator<A>, private val mapper: (A) -> B)
    : MappingIterator<A, B>(backingIterator, mapper), ListIterator<B> {

    override fun hasPrevious() = backingIterator.hasPrevious()

    override fun nextIndex() = backingIterator.nextIndex()

    override fun previous() = mapper(backingIterator.previous())

    override fun previousIndex() = backingIterator.previousIndex()
}