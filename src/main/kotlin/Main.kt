package com.nightlynexus

import kotlinx.browser.document
import kotlinx.browser.localStorage
import kotlinx.browser.window
import kotlinx.dom.appendElement
import kotlinx.dom.appendText
import kotlinx.dom.createElement
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.get

fun main() {
  val tileStorage = TileStorage(localStorage)

  val totalTileCount = baseTiles.size +
    riverTiles.size +
    innsAndCathedralsTiles.size +
    tradersAndBuildersTiles.size +
    princessAndDragonTiles.size +
    dragonAndFairyTiles.size +
    flyingMachinesTiles.size +
    ferriesTiles.size +
    goldminesTiles.size +
    mageAndWitchTiles.size +
    robbersTiles.size +
    cropCircles.size
  val allTileElements = ArrayList<TileElement>(totalTileCount)

  val grid = document.getElementById("grid-container")!!
  val inPileCountDisplay = document.getElementById("in-pile-count")!!
  val usedCountDisplay = document.getElementById("used-count")!!
  val sortUsedCheckbox = document.getElementById("sort-used") as HTMLInputElement
  val resetButton = document.getElementById("reset") as HTMLButtonElement

  val shownTilesCount = ShownTilesCount(0, 0)

  val messengersTilesSize = messengersTiles.size
  val messengerTileElements = ArrayList<TileElement>(messengersTilesSize)
  val messengerTileElementPositionsDefault = ArrayList<Int>(messengersTilesSize)
  for (i in 0 until messengersTilesSize) {
    messengerTileElementPositionsDefault += i
  }
  val messengerTileElementPositions = tileStorage.getMessengerTileOrdinalOrder(
    messengerTileElementPositionsDefault
  ).toMutableList()
  check(messengerTileElementPositions.size == messengersTilesSize)
  val messengersShownTilesCount = ShownTilesCount(0, 0)
  val messengerTileElementListener = object : TileElement.Listener {
    override fun onShowChanged(tileElement: TileElement, show: Boolean) {
      // Do nothing.
    }

    override fun onUsedChanged(tileElement: TileElement, used: Boolean) {
      if (used) {
        if (messengersShownTilesCount.used == messengersTilesSize - 1) {
          messengersShownTilesCount.inPile--
          messengersShownTilesCount.used++
          for (i in messengersTilesSize - 1 downTo 0) {
            val messengerTileElement = messengerTileElements[i]
            messengerTileElement.used = false
          }
          return
        }
        messengerTileElements.remove(tileElement)
        messengerTileElements.add(messengersShownTilesCount.used, tileElement)
        messengersShownTilesCount.inPile--
        messengersShownTilesCount.used++
      } else {
        messengerTileElements.remove(tileElement)
        messengersShownTilesCount.used--
        messengerTileElements.add(messengersShownTilesCount.used, tileElement)
      }
      val gridChildren = grid.childNodes
      for (i in messengersTilesSize - 1 downTo 0) {
        grid.removeChild(gridChildren[i]!!)
      }
      val firstChild = grid.firstChild
      messengerTileElementPositions.clear()
      for (messengerTileElement in messengerTileElements) {
        grid.insertBefore(messengerTileElement.element, firstChild)
        messengerTileElementPositions += messengerTileElement.ordinal
      }
      tileStorage.setUsed(tileElement.ordinal, used)
      tileStorage.setMessengerTileOrdinalOrder(messengerTileElementPositions)

      resetButton.disabled = shownTilesCount.used == 1
        && messengersShownTilesCount.used == 0
        && messengerTileElementPositions == messengerTileElementPositionsDefault
    }
  }
  val messengerTileElementsDefault = ArrayList<TileElement>(messengersTilesSize)
  var ordinal = document.createTiles(
    messengersTiles,
    "messengers",
    grid,
    messengerTileElementsDefault,
    ordinalStart = 0,
    messengersShownTilesCount,
    messengerTileElementListener,
    tileStorage
  )
  for (position in messengerTileElementPositions) {
    val messengerTileElement = messengerTileElementsDefault[position]
    messengerTileElements += messengerTileElement
    grid.appendChild(messengerTileElement.element)
  }

  resetButton.addEventListener("click", {
    if (window.confirm("Reset all tiles?")) {
      for (tileElement in allTileElements) {
        if (tileElement.tile.extra !== Tile.Extra.Source) {
          if (tileElement.used) {
            tileElement.used = false
          }
        }
      }
      for (i in messengerTileElements.size - 1 downTo 0) {
        val messengerTileElement = messengerTileElements[i]
        if (messengerTileElement.used) {
          messengerTileElement.used = false
        }
      }

      // Reset messenger tiles and original ordering.
      val firstChild = grid.childNodes[messengersTilesSize]
      messengerTileElements.sortBy {
        it.ordinal
      }
      resetButton.disabled = true
      for (i in 0 until messengersTilesSize) {
        grid.insertBefore(messengerTileElements[i].element, firstChild)
      }
      tileStorage.setMessengerTileOrdinalOrder(messengerTileElementPositionsDefault)
    }
  })

  val defaultComparator = Comparator<TileElement> { a, b ->
    a.ordinal - b.ordinal
  }
  val sortUsedComparator = Comparator<TileElement> { a, b ->
    if (a.used && !b.used) {
      1
    } else if (!a.used && b.used) {
      -1
    } else {
      a.ordinal - b.ordinal
    }
  }

  var sortUsed = tileStorage.getSortUsedStart()
  sortUsedCheckbox.addEventListener("change", {
    sortUsed = (it.target as HTMLInputElement).checked
    allTileElements.sortWith(if (sortUsed) sortUsedComparator else defaultComparator)
    val gridChildren = grid.childNodes
    for (i in gridChildren.length - 1 downTo messengersTilesSize) {
      grid.removeChild(gridChildren[i]!!)
    }
    for (tileElement in allTileElements) {
      grid.appendChild(tileElement.element)
    }

    tileStorage.setSortUsed(sortUsed)
  })
  val tileElementListener = object : TileElement.Listener {
    override fun onShowChanged(tileElement: TileElement, show: Boolean) {
      val used = tileElement.used
      if (show) {
        if (used) {
          shownTilesCount.used++
        } else {
          shownTilesCount.inPile++
        }
      } else {
        if (used) {
          shownTilesCount.used--
        } else {
          shownTilesCount.inPile--
        }
      }
      inPileCountDisplay.textContent = shownTilesCount.inPile.toString()
      usedCountDisplay.textContent = shownTilesCount.used.toString()
    }

    override fun onUsedChanged(tileElement: TileElement, used: Boolean) {
      if (sortUsed) {
        allTileElements.sortWith(sortUsedComparator)
        val gridChildren = grid.childNodes
        for (i in gridChildren.length - 1 downTo messengersTilesSize) {
          grid.removeChild(gridChildren[i]!!)
        }
        for (tileElementToAppend in allTileElements) {
          grid.appendChild(tileElementToAppend.element)
        }
      }
      if (tileElement.show) {
        if (used) {
          shownTilesCount.inPile--
          shownTilesCount.used++
        } else {
          shownTilesCount.inPile++
          shownTilesCount.used--
        }
        inPileCountDisplay.textContent = shownTilesCount.inPile.toString()
        usedCountDisplay.textContent = shownTilesCount.used.toString()
      }

      resetButton.disabled = shownTilesCount.used == 1
        && messengersShownTilesCount.used == 0
        && messengerTileElementPositions == messengerTileElementPositionsDefault

      tileStorage.setUsed(tileElement.ordinal, used)
    }
  }

  val riverCheckboxElementId = "river"
  var showRiver = tileStorage.getShownStart(riverCheckboxElementId)
  val dragonAndFairyCheckboxElementId = "dragon-and-fairy"
  var showDragonAndFairy = tileStorage.getShownStart(dragonAndFairyCheckboxElementId)
  val dragonAndFairyRiverTileElements = ArrayList<TileElement>(2)

  var baseSourceTileElement: TileElement? = null
  for (tile in baseTiles) {
    val isSource = tile.extra === Tile.Extra.Source
    val show = !isSource || !showRiver
    val used = isSource || tileStorage.getUsedStart(ordinal)
    val baseTileElement = grid.createTile(
      tile,
      ordinal,
      show,
      used,
      shownTilesCount,
      tileElementListener
    )
    if (isSource) {
      check(baseSourceTileElement == null)
      baseSourceTileElement = baseTileElement
    }
    allTileElements += baseTileElement
    ordinal++
  }
  checkNotNull(baseSourceTileElement)

  var riverSourceTileElement: TileElement? = null
  val riverTileElements = ArrayList<TileElement>(riverTiles.size)
  for (tile in riverTiles) {
    val isSource = tile.extra === Tile.Extra.Source
    // Dragon & Fairy has a replacement river source tile.
    val show = showRiver && (!isSource || !showDragonAndFairy)
    val used = isSource || tileStorage.getUsedStart(ordinal)
    val riverTileElement = grid.createTile(
      tile,
      ordinal,
      show,
      used,
      shownTilesCount,
      tileElementListener
    )
    if (isSource) {
      check(riverSourceTileElement == null)
      riverSourceTileElement = riverTileElement
    }
    riverTileElements += riverTileElement
    allTileElements += riverTileElement
    ordinal++
  }
  checkNotNull(riverSourceTileElement)
  val riverCheckboxElement = document.getElementById(
    riverCheckboxElementId
  ) as HTMLInputElement
  riverCheckboxElement.checked = showRiver
  riverCheckboxElement.addEventListener("change", {
    val checked = (it.target as HTMLInputElement).checked
    showRiver = checked
    baseSourceTileElement.show = !checked
    // Show or hide the river tiles in Dragon & Fairy.
    if (showDragonAndFairy) {
      for (dragonAndFairyRiverTileElement in dragonAndFairyRiverTileElements) {
        dragonAndFairyRiverTileElement.show = checked
      }
    }
    for (riverTileElement in riverTileElements) {
      val isSource = riverTileElement.tile.extra === Tile.Extra.Source
      // The source river tile has no change (still not shown) when we are showing Dragon & Fairy.
      if (!isSource || !showDragonAndFairy) {
        riverTileElement.show = checked
      }
    }

    tileStorage.setShown(riverCheckboxElementId, checked)
  })

  ordinal = document.createTiles(
    innsAndCathedralsTiles,
    "inns-and-cathedrals",
    grid,
    allTileElements,
    ordinal,
    shownTilesCount,
    tileElementListener,
    tileStorage
  )

  ordinal = document.createTiles(
    tradersAndBuildersTiles,
    "traders-and-builders",
    grid,
    allTileElements,
    ordinal,
    shownTilesCount,
    tileElementListener,
    tileStorage
  )

  ordinal = document.createTiles(
    princessAndDragonTiles,
    "princess-and-dragon",
    grid,
    allTileElements,
    ordinal,
    shownTilesCount,
    tileElementListener,
    tileStorage
  )

  val dragonAndFairyTileElements = ArrayList<TileElement>(dragonAndFairyTiles.size)
  for (tile in dragonAndFairyTiles) {
    val isSource = tile.extra === Tile.Extra.Source
    val show = showDragonAndFairy && (showRiver || !tile.hasRiver)
    val used = isSource || tileStorage.getUsedStart(ordinal)
    val tileElement = grid.createTile(
      tile,
      ordinal,
      show,
      used,
      shownTilesCount,
      tileElementListener
    )
    if (tile.hasRiver) {
      dragonAndFairyRiverTileElements += tileElement
    }
    dragonAndFairyTileElements += tileElement
    allTileElements += tileElement
    ordinal++
  }
  val dragonAndFairyCheckboxElement = document.getElementById(
    dragonAndFairyCheckboxElementId
  ) as HTMLInputElement
  dragonAndFairyCheckboxElement.checked = showDragonAndFairy
  dragonAndFairyCheckboxElement.addEventListener("change", {
    val checked = (it.target as HTMLInputElement).checked
    showDragonAndFairy = checked
    // If we show the river, show the river's source tile element if and only if we do not show
    // Dragon & Fairy.
    if (showRiver) {
      riverSourceTileElement.show = !checked
    }
    for (dragonAndFairyTileElement in dragonAndFairyTileElements) {
      val isRiver = dragonAndFairyTileElement.tile.hasRiver
      // The Dragon & Fairy river tiles, including the Dragon & Fairy river source tile, have no
      // change (still not shown) when we are not showing the river.
      if (!isRiver || showRiver) {
        dragonAndFairyTileElement.show = checked
      }
    }

    tileStorage.setShown(dragonAndFairyCheckboxElementId, checked)
  })

  ordinal = document.createTiles(
    flyingMachinesTiles,
    "flying-machines",
    grid,
    allTileElements,
    ordinal,
    shownTilesCount,
    tileElementListener,
    tileStorage
  )

  ordinal = document.createTiles(
    ferriesTiles,
    "ferries",
    grid,
    allTileElements,
    ordinal,
    shownTilesCount,
    tileElementListener,
    tileStorage
  )

  ordinal = document.createTiles(
    goldminesTiles,
    "goldmines",
    grid,
    allTileElements,
    ordinal,
    shownTilesCount,
    tileElementListener,
    tileStorage
  )

  ordinal = document.createTiles(
    mageAndWitchTiles,
    "mage-and-witch",
    grid,
    allTileElements,
    ordinal,
    shownTilesCount,
    tileElementListener,
    tileStorage
  )

  ordinal = document.createTiles(
    robbersTiles,
    "robbers",
    grid,
    allTileElements,
    ordinal,
    shownTilesCount,
    tileElementListener,
    tileStorage
  )

  ordinal = document.createTiles(
    cropCircles,
    "crop-circles",
    grid,
    allTileElements,
    ordinal,
    shownTilesCount,
    tileElementListener,
    tileStorage
  )

  inPileCountDisplay.textContent = shownTilesCount.inPile.toString()
  usedCountDisplay.textContent = shownTilesCount.used.toString()

  resetButton.disabled = shownTilesCount.used == 1
    && messengersShownTilesCount.used == 0
    && messengerTileElementPositions == messengerTileElementPositionsDefault

  // This does not call sortUsedCheckbox's change event listener, so we have to do an initial sort.
  sortUsedCheckbox.checked = sortUsed
  allTileElements.sortWith(if (sortUsed) sortUsedComparator else defaultComparator)
  val gridChildren = grid.childNodes
  for (i in gridChildren.length - 1 downTo messengersTilesSize) {
    grid.removeChild(gridChildren[i]!!)
  }
  for (tileElement in allTileElements) {
    grid.appendChild(tileElement.element)
  }
}

