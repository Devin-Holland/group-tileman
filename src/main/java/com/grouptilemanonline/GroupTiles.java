package com.grouptilemanonline;
import java.util.List;
import java.util.TreeMap;
import lombok.Value;

@Value
public class GroupTiles {
    String playerName;
    TreeMap<String, List<TilemanModeTile>> regionTiles;
}