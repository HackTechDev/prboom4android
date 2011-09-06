package doom.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.game.prboom.PrBoomActivity;
import android.util.Log;
import android.view.KeyEvent;


public class DoomTools {
	public static final String DOOM_FOLDER = "/sdcard/doom" + File.separator ;

	// Soundtrack
	public static final String DOOM_SOUND_FOLDER = "/sdcard/doom" + File.separator + "sound";
	
	// Base server URL
	public static final String URL_BASE =  "http://prboom4android.googlecode.com/files/";
	
	// HowTo URL
	public static final String URL_HOWTO = "http://code.google.com/p/prboom4android/wiki/HowTo";
	
	// Url prefix that has all Doom files: WADs, Sound + JNI lib
	public static final String DOWNLOAD_BASE = URL_BASE + "" ;
	
	// Url prefix that has all Sounds
	public static final String SOUND_BASE = DOWNLOAD_BASE + "" ;
	
	// Game files we can handle
	public static final String[] DOOM_WADS = {"freedoom.wad"};

	/**
	 * Doom lib. To be downloaded into /data/data/APP_PKG/files
	 */
	public static final String DOOM_LIB = "libdoom_jni.so";

	// These are required for the game to run
	public static final String REQUIRED_DOOM_WAD = "prboom.wad";  

	/*
	 * ASCII key symbols
	 */
	public static final int KEY_RIGHTARROW	= 0xae;
	public static final int KEY_LEFTARROW	= 0xac;
	public static final int KEY_UPARROW		= 0xad;
	public static final int KEY_DOWNARROW	= 0xaf;
	public static final int KEY_ESCAPE		= 27;
	public static final int KEY_ENTER		= 13;
	public static final int KEY_TAB			= 9;

	public static final int KEY_BACKSPACE	= 127;
	public static final int KEY_PAUSE		= 0xff;

	public static final int KEY_EQUALS		= 0x3d;
	public static final int KEY_MINUS		= 0x2d;

	public static final int KEY_RSHIFT		= (0x80+0x36);
	public static final int KEY_RCTRL		= (0x80+0x1d);
	public static final int KEY_RALT		= (0x80+0x38);

	public static final int KEY_LALT		= KEY_RALT;
	public static final int KEY_SPACE		= 32;
	public static final int KEY_COMMA		= 44;
	public static final int KEY_PERIOD		= 46;
	
	public static final int KEY_0			= 48;
	public static final int KEY_1			= 49;
	public static final int KEY_2			= 50;
	public static final int KEY_3			= 51;
	public static final int KEY_4			= 52;
	public static final int KEY_5			= 53;
	public static final int KEY_6			= 54;
	public static final int KEY_7			= 55;
	public static final int KEY_8			= 56;
	public static final int KEY_9			= 57;
	
	public static final int KEY_Y			= 121;
	public static final int KEY_N			= 110;
	public static final int KEY_Z			= 122;
	
	static boolean result;
	static ProgressDialog mProgressDialog;

	
	/**
	 * Convert an android key to a Doom ASCII
	 * @param key
	 * @return
	 */
	static public int keyCodeToKeySym( int key ) {
		switch (key) {
		
		// Left
		case 84: // SYM
		case KeyEvent.KEYCODE_DPAD_LEFT:
			return KEY_LEFTARROW;
		
		// Right
		case KeyEvent.KEYCODE_AT:	
		case KeyEvent.KEYCODE_DPAD_RIGHT:
			return KEY_RIGHTARROW;
		
		// Up
		case KeyEvent.KEYCODE_SHIFT_LEFT:	
		case KeyEvent.KEYCODE_DPAD_UP:
//		case KeyEvent.KEYCODE_W:
			return KEY_UPARROW;
		
		// Down
		case KeyEvent.KEYCODE_ALT_LEFT:
		case KeyEvent.KEYCODE_DPAD_DOWN:
//		case KeyEvent.KEYCODE_S:
			return KEY_DOWNARROW;
			
		case 23: // DPAD
		case KeyEvent.KEYCODE_ENTER:
			return KEY_ENTER;
			
		case KeyEvent.KEYCODE_SPACE:
//		case KeyEvent.KEYCODE_Q:
			return KEY_SPACE;

		case 4:	// ESC
			return KEY_ESCAPE;
		
		// Doom Map
		case KeyEvent.KEYCODE_ALT_RIGHT:
		case KeyEvent.KEYCODE_TAB:	
//		case KeyEvent.KEYCODE_Z:
			return KEY_TAB;
		
		// Strafe left
		case KeyEvent.KEYCODE_COMMA:
//		case KeyEvent.KEYCODE_A:
			return KEY_COMMA;

		// Strafe right
		case KeyEvent.KEYCODE_PERIOD:
//		case KeyEvent.KEYCODE_D:
			return KEY_PERIOD;

		case KeyEvent.KEYCODE_DEL:
			return KEY_BACKSPACE;
			
		default:
			// A..Z
	  		if (key >= 29 && key <= 54) {
	  			key += 68;
	  		}
	    	// 0..9
	  		else if (key >= 7 && key <= 16) {
	  			key += 41;
	  		}
	  		else {
	  			// Fire
//	  			key = KEY_RCTRL;
	  		}
			break;
		}
		return key;
	}

