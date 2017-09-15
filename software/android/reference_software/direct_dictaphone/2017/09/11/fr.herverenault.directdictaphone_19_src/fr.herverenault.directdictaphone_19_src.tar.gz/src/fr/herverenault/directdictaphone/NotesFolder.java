package fr.herverenault.directdictaphone;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.Collections;

import android.os.Environment;

public final class NotesFolder {

    public static String PATH = "DirectDictaphone";

    public static String[] getNoteList() {
        File sdCard = Environment.getExternalStorageDirectory();
        File folder = new File(sdCard.getAbsolutePath() + "/" + PATH);
        if (!folder.exists()) {
            return new String[0];
        }
        String[] noteList = folder.list(new FilenameFilter() {
            public boolean accept(File dir, String filename) {
                return filename.matches("^" + DirectDictaphone.FILE_PREFIX + "_\\d{4}-\\d{2}-\\d{2}_\\d{2}-\\d{2}-\\d{2}\\.wav$");
            }
        });
        Arrays.sort(noteList, Collections.reverseOrder());
        return noteList;
    }

    public static int numberNotes() {
        return getNoteList().length;
    }
}
