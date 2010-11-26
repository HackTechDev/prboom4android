package doom.util;


import java.io.File;


import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.game.prboom.DoomActivity;
import android.game.prboom.R;
import android.net.Uri;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.Toast;

public class DialogTool 
{
	public static final String TAG = "DialogTool";
	
	public static int wadIdx = 0;
	public static boolean sound = false;
	public static boolean portrait = false;
	public static String wadName = "";
	
	/**
	 * Option dialog
	 * @param ctx
	 */
	static public void showOptionsDialog(final Context ctx, int wad, boolean s, boolean p) {
        LayoutInflater factory = LayoutInflater.from(ctx);
        final View view = factory.inflate(R.layout.options, null);
        
        wadIdx = wad;
        sound = s;
        portrait = p;
       
        // load GUI data
        setGameOptionsUI(ctx , view);
        
        AlertDialog dialog = new AlertDialog.Builder(ctx)
	        .setIcon(R.drawable.icon)
	        .setTitle("Launch Options")
	        .setView(view)
	        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
	            public void onClick(DialogInterface dialog, int whichButton) {
	        		wadIdx = ((Spinner)view.findViewById(R.id.s_files)).getSelectedItemPosition();
	        		wadName = (String)((Spinner)view.findViewById(R.id.s_files)).getSelectedItem();

	        		// Sound 1== yes?
	        		sound = ((CheckBox)view.findViewById(R.id.s_sound)).isChecked();
	        		
	        		// Size P = 320 x 320 L: 320 x 200
	        		portrait = ((CheckBox)view.findViewById(R.id.s_size)).isChecked();
	            }
	        })
	        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
	            public void onClick(DialogInterface dialog, int whichButton) {
	                dialog.dismiss();
	            }
	        })
	        .create();

        dialog.show();
	
	}
	
	/**
	 * Load spinner
	 * @param ctx
	 * @param spinner
	 * @param resID
	 * @param idx
	 */
	private static void loadSpinner(Context ctx, Spinner spinner, int resID, int idx) {
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(ctx, resID
				, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(idx);
	}

	/**
	 * Game options UI
	 * @param ctx
	 * @param v
	 */
	private static void setGameOptionsUI (Context ctx, View v) {
         //Load files spinner
//		loadSpinner(ctx, (Spinner) v.findViewById(R.id.s_files), R.array.DoomFiles, 0);
		loadSpinnerWads(ctx, (Spinner) v.findViewById(R.id.s_files), R.array.DoomFiles, 0);
		((Spinner)v.findViewById(R.id.s_files)).setSelection(wadIdx);
		((CheckBox)v.findViewById(R.id.s_sound)).setChecked(sound);		
		((CheckBox)v.findViewById(R.id.s_size)).setChecked(portrait);		
    }
	
	private static void loadSpinnerWads(Context ctx, Spinner spinner, int resID, int idx) {
		File dir = new File(DoomTools.DOOM_FOLDER);
		File files[] = dir.listFiles();
		if(files.length == 0)
			return;
		
		ArrayAdapter adapter = new ArrayAdapter(ctx, android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(adapter);
		
		for(int i = 0; i < files.length; i++) {
			if(files[i].isFile()) {
				String name = files[i].getName();
				if(!name.contains("prboom") && (name.contains(".wad") || name.contains(".WAD")))
					adapter.add(name);
			}
		}
	}
	
	/**
	 * Download game file dlg
	 * @param ctx
	 */
	static public void showDownloadDialog(final Context ctx) {
        LayoutInflater factory = LayoutInflater.from(ctx);
        final View view = factory.inflate(R.layout.download, null);
        
        setDownloadDlgUI(ctx , view);
        
        AlertDialog dialog = new AlertDialog.Builder(ctx)
	        .setIcon(R.drawable.icon)
	        .setTitle("Download a Game File")
	        .setView(view)
	        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
	            public void onClick(DialogInterface dialog, int whichButton) 
	            {
	            	// game file
	            	wadIdx = ((Spinner)view.findViewById(R.id.s_files)).getSelectedItemPosition();
	            	
	            	// force?
	            	boolean force = ((CheckBox)view.findViewById(R.id.cb_force)).isChecked();
	            	
	            	// Fetch
	            	new GameFileDownloader().downloadGameFiles(ctx, wadIdx, force);
	            }
	        })
	        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
	            public void onClick(DialogInterface dialog, int whichButton) {
	                dialog.dismiss();
	            }
	        })
	        .create();
        dialog.show();
	}
	static public void setGameOptions(int wad, boolean s, boolean p, String name) {
		wadIdx = wad;
		sound = s;
		portrait = p;
		wadName = name;
	}

	/**
	 * Download dlg data
	 * @param ctx
	 * @param v
	 */
	private static void setDownloadDlgUI (Context ctx, View v) {
        /**
         * Load files spinner
         */
		loadSpinner(ctx, (Spinner) v.findViewById(R.id.s_files), R.array.DoomFiles, 0);
    }
	
	/**
	 * Message box
	 * @param text
	 */
	
	public static  void MessageBox (Context ctx, String title, String text) {
		AlertDialog d = createAlertDialog(ctx
				, title
				, text);
			
		d.setButton("Dismiss", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                /* User clicked OK so do some stuff */
            }
        });
		d.show();
	}
	
    /**
     * Create an alert dialog
     * @param ctx App context
     * @param message Message
     * @return
     */
    static public AlertDialog createAlertDialog (Context ctx, String title, String message) {
        return new AlertDialog.Builder(ctx)
	        .setIcon(R.drawable.icon)
	        .setTitle(title)
	        .setMessage(message)
	        .create();
    }
	
    /**
     * Launch web browser
     */
    static public void launchBrowser(Context ctx, String url) {
    	ctx.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));     	
    }

	/**
	 * MessageBox
	 * @param ctx
	 * @param text
	 */
	public static void MessageBox (Context ctx, String text) {
		MessageBox(ctx, ctx.getString(R.string.app_name), text);
	}

	/**
	 * Messagebox
	 * @param ctx
	 * @param text
	 */
	public static void PostMessageBox (final Context ctx, final String text) {
		DoomActivity.mHandler.post(new Runnable() {
			public void run() {
				MessageBox(ctx, ctx.getString(R.string.app_name), text);
			}
		});
	}
	
	/**
	 * Change a view's visibility
	 * @param v
	 */
	public static void toggleView ( View v) {
		if ( v.getVisibility() == View.VISIBLE)
			v.setVisibility(View.GONE);
		else
			v.setVisibility(View.VISIBLE);
	}

    /**
     * Message Box
     * @param ctx
     * @param text
     */
    public static void Toast( final Context ctx, final String text) {
	    Toast.makeText(ctx, text, Toast.LENGTH_LONG).show();
    }

    public static void Toast( Handler handler ,final Context ctx, final String text) {
    	handler.post(new Runnable() {
			public void run() {
				Toast.makeText(ctx, text, Toast.LENGTH_LONG).show();
			}
		});
    }
	
}
