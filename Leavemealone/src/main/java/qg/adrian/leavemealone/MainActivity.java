package qg.adrian.leavemealone;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.drawable.BitmapDrawable;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class MainActivity extends Activity {

    /**
     * String constants
     */
    private static String
            PREFNAME = "lma",
            HOURS = "hours",
            MINUTES = "minutes",
            SECONDS = "seconds",
            VIBRATE = "vibrate",
            RINGCALLS = "ringcalls",
            NOTIFICATION = "notification",
            SHOW_TIP = "show tip",
            CANCEL_COUNTDOWN = "cancel countdown";

    private static int CANCEL_TIMER = 0;

    /**
     * Animation and UI elements
     */

    private ObjectAnimator mProgressBarAnimator;
    private HoloCircularProgressBar progressBar;
    private boolean mAnimationHasEnded = true;
    private Button startButton;
    private TextView labelTime;
    private SeekBar sh, sm, ss; // sekbars (hours, minutees, seconds)
    private TextView lblh, lblm, lbls; // label seekbars (hours, minutees, seconds)

    private SharedPreferences preferences;

    /**
     * Gesture detector because of layout transition
     */

    private GestureDetector gesturedetector = null;

    private TelephonyManager manager;
    private AudioManager audioManager;

    /**
     * Layout references
     */
    RelativeLayout relativeLayout_1_main, relativeLayout_2_settings, relativeLayout_3_tip;
    LinearLayout layout_1_presets;

    /**
     * Dates
     */

    DateFormat outFormat = new SimpleDateFormat("HH:mm:ss");

    /**
     * Notifications variables
     */

    private NotificationManager notificationManager = null;
    private int NOTIFICATION_ID = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /**
         * Relatives layouts for swipping between the layouts at the fragment parent
         */

        relativeLayout_1_main = (RelativeLayout) findViewById(R.id.layoutR);
        relativeLayout_2_settings = (RelativeLayout) findViewById(R.id.layout_settings);
        relativeLayout_3_tip = (RelativeLayout) findViewById(R.id.relativeTip);
        layout_1_presets = (LinearLayout) findViewById(R.id.presets_layout);

        /**
         * Shared preferences for obtain and save settings
         */
        preferences = getSharedPreferences(PREFNAME, MODE_PRIVATE);

        /**
         * Set relative layout for tip for settings. Show first time, then nevermore
         */
        if (preferences.getBoolean(SHOW_TIP, true)) {
            relativeLayout_3_tip.setVisibility(View.VISIBLE);
            relativeLayout_3_tip.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Animation animation =
                            AnimationUtils.loadAnimation(MainActivity.this,
                                    android.R.anim.fade_out);
                    relativeLayout_3_tip.startAnimation(animation);
                    relativeLayout_3_tip.setVisibility(View.GONE);
                    preferences.edit().putBoolean(SHOW_TIP, false).commit();
                }
            });
        }

        /**
         * Obtain first seekbars to disable them
         */

        sh = (SeekBar) findViewById(R.id.seekBar);
        sh.setProgress(preferences.getInt(HOURS, 0));
        sm = (SeekBar) findViewById(R.id.seekBar2);
        sm.setProgress(preferences.getInt(MINUTES, 0));
        ss = (SeekBar) findViewById(R.id.seekBar3);
        ss.setProgress(preferences.getInt(SECONDS, 0));
        lblh = (TextView) findViewById(R.id.labelHours);
        lblh.setText(String.valueOf(sh.getProgress()));
        lblm = (TextView) findViewById(R.id.labelMinutes);
        lblm.setText(String.valueOf(sm.getProgress()));
        lbls = (TextView) findViewById(R.id.labelSeconds);
        lbls.setText(String.valueOf(ss.getProgress()));

        /**
         * Workaround for "align" text at settings. This is, for "mode" and "time" titles
         */

        TextView l1 = (TextView) findViewById(R.id.label_settings_mode);
        l1.setText(l1.getText().toString() + "  "); // 2 spaces
        l1 = (TextView) findViewById(R.id.label_settings_time);
        l1.setText(l1.getText().toString() + "  ");

        /**
         * Work with Date
         */
        outFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        /**
         * Gesture detector:
         *      Detects the gestures "sent". This is, obtain the desired layout,
         *      attach a listener and send a concrete event,
         *      so gesture detector will know which gesture is and do what he has to do.
         *      The goal is have a Fragment with two different layouts,
         *      and use animations to show and hide them, as slicing tabs.
         */
        gesturedetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            private static final int SWIPE_MIN_DISTANCE = 20;

            private static final int SWIPE_MAX_OFF_PATH = 100;

            private static final int SWIPE_THRESHOLD_VELOCITY = 100;

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2,
                                   float velocityX, float velocityY) {

                float
                        dX = e2.getX() - e1.getX(),
                        dY = e1.getY() - e2.getY();

                if (Math.abs(dY) < SWIPE_MAX_OFF_PATH
                        && Math.abs(velocityX) >= SWIPE_THRESHOLD_VELOCITY
                        && Math.abs(dX) >= SWIPE_MIN_DISTANCE) {

                    if (dX < 0) {
                        if (relativeLayout_2_settings.getVisibility() == View.GONE) {
                            Animation fadeInAnimation =
                                    AnimationUtils.loadAnimation(MainActivity.this,
                                            R.anim.slide_right_in);
                            sh.setEnabled(true);
                            sm.setEnabled(true);
                            ss.setEnabled(true);
                            relativeLayout_2_settings.startAnimation(fadeInAnimation);
                            relativeLayout_2_settings.setVisibility(View.VISIBLE);
                            Animation animation =
                                    AnimationUtils.loadAnimation(MainActivity.this,
                                            android.R.anim.fade_out);
                            relativeLayout_1_main.startAnimation(animation);
                            relativeLayout_1_main.setVisibility(View.GONE);
                        }
                    } else {

                        if (relativeLayout_2_settings.getVisibility() == View.VISIBLE) {
                            Animation fadeInAnimation =
                                    AnimationUtils.loadAnimation(MainActivity.this,
                                            R.anim.slide_left_out);
                            sh.setEnabled(false);
                            sm.setEnabled(false);
                            ss.setEnabled(false);
                            relativeLayout_2_settings.startAnimation(fadeInAnimation);
                            relativeLayout_2_settings.setVisibility(View.GONE);
                            Animation animation =
                                    AnimationUtils.loadAnimation(MainActivity.this,
                                            android.R.anim.fade_in);
                            relativeLayout_1_main.startAnimation(animation);
                            relativeLayout_1_main.setVisibility(View.VISIBLE);
                        }

                    }

                    return true;

                } else if (Math.abs(dX) < SWIPE_MAX_OFF_PATH
                        && Math.abs(velocityY) >= SWIPE_THRESHOLD_VELOCITY
                        && Math.abs(dY) >= SWIPE_MIN_DISTANCE) {
                    return true;
                }
                return false;
            }

        });

        FrameLayout frameLayout = (FrameLayout) findViewById(R.id.layout_fragment);
        frameLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                gesturedetector.onTouchEvent(event);
                return true;
            }
        });

        /**
         * Adjust progress bar. Also label for time
         */
        progressBar = (HoloCircularProgressBar) findViewById(R.id.progressBar);
        labelTime = (TextView) findViewById(R.id.timeText);

        progressBar.setProgress(0); // Establecemos los valores por defecto para la progress bar
        progressBar.setMarkerProgress(0f);
        // Obtain seekbars values, convert to millis,
        // add pepper, cook for 20 minutes.
        updateTimeLabel();

