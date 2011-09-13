/****************************************************************************

    This file is part of Doom-for-Android.

    Doom-for-Android is free software: you can redistribute it and/or
    modify it under the terms of the GNU General Public License as published
    by the Free Software Foundation version 1 of the License.

    Doom-for-Android is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Doom-for-Android. If not, see http://www.gnu.org/licenses
    
 ****************************************************************************/

package android.game.prboom;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;

import doom.audio.AudioClip;
import doom.audio.AudioManager;
import doom.util.DialogTool;
import doom.util.DoomTools;
import doom.util.Natives;
import doom.util.GameFileDownloader;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Bitmap.Config;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CheckedTextView;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.CompoundButton.OnCheckedChangeListener;

/**
 * Doom for Android Main Activity
 * 
 * @author vsilva
 * 
 */
public class PrBoomActivity extends Activity implements Natives.EventListener,
		SurfaceHolder.Callback {
	private static final String TAG = "PrBoomActivity";
	public static final String PREFS_NAME = "PrBoom4Android";
	private static final int MOUSE_HSENSITIVITY = 100;
	private static final int MOUSE_VSENSITIVITY = 40;

	static private Bitmap mDoomBitmap;
	static private ImageView mView;
	// width of mBitmap
	private int mDoomWidth;
	// height of mBitmap
	private int mDoomHeight;
	private boolean mFirstRun = true;

	private SurfaceView mSurfaceView;
	private SurfaceHolder mSurfaceHolder;
	private Matrix mMatrix = new Matrix();
	private int mSurfaceWidth;
	private int mSurfaceHeight;

	public static final Handler mHandler = new Handler();

	private int wadIdx = 0;
	private String wadName = "";
	private String extraArgs = "";

	// Audio Cache Manager
	private AudioManager mAudioMgr;

	// Sound? ( no by default)
	private boolean mSound = false;
	private boolean mFullscreen = false;

	private static boolean mGameStarted = false;

	private static boolean mGamePaused = false;

	private Thread mGameThread = null;

	private boolean mMenuLongPressed = false;

	// members for dealing with the Virtual D-Pad
	private boolean mUseTouchControls = false;
	private VirtualDPad mVStick = null;
	private int mVstickPosition = VirtualDPad.POS_CENTER;
	private int mVstickLastPos = VirtualDPad.POS_CENTER;
	private float mVstickX = 0;
	private float mVstickY = 0;
	private View mMultiTouchController;

	private HashMap<String, View> mOnScreenControls;
	
	private HashMap.Entry<String,View> touchedViews[] = new HashMap.Entry[2]; 

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);

		setContentView(R.layout.main_screen);
		
		changeFonts((ViewGroup)findViewById(R.id.rootView));

		loadOnScreenControls();

		mView = (ImageView) findViewById(R.id.doom_iv);
		loadSpinnerWads(this.getApplicationContext(),
				(Spinner) findViewById(R.id.s_files), 0);
		
		mSurfaceView = (SurfaceView) findViewById(R.id.gameCanvas);
		mSurfaceHolder = mSurfaceView.getHolder();
		mSurfaceHolder.addCallback(this);

		if (mGameStarted) {
			if (mGamePaused)
				resumeGame();
			return;
		}
		new AudioClip(this, R.raw.d1intro).play();

		loadPreferences();
		initMainScreen();
		if (!DoomTools.wadsExist()) {
			wadIdx = 0;
			MessageBox(
					"Read this carefully",
					"You must install a game file. Tap \"Install WADs\" for auto-install. "
							+ "Tap \"Help Me!\" for instructions on manual installation. "
							+ "A fast WIFI network and SDCARD are required."
							+ "If you experience game problems, try the Cleanup option.");
		}
		if (DoomTools.hasSDCard()) {
			// check that prboom.cfg and prboom.wad exist
			checkForPrBoom();
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		pauseGame();
	}

	@Override
	public void onResume() {
		super.onResume();
		resumeGame();
	}

	@Override
	public void onStop() {
		super.onStop();
		DoomTools.hardExit(0);
	}

	/**
	 * Play
	 */
	private void play() {
		if (!DoomTools.checkSDCard(this))
			return;

		savePreferences();

		// Make sure all required files are in place
		if (!checkSanity()) {
			return;
		}

		// Load lib
		if (!loadLibrary()) {
			// this should not happen
			return;
		}

		// start doom!
		startGame();
	}

	/**
	 * Make sure all required stuff is in /sdcard/doom
	 * 
	 * @return
	 */
	private boolean checkSanity() {
		// check for game file
		if (!DoomTools.wadExists(wadName)) {
			MessageBox("Missing Game file " + DoomTools.DOOM_FOLDER
					+ File.separator + wadName + ". Try installing a game.");
			return false;
		}

		// check 4 prboom.wad
		File prboom = new File(DoomTools.DOOM_FOLDER
				+ DoomTools.REQUIRED_DOOM_WAD);

		if (!prboom.exists()) {
			MessageBox("Missing required Game file " + prboom);
			return false;
		}

		// Sound?
		if (!DoomTools.hasSound() && mSound) {
			MessageBox("Warning: Soundtrack not found!");
			return true;
		}
		return true;
	}

	void MessageBox(String text) {
		DialogTool.MessageBox(this, getString(R.string.app_name), text);
	}

	void MessageBox(String title, String text) {
		DialogTool.MessageBox(this, title, text);
	}

	void checkForConfig() {
		// check 4 prboom.cfg
		File prboom = new File(DoomTools.DOOM_FOLDER + "prboom.cfg");

		if (!prboom.exists() || mFirstRun) {
			DoomTools.installConfig(this);
		}
	}

	void checkForPrBoom() {
		// check 4 prboom.cfg and prboom.wad
		File prboom = new File(DoomTools.DOOM_FOLDER + "prboom.wad");

		if (!prboom.exists() || mFirstRun) {
			DoomTools.installPrBoom(this);
		}
	}

	void checkForUnhiddenSound() {
		File soundDir = new File(DoomTools.DOOM_FOLDER + "sound/.nomedia");
		if (!soundDir.exists()) {
			try {
				soundDir.createNewFile();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				MessageBox("Error",
						"Unable to hide sound folder from Music Player.  Oops.");
			}
		}
	}

	/******************************************************************
	 * GAME subs
	 ******************************************************************/
	private void startGame() {
		// final String wad = DoomTools.DOOM_WADS[wadIdx];

		if (wadName == null || wadName == "") {
			MessageBox(this, "Invalid game file!");
			return;
		}

		// Audio?
		if (mSound)
			mAudioMgr = AudioManager.getInstance(this, wadIdx);

		enableDPad(mUseTouchControls);
		adjustFireButtonSize();

		// Doom args
		final String[] argv;

		// Window size: P 320x320 L: 320x200 (will autoscale to fit the screen)
		ViewGroup.LayoutParams lp = mView.getLayoutParams();
		float w;
		float h;
		if (mFullscreen) {
			//lp.width = 480;
			//lp.height = 320;
			w = 480;
			h = 320;
			// mView.setLayoutParams(lp);
		} else {
			lp.width = 320;
			lp.height = 200;
			w = 320;
			h = 200;
			mView.setLayoutParams(lp);
		}

		boolean stockWad = false;
		for (int i = 0; i < DoomTools.DOOM_WADS.length; i++) {
			String wad = DoomTools.DOOM_WADS[i];
			if (wadName.contains(wad)) {
				stockWad = true;
				break;
			}
		}
		String args = new String();
		if (stockWad)
			args = "doom -width " + w + " -height " + h + " -iwad " + wadName
					+ " " + extraArgs;
		// argv = new String[]{"doom" , "-width", ""+w, "-height", ""+h,
		// "-iwad", wadName};
		else
			args = "doom -width " + w + " -height " + h + " -iwad doom2.wad"
					+ " -file " + DoomTools.DOOM_FOLDER + wadName + " "
					+ extraArgs;
		// argv = new String[]{"doom" , "-width", ""+w, "-height", ""+h,
		// "-iwad", "doom2.wad", "-file", DoomTools.DOOM_FOLDER+wadName};

		Log.i(TAG, "Starting doom thread with " + args);
		Log.i(TAG,
				"extraArgs = " + extraArgs + " length is " + extraArgs.length());

		argv = args.split(" ");

		mGameThread = new Thread(new Runnable() {
			public void run() {
				mGameStarted = true;
				Natives.DoomMain(argv);
			}
		});
		mGameThread.start();

		showGameScreen();

		// ugly hack to get prboom to save the config file so it can write
		// to it and save settings user changes.
		new Thread(new Runnable() {
			public void run() {
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				File prboom = new File(DoomTools.DOOM_FOLDER + "prboom.cfg");
				try {
					FileInputStream cfgIn = new FileInputStream(
							DoomTools.DOOM_FOLDER + "prboom.cfg");
					try {
						char chr1 = (char) cfgIn.read();
						char chr2 = (char) cfgIn.read();
						if (chr1 == '#' && chr2 == '!')
							prboom.delete();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}).start();
	}

	/**
	 * Message Box
	 * 
	 * @param ctx
	 * @param text
	 */
	static void MessageBox(final Context ctx, final String text) {
		Toast.makeText(ctx, text, Toast.LENGTH_LONG).show();
	}

	boolean initialized = false;

	/**
	 * Load JNI library. Lib must exist in /data/data/APP_PKG/files
	 */
	private boolean loadLibrary() {
		if (initialized)
			return true;

		Log.d(TAG, "Loading JNI library... ");
		// now that the library is in libs/armeabi we can use the following
		System.loadLibrary(DoomTools.DOOM_LIB);

		// Listen for Doom events
		Natives.setListener(this);
		return true;
	}

	/*************************************************************
	 * Android Events
	 *************************************************************/
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		// Toggle nav ctls visibility when the menu key is pressed
		// if (keyCode == KeyEvent.KEYCODE_MENU) {
		// DoomTools.toggleView(findViewById(R.id.pan_ctls));
		// DoomTools.toggleView(findViewById(R.id.other_ctls));
		// return true;
		// }

		if (keyCode == KeyEvent.KEYCODE_MENU) {
			if (mGameStarted && mMenuLongPressed == false)
				if (!mGamePaused) {
					pauseGame();
					showMainScreen();
				} else {
					resumeGame();
					showGameScreen();
				}
			mMenuLongPressed = false;
			return true;
		}
		int sym = DoomTools.keyCodeToKeySym(keyCode);

		try {
			if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER)
				Natives.keyEvent(Natives.EV_KEYUP, DoomTools.KEY_RCTRL);
			Natives.keyEvent(Natives.EV_KEYUP, sym);
			Log.d(TAG, "onKeyUp sent key " + keyCode);

		} catch (UnsatisfiedLinkError e) {
			// Should not happen
			Log.e(TAG, e.toString());
		}
		return false;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// Gotta ignore menu key (used to toggle pan ctls)
		// if (keyCode == KeyEvent.KEYCODE_MENU) {
		// return true;
		// }

		if (keyCode == KeyEvent.KEYCODE_MENU) {
			int flags = event.getFlags();
			if ((flags & KeyEvent.FLAG_LONG_PRESS) != 0) {
				mMenuLongPressed = true;
				InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				// only will trigger it if no physical keyboard is open
				mgr.showSoftInput(this.mView, InputMethodManager.SHOW_FORCED);
			}
			return true;
		}
		int sym = DoomTools.keyCodeToKeySym(keyCode);

		try {
			if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER)
				Natives.keyEvent(Natives.EV_KEYDOWN, DoomTools.KEY_RCTRL);
			Natives.keyEvent(Natives.EV_KEYDOWN, sym);
			Log.d(TAG, "onKeyDown sent  key " + keyCode);
		} catch (UnsatisfiedLinkError e) {
			// Should not happen
			Log.e(TAG, e.toString());
		}

		return false;
	}

	@Override
	public boolean onTrackballEvent(MotionEvent event) {
		if (!mGameStarted || mGamePaused) {
			super.onTrackballEvent(event);
			return false;
		}
		int keyCode = 0;
		if (event.getAction() == MotionEvent.ACTION_MOVE) {
			float x = event.getX() * event.getXPrecision();
			float y = event.getY() * event.getYPrecision();

			if (mGameStarted)
				Natives.motionEvent(0, (int) (x * MOUSE_HSENSITIVITY),
						-(int) (y * MOUSE_VSENSITIVITY));

			return true;
		}
		return false;
	}

	/*************************************************************
	 * Doom Event callbacks
	 *************************************************************/

	/**
	 * Fires when there an image update from Doom lib
	 */
	@Override
	public void OnImageUpdate(int[] pixels) {
		mDoomBitmap.setPixels(pixels, 0, mDoomWidth, 0, 0, mDoomWidth,
				mDoomHeight);

		mHandler.post(new Runnable() {
			public void run() {
				drawScreen();
				//mView.setImageBitmap(mDoomBitmap);
				
			}
		});
	}

	/**
	 * Fires on LIB message
	 */
	@Override
	public void OnMessage(String text) {
		System.out.println("**Doom Message: " + text);

	}

	@Override
	public void OnInitGraphics(int w, int h) {
		Log.d(TAG, "OnInitGraphics creating Bitmap of " + w + " by " + h);
		mDoomWidth = w;
		mDoomHeight = h;
		mDoomBitmap = Bitmap.createBitmap(w, h, Config.RGB_565);
		int x = (mSurfaceWidth - w) / 2;
		int y = (mSurfaceHeight - h) / 2;
		if(mFullscreen) {
			float scale = (float)mSurfaceHeight / (float)h;
			mMatrix.preScale(scale, scale, w/2, h/2);
		}
		mMatrix.postTranslate(x, y);
	}

	@Override
	public void OnFatalError(final String text) {
		mHandler.post(new Runnable() {
			public void run() {
				MessageBox("Fatal Error", "Doom has terminated. " + "Reason: "
						+ text + " - Please report this error.");
			}
		});

		// Wait for the user to read the box
		try {
			Thread.sleep(8000);
		} catch (InterruptedException e) {

		}
		// Must quit here or the LIB will crash
		DoomTools.hardExit(-1);
	}

	@Override
	public void OnStartSound(String name, int vol) {
		if (mSound && mAudioMgr == null) {
			Log.e(TAG, "Bug: Audio Mgr is NULL but sound is enabled!");
			return;
		}

		try {
			if (mSound && mAudioMgr != null)
				mAudioMgr.startSound(name, vol);

		} catch (Exception e) {
			Log.e(TAG, "OnStartSound: " + e.toString());
		}
	}

	/**
	 * Fires on background music
	 */
	@Override
	public void OnStartMusic(String name, int loop) {
		if (mSound && mAudioMgr != null)
			mAudioMgr.startMusic(PrBoomActivity.this, name, loop);
	}

	/**
	 * Stop bg music
	 */
	@Override
	public void OnStopMusic(String name) {
		if (mSound && mAudioMgr != null)
			mAudioMgr.stopMusic(name);
	}

	@Override
	public void OnSetMusicVolume(int volume) {
		if (mSound && mAudioMgr != null)
			mAudioMgr.setMusicVolume(volume);
	}

	/**
	 * Send a key event to the native layer
	 * 
	 * @param type
	 * @param sym
	 */
	private void sendNativeKeyEvent(int type, int sym) {
		try {
			Natives.keyEvent(type, sym);
		} catch (UnsatisfiedLinkError e) {
			Log.e(TAG, e.toString());
		}
	}

	@Override
	public void OnQuit(int code) {
		// TODO Not yet implemented in the JNI lib
		Log.d(TAG, "Doom Hard Stop.");
		mGameStarted = false;
		mView.setBackgroundResource(R.drawable.doom);
	}

	public void surfaceCreated(SurfaceHolder holder) {
		Log.d(getClass().getSimpleName(), "surfaceCreated");
	}

	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
		Log.d(getClass().getSimpleName(), "surfaceChanged");
		mSurfaceWidth = w;
		mSurfaceHeight = h;
	}

	public void surfaceDestroyed(SurfaceHolder holder) {
		Log.d(getClass().getSimpleName(), "surfaceDestroyed");
	}

	private void drawScreen() {
		Canvas canvas = mSurfaceHolder.lockCanvas();
		canvas.drawBitmap(mDoomBitmap, mMatrix, null);
		mSurfaceHolder.unlockCanvasAndPost(canvas);
	}

	private void showMainScreen() {
		findViewById(R.id.main_ctls).setVisibility(View.VISIBLE);
		if(mUseTouchControls)
			findViewById(R.id.gameControls).setVisibility(View.INVISIBLE);
		if (mGameStarted) {
			findViewById(R.id.install).setEnabled(false);
			findViewById(R.id.installSound).setEnabled(false);
			findViewById(R.id.sound).setEnabled(false);
			findViewById(R.id.fullscreen).setEnabled(false);
			findViewById(R.id.arguments).setEnabled(false);
			findViewById(R.id.clean).setEnabled(false);
			findViewById(R.id.s_files).setEnabled(false);
		}
	}

	private void showGameScreen() {
		findViewById(R.id.main_ctls).setVisibility(View.INVISIBLE);
		if(mUseTouchControls)
			findViewById(R.id.gameControls).setVisibility(View.VISIBLE);
	}

	private void loadSpinnerWads(Context ctx, Spinner spinner, int idx) {
		ArrayAdapter adapter = new ArrayAdapter(ctx,
				android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(adapter);

		File dir = new File(DoomTools.DOOM_FOLDER);
		if (!dir.exists() || !DoomTools.wadsExist())
			return;
		File files[] = dir.listFiles();
		if (files.length == 0)
			return;

		for (int i = 0; i < files.length; i++) {
			if (files[i].isFile()) {
				String name = files[i].getName();
				if (!name.contains("prboom")
						&& (name.contains(".wad") || name.contains(".WAD")))
					adapter.add(name);
			}
		}
		spinner.setSelection(wadIdx);
	}

	public void initMainScreen() {
		if (DoomTools.wadExists(0))
			((Spinner) this.findViewById(R.id.s_files)).setSelection(wadIdx);
		((CheckBox) this.findViewById(R.id.fullscreen)).setChecked(mFullscreen);
		((CheckBox) this.findViewById(R.id.sound)).setChecked(mSound);
		((CheckBox) this.findViewById(R.id.touch)).setChecked(mUseTouchControls);
		((EditText) this.findViewById(R.id.arguments)).setText(extraArgs);

		findViewById(R.id.s_files).setOnTouchListener(
				new View.OnTouchListener() {

					@Override
					public boolean onTouch(View v, MotionEvent e) {
						if (e.getAction() == MotionEvent.ACTION_DOWN) {
							// TODO Auto-generated method stub
							loadSpinnerWads(v.getContext(), (Spinner) v, 0);
							if (!DoomTools.wadsExist()) {
								DialogTool.MessageBox(
										v.getContext(),
										"No WADs",
										"You will need to install some"
												+ " WAD files in order to play.  Press \"Install WADs\" to get going.");
							}
						}
						return false;
					}

				});

		((Spinner) findViewById(R.id.s_files))
				.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

					@Override
					public void onItemSelected(AdapterView<?> arg0, View arg1,
							int arg2, long arg3) {
						// TODO Auto-generated method stub
						wadIdx = arg2;
					}

					@Override
					public void onNothingSelected(AdapterView<?> arg0) {
						// TODO Auto-generated method stub

					}

				});

		findViewById(R.id.play).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mGameStarted && mGamePaused) {
					showGameScreen();
					resumeGame();
				} else {
					getGameSettings();
					play();
				}
			}
		});

		((CheckBox) findViewById(R.id.touch))
				.setOnCheckedChangeListener(new OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton buttonView,
							boolean isChecked) {
						// TODO Auto-generated method stub
						if (mGameStarted) {
							mUseTouchControls = isChecked;
						}
					}

				});

		mMultiTouchController = findViewById(R.id.multiTouchView);
		mMultiTouchController.setOnTouchListener(touchControlsListener);

		ImageButton vstick = (ImageButton) findViewById(R.id.vstick);
		mVStick = new VirtualDPad(180, 180, 24);

		findViewById(R.id.install).setOnClickListener(
				new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						if (mGameStarted) {
							MessageBox("Can't install while game in progress.");
						}

						// Download Game file
						DialogTool.showDownloadDialog(v.getContext());
					}
				});

		findViewById(R.id.installSound).setOnClickListener(
				new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						if (mGameStarted) {
							MessageBox("Can't install while game in progress.");
						}

						// Download soundtrack.zip file
						new GameFileDownloader().downloadSound(v.getContext());
					}
				});

		findViewById(R.id.clean).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// Cleanup
				if (mGameStarted) {
					MessageBox("Can't cleanup while game in progress.");
				}

				DoomTools.cleanUp(PrBoomActivity.this, wadIdx);
			}
		});

		findViewById(R.id.help).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// Help
				DialogTool
						.launchBrowser(PrBoomActivity.this, DoomTools.URL_HOWTO);
			}
		});

		findViewById(R.id.quit).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				getGameSettings();
				savePreferences();
				DoomTools.hardExit(0);
			}
		});
	}

	/**
	 * Custom touch listener for the multi-touch implementation
	 */
	private OnTouchListener touchControlsListener = new OnTouchListener() {

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			final int action = event.getAction();
			float x = -1f, y = -1f;
			boolean consumed = false;
			switch (action & MotionEvent.ACTION_MASK) {
			case MotionEvent.ACTION_DOWN:
				x = event.getX(0);
				y = event.getY(0);
				return processTouch(x, y, event, 0);
			case MotionEvent.ACTION_UP:
				x = event.getX(0);
				y = event.getY(0);
				return processTouch(x, y, event, 0);
			case MotionEvent.ACTION_POINTER_DOWN:
				x = event.getX(1);
				y = event.getY(1);
				return processTouch(x, y, event, 0);
			case MotionEvent.ACTION_POINTER_UP:
				x = event.getX();
				y = event.getY();
				return processTouch(x, y, event, 1);
			case MotionEvent.ACTION_MOVE:
				for(int i = 0; i < event.getPointerCount(); i++)
				{
					x = event.getX(i);
					y = event.getY(i);
					consumed |= processTouch(x, y, event, i);
				}
				return consumed;
			}
			return false;
		}

	};

	/**
	 * Processes a touch based at (x,y). This should handle touching parts of
	 * the on-screen controls
	 * 
	 * @param x - x coordinate of the touch
	 * @param y - y coordinate of the touch
	 * @return - true if the touch was on an onScreenControl
	 */
	private boolean processTouch(float x, float y, MotionEvent event, int pointerIndex) {
		if(mGameStarted == false)
			return false;
		
		int action = event.getAction() & MotionEvent.ACTION_MASK;
		String controlId;
		View v;
		Rect outRect = new Rect();
		StringBuilder sb = new StringBuilder();
		sb.append("processTouch: " + "(");
		sb.append(x);
		sb.append(",");
		sb.append(y);
		sb.append(") ");
		
		switch (action) {
		case MotionEvent.ACTION_DOWN:
			pointerIndex = 0;
		case MotionEvent.ACTION_POINTER_DOWN:
			pointerIndex = (event.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK)
				>> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
			boolean foundView = false;
			HashMap.Entry<String, View> other = null;
			Iterator it = mOnScreenControls.entrySet().iterator();
			while (it.hasNext() && !foundView) {
				HashMap.Entry<String, View> entry = (HashMap.Entry<String, View>) it.next();
				controlId = entry.getKey();
				v = entry.getValue();
				v.getHitRect(outRect);
				if ( (x >= outRect.left && x <= outRect.right) && 
					 (y >= outRect.top && y <= outRect.bottom) ) {
					touchedViews[pointerIndex] = entry;
					foundView = true;
					sb.append(controlId); sb.append(" touched! [");
					sb.append(pointerIndex); sb.append("]");
					Log.d(TAG, controlId + " touched! [" + pointerIndex + "]");
				} else if (controlId.contains("FIRE"))
					other = entry;
			}
			Log.d(TAG, sb.toString());
			if (!foundView)
				return false;
			
			v = touchedViews[pointerIndex].getValue();
			controlId = touchedViews[pointerIndex].getKey();
			if (controlId.contains("DPAD")) {
				v.getHitRect(outRect);
				mVstickX = x - outRect.left;
				mVstickY = y - outRect.top;
				mVstickPosition = mVStick.getPosition(mVstickX, mVstickY);
				int pos = VirtualDPad.POS_CENTER;
				if ((mVstickPosition & VirtualDPad.POS_UP) != 0) {
					Natives.keyEvent(Natives.EV_KEYDOWN,
							DoomTools.keyCodeToKeySym(KeyEvent.KEYCODE_W));
					pos |= VirtualDPad.POS_UP;
				}
				if ((mVstickPosition & VirtualDPad.POS_LEFT) != 0) {
					Natives.keyEvent(Natives.EV_KEYDOWN,
							DoomTools.keyCodeToKeySym(KeyEvent.KEYCODE_A));
					pos |= VirtualDPad.POS_LEFT;
				}
				if ((mVstickPosition & VirtualDPad.POS_DOWN) != 0) {
					Natives.keyEvent(Natives.EV_KEYDOWN,
							DoomTools.keyCodeToKeySym(KeyEvent.KEYCODE_S));
					pos |= VirtualDPad.POS_DOWN;
				}
				if ((mVstickPosition & VirtualDPad.POS_RIGHT) != 0) {
					Natives.keyEvent(Natives.EV_KEYDOWN,
							DoomTools.keyCodeToKeySym(KeyEvent.KEYCODE_D));
					pos |= VirtualDPad.POS_RIGHT;
				}
				mVstickLastPos = pos;
				mVstickPosition = pos;
				setIndicator(pos);
			} else if (controlId.contains("TURN")) {
				v.getHitRect(outRect);
				if (x >= outRect.left && x < outRect.left + v.getWidth()/2) {
					Natives.keyEvent(Natives.EV_KEYDOWN, DoomTools.KEY_LEFTARROW);
					Natives.keyEvent(Natives.EV_KEYUP, DoomTools.KEY_RIGHTARROW);
				} else {
					Natives.keyEvent(Natives.EV_KEYUP, DoomTools.KEY_LEFTARROW);
					Natives.keyEvent(Natives.EV_KEYDOWN, DoomTools.KEY_RIGHTARROW);
				}
			} else if (controlId.contains("OPEN")) {
				Natives.keyEvent(Natives.EV_KEYDOWN,
						DoomTools.keyCodeToKeySym(KeyEvent.KEYCODE_Q));
			} else if (controlId.contains("FIRE")) {
				Natives.keyEvent(Natives.EV_KEYDOWN, DoomTools.KEY_RCTRL);
				Natives.keyEvent(Natives.EV_KEYDOWN, DoomTools.KEY_ENTER);
			} else if (controlId.contains("WEAPON1")) {
				Natives.keyEvent(Natives.EV_KEYDOWN, DoomTools.KEY_1);
			} else if (controlId.contains("WEAPON2")) {
				Natives.keyEvent(Natives.EV_KEYDOWN, DoomTools.KEY_2);
			} else if (controlId.contains("WEAPON3")) {
				Natives.keyEvent(Natives.EV_KEYDOWN, DoomTools.KEY_3);
			} else if (controlId.contains("WEAPON4")) {
				Natives.keyEvent(Natives.EV_KEYDOWN, DoomTools.KEY_4);
			} else if (controlId.contains("WEAPON5")) {
				Natives.keyEvent(Natives.EV_KEYDOWN, DoomTools.KEY_5);
			} else if (controlId.contains("WEAPON6")) {
				Natives.keyEvent(Natives.EV_KEYDOWN, DoomTools.KEY_6);
			} else if (controlId.contains("WEAPON7")) {
				Natives.keyEvent(Natives.EV_KEYDOWN, DoomTools.KEY_7);
			} else if (controlId.contains("WEAPON8")) {
				Natives.keyEvent(Natives.EV_KEYDOWN, DoomTools.KEY_8);
			} else if (controlId.contains("WEAPON9")) {
				Natives.keyEvent(Natives.EV_KEYDOWN, DoomTools.KEY_9);
			} else if (controlId.contains("YES")) {
				Natives.keyEvent(Natives.EV_KEYDOWN, DoomTools.KEY_Y);
			} else if (controlId.contains("NO")) {
				Natives.keyEvent(Natives.EV_KEYDOWN, DoomTools.KEY_N);
			} else if (controlId.contains("MAP")) {
				Natives.keyEvent(Natives.EV_KEYDOWN, DoomTools.KEY_Z);
			} else if (controlId.contains("RUN")) {
				CheckedTextView ctv = (CheckedTextView)v;
				ctv.setChecked(!ctv.isChecked());
				if(ctv.isChecked())
					Natives.keyEvent(Natives.EV_KEYDOWN, DoomTools.KEY_RSHIFT);
				else
					Natives.keyEvent(Natives.EV_KEYUP, DoomTools.KEY_RSHIFT);
			}
			
			return true;
		case MotionEvent.ACTION_POINTER_UP:
		case MotionEvent.ACTION_UP:
			pointerIndex = (event.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK)
				>> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
			if(touchedViews[pointerIndex] != null) {
				sb.append(touchedViews[pointerIndex].getKey() + " untouched! [");
				sb.append(pointerIndex); sb.append("]");
				Log.d(TAG, sb.toString());
			} else
				return true;

			v = touchedViews[pointerIndex].getValue();
			controlId = touchedViews[pointerIndex].getKey();
			
			if (controlId.contains("DPAD")) {
				if ((mVstickLastPos & VirtualDPad.POS_UP) != 0)
					Natives.keyEvent(Natives.EV_KEYUP,
							DoomTools.keyCodeToKeySym(KeyEvent.KEYCODE_W));
				if ((mVstickLastPos & VirtualDPad.POS_LEFT) != 0)
					Natives.keyEvent(Natives.EV_KEYUP,
							DoomTools.keyCodeToKeySym(KeyEvent.KEYCODE_A));
				if ((mVstickLastPos & VirtualDPad.POS_DOWN) != 0)
					Natives.keyEvent(Natives.EV_KEYUP,
							DoomTools.keyCodeToKeySym(KeyEvent.KEYCODE_S));
				if ((mVstickLastPos & VirtualDPad.POS_RIGHT) != 0)
					Natives.keyEvent(Natives.EV_KEYUP,
							DoomTools.keyCodeToKeySym(KeyEvent.KEYCODE_D));

				mVstickLastPos = VirtualDPad.POS_CENTER;
				mVstickPosition = VirtualDPad.POS_CENTER;
				setIndicator(-1);
			} else if (controlId.contains("OPEN")) {
				Natives.keyEvent(Natives.EV_KEYUP,
						DoomTools.keyCodeToKeySym(KeyEvent.KEYCODE_Q));
			} else if (controlId.contains("FIRE")) {
				Natives.keyEvent(Natives.EV_KEYUP, DoomTools.KEY_RCTRL);
				Natives.keyEvent(Natives.EV_KEYUP, DoomTools.KEY_ENTER);
			} else if (controlId.contains("TURN")) {
				Natives.keyEvent(Natives.EV_KEYUP, DoomTools.KEY_LEFTARROW);
				Natives.keyEvent(Natives.EV_KEYUP, DoomTools.KEY_RIGHTARROW);
			} else if (controlId.contains("WEAPON1")) {
				Natives.keyEvent(Natives.EV_KEYUP, DoomTools.KEY_1);
			} else if (controlId.contains("WEAPON2")) {
				Natives.keyEvent(Natives.EV_KEYUP, DoomTools.KEY_2);
			} else if (controlId.contains("WEAPON3")) {
				Natives.keyEvent(Natives.EV_KEYUP, DoomTools.KEY_3);
			} else if (controlId.contains("WEAPON4")) {
				Natives.keyEvent(Natives.EV_KEYUP, DoomTools.KEY_4);
			} else if (controlId.contains("WEAPON5")) {
				Natives.keyEvent(Natives.EV_KEYUP, DoomTools.KEY_5);
			} else if (controlId.contains("WEAPON6")) {
				Natives.keyEvent(Natives.EV_KEYUP, DoomTools.KEY_6);
			} else if (controlId.contains("WEAPON7")) {
				Natives.keyEvent(Natives.EV_KEYUP, DoomTools.KEY_7);
			} else if (controlId.contains("WEAPON8")) {
				Natives.keyEvent(Natives.EV_KEYUP, DoomTools.KEY_8);
			} else if (controlId.contains("WEAPON9")) {
				Natives.keyEvent(Natives.EV_KEYUP, DoomTools.KEY_9);
			} else if (controlId.contains("YES")) {
				Natives.keyEvent(Natives.EV_KEYUP, DoomTools.KEY_Y);
			} else if (controlId.contains("NO")) {
				Natives.keyEvent(Natives.EV_KEYUP, DoomTools.KEY_N);
			} else if (controlId.contains("MAP")) {
				Natives.keyEvent(Natives.EV_KEYUP, DoomTools.KEY_Z);
			}
			
			if(pointerIndex == 0) {
				touchedViews[0] = touchedViews[1];
				touchedViews[1] = null;
			} else
				touchedViews[1] = null;
				
			return true;
		case MotionEvent.ACTION_MOVE:
			if(touchedViews[pointerIndex] == null)
				return true;
			//sb.append(touchedViews[pointerIndex].getKey() + " moved! [");
			//sb.append(pointerIndex); sb.append("]");
			//Log.d(TAG, sb.toString());
			v = touchedViews[pointerIndex].getValue();
			controlId = touchedViews[pointerIndex].getKey();
			if (controlId.contains("DPAD")) {
				v.getHitRect(outRect);
				mVstickX = x - outRect.left;
				mVstickY = y - outRect.top;
				mVstickPosition = mVStick.getPosition(mVstickX, mVstickY);
				if (mVstickPosition == mVstickLastPos)
					return true;
				else {
					// get the bits that changed
					int diff = (mVstickPosition ^ mVstickLastPos) & 0x0F;
					// get the bits that changed and are current
					int curr = (mVstickPosition & diff);
					// get the bits that changed and are previous
					int prev = (mVstickLastPos & diff);

					// first check which direction(s) is no longer being
					// pressed from the previous time
					// and send a keyup event
					if ((prev & VirtualDPad.POS_UP) != 0)
						Natives.keyEvent(Natives.EV_KEYUP, DoomTools
								.keyCodeToKeySym(KeyEvent.KEYCODE_W));
					if ((prev & VirtualDPad.POS_LEFT) != 0)
						Natives.keyEvent(Natives.EV_KEYUP, DoomTools
								.keyCodeToKeySym(KeyEvent.KEYCODE_A));
					if ((prev & VirtualDPad.POS_DOWN) != 0)
						Natives.keyEvent(Natives.EV_KEYUP, DoomTools
								.keyCodeToKeySym(KeyEvent.KEYCODE_S));
					if ((prev & VirtualDPad.POS_RIGHT) != 0)
						Natives.keyEvent(Natives.EV_KEYUP, DoomTools
								.keyCodeToKeySym(KeyEvent.KEYCODE_D));
					// now check which direction(s) is current and was not
					// pressed the previous time
					// and send the keydown event.
					if ((curr & VirtualDPad.POS_UP) != 0)
						Natives.keyEvent(Natives.EV_KEYDOWN, DoomTools
								.keyCodeToKeySym(KeyEvent.KEYCODE_W));
					if ((curr & VirtualDPad.POS_LEFT) != 0)
						Natives.keyEvent(Natives.EV_KEYDOWN, DoomTools
								.keyCodeToKeySym(KeyEvent.KEYCODE_A));
					if ((curr & VirtualDPad.POS_DOWN) != 0)
						Natives.keyEvent(Natives.EV_KEYDOWN, DoomTools
								.keyCodeToKeySym(KeyEvent.KEYCODE_S));
					if ((curr & VirtualDPad.POS_RIGHT) != 0)
						Natives.keyEvent(Natives.EV_KEYDOWN, DoomTools
								.keyCodeToKeySym(KeyEvent.KEYCODE_D));
						if (mVstickPosition != mVstickLastPos)
						mVstickLastPos = mVstickPosition;

					setIndicator(mVstickPosition);
				}
			} else if (controlId.contains("TURN")) {
				v.getHitRect(outRect);
				if (x < outRect.left + v.getWidth()/2) {
					Natives.keyEvent(Natives.EV_KEYDOWN, DoomTools.KEY_LEFTARROW);
					Natives.keyEvent(Natives.EV_KEYUP, DoomTools.KEY_RIGHTARROW);
				} else {
					Natives.keyEvent(Natives.EV_KEYUP, DoomTools.KEY_LEFTARROW);
					Natives.keyEvent(Natives.EV_KEYDOWN, DoomTools.KEY_RIGHTARROW);
				}
			} else if (controlId.contains("OPEN")) {
				Natives.keyEvent(Natives.EV_KEYDOWN,
						DoomTools.keyCodeToKeySym(KeyEvent.KEYCODE_Q));
			}
			return true;
		}
		
		return false;
	}
	
	/**
	 * Gets the state of the various game settings in main_screen layout.
	 */
	private void getGameSettings() {
		wadIdx = ((Spinner) this.findViewById(R.id.s_files))
				.getSelectedItemPosition();
		wadName = (String) ((Spinner) this.findViewById(R.id.s_files))
				.getSelectedItem();
		extraArgs = (String) ((EditText) this.findViewById(R.id.arguments))
				.getText().toString();
		mFullscreen = ((CheckBox) this.findViewById(R.id.fullscreen))
				.isChecked();
		mSound = ((CheckBox) this.findViewById(R.id.sound)).isChecked();
		mUseTouchControls = ((CheckBox) this.findViewById(R.id.touch))
				.isChecked();
	}

	/**
	 * Save the sharedPreferences
	 */
	private void savePreferences() {
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putBoolean("fullscreen", mFullscreen);
		editor.putBoolean("sound", mSound);
		editor.putInt("wadIdx", wadIdx);
		editor.putBoolean("prboomcfg", false);
		editor.putString("wadName", wadName);
		editor.putString("extraArgs", extraArgs);
		editor.putBoolean("useTouch", mUseTouchControls);
		// Don't forget to commit your edits!!!
		editor.commit();
	}

	/**
	 * Load the sharedPreferences
	 */
	private void loadPreferences() {
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		wadIdx = settings.getInt("wadIdx", 0);
		mSound = settings.getBoolean("sound", false);
		mFullscreen = settings.getBoolean("fullscreen", false);
		wadName = settings.getString("wadName", "doom1.wad");
		mFirstRun = settings.getBoolean("prboomcfg", true);
		extraArgs = settings.getString("extraArgs", "");
		mUseTouchControls = settings.getBoolean("useTouch", false);
	}

	/**
	 * Pauses the doom engine
	 */
	private void pauseGame() {
		if (mGameStarted) {
			if (!mGamePaused) {
				Natives.keyEvent(Natives.EV_KEYDOWN, DoomTools.KEY_PAUSE);
				Natives.keyEvent(Natives.EV_KEYUP, DoomTools.KEY_PAUSE);
			}
			mGamePaused = true;
			if (mSound) {
				mAudioMgr.setMusicVolume(0);
				mAudioMgr.pauseAudioMgr(true);
			}
		}
	}

	/**
	 * Un-pauses the doom engine
	 */
	private void resumeGame() {
		if (mGamePaused) {
			if (mGameStarted) {
				Natives.keyEvent(Natives.EV_KEYDOWN, DoomTools.KEY_PAUSE);
				Natives.keyEvent(Natives.EV_KEYUP, DoomTools.KEY_PAUSE);
			}
			if (mSound) {
				mAudioMgr.setMusicVolume(100);
				mAudioMgr.pauseAudioMgr(false);
			}
			mGamePaused = false;
		}
	}

	/**
	 * Toggles the on-screen controls on and off
	 * @param enable - if true on-screen controls are visible, else invisible.
	 */
	private void enableDPad(boolean enable) {
		if (enable) {
			this.findViewById(R.id.gameControls).setVisibility(View.VISIBLE);
		} else {
			this.findViewById(R.id.gameControls).setVisibility(View.INVISIBLE);
		}
	}

	/**
	 * Displays what D-Pad position(s) are being pressed
	 * @param status - status of the D-Pad. Found in VirtualDPad class
	 */
	private void setIndicator(int status) {
		ImageView ib = (ImageView) this.findViewById(R.id.indicator);

		if (status == -1)
			ib.setBackgroundResource(R.drawable.dpadindicator_off);
		else
			switch (status) {
			case VirtualDPad.POS_CENTER:
				ib.setBackgroundResource(R.drawable.dpadindicator_center);
				break;

			case VirtualDPad.POS_DOWN:
				ib.setBackgroundResource(R.drawable.dpadindicator_down);
				break;

			case VirtualDPad.POS_DOWN_LEFT:
				ib.setBackgroundResource(R.drawable.dpadindicator_downleft);
				break;

			case VirtualDPad.POS_DOWN_RIGHT:
				ib.setBackgroundResource(R.drawable.dpadindicator_downright);
				break;

			case VirtualDPad.POS_LEFT:
				ib.setBackgroundResource(R.drawable.dpadindicator_left);
				break;

			case VirtualDPad.POS_RIGHT:
				ib.setBackgroundResource(R.drawable.dpadindicator_right);
				break;

			case VirtualDPad.POS_UP:
				ib.setBackgroundResource(R.drawable.dpadindicator_up);
				break;

			case VirtualDPad.POS_UP_LEFT:
				ib.setBackgroundResource(R.drawable.dpadindicator_upleft);
				break;

			case VirtualDPad.POS_UP_RIGHT:
				ib.setBackgroundResource(R.drawable.dpadindicator_upright);
				break;
			}
	}

	/**
	 * Adds all the touchscreen controls to the mOnScreenControls HashMap
	 */
	private void loadOnScreenControls() {
		mOnScreenControls = new HashMap<String, View>();
		mOnScreenControls.put("DPAD", this.findViewById(R.id.vstick));
		mOnScreenControls.put("TURN", this.findViewById(R.id.vstick_lr));
		mOnScreenControls.put("OPEN", this.findViewById(R.id.open));
		mOnScreenControls.put("WEAPON1", this.findViewById(R.id.weapon1));
		mOnScreenControls.put("WEAPON2", this.findViewById(R.id.weapon2));
		mOnScreenControls.put("WEAPON3", this.findViewById(R.id.weapon3));
		mOnScreenControls.put("WEAPON4", this.findViewById(R.id.weapon4));
		mOnScreenControls.put("WEAPON5", this.findViewById(R.id.weapon5));
		mOnScreenControls.put("WEAPON6", this.findViewById(R.id.weapon6));
		mOnScreenControls.put("WEAPON7", this.findViewById(R.id.weapon7));
		mOnScreenControls.put("WEAPON8", this.findViewById(R.id.weapon8));
		mOnScreenControls.put("WEAPON9", this.findViewById(R.id.weapon9));
		mOnScreenControls.put("YES", this.findViewById(R.id.yes));
		mOnScreenControls.put("NO", this.findViewById(R.id.no));
		mOnScreenControls.put("MAP", this.findViewById(R.id.map));
		mOnScreenControls.put("RUN", this.findViewById(R.id.enableRun));
		mOnScreenControls.put("FIRE", this.findViewById(R.id.fireButton));
		
		touchedViews[0] = touchedViews[1] = null;
	}
	
	private void changeFonts(ViewGroup root) {
		Typeface type = Typeface.createFromAsset(this.getAssets(), "fonts/DooM.ttf");
		
		for (int i = 0; i < root.getChildCount(); i++) {
			View v = root.getChildAt(i);
			if (v instanceof FrameLayout || v instanceof RelativeLayout) {
				changeFonts((ViewGroup)v);
			}
			if (v instanceof TextView) {
				((TextView)v).setTypeface(type);
			}
			if (v instanceof EditText) {
				((EditText)v).setTypeface(type);
			}
			if (v instanceof CheckBox) {
				((CheckBox)v).setTypeface(type);
			}
			if (v instanceof Button) {
				((Button)v).setTypeface(type);
			}
			if (v instanceof CheckedTextView) {
				((CheckedTextView)v).setTypeface(type);
			}
		}
	}
	
	/**
	 * adjusts the width and height of the fireButton ImageButton to take up
	 * half of the screen.
	 */
	private void adjustFireButtonSize() {
		ViewGroup.LayoutParams lp = findViewById(R.id.fireButton).getLayoutParams();
		lp.width = mSurfaceWidth / 2;
		lp.height = mSurfaceHeight / 2;
		findViewById(R.id.fireButton).setLayoutParams(lp);
	}
}
