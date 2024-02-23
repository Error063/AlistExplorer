package work.error063.alist_explorer;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import java.util.regex.Pattern;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new SettingsFragment())
                    .commit();
        }
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
            EditTextPreference alistUrl = findPreference("alist_url");
            if (alistUrl != null) {
                Preference.OnPreferenceChangeListener onPreferenceChangeListener = new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
                        String value = newValue.toString();
                        boolean flag = Pattern.matches("http(s?)://[0-9a-zA-Z]([-.\\w]*[0-9a-zA-Z])*(:(0-9)*)*(/?)([a-zA-Z0-9\\-.?,'/\\\\&%+$#_=]*)?", value);
                        if (value.isEmpty()) {
                            flag = false;
                        }
                        if(!flag){
                            Toast.makeText(preference.getContext(), "URL地址错误", Toast.LENGTH_SHORT).show();
                        }
                        return flag;
                    }
                };
            }

            Preference reload_app = findPreference("reload_app");
            if (reload_app != null) {
                reload_app.setOnPreferenceClickListener(preference -> {
                    // 创建并显示确认对话框
                    AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
                    builder.setTitle("确认重启应用")
                            .setMessage("您确定要重启应用吗？")
                            .setPositiveButton("确定", (dialog, which) -> System.exit(0))
                            .setNegativeButton("取消", null)
                            .create()
                            .show();

                    return true;
                });
            }


            Preference version = findPreference("version");
            if (version != null) {
                try {
                    Context context = requireContext();
                    PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
                    String versionName = pInfo.versionName;
                    int versionCode = pInfo.versionCode;

                    // 设置版本名称和代码作为摘要信息，可以根据需要自行调整格式
                    String summary = versionName + " (" + versionCode + ")";
                    version.setSummary(summary);
                } catch (PackageManager.NameNotFoundException e) {
                    version.setSummary("无法获取应用版本信息");
                }
            }
        }
    }
}