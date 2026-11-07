package com.nightlynexus

internal data class Tile(
  val path: String,
  val hasRiver: Boolean = false,
  val hasRoad: Boolean = false,
  val hasTunnelRoad: Boolean = false,
  val hasInnRoad: Boolean = false,
  val hasCastle: Boolean = false,
  val hasShieldCastle: Boolean = false,
  val hasWineCastle: Boolean = false,
  val hasWheatCastle: Boolean = false,
  val hasClothCastle: Boolean = false,
  val hasMonastery: Boolean = false,
  val hasPrincess: Boolean = false,
  val hasDragon: Boolean = false,
  val hasDragonHoard: Boolean = false,
  val hasMagicPortal: Boolean = false,
  val hasVolcano: Boolean = false,
  val extra: Extra = Extra.None
) {
  init {
    if (hasTunnelRoad) {
      require(hasRoad) {
        "A tile with a tunnel must also have a road."
      }
    }
    if (hasShieldCastle) {
      require(hasCastle) {
        "A tile with a shield must also have a castle."
      }
    }
  }

  enum class Extra(val text: String) {
    None(""),
    Source("Source"),
    Garden("Garden"),
    Farmhouse("Farmhouse"),
    Cowshed("Cowshed"),
    WaterTower("Water Tower"),
    Highwaymen("Highwaymen"),
    Pigsty("Pigsty"),
    DonkeyStable("Donkey Stable"),
    Cathedral("Cathedral");
  }
}
