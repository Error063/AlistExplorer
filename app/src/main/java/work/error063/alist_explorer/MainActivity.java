package work.error063.alist_explorer;

import static android.view.KeyEvent.KEYCODE_BACK;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.DownloadListener;
import android.webkit.JavascriptInterface;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.Toast;
import android.Manifest;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private DownloadManager downloadManager;
    private static AppConfigs appConfigs;


    static class CustomWebChromeClient extends WebChromeClient {
        private CustomViewCallback mCustomViewCallback;
        private View mCustomView;
        private final ViewGroup mContentView;
        private final WebView mWebView;

        public CustomWebChromeClient(WebView webView, ViewGroup contentView) {
            mWebView = webView;
            mContentView = contentView;
        }

        @Override
        public void onShowCustomView(View view, CustomViewCallback callback) {
            if (mCustomView != null) {
                // 如果已经有全屏视图，则需要先隐藏它
                onHideCustomView();
                return;
            }

            mCustomView = view;
            mCustomViewCallback = callback;

            // 获取当前窗口参数并允许全屏显示
            Window window = ((Activity) mWebView.getContext()).getWindow();
            FrameLayout.LayoutParams mLayoutParams = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);

            // 将全屏视图添加到contentView中
            mContentView.addView(mCustomView, mLayoutParams);
            mContentView.setVisibility(View.VISIBLE);

            // 隐藏WebView
            mWebView.setVisibility(View.GONE);

            // 设置系统UI可见性（比如状态栏和导航栏）
            // 根据需求选择合适的方式，这里仅作为示例
            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }

        @Override
        public void onHideCustomView() {
            if (mCustomView == null) return;

            // 移除全屏视图
            mContentView.removeView(mCustomView);
            mCustomView = null;
            mCustomViewCallback.onCustomViewHidden();

            // 恢复WebView的显示
            mWebView.setVisibility(View.VISIBLE);
            mContentView.setVisibility(View.VISIBLE);

            // 取消全屏模式（恢复原窗口UI）
            Window window = ((Activity) mWebView.getContext()).getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

            // 重新设置系统UI可见性
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        appConfigs = new AppConfigs(this);

        webView = findViewById(R.id.webview);

        String useragent = appConfigs.getValue("user_agent");
        if (useragent != null) {
            if (!useragent.isEmpty()) {
                webView.getSettings().setUserAgentString(useragent);
            }
        }

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedSslError(WebView view,
                                           SslErrorHandler handler, SslError error) {
                handler.proceed();
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                String myAlistUrl = appConfigs.getValue("alist_url");
                if (myAlistUrl == null) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(intent);
                    return true;
                } else {
                    Uri uri = Uri.parse(url);
                    Uri myUri = Uri.parse(myAlistUrl);
                    if (Objects.requireNonNull(uri.getHost()).endsWith(myUri.getHost())) {
                        view.loadUrl(url);
                        return true;
                    } else {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        startActivity(intent);
                        return true;
                    }
                }
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                Toast.makeText(MainActivity.this, "打开网页失败！", Toast.LENGTH_SHORT).show();
                Utils.showSetting(MainActivity.this);
            }
        });

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.addJavascriptInterface(new WebViewHandler(webView, this), "appHandler");
        webView.setWebChromeClient(new CustomWebChromeClient(webView, findViewById(R.id.main)));
        webView.setDownloadListener(new CustomDownloadListener(this));
        downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestPermission();
        } else {
            init();
        }

    }

    private void init() {
        String alistHost = appConfigs.getValue("alist_url");
        if (alistHost != null) {
            if (alistHost.equals("http://alist.example.com") || alistHost.isEmpty() || !Utils.isUrl(alistHost)) {
                Utils.showSetting(this);
            } else {
                Utils.loadPage(alistHost, webView, this);
            }
        } else {
            Utils.showSetting(this);
        }
    }

    private void requestPermission() {
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 100);
        } else {
            init();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            init();
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (webView != null) {
            webView.saveState(outState);
        }
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (webView != null) {
            webView.restoreState(savedInstanceState);
        }
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KEYCODE_BACK) && webView.canGoBack()) {
            webView.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private class CustomDownloadListener implements DownloadListener {

        private final Context context;

        public CustomDownloadListener(Context context) {
            this.context = context;
        }

        @Override
        public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {
            Uri downloadUri = Uri.parse(url);

            // 创建DownloadManager.Request对象并设置User-Agent
            DownloadManager.Request request = new DownloadManager.Request(downloadUri);
            request.addRequestHeader("User-Agent", userAgent);  // 使用WebView的User-Agent

            // 解析Content-Disposition获取文件名
            String fileName = getFileNameFromContentDisposition(contentDisposition);

            // 设置下载目录（这里是在公共外部存储目录下的指定文件夹）
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                request.setDestinationInExternalPublicDir("AlistExplorer", fileName != null ? fileName : getFileNameFromUrl(url));
            } else {
                request.setDestinationUri(getDownloadUri(fileName));
            }

            // 显示下载进度通知
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

            // 允许在下载管理器UI中可见
            request.setVisibleInDownloadsUi(true);

            // 添加到DownloadManager队列开始下载
            downloadManager.enqueue(request);
        }

        // 在Android 10及以上版本创建下载URI
        @RequiresApi(api = Build.VERSION_CODES.Q)
        private Uri getDownloadUri(String fileName) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
            values.put(MediaStore.Downloads.IS_PENDING, 1);
            return getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
        }

        // 从URL获取文件名
        private String getFileNameFromUrl(String url) {
            String[] parts = url.split("/");
            return parts[parts.length - 1];
        }

        // 从Content-Disposition头信息获取文件名
        private String getFileNameFromContentDisposition(String contentDisposition) {
            Pattern pattern = Pattern.compile("filename\\*?=\"?(.+?)\"?$");
            Matcher matcher = pattern.matcher(contentDisposition);

            if (matcher.find()) {
                return URLDecoder.decode(matcher.group(1)); // 对编码的filename进行解码
            } else {
                return null;
            }
        }
    }


    class Utils {
        public static boolean isUrl(String urls) {
            String regex = "http(s?)://[0-9a-zA-Z]([-.\\w]*[0-9a-zA-Z])*(:(0-9)*)*(/?)([a-zA-Z0-9\\-.?,'/\\\\&%+$#_=]*)?";
            return Pattern.compile(regex).matcher(urls.trim()).matches();
        }

        public static void showSetting(AppCompatActivity activity) {
            Intent intent = new Intent(activity, SettingsActivity.class);
            activity.startActivity(intent);
        }

        public static void loadPage(String url, WebView webView, AppCompatActivity activity) {
            appConfigs.setValue("isRealAlistUrl", "false");
            webView.loadUrl(url);
            new Thread(() -> {
                try {
                    Thread.sleep(6000);
                    if (appConfigs.getValue("isRealAlistUrl").equals("false")) {
                        appConfigs.removeValue("alist_url");
                        activity.runOnUiThread(() -> {
                            Toast.makeText(activity, "当前页面似乎不是Alist页面或页面未正确加载初始化函数，请重新输入链接", Toast.LENGTH_SHORT).show();
                            showSetting(activity);
                        });
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }).start();
        }
    }

    class WebViewHandler {
        private final WebView webView;
        private final AppCompatActivity activity;
        private final AppConfigs appConfigs;

        public WebViewHandler(WebView webView, AppCompatActivity activity) {
            this.webView = webView;
            this.activity = activity;
            this.appConfigs = new AppConfigs(activity);
        }

        @JavascriptInterface
        public int reloadPage() {
            activity.runOnUiThread(webView::reload);
            return 1;
        }

        @JavascriptInterface
        public int toHomePage() {
            activity.runOnUiThread(() -> webView.loadUrl(appConfigs.getValue("alist_url")));
            return 1;
        }

        @JavascriptInterface
        public int init() {
            activity.runOnUiThread(() -> {
                webView.evaluateJavascript("console.log(\"running!\")", null);
                webView.evaluateJavascript("(()=>{$(document).ready(()=>{\n" +
                        "  let root_element = document.getElementById('root');\n" +
                        "  root_element.id = 'old-root';\n" +
                        "  let inner_root_tmp = root_element.innerHTML;\n" +
                        "  root_element.innerHTML = '';\n" +
                        "  let new_root_element = document.createElement('div');\n" +
                        "  new_root_element.id = 'root';\n" +
                        "  new_root_element.innerHTML = inner_root_tmp;\n" +
                        "  root_element.appendChild(new_root_element);\n" +
                        "  root_element.classList.add('minirefresh-wrap');\n" +
                        "  new_root_element.classList.add('minirefresh-scroll');\n" +
                        "  let miniRefresh = new MiniRefresh({\n" +
                        "    container: '#old-root',\n" +
                        "    down: {\n" +
                        "      callback: function() {\n" +
                        "        appHandler.reloadPage();\n" +
                        "        miniRefresh.endDownLoading();\n" +
                        "      }\n" +
                        "    },\n" +
                        "    up: {\n" +
                        "      callback: function() {\n" +
                        "        miniRefresh.endUpLoading(true);\n" +
                        "      }\n" +
                        "    }\n" +
                        "  });\n" +
                        "$(\".minirefresh-theme-default .minirefresh-upwrap\").css(\"display\", \"none\");$(\".minirefresh-totop\").css(\"left\", \"10px\");$(\".minirefresh-wrap\").css(\"z-index\", \"0\");})})()", null);
                webView.evaluateJavascript("(()=>{setTimeout(()=>{$(\".footer\")[0].innerHTML += `<p style=\"margin:12px;\"><a onclick=\"appHandler.showSetting()\">Alist Explorer设置</a></p>`;},500)})()", null);
            });
            appConfigs.setValue("isRealAlistUrl", "true");
            return 1;
        }

        @JavascriptInterface
        public void showSetting() {
            activity.runOnUiThread(() -> Utils.showSetting(activity));
        }
    }

    class AppConfigs {
        private final SharedPreferences sharedPreferences;
        private final SharedPreferences.Editor editor;

        public AppConfigs(Context context) {
            sharedPreferences = context.getSharedPreferences("{pkg_name}_preferences".replace("{pkg_name}", context.getPackageName()), Context.MODE_PRIVATE);
            editor = sharedPreferences.edit();
        }

        public void setValue(String key, String value) {
            editor.putString(key, value);
            editor.apply();
        }

        public String getValue(String key) {
            return sharedPreferences.getString(key, null);
        }

        public void removeValue(String key) {
            editor.remove(key);
            editor.apply();
        }
    }
}
