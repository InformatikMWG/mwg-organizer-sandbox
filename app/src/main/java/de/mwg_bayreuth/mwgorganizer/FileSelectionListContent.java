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

    private static String makeDetails(int position) {
        StringBuilder builder = new StringBuilder();
        builder.append("Details about Item: ").append(position);
        for (int i = 0; i < position; i++) {
            builder.append("\nMore details information here.");
        }
        return builder.toString();
    }

    /**
     * A dummy item representing a piece of content.
     */
    public static class Item {
        public final String id;
        public final String content;
        public final String filepath;
        public boolean updated;

        public Item(String id, String content, String details, boolean upToDate) {
            this.id = id;
            this.content = content;
            this.filepath = details;
            this.updated = upToDate;
        }

        public void openPDF()
        {

            //TODO: open PDFView with filepath
        }
        @Override
        public String toString() {
            return content;
        }
    }
}