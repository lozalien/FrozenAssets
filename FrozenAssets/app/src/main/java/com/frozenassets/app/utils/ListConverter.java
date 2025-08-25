package com.frozenassets.app.utils;

import androidx.room.TypeConverter;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class ListConverter {
    private static final Gson gson = new Gson();
    private static final Type listType = new TypeToken<ArrayList<String>>(){}.getType();

    @TypeConverter
    public static String fromList(List<String> list) {
        if (list == null) {
            return null;
        }
        return gson.toJson(list);
    }

    @TypeConverter
    public static List<String> toList(String value) {
        if (value == null) {
            return null;
        }
        return gson.fromJson(value, listType);
    }
}