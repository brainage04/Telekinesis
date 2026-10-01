package io.github.brainage04.telekinesis;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

public class TelekinesisGameTest {
    @GameTest
    public void commandIsRegistered(GameTestHelper context) {
        TelekinesisGameTests.commandIsRegistered(context);
    }

    @GameTest
    public void minedBlockDropGoesDirectlyToInventory(GameTestHelper context) {
        TelekinesisGameTests.minedBlockDropGoesDirectlyToInventory(context);
    }

    @GameTest
    public void blockExperienceGoesDirectlyToPlayer(GameTestHelper context) {
        TelekinesisGameTests.blockExperienceGoesDirectlyToPlayer(context);
    }

    @GameTest
    public void randomLootResultGoesDirectlyToInventory(GameTestHelper context) {
        TelekinesisGameTests.randomLootResultGoesDirectlyToInventory(context);
    }

    @GameTest
    public void multipleUniqueContainerDropsGoDirectlyToInventory(GameTestHelper context) {
        TelekinesisGameTests.multipleUniqueContainerDropsGoDirectlyToInventory(context);
    }

    @GameTest
    public void fullInventoryDropsRemainderAtPlayerFeet(GameTestHelper context) {
        TelekinesisGameTests.fullInventoryDropsRemainderAtPlayerFeet(context);
    }

    @GameTest
    public void disabledServerConfigLeavesDropInWorld(GameTestHelper context) {
        TelekinesisGameTests.disabledServerConfigLeavesDropInWorld(context);
    }

    @GameTest
    public void disabledPlayerPreferenceLeavesDropInWorld(GameTestHelper context) {
        TelekinesisGameTests.disabledPlayerPreferenceLeavesDropInWorld(context);
    }
}
