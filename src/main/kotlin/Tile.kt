package com.nightlynexus

internal data class Tile(
  val path: String,
  val hasRoad: Boolean = false,
  val hasInnRoad: Boolean = false,
  val hasCastle: Boolean = false,
  val hasShieldCastle: Boolean = false,
  val hasWineCastle: Boolean = false,
  val hasWheatCastle: Boolean = false,
  val hasClothCastle: Boolean = false,
  val hasMonastery: Boolean = false,
  val extra: Extra = Extra.None
) {
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
