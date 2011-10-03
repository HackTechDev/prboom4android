package android.game.prboom;

import android.app.Activity;
import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

public class HUDToast {
	public static final int TT_SHORT_DELAY = 0;
	public static final int TT_LONG_DELAY = 1;
	public static final int TT_COLOR_RED = 2;
	public static final int TT_COLOR_GREEN = 4;
	public static final int TT_COLOR_BLUE = 8;
	public static final int TT_COLOR_YELLOW = 6;
	public static final int TT_COLOR_MAGENTA = 10;
	public static final int TT_COLOR_CYAN = 12;
	public static final int TT_COLOR_WHITE = 14;
	
	private View mToastView = null;
	private Context mContext = null;
	private Toast mToast = null;
	private TextView mToastText = null;
	
	public HUDToast(Activity activity) {
		this.mContext = activity.getApplicationContext();
		this.mToast = Toast.makeText(mContext, "", Toast.LENGTH_SHORT);
		
		LayoutInflater inflater = activity.getLayoutInflater();
		mToastView = inflater.inflate(R.layout.toast_layout,
		                               (ViewGroup) activity.findViewById(R.id.toast_layout_root));
		
		mToastText = (TextView)mToastView.findViewById(R.id.text);
		mToastText.setText("");
		setTextColor(TT_COLOR_RED);
		mToast = new Toast(mContext);
		mToast.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM, 0, -5);
		mToast.setDuration(Toast.LENGTH_SHORT);
		mToast.setView(mToastView);
	}
	
	private void setTextColor(int tt_color) {
		int color = 0;
		switch(tt_color) {
		case TT_COLOR_RED:
			color = 0xFFFF0000;
			break;
		case TT_COLOR_GREEN:
			color = 0xFF00FF00;
			break;
		case TT_COLOR_BLUE:
			color = 0xFF0000FF;
			break;
		case TT_COLOR_YELLOW:
			color = 0xFFFFFF00;
			break;
		case TT_COLOR_MAGENTA:
			color = 0xFFFF00FF;
			break;
		case TT_COLOR_CYAN:
			color = 0xFF00FFFF;
			break;
		case TT_COLOR_WHITE:
		default:
			color = 0xFFFFFFFF;
			break;
		}
		
		mToastText.setTextColor(color);
	}
	
	public void show(String text, int toastType) {
		mToast.cancel();
		mToast.setDuration(toastType & 1);
		setTextColor(toastType & ~1);
		mToastText.setText(text);
		mToast.show();
	}
}