private class ShownTilesCount(var inPile: Int, var used: Int)

private fun Document.createTiles(
  tiles: List<Tile>,
  checkboxElementId: String,
  grid: Element,
  allTileElements: MutableList<TileElement>,
  ordinalStart: Int,
  shownTilesCount: ShownTilesCount,
  tileElementListener: TileElement.Listener,
  tileStorage: TileStorage
): Int {
  val show = tileStorage.getShownStart(checkboxElementId)
  val tileElements = ArrayList<TileElement>(tiles.size)
  var ordinal = ordinalStart
  for (tile in tiles) {
    val used = tileStorage.getUsedStart(ordinal)
    val tileElement = grid.createTile(
      tile,
      ordinal,
      show,
      used,
      shownTilesCount,
      tileElementListener
    )
    tileElements += tileElement
    allTileElements += tileElement
    ordinal++
  }
  val checkboxElement = getElementById(
    checkboxElementId
  ) as HTMLInputElement
  checkboxElement.checked = show
  checkboxElement.addEventListener("change", {
    val checked = (it.target as HTMLInputElement).checked
    for (tileElement in tileElements) {
      tileElement.show = checked
    }

    tileStorage.setShown(checkboxElementId, checked)
  })
  return ordinal
}

private fun Element.createTile(
  tile: Tile,
  ordinal: Int,
  show: Boolean,
  used: Boolean,
  shownTilesCount: ShownTilesCount,
  listener: TileElement.Listener
): TileElement {
  if (show) {
    if (used) {
      shownTilesCount.used++
    } else {
      shownTilesCount.inPile++
    }
  }
  return TileElement(
    tile,
    ownerDocument!!,
    ordinal,
    show,
    used,
    listener
  )
}

