package com.gun0912.tedpermission;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AppOpsManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.WindowManager;

import com.gun0912.tedpermission.busevent.TedBusProvider;
import com.gun0912.tedpermission.busevent.TedPermissionEvent;
import com.gun0912.tedpermission.util.Dlog;

import java.util.ArrayList;

/**
 * Created by TedPark on 16. 2. 17..
 * Usage Stats Permission by Darkhost on 16. 12. 13
 */
public class TedPermissionActivity extends AppCompatActivity {

    public static final int REQ_CODE_PERMISSION_REQUEST = 10;
    public static final int REQ_CODE_REQUEST_SETTING = 20;
    public static final int REQ_CODE_SYSTEM_ALERT_WINDOW_PERMISSION_REQUEST = 30;
    public static final int REQ_CODE_SYSTEM_ALERT_WINDOW_PERMISSION_REQUEST_SETTING = 31;
    public static final int REQ_CODE_PACKAGE_USAGE_STATS_PERMISSION_REQUEST = 40;
    public static final int REQ_CODE_PACKAGE_USAGE_STATS_PERMISSION_REQUEST_SETTING = 41;


    public static final String EXTRA_PERMISSIONS = "permissions";
    public static final String EXTRA_RATIONALE_MESSAGE = "rationale_message";
    public static final String EXTRA_DENY_MESSAGE = "deny_message";
    public static final String EXTRA_PACKAGE_NAME = "package_name";
    public static final String EXTRA_SETTING_BUTTON = "setting_button";
    public static final String EXTRA_SETTING_BUTTON_TEXT = "setting_button_text";
    public static final String EXTRA_RATIONALE_CONFIRM_TEXT = "rationale_confirm_text";
    public static final String EXTRA_DENIED_DIALOG_CLOSE_TEXT = "denied_dialog_close_text";

    String rationale_message;
    String denyMessage;
    String[] permissions;
    String packageName;
    boolean hasSettingButton;
    String settingButtonText;

    String deniedCloseButtonText;
    String rationaleConfirmText;
    boolean isShownRationaleDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Dlog.d("");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        setupFromSavedInstanceState(savedInstanceState);
        if(needWindowPermission()) {
            requestWindowPermission();
        }
        else {
            if(needUsageStatsPermission()) {
                requestUsageStatsPermission(true);
            }
            else {
                checkPermissions(false);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Dlog.d("");
    }

    private void setupFromSavedInstanceState(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            permissions = savedInstanceState.getStringArray(EXTRA_PERMISSIONS);
            rationale_message = savedInstanceState.getString(EXTRA_RATIONALE_MESSAGE);
            denyMessage = savedInstanceState.getString(EXTRA_DENY_MESSAGE);
            packageName = savedInstanceState.getString(EXTRA_PACKAGE_NAME);


            hasSettingButton = savedInstanceState.getBoolean(EXTRA_SETTING_BUTTON, true);

            rationaleConfirmText = savedInstanceState.getString(EXTRA_RATIONALE_CONFIRM_TEXT);
            deniedCloseButtonText = savedInstanceState.getString(EXTRA_DENIED_DIALOG_CLOSE_TEXT);


            settingButtonText = savedInstanceState.getString(EXTRA_SETTING_BUTTON_TEXT);
        } else {

            Intent intent = getIntent();
            permissions = intent.getStringArrayExtra(EXTRA_PERMISSIONS);
            rationale_message = intent.getStringExtra(EXTRA_RATIONALE_MESSAGE);
            denyMessage = intent.getStringExtra(EXTRA_DENY_MESSAGE);
            packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME);
            hasSettingButton = intent.getBooleanExtra(EXTRA_SETTING_BUTTON, true);
            rationaleConfirmText = intent.getStringExtra(EXTRA_RATIONALE_CONFIRM_TEXT);
            deniedCloseButtonText = intent.getStringExtra(EXTRA_DENIED_DIALOG_CLOSE_TEXT);
            settingButtonText = intent.getStringExtra(EXTRA_SETTING_BUTTON_TEXT);

        }


    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putStringArray(EXTRA_PERMISSIONS, permissions);
        outState.putString(EXTRA_RATIONALE_MESSAGE, rationale_message);
        outState.putString(EXTRA_DENY_MESSAGE, denyMessage);
        outState.putString(EXTRA_PACKAGE_NAME, packageName);
        outState.putBoolean(EXTRA_SETTING_BUTTON, hasSettingButton);
        outState.putString(EXTRA_SETTING_BUTTON, deniedCloseButtonText);
        outState.putString(EXTRA_RATIONALE_CONFIRM_TEXT, rationaleConfirmText);
        outState.putString(EXTRA_SETTING_BUTTON_TEXT, settingButtonText);

