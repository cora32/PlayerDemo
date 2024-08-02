package io.iskopasi.player_test.utils

import io.iskopasi.player_test.utils.Utils.e

class LoopIterator<T>(val dataList: List<T> = listOf()) {
    var value: T? = null
    private var index = -1

    init {
        if (dataList.isNotEmpty()) {
            index = 0
            value = dataList[index]
        }
    }

    fun reset() {
        index = 0
    }

    fun next(): T? {
        if (index == -1) return null

        value = if (index < dataList.size - 1) {
            dataList[++index]
        } else {
            index = 0
            dataList[index]
        }

        "next curIndex: index=$index; val=$value".e

        return value
    }

    fun prev(): T? {
        if (index == -1) return null

        value = if (index > 0) {
            dataList[--index]
        } else {
            index = dataList.size - 1
            dataList[index]
        }

        "prev curIndex: index=$index; val=$value".e

        return value
    }

    fun setIndex(id: Int): T? {
        if (0 <= id && id < dataList.size) {
            index = id
            value = dataList[index]
        }

        "setIndex to $id (0 <= $id < ${dataList.size}): index=$index; val=$value".e

        return value
    }

    fun get(index: Int) = dataList[index]
}