private class TileElement(
  val tile: Tile,
  ownerDocument: Document,
  val ordinal: Int,
  show: Boolean,
  used: Boolean,
  private val listener: Listener,
) {
  interface Listener {
    fun onShowChanged(tileElement: TileElement, show: Boolean)
    fun onUsedChanged(tileElement: TileElement, used: Boolean)
  }

  var show = show
    set(value) {
      check(value != field)
      field = value
      val display = if (value) "initial" else "none"
      element.setAttribute("style", "display: $display;")
      listener.onShowChanged(this, value)
    }
  var used = used
    set(value) {
      check(value != field)
      field = value
      val grayscale = if (value) "100%" else "0%"
      imageElement.setAttribute("style", "filter: grayscale($grayscale);")
      listener.onUsedChanged(this, value)
    }

  // This is a val, but createElement doesn't have a Kotlin contract
  // to promise the compiler the capture is run once.
  private var imageElement: Element

  val element = ownerDocument.createElement("figure") {
    val display = if (show) "initial" else "none"
    setAttribute("style", "display: $display;")

    imageElement = appendElement("img") {
      setAttribute("src", tile.path)

      val grayscale = if (this@TileElement.used) "100%" else "0%"
      setAttribute("style", "filter: grayscale($grayscale);")

      if (tile.extra !== Tile.Extra.Source) {
        addEventListener("click", {
          this@TileElement.used = !this@TileElement.used
        })
      }
    }
    appendElement("figcaption") {
      if (tile.extra === Tile.Extra.Source) {
        appendElement("b") {
          appendText(Tile.Extra.Source.text)
        }
      } else {
        appendText(tile.extra.text)
      }
    }
  }
}

