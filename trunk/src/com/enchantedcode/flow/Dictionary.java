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

import android.content.*;
import android.database.*;
import android.provider.*;
import android.util.*;

import java.io.*;
import java.util.*;

public class Dictionary
{
  final char words[][];
  final char wordTraces[][];
  final short wordFrequency[];
  final Prefix prefixes[];
  final int shortWords[];
  final char mediumPrefixes[][];
  final int mediumPrefixIndex[];
  final char longPrefixes[][];
  final int longPrefixIndex[];
  final HashMap<Character, Character> replacements = new HashMap<Character, Character>();

  private static final float PREFIX_CUTOFF = 3.0f;
  private static final float MEDIUM_PREFIX_CUTOFF = 3.5f;
  private static final float LONG_PREFIX_CUTOFF = 5.0f;
  private static final float MISSING_TERMINAL_COST = 0.5f;
  private static final float MISSING_VIA_LETTER = 1.0f;
  private static final float VIA_DISTANCE_MULTIPLIER = 0.2f;
  private static final float UNORDERED_VIA_COST = 0.5f;

  private static final int SHORT_PREFIX_LENGTH = 5;
  private static final int MEDIUM_PREFIX_LENGTH = 7;
  private static final int LONG_PREFIX_LENGTH = 9;

  private static final Comparator<char[]> traceComparator = new Comparator<char[]>()
  {
    public int compare(char[] a, char[] b)
    {
      int len = Math.min(a.length, b.length);
      for (int i = 0; i < len; i++)
      {
        if (a[i] != b[i])
          return (a[i] < b[i] ? -1 : 1);
      }
      if (a.length < b.length)
        return -1;
      if (a.length == b.length)
        return 0;
      return 1;
    }
  };

  public Dictionary(Context context, String dictionary)
  {
    // Load the dictionary.

    ArrayList<SortedWord> wordList = new ArrayList<SortedWord>();
    for (HashMap.Entry<Character, String[]> entry : Flow.alternates.entrySet())
      for (String c : entry.getValue())
        if (c.length() == 1)
          replacements.put(c.charAt(0), entry.getKey());
    try
    {
      int id = (dictionary.equals("american") ? R.raw.american : R.raw.british);
      DataInputStream in = new DataInputStream(context.getResources().openRawResource(id));
      try
      {
        while (true)
        {
          int length = in.readByte();
          char word[] = new char[length];
          for (int i = 0; i < length; i++)
            word[i] = in.readChar();
          wordList.add(new SortedWord(word, (short) (in.readByte()&255)));
        }
      }
      catch (EOFException ex)
      {
        // End of file reached
      }
    }
    catch (IOException ex)
    {
      Log.d("Flow", "Exception loading dictionary", ex);
      words = new char[0][];
      wordTraces = new char[0][];
      wordFrequency = new short[0];
      prefixes = new Prefix[0];
      shortWords = new int[0];
      mediumPrefixes = new char[0][];
      mediumPrefixIndex = new int[0];
      longPrefixes = new char[0][];
      longPrefixIndex = new int[0];
      return;
    }

    // Add words from the user dictionary.

    Cursor cursor = context.getContentResolver().query(UserDictionary.Words.CONTENT_URI, new String[] {UserDictionary.Words.WORD, UserDictionary.Words.FREQUENCY}, null, null, null);
    if (cursor.moveToFirst())
    {
      do
      {
        String word = cursor.getString(0);
        int frequency = cursor.getInt(1);
        if (frequency < 0)
          frequency = 0;
        if (frequency > 255)
          frequency = 255;
        wordList.add(new SortedWord(word.toCharArray(), (short) frequency));
      } while (cursor.moveToNext());
    }
    cursor.close();

    // Build the data structures.
    
    Collections.sort(wordList);
    words = new char[wordList.size()][];
    wordTraces = new char[wordList.size()][];
    wordFrequency = new short[wordList.size()];
    for (int i = 0; i < words.length; i++)
    {
      char word[] = wordList.get(i).word;
      words[i] = word;
      wordFrequency[i] = wordList.get(i).frequency;
      int length = word.length;
      int j;
      for (j = 0; j < length; j++)
      {
        char c = word[j];
        if (c != '\'' && !(c >= 'a' && c <= 'z'))
          break;
      }
      if (j != length)
      {
        if (word[length-1] == '.')
          length--;
        char trace[] = new char[length];
        for (j = 0; j < length; j++)
        {
          char c = Character.toLowerCase(word[j]);
          if (c != '\'' && !(c >= 'a' && c <= 'z'))
          {
            if (replacements.containsKey(c))
              c = replacements.get(c);
            else
              c = '\'';
          }
          trace[j] = c;
        }
        wordTraces[i] = trace;
      }
      else
        wordTraces[i] = word;
    }
    Prefix currentPrefix = null;
    ArrayList<Prefix> prefixList = new ArrayList<Prefix>();
    ArrayList<Integer> shortWordList = new ArrayList<Integer>();
    for (int i = 0; i < wordTraces.length; i++)
    {
      char word[] = wordTraces[i];
      if (word.length >= SHORT_PREFIX_LENGTH)
      {
        if (currentPrefix == null || !currentPrefix.hasPrefix(word))
          prefixList.add(currentPrefix = new Prefix(substring(word, SHORT_PREFIX_LENGTH), i));
        currentPrefix.end++;
      }
      else
        shortWordList.add(i);
    }
    prefixes = prefixList.toArray(new Prefix[prefixList.size()]);
    shortWords = new int[shortWordList.size()];
    for (int i = 0; i < shortWords.length; i++)
      shortWords[i] = shortWordList.get(i);
    ArrayList<char[]> mediumPrefixList = new ArrayList<char[]>();
    mediumPrefixIndex = new int[wordTraces.length];
    findPrefixes(MEDIUM_PREFIX_LENGTH, wordTraces, mediumPrefixList, mediumPrefixIndex);
    mediumPrefixes = new char[mediumPrefixList.size()][];
    for (int i = 0; i < mediumPrefixes.length; i++)
      mediumPrefixes[i] = mediumPrefixList.get(i);
    ArrayList<char[]> longPrefixList = new ArrayList<char[]>();
    longPrefixIndex = new int[wordTraces.length];
    findPrefixes(LONG_PREFIX_LENGTH, wordTraces, longPrefixList, longPrefixIndex);
    longPrefixes = new char[longPrefixList.size()][];
    for (int i = 0; i < longPrefixes.length; i++)
      longPrefixes[i] = longPrefixList.get(i);
  }

