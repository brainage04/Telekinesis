package io.github.brainage04.telekinesis;

import net.minecraft.gametest.framework.GameTestHelper;

import java.util.List;
import java.util.function.Consumer;

public final class TelekinesisNeoForgeGameTestSuite {
    private TelekinesisNeoForgeGameTestSuite() {
    }

    public static List<TestCase> tests() {
        return List.of(
                test("command_is_registered", TelekinesisGameTest::commandIsRegistered),
                test("mined_block_drop_goes_directly_to_inventory", TelekinesisGameTest::minedBlockDropGoesDirectlyToInventory),
                test("block_experience_goes_directly_to_player", TelekinesisGameTest::blockExperienceGoesDirectlyToPlayer),
                test("random_loot_result_goes_directly_to_inventory", TelekinesisGameTest::randomLootResultGoesDirectlyToInventory),
                test("multiple_unique_container_drops_go_directly_to_inventory", TelekinesisGameTest::multipleUniqueContainerDropsGoDirectlyToInventory),
                test("full_inventory_drops_remainder_at_player_feet", TelekinesisGameTest::fullInventoryDropsRemainderAtPlayerFeet),
                test("disabled_server_config_leaves_drop_in_world", TelekinesisGameTest::disabledServerConfigLeavesDropInWorld),
                test("disabled_player_preference_leaves_drop_in_world", TelekinesisGameTest::disabledPlayerPreferenceLeavesDropInWorld)
        );
    }

    private static TestCase test(String path, Consumer<GameTestHelper> function) {
        return new TestCase(path, function);
    }

    public record TestCase(String path, Consumer<GameTestHelper> function) {
    }
}
