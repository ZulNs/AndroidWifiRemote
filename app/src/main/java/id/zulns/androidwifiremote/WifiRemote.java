package id.zulns.androidwifiremote;

import android.net.Uri;
import android.os.Build;
import android.os.Environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


/**
 * Created by ZulNs on 19-Jan-15.
 */
public class WifiRemote extends NanoHTTPD {

    private final long TIMEOUT;
    private static final String DIR_EXPLORER = "storage";
    private final File ROOT_DIR;

    private File mCurrentDir;
    private Locale mCurrentVoice;
    private float mCurrentPitch;
    private float mCurrentSpeechRate;
    private int mCurrentVolume;
    private long mExpireLimit;

    /**
     * Common mime type for dynamic content: binary
     */
    public static final String MIME_DEFAULT_BINARY = "application/octet-stream";

    /**
     * Hashtable mapping (String)FILENAME_EXTENSION -> (String)MIME_TYPE
     */
    private static final Map<String, String> MIME_TYPES = new HashMap<String, String>() {{
        put("css",   "text/css");
        put("htm",   "text/html");
        put("html",  "text/html");
        put("xml",   "text/xml");
        put("java",  "text/x-java-source, text/java");
        put("md",    "text/plain");
        put("txt",   "text/plain");
        put("asc",   "text/plain");
        put("gif",   "image/gif");
        put("jpg",   "image/jpeg");
        put("jpeg",  "image/jpeg");
        put("png",   "image/png");
        put("ico",   "image/x-icon");
        put("mp3",   "audio/mpeg");
        put("m3u",   "audio/mpeg-url");
        put("mp4",   "video/mp4");
        put("ogv",   "video/ogg");
        put("flv",   "video/x-flv");
        put("mov",   "video/quicktime");
        put("swf",   "application/x-shockwave-flash");
        put("js",    "application/javascript");
        put("pdf",   "application/pdf");
        put("doc",   "application/msword");
        put("ogg",   "application/x-ogg");
        put("zip",   "application/octet-stream");
        put("exe",   "application/octet-stream");
        put("class", "application/octet-stream");
    }};

    // The Class Constructor
    public WifiRemote(int port, int timeout) {
        super(port);

        TIMEOUT = (long) (timeout * 60 * 1000);
        File f = new File("/");
        ROOT_DIR = (f.exists()) ? f : Environment.getExternalStorageDirectory().getAbsoluteFile();
        mCurrentDir = ROOT_DIR;
        if (Main.getSupportTts()) {
            mCurrentVoice = Main.getService().getVoice();
            mCurrentPitch = 1.0f;
            mCurrentSpeechRate = 1.0f;
            mCurrentVolume = Main.getService().getCurrentVolume();
        }
        mExpireLimit = Main.getExpireLimit();
    }

