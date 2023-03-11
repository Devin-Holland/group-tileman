package com.grouptilemanonline;

/*
 * Copyright (c) 2021, Adam <Adam@sigterm.info>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */


import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;

import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.MenuEntry;

import static net.runelite.api.widgets.WidgetInfo.MINIMAP_WORLDMAP_OPTIONS;

import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.chatbox.ChatboxPanelManager;
import net.runelite.client.menus.MenuManager;
import net.runelite.client.menus.WidgetMenuOption;

public class DatabaseIntegrationManager {
    private static final WidgetMenuOption EXPORT_MARKERS_OPTION = new WidgetMenuOption("Export", "Tileman Markers", MINIMAP_WORLDMAP_OPTIONS);
    private static final WidgetMenuOption IMPORT_MARKERS_OPTION = new WidgetMenuOption("Import", "Group Tileman Markers", MINIMAP_WORLDMAP_OPTIONS);
    private final TilemanModePlugin plugin;
    private final Client client;
    private final MenuManager menuManager;
    private final ChatMessageManager chatMessageManager;
    private final ChatboxPanelManager chatboxPanelManager;
    private final Gson gson;

    @Inject
    private ConfigManager configManager;

    @Inject
    private DatabaseIntegrationManager(TilemanModePlugin plugin, Client client, MenuManager menuManager,
                                       ChatMessageManager chatMessageManager, ChatboxPanelManager chatboxPanelManager, Gson gson) {
        this.plugin = plugin;
        this.client = client;
        this.menuManager = menuManager;
        this.chatMessageManager = chatMessageManager;
        this.chatboxPanelManager = chatboxPanelManager;
        this.gson = gson;
    }

    public void addImportExportMenuOptions() {
        menuManager.addManagedCustomMenu(EXPORT_MARKERS_OPTION, this::exportTilesFromPlayer);
        menuManager.addManagedCustomMenu(IMPORT_MARKERS_OPTION, this::importTilesFromPlayer);
    }

    private void exportTilesFromPlayer(MenuEntry menuEntry) {
        List<String> keys = configManager.getConfigurationKeys(TilemanModePlugin.CONFIG_GROUP);

        TreeMap<String, List<TilemanModeTile>> tilesToExport = new TreeMap<>();

        for (String key : keys) {
            if (key.startsWith(TilemanModePlugin.CONFIG_GROUP + "." + TilemanModePlugin.REGION_PREFIX)) {
                key = key.replace(TilemanModePlugin.CONFIG_GROUP + ".","");
                List<TilemanModeTile> regionTiles = gson.fromJson(configManager.getConfiguration(TilemanModePlugin.CONFIG_GROUP, key), new TypeToken<List<TilemanModeTile>>() {
                }.getType());
                regionTiles.removeIf(tile -> tile.getPlayerName().equals(plugin.getPlayerName()));
                if(regionTiles.size() > 0 ) {
                  tilesToExport.put(key, regionTiles);
                }
            }
        }

        final String exportDump = gson.toJson(new GroupTiles(plugin.getPlayerName(), tilesToExport));

        Toolkit.getDefaultToolkit()
                .getSystemClipboard()
                .setContents(new StringSelection(exportDump), null);
    }

    private void importTilesFromPlayer(MenuEntry menuEntry) {
        final String clipboardText;
        try {
            clipboardText = Toolkit.getDefaultToolkit()
                    .getSystemClipboard()
                    .getData(DataFlavor.stringFlavor)
                    .toString();
        } catch (IOException | UnsupportedFlavorException ex) {
            sendChatMessage("Unable to read system clipboard.");
            return;
        }

        if (Strings.isNullOrEmpty(clipboardText)) {
            sendChatMessage("You do not have any ground markers copied in your clipboard.");
            return;
        }
        try {
            GroupTiles remoteTiles = gson.fromJson(clipboardText, GroupTiles.class);
            for (String region : remoteTiles.getRegionTiles().keySet() ) {
                Collection<TilemanModeTile> localRegionTiles = Collections.emptyList();
                String json = configManager.getConfiguration(TilemanModePlugin.CONFIG_GROUP, region);
                if (!Strings.isNullOrEmpty(json)) {
                    localRegionTiles = gson.fromJson(json, new TypeToken<List<TilemanModeTile>>() {
                    }.getType());
                }
                Collection<TilemanModeTile> remoteRegionTiles = remoteTiles.getRegionTiles().get(region);
                Collection<TilemanModeTile> mergedRegionTiles = Stream.concat(localRegionTiles.stream(), remoteRegionTiles.stream())
                        .collect(Collectors.toList());

                configManager.setConfiguration(TilemanModePlugin.CONFIG_GROUP, region, gson.toJson(mergedRegionTiles));
            }

            plugin.loadPoints();
        } catch (JsonSyntaxException e) {
            sendChatMessage("You do not have any ground markers copied in your clipboard.");
        }
    }

    private void sendChatMessage(final String message) {
        chatMessageManager.queue(QueuedMessage.builder()
                .type(ChatMessageType.CONSOLE)
                .runeLiteFormattedMessage(message)
                .build());
    }
}