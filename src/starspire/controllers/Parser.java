package starspire.controllers;

import java.io.*;
import java.util.*;

/**
 * Parser is what we use to handle all the extraction of entities from raw input (strings).
 *
 * @author Patrick Fiaux, Alex Endert
 */
public class Parser {

    private ArrayList<String> stopwords;
    private ArrayList<Character> punct;
    private final int WORD_MIN_LENGTH = 6;

    /**
     * Default Constructor
     */
    public Parser() {
        setup();
    }

    /**
     * Helper for the construction of the Parser
     */
    private void setup() {
        /*
         * Set up the stopWords from the local file
         */
        File f;
        String word;
        stopwords = new ArrayList<String>(Arrays.asList(ParserStopWords.stopWords));

        /*
         * Set up the punctuation list
         */
        punct = new ArrayList<Character>();
        punct.add(',');
        punct.add('.');
        punct.add(':');
        punct.add(';');
        punct.add('?');
        punct.add('!');
        punct.add(')');
        punct.add('>');
        punct.add(']');
        punct.add('[');
        punct.add('{');
        punct.add('(');
        punct.add('<');
        punct.add('}');
        punct.add('*');
        punct.add('"');
        punct.add('`');
        punct.add('\'');
    }

    /**
     * Check if the string ends with a punctuation.
     * If yes, remove it, and return the string without the punctuation.
     * It no, return the string back.
     * @param input The string that is being checked.
     * @return String The string without the punctuation at the end.
     */
    public String removeStartEndPunctuation(String input) {
        StringBuilder sb = new StringBuilder();
        String returnString;


        returnString = input;
        // remove ending punctuation
        if (returnString.length() > 1) {
            while (returnString.length() > 1 && punct.contains(returnString.charAt(returnString.length() - 1))) {
                //last character in the string is not a letter, remove it
                returnString = returnString.substring(0, returnString.length() - 1);
            }
            // remove starting punctuation
            while (returnString.length() > 1 && punct.contains(returnString.charAt(0))) {
                returnString = returnString.substring(1, returnString.length());
            }
        }
        return returnString;
    }

    /**
     * Checks to see if the word is a stop word.
     * @param word the string to check
     * @return true if a stop word, false if not
     */
    public boolean isStopWord(String word) {
        boolean isStopWord = false;
        for (String s : stopwords) {
            if (s.equalsIgnoreCase(word)) {
                isStopWord = true;
            } else if (word.equalsIgnoreCase(s + "s")) { //check for plurals
                isStopWord = true;
            }
        }

        return isStopWord;
    }

    /**
     * Parses a string
     * @param toParse
     * @return returns a list of tokens
     */
    public ArrayList<String> parseString(String toParse) {
        StringTokenizer st = new StringTokenizer(toParse);
        ArrayList<String> list = new ArrayList<String>();

        while (st.hasMoreTokens()) {
            String current = st.nextToken();
            if (!this.isStopWord(current)) {
                //this is an entity, do something with it
                //check for punctuation at the start and end
                current = removeStartEndPunctuation(current);
                //add the entity (String) to the list
                list.add(current);
            }
        }
        return list;
    }
}