    @Override
    public Response serve(IHTTPSession session) {
        Map<String, String> headers = session.getHeaders();
        Map<String, String> parms = session.getParms();
        Method method = session.getMethod();
        String uri = session.getUri();
        Map<String, String> files = new HashMap<>();

        if (Method.POST.equals(method) || Method.PUT.equals(method)) {
            try {
                session.parseBody(files);
            }
            catch (IOException e) {
                return getResponse("Internal Error IO Exception: " + e.getMessage());
            }
            catch (ResponseException e) {
                return new Response(e.getStatus(), MIME_PLAINTEXT, e.getMessage());
            }
        }

        uri = uri.trim().replace(File.separatorChar, '/');
        if (uri.indexOf('?') >= 0) {
            uri = uri.substring(0, uri.indexOf('?'));
        }

        if ("/".equals(uri) || "/index".equalsIgnoreCase(uri) || "/index.htm".equalsIgnoreCase(uri) || "/index.html".equalsIgnoreCase(uri)) {
            return createAssetsResponse("index.html");
        }

        if ("/favicon.png".equalsIgnoreCase(uri) || "/jquery.min.js".equalsIgnoreCase(uri) || "/jquery-ui.min.js".equalsIgnoreCase(uri) || "/jquery-ui.min.css".equalsIgnoreCase(uri)) {
            return createAssetsResponse(uri.substring(1));
        }

        if ("images".equalsIgnoreCase(getRootName(uri))) {
            if ("ui-icons_".equalsIgnoreCase(uri.substring(8, 17))) {
                return createAssetsResponse(uri.substring(1));
            }
            return getResponse("Error 404: File not found");
        }

        if ("/availablevoice".equalsIgnoreCase(uri)) {
            StringBuilder sb = new StringBuilder();
            if (! Main.getSupportTts()) {
                return getResponse("TTS not supported");
            }
            for (String voice : Main.getService().getVoices()) {
                sb.append("<option value='" + voice.substring(voice.length() - 7) + "'");
                if (Main.getService().getVoice().toString().equals(voice.substring(voice.length() - 7))) {
                    sb.append(" selected='selected'");
                }
                sb.append(">" + voice.substring(0, voice.length() - 7) + "</option>");
            }
            return getResponse(sb.toString());
        }

        if ("/camera/getsupportedpicturesizes".equalsIgnoreCase(uri)) {
            StringBuilder sb = new StringBuilder();
            if (Main.getCameraActivity() != null) {
                if (Main.getCameraActivity().isCameraActive()) {
                    for (String ps : Main.getCameraActivity().getSupportedPictureSizes()) {
                        sb.append("<option value='" + ps + "'");
                        if (ps.contentEquals(Main.getCameraActivity().getPictureSize())) {
                            sb.append(" selected='selected'");
                        }
                        sb.append(">" + ps + "</option>");
                    }
                    if (sb.length() != 0) {
                        return getResponse(sb.toString());
                    }
                }
            }
            return getResponse("Camera inactive");
        }

        if ("/camera/getsupportedscenemodes".equalsIgnoreCase(uri)) {
            StringBuilder sb = new StringBuilder();
            if (Main.getCameraActivity() != null) {
                if (Main.getCameraActivity().isCameraActive()) {
                    for (String sm : Main.getCameraActivity().getSupportedSceneModes()) {
                        sb.append("<option value='" + sm + "'");
                        if (sm.contentEquals(Main.getCameraActivity().getSceneMode())) {
                            sb.append(" selected='selected'");
                        }
                        sb.append(">" + sm + "</option>");
                    }
                    if (sb.length() != 0) {
                        return getResponse(sb.toString());
                    }
                }
            }
            return getResponse("Camera inactive");
        }

        if ("/camera/getsupportedwhitebalance".equalsIgnoreCase(uri)) {
            StringBuilder sb = new StringBuilder();
            if (Main.getCameraActivity() != null) {
                if (Main.getCameraActivity().isCameraActive()) {
                    for (String wb : Main.getCameraActivity().getSupportedWhiteBalance()) {
                        sb.append("<option value='" + wb + "'");
                        if (wb.contentEquals(Main.getCameraActivity().getWhiteBalance())) {
                            sb.append(" selected='selected'");
                        }
                        sb.append(">" + wb + "</option>");
                    }
                    if (sb.length() != 0) {
                        return getResponse(sb.toString());
                    }
                }
            }
            return getResponse("Camera inactive");
        }

        if ("/camera/getsupportedcoloreffects".equalsIgnoreCase(uri)) {
            StringBuilder sb = new StringBuilder();
            if (Main.getCameraActivity() != null) {
                if (Main.getCameraActivity().isCameraActive()) {
                    for (String ce : Main.getCameraActivity().getSupportedColorEffects()) {
                        sb.append("<option value='" + ce + "'");
                        if (ce.contentEquals(Main.getCameraActivity().getColorEffect())) {
                            sb.append(" selected='selected'");
                        }
                        sb.append(">" + ce + "</option>");
                    }
                    if (sb.length() != 0) {
                        return getResponse(sb.toString());
                    }
                }
            }
            return getResponse("Camera inactive");
        }

        if ("/camera/getsupportedvideoprofiles".equalsIgnoreCase(uri)) {
            StringBuilder sb = new StringBuilder();
            if (Main.getCameraActivity() != null) {
                if (Main.getCameraActivity().isCameraActive()) {
                    for (String vp : Main.getCameraActivity().getSupportedVideoProfiles()) {
                        String vpv = vp.substring(0, vp.indexOf(' '));
                        sb.append("<option value='" + vpv + "'");
                        if (vpv.contentEquals(Main.getCameraActivity().getVideoProfile())) {
                            sb.append(" selected='selected'");
                        }
                        sb.append(">" + vp + "</option>");
                    }
                    if (sb.length() != 0) {
                        return getResponse(sb.toString());
                    }
                }
            }
            return getResponse("Camera inactive");
        }

        if ("/camera/issupportedexposure".equalsIgnoreCase(uri)) {
            if (Main.getCameraActivity() != null) {
                return getResponse(String.valueOf(Main.getCameraActivity().isSupportedExposureCompensation()));
            }
            return getResponse("false");
        }

        if ("/camera/getminexposure".equalsIgnoreCase(uri)) {
            if (Main.getCameraActivity() != null) {
                if (Main.getCameraActivity().isCameraActive()) {
                    return getResponse(String.valueOf(Main.getCameraActivity().getMinExposureCompensation()));
                }
            }
            return getResponse("Camera inactive");
        }

        if ("/camera/getmaxexposure".equalsIgnoreCase(uri)) {
            if (Main.getCameraActivity() != null) {
                if (Main.getCameraActivity().isCameraActive()) {
                    return getResponse(String.valueOf(Main.getCameraActivity().getMaxExposureCompensation()));
                }
            }
            return getResponse("Camera inactive");
        }

        if ("/camera/getexposurevalue".equalsIgnoreCase(uri)) {
            if (Main.getCameraActivity() != null) {
                if (Main.getCameraActivity().isCameraActive()) {
                    return getResponse(String.valueOf(Main.getCameraActivity().getExposureCompensation()));
                }
            }
            return getResponse("Camera inactive");
        }

        if ("/camera/getexposurestep".equalsIgnoreCase(uri)) {
            if (Main.getCameraActivity() != null) {
                if (Main.getCameraActivity().isCameraActive()) {
                    return getResponse(String.valueOf(Main.getCameraActivity().getExposureCompensationStep()));
                }
            }
            return getResponse("Camera inactive");
        }

        if ("/logged".equalsIgnoreCase(uri)) {
            return getResponse(String.valueOf(! Main.getUserIp().equals("")));
        }

        if ("/getdevicename".equalsIgnoreCase(uri)) {
            return getResponse(Build.MANUFACTURER + "&nbsp;" + Build.MODEL);
        }

        if ("/getappstatus".equalsIgnoreCase(uri)) {
            String status = "";
            if (Main.isExpired()) {
                status = "Expired";
            }
            else if (! Main.isFullAccess()) {
                status = "Trial";
            }
            if (status.length() > 0) {
                status = "&nbsp;(" + status + ")";
            }
            return getResponse(status);
        }

        if ("/getappabout".equalsIgnoreCase(uri)) {
            return getResponse(Main.APP_ABOUT);
        }

        if ("/volumemax".equalsIgnoreCase(uri)) {
            return getResponse(String.valueOf(Main.getService().getMaxVolume()));
        }

        if ("/volumecurrent".equalsIgnoreCase(uri)) {
            return getResponse(String.valueOf(mCurrentVolume));
        }

        if ("/camera/iscamerasupport".equalsIgnoreCase(uri)) {
            return getResponse(String.valueOf(Main.getCameraId() != -1));
        }

        if ("/camera/iscamerafrontsupport".equalsIgnoreCase(uri)) {
            return getResponse(String.valueOf(Main.getCameraFrontId() != -1));
        }

        if ("/camera/iscameraflashsupport".equalsIgnoreCase(uri)) {
            if (Main.getCameraActivity() != null) {
                return getResponse(String.valueOf(Main.getCameraActivity().isFlashSupport()));
            }
            return getResponse("false");
        }

        if ("/login".equalsIgnoreCase(uri)) {
            if (! Main.getUserIp().equals("")) {
                return getResponse("Logged");
            }
            String un = parms.get("username");
            String pw = parms.get("password");
            if (un == null || pw == null || un.contentEquals("") || pw.contentEquals("")) {
                return getResponse("Invalid parameter");
            }
            if (Main.isExpired()) {
                if (! Main.setAppStatus(un, pw)) {
                    return getResponse("Unauthorized: Please enter a matching password to regain your access");
                }
                else {
                    mExpireLimit = Main.getExpireLimit();
                    Main.setUserIp(getRemoteIp(headers));
                    Main.setUsername(un);
                    Main.setUserTimeout(new Date().getTime() + TIMEOUT);
                    Main.saveSharedPreferences();
                    if (un.contentEquals(Main.APP_AUTHOR)) {
                        return getResponse("Success: Nice to see you, <strong>BOSS</strong>...:-)");
                    }
                    else {
                        if (Main.isFullAccess()) {
                            return getResponse("Success: Congratulations <strong>" + un + "</strong>, you've got full access rights");
                        }
                        else {
                            return getResponse("Success: Congratulations <strong>" + un + "</strong>, you've successfully extended the trial period");
                        }
                    }
                }
            }
            if (! Main.isFullAccess()) {
                if (Main.setAppStatus(un, pw)) {
                    mExpireLimit = Main.getExpireLimit();
                    Main.setUserIp(getRemoteIp(headers));
                    Main.setUsername(un);
                    Main.setUserTimeout(new Date().getTime() + TIMEOUT);
                    Main.saveSharedPreferences();
                    if (un.contentEquals(Main.APP_AUTHOR)) {
                        return getResponse("Success: Nice to see you, <strong>BOSS</strong>...:-)");
                    }
                    else {
                        return getResponse("Success: Congratulations <strong>" + un + "</strong>, you've got full access rights");
                    }
                }
            }
            String spw = (un.contentEquals(Main.APP_AUTHOR)) ? Main.APP_CODE : Main.getUser(un);
            if (spw == null) {
                return getResponse("Unauthorized: Registration required");
            }
            if (! spw.contentEquals(pw)) {
                return getResponse("Unauthorized: Incorrect password");
            }
            Main.setUserIp(getRemoteIp(headers));
            Main.setUsername(un);
            Main.setUserTimeout(new Date().getTime() + TIMEOUT);
            Main.saveSharedPreferences();
            if (un.contentEquals(Main.APP_AUTHOR)) {
                return getResponse("Success: Nice to see you, <strong>BOSS</strong>...:-)");
            }
            else {
                return getResponse("Success: Welcome you <strong>" + un + "</strong>");
            }
        }

        if (Main.getUserIp().equals("")) {
            return getResponse("Unauthorized: Login required");
        }

        if (! Main.getUserIp().contentEquals(getRemoteIp(headers))) {
            return getResponse("Unauthorized: Already connected to another user");
        }

        long time = new Date().getTime();

        if (Main.getUserTimeout() <= time || time >= mExpireLimit) {
            if (Main.getCameraActivity() != null) {
                Main.getCameraActivity().finish();
            }
            Main.setUserIp("");
            Main.saveSharedPreferences();
            if (time >= Main.getUserTimeout()) {
                return getResponse("Timeout: Login required");
            }
            return getResponse("Unauthorized: Your access has been expired, please enter a match password to regain your access");
        }

        Main.setUserTimeout(time + TIMEOUT);

        if ("/tts".equalsIgnoreCase(uri)) {
            if (! Main.getSupportTts()) {
                return getResponse("TTS not supported");
            }
            String voice = parms.get("voice");
            String pitch = parms.get("pitch");
            String speechRate = parms.get("speechrate");
            String volume = parms.get("volume");
            String msg = parms.get("message");
            if (voice != null) {
                Locale lVoice = new Locale(voice.substring(0,3), voice.substring(4));
                if (! lVoice.equals(mCurrentVoice)) {
                    if (! Main.getService().setVoice(lVoice)) {
                        return getResponse("Failed while set the voice");
                    }
                    mCurrentVoice = lVoice;
                }
            }
            if (pitch != null) {
                Float fPitch = Float.parseFloat(pitch);
                if (fPitch != mCurrentPitch) {
                    if (! Main.getService().setPitch(fPitch)) {
                        return getResponse("Failed while set the pitch");
                    }
                    mCurrentPitch = fPitch;
                }
            }
            if (speechRate != null) {
                Float fSpeechRate = Float.parseFloat(speechRate);
                if (fSpeechRate != mCurrentSpeechRate) {
                    if (! Main.getService().setSpeechRate(fSpeechRate)) {
                        return getResponse("Failed while set the speech rate");
                    }
                    mCurrentSpeechRate = fSpeechRate;
                }
            }
            if (volume != null) {
                int iVolume = Integer.parseInt(volume);
                if (iVolume != mCurrentVolume) {
                    Main.getService().setVolume(iVolume);
                    mCurrentVolume = iVolume;
                }
            }
            if (msg != null) {
                if (! Main.getService().speak(msg)) {
                    return getResponse("Failed while speaking");
                }
            }
            return getResponse("Success");
        }

        if ("/flush".equalsIgnoreCase(uri)) {
            if (Main.getService().isSpeaking()) {
                if (! Main.getService().flush()) {
                    getResponse("Failed while stop the speaking");
                }
            }
            return getResponse("Success");
        }

        if ("/newdirectory".equalsIgnoreCase(uri)) {
            String filename = parms.get("filename");
            if (null == filename) {
                return getResponse("Invalid parameter");
            }
            File src = new File(mCurrentDir, filename);
            if (src.exists()) {
                return getResponse("Internal Error: Directory already exist");
            }
            if (! src.mkdirs()) {
                return getResponse("Internal Error: Failure");
            }
            return getResponse("Success");
        }

        if ("/uploadfiles".equalsIgnoreCase(uri)) {
            String filename, tmpFilePath;
            File src, dst;
            for (Map.Entry entry : parms.entrySet()) {
                if (entry.getKey().toString().substring(0, 8).equalsIgnoreCase("filename")) {
                    filename = entry.getValue().toString();
                    tmpFilePath = files.get(entry.getKey().toString());
                    dst = new File(mCurrentDir, filename);
                    if (dst.exists()) {
                        return getResponse("Internal Error: File already exist");
                    }
                    src = new File(tmpFilePath);
                    if (! copyFile(src, dst)) {
                        return getResponse("Internal Error: Uploading failed");
                    }
                }
            }
            //return getResponse("Success" + getFileType(filename) + ":" + getFileSizeHtml(filename));
            return getResponse("Success");
        }

        if ("/copyfile".equalsIgnoreCase(uri)) {
            String source = parms.get("source");
            if (null == source) {
                return getResponse("Invalid parameter");
            }
            File src = getAbsolutePath(source);
            if (! src.exists()) {
                return getResponse("Error 404: File not found");
            }
            File dst = new File(mCurrentDir, src.getName());
            if (dst.exists()) {
                return getResponse("Object already exist");
            }
            if (! copyFile(src, dst)) {
                return getResponse("Internal Error: Copying failed");
            }
            String type = (dst.isFile()) ? getFileType(dst.getName()) : "dir";
            String size = (dst.isFile()) ? ":" + getFileSizeHtml(dst.getName()) : "";
            return getResponse("Success:" + type + size);
        }

        if ("/movefile".equalsIgnoreCase(uri)) {
            String source = parms.get("source");
            if (null == source) {
                return getResponse("Invalid parameter");
            }
            File src = getAbsolutePath(source);
            if (! src.exists()) {
                return getResponse("Error 404: File not found");
            }
            File dst = new File(mCurrentDir, src.getName());
            if (dst.exists()) {
                return getResponse("Object already exist");
            }
            if (! copyFile(src, dst)) {
                return getResponse("Internal Error: Copying failed");
            }
            if (! deleteFile(src)) {
                return getResponse("Internal Error: Deleting failed");
            }
            String type = (dst.isFile()) ? getFileType(dst.getName()) : "dir";
            String size = (dst.isFile()) ? ":" + getFileSizeHtml(dst.getName()) : "";
            return getResponse("Success:" + type + size);
        }

        if ("/deletefile".equalsIgnoreCase(uri)) {
            String filename = parms.get("filename");
            if (null == filename) {
                return getResponse("Invalid parameter");
            }
            File src = new File(mCurrentDir, filename);
            if (! src.exists()) {
                return getResponse("Error 404: File not found");
            }
            if (! deleteFile(src)) {
                return getResponse("Internal Error: Failure");
            }
            return getResponse("Success");
        }

        if ("/rename".equalsIgnoreCase(uri)) {
            String filename = parms.get("filename");
            String dest = parms.get("destination");
            if (null == filename || null == dest) {
                return getResponse("Invalid parameter");
            }
            File src = new File(mCurrentDir, filename);
            File dst = new File(mCurrentDir, dest);
            if (! src.exists()) {
                return getResponse("Error 404: File not found");
            }
            if (dst.exists()) {
                return getResponse("Internal Error: File already exist");
            }
            if (! src.renameTo(dst)) {
                return getResponse("Internal Error: Rename failure");
            }
            return getResponse("Success");
        }

        if (("/" + DIR_EXPLORER).equalsIgnoreCase(uri)) {
            return getResponse(listDirectory(mCurrentDir));
        }

        if (DIR_EXPLORER.equalsIgnoreCase(getRootName(uri))) {
            return respond(uri, Collections.unmodifiableMap(headers));
        }

        if ("/camera/preview.jpg".equalsIgnoreCase(uri)) {
            if (Main.getCameraActivity() != null) {
                if (Main.getCameraActivity().isCameraActive()) {
                    return createResponse(Response.Status.OK, "image/jpeg", Main.getCameraActivity().getInputStream());
                }
            }
            return getResponse("Camera inactive");
        }

        if ("/camera/isactive".equalsIgnoreCase(uri)) {
            if (Main.getCameraActivity() != null) {
                return getResponse(String.valueOf(Main.getCameraActivity().isCameraActive()));
            }
            return getResponse("false");
        }

        if ("/camera/isrecording".equalsIgnoreCase(uri)) {
            if (Main.getCameraActivity() != null) {
                return getResponse(String.valueOf(Main.getCameraActivity().isRecording()));
            }
            return getResponse("false");
        }

        if ("/camera/setcameraon".equalsIgnoreCase(uri)) {
            if (Main.getCameraId() != -1) {
                Main.getService().setCameraOn(Main.getCameraId());
                return getResponse("Success");
            }
            return getResponse("Rear Facing Camera is not supported");
        }

        if ("/camera/setcamerafronton".equalsIgnoreCase(uri)) {
            if (Main.getCameraFrontId() != -1) {
                Main.getService().setCameraOn(Main.getCameraFrontId());
                return getResponse("Success");
            }
            return getResponse("Front Facing Camera is not supported");
        }

        if ("/camera/setcameraoff".equalsIgnoreCase(uri)) {
            Main.getService().setCameraOff();
            return getResponse("Success");
        }

        if ("/camera/takepicture".equalsIgnoreCase(uri)) {
            if (Main.getCameraActivity() != null) {
                Main.getCameraActivity().takePicture();
                return getResponse("Success");
            }
            return getResponse("Camera inactive");
        }

        if ("/camera/startrecord".equalsIgnoreCase(uri)) {
            if (Main.getCameraActivity() != null) {
                if (Main.getCameraActivity().startRecordVideo()) {
                    return getResponse("Success");
                }
                return getResponse("Failed starting record video");
            }
            return getResponse("Camera inactive");
        }

        if ("/camera/stoprecord".equalsIgnoreCase(uri)) {
            if (Main.getCameraActivity() != null) {
                Main.getCameraActivity().stopRecordVideo();
                return getResponse("Success");
            }
            return getResponse("Camera inactive");
        }

        if ("/camera/setflashon".equalsIgnoreCase(uri)) {
            if (Main.getCameraActivity() != null) {
                return getResponse(String.valueOf(Main.getCameraActivity().setFlashlight(true)));
            }
            return getResponse("Camera inactive");
        }

        if ("/camera/setflashoff".equalsIgnoreCase(uri)) {
            if (Main.getCameraActivity() != null) {
                return getResponse(String.valueOf(Main.getCameraActivity().setFlashlight(false)));
            }
            return getResponse("Camera inactive");
        }

        if ("/camera/setcameraparameters".equalsIgnoreCase(uri)) {
            if (Main.getCameraActivity() != null) {
                if (Main.getCameraActivity().isCameraActive()) {
                    String ps = parms.get("picturesize");
                    String sm = parms.get("scenemode");
                    String wb = parms.get("whitebalance");
                    String ce = parms.get("coloreffect");
                    String vp = parms.get("videoprofile");
                    String ev = parms.get("exposurevalue");
                    if (ps != null || ! ps.contentEquals("")) {
                        Main.getCameraActivity().setPictureSize(ps);
                    }
                    if (sm != null || ! sm.contentEquals("")) {
                        Main.getCameraActivity().setSceneMode(sm);
                    }
                    if (wb != null || ! wb.contentEquals("")) {
                        Main.getCameraActivity().setWhiteBalance(wb);
                    }
                    if (ce != null || ! ce.contentEquals("")) {
                        Main.getCameraActivity().setColorEffect(ce);
                    }
                    if (vp != null || ! vp.contentEquals("")) {
                        Main.getCameraActivity().setVideoProfile(vp);
                    }
                    if (ev != null || ! ev.contentEquals("")) {
                        int iev = Integer.parseInt(ev);
                        Main.getCameraActivity().setExposureCompensation(iev);
                    }
                    return getResponse("Success");
                }
            }
            return getResponse("Camera inactive");
        }

        if ("/logout".equalsIgnoreCase(uri)) {
            if (Main.getCameraActivity() != null) {
                Main.getCameraActivity().finish();
            }
            Main.setUserIp("");
            Main.saveSharedPreferences();
            return getResponse("Success: Bye <strong>" + Main.getUsername() + "</strong>");
        }

        if ("/shutdown".equalsIgnoreCase(uri)) {
            Main.getService().shutdown();
            return null;
        }

        return getResponse("Error 404: File not found");
    }

