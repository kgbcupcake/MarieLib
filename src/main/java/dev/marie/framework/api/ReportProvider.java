package dev.marie.framework.api;

import dev.marie.framework.api.ApiStatus;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.List;

/**
 * Extension point allowing other mods to inject custom sections into the
 * {@code /marie} command's tracking report output.
 *
 * <p>Implementations are registered via {@link MarieAPI#registerReportProvider(ReportProvider)}
 * and will be invoked in registration order when the report is generated.</p>
 */
@ApiStatus.Experimental
public interface ReportProvider {

    /**
     * Returns a unique identifier for this report section, used for ordering
     * and deduplication.
     *
     * @return a non-null, unique string identifier (e.g. "mymod:report_id")
     */
    String getSectionId();

    /**
     * Returns the display title for this report section, shown as a header
     * in the command output.
     *
     * @return a translatable or literal {@link Component} representing the section header
     */
    Component getSectionTitle();

    /**
     * Generates the report lines for the given player. Each component in the
     * returned list represents one line of output in the report.
     *
     * @param player the player whose tracking report is being generated
     * @return an ordered list of {@link Component} lines to display; may be empty but not null
     */
    List<Component> generateReport(Player player);
}