private val baseTiles = listOf(
  Tile(
    "base/Base_Game_C3_Tile_A.png",
    hasRoad = true,
    hasMonastery = true
  ),
  Tile(
    "base/Base_Game_C3_Tile_A.png",
    hasRoad = true,
    hasMonastery = true,
    extra = Tile.Extra.Highwaymen
  ),
  Tile(
    "base/Base_Game_C3_Tile_B.png",
    hasMonastery = true
  ),
  Tile(
    "base/Base_Game_C3_Tile_B.png",
    hasMonastery = true
  ),
  Tile(
    "base/Base_Game_C3_Tile_B.png",
    hasMonastery = true,
    extra = Tile.Extra.Farmhouse
  ),
  Tile(
    "base/Base_Game_C3_Tile_B.png",
    hasMonastery = true,
    extra = Tile.Extra.DonkeyStable
  ),
  Tile(
    "base/Base_Game_C3_Tile_C.png",
    hasCastle = true,
    hasShieldCastle = true,
    extra = Tile.Extra.WaterTower
  ),
  Tile(
    "base/Base_Game_C3_Tile_D.png",
    hasRoad = true,
    hasCastle = true,
    extra = Tile.Extra.Source
  ),
  Tile(
    "base/Base_Game_C3_Tile_D.png",
    hasRoad = true,
    hasCastle = true
  ),
  Tile(
    "base/Base_Game_C3_Tile_D.png",
    hasRoad = true,
    hasCastle = true,
    extra = Tile.Extra.Farmhouse
  ),
  Tile(
    "base/Base_Game_C3_Tile_D.png",
    hasRoad = true,
    hasCastle = true,
    extra = Tile.Extra.Pigsty
  ),
  Tile(
    "base/Base_Game_C3_Tile_E.png",
    hasCastle = true
  ),
  Tile(
    "base/Base_Game_C3_Tile_E.png",
    hasCastle = true
  ),
  Tile(
    "base/Abbot-Base_Game_C2_Tile_E_Garden.jpg",
    hasCastle = true,
    extra = Tile.Extra.Garden
  ),
  Tile(
    "base/Base_Game_C3_Tile_E.png",
    hasCastle = true,
    extra = Tile.Extra.Farmhouse
  ),
  Tile(
    "base/Base_Game_C3_Tile_E.png",
    hasCastle = true,
    extra = Tile.Extra.WaterTower
  ),
  Tile(
    "base/Base_Game_C3_Tile_F.png",
    hasCastle = true,
    hasShieldCastle = true
  ),
  Tile(
    "base/Base_Game_C3_Tile_F.png",
    hasCastle = true,
    hasShieldCastle = true,
    extra = Tile.Extra.WaterTower
  ),
  Tile(
    "base/Base_Game_C3_Tile_G.png",
    hasCastle = true
  ),
  Tile(
    "base/Base_Game_C3_Tile_H.png",
    hasCastle = true
  ),
  Tile(
    "base/Abbot-Base_Game_C2_Tile_H_Garden.jpg",
    hasCastle = true,
    extra = Tile.Extra.Garden
  ),
  Tile(
    "base/Base_Game_C3_Tile_H.png",
    hasCastle = true,
    extra = Tile.Extra.WaterTower
  ),
  Tile(
    "base/Base_Game_C3_Tile_I.png",
    hasCastle = true
  ),
  Tile(
    "base/Abbot-Base_Game_C2_Tile_I_Garden.jpg",
    hasCastle = true,
    extra = Tile.Extra.Garden
  ),
  Tile(
    "base/Base_Game_C3_Tile_J.png",
    hasRoad = true,
    hasCastle = true,
    extra = Tile.Extra.Highwaymen
  ),
  Tile(
    "base/Base_Game_C3_Tile_J.png",
    hasRoad = true,
    hasCastle = true,
    extra = Tile.Extra.Farmhouse
  ),
  Tile(
    "base/Base_Game_C3_Tile_J.png",
    hasRoad = true,
    hasCastle = true,
    extra = Tile.Extra.Cowshed
  ),
  Tile(
    "base/Base_Game_C3_Tile_K.png",
    hasRoad = true,
    hasCastle = true,
    extra = Tile.Extra.Highwaymen
  ),
  Tile(
    "base/Base_Game_C3_Tile_K.png",
    hasRoad = true,
    hasCastle = true,
    extra = Tile.Extra.Farmhouse
  ),
  Tile(
    "base/Base_Game_C3_Tile_K.png",
    hasRoad = true,
    hasCastle = true,
    extra = Tile.Extra.DonkeyStable
  ),
  Tile(
    "base/Base_Game_C3_Tile_L.png",
    hasRoad = true,
    hasCastle = true
  ),
  Tile(
    "base/Base_Game_C3_Tile_L.png",
    hasRoad = true,
    hasCastle = true
  ),
  Tile(
    "base/Base_Game_C3_Tile_L.png",
    hasRoad = true,
    hasCastle = true
  ),
  Tile(
    "base/Base_Game_C3_Tile_M.png",
    hasCastle = true,
    hasShieldCastle = true
  ),
  Tile(
    "base/Abbot-Base_Game_C2_Tile_M_Garden.jpg",
    hasCastle = true,
    hasShieldCastle = true,
    extra = Tile.Extra.Garden
  ),
  Tile(
    "base/Base_Game_C3_Tile_N.png",
    hasCastle = true
  ),
  Tile(
    "base/Abbot-Base_Game_C2_Tile_N_Garden.jpg",
    hasCastle = true,
    extra = Tile.Extra.Garden
  ),
  Tile(
    "base/Base_Game_C3_Tile_N.png",
    hasCastle = true,
    extra = Tile.Extra.WaterTower
  ),
  Tile(
    "base/Base_Game_C3_Tile_O.png",
    hasRoad = true,
    hasCastle = true,
    hasShieldCastle = true,
    extra = Tile.Extra.Farmhouse
  ),
  Tile(
    "base/Base_Game_C3_Tile_O.png",
    hasRoad = true,
    hasCastle = true,
    hasShieldCastle = true,
    extra = Tile.Extra.Cowshed
  ),
  Tile(
    "base/Base_Game_C3_Tile_P.png",
    hasRoad = true,
    hasCastle = true
  ),
  Tile(
    "base/Base_Game_C3_Tile_P.png",
    hasRoad = true,
    hasCastle = true,
    extra = Tile.Extra.Highwaymen
  ),
  Tile(
    "base/Base_Game_C3_Tile_P.png",
    hasRoad = true,
    hasCastle = true,
    extra = Tile.Extra.WaterTower
  ),
  Tile(
    "base/Base_Game_C3_Tile_Q.png",
    hasCastle = true,
    hasShieldCastle = true
  ),
  Tile(
    "base/Base_Game_C3_Tile_R.png",
    hasCastle = true
  ),
  Tile(
    "base/Abbot-Base_Game_C2_Tile_R_Garden.jpg",
    hasCastle = true,
    extra = Tile.Extra.Garden
  ),
  Tile(
    "base/Base_Game_C3_Tile_R.png",
    hasCastle = true,
    extra = Tile.Extra.WaterTower
  ),
  Tile(
    "base/Base_Game_C3_Tile_S.png",
    hasRoad = true,
    hasCastle = true,
    hasShieldCastle = true
  ),
  Tile(
    "base/Base_Game_C3_Tile_S.png",
    hasRoad = true,
    hasCastle = true,
    hasShieldCastle = true,
    extra = Tile.Extra.WaterTower
  ),
  Tile(
    "base/Base_Game_C3_Tile_T.png",
    hasRoad = true,
    hasCastle = true
  ),
  Tile(
    "base/Base_Game_C3_Tile_U.png",
    hasRoad = true
  ),
  Tile(
    "base/Base_Game_C3_Tile_U.png",
    hasRoad = true
  ),
  Tile(
    "base/Base_Game_C3_Tile_U.png",
    hasRoad = true
  ),
  Tile(
    "base/Abbot-Base_Game_C2_Tile_U_Garden.jpg",
    hasRoad = true,
    extra = Tile.Extra.Garden
  ),
  Tile(
    "base/Base_Game_C3_Tile_U.png",
    hasRoad = true,
    extra = Tile.Extra.Highwaymen
  ),
  Tile(
    "base/Base_Game_C3_Tile_U.png",
    hasRoad = true,
    extra = Tile.Extra.Highwaymen
  ),
  Tile(
    "base/Base_Game_C3_Tile_U.png",
    hasRoad = true,
    extra = Tile.Extra.Farmhouse
  ),
  Tile(
    "base/Base_Game_C3_Tile_U.png",
    hasRoad = true,
    extra = Tile.Extra.Pigsty
  ),
  Tile(
    "base/Base_Game_C3_Tile_V.png",
    hasRoad = true
  ),
  Tile(
    "base/Base_Game_C3_Tile_V.png",
    hasRoad = true
  ),
  Tile(
    "base/Base_Game_C3_Tile_V.png",
    hasRoad = true
  ),
  Tile(
    "base/Abbot-Base_Game_C2_Tile_V_Garden.jpg",
    hasRoad = true,
    extra = Tile.Extra.Garden
  ),
  Tile(
    "base/Base_Game_C3_Tile_V.png",
    hasRoad = true,
    extra = Tile.Extra.Highwaymen
  ),
  Tile(
    "base/Base_Game_C3_Tile_V.png",
    hasRoad = true,
    extra = Tile.Extra.Highwaymen
  ),
  Tile(
    "base/Base_Game_C3_Tile_V.png",
    hasRoad = true,
    extra = Tile.Extra.Farmhouse
  ),
  Tile(
    "base/Base_Game_C3_Tile_V.png",
    hasRoad = true,
    extra = Tile.Extra.Pigsty
  ),
  Tile(
    "base/Base_Game_C3_Tile_V.png",
    hasRoad = true,
    extra = Tile.Extra.Cowshed
  ),
  Tile(
    "base/Base_Game_C3_Tile_W.png",
    hasRoad = true
  ),
  Tile(
    "base/Base_Game_C3_Tile_W.png",
    hasRoad = true
  ),
  Tile(
    "base/Base_Game_C3_Tile_W.png",
    hasRoad = true
  ),
  Tile(
    "base/Base_Game_C3_Tile_W.png",
    hasRoad = true
  ),
  Tile(
    "base/Base_Game_C3_Tile_X.png",
    hasRoad = true
  )
)

