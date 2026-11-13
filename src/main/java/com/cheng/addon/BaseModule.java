/*
 * 本文件属于 meteor-cheng 项目：https://github.com/cheng7/meteor-cheng
 * meteor-cheng 是 meteor-cheng 在 Minecraft 26.1 / Fabric 上的移植版本。
 *
 * Copyright (C) 2024-2026 XC_XinCheng
 *
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * 本程序是自由软件：你可以依据 GNU 通用公共许可证（GNU GPL）第 3 版的条款
 * 重新发布和/或修改它。
 *
 * 本程序基于“有用”的期望分发，但不提供任何担保，甚至不提供适销性或特定
 * 用途适用性的默示担保。详见 GNU 通用公共许可证。
 *
 * 你应当已随本程序收到一份 GNU 通用公共许可证副本；如果没有，请见
 * <https://www.gnu.org/licenses/>。
 */
package com.cheng.addon;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import com.cheng.addon.mixin.IClientWorld;
import com.cheng.addon.modules.MEnum;
import meteordevelopment.meteorclient.pathing.IPathManager;
import meteordevelopment.meteorclient.pathing.NopPathManager;
import meteordevelopment.meteorclient.pathing.PathManagers;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.prediction.BlockStatePredictionHandler;
import net.minecraft.client.multiplayer.prediction.PredictiveAction;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

import static com.cheng.addon.util.ChatUtils.sendMsg;

public abstract class BaseModule extends Module implements MEnum {

    public static final Category CATEGORY_MIKU_BUILD = new Category("Cheng 建设");
    public static final Category CATEGORY_MIKU_COMBAT = new Category("Cheng 战斗");
    public static final Category CATEGORY_MIKU_PRO = new Category("Cheng 特调");
    /** 「Cheng 合法」：低行为特征、以真人操作节奏为优先的模块（合法杀戮/挖掘光环等）。 */
    public static final Category CATEGORY_MIKU_LEGIT = new Category("Cheng 合法");
    /**
     * 主分类「Cheng」：图里那个分类。聊天模块（Cheng 聊天）就挂在它下面，
     * 模块名和其它模块一样用主题默认颜色显示。
     */
    public static final Category CATEGORY = new Category("Cheng");

    public Minecraft mc = Minecraft.getInstance();

    IBaritone baritone;

    public BaseModule(Category category, String name, String description, String... aliases) {
        super(category, name, description, aliases);
        mc = Minecraft.getInstance();
    }

    public BaseModule(Category category, String name, String desc) {
        super(category, name, desc);
        mc = Minecraft.getInstance();


        try {
            IPathManager pathManager = PathManagers.get();

            if (PathManagers.get() instanceof NopPathManager) {
                //noop
            } else {
                baritone = BaritoneAPI.getProvider().getPrimaryBaritone();
            }
        } catch (Throwable e) {
            // Baritone 可能整个没装，或者当前游戏版本没有对应构建；
            // 缺失时是 NoClassDefFoundError（Error，不是 Exception），必须按 Throwable 兜住，
            // 否则启用任意模块都会崩游戏。
            error("请安装Baritone!");
        }
    }

    public BaseModule(String name, String desc) {
        super(CATEGORY, name, desc);
    }


    @Override
    public void onActivate() {
        super.onActivate();
        if (mc == null) {
            mc = Minecraft.getInstance();
        }
        initBaritone();
        initMni();
    }

    private void initMinihud() {
        // 这里原来每次启用模块都会 System.out.println("324")，
        // 控制台输出会走同步 IO，已移除（无功能影响）。
    }

    private void initMni() {
        // 这里原来每次启用模块都会 System.out.println("ws")，已移除（无功能影响）。
    }

    private void initBaritone() {

        try {

            try {
                if (PathManagers.get() instanceof NopPathManager) {
                    //noop
                } else {
                    baritone = BaritoneAPI.getProvider().getPrimaryBaritone();
                }
            } catch (Throwable e) {
                error("请安装Baritone!");
            }
        } catch (Throwable e) {

        }
    }

    public void sendToggledMsg() {
        Component onMsg = Component.empty().setStyle(Style.EMPTY.applyFormat(ChatFormatting.GREEN)).append("ON");
        Component offMsg = Component.empty().setStyle(Style.EMPTY.applyFormat(ChatFormatting.RED)).append("OFF");
        ChatUtils.forceNextPrefixClass(getClass());
        MutableComponent toggledMsg = Component.empty();
        toggledMsg.append(Component.empty().setStyle(Style.EMPTY.applyFormat(ChatFormatting.WHITE)).append(title));
        toggledMsg.append(" ");
        toggledMsg.append(Component.empty().setStyle(Style.EMPTY.applyFormat(ChatFormatting.GRAY)).append("toggled "));
        toggledMsg.append((isActive() ? onMsg : offMsg));

        sendMsg(toggledMsg, hashCode());
    }


    public static void sendSequencedPacket(PredictiveAction packetCreator) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() == null || mc.level == null) return;
        try (BlockStatePredictionHandler pendingUpdateManager = ((IClientWorld) mc.level).getPendingManager().startPredicting()) {
            int i = pendingUpdateManager.currentSequence();
            mc.getConnection().send(packetCreator.predict(i));
        }
    }
}