    private boolean deleteFile(File target) {
        if (target.isDirectory()) {
            for (File child : target.listFiles()) {
                if (! deleteFile(child)) {
                    return false;
                }
            }
        }
        return target.delete();
    }

    private static boolean copyFile(File source, File target) {
        if (source.isDirectory()) {
            if (! target.exists()) {
                if (! target.mkdir()) {
                    return false;
                }
            }
            String[] children = source.list();
            for (int i = 0; i < source.listFiles().length; i++) {
                if (! copyFile(new File(source, children[i]), new File(target, children[i]))) {
                    return false;
                }
            }
        } else {
            try {
                InputStream in = new FileInputStream(source);
                OutputStream out = new FileOutputStream(target);

                byte[] buf = new byte[65536];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                in.close();
                out.close();
            }
            catch (IOException ioe) {
                return false;
            }
        }
        return true;
    }

    private Response respond(String uri, Map<String, String> headers) {
        File f = new File(ROOT_DIR, uri.replace("/" + getRootName(uri), ""));
        return (f.isDirectory()) ? serveDirectory(uri, f) : serveFile(uri, headers, f);
    }

    private Response serveDirectory(String uri, File f) {
        // Browsers get confused without '/' after the directory, send a redirect.
        if (f.isDirectory() && ! uri.endsWith("/")) {
            uri += "/";
            Response res = createResponse(Response.Status.REDIRECT, MIME_HTML, "<html><body>Redirected: <a href=\"" + uri + "\">" + uri + "</a></body></html>");
            res.addHeader("Location", uri);
            return res;
        }

        if (! f.canRead()) {
            return getResponse("Forbidden: No directory listing");
        }

        mCurrentDir = f;
        return getResponse(listDirectory(f));
    }

