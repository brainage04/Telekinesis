package io.github.brainage04.telekinesis.neoforge;

import io.github.brainage04.telekinesis.Telekinesis;
import io.github.brainage04.telekinesis.TelekinesisBehaviorGameTests;
import io.github.brainage04.telekinesis.TelekinesisGameTests;
import io.github.brainage04.telekinesis.TelekinesisKillAndExplosionGameTests;
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
        register(event, "melee_kill_sends_loot_equipment_and_experience_to_killer", TelekinesisKillAndExplosionGameTests::meleeKillSendsLootEquipmentAndExperienceToKiller);
        register(event, "projectile_kill_sends_loot_to_shooter", TelekinesisKillAndExplosionGameTests::projectileKillSendsLootToShooter);
        register(event, "looting_applies_to_captured_loot", TelekinesisKillAndExplosionGameTests::lootingAppliesToCapturedLoot);
        register(event, "full_inventory_drops_kill_loot_at_killer_feet", TelekinesisKillAndExplosionGameTests::fullInventoryDropsKillLootAtKillerFeet);
        register(event, "tnt_lit_by_player_sends_block_drops_to_player", TelekinesisKillAndExplosionGameTests::tntLitByPlayerSendsBlockDropsToPlayer);
        register(event, "explosion_without_player_leaves_drops_in_world", TelekinesisKillAndExplosionGameTests::explosionWithoutPlayerLeavesDropsInWorld);
        register(event, "mob_death_without_player_leaves_drops_in_world", TelekinesisKillAndExplosionGameTests::mobDeathWithoutPlayerLeavesDropsInWorld);
        register(event, "another_players_kill_goes_to_that_player", TelekinesisKillAndExplosionGameTests::anotherPlayersKillGoesToThatPlayer);
        register(event, "killed_players_drops_stay_in_world", TelekinesisKillAndExplosionGameTests::killedPlayersDropsStayInWorld);
        register(event, "dead_tnt_owner_leaves_drops_in_world", TelekinesisKillAndExplosionGameTests::deadTntOwnerLeavesDropsInWorld);
        register(event, "dead_killer_leaves_kill_loot_in_world", TelekinesisKillAndExplosionGameTests::deadKillerLeavesKillLootInWorld);
        register(event, "expired_kill_credit_leaves_kill_loot_in_world", TelekinesisKillAndExplosionGameTests::expiredKillCreditLeavesKillLootInWorld);
        register(event, "player_death_inside_another_scope_stays_in_world", TelekinesisKillAndExplosionGameTests::playerDeathInsideAnotherScopeStaysInWorld);
        register(event, "dragon_death_experience_goes_to_killer", TelekinesisKillAndExplosionGameTests::dragonDeathExperienceGoesToKiller);
        register(event, "tamed_wolf_kill_goes_to_owner", TelekinesisKillAndExplosionGameTests::tamedWolfKillGoesToOwner);
        register(event, "harming_potion_kill_goes_to_thrower", TelekinesisKillAndExplosionGameTests::harmingPotionKillGoesToThrower);
        register(event, "chained_tnt_credits_player_who_lit_the_first", TelekinesisKillAndExplosionGameTests::chainedTntCreditsPlayerWhoLitTheFirst);
        register(event, "living_drops_event_item_goes_to_killer_once", NeoForgeLivingDropsGameTests::livingDropsEventItemGoesToKillerOnce);
        register(event, "command_items_are_not_captured", TelekinesisKillAndExplosionGameTests::commandItemsAreNotCaptured);
    }

    private static void register(RegisterEvent event, String path, Consumer<GameTestHelper> function) {
        event.register(BuiltInRegistries.TEST_FUNCTION.key(), Identifier.fromNamespaceAndPath(Telekinesis.MOD_ID, path), () -> function);
    }
}
