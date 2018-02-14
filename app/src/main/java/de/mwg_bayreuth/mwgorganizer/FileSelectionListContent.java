package de.mwg_bayreuth.mwgorganizer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.mwg_bayreuth.mwgorganizer.DisplayPDF;

/**
 * Helper class for providing sample content for user interfaces created by
 * Android template wizards.
 */
public class FileSelectionListContent {

    public List<Item> ITEMS = new ArrayList<Item>();

    public FileSelectionListContent(){
    }

    public  void addItem(Item item) {
        ITEMS.add(item);
    }

    public  void removeItem(Item item)
    {
        ITEMS.remove(item);
    }

    public  void removeItem(int position)
    {
        ITEMS.remove(position);
    }

    public String[][] openPDF()
    {
        String[][] pdffiles = new String[ITEMS.size()][2];
        for(int i = 0; i < ITEMS.size(); i++)
        {
            pdffiles[i][0] = ITEMS.get(i).content;
            pdffiles[i][1] = ITEMS.get(i).filepath;
        }
        return pdffiles;
    }

    /**
     * A dummy item representing a piece of content.
     */
    public static class Item {
        public final String id;
        public final String content;
        public final String filepath;
        public boolean updated;

        public Item(String id, String content, String filename, boolean upToDate) {
            this.id = id;
            this.content = content;
            this.filepath = filename;
            this.updated = upToDate;
        }

        @Override
        public String toString() {
            return content;
        }
    }
}
