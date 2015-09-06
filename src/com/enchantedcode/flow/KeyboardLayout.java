package com.enchantedcode.flow;

/**
 * Copyright 2011-2013 by Peter Eastman
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.Arrays;

public class KeyboardLayout
{
  public static final char ENTER = 0;
  public static final char DELETE = 1;
  public static final char SHIFT = 2;
  public static final char ALT = 3;
  public static final char VOICE = 4;
  public static final char FORWARD_DELETE = 5;

  public static enum KeyType {VOWEL, CONSONANT, NUMBER, PUNCTUATION, CONTROL}
  public final char keys[];
  public final KeyType keyType[];
  public final int keyCode[];
  public final int slideCharIndex[];

  public KeyboardLayout(char keys[])
  {
    this.keys = keys;
    keyType = new KeyType[keys.length];
    keyCode = new int[keys.length];
    slideCharIndex = new int[keys.length];
    Arrays.fill(slideCharIndex, -1);
    for (int i = 0; i < keys.length; i++)
    {
      char key = keys[i];
      char lowerCase = Character.toLowerCase(key);
      if (key < 32)
        keyType[i] = KeyType.CONTROL;
      else if (Character.isDigit(key))
        keyType[i] = KeyType.NUMBER;
      else if (lowerCase == 'a' || lowerCase == 'e' || lowerCase == 'i' || lowerCase == 'o' || lowerCase == 'u')
        keyType[i] = KeyType.VOWEL;
      else if (Character.isLetter(key))
        keyType[i] = KeyType.CONSONANT;
      else
        keyType[i] = KeyType.PUNCTUATION;
      if (key == '\'')
        slideCharIndex[i] = 26;
      else if (lowerCase >= 'a' && lowerCase <= 'z')
        slideCharIndex[i] = lowerCase-'a';
    }
  }
}
