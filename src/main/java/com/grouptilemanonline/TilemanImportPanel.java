package com.grouptilemanonline;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;

import javax.inject.Singleton;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

@Slf4j
@Singleton
public class TilemanImportPanel extends PluginPanel {
    private final TilemanModePlugin plugin;

    public TilemanImportPanel(TilemanModePlugin plugin) {
        this.plugin = plugin;

        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBorder(new EmptyBorder(1, 0, 10, 0));

        JLabel title = new JLabel();
        title.setText("Group Tileman Mode Config Panel");
        title.setForeground(Color.WHITE);

        titlePanel.add(title, BorderLayout.NORTH);

        // Ground Marker Plugin Import Info
        JPanel infoPanel = new JPanel();
        infoPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        infoPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        infoPanel.setLayout(new GridLayout(0, 1));

        JLabel info = new JLabel(htmlLabel("Clicking the Import button below will migrate all tiles marked with the Ground Marker plugin into the Tileman Mode plugin. They will NOT be removed from the Ground Marker Plugin.", "#FFFFFF"));

        JLabel warning = new JLabel(htmlLabel("WARNING: This directly modifies RuneLite's settings.properties file. You should make a back up before importing.", "#FFFF00"));

        JLabel groupTilemanInfoCreate = new JLabel(htmlLabel(
        "To Create a Group Tileman Group, enter a group name and a SECRET join code that you will "
                + "share with other group members, then click the 'Create Group' button.", "#FFFFFF"
        ));

        JLabel groupTilemanInfoJoin = new JLabel(htmlLabel(
        "To Join a Group Tileman Group, enter the group name and the SECRET group join code from the group's "
                + "creation, then click the 'Join Group' button.", "#FFFFFF"
        ));


        infoPanel.add(info);
        infoPanel.add(warning);
        infoPanel.add(groupTilemanInfoCreate);
        infoPanel.add(groupTilemanInfoJoin);

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        centerPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        centerPanel.setLayout(new GridLayout(0, 1));

        JButton importButton = new JButton("Import");
        centerPanel.add(importButton, BorderLayout.SOUTH);
        importButton.addActionListener(l -> plugin.importGroundMarkerTiles());

        JLabel groupNameLabel = new JLabel();
        groupNameLabel.setText("Group Name");
        groupNameLabel.setForeground(Color.WHITE);
        groupNameLabel.setBorder(new EmptyBorder(10, 0, 0, 0));
        centerPanel.add(groupNameLabel);

        JTextField groupNameInput = new JTextField(10);
        centerPanel.add(groupNameInput);

        JLabel groupJoinCodeLabel = new JLabel();
        groupJoinCodeLabel.setText("Group Join Code (Alphanumeric)");
        groupJoinCodeLabel.setForeground(Color.WHITE);
        groupJoinCodeLabel.setBorder(new EmptyBorder(10, 0, 0, 0));
        centerPanel.add(groupJoinCodeLabel);

        JTextField groupJoinCodeInput = new JTextField(10);
        centerPanel.add(groupJoinCodeInput);

        JLabel blankSpaceBaby = new JLabel();
        blankSpaceBaby.setText(" ");
        centerPanel.add(blankSpaceBaby);

        JButton createGroup = new JButton("Create Group");
        centerPanel.add(createGroup);

        JButton joinGroup = new JButton("Join Group");
        centerPanel.add(joinGroup);

        importButton.setToolTipText("Import Ground Markers");

        add(titlePanel, BorderLayout.NORTH);
        add(infoPanel, BorderLayout.CENTER);
        add(centerPanel, BorderLayout.SOUTH);
    }


    private static String htmlLabel(String key, String color)
    {
        return "<html><body style = 'color:" + color + "'>" + key + "</body></html>";
    }
}