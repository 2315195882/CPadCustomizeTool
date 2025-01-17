package com.saradabar.cpadcustomizetool.data.connection;

import static com.saradabar.cpadcustomizetool.util.Common.isDhizukuActive;
import static com.saradabar.cpadcustomizetool.util.Common.mDhizukuService;
import static com.saradabar.cpadcustomizetool.util.Common.tryBindDhizukuService;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.saradabar.cpadcustomizetool.R;
import com.saradabar.cpadcustomizetool.data.event.InstallEventListener;
import com.saradabar.cpadcustomizetool.data.installer.SplitInstaller;
import com.saradabar.cpadcustomizetool.util.Constants;
import com.saradabar.cpadcustomizetool.util.Preferences;
import com.saradabar.cpadcustomizetool.util.Toast;
import com.saradabar.cpadcustomizetool.util.Variables;
import com.saradabar.cpadcustomizetool.view.flagment.MainFragment;

import java.io.File;

import jp.co.benesse.dcha.dchaservice.IDchaService;

public class Updater implements InstallEventListener {

    IDchaService mDchaService;
    Activity activity;

    @SuppressLint("StaticFieldLeak")
    static Updater instance = null;

    public static Updater getInstance() {
        return instance;
    }

    public Updater(Activity act) {
        instance = this;
        activity = act;
    }

    @Override
    public void onInstallSuccess(int reqCode) {

    }

