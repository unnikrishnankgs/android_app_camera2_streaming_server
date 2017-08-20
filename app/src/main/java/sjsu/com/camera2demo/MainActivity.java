package sjsu.com.camera2demo;

import android.media.MediaCodecInfo;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.TextureView;
import android.view.TextureView.SurfaceTextureListener;
import android.util.Log;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.util.Size;
import android.hardware.camera2.CameraDevice;
import android.graphics.SurfaceTexture;
import android.view.Surface;
import android.hardware.camera2.CaptureRequest;
import android.os.HandlerThread;
import android.os.Handler;
import android.hardware.camera2.CameraCaptureSession;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import android.media.MediaCodec;
import android.media.MediaFormat;
import java.nio.ByteBuffer;
import java.io.FileOutputStream;
import java.io.File;
import java.nio.channels.FileChannel;
import android.os.Environment;

import android.view.SurfaceView;
import java.net.Socket;
import java.io.DataOutputStream;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "Camera2Demo";
    private AutoFitTextureView mTextureView = null;
    private Size mPreviewSize;
    private List<Surface> mSurfaces = null;
    private MediaCodec mCodecAvc = null;
    private Surface mEncodeSurface = null;


    private SurfaceTextureListener mSurfaceTextureListener = new SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            Log.i(TAG, "surface texture available w=" + width + " h=" + height);
            CameraManager manager = (CameraManager) getSystemService(CAMERA_SERVICE);
            try {
                Log.v(TAG, "camera id list count=" + manager.getCameraIdList().length);
                String cameraId = manager.getCameraIdList()[0];
                CameraCharacteristics cameraCharacteristics = manager.getCameraCharacteristics(cameraId);
                StreamConfigurationMap map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                mPreviewSize = map.getOutputSizes(SurfaceTexture.class)[0];
                mTextureView.setAspectRatio(mPreviewSize.getWidth(), mPreviewSize.getHeight());
                Log.v(TAG, "preview size w=" + mPreviewSize.getWidth() + " h=" + mPreviewSize.getHeight());
                try {
                    manager.openCamera(cameraId, mCamStateCb, null /**< use current thread's looper; no new handler */);
                } catch (SecurityException e) {Log.e(TAG, ""+e);};
            } catch (Exception e) {
                Log.e(TAG, "onSurfaceTextureAvailable exception " + e);
            }
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            Log.i(TAG, "surface texture size changed w=" + width + " h=" + height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            //Log.d(TAG, "surface texture updated");

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTextureView = (AutoFitTextureView) findViewById(R.id.textureCamera);
        mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        /*
        if(false) {
            Camera2RawFragment rawFragment = Camera2RawFragment.newInstance(this);
            getFragmentManager().beginTransaction().replace(R.id.container, rawFragment).commit();
            Log.v(TAG, "fragment loaded?");
        }
        */



        mSurfaces = new ArrayList<>();
    }

    public void openSurfaceTexture(AutoFitTextureView autoFitTextureView)
    {
        mTextureView = autoFitTextureView;
        mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
    }

    private CameraDevice mCamDevice = null;
    private CaptureRequest.Builder mPreviewBuilder;
    private CameraCaptureSession mPreviewSession;

    @Override
    protected void onPause() {
        super.onPause();

    }

    CameraDevice.StateCallback mCamStateCb = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            Log.v(TAG, "camera opened");
            mCamDevice = camera;
            //capture the frames from mTextureView image stream as an OpenGL ES stream
            SurfaceTexture surfaceTexture = mTextureView.getSurfaceTexture();
            //set image buffer size
            surfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            Surface surfaceT = new Surface(surfaceTexture);
            Log.v(TAG, "surface created " + surfaceT);
            try {
                Log.v(TAG, "create cap req");
                mPreviewBuilder = mCamDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                Log.v(TAG, "add target surface");
                mPreviewBuilder.addTarget(surfaceT);
                mSurfaces.add(surfaceT);

            } catch (Exception e) {Log.e(TAG, "onOpened " + e);};

            //now create a surface for MediaCodec; AVC
            try {
                mCodecAvc = MediaCodec.createEncoderByType("video/avc");
            } catch (Exception e) { Log.e(TAG, "MediaCodec creation " + e);};
            MediaFormat formatAvc = MediaFormat.createVideoFormat("video/avc", mPreviewSize.getWidth(), mPreviewSize.getHeight());
            //MediaFormat formatAvc = MediaFormat.createVideoFormat("video/avc", 1920, 1080);
            int colorFormat = MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface; //will be a graphic buffer data reference
            int videoBitRate = 2000000;
            int fps = 25;
            int iFrameInterval = 2;
            formatAvc.setInteger(MediaFormat.KEY_COLOR_FORMAT, colorFormat);
            formatAvc.setInteger(MediaFormat.KEY_BIT_RATE, videoBitRate);
            formatAvc.setInteger(MediaFormat.KEY_FRAME_RATE, fps);
            formatAvc.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, iFrameInterval);
            /*
            //API 23 required!!!
            //Create the handlerThread and handler to process the encoded buffers at
            HandlerThread htEnc = new HandlerThread("EncodedBufferProcessor");
            Handler hEnc = new Handler(htEnc.getLooper());
            mCodecAvc.setCallback(mEncodedBufferCb, hEnc);
            */


            /*
            try {
                mCodecAvc.configure(formatAvc, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
                mEncodeSurface = mCodecAvc.createInputSurface();
                mCodecAvc.start();
                mPreviewBuilder.addTarget(mEncodeSurface);
                mSurfaces.add(mEncodeSurface);
            }catch(Exception e) {Log.e(TAG, "mCodecAvc configure failed");};
            */
            /** start the thread to received encoded data */
            //mEncoderBufferProcessor.start();

            Log.v(TAG, "started encode-buffer-processor");
            /** create the file to which we could dump avc data */
            try {
                File path = Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_PICTURES);

                File file = new File(path, "sample.h264");
                file.createNewFile();
                mDumpAvcChannel = new FileOutputStream(file).getChannel();
            } catch (Exception e) { Log.e(TAG, "dump file creation failed " + e);}

            try
            {
                Log.v(TAG, "create cap session");
                mCamDevice.createCaptureSession(mSurfaces, mCaptureSessionCb, null /**< use this thread's looper */);
                Log.v(TAG, "cap session created");
            } catch (Exception e) {Log.e(TAG, "createCaptureSession " + e);};

        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            Log.e(TAG, "camera disconnected ");
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            Log.e(TAG, "camera error " + error);
        }
    };

    private CameraCaptureSession.StateCallback mCaptureSessionCb = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(CameraCaptureSession session) {
            mPreviewSession = session;
            mPreviewBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);

            HandlerThread bgThread = new HandlerThread("Cam2Demo");
            bgThread.start();
            Handler bgHandler = new Handler(bgThread.getLooper());
            try {
                //request endlessly repeating capture of images by this capture session
                mPreviewSession.setRepeatingRequest(mPreviewBuilder.build(), null, bgHandler);
            } catch (Exception e) { Log.e(TAG, ""+e);};
        }

        @Override
        public void onConfigureFailed(CameraCaptureSession session) {

        }
    };

    private FileChannel mDumpAvcChannel;
    private static final int RTP_HEADER_LEN_MAX = (1 + 4);
    private static final int RTP_KEY_FRAME = 2;
    private static final int RTP_START_FRAME = 1;
    private static final int RTP_CONT_FRAME = 0;

    private static final String clientIPAddress = "192.168.43.125";//"10.0.0.5";
    private static final int mClientPort = 5600;
    private static final boolean mOverUdp = true;

    private Thread mEncoderBufferProcessor = new Thread(new Runnable() {
        @Override
        public void run() {
            DatagramSocket socketDG = null;
            Socket socketS = null;
            DataOutputStream dosS = null;
            InetAddress ipClient = null;
            try {
                ipClient = InetAddress.getByName(clientIPAddress);
                if(mOverUdp) {
                    socketDG = new DatagramSocket();
                }else{
                    try {
                        socketS = new Socket(ipClient, mClientPort);
                        dosS = new DataOutputStream(socketS.getOutputStream());
                    }catch(Exception e) {Log.e(TAG, "socket err");};
                }
            }catch(Exception e) {Log.e(TAG, "error inetaddr gen");}
            while(true) {
                MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
                ByteBuffer[] encoderOutputBuffers;
                int nBufIdx = mCodecAvc.dequeueOutputBuffer(info, -1);
                if (nBufIdx >= 0 && info.size > 0) {
                    ByteBuffer opBuf = mCodecAvc.getOutputBuffer(nBufIdx);
                    byte[] encodedBuffer = new byte[1140];
                    /** 1byte: 1/0 start/other packet
                     * 4 bytes: size */

                    int j = 0;
                    int nHeaderLen = 0;

                    do
                    {
                        int nDataOutNow = 0;
                        int i = 0;

                        if(nHeaderLen == 0)
                        {
                            encodedBuffer[i] = RTP_START_FRAME;
                            i++;
                            System.arraycopy(ByteBuffer.allocate(4).putInt(info.size).array(), 0, encodedBuffer, i, 4);
                            i += 4;
                        }
                        else
                        {
                            encodedBuffer[i] = RTP_CONT_FRAME;
                            i++;
                        }
                        nHeaderLen = i;
                        nDataOutNow = (info.size - j) > (1140 - nHeaderLen) ? (1140 - nHeaderLen) : (info.size - j);
                        opBuf.get(encodedBuffer, i, nDataOutNow);
                        i += nDataOutNow;
                        j += nDataOutNow;
                        if(mOverUdp) {
                            if (socketDG != null && ipClient != null) {
                                try {
                                    socketDG.send(new DatagramPacket(encodedBuffer, 0, 1140, ipClient, mClientPort));
                                } catch (Exception e) {
                                    Log.e(TAG, "data send failed");
                                }
                                ;
                            }
                        }else{
                            if(socketS != null) {
                                try {
                                    dosS.write(encodedBuffer, 0, 1140);
                                }catch(Exception e){Log.e(TAG, "data write err");};
                            }
                        }
                    }while(j < info.size);
                    try {
                        opBuf.rewind();
                        mDumpAvcChannel.write(opBuf);
                    } catch (Exception e) {
                        Log.e(TAG, "file write failed " + e);
                    };
                    mCodecAvc.releaseOutputBuffer(nBufIdx, false);
                } else if (nBufIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                } else {
                    Log.v(TAG, "unhandled dequeue result " + nBufIdx);
                }
            }
        }
    });
}