  private static void findPrefixes(int size, char words[][], ArrayList<char[]> prefixes, int prefixIndex[])
  {
    char lastPrefix[] = null;
    int numWords = words.length;
    for (int i = 0; i < numWords; i++)
    {
      char word[] = words[i];
      if (word.length >= size)
      {
        if (lastPrefix == null || !startsWith(word, lastPrefix))
        {
          char prefix[] = substring(word, size);
          if (i < numWords-1 && startsWith(words[i+1], prefix))
          {
            lastPrefix = prefix;
            prefixes.add(prefix);
          }
          else
            lastPrefix = null;
        }
      }
      else
        lastPrefix = null;
      prefixIndex[i] = (lastPrefix == null ? -1 : prefixes.size()-1);
    }
  }

  private static char[] substring(char word[], int length)
  {
    char s[] = new char[length];
    for (int i = 0; i < length; i++)
      s[i] = word[i];
    return s;
  }

  private static boolean startsWith(char word[], char prefix[])
  {
    int length = prefix.length;
    if (word.length < length)
      return false;
    for (int i = 0; i < length; i++)
    {
      if (word[i] != prefix[i])
        return false;
    }
    return true;
  }

  public String[] guessWord(TracePoint trace[], KeyboardView.ModifierMode shiftMode)
  {
    int bestWords[] = new int[5];
    Arrays.fill(bestWords, -1);
    float bestScores[] = new float[5];
    float sumWeights = 0.0f;
    for (int i = 0; i < trace.length; i++)
      sumWeights += trace[i].weight;
    for (int i = 0; i < bestScores.length; i++)
      bestScores[i] = sumWeights;
    ArrayList<SortedPrefix> candidatePrefixes = new ArrayList<SortedPrefix>();
    float prefixCutoff = PREFIX_CUTOFF;
    for (Prefix prefix : prefixes)
    {
      if (trace[0].getKeyDistance(prefix.prefix[0]) > 0.7f)
        continue;
      float score = scorePrefix(prefix.prefix, trace, prefixCutoff);
      if (score < prefixCutoff)
      {
        candidatePrefixes.add(new SortedPrefix(prefix, score));
        if (score < prefixCutoff-2.0f)
          prefixCutoff = score+2.0f;
      }
    }
    for (int i = candidatePrefixes.size()-1; i >= 0; i--)
      if (candidatePrefixes.get(i).score > prefixCutoff)
        candidatePrefixes.remove(i);
    Collections.sort(candidatePrefixes);
    int minLength = trace.length/2;
    int maxLength = (int) (trace.length*1.26f)+3
        ;
    if (minLength < SHORT_PREFIX_LENGTH)
    {
      for (int wordIndex : shortWords)
      {
        char word[] = wordTraces[wordIndex];
        if (word.length < minLength)
          continue;
        if (trace[0].getKeyDistance(word[0]) > 0.7f)
          continue;
        float score = scoreWord(word, trace, bestScores[4]);
        if (score >= bestScores[4])
          continue;
        for (int i = 0; i < 5; i++)
          if (score < bestScores[i])
          {
            for (int j = 4; j > i; j--)
            {
              bestWords[j] = bestWords[j-1];
              bestScores[j] = bestScores[j-1];
            }
            bestWords[i] = wordIndex;
            bestScores[i] = score;
            break;
          }
      }
    }
    int lastMediumPrefix = -1;
    int lastLongPrefix = -1;
    boolean skipMediumPrefix = false;
    boolean skipLongPrefix = false;
    for (SortedPrefix prefix : candidatePrefixes)
    {
      Prefix p = prefix.prefix;
      for (int k = p.start; k < p.end; k++)
      {
        char word[] = wordTraces[k];
        if (word.length < minLength || word.length > maxLength)
          continue;
        int mediumPrefix = mediumPrefixIndex[k];
        if (mediumPrefix == lastMediumPrefix)
        {
          if (skipMediumPrefix)
            continue;
        }
        else
        {
          lastMediumPrefix = mediumPrefix;
          skipMediumPrefix = false;
          if (mediumPrefix > -1)
          {
            float cutoff = (bestScores[4] < MEDIUM_PREFIX_CUTOFF ? bestScores[4] :  MEDIUM_PREFIX_CUTOFF);
            float score = scorePrefix(mediumPrefixes[mediumPrefix], trace, cutoff);
            if (score > cutoff)
            {
              skipMediumPrefix = true;
              continue;
            }
          }
        }
        int longPrefix = longPrefixIndex[k];
        if (longPrefix == lastLongPrefix)
        {
          if (skipLongPrefix)
            continue;
        }
        else
        {
          lastLongPrefix = longPrefix;
          skipLongPrefix = false;
          if (longPrefix > -1)
          {
            float cutoff = (bestScores[4] < LONG_PREFIX_CUTOFF ? bestScores[4] :  LONG_PREFIX_CUTOFF);
            float score = scorePrefix(longPrefixes[longPrefix], trace, cutoff);
            if (score > cutoff)
            {
              skipLongPrefix = true;
              continue;
            }
          }
        }
        float score = scoreWord(word, trace, bestScores[4]);
        if (score >= bestScores[4])
          continue;
        for (int i = 0; i < 5; i++)
          if (score < bestScores[i])
          {
            for (int j = 4; j > i; j--)
            {
              bestWords[j] = bestWords[j-1];
              bestScores[j] = bestScores[j-1];
            }
            bestWords[i] = k;
            bestScores[i] = score;
            break;
          }
      }
    }
    float adjustedScores[] = new float[bestScores.length];
    for (int i = 0; i < bestScores.length; i++)
    {
      if (bestWords[i] > -1)
        adjustedScores[i] = bestScores[i]-0.0015f*wordFrequency[bestWords[i]];
      else
        adjustedScores[i] = Float.MAX_VALUE;
    }
    for (int i = 1; i < bestWords.length; i++)
      for (int j = i-1; j >= 0 && adjustedScores[j] > adjustedScores[j+1]; j--)
      {
        int swapWord = bestWords[j];
        float swapScore = bestScores[j];
        float swapAdjusted = adjustedScores[j];
        bestWords[j] = bestWords[j+1];
        bestScores[j] = bestScores[j+1];
        adjustedScores[j] = adjustedScores[j+1];
        bestWords[j+1] = swapWord;
        bestScores[j+1] = swapScore;
        adjustedScores[j+1] = swapAdjusted;
      }
    String choices[] = new String[bestWords.length];
    for (int i = 0; i < bestWords.length; i++)
      if (bestWords[i] != -1)
      {
        char word[] = words[bestWords[i]];
        if (shiftMode == KeyboardView.ModifierMode.DOWN)
        {
          word = word.clone();
          word[0] = Character.toUpperCase(word[0]);
        }
        else if (shiftMode == KeyboardView.ModifierMode.LOCKED)
        {
          word = word.clone();
          for (int j = 0; j < word.length; j++)
            word[j] = Character.toUpperCase(word[j]);
        }
        choices[i] = new String(word);
      }
    return choices;
  }

