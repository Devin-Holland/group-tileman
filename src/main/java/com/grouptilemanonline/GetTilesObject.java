package com.grouptilemanonline;

import com.google.gson.annotations.SerializedName;
import lombok.Value;

@Value
public class GetTilesObject {
    GroupTiles tiles;

    String username;
}
