package com.nightlynexus

import org.w3c.dom.Storage
import org.w3c.dom.get
import org.w3c.dom.set

internal class TileStorage(private val storage: Storage) {
  private val currentVersion = "1.0099999999900001"
  private val isCurrentVersion = run {
    val version = storage["version"]
    if (version == null || version != currentVersion) {
      storage["version"] = currentVersion
      false
    } else {
      true
    }
  }

  fun getShownStart(checkboxElementId: String): Boolean {
    if (!isCurrentVersion) return true
    val shown = storage[checkboxElementId] ?: return true
    return shown.toBoolean()
  }

  fun getUsedStart(ordinal: Int): Boolean {
    if (!isCurrentVersion) return false
    val used = storage["used_$ordinal"] ?: return false
    return used.toBoolean()
  }

  fun getSortUsedStart(): Boolean {
    if (!isCurrentVersion) return false
    val sortUsed = storage["sort_used"] ?: return false
    return sortUsed.toBoolean()
  }

  fun getMessengerTileOrdinalOrder(default: List<Int>): List<Int> {
    if (!isCurrentVersion) return default
    val encodedOrder = storage["messenger_tile_ordinal_order"] ?: return default
    return encodedOrder.decodeIntList()
  }

  fun setShown(checkboxElementId: String, checked: Boolean) {
    storage[checkboxElementId] = checked.toString()
  }

  fun setUsed(ordinal: Int, used: Boolean) {
    storage["used_$ordinal"] = used.toString()
  }

  fun setSortUsed(sortUsed: Boolean) {
    storage["sort_used"] = sortUsed.toString()
  }

  fun setMessengerTileOrdinalOrder(ordinals: List<Int>) {
    storage["messenger_tile_ordinal_order"] = ordinals.encode()
  }

  private fun List<Int>.encode(): String {
    val builder = StringBuilder()
    for (int in this) {
      builder
        .append(int)
        .append(',')
    }
    if (isNotEmpty()) {
      builder.setLength(builder.length - 1)
    }
    return builder.toString()
  }

  private fun String.decodeIntList(): List<Int> {
    return split(',').map { it.toInt() }
  }

  private fun <T> sortByPositions(list: MutableList<T>, positions: List<Int>) {
    require(list.size == positions.size)
    val result = ArrayList<T>(list.size)
    for (position in positions) {
      result += list[position]
    }
    list.clear()
    list += result
  }
}