        super.onSaveInstanceState(outState);
    }


    private void permissionGranted() {
        TedBusProvider.getInstance().post(new TedPermissionEvent(true, null));
        finish();
        overridePendingTransition(0, 0);
    }

    private void permissionDenied(ArrayList<String> deniedpermissions) {
        TedBusProvider.getInstance().post(new TedPermissionEvent(false, deniedpermissions));
        finish();
        overridePendingTransition(0, 0);
    }


    private boolean needWindowPermission() {
        for (String permission : permissions) {
            if (permission.equals(Manifest.permission.SYSTEM_ALERT_WINDOW)) {
                return !hasWindowPermission();
            }
        }
        return false;
    }

    @TargetApi(Build.VERSION_CODES.M)
    private boolean hasWindowPermission(){
        return Settings.canDrawOverlays(getApplicationContext());
    }

    private void requestWindowPermission() {
        Uri uri = Uri.fromParts("package", packageName, null);
        final Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, uri);

        if(!TextUtils.isEmpty(rationale_message)) {
            new AlertDialog.Builder(this)
                    .setMessage(rationale_message)
                    .setCancelable(false)

                    .setNegativeButton(rationaleConfirmText, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            startActivityForResult(intent, REQ_CODE_SYSTEM_ALERT_WINDOW_PERMISSION_REQUEST);
                        }
                    })
                    .show();
            isShownRationaleDialog = true;
        }else {
            startActivityForResult(intent, REQ_CODE_SYSTEM_ALERT_WINDOW_PERMISSION_REQUEST);
        }
    }

    private boolean needUsageStatsPermission() {
        for (String permission : permissions) {
            if (permission.equals(Manifest.permission.PACKAGE_USAGE_STATS)) {
                return !hasUsageStatsPermission();
            }
        }
        return false;
    }

    @TargetApi(Build.VERSION_CODES.M)
    // http://stackoverflow.com/questions/28921136/how-to-check-if-android-permission-package-usage-stats-permission-is-given
    private boolean hasUsageStatsPermission() {
        AppOpsManager appOpsManager = (AppOpsManager) getApplicationContext().getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOpsManager.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), getApplicationContext().getPackageName());
        boolean granted = mode == AppOpsManager.MODE_ALLOWED;
        return granted;
    }

    private void requestUsageStatsPermission(boolean showDialog) {
        final Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
        // Dialog를 보여주고 싶은경우 (보여주고 싶은 경우는 SYSTEM_ALERT_WINDOW 권한이 없는 앱일경우)
        // 두 권한을 모두 받는 앱의 경우 같은 창이 두번 뜨므로 추가
        if(showDialog)
        {
            if (!TextUtils.isEmpty(rationale_message)) {
                new AlertDialog.Builder(this)
                        .setMessage(rationale_message)
                        .setCancelable(false)

                        .setNegativeButton(rationaleConfirmText, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                startActivityForResult(intent, REQ_CODE_PACKAGE_USAGE_STATS_PERMISSION_REQUEST);
                            }
                        })
                        .show();
                isShownRationaleDialog = true;
            } else {
                startActivityForResult(intent, REQ_CODE_PACKAGE_USAGE_STATS_PERMISSION_REQUEST);
            }
        }
        else // Dialog를 보여주지 않아도 될경우
        {
            startActivityForResult(intent, REQ_CODE_PACKAGE_USAGE_STATS_PERMISSION_REQUEST);
        }
    }


    private void checkPermissions(boolean fromOnActivityResult) {
        Dlog.d("");

        ArrayList<String> needPermissions = new ArrayList<>();


        for (String permission : permissions) {
            if (permission.equals(Manifest.permission.SYSTEM_ALERT_WINDOW) || permission.equals(Manifest.permission.PACKAGE_USAGE_STATS)) {
                if (!hasWindowPermission() || !hasUsageStatsPermission()) {
                    needPermissions.add(permission);
                }
            } else {
                if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                    needPermissions.add(permission);
                }
            }
        }

        if (needPermissions.isEmpty()) {
            permissionGranted();
        } else if (fromOnActivityResult) { //From Setting Activity
            permissionDenied(needPermissions);
        } else if (needPermissions.size() == 1 && needPermissions.contains(Manifest.permission.SYSTEM_ALERT_WINDOW)) {   // window permission deny
            permissionDenied(needPermissions);
        } else if (needPermissions.size() == 1 && needPermissions.contains(Manifest.permission.PACKAGE_USAGE_STATS)) { // Usage Stats Permission Deny
            permissionDenied(needPermissions);
        } else if (!isShownRationaleDialog && !TextUtils.isEmpty(rationale_message)) { // //Need Show Rationale
            showRationaleDialog(needPermissions);
        } else { // //Need Request Permissions
            requestPermissions(needPermissions);
        }
    }

    public void requestPermissions(ArrayList<String> needPermissions) {
        ActivityCompat.requestPermissions(this, needPermissions.toArray(new String[needPermissions.size()]), REQ_CODE_PERMISSION_REQUEST);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        Dlog.d("");
        ArrayList<String> deniedPermissions = new ArrayList<>();


        for (int i = 0; i < permissions.length; i++) {
            String permission = permissions[i];
            if (grantResults[i] == PackageManager.PERMISSION_DENIED) {


                deniedPermissions.add(permission);

            }
        }

        if (deniedPermissions.isEmpty()) {
            permissionGranted();
        } else {


            showPermissionDenyDialog(deniedPermissions);


        }


    }


    private void showRationaleDialog(final ArrayList<String> needPermissions) {

        new AlertDialog.Builder(this)
                .setMessage(rationale_message)
                .setCancelable(false)

                .setNegativeButton(rationaleConfirmText, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        requestPermissions(needPermissions);

                    }
                })
                .show();
        isShownRationaleDialog = true;


    }

    public void showPermissionDenyDialog(final ArrayList<String> deniedPermissions) {

        if (TextUtils.isEmpty(denyMessage)) {
            // denyMessage 설정 안함
            permissionDenied(deniedPermissions);
            return;
        }


        AlertDialog.Builder builder = new AlertDialog.Builder(this);


        builder.setMessage(denyMessage)
                .setCancelable(false)

                .setNegativeButton(deniedCloseButtonText, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        permissionDenied(deniedPermissions);
                    }
                });

        if (hasSettingButton) {


            if (TextUtils.isEmpty(settingButtonText)) {
                settingButtonText = getString(R.string.tedpermission_setting);
            }

            builder.setPositiveButton(settingButtonText, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                    try {
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                .setData(Uri.parse("package:" + packageName));
                        startActivityForResult(intent, REQ_CODE_REQUEST_SETTING);
                    } catch (ActivityNotFoundException e) {
                        e.printStackTrace();
                        Dlog.e(e.toString());
                        Intent intent = new Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS);
                        startActivityForResult(intent, REQ_CODE_REQUEST_SETTING);
                    }

                }
            });

        }
        builder.show();
    }

    public void showWindowPermissionDenyDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(denyMessage)
                .setCancelable(false)
                .setNegativeButton(deniedCloseButtonText, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        checkPermissions(false);
                    }
                });

        if (hasSettingButton) {
            if (TextUtils.isEmpty(settingButtonText)) {
                settingButtonText = getString(R.string.tedpermission_setting);
            }

            builder.setPositiveButton(settingButtonText, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Uri uri = Uri.fromParts("package", packageName, null);
                    final Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, uri);
                    startActivityForResult(intent, REQ_CODE_SYSTEM_ALERT_WINDOW_PERMISSION_REQUEST_SETTING);
                }
            });

        }
        builder.show();
    }

    public void showUsageStatsPermissionDenyDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(denyMessage)
                .setCancelable(false)
                .setNegativeButton(deniedCloseButtonText, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        checkPermissions(false);
                    }
                });

        if (hasSettingButton) {
            if (TextUtils.isEmpty(settingButtonText)) {
                settingButtonText = getString(R.string.tedpermission_setting);
            }

            builder.setPositiveButton(settingButtonText, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    final Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
                    startActivityForResult(intent, REQ_CODE_PACKAGE_USAGE_STATS_PERMISSION_REQUEST_SETTING);
                }
            });

        }
        builder.show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQ_CODE_REQUEST_SETTING:
                checkPermissions(true);
                break;
            case REQ_CODE_SYSTEM_ALERT_WINDOW_PERMISSION_REQUEST:   // 최초 ALERT WINDOW 요청에 대한 결과
                if(!hasWindowPermission() && !TextUtils.isEmpty(denyMessage)){  // 권한이 거부되고 denyMessage 가 있는 경우
                    showWindowPermissionDenyDialog();
                }else {     // 권한있거나 또는 denyMessage가 없는 경우
                    if(needUsageStatsPermission() && !hasUsageStatsPermission()) { // UsageStats 권한이 필요한데 없는경우
                        requestUsageStatsPermission(false); // UsageStats 권한을 요청한다
                    }
                    else // 권한이 필요 없을경우
                    {
                        checkPermissions(false); // 일반 Permission 권한을 체크한다.
                    }
                }
                break;
            case REQ_CODE_SYSTEM_ALERT_WINDOW_PERMISSION_REQUEST_SETTING:   //  ALERT WINDOW 권한 설정 실패후 재 요청에 대한 결과
                if(needUsageStatsPermission() && !hasUsageStatsPermission()) { // UsageStats 권한이 필요한데 없는경우
                    requestUsageStatsPermission(false); // UsageStats 권한을 요청한다
                }
                else // 권한이 필요 없을경우
                {
                    checkPermissions(false); // 일반 Permission 권한을 체크한다.
                }
                break;
            case REQ_CODE_PACKAGE_USAGE_STATS_PERMISSION_REQUEST:
                if(!hasUsageStatsPermission() && !TextUtils.isEmpty(denyMessage)){
                    showUsageStatsPermissionDenyDialog();
                }else {
                    checkPermissions(false);
                }
                break;
            case REQ_CODE_PACKAGE_USAGE_STATS_PERMISSION_REQUEST_SETTING:
                    checkPermissions(false);
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }

    }



}
