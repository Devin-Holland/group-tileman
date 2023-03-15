package com.grouptilemanonline;

import com.google.gson.annotations.SerializedName;
import lombok.Value;

@Value
public class LeaveGroupObject {
    String username;

    @SerializedName("group_join_code")
    String groupJoinCode;
}
