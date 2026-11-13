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
package com.cheng.addon.gui;

import com.cheng.addon.util.ChatScheme;
import com.cheng.addon.util.ChatSchemeListSetting;
import com.cheng.addon.util.ChengChatSnapshot;
import com.cheng.addon.util.ChengChatStore;
import com.cheng.addon.util.ChengCompat;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.utils.player.ChatUtils;

import java.util.List;

/**
 * 「加载旧方案集」界面：把 {@code Cheng-Chat-old.txt} 里保存过的每一份旧方案集列成一张表，
 * 最新的排在最上面。每行可以：
 *
 * <ul>
 *   <li>「查看」——打开 {@link ChatSnapshotScreen}，在新的界面里看这一份里有哪些方案；</li>
 *   <li>「恢复」——用这一份旧方案集替换当前方案，并写回 {@code Cheng-Chat.txt}。</li>
 * </ul>
 *
 * <p>恢复后用构造时传进来的回调把设置界面里的方案表重建一遍，所以回到设置界面看到的就是
 * 已经恢复好的内容，不用手动刷新。</p>
 */
public class ChatHistoryScreen extends WindowScreen {

    private final ChatSchemeListSetting setting;
    private final Runnable onRestored;

    public ChatHistoryScreen(GuiTheme theme, ChatSchemeListSetting setting, Runnable onRestored) {
        super(theme, "加载旧方案集");
        this.setting = setting;
        this.onRestored = onRestored;
    }

    @Override
    public void initWidgets() {
        add(theme.label("每次点「重置」都会把当时的整套方案存一份进来，最新的排在最上面。"));
        add(theme.label("点「查看」看这一份里有哪些方案；点「恢复」用它替换掉当前方案。"));

        WTable table = add(theme.table()).expandX().widget();
        fill(table);

        WButton refresh = add(theme.button("刷新")).widget();
        refresh.tooltip = "重新读一遍 " + ChengChatStore.OLD_FILE_NAME;
        refresh.action = this::reload;
    }

    /** 把历史方案集倒着填进表里（文件里最新的在最后，界面上最新的在最上面）。 */
    private void fill(WTable table) {
        table.clear();

        List<ChengChatSnapshot> history = ChengChatStore.loadHistory();

        table.add(theme.label("旧方案集")).expandCellX().widget();
        table.add(theme.label("方案数")).widget();
        table.add(theme.label("查看")).widget();
        table.add(theme.label("恢复")).widget();
        table.row();

        if (history.isEmpty()) {
            table.add(theme.label("还没有旧方案集。")).expandCellX().widget();
            table.row();
            return;
        }

        for (int i = history.size() - 1; i >= 0; i--) {
            ChengChatSnapshot snapshot = history.get(i);

            table.add(theme.label(snapshot.title())).expandCellX().widget();
            table.add(theme.label(snapshot.summary())).widget();

            WButton view = table.add(theme.button("查看")).widget();
            view.tooltip = "在新的界面里看这一份旧方案集里的所有方案";
            view.action = () -> {
                ChatSnapshotScreen screen = new ChatSnapshotScreen(theme, snapshot);
                screen.setOnRestore(() -> apply(snapshot));
                ChengCompat.setScreen(screen);
            };

            WButton restore = table.add(theme.button("恢复")).widget();
            restore.tooltip = "用这一份旧方案集替换当前方案，并写回 " + ChengChatStore.FILE_NAME;
            restore.action = () -> {
                apply(snapshot);
                onClose();
            };

            table.row();
        }
    }

    /** 把一份旧方案集换回当前方案，写回主文件，并让设置界面里的表格重建。 */
    private void apply(ChengChatSnapshot snapshot) {
        List<ChatScheme> list = setting.get();
        list.clear();

        long now = System.currentTimeMillis();
        for (ChatScheme scheme : snapshot.schemes) {
            ChatScheme copy = scheme.copy();
            copy.resetRuntime(now);
            list.add(copy);
        }
        setting.onChanged();

        if (ChengChatStore.save(ChengChatStore.mainFile(), list)) {
            ChatUtils.info("已恢复旧方案集「" + snapshot.title() + "」，共 " + list.size() + " 套方案");
        } else {
            ChatUtils.warning("方案已恢复，但写回 " + ChengChatStore.FILE_NAME + " 失败");
        }

        if (onRestored != null) onRestored.run();
    }
}
