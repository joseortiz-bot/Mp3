package com.ldt.musicr.ui;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import androidx.annotation.NonNull;

import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;


import com.ldt.musicr.helper.LocaleHelper;
import com.ldt.musicr.helper.songpreview.SongPreviewController;
import com.ldt.musicr.loader.medialoader.PaletteGeneratorTask;
import com.ldt.musicr.service.MusicPlayerRemote;
import com.ldt.musicr.service.MusicServiceEventListener;

import com.ldt.musicr.service.MusicService;
import com.ldt.musicr.util.Tool;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Locale;

/**
 *  Create relationship between Activity and Music Player Service
 */

public abstract class BaseActivity extends AppCompatActivity implements MusicServiceEventListener {
    private static final String TAG = "BaseActivity";

    private final ArrayList<MusicServiceEventListener> mMusicServiceEventListeners = new ArrayList<>();

    private MusicPlayerRemote.ServiceToken serviceToken;
    private MusicStateReceiver musicStateReceiver;
    private boolean receiverRegistered;

    private SongPreviewController mSongPreviewController = null;

    public SongPreviewController getSongPreviewController() {
        return mSongPreviewController;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(createBundleNoFragmentRestore(savedInstanceState));
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        serviceToken = MusicPlayerRemote.bindToService(this, new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                BaseActivity.this.onServiceConnected();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                BaseActivity.this.onServiceDisconnected();
            }
        });

        if(mSongPreviewController==null) mSongPreviewController = new SongPreviewController();
        addMusicServiceEventListener(mSongPreviewController);
    }

    /**
     * Improve bundle to prevent restoring of fragments.
     * @param bundle bundle container
     * @return improved bundle with removed "fragments parcelable"
     */
    private static Bundle createBundleNoFragmentRestore(Bundle bundle) {
        if (bundle != null) {
            bundle.remove("android:support:fragments");
        }
        return bundle;
    }

    @Override
    protected void onDestroy() {
        if(mPaletteGeneratorTask!=null) mPaletteGeneratorTask.cancel();
        mPaletteGeneratorTask = null;

        if(mSongPreviewController!=null) mSongPreviewController.destroy();

        MusicPlayerRemote.unbindFromService(serviceToken);
        if (receiverRegistered) {
            unregisterReceiver(musicStateReceiver);
            receiverRegistered = false;
        }

        removeAllMusicServiceEventListener();
        super.onDestroy();
    }

    public void addMusicServiceEventListener(final MusicServiceEventListener listener) {
        if (listener != null) {
            mMusicServiceEventListeners.add(listener);
        }
    }

    public void removeMusicServiceEventListener(final MusicServiceEventListener listener) {
        if (listener != null) {
            mMusicServiceEventListeners.remove(listener);
        }
    }
    public void removeAllMusicServiceEventListener() {
        mMusicServiceEventListeners.clear();
    }

    @Override
    public void onServiceConnected() {
        if (!receiverRegistered) {
            musicStateReceiver = new MusicStateReceiver(this);

            final IntentFilter filter = new IntentFilter();
            filter.addAction(MusicService.PLAY_STATE_CHANGED);
            filter.addAction(MusicService.SHUFFLE_MODE_CHANGED);
            filter.addAction(MusicService.REPEAT_MODE_CHANGED);
            filter.addAction(MusicService.META_CHANGED);
            filter.addAction(MusicService.QUEUE_CHANGED);
            filter.addAction(MusicService.MEDIA_STORE_CHANGED);
            filter.addAction(PaletteGeneratorTask.PALETTE_ACTION);

            registerReceiver(musicStateReceiver, filter);

            receiverRegistered = true;
        }

        for (MusicServiceEventListener listener : mMusicServiceEventListeners) {
            if (listener != null) {
                listener.onServiceConnected();
            }
        }
    }

    @Override
    public void onServiceDisconnected() {
        if (receiverRegistered) {
            unregisterReceiver(musicStateReceiver);
            receiverRegistered = false;
        }

        for (MusicServiceEventListener listener : mMusicServiceEventListeners) {
            if (listener != null) {
                listener.onServiceDisconnected();
            }
        }
    }

    PaletteGeneratorTask mPaletteGeneratorTask = null;

    @Override
    public void onPlayingMetaChanged() {
        refreshPalette();
        for (MusicServiceEventListener listener : mMusicServiceEventListeners) {
            if (listener != null) {
                listener.onPlayingMetaChanged();
            }
        }
    }

    public void refreshPalette() {
        if(mPaletteGeneratorTask!=null) mPaletteGeneratorTask.cancel();
        mPaletteGeneratorTask = new PaletteGeneratorTask(getApplicationContext());
        mPaletteGeneratorTask.execute();
    }

    @Override
    public void onQueueChanged() {
        for (MusicServiceEventListener listener : mMusicServiceEventListeners) {
            if (listener != null) {
                listener.onQueueChanged();
            }
        }
    }

    @Override
    public void onPlayStateChanged() {
        for (MusicServiceEventListener listener : mMusicServiceEventListeners) {
            if (listener != null) {
                listener.onPlayStateChanged();
            }
        }
    }

    @Override
    public void onMediaStoreChanged() {
        for (MusicServiceEventListener listener : mMusicServiceEventListeners) {
            if (listener != null) {
                listener.onMediaStoreChanged();
            }
        }
    }

    @Override
    public void onRepeatModeChanged() {
        for (MusicServiceEventListener listener : mMusicServiceEventListeners) {
            if (listener != null) {
                listener.onRepeatModeChanged();
            }
        }
    }

    @Override
    public void onShuffleModeChanged() {
        for (MusicServiceEventListener listener : mMusicServiceEventListeners) {
            if (listener != null) {
                listener.onShuffleModeChanged();
            }
        }
    }

    @Override
    public void onPaletteChanged() {
        for(MusicServiceEventListener listener : mMusicServiceEventListeners) {
            if(listener != null) {
                listener.onPaletteChanged();
            }
        }
    }

    private static final class MusicStateReceiver extends BroadcastReceiver {

        private final WeakReference<BaseActivity> reference;

        public MusicStateReceiver(final BaseActivity activity) {
            reference = new WeakReference<>(activity);
        }

        @Override
        public void onReceive(final Context context, @NonNull final Intent intent) {
            final String action = intent.getAction();
            BaseActivity activity = reference.get();
            if (activity != null&&action!=null) {
                switch (action) {
                    case MusicService.META_CHANGED:
                        activity.onPlayingMetaChanged();
                        break;
                    case MusicService.QUEUE_CHANGED:
                        activity.onQueueChanged();
                        break;
                    case MusicService.PLAY_STATE_CHANGED:
                        activity.onPlayStateChanged();
                        break;
                    case MusicService.REPEAT_MODE_CHANGED:
                        activity.onRepeatModeChanged();
                        break;
                    case MusicService.SHUFFLE_MODE_CHANGED:
                        activity.onShuffleModeChanged();
                        break;
                    case MusicService.MEDIA_STORE_CHANGED:
                        activity.onMediaStoreChanged();
                        break;
                    case PaletteGeneratorTask.PALETTE_ACTION:
                         if(intent.getBooleanExtra(PaletteGeneratorTask.RESULT,false)) {
                             Log.d(TAG, "onReceive: PaletteGeneratorTask true");
                             Tool.ColorOne = intent.getIntExtra(PaletteGeneratorTask.COLOR_ONE,Tool.ColorOne);
                             Tool.ColorTwo = intent.getIntExtra(PaletteGeneratorTask.COLOR_TWO,Tool.ColorTwo);
                             Tool.AlphaOne = intent.getFloatExtra(PaletteGeneratorTask.ALPHA_ONE,Tool.AlphaOne);
                             Tool.AlphaTwo = intent.getFloatExtra(PaletteGeneratorTask.ALPHA_TWO,Tool.AlphaTwo);

                         } else Log.d(TAG, "onReceive: PaletteGeneratorTask false");
                        activity.onPaletteChanged();
                }
            }
        }
    }


