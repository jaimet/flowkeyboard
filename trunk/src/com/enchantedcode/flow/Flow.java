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

import java.util.*;

public class Flow
{
  public static final char ENTER = 0;
  public static final char DELETE = 1;
  public static final char SHIFT = 2;
  public static final char ALT = 3;
  public static final char VOICE = 4;
  public static final char keys[] =
      {'?', 'x', 'w', 'v', 'y', 'b', DELETE,
       ',', 't', 'h', 'e', 'r', 'm', ' ',
       '.', 'c', 'a', 'i', 'o', 'l', 'p',
       ALT, 'k', 's', 'n', 'u', 'd', 'j',
       SHIFT, 'z', '\'', 'g', 'f', 'q', ENTER};
  public static final char shiftKeys[] =
      {'?', 'X', 'W', 'V', 'Y', 'B', DELETE,
       ',', 'T', 'H', 'E', 'R', 'M', ' ',
       '.', 'C', 'A', 'I', 'O', 'L', 'P',
       ALT, 'K', 'S', 'N', 'U', 'D', 'J',
       SHIFT, 'Z', '\'', 'G', 'F', 'Q', ENTER};
  public static final char altKeys[] =
      {'!', '@', '(', ')', '%', '^', DELETE,
       ',', ':', '1', '2', '3', '/', '$',
       '.', ';', '4', '5', '6', '+', '&',
       ALT, '~', '7', '8', '9', '-', VOICE,
       SHIFT, '=', '"', '0', '#', '*', ENTER};
  public static final KeyboardLayout baseKeyboard = new KeyboardLayout(keys);
  public static final KeyboardLayout shiftKeyboard = new KeyboardLayout(shiftKeys);
  public static final KeyboardLayout altKeyboard = new KeyboardLayout(altKeys);
  public static final KeyboardLayout altShiftKeyboard = new KeyboardLayout(altKeys);
  public static final HashMap<Character, String[]> alternates = new HashMap<Character, String[]>();

  static
  {
    alternates.put('a', new String[] {"á", "à", "ä", "â", "å", "æ"});
    alternates.put('A', new String[] {"Á", "À", "Ä", "Â", "Å", "Æ"});
    alternates.put('e', new String[] {"é", "è", "ë", "ê"});
    alternates.put('E', new String[] {"É", "È", "Ë", "Ê"});
    alternates.put('i', new String[] {"í", "ì", "ï", "î"});
    alternates.put('I', new String[] {"Í", "Ì", "Ï", "Î"});
    alternates.put('o', new String[] {"ó", "ò", "ö", "ô", "œ", "ø"});
    alternates.put('O', new String[] {"Ó", "Ò", "Ö", "Ô", "Œ", "Ø"});
    alternates.put('u', new String[] {"ú", "ù", "ü", "û"});
    alternates.put('U', new String[] {"Ú", "Ù", "Ü", "Û"});
    alternates.put('n', new String[] {"ñ"});
    alternates.put('N', new String[] {"Ñ"});
    alternates.put('s', new String[] {"ß", "§"});
    alternates.put('S', new String[] {"ß", "§"});
    alternates.put('c', new String[] {"ç", "©"});
    alternates.put('C', new String[] {"Ç", "©"});
    alternates.put('p', new String[] {"π", "¶"});
    alternates.put('P', new String[] {"∏", "¶"});
    alternates.put('r', new String[] {"®"});
    alternates.put('R', new String[] {"®"});
    alternates.put('t', new String[] {"™", "þ"});
    alternates.put('T', new String[] {"™", "Þ"});
    alternates.put('d', new String[] {"ð"});
    alternates.put('D', new String[] {"Ð"});
    alternates.put('$', new String[] {"€", "£", "¢", "¥"});
    alternates.put('+', new String[] {"±"});
    alternates.put('-', new String[] {"–", "_"});
    alternates.put('*', new String[] {"°", "‡", "†"});
    alternates.put('/', new String[] {"\\", "|", "÷"});
    alternates.put('(', new String[] {"<", "[", "{", "≤", "«", " :-("});
    alternates.put(')', new String[] {">", "]", "}", "≥", "»", " :-)"});
    alternates.put('!', new String[] {"¡"});
    alternates.put('?', new String[] {"¿"});
    alternates.put('=', new String[] {"≠", "≈"});
    alternates.put('.', new String[] {"…"});
    alternates.put(';', new String[] {" ;-)"});
  }

}
