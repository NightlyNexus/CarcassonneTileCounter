package com.nightlynexus

import kotlinx.browser.document
import kotlinx.browser.localStorage
import kotlinx.browser.window
import kotlinx.dom.appendElement
import kotlinx.dom.appendText
import kotlinx.dom.createElement
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.get

fun main() {
  val tileStorage = TileStorage(localStorage)

  val totalTileCount = baseTiles.size +
    riverTiles.size +
    innsAndCathedralsTiles.size +
    tradersAndBuildersTiles.size +
    flyingMachinesTiles.size +
    messengersTiles.size
  val allTileElements = ArrayList<TileElement>(totalTileCount)

  val grid = document.getElementsByClassName("grid-container")[0] as HTMLDivElement
  val inPileCountDisplay = document.getElementById("in_pile_count")!!
  val usedCountDisplay = document.getElementById("used_count")!!
  val sortUsedCheckbox = document.getElementById("sort_used") as HTMLInputElement
  document.getElementById("reset")!!.apply {
    addEventListener("click", {
      if (window.confirm("Reset all tiles?")) {
        tileStorage.resetUsed()
        for (tileElement in allTileElements) {
          if (tileElement.tile.extra !== Tile.Extra.Source) {
            if (tileElement.used) {
              tileElement.used = false
            }
          }
        }
      }
    })
  }

  val shownTilesCount = ShownTilesCount(0, 0)

  val defaultComparator = Comparator<TileElement> { a, b ->
    a.ordinal - b.ordinal
  }
  val sortUnusedComparator = Comparator<TileElement> { a, b ->
    if (a.used && !b.used) {
      1
    } else if (!a.used && b.used) {
      -1
    } else {
      a.ordinal - b.ordinal
    }
  }

  var sortUnused = false
  sortUsedCheckbox.addEventListener("change", {
    sortUnused = (it.target as HTMLInputElement).checked
    allTileElements.sortWith(if (sortUnused) sortUnusedComparator else defaultComparator)
    grid.innerHTML = ""
    for (tileElement in allTileElements) {
      grid.appendChild(tileElement.element)
    }

    tileStorage.setSortUsed(sortUnused)
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
      if (sortUnused) {
        allTileElements.sortWith(sortUnusedComparator)
        grid.innerHTML = ""
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

      tileStorage.setUsed(tileElement.ordinal, used)
    }
  }

  val riverCheckboxElementId = "river"
  val showRiver = tileStorage.getShownStart(riverCheckboxElementId)

  var baseSourceTileElement: TileElement? = null
  for (tile in baseTiles) {
    val ordinal = allTileElements.size
    val isSource = tile.extra === Tile.Extra.Source
    val show = !isSource || !showRiver
    val used = isSource || tileStorage.getUsedStart(ordinal)
    val baseTileElement = grid.addTile(
      tile,
      ordinal,
      show,
      used,
      shownTilesCount,
      tileElementListener
    )
    if (tile.extra == Tile.Extra.Source) {
      check(baseSourceTileElement == null)
      baseSourceTileElement = baseTileElement
    }
    allTileElements += baseTileElement
  }
  checkNotNull(baseSourceTileElement)

  val riverTileElements = ArrayList<TileElement>(riverTiles.size)
  for (tile in riverTiles) {
    val ordinal = allTileElements.size
    val isSource = tile.extra === Tile.Extra.Source
    val used = isSource || tileStorage.getUsedStart(ordinal)
    val riverTileElement = grid.addTile(
      tile,
      ordinal,
      showRiver,
      used,
      shownTilesCount,
      tileElementListener
    )
    riverTileElements += riverTileElement
    allTileElements += riverTileElement
  }

  val riverCheckbox = document.getElementById(riverCheckboxElementId) as HTMLInputElement
  riverCheckbox.addEventListener("change", {
    val checked = (it.target as HTMLInputElement).checked
    baseSourceTileElement.show = !checked
    for (tileElement in riverTileElements) {
      tileElement.show = checked
    }
  })

  addTiles(
    innsAndCathedralsTiles,
    "inns_and_cathedrals",
    grid,
    allTileElements,
    shownTilesCount,
    tileElementListener,
    tileStorage
  )

  addTiles(
    tradersAndBuildersTiles,
    "traders_and_builders",
    grid,
    allTileElements,
    shownTilesCount,
    tileElementListener,
    tileStorage
  )

  addTiles(
    flyingMachinesTiles,
    "flying_machines",
    grid,
    allTileElements,
    shownTilesCount,
    tileElementListener,
    tileStorage
  )

  addTiles(
    messengersTiles,
    "messengers",
    grid,
    allTileElements,
    shownTilesCount,
    tileElementListener,
    tileStorage
  )

  inPileCountDisplay.textContent = shownTilesCount.inPile.toString()
  usedCountDisplay.textContent = shownTilesCount.used.toString()

  sortUsedCheckbox.checked = tileStorage.getSortUsedStart()
}

private class ShownTilesCount(var inPile: Int, var used: Int)

private fun addTiles(
  tiles: List<Tile>,
  checkboxElementId: String,
  grid: HTMLDivElement,
  allTileElements: MutableList<TileElement>,
  shownTilesCount: ShownTilesCount,
  tileElementListener: TileElement.Listener,
  tileStorage: TileStorage
) {
  val show = tileStorage.getShownStart(checkboxElementId)
  val tileElements = ArrayList<TileElement>(tiles.size)
  for (tile in tiles) {
    val ordinal = allTileElements.size
    val used = tileStorage.getUsedStart(ordinal)
    val messengersTileElement = grid.addTile(
      tile,
      ordinal,
      show,
      used,
      shownTilesCount,
      tileElementListener
    )
    tileElements += messengersTileElement
    allTileElements += messengersTileElement
  }
  val checkboxElement = document.getElementById(checkboxElementId) as HTMLInputElement
  checkboxElement.checked = show
  checkboxElement.addEventListener("change", {
    val checked = (it.target as HTMLInputElement).checked
    for (tileElement in tileElements) {
      tileElement.show = checked
    }

    tileStorage.setShown(checkboxElementId, checked)
  })
}

private fun HTMLDivElement.addTile(
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
  ).also { appendChild(it.element) }
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
      if (tile.extra == Tile.Extra.Source) {
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
    hasShieldCastle = true
  ),
  Tile(
    "base/Base_Game_C3_Tile_F.png",
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
    hasShieldCastle = true
  ),
  Tile(
    "base/Abbot-Base_Game_C2_Tile_M_Garden.jpg",
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
    hasShieldCastle = true,
    extra = Tile.Extra.Farmhouse
  ),
  Tile(
    "base/Base_Game_C3_Tile_O.png",
    hasRoad = true,
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
    hasShieldCastle = true
  ),
  Tile(
    "base/Base_Game_C3_Tile_S.png",
    hasRoad = true,
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
    hasRoad = true,
    extra = Tile.Extra.Source
  ),
  Tile(
    "river/River_I_C3_Tile_B.png",
    hasRoad = true,
    hasCastle = true
  ),
  Tile(
    "river/River_I_C3_Tile_C.png",
    hasCastle = true
  ),
  Tile(
    "river/River_I_C3_Tile_D.png",
    extra = Tile.Extra.DonkeyStable
  ),
  Tile(
    "river/River_I_C3_Tile_E.png",
    hasCastle = true,
    extra = Tile.Extra.WaterTower
  ),
  Tile(
    "river/River_I_C3_Tile_F.png"
  ),
  Tile(
    "river/River_I_C3_Tile_G.png",
    extra = Tile.Extra.Farmhouse
  ),
  Tile(
    "river/River_I_C3_Tile_H.png",
    hasRoad = true,
    hasMonastery = true
  ),
  Tile(
    "river/River_I_C3_Tile_I.png",
    hasRoad = true,
    extra = Tile.Extra.Highwaymen
  ),
  Tile(
    "river/River_I_C3_Tile_J.png",
    extra = Tile.Extra.Garden
  ),
  Tile(
    "river/River_I_C3_Tile_K.png",
    hasRoad = true
  ),
  Tile(
    "river/River_I_C3_Tile_L.png",
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
