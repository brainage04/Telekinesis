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

    @GameTest
    public void preferencesFromPreviousVersionLoad(GameTestHelper context) {
        TelekinesisBehaviorGameTests.preferencesFromPreviousVersionLoad(context);
    }

    @GameTest
    public void meleeKillSendsLootEquipmentAndExperienceToKiller(GameTestHelper context) {
        TelekinesisKillAndExplosionGameTests.meleeKillSendsLootEquipmentAndExperienceToKiller(context);
    }

    @GameTest(maxTicks = 100)
    public void projectileKillSendsLootToShooter(GameTestHelper context) {
        TelekinesisKillAndExplosionGameTests.projectileKillSendsLootToShooter(context);
    }

    @GameTest
    public void lootingAppliesToCapturedLoot(GameTestHelper context) {
        TelekinesisKillAndExplosionGameTests.lootingAppliesToCapturedLoot(context);
    }

    @GameTest
    public void fullInventoryDropsKillLootAtKillerFeet(GameTestHelper context) {
        TelekinesisKillAndExplosionGameTests.fullInventoryDropsKillLootAtKillerFeet(context);
    }

    @GameTest(maxTicks = 100, padding = 16)
    public void tntLitByPlayerSendsBlockDropsToPlayer(GameTestHelper context) {
        TelekinesisKillAndExplosionGameTests.tntLitByPlayerSendsBlockDropsToPlayer(context);
    }

    @GameTest(maxTicks = 100, padding = 16)
    public void explosionWithoutPlayerLeavesDropsInWorld(GameTestHelper context) {
        TelekinesisKillAndExplosionGameTests.explosionWithoutPlayerLeavesDropsInWorld(context);
    }

    @GameTest
    public void mobDeathWithoutPlayerLeavesDropsInWorld(GameTestHelper context) {
        TelekinesisKillAndExplosionGameTests.mobDeathWithoutPlayerLeavesDropsInWorld(context);
    }

    @GameTest
    public void anotherPlayersKillGoesToThatPlayer(GameTestHelper context) {
        TelekinesisKillAndExplosionGameTests.anotherPlayersKillGoesToThatPlayer(context);
    }

    @GameTest(maxTicks = 100, padding = 16)
    public void deadTntOwnerLeavesDropsInWorld(GameTestHelper context) {
        TelekinesisKillAndExplosionGameTests.deadTntOwnerLeavesDropsInWorld(context);
    }

    @GameTest
    public void deadKillerLeavesKillLootInWorld(GameTestHelper context) {
        TelekinesisKillAndExplosionGameTests.deadKillerLeavesKillLootInWorld(context);
    }

    @GameTest
    public void expiredKillCreditLeavesKillLootInWorld(GameTestHelper context) {
        TelekinesisKillAndExplosionGameTests.expiredKillCreditLeavesKillLootInWorld(context);
    }

    @GameTest
    public void playerDeathInsideAnotherScopeStaysInWorld(GameTestHelper context) {
        TelekinesisKillAndExplosionGameTests.playerDeathInsideAnotherScopeStaysInWorld(context);
    }

    @GameTest(maxTicks = 300, padding = 16)
    public void dragonDeathExperienceGoesToKiller(GameTestHelper context) {
        TelekinesisKillAndExplosionGameTests.dragonDeathExperienceGoesToKiller(context);
    }

    @GameTest
    public void killedPlayersDropsStayInWorld(GameTestHelper context) {
        TelekinesisKillAndExplosionGameTests.killedPlayersDropsStayInWorld(context);
    }

    @GameTest
    public void commandItemsAreNotCaptured(GameTestHelper context) {
        TelekinesisKillAndExplosionGameTests.commandItemsAreNotCaptured(context);
    }
}
