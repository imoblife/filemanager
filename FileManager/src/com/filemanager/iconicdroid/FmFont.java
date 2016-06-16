/*
 * Copyright 2014 Mike Penz
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.filemanager.iconicdroid;

import android.content.Context;
import android.graphics.Typeface;
import com.iconics.typeface.IIcon;
import com.iconics.typeface.ITypeface;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;

public class FmFont implements ITypeface {
    private static final String TTF_FILE = "fmfont.ttf";

    private static Typeface typeface = null;

    private static HashMap<String, Character> mChars;

    @Override
    public IIcon getIcon(String key) {
        return Icon.valueOf(key);
    }

    @Override
    public HashMap<String, Character> getCharacters() {
        if (mChars == null) {
            HashMap<String, Character> aChars = new HashMap<String, Character>();
            for (Icon v : Icon.values()) {
                aChars.put(v.name(),
                        v.character);
            }
            mChars = aChars;
        }

        return mChars;
    }

    @Override
    public String getMappingPrefix() {
        return "FMT";
    }

    @Override
    public String getFontName() {
        return "FmFont";
    }

    @Override
    public String getVersion() {
        return "1.0.0.1";
    }

    @Override
    public int getIconCount() {
        return mChars.size();
    }

    @Override
    public Collection<String> getIcons() {
        Collection<String> icons = new LinkedList<String>();

        for (Icon value : Icon.values()) {
            icons.add(value.name());
        }

        return icons;
    }

    @Override
    public String getAuthor() {
        return "";
    }

    @Override
    public String getUrl() {
        return "";
    }

    @Override
    public String getDescription() {
        return "";
    }

    @Override
    public String getLicense() {
        return "CC BY-SA 4.0";
    }

    @Override
    public String getLicenseUrl() {
        return "";
    }

    @Override
    public Typeface getTypeface(Context context) {
        if (typeface == null) {
            try {
                typeface = Typeface.createFromAsset(context.getAssets(), "fonts/" + TTF_FILE);
            } catch (Exception e) {
                return null;
            }
        }
        return typeface;
    }

    public enum Icon implements IIcon {

        FMT_ICON_COPY('\uE900'),
        FMT_ICON_CUT('\uE901'),
        FMT_ICON_DELETE('\uE902'),
        FMT_ICON_SELECT_NONE('\uE903'),
        FMT_ICON_SELECT_ALL('\uE904'),
        FMT_ICON_MORE('\uE905'),
        FMT_ICON_RENAME('\uE906'),
        FMT_ICON_SEND('\uE907'),
        FMT_ICON_SHORT('\uE908'),
        FMT_ICON_ZIP('\uE909'),
        FMT_ICON_UNZIP('\uE91A'),


        FMT_ICON_IMAGE('\uE91B'),
        FMT_ICON_MUSIC('\uE91C'),
        FMT_ICON_DOWNLOAD('\uE91D'),
        FMT_ICON_DOCUMENT('\uE91E'),
        FMT_ICON_APK('\uE91F'),
        FMT_ICON_VIDEO('\uE920'),
        FMT_ICON_COMPRESS('\uE921'),
        FMT_ICON_RECENT('\uE922'),
        FMT_ICON_INTERNAL('\uE923'),
        FMT_ICON_SD('\uE924');


        char character;

        Icon(char character) {
            this.character = character;
        }

        public String getFormattedName() {
            return "{" + name() + "}";
        }

        public char getCharacter() {
            return character;
        }

        public String getName() {
            return name();
        }

        // remember the typeface so we can use it later
        private static ITypeface typeface;

        public ITypeface getTypeface() {
            if (typeface == null) {
                typeface = new FmFont();
            }
            return typeface;
        }
    }
}