    /**
     * Serves file from homeDir and its' subdirectories (only).
     * Uses only URI, ignores all headers and HTTP parameters.
     */
    private Response serveFile(String uri, Map<String, String> header, File file) {
        Response res;
        String mime = getMimeTypeForFile(uri);
        try {
            // Calculate etag
            String etag = Integer.toHexString((file.getAbsolutePath() +
                    file.lastModified() + "" + file.length()).hashCode());

            // Support (simple) skipping:
            long startFrom = 0;
            long endAt = -1;
            String range = header.get("range");
            if (range != null) {
                if (range.startsWith("bytes=")) {
                    range = range.substring("bytes=".length());
                    int minus = range.indexOf('-');
                    try {
                        if (minus > 0) {
                            startFrom = Long.parseLong(range.substring(0, minus));
                            endAt = Long.parseLong(range.substring(minus + 1));
                        }
                    }
                    catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                }
            }

            // Change return code and add Content-Range header when skipping is requested
            long fileLen = file.length();
            if (range != null && startFrom >= 0) {
                if (startFrom >= fileLen) {
                    res = createResponse(Response.Status.RANGE_NOT_SATISFIABLE, MIME_PLAINTEXT, "");
                    res.addHeader("Content-Range", "bytes 0-0/" + fileLen);
                    res.addHeader("ETag", etag);
                }
                else {
                    if (endAt < 0) {
                        endAt = fileLen - 1;
                    }
                    long newLen = endAt - startFrom + 1;
                    if (newLen < 0) {
                        newLen = 0;
                    }

                    final long dataLen = newLen;
                    FileInputStream fis = new FileInputStream(file) {
                        @Override
                        public int available() throws IOException {
                            return (int) dataLen;
                        }
                    };
                    fis.skip(startFrom);

                    res = createResponse(Response.Status.PARTIAL_CONTENT, mime, fis);
                    res.addHeader("Content-Length", "" + dataLen);
                    res.addHeader("Content-Range", "bytes " + startFrom + "-" + endAt + "/" + fileLen);
                    res.addHeader("ETag", etag);
                }
            }
            else {
                if (etag.equals(header.get("if-none-match")))
                    res = createResponse(Response.Status.NOT_MODIFIED, mime, "");
                else {
                    res = createResponse(Response.Status.OK, mime, new FileInputStream(file));
                    res.addHeader("Content-Length", "" + fileLen);
                    res.addHeader("ETag", etag);
                }
            }
        }
        catch (IOException e) {
            res = getResponse("Forbidden: Reading file failed");
        }

        return (res == null) ? getResponse("Error 404: File not found") : res;
    }