	public static void downloadFile (String url, File dest, String type, File folder, boolean force)	throws Exception 
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
	        	
	        	if ( ! folder.mkdirs() )
	        		throw new IOException("Unable to create local folder " + folder);
	        	
	        	unzip(new FileInputStream(dest), folder);
	        	
	        	// cleanup
	        	dest.delete();
	    	}
		}
		else {
			Log.d(TAG, "Not fetching " + dest + " already exists.");
		}
	}
	
	/**
	 * installs libdoom_jni.so from the assets included in the .apk package 
	 * @param ctx
	 * @return
	 */
	static public boolean installLib(final Context ctx) {
        InputStream cfgIn;
        try {
                cfgIn = ctx.getAssets().open("libdoom_jni.so.zip");
                unzip(cfgIn, new File(ctx.getFileStreamPath("").getAbsolutePath()));
                return true;
        } catch (IOException e) {
                e.printStackTrace();
        }
        return false;
	}
	
	static public boolean installConfig(Context ctx) {
		File f = new File(DoomTools.DOOM_FOLDER + File.separator);
		if(!f.exists())
			f.mkdir();
        InputStream cfgIn;
        try {
                cfgIn = ctx.getAssets().open("prboom.cfg.zip");
                unzip(cfgIn, new File(DOOM_FOLDER));
                return true;
        } catch (IOException e) {
                e.printStackTrace();
        }
        return false;
	}
	
	static public boolean installPrBoom(Context ctx) {
		// check of /sdcard/doom exists, if not create it
		File f = new File(DoomTools.DOOM_FOLDER + File.separator);
		if(!f.exists())
			f.mkdir();
        InputStream prboomIn;
        try {
                prboomIn = ctx.getAssets().open("prboom.zip");
                unzip(prboomIn, new File(DOOM_FOLDER));
                return true;
        } catch (IOException e) {
                e.printStackTrace();
        }
        return false;
	}
	
	private static boolean doDownload(Context ctx) {
		try {
			downloadFile(DoomTools.DOWNLOAD_BASE + DoomTools.DOOM_LIB + ".gz"
			, ctx.getFileStreamPath(DoomTools.DOOM_LIB)
			, "gzip", null, true);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			Log.e(TAG, "installLib: " + e.toString());
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	/**
	 * SDCARD?
	 * @return
	 */
	static public boolean hasSDCard() {
		try {
			File f = new File(DOOM_FOLDER);
			
			// Does /sdcard/doom exist?
			if ( f.exists()) return true;
			
			// Can we write into it?
			return  f.mkdir();
		} catch (Exception e) {
			System.err.println(e.toString());
			return false;
		}
	}
	
	/**
	 * Ping the download server
	 * @return
	 */
	static public boolean pingServer() {
		try {
			WebDownload wd = new WebDownload(URL_BASE);
			wd.doGet();
			int rc = wd.getResponseCode();
			Log.d(TAG, "PingServer Response:" + rc);
			return  rc == 200;
		} catch (Exception e) {
			Log.e(TAG, "PingServer: " + e.toString());
			return false;
		}
	}
	
	static public boolean wadExists (int idx) {
		final String path = DOOM_FOLDER + File.separator + DOOM_WADS[idx];
		return new File(path).exists();
	}
	
	static public boolean wadExists (String name) {
		final String path = DOOM_FOLDER + File.separator + name;
		return new File(path).exists();
	}
	
	static public boolean wadsExist() {
		for(int i = 0; i < DOOM_WADS.length; i++) {
			File f = new File(DOOM_FOLDER + File.separator + DOOM_WADS[i]);
			if(f.exists())
				return true;
		}
		return false;
	}
	
	static public void hardExit ( int code) {
		System.exit(code);
	}
	

    /**
     * Sound present for a WAD?
     * @param wadName
     * @return
     */
    public static boolean hasSound() { 
    	Log.d(TAG, "Sound folder: " + DOOM_SOUND_FOLDER);
    	return new File(DOOM_SOUND_FOLDER).exists(); 
    }
    
    /**
     * Get the sound folder name for a game file 
     * @param wadName
     * @return
     */
    public static File getSoundFolder() { 
    	return new File(DOOM_SOUND_FOLDER);
    }
    
    
    /**
     * Unzip utility
     * @param is
     * @param dest
     * @throws IOException
     */
    public static void unzip (InputStream is, File dest) throws IOException
    {
    	if ( !dest.isDirectory()) 
    		throw new IOException("Invalid Unzip destination " + dest );
    	
    	ZipInputStream zip = new ZipInputStream(is);
    	
    	ZipEntry ze;
    	
    	while ( (ze = zip.getNextEntry()) != null ) {
    		final String path = dest.getAbsolutePath() 
    			+ File.separator + ze.getName();
    		
    		//System.out.println(path);
    		
    		FileOutputStream fout = new FileOutputStream(path);
    		byte[] bytes = new byte[1024];
    		
            for (int c = zip.read(bytes); c != -1; c = zip.read(bytes)) {
              fout.write(bytes,0, c);
            }
            zip.closeEntry();
            fout.close();    		
    	}
    }
 

    static final String TAG = "DoomTools";
    
	
	/**
	 * Ckech 4 sdcard
	 * @return
	 */
	public static  boolean checkSDCard(Context ctx) {
		boolean sdcard = DoomTools.hasSDCard();
		
		if ( ! sdcard) {
			DialogTool.MessageBox(ctx, "No SDCARD", "An SDCARD is required to store game files.");
			return false;
		}		
		return true;
	}

	/**
	 * Make sure you have a web cn
	 * @param ctx
	 * @return
	 */
	public static  boolean checkServer(Context ctx) {
		boolean alive = DoomTools.pingServer();
		
		if ( ! alive) {
			DialogTool.MessageBox(ctx, "Sever Ping Failed", "Make sure you have a web connection.");
			return false;
		}		
		return true;
	}
	
	
	/**
	 * Clean old JNI libs & other files. 
	 */
	public static void cleanUp (final Context ctx, final int wadIdx) {
		AlertDialog d = DialogTool.createAlertDialog(ctx, "Clean Up?"
				, "This will remove game files from the sdcard. Use this option if you are experiencing major problems.");
		
		d.setButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
        		// remove JNI lib
            	Log.d(TAG, "Removing " + DOOM_LIB);
            	
        		File f = ctx.getFileStreamPath(DOOM_LIB);
        		if ( f.exists()) f.delete();
        		
        		// clean sounds
        		deleteSounds(); //wadIdx);
        		
        		// game files
        		deleteWads();
            }
        });
		d.setButton2("No", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                /* User clicked OK so do some stuff */
            }
        });
		d.show();
	}

	/**
	 * Clean sounds
	 * @param wadIdx
	 */
	private static void deleteSounds() //int wadIdx) 
	{
		File folder = getSoundFolder(); //DoomTools.DOOM_WADS[wadIdx]);
		
		if ( !folder.exists()) {
			Log.e(TAG, "Error: Sound folder " + folder + " not found.");
			return;
		}
		
		File[] files = folder.listFiles();
		
		for (int i = 0; i < files.length; i++) {
			
			if (files[i].exists() )
				files[i].delete();
		}
		if ( folder.exists() ) folder.delete();
	}
	
	/**
	 * Cleanup game files
	 */
	private static void deleteWads() {
		File folder = new File(DoomTools.DOOM_FOLDER);
		
		if ( !folder.exists()) {
			Log.e(TAG, "Error: Doom folder " + folder + " not found.");
			return;
		}
		
		File[] files = folder.listFiles();
		
		for (int i = 0; i < files.length; i++) {
			
			if (files[i].exists() )
				files[i].delete();
		}
		if ( folder.exists() ) folder.delete();
	}

	/**
	 * Add a default config to DOOM_FOLDER with default key bindings
	 * @param ctx
	 */
//	static final String DEFAULT_CFG = "prboom.cfg";
//	
//	public static void createDefaultDoomConfig(Handler handler, Context ctx) {
//		File dest = new File(DOOM_FOLDER + DEFAULT_CFG);
//		
//		// Skip if config exists
//		if ( dest.exists()) {
//			Log.w(TAG, "A default Doom config already exists in " + dest);
//			return;
//		}
//		
//		try {
//			BufferedInputStream is = new BufferedInputStream( ctx.getAssets().open(DEFAULT_CFG));
//			FileOutputStream fos = new FileOutputStream(dest);
//			
//			byte[] buf = new byte [1024];
//			
//			// let the user know
//			Toast(handler, ctx, "Default movement keys: 1AQW");
//			
//			Log.d(TAG, "Writing a default DOOM config to " + dest);
//			
//            for (int read = is.read(buf); read != -1; read = is.read(buf)) {
//                fos.write(buf,0, read);
//            }
//
//		} catch (Exception e) {
//			Log.e(TAG, "Error saving default DOOm config: " + e.toString());
//		}
//	}
	
}
