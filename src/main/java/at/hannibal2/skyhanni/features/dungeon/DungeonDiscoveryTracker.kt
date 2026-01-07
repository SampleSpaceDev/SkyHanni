package at.hannibal2.skyhanni.features.dungeon

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.model.TabWidget
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.WidgetUpdateEvent
import at.hannibal2.skyhanni.events.dungeon.DungeonStartEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

@SkyHanniModule
object DungeonDiscoveryTracker {
    private val config get() = SkyHanniMod.feature.dungeon

    private val secretsFoundMatch = "^ Secrets Found: §r§e(\\d+(?:\\.\\d+)?)%".toPattern()
    private val secretsMatch = "^ Secrets Found: §r§b(\\d+)$".toPattern()
    private val cryptsMatch = "^ Crypts: §r§6(\\d+)$".toPattern()

    private var secrets = 0
    private var maxSecrets = 0
    private var requiredSecrets = 0
    private var crypts = 0
    private var floor: String? = null

    private val floorRequirements: Map<String, Double> = mapOf(
        "E" to 0.3,
        "F1" to 0.3,
        "F2" to 0.4,
        "F3" to 0.5,
        "F4" to 0.6,
        "F5" to 0.7,
        "F6" to 0.85,
    )

    @HandleEvent(onlyOnIsland = IslandType.CATACOMBS)
    fun onDungeonStart(event: DungeonStartEvent) {
        secrets = 0
        maxSecrets = 0
        crypts = 0

        floor = event.dungeonFloor
    }

    @HandleEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return

        val secretColor = if (secrets >= requiredSecrets) "§a"
        else "§c"

        val cryptColor = if (crypts >= 5) "§a"
        else "§c"

        config.showDiscoveriesDisplayPos.renderRenderables(
            listOf(
                Renderable.text(
                    if (secrets > 0) "§eSecrets: $secretColor$secrets§7/§a$requiredSecrets §7(Total: §6$maxSecrets§7)"
                    else ""
                ),
                Renderable.text(
                    if (crypts > 0) "§eCrypts: $cryptColor$crypts"
                    else ""
                ),
            ),
            posLabel = "Dungeon Discoveries"
        )
    }

    @HandleEvent(onlyOnIsland = IslandType.CATACOMBS)
    fun onDungeonPlayerStatsWidgetUpdate(event: WidgetUpdateEvent) {
        if (!event.isWidget(TabWidget.DUNGEON_PLAYER_STATS)) return

        val widget: TabWidget = event.widget
        val lines = widget.lines

        lines.forEach { line ->
            val secretsMatcher = secretsMatch.matcher(line)
            val cryptsMatcher = cryptsMatch.matcher(line)

            secretsMatcher.group(1)?.toIntOrNull()?.let { secrets = it }

            cryptsMatcher.group(1)?.toIntOrNull()?.let { crypts = it }
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.CATACOMBS)
    fun onDungeonStatsWidgetUpdate(event: WidgetUpdateEvent) {
        if (!event.isWidget(TabWidget.DUNGEON_STATS)) return

        val widget: TabWidget = event.widget
        val lines = widget.lines

        lines.forEach { line ->
            val find = secretsFoundMatch.matcher(line)

            if (find != null) {
                val currentPercentage = find.group(1).toDouble().div(100.0)

                if (currentPercentage.isNaN() || secrets == 0) return@forEach

                maxSecrets = floor(secrets.div(currentPercentage)).toInt()

                requiredSecrets = (maxSecrets * (floorRequirements[floor] ?: 1.0)).roundToInt()
                println(requiredSecrets)
            }
        }
    }

    private fun isEnabled(): Boolean = DungeonApi.inDungeon() && config.showDiscoveriesDisplay
}