package cn.yview.camera2streamget;

import android.support.v7.app.AppCompatActivity;
import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private byte[] config;

//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_camera2);
//        if (null == savedInstanceState) {
//            getSupportFragmentManager().beginTransaction()
//                    .replace(R.id.container, Camera2BasicFragment.newInstance())
//                    .commit();
//        }
//    }
//}

    private AutoFitTextureView previewSurface;
//    private SurfaceView previewSurface;
//    private SurfaceHolder previewSurfaceHolder;

    private MediaCodec mediaEncodeSave;    //保存流
    private MediaFormat mediaFormatSave;  //保存编码格式

    private Surface MediaSaveSurface;    //保存流使用的surface
    private CaptureRequest.Builder mPreviewBuilder = null;

    private String codecType = MediaFormat.MIMETYPE_VIDEO_AVC; //压缩格式
    private CameraDevice cameraDevice;    //camera
    private boolean upRecordFlag = false; //录制标志
    private boolean saveRecordFlag = false; //录制标志
    private MediaCodec.BufferInfo mBufferInfo = null;
    FileOutputStream fileOutputStream1;
    private CameraCharacteristics cameraCharacteristics;

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    startMediaCodecRecording();
                    break;

                case 1:
//                    new Thread(new Runnable() {
//
//                        @Override
//                        public void run() {
//                            boolean isFirst = true;
//
//                            do {
//                                if (!isFirst) {
//                                    try {
//                                        Thread.sleep(2000);
//                                    } catch (InterruptedException e) {
//                                        e.printStackTrace();
//                                    }
//                                }
//                                int ret = Jni.getInstance().connect("192.168.10.218", 11000, "");
//                                if (ret == 0) {
//                                    connect = true;
//                                }
//                            } while (!connect);
//
//                            Jni.getInstance().setCallback();
//                            handler.sendEmptyMessage(0);
//
//                            while (connect) {
//
//                                Jni.getInstance().parseStat();
//                                try {
//                                    Thread.sleep(1500);
//                                } catch (InterruptedException e) {
//                                    e.printStackTrace();
//                                }
//                            }
//
//                        }
//                    }).start();
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        previewSurface = findViewById(R.id.previewSurface);
//        previewSurfaceHolder = previewSurface.getHolder();
//        previewSurfaceHolder.addCallback(new SurfaceHolder.Callback() {
//            @Override
//            public void surfaceCreated(SurfaceHolder holder) {
//                try {
//                    fileOutputStream1 = new FileOutputStream(new File(Environment.getExternalStorageDirectory() + "/1080.h264"));
//                } catch (FileNotFoundException e) {
//                    e.printStackTrace();
//                }
//                openCamera(1920, 1080);
//            }
//
//            @Override
//            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
//                holder.setFixedSize(width, height);
//            }
//
//            @Override
//            public void surfaceDestroyed(SurfaceHolder holder) {
//
//            }
//        });

        previewSurface.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                try {
                    fileOutputStream1 = new FileOutputStream(new File(Environment.getExternalStorageDirectory() + "/1080.h264"));
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                openCamera(1920, 1080);
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {

            }
        });

