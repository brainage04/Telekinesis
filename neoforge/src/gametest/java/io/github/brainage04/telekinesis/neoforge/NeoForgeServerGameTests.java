package io.github.brainage04.telekinesis.neoforge;

import io.github.brainage04.telekinesis.Telekinesis;
import io.github.brainage04.telekinesis.TelekinesisBehaviorGameTests;
import io.github.brainage04.telekinesis.TelekinesisGameTests;
import java.util.function.Consumer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * Registers the shared GameTests as NeoForge test functions. Each function needs a matching
 * {@code data/<mod_id>/test_instance/<name>.json} in this source set's resources.
 */
@EventBusSubscriber(modid = Telekinesis.MOD_ID)
public final class NeoForgeServerGameTests {
    private NeoForgeServerGameTests() {
    }

    @SubscribeEvent
    public static void registerTestFunctions(RegisterEvent event) {
        register(event, "mined_block_drop_goes_directly_to_inventory", TelekinesisGameTests::minedBlockDropGoesDirectlyToInventory);
        register(event, "block_experience_goes_directly_to_player", TelekinesisGameTests::blockExperienceGoesDirectlyToPlayer);
        register(event, "random_loot_result_goes_directly_to_inventory", TelekinesisGameTests::randomLootResultGoesDirectlyToInventory);
        register(event, "multiple_unique_container_drops_go_directly_to_inventory", TelekinesisGameTests::multipleUniqueContainerDropsGoDirectlyToInventory);
        register(event, "full_inventory_drops_remainder_at_player_feet", TelekinesisGameTests::fullInventoryDropsRemainderAtPlayerFeet);
        register(event, "disabled_server_config_leaves_drop_in_world", TelekinesisGameTests::disabledServerConfigLeavesDropInWorld);
        register(event, "disabled_player_preference_leaves_drop_in_world", TelekinesisGameTests::disabledPlayerPreferenceLeavesDropInWorld);
        register(event, "personal_commands_and_operator_permissions", TelekinesisBehaviorGameTests::personalCommandsAndOperatorPermissions);
        register(event, "operator_commands_save_and_reload", TelekinesisBehaviorGameTests::operatorCommandsSaveAndReload);
        register(event, "partial_inventory_conserves_overflow", TelekinesisBehaviorGameTests::partialInventoryConservesOverflow);
        register(event, "silk_touch_preserves_loot_and_suppresses_experience", TelekinesisBehaviorGameTests::silkTouchPreservesLootAndSuppressesExperience);
        register(event, "experience_repairs_mending_before_awarding_points", TelekinesisBehaviorGameTests::experienceRepairsMendingBeforeAwardingPoints);
        register(event, "unrelated_spawns_remain_in_world_after_break", TelekinesisBehaviorGameTests::unrelatedSpawnsRemainInWorldAfterBreak);
        register(event, "nested_breaks_restore_outer_player", TelekinesisBehaviorGameTests::nestedBreaksRestoreOuterPlayer);
        register(event, "preferences_survive_server_lifecycle", TelekinesisBehaviorGameTests::preferencesSurviveServerLifecycle);
        register(event, "unreadable_preferences_are_not_overwritten", TelekinesisBehaviorGameTests::unreadablePreferencesAreNotOverwritten);
        register(event, "preferences_from_previous_version_load", TelekinesisBehaviorGameTests::preferencesFromPreviousVersionLoad);
    }

    private static void register(RegisterEvent event, String path, Consumer<GameTestHelper> function) {
        event.register(BuiltInRegistries.TEST_FUNCTION.key(), Identifier.fromNamespaceAndPath(Telekinesis.MOD_ID, path), () -> function);
    }
}
