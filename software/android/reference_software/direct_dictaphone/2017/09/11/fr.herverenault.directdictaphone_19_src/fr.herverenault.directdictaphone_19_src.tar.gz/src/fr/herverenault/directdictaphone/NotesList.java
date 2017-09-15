package fr.herverenault.directdictaphone;

import java.io.File;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class NotesList extends ListActivity {

    private static final int DELETE_ID = 0;
    private static final int SEND_ID = 1;
    private static final int MARK_UNREAD_ID = 2;
    private static final int DELETE_ALL_ID = 3;

    private SharedPreferences preferences;

    private String[] dateList;

    private int currentPosition = 0;
    private int currentTopPosition = 0;

    private boolean confirmDelete;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.list);

        registerForContextMenu(getListView());

        preferences = PreferenceManager.getDefaultSharedPreferences(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        currentPosition = getListView().getFirstVisiblePosition();
        View v = getListView().getChildAt(0);
        currentTopPosition = (v == null) ? 0 : v.getTop();
    }

    @Override
    protected void onStart() {
        super.onStart();

        String[] noteList = NotesFolder.getNoteList();

        TextView notesHeader = (TextView) findViewById(R.id.head_note_list);
        if (noteList.length > 1) {
            notesHeader.setText(String.valueOf(noteList.length) + " " + getString(R.string.notes));
        } else if (noteList.length == 1) {
            notesHeader.setText("1 " + getString(R.string.note));
        } else {
            notesHeader.setVisibility(View.INVISIBLE);
        }

        dateList = new String[noteList.length];

        for (int i = 0; i < noteList.length; i++) {
            dateList[i] = formatName(noteList[i]);

            if (!preferences.getBoolean(noteList[i], false)) {
                dateList[i] = dateList[i] + "  ★";
            }
        }

        setListAdapter(new ArrayAdapter<String>(this, R.layout.note, R.id.note_text, dateList));

        ListView lv = getListView();
        lv.setSelectionFromTop(currentPosition, currentTopPosition);
    }

    @Override
    protected void onResume() {
        super.onResume();
        confirmDelete = preferences.getBoolean("prefs_confirm_delete", false);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {

        String[] noteList = NotesFolder.getNoteList();
        ViewGroup vg = (ViewGroup) v;
        TextView clickedItemText = (TextView) vg.getChildAt(1);

        // mark item as read
        String s = clickedItemText.getText().toString();
        if (s.endsWith("★")) {
            // mark item as read in list
            dateList[position] = s.substring(0, s.length() - 3); // remove star
            clickedItemText.setText(dateList[position]); // refresh listView
            // mark item as read in prefs
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean(noteList[position], true);
            editor.commit();
        }

        // "view" (listen to) item
        File file = getFileFromNote(noteList[position]);
        Intent intent = new Intent();
        intent.setAction(android.content.Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(file), "audio/*");
        startActivity(intent);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        menu.add(0, DELETE_ID, 0, R.string.context_menu_delete);
        menu.add(0, SEND_ID, 0, R.string.context_menu_send);
        menu.add(0, MARK_UNREAD_ID, 0, R.string.context_menu_mark_unread);
        menu.add(0, DELETE_ALL_ID, 0, R.string.context_menu_delete_all);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        String[] noteList = NotesFolder.getNoteList();
        // must be final for anonymous DialogInterface.OnClickListener()
        final String note = noteList[info.position];

        switch (item.getItemId()) {
            case DELETE_ID:
                if (confirmDelete) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setMessage(R.string.confirm_delete)
                            .setCancelable(false)
                            .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    NotesList.this.doDelete(note);
                                }
                            })
                            .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                }
                            });
                    AlertDialog alert = builder.create();
                    alert.show();
                } else {
                    doDelete(note);
                }
                break;
            case SEND_ID:
                Intent intent = new Intent();
                intent.setAction(android.content.Intent.ACTION_SEND);
                intent.setType("audio/*");
                intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(getFileFromNote(note)));
                startActivity(Intent.createChooser(intent, getString(R.string.send_with)));
                break;
            case MARK_UNREAD_ID:
                if (! dateList[info.position].endsWith("★")) {
                    dateList[info.position] = dateList[info.position] + "  ★";
                    ViewGroup vg = (ViewGroup) info.targetView;
                    TextView clickedItemText = (TextView) vg.getChildAt(1);
                    // refresh listView
                    clickedItemText.setText(dateList[info.position]);
                    // unmark item as read in prefs
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.remove(note);
                    editor.commit();
                }
                break;
            case DELETE_ALL_ID:
                deleteAll();
        }

        return super.onContextItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.list_menu, menu);
        return true;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        Intent i;
        switch (item.getItemId()) {
            case R.id.menu_delete_all:
                deleteAll();
                break;
        }
        return super.onMenuItemSelected(featureId, item);
    }

    private void doDelete(String note) {
        getFileFromNote(note).delete();
        // unmark item as read in prefs
        SharedPreferences.Editor editor = preferences.edit();
        editor.remove(note);
        editor.commit();
        Toast.makeText(this, getString(R.string.deleted) + " " + formatName(note), Toast.LENGTH_SHORT).show();
        onStart();
    }

    private void deleteAll() {
        if (confirmDelete) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.confirm_delete)
                    .setCancelable(false)
                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            NotesList.this.doDeleteAll();
                        }
                    })
                    .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    });
            AlertDialog alert = builder.create();
            alert.show();
        } else {
            doDeleteAll();
        }
    }

    private void doDeleteAll() {
        String[] noteList = NotesFolder.getNoteList();
        // unmark items as read in prefs
        SharedPreferences.Editor editor = preferences.edit();
        for (String note : noteList) {
            getFileFromNote(note).delete();
            editor.remove(note);
        }
        editor.commit();
        Toast.makeText(this, getString(R.string.deleted), Toast.LENGTH_SHORT).show();
        onStart();
    }

    private String formatName(String name) {
        return name.substring(25, 27)
                + "/" + name.substring(22, 24)
                + "/" + name.substring(17, 21)
                + " " + name.substring(28, 30)
                + ":" + name.substring(31, 33)
                + ":" + name.substring(34, 36);
    }

    private File getFileFromNote(String note) {
        return new File(Environment.getExternalStorageDirectory().getAbsolutePath()
                + "/" + NotesFolder.PATH + "/" + note);
    }
}