private val riverTiles = listOf(
  Tile(
    "river/River_I_C3_Tile_A.png",
    hasRiver = true,
    hasRoad = true,
    extra = Tile.Extra.Source
  ),
  Tile(
    "river/River_I_C3_Tile_B.png",
    hasRiver = true,
    hasRoad = true,
    hasCastle = true
  ),
  Tile(
    "river/River_I_C3_Tile_C.png",
    hasRiver = true,
    hasCastle = true
  ),
  Tile(
    "river/River_I_C3_Tile_D.png",
    hasRiver = true,
    extra = Tile.Extra.DonkeyStable
  ),
  Tile(
    "river/River_I_C3_Tile_E.png",
    hasRiver = true,
    hasCastle = true,
    extra = Tile.Extra.WaterTower
  ),
  Tile(
    "river/River_I_C3_Tile_F.png",
    hasRiver = true
  ),
  Tile(
    "river/River_I_C3_Tile_G.png",
    hasRiver = true,
    extra = Tile.Extra.Farmhouse
  ),
  Tile(
    "river/River_I_C3_Tile_H.png",
    hasRiver = true,
    hasRoad = true,
    hasMonastery = true
  ),
  Tile(
    "river/River_I_C3_Tile_I.png",
    hasRiver = true,
    hasRoad = true,
    extra = Tile.Extra.Highwaymen
  ),
  Tile(
    "river/River_I_C3_Tile_J.png",
    hasRiver = true,
    extra = Tile.Extra.Garden
  ),
  Tile(
    "river/River_I_C3_Tile_K.png",
    hasRiver = true,
    hasRoad = true
  ),
  Tile(
    "river/River_I_C3_Tile_L.png",
    hasRiver = true,
    hasMonastery = true
  )
)

private val innsAndCathedralsTiles = listOf(
  Tile(
    "innsandcathedrals/Inns_And_Cathedrals_C3_Tile_A.png",
    hasInnRoad = true,
    extra = Tile.Extra.Highwaymen
  ),
  Tile(
    "innsandcathedrals/Inns_And_Cathedrals_C3_Tile_B.png",
    hasInnRoad = true,
    extra = Tile.Extra.Garden
  ),
  Tile(
    "innsandcathedrals/Inns_And_Cathedrals_C3_Tile_C.png",
    hasInnRoad = true,
    extra = Tile.Extra.Pigsty
  ),
  Tile(
    "innsandcathedrals/Inns_And_Cathedrals_C3_Tile_D.png",
    hasRoad = true,
    hasMonastery = true
  ),
  Tile(
    "innsandcathedrals/Inns_And_Cathedrals_C3_Tile_E.png",
    hasRoad = true,
    extra = Tile.Extra.Farmhouse
  ),
  Tile(
    "innsandcathedrals/Inns_And_Cathedrals_C3_Tile_F.png",
    hasRoad = true,
    hasCastle = true,
    extra = Tile.Extra.DonkeyStable
  ),
  Tile(
    "innsandcathedrals/Inns_And_Cathedrals_C3_Tile_G.png",
    hasCastle = true,
    extra = Tile.Extra.WaterTower
  ),
  Tile(
    "innsandcathedrals/Inns_And_Cathedrals_C3_Tile_H.png",
    hasCastle = true,
    extra = Tile.Extra.Garden
  ),
  Tile(
    "innsandcathedrals/Inns_And_Cathedrals_C3_Tile_I.png",
    hasRoad = true,
    hasCastle = true
  ),
  Tile(
    "innsandcathedrals/Inns_And_Cathedrals_C3_Tile_J.png",
    hasRoad = true,
    hasCastle = true,
    extra = Tile.Extra.Highwaymen
  ),
  Tile(
    "innsandcathedrals/Inns_And_Cathedrals_C3_Tile_Ka.png",
    hasCastle = true,
    extra = Tile.Extra.Cathedral
  ),
  Tile(
    "innsandcathedrals/Inns_And_Cathedrals_C3_Tile_Kb.png",
    hasCastle = true,
    extra = Tile.Extra.Cathedral
  ),
  Tile(
    "innsandcathedrals/Inns_And_Cathedrals_C3_Tile_L.png",
    hasInnRoad = true,
    hasCastle = true,
    hasShieldCastle = true
  ),
  Tile(
    "innsandcathedrals/Inns_And_Cathedrals_C3_Tile_M.png",
    hasInnRoad = true,
    hasCastle = true
  ),
  Tile(
    "innsandcathedrals/Inns_And_Cathedrals_C3_Tile_N.png",
    hasInnRoad = true,
    hasCastle = true
  ),
  Tile(
    "innsandcathedrals/Inns_And_Cathedrals_C3_Tile_O.png",
    hasCastle = true,
    extra = Tile.Extra.Farmhouse
  ),
  Tile(
    "innsandcathedrals/Inns_And_Cathedrals_C3_Tile_P.png",
    hasCastle = true,
    hasShieldCastle = true,
    extra = Tile.Extra.WaterTower
  ),
  Tile(
    "innsandcathedrals/Inns_And_Cathedrals_C3_Tile_Q.png",
    hasRoad = true,
    hasCastle = true,
    hasShieldCastle = true
  )
)

