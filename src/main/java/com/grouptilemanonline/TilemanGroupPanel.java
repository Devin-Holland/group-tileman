/*
 * Copyright (c) 2021, Jonathan Rousseau <https://github.com/JoRouss>
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
import com.google.common.base.CharMatcher;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.ItemComposition;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.PluginErrorPanel;
import net.runelite.client.callback.ClientThread;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Random;

@Slf4j
@Singleton
public class TilemanGroupPanel extends PluginPanel {
    private final TilemanModePlugin plugin;

    private static final String ALPHABET = "abcdfghjklmnpqrstvwxyz";
    private static final String BTN_CREATE_GROUP_TEXT = "Create group";
    private static final String BTN_JOIN_GROUP_TEXT = "Join group";
    private static final String BTN_COPY_CODE_TEXT = "Copy Join Code";
    private static final String BTN_LEAVE_GROUP_TEXT = "Leave Group";

    private final JButton startButton = new JButton();
    private final JButton joinButton = new JButton();
    private final JButton copyJoinCodeButton = new JButton();

    private final PluginErrorPanel noGroupPanel = new PluginErrorPanel();
    private final PluginErrorPanel activeGroupPanel = new PluginErrorPanel();
    private final Client client;

    private final ConfigManager configManager;

    private final Gson gson;

    @Inject
    public TilemanGroupPanel(TilemanModePlugin plugin, Client client, ClientThread clientThread, ConfigManager configManager, Gson gson) {
        this.plugin = plugin;
        this.client = client;
        this.configManager = configManager;
        this.gson = gson;

        setBorder(new EmptyBorder(10, 10, 10, 10));
        setBackground(ColorScheme.DARK_GRAY_COLOR);
        setLayout(new BorderLayout());

        final JPanel layoutPanel = new JPanel();
        BoxLayout boxLayout = new BoxLayout(layoutPanel, BoxLayout.Y_AXIS);
        layoutPanel.setLayout(boxLayout);
        add(layoutPanel, BorderLayout.NORTH);

        final JPanel topPanel = new JPanel();

        topPanel.setBorder(new EmptyBorder(0, 0, 4, 0));
        topPanel.setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 2, 4, 2);

        c.gridx = 0;
        c.gridy = 0;
        topPanel.add(startButton, c);

        c.gridx = 1;
        c.gridy = 0;
        topPanel.add(joinButton, c);

        c.gridx = 1;
        c.gridy = 0;
        topPanel.add(copyJoinCodeButton, c);
        layoutPanel.add(topPanel);

        startButton.setText(isInGroup() ? BTN_LEAVE_GROUP_TEXT : BTN_CREATE_GROUP_TEXT);
        startButton.setFocusable(false);

        joinButton.setText(BTN_JOIN_GROUP_TEXT);
        joinButton.setFocusable(false);


        copyJoinCodeButton.setText(BTN_COPY_CODE_TEXT);
        copyJoinCodeButton.setFocusable(false);

        startButton.addActionListener(e ->
        {
            if (isInGroup())
            {
                // Leave party
                final int result = JOptionPane.showOptionDialog(startButton,
                        "Are you sure you want to leave the Tileman Group?",
                        "Leave Tileman Group?", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE,
                        null, new String[]{"Yes", "No"}, "No");

                if (result == JOptionPane.YES_OPTION)
                {
                    configManager.setConfiguration(TilemanModePlugin.CONFIG_GROUP, "groupJoinCode", "");
                    updateGroup();
                    //TODO: Post to database
                }
            }
            else
            {
                // Create party
                clientThread.invokeLater(() -> this.generatePassphrase());
            }
        });

        joinButton.addActionListener(e ->
        {
            if (!isInGroup())
            {
                String s = (String) JOptionPane.showInputDialog(
                        joinButton,
                        "Please enter the Tileman Group Join Code:",
                        "Tileman Group Join Code",
                        JOptionPane.PLAIN_MESSAGE,
                        null,
                        null,
                        "");

                if (s == null)
                {
                    return;
                }

                for (int i = 0; i < s.length(); ++i)
                {
                    char ch = s.charAt(i);
                    if (!Character.isLetter(ch) && !Character.isDigit(ch))
                    {
                        JOptionPane.showMessageDialog(joinButton,
                                "Group Tileman Join Code must be a combination of alphanumeric characters.",
                                "Invalid Group Tileman Join Code passphrase",
                                JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }

                configManager.setConfiguration(TilemanModePlugin.CONFIG_GROUP, "groupJoinCode", s);
                updateGroup();
                // TODO: Post to database
            }
        });

        copyJoinCodeButton.addActionListener(e ->
        {
            if (isInGroup())
            {
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                clipboard.setContents(new StringSelection(this.getGroupJoinCode()), null);
            }
        });

        noGroupPanel.setContent("Not in a party", "Create a party to begin.");

        updateGroup();
    }

    private void updateGroup(){
        remove(noGroupPanel);
        remove(activeGroupPanel);
        startButton.setText(isInGroup() ? BTN_LEAVE_GROUP_TEXT : BTN_CREATE_GROUP_TEXT);

        joinButton.setVisible(!isInGroup());
        copyJoinCodeButton.setVisible(isInGroup());

        if (!isInGroup()) {
            add(noGroupPanel);
        }
        else {
            String groupPanelText = "You can now invite group members.<br/>" +
                    "Your party join code is: " + getGroupJoinCode() + "<br/><br/>" +
                    "Your other group members (refresh by disabling & re-enabling plugin):<br/><br/>";

            String groupMembersJson = configManager.getConfiguration(TilemanModePlugin.CONFIG_GROUP, "groupmembers");
            if (!Strings.isNullOrEmpty(groupMembersJson)) {
                Collection<GroupMember> groupMembers = gson.fromJson(groupMembersJson, new TypeToken<List<GroupMember>>() {
                }.getType());

                for (GroupMember member : groupMembers) {
                    groupPanelText += "Group Member " + member.getMemberNumber() + ": " + member.getPlayerName() + "<br/>";
                }
            }

            activeGroupPanel.setContent("Group Active!", groupPanelText);
            add(activeGroupPanel);

            // TODO: List group members?
        }
    }

    private void generatePassphrase()
    {
        assert client.isClientThread();

        Random r = new Random();
        StringBuilder sb = new StringBuilder();

        if (client.getGameState().getState() >= GameState.LOGIN_SCREEN.getState())
        {
            int len = 0;
            final CharMatcher matcher = CharMatcher.javaLetter();
            do
            {
                final int itemId = r.nextInt(client.getItemCount());
                final ItemComposition def = client.getItemDefinition(itemId);
                final String name = def.getName();
                if (name == null || name.isEmpty() || name.equals("null"))
                {
                    continue;
                }

                final String[] split = name.split(" ");
                final String token = split[r.nextInt(split.length)];
                if (!matcher.matchesAllOf(token) || token.length() <= 2)
                {
                    continue;
                }

                sb.append(token.toLowerCase(Locale.US));
                ++len;
            }
            while (len < 4);
        }
        else
        {
            int len = 0;
            do
            {
                for (int i = 0; i < 5; ++i)
                {
                    sb.append(ALPHABET.charAt(r.nextInt(ALPHABET.length())));
                }
                ++len;
            }
            while (len < 4);
        }

        String partyPassphrase = sb.toString();
        log.debug("Generated party passphrase {}", partyPassphrase);

        configManager.setConfiguration(TilemanModePlugin.CONFIG_GROUP, "groupJoinCode", partyPassphrase);
        updateGroup();

        // TODO: Post to database
    }

    private String getGroupJoinCode() {
        return configManager.getConfiguration(TilemanModePlugin.CONFIG_GROUP, "groupJoinCode");
    }

    private boolean isInGroup() {
        return !Strings.isNullOrEmpty(configManager.getConfiguration(TilemanModePlugin.CONFIG_GROUP,
                "groupJoinCode"));
    }
}
