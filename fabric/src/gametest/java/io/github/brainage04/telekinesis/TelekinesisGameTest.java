package io.github.brainage04.telekinesis;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

public class TelekinesisGameTest {

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

    @GameTest
    public void personalCommandsAndOperatorPermissions(GameTestHelper context) {
        TelekinesisBehaviorGameTests.personalCommandsAndOperatorPermissions(context);
    }

    @GameTest
    public void operatorCommandsSaveAndReload(GameTestHelper context) {
        TelekinesisBehaviorGameTests.operatorCommandsSaveAndReload(context);
    }

    @GameTest
    public void partialInventoryConservesOverflow(GameTestHelper context) {
        TelekinesisBehaviorGameTests.partialInventoryConservesOverflow(context);
    }

    @GameTest
    public void silkTouchPreservesLootAndSuppressesExperience(GameTestHelper context) {
        TelekinesisBehaviorGameTests.silkTouchPreservesLootAndSuppressesExperience(context);
    }

    @GameTest
    public void experienceRepairsMendingBeforeAwardingPoints(GameTestHelper context) {
        TelekinesisBehaviorGameTests.experienceRepairsMendingBeforeAwardingPoints(context);
    }

    @GameTest
    public void unrelatedSpawnsRemainInWorldAfterBreak(GameTestHelper context) {
        TelekinesisBehaviorGameTests.unrelatedSpawnsRemainInWorldAfterBreak(context);
    }

    @GameTest
    public void nestedBreaksRestoreOuterPlayer(GameTestHelper context) {
        TelekinesisBehaviorGameTests.nestedBreaksRestoreOuterPlayer(context);
    }

    @GameTest
    public void preferencesSurviveServerLifecycle(GameTestHelper context) {
        TelekinesisBehaviorGameTests.preferencesSurviveServerLifecycle(context);
    }

    @GameTest
    public void unreadablePreferencesAreNotOverwritten(GameTestHelper context) {
        TelekinesisBehaviorGameTests.unreadablePreferencesAreNotOverwritten(context);
    }
}
