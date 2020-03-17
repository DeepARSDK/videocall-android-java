package ai.deepar.videocall;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/**
 * IMPORTANT NOTE asking for a permission in the onResume() method can cause an infinite loop of onResume() cycles,
 * permissions MUST be checked in the onStart() method
 */
public abstract class PermissionsActivity extends Activity {

    public interface SinglePermissionCallback {
        void onPermissionGranted();
        void onPermissionDenied();
    }

    public interface MultiplePermissionsCallback {
        void onAllPermissionsGranted();
        void onPermissionsDenied(List<String> deniedPermissions);
    }

    protected SinglePermissionCallback singlePermissionCallback;
    protected MultiplePermissionsCallback multiplePermissionsCallback;

    /**
     * Single permission request methods
     * IMPORTANT NOTE asking for a permission in the onResume() method can cause an infinite loop of onResume() cycles,
     * permissions MUST be checked in the onStart() method
     */
    protected void checkPermission(final String permission, final String explanationMessage, final int permissionRequestCode, SinglePermissionCallback callback) {
        if (permission == null || explanationMessage == null || callback == null) {
            throw new UnsupportedOperationException("Permission, explanationMessage or callback should not be null!");
        }

        singlePermissionCallback = callback;

        if (hasPermission(permission)) {
            callback.onPermissionGranted();
            return;
        }
        handlePermissionRequests(new String[]{permission}, explanationMessage, permissionRequestCode);
    }
    /******************************************************/

    /**
     * Multiple permissions request methods
     * IMPORTANT NOTE asking for a permission in the onResume() method can cause an infinite loop of onResume() cycles,
     * permissions MUST be checked in the onStart() method
     */
    protected void checkMultiplePermissions(final List<String> permissions, final String explanationMessage, final int multiplePermissionsRequestCode, MultiplePermissionsCallback callback) {
        if (permissions == null || explanationMessage == null || callback == null) {
            throw new UnsupportedOperationException("Permissions, explanationMessage or callback should not be null!");
        }

        multiplePermissionsCallback = callback;

        List<String> permissionsNeeded = new ArrayList<>();
        for (String permission : permissions) {
            if (!hasPermission(permission)) {
                permissionsNeeded.add(permission);
            }
        }

        if (permissionsNeeded.isEmpty()) {
            callback.onAllPermissionsGranted();
            return;
        }
        handlePermissionRequests(permissionsNeeded.toArray(new String[permissionsNeeded.size()]), explanationMessage, multiplePermissionsRequestCode);
    }

    /******************************************************/

    private boolean hasPermission(String permission) {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED;
    }

    private void handlePermissionRequests(final String[] permissions, String explanationMessage, final int permissionRequestCode) {
        // Should we show an explanation?
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, permissions[0])) {
            // Show an explanation to the user *asynchronously* -- don't block this thread waiting for the user's response!
            // After the user sees the explanation, try again to request the permission.
            showPermissionOkCancelDialog(explanationMessage, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    ActivityCompat.requestPermissions(
                            PermissionsActivity.this,
                            permissions, permissionRequestCode);
                }
            });
            return;
        }
        // No explanation needed, we can request the permission.
        ActivityCompat.requestPermissions(this, permissions, permissionRequestCode);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (singlePermissionCallback != null && permissions.length == 1) {
            handleSinglePermissionRequestResult(permissions, grantResults);
            return;
        }
        handleMultiplePermissionsRequestsResult(permissions, grantResults);
    }

    private void handleSinglePermissionRequestResult(String[] permissions, int[] grantResults) {
        SinglePermissionCallback callback = singlePermissionCallback;
        if (callback == null) {
            return;
        }

        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            callback.onPermissionGranted();
            Log.d("Permissions", "Permission Granted: " + permissions[0]);
            return;
        }
        callback.onPermissionDenied();
        Log.d("Permissions", "Permission Denied: " + permissions[0]);
    }

    private void handleMultiplePermissionsRequestsResult(String[] permissions, int[] grantResults) {
        MultiplePermissionsCallback callback = multiplePermissionsCallback;
        if (callback == null) {
            return;
        }

        List<String> deniedPermissions = new ArrayList<>();
        for (int i = 0; i < permissions.length; i++) {
            if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                deniedPermissions.add(permissions[i]);
            }
        }

        if (deniedPermissions.isEmpty()) {
            callback.onAllPermissionsGranted();
            return;
        }
        callback.onPermissionsDenied(deniedPermissions);
    }

    /**
     * Dialog methods
     */
    protected void showPermissionOkCancelDialog(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(this)
                .setMessage(message)
                .setPositiveButton(getString(android.R.string.ok), okListener)
                .setNegativeButton(getString(android.R.string.cancel), null)
                .create()
                .show();
    }

    protected void showPermissionOkDialog(String message) {
        new AlertDialog.Builder(this)
                .setMessage(message)
                .setPositiveButton(getString(android.R.string.ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .create()
                .show();
    }
}