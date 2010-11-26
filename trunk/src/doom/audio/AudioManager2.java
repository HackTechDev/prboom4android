package doom.audio;

import java.io.File;
import java.util.HashMap;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import doom.util.DoomTools;

/**
 * Audio manager. Caches sounds for performance
 * @author vsilva
 *
 */
public class AudioManager2 
{
	static final String TAG = "AudioMgr";
	public static final int MAX_CLIPS = 50;
	
	static private AudioManager2 am ;

	// Game sound (WAVs)
	private volatile HashMap<String, AudioClip> mSounds = new HashMap<String, AudioClip>();
	private Context mContext;
	private int mClipCount = 0;
	private boolean mPaused = false;
	
	// BG music
	private AudioClip music;
	
	/**
	 * get Instance
	 * @param ctx
	 * @param wadIdx
	 * @return
	 */
	static public AudioManager2 getInstance(Context ctx, int wadIdx) {
		if ( am == null) return new AudioManager2(ctx, wadIdx);
		return am;
	}

	private AudioManager2(Context ctx, int wadIdx) {
		mContext = ctx;
//		mWadIdx = wadIdx;
		preloadSounds(ctx, wadIdx);
	}
	
	/**
	 * Start a sound by name & volume
	 * @param name example "pistol" when firing the gun
	 * @param vol
	 */
	public synchronized void startSound( String name, int vol) 
	{ 
		if(mPaused)
			return;
		// The sound key as stored in the FS -> DS[NAME-UCASE].wav
		String key = "DS" + name.toUpperCase() + ".wav";
		
		if ( mSounds.containsKey(key)) {
			//Log.d(TAG, "Playing " + key + " from cache  vol:" + vol);
			mSounds.get(key).play();
		}
		else {
			// load clip from disk
			File folder = DoomTools.getSoundFolder(); //DoomTools.DOOM_WADS[mWadIdx]);
			File sound = new File(folder.getAbsolutePath() + File.separator + key);
			
			if ( ! sound.exists()) {
				key = "DS" + name.toUpperCase() + ".wav";
				sound = new File(folder.getAbsolutePath() + File.separator + key);
				if(!sound.exists()) {
					Log.e(TAG, sound + " not found.");
					return;
				}
			}
			
			// If the sound table is full remove a random entry
			if ( mClipCount > MAX_CLIPS) {
				// Remove a last key
				int idx = mSounds.size() - 1; //(int)(Math.random() * (mSounds.size() - 1));
				
				//Log.d(TAG, "Removing cached sound " + idx 
				//		+ " HashMap size=" + mSounds.size());
				
				String k = (String)mSounds.keySet().toArray()[idx];
				AudioClip clip = mSounds.remove(k);
				clip.release();
				clip = null;
				mClipCount--;
			}
			
			//Log.d(TAG, "Play & Cache " + key + " u:" + Uri.fromFile(sound));

			AudioClip clip = new AudioClip(mContext, Uri.fromFile(sound));
			clip.play(vol);
			
			mSounds.put(key, clip);
			mClipCount++;
		}
	}
	

	/**
	 * PreLoad the most used sounds into a hash map
	 * @param ctx
	 * @param wadIdx
	 * @return
	 */
	public void preloadSounds(Context ctx, int wadIdx)   
	{
		// These are some common sound keys pre-loaded for speed
		String [] names = new String[] {"DSPISTOL.wav"	// pistol
				, "DSDOROPN.wav", "DSDORCLS.wav" 		// doors open/close
				, "DSPSTOP.wav", "DSSWTCHN.wav", "DSSWTCHX.wav"
				, "DSITEMUP.wav", "DSPOSACT.wav"
				, "DSPOPAIN.wav", "DSPODTH1.wav"
				, "DSSHOTGN.wav" };
		
		// Sound folder
		File folder = DoomTools.getSoundFolder(); //DoomTools.DOOM_WADS[wadIdx]);
		
		if ( !folder.exists()) {
			Log.e(TAG, "Error: Sound folder " + folder + " not found.");
			return;
		}
		
		// WAVs
		File[] files =  new File[names.length]; 
		
		for (int i = 0; i < files.length; i++ ) { 
			files[i] = new File(folder +  File.separator +  names[i]);
			
			if ( files[i].exists()) {
				//Log.d(TAG "PreLoading sound " + files[i].getName() + " uri=" + Uri.fromFile(files[i]));
				mSounds.put(files[i].getName(), new AudioClip(ctx, Uri.fromFile(files[i])));
			}
			else
				System.err.println("AudioMgr:" + files[i] + " not found");
			
		}
	}
	
	/**
	 * Start background music
	 * @param ctx
	 * @param key music key (e.g intro, e1m1)
	 */
	public void startMusic (Context ctx , String key, int loop) {
		if(mPaused)
			return;
		// Sound folder
		File folder = DoomTools.getSoundFolder(); //DoomTools.DOOM_WADS[mWadIdx]);
		File sound = new File(folder +  File.separator + "d1" + key + ".ogg");
		
		if ( !sound.exists()) {
			Log.e(TAG, "Unable to find music " + sound);
			return;
		}
		
		if ( music != null) {
			music.stop();
			music.release();
		}
		
		Log.d(TAG, "Starting music " + sound + " loop=" + loop);
		music = new AudioClip(ctx, Uri.fromFile( sound ));
		
		// Too anoying!
//		if ( loop != 0 ) 
//			music.loop();
//		else
		music.setVolume(100);
		music.play();
	}

	/**
	 * Stop background music
	 * @param key
	 */
	public void stopMusic (String key) {
		// Sound folder
		File folder = DoomTools.getSoundFolder(); //DoomTools.DOOM_WADS[mWadIdx]);
		Uri sound = Uri.fromFile(new File(folder +  File.separator + "d1" + key + ".ogg"));
		
		if ( music != null  ) {
			if ( !sound.equals(Uri.parse(music.getName()))) {
				Log.w(TAG, "Stop music uri "  + sound  + " different than " + music.getName());
			}
			music.stop();
			music.release();
			music = null;
		}
	}

	public void setMusicVolume (int vol) {
		if ( music != null)
			music.setVolume(vol);
		else
			Log.e(TAG, "setMusicVolume " + vol + " called with NULL music player");
	}
	
	public void pauseAudioMgr(boolean pause) {
		mPaused = pause;
	}
	
}
