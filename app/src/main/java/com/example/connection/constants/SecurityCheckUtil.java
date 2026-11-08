package com.example.connection.constants;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;
import android.os.Debug;
import android.util.Base64;
import android.util.Log;

import java.io.File;
import java.security.MessageDigest;

/**
 * Tiện ích kiểm tra tính toàn vẹn ứng dụng và an toàn thiết bị (Anti-Tampering & Anti-Hooking).
 */
public class SecurityCheckUtil {

    private static final String TAG = "SecurityCheckUtil";

    private static final String[] SU_PATHS = {
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su",
            "/system/app/Superuser.apk",
            "/system/xbin/daemonsu",
            "/system/etc/init.d/99SuperSUDaemon"
    };

    /**
     * Kiểm tra xem thiết bị có đang bị can thiệp (Root, Hooking, Debugger gắn vào) hay không.
     *
     * @return true nếu phát hiện bất thường, false nếu an toàn.
     */
    public static boolean isDeviceCompromised() {
        if (Debug.isDebuggerConnected() || Debug.waitingForDebugger()) {
            Log.w(TAG, "Debugger detected attached to process");
            return true;
        }

        for (String path : SU_PATHS) {
            try {
                File file = new File(path);
                if (file.exists()) {
                    Log.w(TAG, "Su binary detected at: " + path);
                    return true;
                }
            } catch (Exception ignored) {
            }
        }

        String buildTags = Build.TAGS;
        if (buildTags != null && buildTags.contains("test-keys")) {
            Log.w(TAG, "Build test-keys detected");
            return true;
        }

        return false;
    }

    /**
     * Kiểm tra chữ ký số của ứng dụng so với giá trị băm SHA-256 mong đợi.
     * Hỗ trợ cả 2 định dạng:
     * 1. Định dạng HEX (từ `keytool` hoặc `.\gradlew signingReport`), ví dụ: "A1:B2:C3:..." hoặc "a1b2c3..."
     * 2. Định dạng Base64, ví dụ: "AbCd123...="
     *
     * @param context            Context ứng dụng
     * @param expectedCertSha256 Chuỗi SHA-256 mong đợi (HEX hoặc Base64)
     * @return true nếu chữ ký bị sửa đổi hoặc lỗi trích xuất, false nếu chữ ký khớp chính xác.
     */
    public static boolean isTampered(Context context, String expectedCertSha256) {
        if (expectedCertSha256 == null || expectedCertSha256.trim().isEmpty()) {
            return false;
        }
        String cleanExpected = expectedCertSha256.replace(":", "").trim();

        try {
            PackageManager pm = context.getPackageManager();
            String packageName = context.getPackageName();
            Signature[] signatures;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                PackageInfo packageInfo = pm.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES);
                if (packageInfo.signingInfo == null) return true;
                signatures = packageInfo.signingInfo.getApkContentsSigners();
            } else {
                @SuppressWarnings("deprecation")
                PackageInfo packageInfo = pm.getPackageInfo(packageName, PackageManager.GET_SIGNATURES);
                signatures = packageInfo.signatures;
            }

            if (signatures == null || signatures.length == 0) return true;

            for (Signature sig : signatures) {
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                byte[] digest = md.digest(sig.toByteArray());

                // 1. So khớp định dạng HEX (không phân biệt hoa thường và bỏ dấu hai chấm ':')
                String hexHash = bytesToHex(digest);
                if (hexHash.equalsIgnoreCase(cleanExpected)) {
                    return false;
                }

                // 2. So khớp định dạng Base64
                String base64Hash = Base64.encodeToString(digest, Base64.NO_WRAP);
                if (base64Hash.equals(expectedCertSha256.trim())) {
                    return false;
                }
            }
            Log.w(TAG, "Apk signature hash mismatch");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to verify signature", e);
            return true;
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