    /* 失敗 */
    @Override
    public void onInstallFailure(int reqCode, String str) {
        new MaterialAlertDialogBuilder(activity)
                .setMessage(activity.getString(R.string.dialog_info_failure_silent_install) + "\n" + str)
                .setCancelable(false)
                .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> activity.finish())
                .show();
    }

    @Override
    public void onInstallError(int reqCode, String str) {
        new MaterialAlertDialogBuilder(activity)
                .setMessage(activity.getString(R.string.dialog_error) + "\n" + str)
                .setCancelable(false)
                .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> activity.finish())
                .show();
    }

    public void installApk(Context context, int flag) {
        switch (Preferences.load(activity, Constants.KEY_FLAG_UPDATE_MODE, 1)) {
            case 0:
                try {
                    LinearProgressIndicator linearProgressIndicator = activity.findViewById(R.id.act_progress_main);
                    linearProgressIndicator.hide();
                } catch (Exception ignored) {
                }

                try {
                    MainFragment.getInstance().preGetApp.setSummary(R.string.pre_main_sum_get_app);
                } catch (Exception ignored) {
                }
                activity.startActivityForResult(new Intent(Intent.ACTION_VIEW).setDataAndType(Uri.fromFile(new File(new File(context.getExternalCacheDir(), "update.apk").getPath())), "application/vnd.android.package-archive").addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP), Constants.REQUEST_ACTIVITY_UPDATE);
                break;
            case 1:
                switch (flag) {
                    case 0:
                        new MaterialAlertDialogBuilder(activity)
                                .setCancelable(false)
                                .setTitle(R.string.dialog_title_update)
                                .setMessage(R.string.dialog_info_update_caution)
                                .setPositiveButton(R.string.dialog_common_ok, (dialog2, which2) -> {
                                    try {
                                        activity.startActivityForResult(new Intent(Intent.ACTION_VIEW, Uri.parse(Constants.URL_UPDATE)), Constants.REQUEST_ACTIVITY_UPDATE);
                                    } catch (ActivityNotFoundException ignored) {
                                        Toast.toast(activity, R.string.toast_unknown_activity);
                                        activity.finish();
                                    }
                                })
                                .show();
                        break;
                    case 1:
                        try {
                            LinearProgressIndicator linearProgressIndicator = activity.findViewById(R.id.act_progress_main);
                            linearProgressIndicator.hide();
                        } catch (Exception ignored) {
                        }

                        try {
                            MainFragment.getInstance().preGetApp.setSummary(R.string.pre_main_sum_get_app);
                        } catch (Exception ignored) {
                        }
                        new MaterialAlertDialogBuilder(activity)
                                .setCancelable(false)
                                .setTitle("インストール")
                                .setMessage("遷移先のページよりapkファイルをダウンロードしてadbでインストールしてください")
                                .setPositiveButton(R.string.dialog_common_ok, (dialog2, which2) -> {
                                    try {
                                        activity.startActivityForResult(new Intent(Intent.ACTION_VIEW, Uri.parse(Variables.DOWNLOAD_FILE_URL)), Constants.REQUEST_ACTIVITY_UPDATE);
                                    } catch (ActivityNotFoundException ignored) {
                                        Toast.toast(activity, R.string.toast_unknown_activity);
                                        activity.finish();
                                    }
                                })
                                .show();
                        break;
                }
                break;
            case 2:
                try {
                    LinearProgressIndicator linearProgressIndicator = activity.findViewById(R.id.act_progress_main);
                    linearProgressIndicator.setIndeterminate(true);
                    linearProgressIndicator.show();
                } catch (Exception ignored) {
                }

                try {
                    MainFragment.getInstance().preGetApp.setSummary(R.string.progress_state_installing);
                } catch (Exception ignored) {
                }

                if (tryBindDchaService()) {
                    Runnable runnable = () -> {
                        if (!tryInstallPackage()) {
                            try {
                                LinearProgressIndicator linearProgressIndicator = activity.findViewById(R.id.act_progress_main);
                                linearProgressIndicator.hide();
                            } catch (Exception ignored) {
                            }

                            try {
                                MainFragment.getInstance().preGetApp.setSummary(R.string.pre_main_sum_get_app);
                            } catch (Exception ignored) {
                            }

                            new MaterialAlertDialogBuilder(activity)
                                    .setCancelable(false)
                                    .setMessage(context.getResources().getString(R.string.dialog_error) + "\n繰り返し発生する場合は”アプリ設定→アップデートモードを選択”が有効なモードに設定されているかをご確認ください")
                                    .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> activity.finish())
                                    .show();
                        } else {
                            try {
                                LinearProgressIndicator linearProgressIndicator = activity.findViewById(R.id.act_progress_main);
                                linearProgressIndicator.hide();
                            } catch (Exception ignored) {
                            }

                            try {
                                MainFragment.getInstance().preGetApp.setSummary(R.string.pre_main_sum_get_app);
                            } catch (Exception ignored) {
                            }
                        }
                    };
                    new Handler().postDelayed(runnable, 10);
                } else {
                    try {
                        LinearProgressIndicator linearProgressIndicator = activity.findViewById(R.id.act_progress_main);
                        linearProgressIndicator.hide();
                    } catch (Exception ignored) {
                    }

                    try {
                        MainFragment.getInstance().preGetApp.setSummary(R.string.pre_main_sum_get_app);
                    } catch (Exception ignored) {
                    }

                    new MaterialAlertDialogBuilder(activity)
                            .setCancelable(false)
                            .setMessage(context.getResources().getString(R.string.dialog_error) + "\n繰り返し発生する場合は”アプリ設定→アップデートモードを選択”が有効なモードに設定されているかをご確認ください")
                            .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> activity.finish())
                            .show();
                }
                break;
            case 3:
                try {
                    LinearProgressIndicator linearProgressIndicator = activity.findViewById(R.id.act_progress_main);
                    linearProgressIndicator.setIndeterminate(true);
                    linearProgressIndicator.show();
                } catch (Exception ignored) {
                }

                try {
                    MainFragment.getInstance().preGetApp.setSummary(R.string.progress_state_installing);
                } catch (Exception ignored) {
                }

                if (((DevicePolicyManager) activity.getSystemService(Context.DEVICE_POLICY_SERVICE)).isDeviceOwnerApp(activity.getPackageName())) {
                    if (!trySessionInstall(flag)) {
                        try {
                            LinearProgressIndicator linearProgressIndicator = activity.findViewById(R.id.act_progress_main);
                            linearProgressIndicator.hide();
                        } catch (Exception ignored) {
                        }

                        try {
                            MainFragment.getInstance().preGetApp.setSummary(R.string.pre_main_sum_get_app);
                        } catch (Exception ignored) {
                        }
                        new MaterialAlertDialogBuilder(activity)
                                .setCancelable(false)
                                .setMessage(context.getResources().getString(R.string.dialog_error) + "\n繰り返し発生する場合は”アプリ設定→アップデートモードを選択”が有効なモードに設定されているかをご確認ください")
                                .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> activity.finish())
                                .show();
                    } else {
                        try {
                            LinearProgressIndicator linearProgressIndicator = activity.findViewById(R.id.act_progress_main);
                            linearProgressIndicator.hide();
                        } catch (Exception ignored) {
                        }

                        try {
                            MainFragment.getInstance().preGetApp.setSummary(R.string.pre_main_sum_get_app);
                        } catch (Exception ignored) {
                        }
                    }
                } else {
                    if (Preferences.load(activity, Constants.KEY_MODEL_NAME, 0) == Constants.MODEL_CTX || Preferences.load(activity, Constants.KEY_MODEL_NAME, 0) == Constants.MODEL_CTZ) {
                        Preferences.save(activity, Constants.KEY_FLAG_UPDATE_MODE, 1);
                    } else Preferences.save(activity, Constants.KEY_FLAG_UPDATE_MODE, 0);
                    try {
                        LinearProgressIndicator linearProgressIndicator = activity.findViewById(R.id.act_progress_main);
                        linearProgressIndicator.hide();
                    } catch (Exception ignored) {
                    }

                    try {
                        MainFragment.getInstance().preGetApp.setSummary(R.string.pre_main_sum_get_app);
                    } catch (Exception ignored) {
                    }
                    new MaterialAlertDialogBuilder(activity)
                            .setCancelable(false)
                            .setMessage(activity.getString(R.string.dialog_error_reset_update_mode))
                            .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> activity.finish())
                            .show();
                }
                break;
            case 4:
                try {
                    LinearProgressIndicator linearProgressIndicator = activity.findViewById(R.id.act_progress_main);
                    linearProgressIndicator.setIndeterminate(true);
                    linearProgressIndicator.show();
                } catch (Exception ignored) {
                }

                try {
                    MainFragment.getInstance().preGetApp.setSummary(R.string.progress_state_installing);
                } catch (Exception ignored) {
                }

                if (isDhizukuActive(activity)) {
                    if (tryBindDhizukuService(activity)) {
                        Runnable runnable = () -> {
                            try {
                                String[] installData = new String[1];
                                installData[0] = new File(activity.getExternalCacheDir(), "update.apk").getPath();
                                int reqCode;

                                if (flag == 0) {
                                    reqCode = Constants.REQUEST_INSTALL_SELF_UPDATE;
                                } else {
                                    reqCode = Constants.REQUEST_INSTALL_GET_APP;
                                }

                                if (!mDhizukuService.tryInstallPackages(installData, reqCode)) {
                                    try {
                                        LinearProgressIndicator linearProgressIndicator = activity.findViewById(R.id.act_progress_main);
                                        linearProgressIndicator.hide();
                                    } catch (Exception ignored) {
                                    }

                                    try {
                                        MainFragment.getInstance().preGetApp.setSummary(R.string.pre_main_sum_get_app);
                                    } catch (Exception ignored) {
                                    }
                                    new MaterialAlertDialogBuilder(activity)
                                            .setCancelable(false)
                                            .setMessage(context.getResources().getString(R.string.dialog_error) + "\n繰り返し発生する場合は”アプリ設定→アップデートモードを選択”が有効なモードに設定されているかをご確認ください")
                                            .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> activity.finish())
                                            .show();
                                }
                            } catch (RemoteException ignored) {
                            }
                        };
                        new Handler().postDelayed(runnable, 5000);
                        return;
                    } else {
                        try {
                            LinearProgressIndicator linearProgressIndicator = activity.findViewById(R.id.act_progress_main);
                            linearProgressIndicator.hide();
                        } catch (Exception ignored) {
                        }

                        try {
                            MainFragment.getInstance().preGetApp.setSummary(R.string.pre_main_sum_get_app);
                        } catch (Exception ignored) {
                        }
                        new MaterialAlertDialogBuilder(activity)
                                .setCancelable(false)
                                .setMessage(context.getResources().getString(R.string.dialog_error) + "\n繰り返し発生する場合は”アプリ設定→アップデートモードを選択”が有効なモードに設定されているかをご確認ください")
                                .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> activity.finish())
                                .show();
                    }
                } else {
                    try {
                        LinearProgressIndicator linearProgressIndicator = activity.findViewById(R.id.act_progress_main);
                        linearProgressIndicator.hide();
                    } catch (Exception ignored) {
                    }

                    try {
                        MainFragment.getInstance().preGetApp.setSummary(R.string.pre_main_sum_get_app);
                    } catch (Exception ignored) {
                    }
                    new MaterialAlertDialogBuilder(activity)
                            .setCancelable(false)
                            .setMessage(context.getResources().getString(R.string.dialog_error) + "\n繰り返し発生する場合は”アプリ設定→アップデートモードを選択”が有効なモードに設定されているかをご確認ください")
                            .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> activity.finish())
                            .show();
                }
                break;
        }
    }

    ServiceConnection mDchaServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mDchaService = IDchaService.Stub.asInterface(iBinder);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
        }
    };

    public boolean tryBindDchaService() {
        return activity.bindService(Constants.DCHA_SERVICE, mDchaServiceConnection, Context.BIND_AUTO_CREATE);
    }

    private boolean tryInstallPackage() {
        if (mDchaService != null) {
            try {
                return mDchaService.installApp(new File(activity.getExternalCacheDir(), "update.apk").getPath(), 1);
            } catch (RemoteException ignored) {
            }
        }
        return false;
    }

    private boolean trySessionInstall(int reqCode) {
        SplitInstaller splitInstaller = new SplitInstaller();
        int sessionId;

        try {
            sessionId = splitInstaller.splitCreateSession(activity).i;
            if (sessionId < 0) {
                return false;
            }
        } catch (Exception ignored) {
            return false;
        }

        try {
            if (!splitInstaller.splitWriteSession(activity, new File(activity.getExternalCacheDir(), "update.apk"), sessionId).bl) {
                return false;
            }
        } catch (Exception ignored) {
            return false;
        }

        try {
            if (reqCode == 0) {
                return splitInstaller.splitCommitSession(activity, sessionId, Constants.REQUEST_INSTALL_SELF_UPDATE).bl;
            } else {
                return splitInstaller.splitCommitSession(activity, sessionId, Constants.REQUEST_INSTALL_GET_APP).bl;
            }
        } catch (Exception ignored) {
            return false;
        }
    }
}