//        /**
//         * Getting textview and button
//         */
//        labelTime = (TextView) findViewById(R.id.timeText);
////        labelTime.setText(String.valueOf(getTime()));
//        labelTime.setText(outFormat.format(new Date(getTime() + 1000)));

        /**
         * Seekbars. Obtain them (and set current value, if exists), also their labels.
         * Listener for changes values, update preferences also.
         */


        sh.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                lblh.setText(String.valueOf(progress));
                preferences.edit().putInt(HOURS, progress).commit();
                updateTimeLabel();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        sm.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                lblm.setText(String.valueOf(progress));
                preferences.edit().putInt(MINUTES, progress).commit();
                updateTimeLabel();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        ss.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                lbls.setText(String.valueOf(progress));
                preferences.edit().putInt(SECONDS, progress).commit();
                updateTimeLabel();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        /**
         * Checkboxes, save settings when listener is called
         */

        CheckBox checkBoxVibrate = (CheckBox) findViewById(R.id.checkBoxVibrate);
        checkBoxVibrate.setChecked(preferences.getBoolean(VIBRATE, false));
        checkBoxVibrate.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                preferences.edit().putBoolean(VIBRATE, isChecked).commit();
            }
        });
        CheckBox checkBoxRingcalls = (CheckBox) findViewById(R.id.checkBoxRing);
        checkBoxRingcalls.setChecked(preferences.getBoolean(RINGCALLS, false));
        checkBoxRingcalls.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                preferences.edit().putBoolean(RINGCALLS, isChecked).commit();
            }
        });

        /**
         * Listener creation and counter start
         */

        startButton = (Button) findViewById(R.id.startButton);

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AnimatorListener animationListener = new AnimatorListener() {

                    @Override
                    public void onAnimationStart(Animator animation) {
//                        SystemManager.setSilent(getApplicationContext(), false);
//                        SystemManager.doWhatYouHaveToDo(getApplicationContext(), preferences);
                        silentPhone(getApplicationContext());
                        mAnimationHasEnded = false;
                        progressBar.setProgress(1); // Load progress bar for animation start
                        startButton.setText("Stop");
                        startButton.setTextColor(getResources()
                                .getInteger(android.R.color.holo_red_dark));
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        progressBar.setProgress(0);
                        if (notificationManager != null)
                            notificationManager.cancel(NOTIFICATION_ID);
                        mAnimationHasEnded = true;
                        startButton.setText("Start");
                        startButton.setTextColor(getResources()
                                .getInteger(android.R.color.holo_green_dark));
                        updateTimeLabel();

//                        SystemManager.returnVibrate(getApplicationContext());
                        restartPhoneVolume();
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                        animation.end();
                        progressBar.setProgress(0);
                        mAnimationHasEnded = true;
                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {
                    }
                };

                // Button click jobs
                if (mAnimationHasEnded) {
                    animate(progressBar, animationListener, 1, getTime());

                    // NOTIFICATION stuff. Create with intent filter,
                    // to get it later from the broadcast receiver
                    Intent intent = new Intent(CANCEL_COUNTDOWN);
                    PendingIntent pendingIntent =
                            PendingIntent.getBroadcast(getApplicationContext(), 0,
                                    intent, PendingIntent.FLAG_UPDATE_CURRENT);

                    // TODO: Do this more readable, it's large as hell
                    NotificationCompat.Builder builder =
                            new NotificationCompat.Builder(getApplicationContext())
                                    .setSmallIcon(R.drawable.ic_launcher)
                                    .setLargeIcon((((BitmapDrawable) getApplicationContext()
                                            .getResources().getDrawable(R.drawable.ic_launcher))
                                            .getBitmap())).setContentTitle(getApplicationContext()
                                    .getString(R.string.app_name))
                                    .setContentText(getResources().getString(R.string.tapCancel))
                                    .addAction(android.R.drawable.ic_menu_close_clear_cancel,
                                            getResources().getString(R.string.cancelTimer),
                                            pendingIntent
                                    );

                    builder.setContentIntent(pendingIntent);
                    notificationManager =
                            (NotificationManager) getApplicationContext()
                                    .getSystemService(Context.NOTIFICATION_SERVICE);
                    notificationManager.notify(NOTIFICATION_ID, builder.build());

                } else {
                    deAnimate(progressBar);
                    if (notificationManager != null) notificationManager.cancel(NOTIFICATION_ID);
                }
            }
        });

        /**
         * Broadcast
         */
        // Broadcast receiver for receive the pending intent filter, to know what's happening
        // Get action, and dismiss countdown
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent != null && intent.getAction().equalsIgnoreCase(CANCEL_COUNTDOWN)) {
                    deAnimate(progressBar);
                    if (notificationManager != null) notificationManager.cancel(NOTIFICATION_ID);
                }
            }
        };
        IntentFilter filter = new IntentFilter(CANCEL_COUNTDOWN);
        registerReceiver(receiver, filter);
    }


    /**
     * Values of the seekbars to establish time
     *
     * @return Time in millis from seekbars settings
     */

    private int getTime() {
        SeekBar s1 = (SeekBar) findViewById(R.id.seekBar);
        int temp = (s1.getProgress() * 60 * 60 * 1000);
        SeekBar s2 = (SeekBar) findViewById(R.id.seekBar2);
        temp += (s2.getProgress() * 60 * 1000);
        SeekBar s3 = (SeekBar) findViewById(R.id.seekBar3);
        temp += (s3.getProgress() * 1000);

        return temp;
    }

    private void updateTimeLabel() {
        int time = preferences.getInt(HOURS, 0) * 60 * 60 * 1000;
        time += preferences.getInt(MINUTES, 0) * 60 * 1000;
        time += preferences.getInt(SECONDS, 0) * 1000;
        // Make date and that stuff, format it and roll it out to the string
        Date d = new Date(time);
        labelTime.setText(outFormat.format(d));
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }


    /**
     * Animate.
     *
     * @param progressBar the progress bar
     * @param listener    the listener
     */
    private void animate(final HoloCircularProgressBar progressBar,
                         final AnimatorListener listener) {
        final float progress = (float) (Math.random() * 2);
        int duration = 3000;
        animate(progressBar, listener, progress, duration);
    }

    /**
     * @param progressBar the progress bar
     * @param listener    the listener
     * @param progress    The progress step of the bar
     * @param duration    Duration (in ms) of the progress bar animation
     */
    private void animate(final HoloCircularProgressBar progressBar,
                         final AnimatorListener listener, final float progress,
                         final int duration) {

        mProgressBarAnimator = ObjectAnimator.ofFloat(progressBar, "progress", progress);
        mProgressBarAnimator.setDuration(duration);

        if (listener != null) {
            mProgressBarAnimator.addListener(listener);
        }
        mProgressBarAnimator.reverse();
        mProgressBarAnimator.addUpdateListener(new AnimatorUpdateListener() {

            @Override
            public void onAnimationUpdate(final ValueAnimator animation) {
                progressBar.setProgress((Float) animation.getAnimatedValue());

                /**
                 * Update time for progress bar label time
                 */

                long millis = (Long) animation.getCurrentPlayTime();
                Date d = new Date(duration - millis + 1000);
                labelTime.setText(outFormat.format(d));
            }
        });
        progressBar.setMarkerProgress(progress);
        mProgressBarAnimator.start();
    }

    private void deAnimate(final HoloCircularProgressBar progressBar) {
        mProgressBarAnimator.cancel();
        mAnimationHasEnded = true;
        startButton.setText("Start");
        progressBar.setProgress(0);
    }

    private void silentPhone(final Context context) {

        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        if (preferences.getBoolean(VIBRATE, false))
            audioManager.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
        else
            audioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);

        // Ringcalls management
        if (preferences.getBoolean(RINGCALLS, false)) {
            manager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            manager.listen(new PhoneStateListener() {
                @Override
                public void onCallStateChanged(int state, String incomingNumber) {
                    if (state == TelephonyManager.CALL_STATE_RINGING) {
                        // Phone working, move to normal mode
                        audioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
                    }
                    if (state == TelephonyManager.CALL_STATE_IDLE && !mAnimationHasEnded) {
                        AudioManager audioManager =
                                (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                        // Return default app state
                        if (preferences.getBoolean(MainActivity.VIBRATE, false))
                            audioManager.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
                        else audioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
                    }
                    super.onCallStateChanged(state, incomingNumber);
                }
            }, PhoneStateListener.LISTEN_CALL_STATE);
        }
    }

    private void restartPhoneVolume() {
        manager = null;
        audioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
    }

    /**
     * Workaround because the damn scrollbar for small screens.
     * Can't swipe right to go back to main layout
     */
    @Override
    public void onBackPressed() {
        if (relativeLayout_2_settings.getVisibility() == View.VISIBLE) {
            Animation fadeInAnimation =
                    AnimationUtils.loadAnimation(MainActivity.this, R.anim.slide_left_out);
            relativeLayout_2_settings.startAnimation(fadeInAnimation);
            relativeLayout_2_settings.setVisibility(View.GONE);
            Animation animation =
                    AnimationUtils.loadAnimation(MainActivity.this, android.R.anim.fade_in);
            relativeLayout_1_main.startAnimation(animation);
            relativeLayout_1_main.setVisibility(View.VISIBLE);
        } else if (layout_1_presets.getVisibility() == View.VISIBLE) {
            Animation fadeInAnimation =
                    AnimationUtils.loadAnimation(MainActivity.this, R.anim.slide_left_in);
            layout_1_presets.startAnimation(fadeInAnimation);
            layout_1_presets.setVisibility(View.GONE);
            Animation animation =
                    AnimationUtils.loadAnimation(MainActivity.this, android.R.anim.fade_in);
            relativeLayout_1_main.startAnimation(animation);
            relativeLayout_1_main.setVisibility(View.VISIBLE);
        } else super.onBackPressed(); // If not back to main, make the default action
    }

}