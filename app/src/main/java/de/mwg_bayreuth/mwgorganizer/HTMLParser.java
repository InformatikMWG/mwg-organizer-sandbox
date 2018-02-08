package de.mwg_bayreuth.mwgorganizer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;




class HTMLParser {
    private String path;
    private String filename;
    private String size;
    private String label;


    HTMLParser(String input) {
        path = getPathFromHTMLHref(input);
        filename = getFilenameFromPath(path);
        size = getSizeFromHTMLHref(input);
        label = getLabelFromFilename(filename);
    }


    String getPath() {
        return path;
    }
    String getFilename() {
        return filename;
    }
    String getSize() {
        return size;
    }
    String getLabel() {
        return label;
    }



    /**
     * extract the path to the PDF file from the HTML source
     *
     * @param input - The line containing the link
     * @return the file path; relative to www.mwg-bayreuth.de/
     */
    private String getPathFromHTMLHref(String input) {
        Pattern p = Pattern.compile("href=\"(.*?)\"");
        Matcher m = p.matcher(input);
        String url = null;
        if (m.find()) {
            url = m.group(1); // this variable should contain the link URL
        }
        return url;
    }

    /**
     * extracts the file name from the file path
     *
     * @param path - the file path
     * @return the file name
     */
    private String getFilenameFromPath(String path) {
        return path.substring(path.lastIndexOf('/') + 1);
    }


    /**
     * returns the part of the filename without the ".pdf" extension
     *
     * @param path - the file path
     * @return a properly refractored label
     */
    private String getLabelFromFilename(String path) {
        String cryptolabel = path.substring(0, path.indexOf("."));
        return sortlabels(cryptolabel);
    }


    /**
     * Refractors labels
     *
     * @param label - the raw label
     * @return the refractored label
     */
    private String sortlabels(String label) {
        String sortedLabel = "";

        if((label.length() == 14 && label.charAt(4) == '_' && label.charAt(7) == '_' &&
                label.charAt(10) == '_' && label.charAt(13) == '_') ||
               (label.length() == 13 && label.charAt(4) == '_' &&
                label.charAt(7) == '_' && label.charAt(10) == '_')) {
            // Label has the format YYYY_MM_DD_WW[_]
            String year = label.substring(0,4);
            String month = label.substring(5,7);
            String day = label.substring(8,10);
            String weekd = label.substring(11,13);

            sortedLabel = weekd + ", " + day + "." + month + "." + year;
        } else {
            String partsortLabel;

            if(label.length() >= 10) {
                // May contain a date; searches for dates in YYYY_MM_DD format

                partsortLabel = label;
                String translabel;

                for(int i = 9; i < label.length(); i++) {
                    if(Character.isDigit(label.charAt(i-9)) && Character.isDigit(label.charAt(i-8)) &&
                            Character.isDigit(label.charAt(i-7)) && Character.isDigit(label.charAt(i-6)) &&
                            Character.isDigit(label.charAt(i-4)) && Character.isDigit(label.charAt(i-3)) &&
                            Character.isDigit(label.charAt(i-1)) && Character.isDigit(label.charAt(i)) &&
                            label.charAt(i-5) == '_' && label.charAt(i-2) == '_') {
                        String year = label.substring(i-9,i-5);
                        String month = label.substring(i-4,i-2);
                        String day = label.substring(i-1,i+1);

                        String rangestring = "";
                        int lenwithrange = i + 3;
                        int weiterparsenab = i + 1;
                        if(lenwithrange < label.length()) {
                            if(label.charAt(i+1) == '-' && Character.isDigit(label.charAt(i+2)) &&
                                    Character.isDigit(label.charAt(i+3))) {
                                rangestring += "-";
                                rangestring += label.substring(i+2,i+4);
                                rangestring += ".";
                                weiterparsenab = i + 4;
                            }
                        }

                        translabel = partsortLabel.substring(0,i-9) + day + "." + rangestring + month + "." + year + partsortLabel.substring(weiterparsenab,partsortLabel.length());
                        partsortLabel = translabel;
                    }
                }
            } else {
                // Does NOT contain a date in YYYY_MM_DD format
                partsortLabel = label;
            }

            // Removes all underscores
            for(int i = 0; i < partsortLabel.length(); i++) {
                if(partsortLabel.charAt(i) == '_') { sortedLabel += ' '; }
                else                               { sortedLabel += partsortLabel.charAt(i); }
            }
        }

        return sortedLabel;
    }