  private static float scorePrefix(char word[], TracePoint trace[], float cutoff)
  {
    int minTrace = (word.length-1)/2;
    int maxTrace = Math.min(trace.length, word.length*2);
    TracePoint first = trace[0];
    cutoff -= first.getKeyDistance(word[0])*first.weight;
    float bestScore = cutoff+0.01f;
    for (int i = minTrace; i < maxTrace; i++)
    {
      float score = scorePrefix(word, trace, 0, word.length-1, 0, i, word.length-1, cutoff);
      if (score < bestScore)
      {
        bestScore = score;
        cutoff = bestScore;
      }
    }
    return first.getKeyDistance(word[0])*first.weight+bestScore;
  }

  private static float scorePrefix(char word[], TracePoint trace[], int wordStart, int wordEnd, int traceStart, int traceEnd, int lastCharacterToScore, float cutoff)
  {
    int length = traceEnd-traceStart;
    if (length < 2)
      return scorePrefixSegment(word, trace, wordStart, wordEnd, traceEnd, lastCharacterToScore);
    float bestScore = cutoff+0.01f;
    int mid = traceStart+length/2;
    int min = length/4;
    for (int i = wordStart+min; i <= wordEnd-min; i++)
    {
      float score = scorePrefix(word, trace, wordStart, i, traceStart, mid, lastCharacterToScore, cutoff);
      if (score < cutoff)
      {
        score += scorePrefix(word, trace, i, wordEnd, mid, traceEnd, lastCharacterToScore, cutoff-score);
        if (score < bestScore)
        {
          bestScore = score;
          cutoff = bestScore;
        }
      }
    }
    return bestScore;
  }

