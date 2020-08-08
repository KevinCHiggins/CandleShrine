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
    private Element el;
    private MediaExtractor extractor;
    public TimeAnimator time;
    private Uri uri;
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
            //extractor = new MediaExtractor();
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
        /*
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

         */
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

    public void setVideoFromUri(Context context, Uri _uri) {
        reset();
        /*
        try {
            extractor.setDataSource(getContext(), uri, null);
        } catch (IOException e) {
            e.printStackTrace();
        }
        extractor.selectTrack(0);
        extractorReady = true;


         */
        uri = _uri;
        isDataSourceSet = true;
        Log.d(TAG, "Kevin debug - Set URI successful.");
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
                (el.extractor.getSampleFlags() & MediaCodec.BUFFER_FLAG_END_OF_STREAM) == MediaCodec.BUFFER_FLAG_END_OF_STREAM;
        if (el.getExtractorAdvanced()) {
            if (isEos) {
                extractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
                System.out.println("Kevin debug - extractor " + extractor.toString() + " seeking to start");
            }
            Log.d(TAG, "Kevin debug - about to dequeue el " + el.toString() + " codec " + el.getInitialised());
            int index = el.getCodec().dequeueInputBuffer(100);
            Log.d(TAG, "Got index " + index + " from codec " + el.getCodec().toString());
            // if a buffer index was returned (rather than a negative flag)
            if (index >= 0) {
                // fill that buffer with data and save the size of the data
                int size = el.extractor.readSampleData(el.getCodec().getInputBuffer(index), 0);


                // queue the buffer to be decoded
                Log.d(TAG, el.toString() + "Queuing input buffer size " + size + ", time " + (el.extractor.getSampleTime() / 1000) + ", flags " + (el.extractor.getSampleFlags()));
                el.getCodec().queueInputBuffer(index, 0, size, el.extractor.getSampleTime(), el.extractor.getSampleFlags());
                el.setExtractorAdvanced(false);
                // whoa... concise! This, I hope, advances the extractor (which I've read
                // is a blocking operation) in another thread, then sets the flag when done
                new Thread(new Runnable() {
                    public void run() {
                        extractor.advance();
                        extractorReady = true;
                        Log.d(TAG, "Extractor " + extractor.toString() + "ready.");
                    }
                }).start();
            }
            el.updateOutputBufferIndex();
            if (el.getOutputBufferIndex() >= 0) {
                el.getCodec().releaseOutputBuffer(el.getOutputBufferIndex(), true);
                Log.d(TAG, "outputBuffer " + el.getOutputBufferIndex() + " good to render, time " + (el.getBufferInfo().presentationTimeUs / 1000));
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
        //try {
            el = new Element(getContext(), uri, s);
            /*
            decoder = MediaCodecWrapper.fromVideoFormat(
                    extractor.getTrackFormat(0),
                    s
            );


            System.out.println("Higgs surface used to build codec is " + s.toString());
        } catch (IOException e) {
        }

             */
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