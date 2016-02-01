package id.zulns.androidwifiremote;

import android.app.Activity;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.media.AudioManager;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class CameraActivity extends Activity implements SurfaceHolder.Callback, Camera.PictureCallback, Camera.ShutterCallback, Camera.PreviewCallback {

    private enum MEDIA_TYPE { IMAGE, VIDEO }
    private Camera mCamera;
    private Parameters mCameraParams;
    private SurfaceHolder mSurfaceHolder;
    private MediaRecorder mMediaRecorder;
    private byte[] mByteArray;
    private boolean mIsRecording = false;
    private AudioManager mAudioManager;
    private int mRingerMode;
    private int mPrevWidth, mPrevHeight;
    private int mVideoProfile = CamcorderProfile.QUALITY_HIGH;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window wind = this.getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            wind.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            wind.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        }
        wind.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        wind.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        wind.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);

        setContentView(R.layout.activity_camera);

        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mRingerMode = mAudioManager.getRingerMode();
        mAudioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);

        SurfaceView surfaceView = new SurfaceView(this);
        FrameLayout frameLayout = (FrameLayout) findViewById(R.id.frameLayout);
        frameLayout.addView(surfaceView);
        mSurfaceHolder = surfaceView.getHolder();
        mSurfaceHolder.addCallback(this);
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    @Override
    protected void onResume() {
        super.onResume();

        Main.setCameraActivity(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        releaseMediaRecorder();
        releaseCamera();

        mAudioManager.setRingerMode(mRingerMode);

        Main.setCameraActivity(null);
    }

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {
        File pictureFile = getOutputMediaFile(MEDIA_TYPE.IMAGE);
        if (pictureFile == null){
            return;
        }
        try {
            FileOutputStream fos = new FileOutputStream(pictureFile);
            fos.write(data);
            fos.close();
            mCamera.startPreview();
        }
        catch (FileNotFoundException e) {
        }
        catch (IOException e) {
        }
    }

    @Override
    public void onShutter() {

    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        YuvImage yuvImg = new YuvImage(data, ImageFormat.NV21, mPrevWidth, mPrevHeight, null);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        yuvImg.compressToJpeg(new Rect(0, 0, mPrevWidth - 1, mPrevHeight - 1), 50, baos);
        mByteArray = baos.toByteArray();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mCamera = getCameraInstance(Main.getCurrentCameraId());
        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            CameraInfo info = new CameraInfo();
            Camera.getCameraInfo(Main.getCurrentCameraId(), info);
            if (info.canDisableShutterSound) {
                mCamera.enableShutterSound(false);
            }
        }*/
        if (mCamera != null) {
            setCameraDisplayOrientation(mCamera, Main.getCurrentCameraId());
            try {
                mCamera.setPreviewCallback(this);
                mCamera.setPreviewDisplay(holder);
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (mCamera != null) {
            mCameraParams = mCamera.getParameters();
            Size size = getOptimalPreviewSize(mCameraParams.getSupportedPreviewSizes(), 320, 240);
            mPrevWidth = size.width;
            mPrevHeight = size.height;
            mCameraParams.setPreviewSize(mPrevWidth, mPrevHeight);
            mCamera.setParameters(mCameraParams);
            mCamera.startPreview();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    private Camera getCameraInstance(int cameraId) {
        Camera c = null;
        try {
            c = Camera.open(cameraId);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return c;
    }

    private void setCameraDisplayOrientation(Camera camera, int cameraId) {
        CameraInfo info = new CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }
        int result;
        if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        }
        else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }

    private Size getOptimalPreviewSize(List<Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) w / h;
        if (sizes == null) return null;

        Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        // Try to find an size match aspect ratio and size
        for (Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - h) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - h);
            }
        }

        // Cannot find the one match the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Size size : sizes) {
                if (Math.abs(size.height - h) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - h);
                }
            }
        }
        return optimalSize;
    }

    private boolean prepareVideoRecorder(){
        mMediaRecorder = new MediaRecorder();
        mCamera.unlock();
        mMediaRecorder.setCamera(mCamera);
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        mMediaRecorder.setProfile(CamcorderProfile.get(Main.getCurrentCameraId(), mVideoProfile));
        mMediaRecorder.setOutputFile(getOutputMediaFile(MEDIA_TYPE.VIDEO).getAbsolutePath());
        mMediaRecorder.setPreviewDisplay(mSurfaceHolder.getSurface());
        try {
            mMediaRecorder.prepare();
        }
        catch (IOException | IllegalStateException e) {
            releaseMediaRecorder();
            return false;
        }
        return true;
    }

    private void releaseCamera(){
        if (mCamera != null){
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();        // release the camera for other applications
            mCamera = null;
        }
    }

    private void releaseMediaRecorder(){
        if (mMediaRecorder != null) {
            if (mIsRecording) {
                mMediaRecorder.stop();
            }
            mMediaRecorder.reset();   // clear recorder configuration
            mMediaRecorder.release(); // release the recorder object
            mMediaRecorder = null;
            mCamera.lock();           // lock camera for later use
        }
    }

    private static File getOutputMediaFile(MEDIA_TYPE type){
        File mediaStorageDir = new File(Environment.getExternalStorageDirectory(), "id.ZulNs" + File.separator + "WifiRemote" + File.separator + "Media");
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                return null;
            }
        }
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type.equals(MEDIA_TYPE.IMAGE)){
            mediaFile = new File(mediaStorageDir, "IMG_"+ timeStamp + ".jpg");
        }
        else if(type.equals(MEDIA_TYPE.VIDEO)) {
            mediaFile = new File(mediaStorageDir, "VID_"+ timeStamp + ".mp4");
        }
        else {
            return null;
        }
        return mediaFile;
    }

    public boolean isCameraActive() {
        return (mCamera != null);
    }

    public boolean isRecording() {
        return mIsRecording;
    }

    public void takePicture() {
        mCamera.takePicture(this, null, null, this);
    }

    public boolean startRecordVideo() {
        if (! mIsRecording && prepareVideoRecorder()) {
            mIsRecording = true;
            mMediaRecorder.start();
            mCamera.setPreviewCallback(this);
            return true;
        }
        return false;
    }

    public void stopRecordVideo() {
        if (mIsRecording) {
            releaseMediaRecorder();
            mIsRecording = false;
        }
    }

    public boolean isFlashSupport() {
        return mCamera != null && mCameraParams.getFlashMode() != null;
    }

    public boolean setFlashlight(boolean on) {
        if (mCamera != null) {
            mCameraParams.setFlashMode((on) ? Camera.Parameters.FLASH_MODE_TORCH : Camera.Parameters.FLASH_MODE_OFF);
            mCamera.setParameters(mCameraParams);
            return true;
        }
        return false;
    }

    public ByteArrayInputStream getInputStream() {
        return new ByteArrayInputStream(mByteArray);
    }

    public List<String> getSupportedPictureSizes() {
        if (mCamera != null) {
            List<String> szs = new ArrayList<>();
            List<Size> sizes = mCamera.getParameters().getSupportedPictureSizes();
            for (Size size : sizes) {
                szs.add(String.valueOf(size.width) + "x" + String.valueOf(size.height));
            }
            return szs;
        }
        return null;
    }

    public String getPictureSize() {
        if (mCamera != null) {
            Size size = mCameraParams.getPictureSize();
            return (String.valueOf(size.width) + "x" + String.valueOf(size.height));
        }
        return null;
    }

    public void setPictureSize(String size) {
        if (mCamera != null) {
            int mark = size.indexOf('x');
            int width = Integer.parseInt(size.substring(0, mark));
            int height = Integer.parseInt(size.substring(mark + 1));
            if (width > 0 && height > 0) {
                mCameraParams.setPictureSize(width, height);
                mCamera.setParameters(mCameraParams);
            }
        }
    }

    public List<String> getSupportedSceneModes() {
        return (mCamera != null) ? mCameraParams.getSupportedSceneModes() : null;
    }

    public String getSceneMode() {
        return (mCamera != null) ? mCameraParams.getSceneMode() : null;
    }

    public void setSceneMode(String mode) {
        if (mCamera != null) {
            mCameraParams.setSceneMode(mode);
            mCamera.setParameters(mCameraParams);
        }
    }

    public List<String> getSupportedWhiteBalance() {
        return (mCamera != null) ? mCameraParams.getSupportedWhiteBalance() : null;
    }

    public String getWhiteBalance() {
        return (mCamera != null) ? mCameraParams.getWhiteBalance() : null;
    }

    public void setWhiteBalance(String wb) {
        if (mCamera != null) {
            mCameraParams.setWhiteBalance(wb);
            mCamera.setParameters(mCameraParams);
        }
    }

    public List<String> getSupportedColorEffects() {
        return (mCamera != null) ? mCameraParams.getSupportedColorEffects() : null;
    }

    public String getColorEffect() {
        return (mCamera != null) ? mCameraParams.getColorEffect() : null;
    }

    public void setColorEffect(String ce) {
        if (mCamera != null) {
            //mCamera.stopPreview();
            //mCamera.setPreviewCallback(null);
            mCameraParams.setColorEffect(ce);
            mCamera.setParameters(mCameraParams);
            //mCamera.setPreviewCallback(this);
            //mCamera.startPreview();
        }
    }

    public boolean isSupportedExposureCompensation() {
        return mCamera != null && mCameraParams.getMinExposureCompensation() != 0 && mCameraParams.getMaxExposureCompensation() !=0;
    }

    public int getMinExposureCompensation() {
        return (mCamera != null) ? mCameraParams.getMinExposureCompensation() : 0;
    }

    public int getMaxExposureCompensation() {
        return (mCamera != null) ? mCameraParams.getMaxExposureCompensation() : 0;
    }

    public int getExposureCompensation() {
        return (mCamera != null) ? mCameraParams.getExposureCompensation() : 0;
    }

    public float getExposureCompensationStep() {
        return (mCamera != null) ? mCameraParams.getExposureCompensationStep() : 0.0f;
    }

    public void setExposureCompensation(int ev) {
        if (mCamera != null) {
            int min = mCameraParams.getMinExposureCompensation();
            int max = mCameraParams.getMaxExposureCompensation();
            if (min <= ev && ev <= max) {
                mCameraParams.setExposureCompensation(ev);
                mCamera.setParameters(mCameraParams);
            }
        }
    }

    public List<String> getSupportedVideoProfiles() {
        if (mCamera != null) {
            List<String> vs = new ArrayList<>();
            int id = Main.getCurrentCameraId();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if (CamcorderProfile.hasProfile(id, CamcorderProfile.QUALITY_2160P)) {
                    vs.add("2160p (3840x2160)");
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                if (CamcorderProfile.hasProfile(id, CamcorderProfile.QUALITY_1080P)) {
                    vs.add("1080p (1920x1080)");
                }
                if (CamcorderProfile.hasProfile(id, CamcorderProfile.QUALITY_720P)) {
                    vs.add("720p (1280x720)");
                }
                if (CamcorderProfile.hasProfile(id, CamcorderProfile.QUALITY_480P)) {
                    vs.add("480p (720x480)");
                }
                if (CamcorderProfile.hasProfile(id, CamcorderProfile.QUALITY_CIF)) {
                    vs.add("CIF (352x288)");
                }
                if (CamcorderProfile.hasProfile(id, CamcorderProfile.QUALITY_QVGA)) {
                    vs.add("QVGA (320x240)");
                }
                if (CamcorderProfile.hasProfile(id, CamcorderProfile.QUALITY_QCIF)) {
                    vs.add("QCIF (176x144)");
                }
            }
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.GINGERBREAD || vs.isEmpty()) {
                vs.add("High Quality");
                vs.add("Low Quality");
            }
            return vs;
        }
        return null;
    }

    public String getVideoProfile() {
        if (mCamera != null) {
            if (mVideoProfile == CamcorderProfile.QUALITY_2160P) {
                return "2160p";
            }
            else if (mVideoProfile == CamcorderProfile.QUALITY_1080P) {
                return "1080p";
            }
            else if (mVideoProfile == CamcorderProfile.QUALITY_720P) {
                return "720p";
            }
            else if (mVideoProfile == CamcorderProfile.QUALITY_480P) {
                return "480p";
            }
            else if (mVideoProfile == CamcorderProfile.QUALITY_CIF) {
                return "CIF";
            }
            else if (mVideoProfile == CamcorderProfile.QUALITY_QVGA) {
                return "QVGA";
            }
            else if (mVideoProfile == CamcorderProfile.QUALITY_QCIF) {
                return "QCIF";
            }
            else if (mVideoProfile == CamcorderProfile.QUALITY_HIGH) {
                return "High";
            }
            else if (mVideoProfile == CamcorderProfile.QUALITY_LOW) {
                return "Low";
            }
        }
        return null;
    }

    public void setVideoProfile(String profile) {
        if (mCamera != null) {
            if (profile.equals("2160p")) {
                mVideoProfile = CamcorderProfile.QUALITY_2160P;
            }
            else if (profile.equals("1080p")) {
                mVideoProfile = CamcorderProfile.QUALITY_1080P;
            }
            else if (profile.equals("720p")) {
                mVideoProfile = CamcorderProfile.QUALITY_720P;
            }
            else if (profile.equals("480p")) {
                mVideoProfile = CamcorderProfile.QUALITY_480P;
            }
            else if (profile.equals("CIF")) {
                mVideoProfile = CamcorderProfile.QUALITY_CIF;
            }
            else if (profile.equals("QVGA")) {
                mVideoProfile = CamcorderProfile.QUALITY_QVGA;
            }
            else if (profile.equals("QCIF")) {
                mVideoProfile = CamcorderProfile.QUALITY_QCIF;
            }
            else if (profile.equals("High")) {
                mVideoProfile = CamcorderProfile.QUALITY_HIGH;
            }
            else if (profile.equals("Low")) {
                mVideoProfile = CamcorderProfile.QUALITY_LOW;
            }
        }
    }
}