public void addMusicServiceEventListener(final MusicServiceEventListener listener, boolean firstIndex) {
        if (listener == this) {
            throw new UnsupportedOperationException("Override the method, don't add a listener");
        }

        if (listener != null) {
            mMusicServiceEventListeners.add(0,listener);
        }
    }


    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(updateBaseContextLocale(base));
    }

    private Context updateBaseContextLocale(Context context) {
        String language = LocaleHelper.getLanguage(context); // Helper method to get saved language from SharedPreferences
        Locale locale = new Locale(language);
        Locale.setDefault(locale);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return updateResourcesLocale(context, locale);
        }

        return updateResourcesLocaleLegacy(context, locale);
    }

    @TargetApi(Build.VERSION_CODES.N)
    private Context updateResourcesLocale(Context context, Locale locale) {
        Configuration configuration = context.getResources().getConfiguration();
        configuration.setLocale(locale);
        return context.createConfigurationContext(configuration);
    }

    @SuppressWarnings("deprecation")
    private Context updateResourcesLocaleLegacy(Context context, Locale locale) {
        Resources resources = context.getResources();
        Configuration configuration = resources.getConfiguration();
        configuration.locale = locale;
        resources.updateConfiguration(configuration, resources.getDisplayMetrics());
        return context;
    }

}
