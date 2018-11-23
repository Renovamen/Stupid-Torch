package com.zou.torch;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;


import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class MainActivity extends Activity
{

    // Declaring your view and variables
    private static final String TAG = "MainActivity";
    private static final int ANIMATION_DURATION = 1000;
    private static final int DEMI_DURATION = 500;
	private static MainActivity mActivity;
	private TorchWidgetProvider mWidgetProvider;
    public static boolean mTorchOn;
	private boolean mValue;
	private boolean mLaunch;
    private float mFullScreenScale;
	private ImageView mImageViewShape;
    private ImageView mImageViewOff;
    private ImageView mImageViewOn;
    private Context mContext;
    private SharedPreferences mPreferences;

    //感应器管理器
    private SensorManager sensorManager;
    //光线亮度
    //private TextView light;
    public static float light;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        //当前Activity的内容是一个TextView
        //light = new TextView(this);
        //setContentView(light);
        //获得感应器服务
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        //获得光线感应器
        Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        //注册监听器
        sensorManager.registerListener(listener, sensor, SensorManager. SENSOR_DELAY_NORMAL);


        mActivity = this;
        mContext = this.getApplicationContext();
        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        //设置主题颜色
        setThemeColor();

        setContentView(R.layout.activity_main);

        // 第一次启动时显示欢迎界面
        openFirstDialog();

        //电筒关
        mTorchOn = false;

        mWidgetProvider = TorchWidgetProvider.getInstance();

        mImageViewShape = (ImageView) findViewById(R.id.imageViewShape);
        mImageViewOff = (ImageView) findViewById(R.id.imageViewOff);
        mImageViewOn = (ImageView) findViewById(R.id.imageViewOn);

        startWhenLaunch();

        //短按开启电筒
        mImageViewOff.setOnClickListener(new OnClickListener()
        {
            public void onClick(View v)
            {
                onCreateIntent();
            }
        });

        //长按进入设置界面
        mImageViewOff.setOnLongClickListener(new View.OnLongClickListener()
        {
            @Override
            public boolean onLongClick(View v)
            {
                if (mTorchOn)
                {
                    onCreateIntent();
                    return true;
                }
                else
                {
                    Intent mIntent = new Intent(getApplicationContext(), SettingsActivity.class);
                    startActivity(mIntent);
                    return false;
                }
            }
        });
    }

    private void onCreateIntent()
    {
        Log.d(TAG, "onCreateIntent");
        Intent mIntent = new Intent(TorchSwitch.TOGGLE_FLASHLIGHT);
        mIntent.putExtra("sos", mPreferences.getBoolean(SettingsActivity.KEY_SOS, false));
        mIntent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        getApplicationContext().sendBroadcast(mIntent);
    }

	@Override
    public void onPause()
    {
        updateWidget();
		super.onPause();
        getApplicationContext().unregisterReceiver(mStateReceiver);
    }

	@Override
    public void onResume()
    {
		Log.d(TAG, "onResume");
        updateWidget();
		super.onResume();
        getApplicationContext().registerReceiver(mStateReceiver, new IntentFilter(TorchSwitch.TORCH_STATE_CHANGED));
        setThemeColor();
        setShapeColor();
    }
	
	public void updateWidget()
    {
        this.mWidgetProvider.updateAppWidget(getApplicationContext());
    }

    private void setThemeColor()
    {
        Utils.setMainTheme(mActivity);
    }

    private void setShapeColor()
    {
        String mPrefColor = mPreferences.getString(SettingsActivity.KEY_COLOR, getString(R.string.amber));
        GradientDrawable mShapeDrawable = (GradientDrawable) mImageViewShape.getDrawable();
        mShapeDrawable.setColor(Utils.getPrefColor(this, mPrefColor));
    }

    private void setFlashOn()
    {

        String mPrefColor = mPreferences.getString(SettingsActivity.KEY_COLOR, getString(R.string.amber));
        Boolean mPrefScreen = mPreferences.getBoolean(SettingsActivity.KEY_SCREEN, false);
        Boolean mPrefDevice = mPreferences.getBoolean("mPrefDevice", false);
        Boolean mPrefBright = mPreferences.getBoolean("mPrefBright", false);

        Window mWindow = getWindow();
        WindowManager.LayoutParams mSettings = mWindow.getAttributes();
        GradientDrawable mShape = (GradientDrawable) mImageViewShape.getDrawable();

        if (mImageViewShape == null) return;
        if (!mPrefDevice||mPrefScreen)
        {
            //hideSystemUi(mImageViewOff);
            mWindow.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            mSettings.screenBrightness = 1f; // Set 100% brightness
            if (mPrefBright)
            {
                mShape.setColorFilter(Utils.getPrefColor(mContext, mPrefColor), PorterDuff.Mode.SRC_ATOP);
            }
            else
            {
                mShape.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
            }
            getWindow().setAttributes(mSettings);
            mImageViewOff.animate().alpha(0f).setDuration(DEMI_DURATION);
        }
        else
        {
            mImageViewOn.animate()
                    .alpha(1f)
                    .setStartDelay(DEMI_DURATION)
                    .setDuration(DEMI_DURATION);
            mImageViewOff.animate()
                    .alpha(0f)
                    .setDuration(DEMI_DURATION);
        }
        if (mFullScreenScale <= 0.0f) mFullScreenScale = getMeasureScale();
        mImageViewShape.animate()
                .scaleX(mFullScreenScale)
                .scaleY(mFullScreenScale)
                .setDuration(ANIMATION_DURATION);
    }

    private void setFlashOff()
    {

        Boolean mPrefScreen = mPreferences.getBoolean(SettingsActivity.KEY_SCREEN, false);
        Boolean mPrefDevice = mPreferences.getBoolean("mPrefDevice", false);

        Window mWindow = getWindow();
        WindowManager.LayoutParams mSettings = mWindow.getAttributes();
        GradientDrawable mShape = (GradientDrawable) mImageViewShape.getDrawable();

        if (mImageViewShape == null) return;
        if (!mPrefDevice||mPrefScreen)
        {
            //showSystemUi(mImageViewOff);
            mWindow.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            mSettings.screenBrightness = -1f; // Set automatic brightness
            mShape.clearColorFilter();
            getWindow().setAttributes(mSettings);
        }
        mImageViewShape.animate()
                .scaleX(1)
                .scaleY(1)
                .setDuration(ANIMATION_DURATION);
        mImageViewOn.animate()
                .alpha(0f)
                .setDuration(DEMI_DURATION);
        mImageViewOff.animate()
                .alpha(1f)
                .setStartDelay(DEMI_DURATION)
                .setDuration(DEMI_DURATION);
    }

    /*
    private void hideSystemUi(View v)
    {
        v.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE);
    }

    private void showSystemUi(View v)
    {
        v.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }
*/

    @Override
    public void onAttachedToWindow()
    {
        super.onAttachedToWindow();
        mFullScreenScale = getMeasureScale();
    }

    private BroadcastReceiver mStateReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            if (intent.getAction().equals(TorchSwitch.TORCH_STATE_CHANGED))
            {
                mTorchOn = intent.getIntExtra("state", 0) != 0;
                if (mTorchOn) setFlashOn();
                else setFlashOff();
            }
        }
    };

    private float getMeasureScale()
    {
        WindowManager mWindowManager = getWindowManager();
        Display mDisplay = mWindowManager.getDefaultDisplay();
        DisplayMetrics mDisplayMetrics = new DisplayMetrics();
        mDisplay.getMetrics(mDisplayMetrics);

        float mDisplayHeight = mDisplayMetrics.heightPixels;
        float mDisplayWidth = mDisplayMetrics.widthPixels;
        return (Math.max(mDisplayHeight, mDisplayWidth) /
                getApplicationContext().getResources().getDimensionPixelSize(R.dimen.size_shape)) * 2;
    }

	// Create AlertDialog for the first launch
    private void openFirstDialog()
    {
        mValue = getSharedPreferences("PREFERENCE", MODE_PRIVATE).getBoolean("mValue", true);

        if (mValue)
        {
            AlertDialog.Builder mAlertDialog = new AlertDialog.Builder(MainActivity.this, setThemeDialog());

            mAlertDialog.setTitle(getString(R.string.hello));
            mAlertDialog.setMessage(getString(R.string.first));
            mAlertDialog.setPositiveButton(getString(R.string.okay_ok), new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    Utils.deviceHasCameraFlash(mContext);
                    dialog.dismiss();
                }
            });
            mAlertDialog.show();

            getSharedPreferences("PREFERENCE", MODE_PRIVATE).edit().putBoolean("mValue", false).commit();
        }
    }

    // Switch on the light when app launch
    private void startWhenLaunch()
    {
        //mLaunch = mPreferences.getBoolean(SettingsActivity.KEY_AUTO, false);

        if (mLaunch) onCreateIntent();
    }

    private int setThemeDialog()
    {

        int mTheme;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) mTheme = R.style.MaterialDialog;
        else mTheme = R.style.HoloDialog;
        return mTheme;
    }
	
	public static MainActivity getInstance()
    {
        return mActivity;
    }

    //Activity被销毁
    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        //注销监听器
        if (sensorManager != null)
        {
            sensorManager.unregisterListener(listener);
        }
    }

    //感应器事件监听器
    private SensorEventListener listener = new SensorEventListener()
    {

        //当感应器精度发生变化
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy)
        {
            //当传感器精度改变时回调该方法，一般无需处理
        }

        //当传感器监测到的数值发生变化时
        @Override
        public void onSensorChanged(SensorEvent event)
        {
            // values数组中第一个值就是当前的光照强度
            float value = event.values[0];
            //light.setText("当前亮度 " + value + " lx(勒克斯)");
            light = value;
        }

    };

    public float lightValue()
    {
        return light;
    }

}