private val tradersAndBuildersTiles = listOf(
  Tile(
    "tradersandbuilders/Traders_And_Builders_C3_Tile_A.png",
    hasRoad = true,
    hasCastle = true,
    extra = Tile.Extra.Cowshed
  ),
  Tile(
    "tradersandbuilders/Traders_And_Builders_C3_Tile_B.png",
    hasRoad = true,
    extra = Tile.Extra.Highwaymen
  ),
  Tile(
    "tradersandbuilders/Traders_And_Builders_C3_Tile_C.png",
    hasRoad = true,
    hasCastle = true
  ),
  Tile(
    "tradersandbuilders/Traders_And_Builders_C3_Tile_D.png",
    hasRoad = true,
    hasCastle = true,
    hasWheatCastle = true
  ),
  Tile(
    "tradersandbuilders/Traders_And_Builders_C3_Tile_E.png",
    hasRoad = true,
    hasWheatCastle = true
  ),
  Tile(
    "tradersandbuilders/Traders_And_Builders_C3_Tile_F.png",
    hasRoad = true,
    hasWheatCastle = true,
    extra = Tile.Extra.Highwaymen
  ),
  Tile(
    "tradersandbuilders/Traders_And_Builders_C3_Tile_G.png",
    hasWheatCastle = true,
    extra = Tile.Extra.Pigsty
  ),
  Tile(
    "tradersandbuilders/Traders_And_Builders_C3_Tile_H.png",
    hasWheatCastle = true
  ),
  Tile(
    "tradersandbuilders/Traders_And_Builders_C3_Tile_I.png",
    hasRoad = true,
    hasWheatCastle = true,
    extra = Tile.Extra.WaterTower
  ),
  Tile(
    "tradersandbuilders/Traders_And_Builders_C3_Tile_J.png",
    hasMonastery = true
  ),
  Tile(
    "tradersandbuilders/Traders_And_Builders_C3_Tile_K.png",
    hasRoad = true,
    hasClothCastle = true,
    extra = Tile.Extra.Garden
  ),
  Tile(
    "tradersandbuilders/Traders_And_Builders_C3_Tile_L.png",
    hasRoad = true,
    hasCastle = true,
    hasClothCastle = true
  ),
  Tile(
    "tradersandbuilders/Traders_And_Builders_C3_Tile_M.png",
    hasCastle = true,
    hasClothCastle = true
  ),
  Tile(
    "tradersandbuilders/Traders_And_Builders_C3_Tile_N.png",
    hasCastle = true,
    hasClothCastle = true
  ),
  Tile(
    "tradersandbuilders/Traders_And_Builders_C3_Tile_O.png",
    hasRoad = true,
    hasClothCastle = true
  ),
  Tile(
    "tradersandbuilders/Traders_And_Builders_C3_Tile_P.png",
    hasCastle = true,
    hasWineCastle = true
  ),
  Tile(
    "tradersandbuilders/Traders_And_Builders_C3_Tile_Q.png",
    hasWineCastle = true
  ),
  Tile(
    "tradersandbuilders/Traders_And_Builders_C3_Tile_R.png",
    hasRoad = true,
    hasWineCastle = true
  ),
  Tile(
    "tradersandbuilders/Traders_And_Builders_C3_Tile_S.png",
    hasRoad = true,
    hasWineCastle = true
  ),
  Tile(
    "tradersandbuilders/Traders_And_Builders_C3_Tile_T.png",
    hasWineCastle = true,
    extra = Tile.Extra.Farmhouse
  ),
  Tile(
    "tradersandbuilders/Traders_And_Builders_C3_Tile_U.png",
    hasRoad = true,
    hasWineCastle = true,
    extra = Tile.Extra.Garden
  ),
  Tile(
    "tradersandbuilders/Traders_And_Builders_C3_Tile_V.png",
    hasRoad = true,
    hasWineCastle = true
  ),
  Tile(
    "tradersandbuilders/Traders_And_Builders_C3_Tile_W.png",
    hasRoad = true,
    hasWineCastle = true
  ),
  Tile(
    "tradersandbuilders/Traders_And_Builders_C3_Tile_X.png",
    hasCastle = true,
    hasWineCastle = true,
    extra = Tile.Extra.WaterTower
  )
)

private val princessAndDragonTiles = listOf(
  Tile(
    "princessanddragon/Princess_And_Dragon_C2_Tile_A.jpg",
    hasRoad = true,
    hasCastle = true,
    hasPrincess = true
  ),
  Tile(
    "princessanddragon/Princess_And_Dragon_C2_Tile_B.jpg",
    hasCastle = true,
    hasPrincess = true,
    extra = Tile.Extra.Garden
  ),
  Tile(
    "princessanddragon/Princess_And_Dragon_C2_Tile_C.jpg",
    hasCastle = true,
    hasPrincess = true,
    extra = Tile.Extra.WaterTower
  ),
  Tile(
    "princessanddragon/Princess_And_Dragon_C2_Tile_D.jpg",
    hasRoad = true,
    hasCastle = true,
    hasPrincess = true
  ),
  Tile(
    "princessanddragon/Princess_And_Dragon_C2_Tile_E.jpg",
    hasCastle = true,
    hasPrincess = true,
    extra = Tile.Extra.WaterTower
  ),
  Tile(
    "princessanddragon/Princess_And_Dragon_C2_Tile_F.jpg",
    hasCastle = true,
    hasShieldCastle = true,
    hasPrincess = true
  ),
  Tile(
    "princessanddragon/Princess_And_Dragon_C2_Tile_G.jpg",
    hasRoad = true,
    hasDragon = true,
    extra = Tile.Extra.Highwaymen
  ),
  Tile(
    "princessanddragon/Princess_And_Dragon_C2_Tile_G.jpg",
    hasRoad = true,
    hasDragon = true,
    extra = Tile.Extra.Highwaymen
  ),
  Tile(
    "princessanddragon/Princess_And_Dragon_C2_Tile_H.jpg",
    hasRoad = true,
    hasDragon = true
  ),
  Tile(
    "princessanddragon/Princess_And_Dragon_C2_Tile_I.jpg",
    hasRoad = true,
    hasDragon = true
  ),
  Tile(
    "princessanddragon/Princess_And_Dragon_C2_Tile_J.jpg",
    hasCastle = true,
    hasDragon = true,
    extra = Tile.Extra.Garden
  ),
  Tile(
    "princessanddragon/Princess_And_Dragon_C2_Tile_K.jpg",
    hasCastle = true,
    hasDragon = true,
    extra = Tile.Extra.DonkeyStable
  ),
  Tile(
    "princessanddragon/Princess_And_Dragon_C2_Tile_L.jpg",
    hasRoad = true,
    hasCastle = true,
    hasDragon = true
  ),
  Tile(
    "princessanddragon/Princess_And_Dragon_C2_Tile_M.jpg",
    hasRoad = true,
    hasCastle = true,
    hasDragon = true
  ),
  Tile(
    "princessanddragon/Princess_And_Dragon_C2_Tile_N.jpg",
    hasCastle = true,
    hasShieldCastle = true,
    hasDragon = true
  ),
  Tile(
    "princessanddragon/Princess_And_Dragon_C2_Tile_O.jpg",
    hasCastle = true,
    hasMonastery = true,
    hasDragon = true
  ),
  Tile(
    "princessanddragon/Princess_And_Dragon_C2_Tile_P.jpg",
    hasRoad = true,
    hasMonastery = true,
    hasDragon = true
  ),
  Tile(
    "princessanddragon/Princess_And_Dragon_C2_Tile_Q.jpg",
    hasRoad = true,
    hasCastle = true,
    hasDragon = true
  ),
  Tile(
    "princessanddragon/Princess_And_Dragon_C2_Tile_R.jpg",
    hasRoad = true,
    hasMagicPortal = true,
    extra = Tile.Extra.Farmhouse
  ),
  Tile(
    "princessanddragon/Princess_And_Dragon_C2_Tile_S.jpg",
    hasCastle = true,
    hasMagicPortal = true
  ),
  Tile(
    "princessanddragon/Princess_And_Dragon_C2_Tile_T.jpg",
    hasRoad = true,
    hasCastle = true,
    hasMagicPortal = true
  ),
  Tile(
    "princessanddragon/Princess_And_Dragon_C2_Tile_U.jpg",
    hasRoad = true,
    hasCastle = true,
    hasMagicPortal = true
  ),
  Tile(
    "princessanddragon/Princess_And_Dragon_C2_Tile_V.jpg",
    hasRoad = true,
    hasCastle = true,
    hasMagicPortal = true
  ),
  Tile(
    "princessanddragon/Princess_And_Dragon_C2_Tile_W.jpg",
    hasRoad = true,
    hasMagicPortal = true,
    extra = Tile.Extra.Highwaymen
  ),
  Tile(
    "princessanddragon/Princess_And_Dragon_C2_Tile_X.jpg",
    hasRoad = true,
    hasVolcano = true
  ),
  Tile(
    "princessanddragon/Princess_And_Dragon_C2_Tile_Y.jpg",
    hasCastle = true,
    hasVolcano = true
  ),
  Tile(
    "princessanddragon/Princess_And_Dragon_C2_Tile_Z.jpg",
    hasCastle = true,
    hasVolcano = true
  ),
  Tile(
    "princessanddragon/Princess_And_Dragon_C2_Tile_1.jpg",
    hasRoad = true,
    hasVolcano = true,
    extra = Tile.Extra.Farmhouse
  ),
  Tile(
    "princessanddragon/Princess_And_Dragon_C2_Tile_2.jpg",
    hasVolcano = true,
    extra = Tile.Extra.Pigsty
  ),
  Tile(
    "princessanddragon/Princess_And_Dragon_C2_Tile_3.jpg",
    hasRoad = true,
    hasVolcano = true
  )
)