  private static float scoreWord(char word[], TracePoint trace[], float cutoff)
  {
    TracePoint first = trace[0];
    cutoff -= first.getKeyDistance(word[0])*first.weight;
    return first.getKeyDistance(word[0])*first.weight+scoreWord(word, trace, 0, word.length-1, 0, trace.length-1, cutoff);
  }

  private static float scoreWord(char word[], TracePoint trace[], int wordStart, int wordEnd, int traceStart, int traceEnd, float cutoff)
  {
    int length = traceEnd-traceStart;
    if (length < 2)
      return scoreSegment(word, trace, wordStart, wordEnd, traceEnd);
    float bestScore = cutoff+0.01f;
    int mid = traceStart+length/2;
    int min = length/4;
    for (int i = wordStart+min; i <= wordEnd-min; i++)
    {
      float score = scoreWord(word, trace, wordStart, i, traceStart, mid, cutoff);
      if (score < cutoff)
      {
        score += scoreWord(word, trace, i, wordEnd, mid, traceEnd, cutoff - score);
        if (score < bestScore)
        {
          bestScore = score;
          cutoff = bestScore;
        }
      }
    }
    return bestScore;
  }

  private static float scorePrefixSegment(char word[], TracePoint trace[], int wordStart, int wordEnd, int traceEnd, int lastCharacterToScore)
  {
    float score = 0.0f;
    char cprev = word[wordStart];
    char c = word[wordEnd];
    int end = Math.min(wordEnd, lastCharacterToScore);
    if (traceEnd == 0)
      score += end;
    else
    {
      TracePoint point = trace[traceEnd-1];
      int lastIndex = -1;
      for (int currentChar = wordStart+1; currentChar < end; currentChar++)
      {
        char cvia = word[currentChar];
        int index = point.getViaKeyIndex(cvia);
        if (cvia != c && cvia != cprev)
        {
          if (index == -1)
          {
            score += MISSING_VIA_LETTER;
            continue;
          }
          score += VIA_DISTANCE_MULTIPLIER*point.viaKeys[index].nearestDistance;
          if (index < lastIndex)
            score += UNORDERED_VIA_COST;
        }
        lastIndex = index;
      }
    }
    if (wordEnd <= lastCharacterToScore)
    {
      TracePoint point = trace[traceEnd];
      score += point.getKeyDistance(c)*point.weight;
    }
    return score;
  }

