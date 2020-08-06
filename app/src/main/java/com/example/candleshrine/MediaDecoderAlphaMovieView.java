/*
 * Copyright (C) 2020 Kevin Higgins
 * @author Kevin Higgins
 * This file is a modified version of Pavel Semak's AlphaMovieView class. I
 * changed it to use the MediaCodecWrapper class instead of a MediaPlayer from
 * the standard library. I removed functionality to set up and manage the state of
 *  the MediaPlayer and I also removed the TimeAnimator, because I want the timing
 * coordinated from outside in order to keep three layers of flame together. Finally
 * I added some debug logging for learning purposes.
 * Kevin Higgins 05/08/20
 *
 * Copyright 2017 Pavel Semak
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.candleshrine;
import android.animation.TimeAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;
import android.view.View;

import androidx.annotation.RequiresApi;

import java.io.IOException;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
@SuppressLint("ViewConstructor")
public class MediaDecoderAlphaMovieView extends GLTextureView {


    private static final int GL_CONTEXT_VERSION = 2;

    private static final int NOT_DEFINED = -1;
    private static final int NOT_DEFINED_COLOR = 0;

    private static final String TAG = "MDAMV";

    private static final float VIEW_ASPECT_RATIO = 4f / 3f;
    private float videoAspectRatio = VIEW_ASPECT_RATIO;
    Surface s;
    VideoRenderer renderer;
    private MediaCodecWrapper decoder;
    private MediaExtractor extractor;
    public TimeAnimator time;
    private readyListener mediaDecoderAlphaMovieViewReadyListener;

    private OnVideoStartedListener onVideoStartedListener;
    private OnVideoEndedListener onVideoEndedListener;

    private boolean isSurfaceCreated;
    private boolean isDataSourceSet;
    private boolean extractorReady = false;

    private PlayerState state = PlayerState.NOT_PREPARED;

    public MediaDecoderAlphaMovieView(Context context, AttributeSet attrs) {
        super(context, attrs);

        if (!isInEditMode()) {
            System.out.println("Constructing");
            extractor = new MediaExtractor();
            time = new TimeAnimator();
            init(attrs);
            //initMediaPlayer();
        }
    }


    private void init(AttributeSet attrs) {
        setEGLContextClientVersion(GL_CONTEXT_VERSION);
        setEGLConfigChooser(8, 8, 8, 8, 16, 0);



        renderer = new VideoRenderer();

        obtainRendererOptions(attrs);



        setRenderer(renderer);
        this.addOnSurfacePrepareListener();
        bringToFront();
        setPreserveEGLContextOnPause(true);
        setOpaque(false);

    }



    private void obtainRendererOptions(AttributeSet attrs) {
        //System.out.println("Yozers");
        if (attrs != null) {
            TypedArray arr = getContext().obtainStyledAttributes(attrs, R.styleable.MediaDecoderAlphaMovieView);
            int alphaColor = arr.getColor(R.styleable.MediaDecoderAlphaMovieView_alphaColor, NOT_DEFINED_COLOR);
            if (alphaColor != NOT_DEFINED_COLOR) {
                renderer.setAlphaColor(alphaColor);
            }
            String shader = arr.getString(R.styleable.MediaDecoderAlphaMovieView_shader);
            if (shader != null) {
                renderer.setCustomShader(shader);
                Log.d(TAG, "Setting shader " + shader);
            }
            else {
                Log.d(TAG, "Shader null.");
            }
            float accuracy = arr.getFloat(R.styleable.MediaDecoderAlphaMovieView_accuracy, NOT_DEFINED);
            if (accuracy != NOT_DEFINED) {
                renderer.setAccuracy(accuracy);
            }
            arr.recycle();
        }
    }

    private void addOnSurfacePrepareListener() {
        Log.d(TAG, "Kevin debug - Attempting to add surface prepare listener");
        if (renderer != null) {
            renderer.setOnSurfacePrepareListener(new VideoRenderer.OnSurfacePrepareListener() {
                @Override
                public void surfacePrepared(Surface surface) {
                    isSurfaceCreated = true;
                    s = surface;

                    if (isDataSourceSet) {
                        onReady();
                    }
                }
            });
            Log.d(TAG, "Kevin debug - Added surface prepare listener");
        }
    }



    private void calculateVideoAspectRatio(int videoWidth, int videoHeight) {
        if (videoWidth > 0 && videoHeight > 0) {
            videoAspectRatio = (float) videoWidth / videoHeight;
        }

        requestLayout();
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = View.MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = View.MeasureSpec.getMode(heightMeasureSpec);
        int widthSize = View.MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = View.MeasureSpec.getSize(heightMeasureSpec);

        double currentAspectRatio = (double) widthSize / heightSize;
        if (currentAspectRatio > videoAspectRatio) {
            widthSize = (int) (heightSize * videoAspectRatio);
        } else {
            heightSize = (int) (widthSize / videoAspectRatio);
        }

        super.onMeasure(View.MeasureSpec.makeMeasureSpec(widthSize, widthMode),
                View.MeasureSpec.makeMeasureSpec(heightSize, heightMode));
    }

    public void setVideoFromUri(Context context, Uri uri) {
        reset();
        try {
            extractor.setDataSource(getContext(), uri, null);
        } catch (IOException e) {
            e.printStackTrace();
        }
        extractor.selectTrack(0);
        extractorReady = true;

        isDataSourceSet = true;
        Log.d(TAG, "Kevin debug - Set video from URI successful.");
        if (isSurfaceCreated) {
            Log.d(TAG, "Kevin debug - Surface created so calling onReady().");
            onReady();
        }
        else {
            Log.d(TAG, "Kevin debug - Surface not created yet so waiting on surface created listener.");
        }

    }



        //time.setTimeListener(new TimeAnimator.TimeListener() {
        //    boolean extractorReady = true;
        //    @Override
    public void onTimeUpdate(TimeAnimator animation, long totalTime, long deltaTime) {



        boolean isEos =
                (extractor.getSampleFlags() & MediaCodec.BUFFER_FLAG_END_OF_STREAM) == MediaCodec.BUFFER_FLAG_END_OF_STREAM;

        if (!isEos & extractorReady) {
            Log.d(TAG, "Kevin debug - attempting to write extractor frame to codec");
            // Try to submit the sample to the codec and if successful advance the
            // extractor to the next available sample to read.
            long time = extractor.getSampleTime();
            boolean result = decoder.writeSample(extractor, false,
                    time, extractor.getSampleFlags());

            if (result) {
                Log.d(TAG, "Kevin debug - successfully wrote to codec" + decoder.toString() + " from extractor " + extractor.toString() + " sample time " + time);
                // Advancing the extractor is a blocking operation and it MUST be
                // executed outside the main thread
                System.out.println("Kevin debug - " + extractor.toString() + " not ready till advances from " + extractor.getSampleTime());
                extractorReady = false;
                new Thread(new Runnable() {
                    public void run() {
                        extractor.advance();
                        extractorReady = true;
                        Log.d(TAG, "Extractor " + extractor.toString() + "ready.");
                    }
                }).start();

            }
        }
        else if (isEos) {
            extractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
            System.out.println("Kevin debug - extractor " + extractor.toString() + " seeking to start");
        }
        // END_INCLUDE(write_sample)

        // Examine the sample at the head of the queue to see if its ready to be
        // rendered and is not zero sized End-of-Stream record.
        MediaCodec.BufferInfo out_bufferInfo = new MediaCodec.BufferInfo();
        if (decoder.peekSample(out_bufferInfo)) {

            // BEGIN_INCLUDE(render_sample)
            if (out_bufferInfo.size <= 0 && isEos) {
                //mTimeAnimator.end();
                //mCodecWrapper.stopAndRelease();
                //mExtractor.release();

            } else if (out_bufferInfo.presentationTimeUs / 1000 < totalTime) {
                // Pop the sample off the queue and send it to {@link Surface}
                System.out.println("Higgs calling pop sample time " + out_bufferInfo.presentationTimeUs / 1000);
                decoder.popSample(true);
            }
        }
    }

        //time.start();



        //try {
            //mediaPlayer.setDataSource(context, uri);

            //MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            //retriever.setDataSource(context, uri);

            //onDataSourceSet(retriever);
        //} catch (IOException e) {
      //      Log.e(TAG, e.getMessage(), e);
        //}


    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        pause();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        release();
    }

    private void prepareAsync(final MediaPlayer.OnPreparedListener onPreparedListener) {
        /*
        if (mediaPlayer != null && state == PlayerState.NOT_PREPARED
                || state == PlayerState.STOPPED) {
            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    state = PlayerState.PREPARED;
                    onPreparedListener.onPrepared(mp);
                }
            });
            mediaPlayer.prepareAsync();
        }

         */
    }

    public void start() {
        //time.start();
        /*
        if (mediaPlayer != null) {
            switch (state) {
                case PREPARED:
                    mediaPlayer.start();
                    state = PlayerState.STARTED;
                    if (onVideoStartedListener != null) {
                        onVideoStartedListener.onVideoStarted();
                    }
                    break;
                case PAUSED:
                    mediaPlayer.start();
                    state = PlayerState.STARTED;
                    break;
                case STOPPED:
                    prepareAsync(new MediaPlayer.OnPreparedListener() {
                        @Override
                        public void onPrepared(MediaPlayer mp) {
                            mediaPlayer.start();
                            state = PlayerState.STARTED;
                            if (onVideoStartedListener != null) {
                                onVideoStartedListener.onVideoStarted();
                            }
                        }
                    });
                    break;
            }
        }

         */
    }

    public void pause() {
        // I don't know if this is actually relevant!
        if (time.isRunning() && time != null) { time.end(); }
        /*
        if (mediaPlayer != null && state == PlayerState.STARTED) {
            mediaPlayer.pause();
            state = PlayerState.PAUSED;
            if (time.isRunning() && time != null) { time.end(); }
            if (decoder != null) {
                decoder.stopAndRelease();
            }
            extractor.release();
        }

         */
    }

    public void stop() {
        /*
        if (mediaPlayer != null && (state == PlayerState.STARTED || state == PlayerState.PAUSED)) {
            mediaPlayer.stop();
            state = PlayerState.STOPPED;
        }

         */
    }

    public void reset() {
        /*
        if (mediaPlayer != null && (state == PlayerState.STARTED || state == PlayerState.PAUSED ||
                state == PlayerState.STOPPED)) {
            mediaPlayer.reset();
            state = PlayerState.NOT_PREPARED;

        }

         */
    }

    public void release() {
        /*
        if (mediaPlayer != null) {
            mediaPlayer.release();
            state = PlayerState.RELEASE;
        }

         */
    }

    public PlayerState getState() {
        return state;
    }

    public boolean isPlaying() {
        return state == PlayerState.STARTED;
    }

    public boolean isPaused() {
        return state == PlayerState.PAUSED;
    }

    public boolean isStopped() {
        return state == PlayerState.STOPPED;
    }

    public boolean isReleased() {
        return state == PlayerState.RELEASE;
    }

    public void seekTo(int msec) {
        //mediaPlayer.seekTo(msec);
    }

    public void setLooping(boolean looping) {
       // mediaPlayer.setLooping(looping);
    }

    public int getCurrentPosition() {
       // return mediaPlayer.getCurrentPosition();
        return 0;
    }

    public void setScreenOnWhilePlaying(boolean screenOn) {
      //  mediaPlayer.setScreenOnWhilePlaying(screenOn);
    }

    public void setOnErrorListener(MediaPlayer.OnErrorListener onErrorListener){
      //  mediaPlayer.setOnErrorListener(onErrorListener);
    }

    public void setOnVideoStartedListener(OnVideoStartedListener onVideoStartedListener) {
        this.onVideoStartedListener = onVideoStartedListener;
    }

    public void setOnVideoEndedListener(OnVideoEndedListener onVideoEndedListener) {
        this.onVideoEndedListener = onVideoEndedListener;
    }

    public void setOnSeekCompleteListener(MediaPlayer.OnSeekCompleteListener onSeekCompleteListener) {
      //  mediaPlayer.setOnSeekCompleteListener(onSeekCompleteListener);
    }
    public interface readyListener {
        void onReady(MediaDecoderAlphaMovieView m);
    }
    public void addReadyListener(readyListener ml) {
        mediaDecoderAlphaMovieViewReadyListener = ml;
    }
    private void onReady() {
        Log.d(TAG, "About to build decoder and report ready.");
        try {
            decoder = MediaCodecWrapper.fromVideoFormat(
                    extractor.getTrackFormat(0),
                    s
            );
            System.out.println("Higgs surface used to build codec is " + s.toString());
        } catch (IOException e) {
        }
        if (mediaDecoderAlphaMovieViewReadyListener != null) {
            mediaDecoderAlphaMovieViewReadyListener.onReady(this);
        }
    }





/*
    public MediaPlayer getMediaPlayer() {
        return mediaPlayer;
    }

 */

    public interface OnVideoStartedListener {
        void onVideoStarted();
    }

    public interface OnVideoEndedListener {
        void onVideoEnded();
    }

    private enum PlayerState {
        NOT_PREPARED, PREPARED, STARTED, PAUSED, STOPPED, RELEASE
    }
}