private val dragonAndFairyTiles = listOf(
  Tile(
    "dragonandfairy/Dragon_And_Fairy_C31_Tile_A.jpg",
    hasRiver = true,
    hasCastle = true,
    hasMonastery = true,
    extra = Tile.Extra.Source
  ),
  Tile(
    "dragonandfairy/Dragon_And_Fairy_C31_Tile_B_Front.jpg",
    hasRiver = true,
    extra = Tile.Extra.Garden
  ),
  // We are not going to use the dragonandfairy/Dragon_And_Fairy_C31_Tile_B_Back.jpg tile for now.
  Tile(
    "dragonandfairy/Dragon_And_Fairy_C31_Tile_C.jpg",
    hasRoad = true,
    hasCastle = true,
    hasDragon = true
  ),
  Tile(
    "dragonandfairy/Dragon_And_Fairy_C31_Tile_D.jpg",
    hasCastle = true,
    hasDragon = true,
    extra = Tile.Extra.WaterTower
  ),
  Tile(
    "dragonandfairy/Dragon_And_Fairy_C31_Tile_E.jpg",
    hasCastle = true,
    extra = Tile.Extra.WaterTower
  ),
  Tile(
    "dragonandfairy/Dragon_And_Fairy_C31_Tile_F.jpg",
    hasCastle = true,
    hasShieldCastle = true
  ),
  Tile(
    "dragonandfairy/Dragon_And_Fairy_C31_Tile_G.jpg",
    hasRoad = true,
    hasDragon = true,
    extra = Tile.Extra.Highwaymen
  ),
  Tile(
    "dragonandfairy/Dragon_And_Fairy_C31_Tile_H.jpg",
    hasRoad = true,
    hasDragon = true
  ),
  Tile(
    "dragonandfairy/Dragon_And_Fairy_C31_Tile_I.jpg",
    hasCastle = true,
    hasDragon = true,
    extra = Tile.Extra.Garden
  ),
  Tile(
    "dragonandfairy/Dragon_And_Fairy_C31_Tile_J.jpg",
    hasCastle = true,
    hasDragon = true,
    extra = Tile.Extra.DonkeyStable
  ),
  Tile(
    "dragonandfairy/Dragon_And_Fairy_C31_Tile_K.jpg",
    hasRoad = true,
    hasCastle = true,
    hasDragon = true
  ),
  Tile(
    "dragonandfairy/Dragon_And_Fairy_C31_Tile_L.jpg",
    hasRoad = true,
    hasCastle = true,
    hasDragon = true
  ),
  Tile(
    "dragonandfairy/Dragon_And_Fairy_C31_Tile_M.jpg",
    hasCastle = true,
    hasShieldCastle = true,
    hasDragon = true
  ),
  Tile(
    "dragonandfairy/Dragon_And_Fairy_C31_Tile_N.jpg",
    hasCastle = true,
    hasMonastery = true
  ),
  Tile(
    "dragonandfairy/Dragon_And_Fairy_C31_Tile_O.jpg",
    hasRoad = true,
    hasCastle = true,
    hasDragon = true
  ),
  Tile(
    "dragonandfairy/Dragon_And_Fairy_C31_Tile_P.jpg",
    hasRoad = true,
    hasTunnelRoad = true,
    hasCastle = true,
  ),
  Tile(
    "dragonandfairy/Dragon_And_Fairy_C31_Tile_Q.jpg",
    hasRoad = true,
    hasMagicPortal = true,
    extra = Tile.Extra.Farmhouse
  ),
  Tile(
    "dragonandfairy/Dragon_And_Fairy_C31_Tile_R.jpg",
    hasCastle = true,
    hasMagicPortal = true
  ),
  Tile(
    "dragonandfairy/Dragon_And_Fairy_C31_Tile_S.jpg",
    hasRoad = true,
    hasCastle = true,
    hasMagicPortal = true
  ),
  Tile(
    "dragonandfairy/Dragon_And_Fairy_C31_Tile_T.jpg",
    hasRoad = true,
    hasCastle = true,
    hasMagicPortal = true
  ),
  Tile(
    "dragonandfairy/Dragon_And_Fairy_C31_Tile_U.jpg",
    hasRoad = true,
    hasCastle = true,
    hasMagicPortal = true
  ),
  Tile(
    "dragonandfairy/Dragon_And_Fairy_C31_Tile_V.jpg",
    hasRoad = true,
    hasMagicPortal = true,
    extra = Tile.Extra.Highwaymen
  ),
  Tile(
    "dragonandfairy/Dragon_And_Fairy_C31_Tile_W.jpg",
    hasRoad = true,
    hasDragonHoard = true
  ),
  Tile(
    "dragonandfairy/Dragon_And_Fairy_C31_Tile_X.jpg",
    hasCastle = true,
    hasDragonHoard = true
  ),
  Tile(
    "dragonandfairy/Dragon_And_Fairy_C31_Tile_Y.jpg",
    hasCastle = true,
    hasDragonHoard = true
  ),
  Tile(
    "dragonandfairy/Dragon_And_Fairy_C31_Tile_Z.jpg",
    hasRoad = true,
    hasDragonHoard = true,
    extra = Tile.Extra.Farmhouse
  ),
  Tile(
    "dragonandfairy/Dragon_And_Fairy_C31_Tile_1.jpg",
    hasDragonHoard = true,
    extra = Tile.Extra.Pigsty
  ),
  Tile(
    "dragonandfairy/Dragon_And_Fairy_C31_Tile_2.jpg",
    hasRoad = true,
    hasDragonHoard = true
  )
)