  private static float scoreSegment(char word[], TracePoint trace[], int wordStart, int wordEnd, int traceEnd)
  {
    float score = 0.0f;
    char cprev = word[wordStart];
    char c = word[wordEnd];
    if (traceEnd == 0)
      score += wordEnd;
    else
    {
      TracePoint point = trace[traceEnd-1];
      int lastIndex = -1;
      for (int currentChar = wordStart+1; currentChar < wordEnd; currentChar++)
      {
        char cvia = word[currentChar];
        int index = point.getViaKeyIndex(cvia);
        if (cvia != c && cvia != cprev)
        {
          if (index == -1)
          {
            score += MISSING_VIA_LETTER;
            continue;
          }
          score += VIA_DISTANCE_MULTIPLIER*point.viaKeys[index].nearestDistance;
          if (index < lastIndex)
            score += UNORDERED_VIA_COST;
        }
        lastIndex = index;
      }
    }
    TracePoint point = trace[traceEnd];
    score += point.getKeyDistance(c)*point.weight;
    return score;
  }

  public String[] findWordsStartingWith(String prefix)
  {
    // Find the trace to use.

    char trace[] = prefix.toCharArray();
    for (int i = 0; i < trace.length; i++)
    {
      char c = Character.toLowerCase(trace[i]);
      if (c != '\'' && !(c >= 'a' && c <= 'z') && replacements.containsKey(c))
        c = replacements.get(c);
      trace[i] = c;
    }

    // Find the first word starting with this trace.

    int start = Arrays.binarySearch(wordTraces, trace, traceComparator);
    if (start < 0)
      start = -(start+1);

    // Find the last word starting with this trace.

    int end = start;
    for (; end < wordTraces.length && startsWith(wordTraces[end], trace); end++)
      ;

    // Find the most common words starting with this trace.

    int indices[] = new int[10];
    int last = indices.length-1;
    Arrays.fill(indices, -1);
    for (int i = start; i < end; i++)
    {
      if (indices[last] == -1 || wordFrequency[i] > wordFrequency[indices[last]])
      {
        // Add this word to the list.

        int insert = 0;
        while (indices[insert] > -1 && wordFrequency[i] <= wordFrequency[indices[insert]])
          insert++;
        for (int j = indices.length-1; j > insert; j--)
          indices[j] = indices[j-1];
        indices[insert] = i;
      }
    }
    String choices[] = new String[indices.length];
    for (int i = 0; i < choices.length; i++)
      if (indices[i] > -1)
        choices[i] = new String(words[indices[i]]);
    return choices;
  }

  private static class Prefix
  {
    public final char prefix[];
    public int start, end;

    public Prefix(char prefix[], int start)
    {
      this.prefix = prefix;
      this.start = start;
      end = start;
    }

    public boolean hasPrefix(char word[])
    {
      if (word.length < prefix.length)
        return false;
      for (int i = 0; i < prefix.length; i++)
        if (word[i] != prefix[i])
          return false;
      return true;
    }
  }

  private static class SortedWord implements Comparable<SortedWord>
  {
    public final char word[];
    public final short frequency;

    public SortedWord(char word[], short frequency)
    {
      this.word = word;
      this.frequency = frequency;
    }

    public int compareTo(SortedWord other)
    {
      char word1[] = word;
      char word2[] = other.word;
      int len = Math.min(word1.length, word2.length);
      for (int i = 0; i < len; i++)
      {
        char c1 = Character.toLowerCase(word1[i]);
        char c2 = Character.toLowerCase(word2[i]);
        if (c1 < c2)
          return -1;
        if (c2 < c1)
          return 1;
      }
      if (word1.length < word2.length)
        return -1;
      if (word2.length < word1.length)
        return 1;
      return 0;
    }
  }

  private static class SortedPrefix implements Comparable<SortedPrefix>
  {
    public final Prefix prefix;
    public final float score;

    public SortedPrefix(Prefix prefix, float score)
    {
      this.prefix = prefix;
      this.score = score;
    }

    public int compareTo(SortedPrefix other)
    {
      return Float.compare(score, other.score);
    }
  }
}