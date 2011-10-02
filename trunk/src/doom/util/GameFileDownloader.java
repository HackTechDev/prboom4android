package doom.util;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;


import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.AssetManager;
import android.game.prboom.PrBoomActivity;
import android.util.Log;

/**
 * 
 * @author Owner
 *
 */
public class GameFileDownloader 
{
	public static final String TAG = "GameFileDownloader";
	
	private ProgressDialog mProgressDialog;
	
	/**
	 * Download game files
	 * 1. libdoom_jni.so.gz (gzipped) -> /data/data/org.doom/files
	 * 2. prboom.wad.gz (gzipped) -> /sdacard/doom
	 * 3. game wad : doom1.wad.gz, plutonia.wad or tnt.wad (gzziped) -> /sdcard/doom
	 * 4. sound track: soundtrack.zip (zipped) -> /sdcard/doom/soundtrack
	 * @param ctx
	 * @param wadIdx
	 */
	public void downloadGameFiles (final Context ctx, final int wadIdx, final boolean force) {
		new Thread(new Runnable() {
			public void run() {
				Log.d(TAG, "Calling doDownload with wad: " + wadIdx + " force:" + force);
				doDownload(ctx, wadIdx, force);
				
				// Download complete! Ready to start
				PrBoomActivity.mHandler.post(new Runnable() {
					public void run() 
					{
						// close progress
						mProgressDialog.dismiss();
						
						// ready to go!
						DialogTool.Toast(ctx, "Ready. Select WAD from \"Choose WAD to play\" and press PLAY");
					}
				});
			}
		}).start();
		
		// Show progress
		mProgressDialog = new ProgressDialog(ctx);
        mProgressDialog.setMessage("Downloading files to the sdcard (required once)."
        		+ " This may take some time depending on your connection."
        		+ " Please wait and do not cancel!");
        
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgressDialog.show();
	}
	
	/*
	 * Fetch file
	 */
	private void doDownload(Context ctx, int wadIdx, boolean force) {
		try {
			// game wad
			downloadFile(DoomTools.DOWNLOAD_BASE + DoomTools.DOOM_WADS[wadIdx] + ".gz"
					, new File(DoomTools.DOOM_FOLDER + DoomTools.DOOM_WADS[wadIdx])
					, "gzip", null, force);
			
		} catch (Exception e) {
			e.printStackTrace();
			DialogTool.PostMessageBox(ctx, e.toString());
			mProgressDialog.dismiss();
		}
	}
	
	/**
	 * Download a file
	 * @param url URL
	 * @param dest File destination
	 * @param type one of gzip, zip
	 * @param folder destination folder (File)
	 * @param force force download?
	 * @throws Exception
	 */
	public void downloadFile (String url, File dest, String type, File folder, boolean force)	throws Exception 
	{
		Log.d(TAG, "Download " + url + " -> " + dest + " type: " + type + " folder=" + folder + " force:" + force);

		if ( ! dest.exists() || force) 
		{
			if ( force ) 
				Log.d(TAG, "Forcing download!");
			
	    	WebDownload wd = new WebDownload(url);
	    	wd.doGet(new FileOutputStream(dest), type.equalsIgnoreCase("gzip"));
	    	
	    	// If ZIP file unzip into folder
	    	if ( type.equalsIgnoreCase("zip")) {
	        	if ( folder == null)
	        		throw new Exception("Invalid destination folder for ZIP " + dest);
	        	
	        	if(!folder.exists())
	        		if ( !folder.mkdirs() )
	        			throw new IOException("Unable to create local folder " + folder);
	        	
	        	DoomTools.unzip(new FileInputStream(dest), folder);
	        	
	        	// cleanup
	        	dest.delete();
	    	}
		}
		else {
			Log.d(TAG, "Not fetching " + dest + " already exists.");
		}
	}
	/**
	 * Download game files
	 * 1. libdoom_jni.so.gz (gzipped) -> /data/data/org.doom/files
	 * 2. prboom.wad.gz (gzipped) -> /sdacard/doom
	 * 3. game wad : doom1.wad.gz, plutonia.wad or tnt.wad (gzziped) -> /sdcard/doom
	 * 4. sound track: soundtrack.zip (zipped) -> /sdcard/doom/soundtrack
	 * @param ctx
	 * @param wadIdx
	 */
	public void downloadSound (final Context ctx) {
		new Thread(new Runnable() {
			public void run() {
				doDownloadSound(ctx);
				
				// Download complete! Ready to start
				PrBoomActivity.mHandler.post(new Runnable() {
					public void run() 
					{
						// close progress
						mProgressDialog.dismiss();
						
						// ready to go!
						DialogTool.Toast(ctx, "Sound installed!");
					}
				});
			}
		}).start();
		
		// Show progress
		mProgressDialog = new ProgressDialog(ctx);
        mProgressDialog.setMessage("Downloading sound files to the sdcard."
        		+ " This may take some time depending on your connection."
        		+ " Please wait and do not cancel!");
        
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgressDialog.show();
	}
	
	/*
	 * Fetch file
	 */
	private void doDownloadSound(Context ctx) {
		try {
			// JNI lib /data/data/game.doom/files
			File lib = ctx.getFileStreamPath(DoomTools.DOOM_LIB);
			File parent = lib.getParentFile();
			
			if ( !parent.exists()) {
				// Gotta create parent /data/data/game.doom/files 
				if (  !parent.mkdirs() ) {
					// This should not happen!
					throw new Exception("Unable to create game folder:" + parent);
				}
			}
			File f = new File(DoomTools.DOOM_FOLDER + File.separator);
			if(!f.exists())
				f.mkdir();

/*			if ( DoomTools.hasSound()) {
				Log.d(TAG, "Sound folder " + DoomTools.getSoundFolder() + " already exists!");
				return;
			}
*/			
			f = new File(DoomTools.DOOM_SOUND_FOLDER + File.separator);
			if(!f.exists())
				f.mkdir();
			// soundtrack
			downloadFile(DoomTools.SOUND_BASE + "soundtrack.zip"
					, new File(DoomTools.DOOM_FOLDER + "soundtrack.zip")
					, "zip"		// this is a ZIP !
					, new File(DoomTools.DOOM_SOUND_FOLDER + File.separator) // dest folder
					, true);
			
		} catch (Exception e) {
			e.printStackTrace();
			DialogTool.PostMessageBox(ctx, e.toString());
			mProgressDialog.dismiss();
		}
	}
}
