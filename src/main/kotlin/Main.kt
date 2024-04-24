package com.nightlynexus

import kotlinx.browser.document
import kotlinx.dom.appendElement
import kotlinx.dom.appendText
import kotlinx.dom.createElement
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.get

fun main() {
  val grid = document.getElementsByClassName("grid-container")[0] as HTMLDivElement
  val unusedCountDisplay = document.getElementById("unused_count")!!
  val usedCountDisplay = document.getElementById("used_count")!!
  val sortUsedCheckbox = document.getElementById("sort_used")!!
  val riverCheckbox = document.getElementById("river")!!
  val innsAndCathedralsCheckbox = document.getElementById("inns_and_cathedrals")!!
  val tradersAndBuildersCheckbox = document.getElementById("traders_and_builders")!!
  val flyingMachinesCheckbox = document.getElementById("flying_machines")!!
  val messengersCheckbox = document.getElementById("messengers")!!

  val totalTileCount = baseTiles.size +
    riverTiles.size +
    innsAndCathedralsTiles.size +
    tradersAndBuildersTiles.size +
    flyingMachinesTiles.size +
    messengersTiles.size
  val allTileElements = ArrayList<TileElement>(totalTileCount)

  // We show every tile initially, including the 2 source tiles.
  // We hide the base source tile and update the counts when adding all the tiles below.
  var shownUnusedCount = totalTileCount - 2
  var shownUsedCount = 2
  unusedCountDisplay.textContent = shownUnusedCount.toString()
  usedCountDisplay.textContent = shownUsedCount.toString()

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
  })
  val tileElementListener = object : TileElement.Listener {
    override fun onShowChanged(tileElement: TileElement, show: Boolean) {
      val used = tileElement.used
      if (show) {
        if (used) {
          shownUsedCount++
        } else {
          shownUnusedCount++
        }
      } else {
        if (used) {
          shownUsedCount--
        } else {
          shownUnusedCount--
        }
      }
      unusedCountDisplay.textContent = shownUnusedCount.toString()
      usedCountDisplay.textContent = shownUsedCount.toString()
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
          shownUnusedCount--
          shownUsedCount++
        } else {
          shownUnusedCount++
          shownUsedCount--
        }
        unusedCountDisplay.textContent = shownUnusedCount.toString()
        usedCountDisplay.textContent = shownUsedCount.toString()
      }
    }
  }

  var baseSourceTileElement: TileElement? = null
  for (tile in baseTiles) {
    val baseTileElement = grid.addTile(tile, allTileElements.size, tileElementListener).apply {
      if (tile.extra == Tile.Extra.Source) {
        check(baseSourceTileElement == null)
        baseSourceTileElement = this
        show = false
      }
    }
    allTileElements += baseTileElement
  }
  checkNotNull(baseSourceTileElement)

  val riverTileElements = ArrayList<TileElement>(riverTiles.size)
  for (tile in riverTiles) {
    val riverTileElement = grid.addTile(tile, allTileElements.size, tileElementListener)
    riverTileElements += riverTileElement
    allTileElements += riverTileElement
  }

  riverCheckbox.addEventListener("change", {
    val checked = (it.target as HTMLInputElement).checked
    baseSourceTileElement!!.show = !checked
    for (tileElement in riverTileElements) {
      tileElement.show = checked
    }
  })

  addTiles(
    innsAndCathedralsTiles,
    innsAndCathedralsCheckbox,
    grid,
    allTileElements,
    tileElementListener
  )

  addTiles(
    tradersAndBuildersTiles,
    tradersAndBuildersCheckbox,
    grid,
    allTileElements,
    tileElementListener
  )

  addTiles(
    flyingMachinesTiles,
    flyingMachinesCheckbox,
    grid,
    allTileElements,
    tileElementListener
  )

  addTiles(
    messengersTiles,
    messengersCheckbox,
    grid,
    allTileElements,
    tileElementListener
  )
}

private fun addTiles(
  tiles: List<Tile>,
  checkboxElement: Element,
  grid: HTMLDivElement,
  allTileElements: MutableList<TileElement>,
  tileElementListener: TileElement.Listener
) {
  val tileElements = ArrayList<TileElement>(tiles.size)
  for (tile in tiles) {
    val messengersTileElement = grid.addTile(tile, allTileElements.size, tileElementListener)
    tileElements += messengersTileElement
    allTileElements += messengersTileElement
  }

  checkboxElement.addEventListener("change", {
    val checked = (it.target as HTMLInputElement).checked
    for (tileElement in tileElements) {
      tileElement.show = checked
    }
  })
}

private fun HTMLDivElement.addTile(
  tile: Tile,
  ordinal: Int,
  listener: TileElement.Listener
): TileElement {
  return TileElement(tile, ownerDocument!!, ordinal, listener).also { appendChild(it.element) }
}

private class TileElement(
  val tile: Tile,
  ownerDocument: Document,
  val ordinal: Int,
  private val listener: Listener
) {
  interface Listener {
    fun onShowChanged(tileElement: TileElement, show: Boolean)
    fun onUsedChanged(tileElement: TileElement, used: Boolean)
  }

  var show = true
    set(value) {
      field = value
      val display = if (value) "initial" else "none"
      element.setAttribute("style", "display: $display;")
      listener.onShowChanged(this, value)
    }
  var used = tile.extra == Tile.Extra.Source
    private set

  val element = ownerDocument.createElement("figure") {
    appendElement("img") {
      setAttribute("src", tile.path)
      if (tile.extra == Tile.Extra.Source) {
        setAttribute("style", "filter: grayscale(100%);")
      } else {
        addEventListener("click", {
          val grayscale = if (used) {
            used = false
            "0%"
          } else {
            used = true
            "100%"
          }
          setAttribute("style", "filter: grayscale($grayscale);")
          listener.onUsedChanged(this@TileElement, used)
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