    // Get MIME type from file name extension, if possible
    private String getMimeTypeForFile(String uri) {
        int dot = uri.lastIndexOf('.');
        String mime = null;
        if (dot >= 0) {
            mime = MIME_TYPES.get(uri.substring(dot + 1).toLowerCase());
        }
        return (mime == null) ? MIME_DEFAULT_BINARY : mime;
    }

    private String getFileType(String uri) {
        int dot = uri.lastIndexOf('.');
        String type = null;
        if (dot >=0) {
            type = MIME_TYPES.get(uri.substring(dot + 1).toLowerCase());
        }
        type = (type == null) ? "bin" : type.substring(0, 3);
        if (type.equals("tex")) {
            type = "txt";
        }
        else if (type.equals("ima")) {
            type = "img";
        }
        return type;
    }

    private Response createAssetsResponse(String file) {
        InputStream is;
        try {
            is = Main.getService().getResources().getAssets().open(file);
        }
        catch (IOException e) {
            return getResponse("Internal Error IO Exception: " + e.getMessage());
        }
        return createResponse(Response.Status.OK, getMimeTypeForFile(file), is);
    }

    private Response getResponse(String message) {
        return createResponse(Response.Status.OK, MIME_PLAINTEXT, message);
    }

    // Announce that the file server accepts partial content requests
    private Response createResponse(Response.Status status, String mimeType, InputStream message) {
        Response res = new Response(status, mimeType, message);
        res.addHeader("Accept-Ranges", "bytes");
        return res;
    }