//
//        Jni.getInstance().setPushListener(new PushListener() {
//            @Override
//            public void event(int event, int error) {
//
//            }
//        });
//
//        Jni.getInstance().setParseStatListener(new ParseStatListener() {
//            @Override
//            public void onParse(float dropRate, int bitrate, int jitter, int rtt, int maxDelay) {
//                Log.e("tag", "bitrate = " + bitrate + ",rtt = " + rtt);
//            }
//        });
    }

    /*open camera*/
    @SuppressLint("MissingPermission")
    private void openCamera(int width, int height) {
        /*获得相机管理服务*/
        CameraManager manager = (CameraManager) this.getSystemService(Context.CAMERA_SERVICE);
        try {
            /*获取CameraID列表*/
            String[] cameralist = manager.getCameraIdList();
            manager.openCamera(cameralist[0], CameraOpenCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /*close camera*/
    private void closeCamera() {

    }

    /*摄像头打开回调*/
    CameraDevice.StateCallback CameraOpenCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            cameraDevice = camera;
            try {
                mBufferInfo = new MediaCodec.BufferInfo();
                mediaEncodeSave = MediaCodec.createEncoderByType(codecType);
                mediaFormatSave = MediaFormat.createVideoFormat(codecType, 1920, 1080);
                mediaFormatSave.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
                mediaFormatSave.setInteger(MediaFormat.KEY_BIT_RATE, 4000000);
                mediaFormatSave.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
                mediaFormatSave.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
                mediaEncodeSave.configure(mediaFormatSave, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
                MediaSaveSurface = mediaEncodeSave.createInputSurface();

                /*设置预览尺寸*/
//                previewSurfaceHolder.setFixedSize(1920, 1080);
                previewSurface.setAspectRatio(1080, 1920);
                SurfaceTexture surfaceTexture = previewSurface.getSurfaceTexture();
                Surface surface = new Surface(surfaceTexture);
//
//                /*创建预览请求*/
                mPreviewBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                mPreviewBuilder.addTarget(surface);
                mPreviewBuilder.addTarget(MediaSaveSurface);
                /*创建会话*/
                camera.createCaptureSession(Arrays.asList(surface, MediaSaveSurface), Sessioncallback, null);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {

        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {

        }
    };

    static boolean connect = false;

    /*录制视频回调*/
    CameraCaptureSession.StateCallback Sessioncallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(@NonNull CameraCaptureSession session) {
            try {
//                startMediaCodecRecording();
                handler.sendEmptyMessage(0);
                session.setRepeatingRequest(mPreviewBuilder.build(), null, null);

            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
            //开始录制


        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {

        }
    };

    /*开始录制视频*/
    private void startMediaCodecRecording() {
        /*保存到本地流录制*/
        Thread recordThread1 = new Thread() {
            public void run() {
                super.run();
                if (mediaEncodeSave == null) {
                    return;
                }
                Log.d("MediaCodec", "本地流开始录制###################");
                upRecordFlag = true;
                mediaEncodeSave.start();
                ByteBuffer[] outputBuffers = mediaEncodeSave.getOutputBuffers();
                while (upRecordFlag) {

                    int outputBufferIndex = mediaEncodeSave.dequeueOutputBuffer(mBufferInfo, 1000);

                    if (outputBufferIndex >= 0) {

                        if (outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {

                        } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                            outputBuffers = mediaEncodeSave.getOutputBuffers();
                        } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        } else if (outputBufferIndex < 0) {

                        } else {
                            ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];
                            if (outputBuffer == null) {
                                throw new RuntimeException("encoderOutputBuffer " + outputBufferIndex + " was null");
                            }
                            if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {

                                MediaFormat format = mediaEncodeSave.getOutputFormat();
                                ByteBuffer spsb = format.getByteBuffer("csd-0");
                                ByteBuffer ppsb = format.getByteBuffer("csd-1");
                                byte[] sps = new byte[spsb.capacity()];
                                byte[] pps = new byte[ppsb.capacity()];
                                spsb.get(sps);
                                ppsb.get(pps);
                                config = new byte[sps.length + pps.length];

                                System.arraycopy(sps, 0, config, 0, sps.length);
                                System.arraycopy(pps, 0, config, sps.length, pps.length);

                                mBufferInfo.size = 0;

                            }
                            if (mBufferInfo.size != 0) {
                                //adjust the ByteBuffer values to match
                                outputBuffer.position(mBufferInfo.offset);
                                outputBuffer.limit(mBufferInfo.offset + mBufferInfo.size);
                                byte[] temp;
                                //是否是关键帧
                                if (mBufferInfo.flags == 1) {
//                        Log.e("tag", "关键帧");
//                                    temp = new byte[mBufferInfo.size];
//                                    outputBuffer.get(temp);
                                    temp = new byte[mBufferInfo.size + config.length];
                                    System.arraycopy(config, 0, temp, 0, config.length);
                                    outputBuffer.get(temp, config.length, mBufferInfo.size);
//                                Jni.getInstance().write(
//                                        0,
//                                        1,
//                                        mBufferInfo.presentationTimeUs/10,
//                                        mBufferInfo.presentationTimeUs/10,
//                                        temp,
//                                        temp.length);
//                                    Jni.getInstance().push(
//                                            0,
//                                            1,
//                                            mBufferInfo.presentationTimeUs / 1000 * 90,
//                                            mBufferInfo.presentationTimeUs / 1000 * 90,
//                                            temp,
//                                            temp.length);
                                    try {
                                        fileOutputStream1.write(temp);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                } else {
                                    temp = new byte[mBufferInfo.size];
                                    outputBuffer.get(temp);
//                                Jni.getInstance().write(
//                                        0,
//                                        0,
//                                        mBufferInfo.presentationTimeUs/10,
//                                        mBufferInfo.presentationTimeUs/10,
//                                        temp,
//                                        temp.length);
//                                    Jni.getInstance().push(
//                                            0,
//                                            0,
//                                            mBufferInfo.presentationTimeUs / 1000 * 90,
//                                            mBufferInfo.presentationTimeUs / 1000 * 90,
//                                            temp,
//                                            temp.length);
                                    try {
                                        fileOutputStream1.write(temp);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                                temp = null;
                            }

                            mediaEncodeSave.releaseOutputBuffer(outputBufferIndex, false);
                        }

                    }
                }

                MediaSaveSurface.release();
                MediaSaveSurface = null;
                mediaEncodeSave.stop();
                mediaEncodeSave.release();
                mediaEncodeSave = null;
            }
        };
        recordThread1.start();

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        try {
            cameraDevice.close();

            MediaSaveSurface.release();
            mediaEncodeSave.stop();
            mediaEncodeSave.release();
            fileOutputStream1.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