private val flyingMachinesTiles = listOf(
  Tile(
    "flyingmachines/Flier_C3_Tile_A.png",
    hasRoad = true
  ),
  Tile(
    "flyingmachines/Flier_C3_Tile_B.png",
    hasRoad = true
  ),
  Tile(
    "flyingmachines/Flier_C3_Tile_C.png",
    hasRoad = true,
    extra = Tile.Extra.Pigsty
  ),
  Tile(
    "flyingmachines/Flier_C3_Tile_D.png",
    hasRoad = true
  ),
  Tile(
    "flyingmachines/Flier_C3_Tile_E.png",
    hasRoad = true
  ),
  Tile(
    "flyingmachines/Flier_C3_Tile_F.png",
    hasRoad = true,
    extra = Tile.Extra.Highwaymen
  ),
  Tile(
    "flyingmachines/Flier_C3_Tile_G.png",
    extra = Tile.Extra.Farmhouse
  ),
  Tile(
    "flyingmachines/Flier_C3_Tile_H.png",
    extra = Tile.Extra.Garden
  )
)

private val messengersTiles = listOf(
  Tile(
    "messengers/Messages_C3_Tile_A.png"
  ),
  Tile(
    "messengers/Messages_C3_Tile_B.png"
  ),
  Tile(
    "messengers/Messages_C3_Tile_C.png"
  ),
  Tile(
    "messengers/Messages_C3_Tile_D.png"
  ),
  Tile(
    "messengers/Messages_C3_Tile_E.png"
  ),
  Tile(
    "messengers/Messages_C3_Tile_F.png"
  ),
  Tile(
    "messengers/Messages_C3_Tile_G.png"
  ),
  Tile(
    "messengers/Messages_C3_Tile_H.png"
  )
)

private val ferriesTiles = listOf(
  Tile(
    "ferries/Ferries_C3_Tile_01.png",
    hasRoad = true,
    hasCastle = true
  ),
  Tile(
    "ferries/Ferries_C3_Tile_01.png",
    hasRoad = true,
    hasCastle = true
  ),
  Tile(
    "ferries/Ferries_C3_Tile_01.png",
    hasRoad = true,
    hasCastle = true
  ),
  Tile(
    "ferries/Ferries_C3_Tile_02.png",
    hasRoad = true
  ),
  Tile(
    "ferries/Ferries_C3_Tile_02.png",
    hasRoad = true
  ),
  Tile(
    "ferries/Ferries_C3_Tile_02.png",
    hasRoad = true
  ),
  Tile(
    "ferries/Ferries_C3_Tile_03.png",
    hasRoad = true
  ),
  Tile(
    "ferries/Ferries_C3_Tile_03.png",
    hasRoad = true
  )
)

private val goldminesTiles = listOf(
  Tile(
    "goldmines/Goldmines_C3_Tile_A.png",
    hasRoad = true,
    extra = Tile.Extra.Cowshed
  ),
  Tile(
    "goldmines/Goldmines_C3_Tile_B.png",
    hasRoad = true,
    hasCastle = true,
    extra = Tile.Extra.WaterTower
  ),
  Tile(
    "goldmines/Goldmines_C3_Tile_C.png",
    hasRoad = true,
    hasCastle = true,
    hasMonastery = true
  ),
  Tile(
    "goldmines/Goldmines_C3_Tile_D.png",
    hasRoad = true,
    hasCastle = true
  ),
  Tile(
    "goldmines/Goldmines_C3_Tile_E.png",
    hasRoad = true,
    hasMonastery = true
  ),
  Tile(
    "goldmines/Goldmines_C3_Tile_F.png",
    hasRoad = true,
    extra = Tile.Extra.Highwaymen
  ),
  Tile(
    "goldmines/Goldmines_C3_Tile_G.png",
    hasCastle = true,
    hasMonastery = true
  ),
  Tile(
    "goldmines/Goldmines_C3_Tile_H.png",
    hasRoad = true,
    hasMonastery = true
  )
)

private val mageAndWitchTiles = listOf(
  Tile(
    "mageandwitch/Mage_And_Witch_C3_Tile_A.png",
    hasRoad = true,
    hasCastle = true
  ),
  Tile(
    "mageandwitch/Mage_And_Witch_C3_Tile_B.png",
    hasRoad = true,
    hasCastle = true
  ),
  Tile(
    "mageandwitch/Mage_And_Witch_C3_Tile_C.png",
    hasRoad = true,
    hasCastle = true,
    extra = Tile.Extra.WaterTower
  ),
  Tile(
    "mageandwitch/Mage_And_Witch_C3_Tile_D.png",
    hasCastle = true
  ),
  Tile(
    "mageandwitch/Mage_And_Witch_C3_Tile_E.png",
    hasRoad = true,
    hasCastle = true,
    extra = Tile.Extra.Garden
  ),
  Tile(
    "mageandwitch/Mage_And_Witch_C3_Tile_F.png",
    hasRoad = true,
    hasCastle = true
  ),
  Tile(
    "mageandwitch/Mage_And_Witch_C3_Tile_G.png",
    hasRoad = true,
    hasCastle = true
  ),
  Tile(
    "mageandwitch/Mage_And_Witch_C3_Tile_H.png",
    hasRoad = true,
    hasCastle = true,
    extra = Tile.Extra.Farmhouse
  )
)

private val robbersTiles = listOf(
  Tile(
    "robbers/Robbers_C3_Tile_A.png",
    hasRoad = true
  ),
  Tile(
    "robbers/Robbers_C3_Tile_A.png",
    hasRoad = true,
    extra = Tile.Extra.Highwaymen
  ),
  Tile(
    "robbers/Robbers_C3_Tile_C.png",
    hasRoad = true
  ),
  Tile(
    "robbers/Robbers_C3_Tile_D.png",
    hasCastle = true,
    extra = Tile.Extra.Garden
  ),
  Tile(
    "robbers/Robbers_C3_Tile_E.png",
    hasRoad = true,
    hasCastle = true
  ),
  Tile(
    "robbers/Robbers_C3_Tile_F.png",
    hasRoad = true
  ),
  Tile(
    "robbers/Robbers_C3_Tile_F.png",
    hasRoad = true
  ),
  Tile(
    "robbers/Robbers_C3_Tile_H.png",
    hasCastle = true,
    extra = Tile.Extra.WaterTower
  )
)

private val cropCircles = listOf(
  Tile(
    "cropcircles/Crop_Circles_C3_Tile_A.png",
    hasRoad = true
  ),
  Tile(
    "cropcircles/Crop_Circles_C3_Tile_B.png",
    hasRoad = true
  ),
  Tile(
    "cropcircles/Crop_Circles_C3_Tile_C.png",
    hasRoad = true,
    hasCastle = true,
    extra = Tile.Extra.Farmhouse
  ),
  Tile(
    "cropcircles/Crop_Circles_C3_Tile_D.png",
    hasRoad = true,
    hasCastle = true
  ),
  Tile(
    "cropcircles/Crop_Circles_C3_Tile_E.png",
    hasCastle = true
  ),
  Tile(
    "cropcircles/Crop_Circles_C3_Tile_F.png",
    hasCastle = true
  )
)
