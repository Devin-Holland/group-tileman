/*
 * Copyright (c) 2019, Benjamin <https://github.com/genetic-soybean>
 * Copyright (c) 2020, ConorLeckey <https://github.com/ConorLeckey>
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
package com.grouptilemanonline;

import com.google.common.base.Strings;
import com.google.gson.Gson;

import com.google.gson.reflect.TypeToken;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.ui.overlay.*;

import javax.inject.Inject;
import java.awt.*;
import java.util.Collection;
import java.util.List;

class TilemanModeMinimapOverlay extends Overlay
{
	private static final int MAX_DRAW_DISTANCE = 16;
	private static final int TILE_WIDTH = 4;
	private static final int TILE_HEIGHT = 4;

	private final Client client;
	private final TilemanModeConfig config;
	private final TilemanModePlugin plugin;

	private final Gson gson;

	@Inject
	private ConfigManager configManager;

	@Inject
	private TilemanModeMinimapOverlay(Client client, TilemanModeConfig config, TilemanModePlugin plugin, Gson gson)
	{
		this.client = client;
		this.config = config;
		this.plugin = plugin;
		this.gson = gson;
		setPosition(OverlayPosition.DYNAMIC);
		setPriority(OverlayPriority.LOW);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.drawTilesOnMinimap())
		{
			return null;
		}

		final Collection<TilemanModeTile> points = plugin.getPoints();
		for (final TilemanModeTile tilemanPoint : points)
		{
			final WorldPoint worldPoint = translateToWorldPoint(tilemanPoint);
			if (worldPoint == null) { continue; }

			if (worldPoint.getPlane() != client.getPlane())
			{
				continue;
			}

			drawOnMinimap(graphics, worldPoint, tilemanPoint.getPlayerName());
		}

		return null;
	}

	private void drawOnMinimap(Graphics2D graphics, WorldPoint point, String playerName)
	{
		WorldPoint playerLocation = client.getLocalPlayer().getWorldLocation();

		if (point.distanceTo(playerLocation) >= MAX_DRAW_DISTANCE)
		{
			return;
		}

		LocalPoint lp = LocalPoint.fromWorld(client, point);
		if (lp == null)
		{
			return;
		}

		Point posOnMinimap = Perspective.localToMinimap(client, lp);
		if (posOnMinimap == null)
		{
			return;
		}

		OverlayUtil.renderMinimapRect(client, graphics, posOnMinimap, TILE_WIDTH, TILE_HEIGHT, getTileColor(playerName));
	}

	private Color getTileColor(String playerName) {
		if (playerName == plugin.getPlayerName()) {
			if (config.enableTileWarnings()) {
				if (plugin.getRemainingTiles() <= 0) {
					return Color.RED;
				} else if (plugin.getRemainingTiles() <= config.warningLimit()) {
					return new Color(255, 153, 0);
				}
			}
		}

		String groupMembersJson = configManager.getConfiguration(TilemanModePlugin.CONFIG_GROUP, "groupmembers");
		if (!Strings.isNullOrEmpty(groupMembersJson)) {
			java.util.List<GroupMember> groupMembers = gson.fromJson(groupMembersJson, new TypeToken<List<GroupMember>>() {}.getType());
			groupMembers.removeIf(member -> !member.getPlayerName().equals(playerName));
			if (groupMembers.size() == 1) {
				int memberNumber = groupMembers.get(0).getMemberNumber();
				switch (memberNumber) {
					case 1:
						return config.groupMarkerColor1();
					case 2:
						return config.groupMarkerColor2();
					case 3:
						return config.groupMarkerColor3();
					case 4:
						return config.groupMarkerColor4();
					default:
						break;
				}
			}
		}

		return config.markerColor();
	}

	private WorldPoint translateToWorldPoint(TilemanModeTile point) {
		if (point == null) {
			return null;
		}

		return WorldPoint.fromRegion(point.getRegionId(), point.getRegionX(), point.getRegionY(), point.getZ());
	}
}