    // Announce that the file server accepts partial content requests
    private Response createResponse(Response.Status status, String mimeType, String message) {
        Response res = new Response(status, mimeType, message);
        res.addHeader("Accept-Ranges", "bytes");
        return res;
    }

    private String listDirectory(File f) {
        String curDir = getPathName(f);
        String encoded = encodeURI(curDir);
        StringBuilder sb = new StringBuilder("<!--success-->");
        File path = f;
        String activeDir = "<a id='parent_option' rel='directory' href='" + encoded + "'>" +
                getDirName(f) + "</a>";
        while (! path.equals(ROOT_DIR)) {
            path = path.getParentFile();
            activeDir = "<a rel='directory' href='" + encodeURI(getPathName(path)) +
                    "'>" + getDirName(path) + "</a>&nbsp;/&nbsp;" + activeDir;
        }
        sb.append("<div id='parent_dir'><h3>" + activeDir + "</h3></div>")
          .append("<div id='total_item'></div>");

        List<String> dirs = Arrays.asList(f.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return new File(dir, name).isDirectory();
            }
        }));
        Collections.sort(dirs, String.CASE_INSENSITIVE_ORDER);

        List<String> files = Arrays.asList(f.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return new File(dir, name).isFile();
            }
        }));
        Collections.sort(files, String.CASE_INSENSITIVE_ORDER);

        sb.append("<ul id='dirs'>");
        int count = 0;
        if (dirs.size() > 0) {
            for (String dir : dirs) {
                sb.append("<li id='dir_" + count + "'><a rel='directory' href='")
                  .append(encoded + encodeURI(dir) + "/'>" + dir + "</a></li>");
                count++;
            }
        }
        sb.append("</ul>")
          .append("<ul id='files'>");
        if (files.size() > 0) {
            count = 0;
            for (String file : files) {
                sb.append("<li id='file_" + count + "' class='" + getFileType(file) + "'>")
                  .append("<a href='" + encoded + encodeURI(file) + "'>" + file + "</a>")
                  .append("&nbsp;&nbsp;&nbsp;" + getFileSizeHtml(file) + "</li>");
                count++;
            }
        }
        sb.append("</ul>");
        return sb.toString();
    }

    private String getFileSizeHtml(String file){
        StringBuilder sb =  new StringBuilder("");
        File f = new File(mCurrentDir, file);
        long len = f.length();
        sb.append("<span class='filesize'>(&nbsp;");
        if (len < 1024) {
            sb.append(len).append("&nbsp;bytes");
        }
        else if (len < 1048576) {
            sb.append(String.format("%1.2f&nbsp;KB", len / 1.024e3));
        }
        else {
            sb.append(String.format("%1.2f&nbsp;MB", len / 1.048576e6));
        }
        sb.append("&nbsp;)</span>");
        return sb.toString();
    }

    private String getRootName(String path) {
        int s = path.indexOf('/', 1);
        if (s > 1) {
            return path.substring(1, s);
        }
        return path.substring(1);
    }

    private File getAbsolutePath(String file) {
        String f = file;
        if (getRootName(file).equalsIgnoreCase(DIR_EXPLORER)) {
            f = f.substring(DIR_EXPLORER.length()+1);
        }
        return new File(ROOT_DIR, f);
    }

    private String getPathName(File path) {
        File p = path;
        String sp = "";
        while (! p.equals(ROOT_DIR)) {
            sp = "/" + p.getName() + sp;
            p = p.getParentFile();
        }
        return "/" + DIR_EXPLORER + sp + "/";
    }

    private String getDirName(File dir) {
        return (dir.equals(ROOT_DIR)) ? DIR_EXPLORER : dir.getName();
    }

    /**
     * URL-encodes everything between "/"-characters. Encodes spaces as '%20' instead of '+'.
     */
    private String encodeURI(String uri) {
        return Uri.encode(uri).replace("%2F", "/");
    }

    private String getRemoteIp(Map<String, String> headers) {
        String ip = headers.get("http-client-ip");
        if (ip == null) {
            ip = headers.get("remote-addr");
        }
        return ip;
    }
}
