package io.iskopasi.player_test.utils

import io.iskopasi.player_test.utils.Utils.e

class LoopIterator<T>(private val data: List<T> = listOf()) {
    var value: T? = null
    var index = -1

    init {
        if (data.isNotEmpty()) {
            index = 0
            value = data[index]
        }
    }

    fun reset() {
        index = 0
    }

    fun next(): T? {
        if (index == -1) return null

        value = if (index < data.size - 1) {
            data[++index]
        } else {
            index = 0
            data[index]
        }

        "next curIndex: index=$index; val=$value".e

        return value
    }

    fun prev(): T? {
        if (index == -1) return null

        value = if (index > 0) {
            data[--index]
        } else {
            index = data.size - 1
            data[index]
        }

        "prev curIndex: index=$index; val=$value".e

        return value
    }

    fun setIndex(id: Int): T? {
        if (0 <= id && id < data.size) {
            index = id
            value = data[index]
        }

        "setIndex to $id (0 <= $id < ${data.size}): index=$index; val=$value".e

        return value
    }
}