    private int charToInt(char toconvert) {
        switch (toconvert) {
            case '1':   return 1;           case '6':   return 6;
            case '2':   return 2;           case '7':   return 7;
            case '3':   return 3;           case '8':   return 8;
            case '4':   return 4;           case '9':   return 9;
            case '5':   return 5;           default:    return 0;
        }
    }


    /**
     * Refractors labels from mensa files
     * Supports labels like KW46-17-SPEISEPLAN-MENSA or KW6-17-SPEISEPLAN-MENSA
     */
    /**String parseMensaStuff(String input) {
        int week, year;

        String checkstring = input.toLowerCase();

        // Cancel the refractoring when there is obviously no week number present
        if(!(checkstring.charAt(0) == 'k' && checkstring.charAt(1) == 'w')) {
            // String does not start  with "KW"
            return input;
        }


        // Check whether the label is continued with "speiseplan-mensa"
        // true:  determine week and year
        // false: cancel
        if(input.length() >= 24 && checkstring.substring(8,24).equals("speiseplan-mensa")) {
            // KW-46-17-SPEISEPLAN-MENSA

            if(!(Character.isDigit(input.charAt(2)) && Character.isDigit(input.charAt(3)) // Weeknr
                    && input.charAt(4) == '-' && input.charAt(7) == '-' // Week-Year-etc-separator
                    && Character.isDigit(input.charAt(5)) && Character.isDigit(input.charAt(6)))) { // Year
                // Week and year not specified
                return input;
            }

            // Calculate Week and Year from Input
            week = charToInt(input.charAt(2))*10 + charToInt(input.charAt(3));
            year = charToInt(input.charAt(5))*10 + charToInt(input.charAt(6)) + 2000;
        } else if (input.length() == 23 && checkstring.substring(7,23).equals("speiseplan-mensa")) {
            // KW6-17-SPEISEPLAN-MENSA

            if(!(Character.isDigit(input.charAt(2)) // Weeknr
                    && input.charAt(3) == '-' && input.charAt(6) == '-' // Week-Year-etc-separator
                    && Character.isDigit(input.charAt(4)) && Character.isDigit(input.charAt(5)))) { // Year
                // Week and year not specified
                return input;
            }

            // Calulate Week and Year from input
            week = charToInt(input.charAt(2));
            year = charToInt(input.charAt(4))*10 + charToInt(input.charAt(5)) + 2000;
        } else { return input; }


        // The method is still running  --> week and year have been determined


        // WARNING!!!
        // This code returns wrong data when the week spans two years
        // However, this week is during the holidays :D

        // Determine the date of monday and friday
        DateTime weekStartDate = new DateTime().withWeekOfWeekyear(week).withDayOfWeek(1).withYear(year);
        DateTime weekEndDate = new DateTime().withWeekOfWeekyear(week).withDayOfWeek(5).withYear(year);

        // Save dates as string
        DateTimeFormatter dateformat = DateTimeFormat.forPattern("dd.MM.");
        String firstdate = dateformat.print(weekStartDate);
        String lastdate = dateformat.print(weekEndDate);

        // Put the label together
        return "KW " + week + "/" + year + " (" + firstdate + "-" + lastdate + ")";
    }*/



    /**
     * extracts the PDF file's size from the HTML code
     *
     * @param input - the HTML line containing the link to the PDF file
     * @return the PDF file's size
     */
    private String getSizeFromHTMLHref(String input) {

        Pattern p = Pattern.compile("\\((.*?)\\)");
        Matcher m = p.matcher(input);
        String size = null;
        if (m.find()) {
            size = m.group(1); // this variable should contain the file size
        }
        return size;
    }
}
