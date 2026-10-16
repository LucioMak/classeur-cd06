package fr.cd06kda.classeur;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import androidx.core.content.FileProvider;
import androidx.webkit.WebViewAssetLoader;
import java.io.File;
import java.io.FileOutputStream;

public class MainActivity extends Activity {
  private static final String HOST = "appassets.androidplatform.net";
  private WebView wv;

  @Override protected void onCreate(Bundle state) {
    super.onCreate(state);
    wv = new WebView(this);
    setContentView(wv);
    WebSettings s = wv.getSettings();
    s.setJavaScriptEnabled(true);
    s.setDomStorageEnabled(true);
    s.setAllowFileAccess(false);
    s.setAllowContentAccess(false);
    s.setLoadWithOverviewMode(true);
    s.setUseWideViewPort(true);
    final WebViewAssetLoader loader = new WebViewAssetLoader.Builder()
        .addPathHandler("/www/", new WebViewAssetLoader.AssetsPathHandler(this)).build();
    wv.setWebViewClient(new WebViewClient() {
      @Override public WebResourceResponse shouldInterceptRequest(WebView v, WebResourceRequest r) {
        return loader.shouldInterceptRequest(r.getUrl());
      }
      @Override public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
        Uri u = r.getUrl();
        if (HOST.equals(u.getHost())) return false;
        try { startActivity(new Intent(Intent.ACTION_VIEW, u)); } catch (ActivityNotFoundException e) { }
        return true;
      }
    });
    wv.addJavascriptInterface(new Bridge(), "AndroidBridge");
    if (state != null) wv.restoreState(state);
    else wv.loadUrl("https://" + HOST + "/www/www/index.html");
  }

  @Override protected void onSaveInstanceState(Bundle out) { super.onSaveInstanceState(out); wv.saveState(out); }

  @Override public void onBackPressed() {
    wv.evaluateJavascript("(window.__back&&window.__back())?'1':'0'", v -> {
      if (!"\"1\"".equals(v)) MainActivity.super.onBackPressed();
    });
  }

  class Bridge {
    @JavascriptInterface public void openFile(String name, String b64, String mime) {
      try {
        File dir = new File(getCacheDir(), "docs");
        dir.mkdirs();
        File f = new File(dir, name.replaceAll("[^A-Za-z0-9._-]", "_"));
        try (FileOutputStream o = new FileOutputStream(f)) { o.write(Base64.decode(b64, Base64.DEFAULT)); }
        Uri uri = FileProvider.getUriForFile(MainActivity.this, "fr.cd06kda.classeur.files", f);
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setDataAndType(uri, mime);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(Intent.createChooser(i, "Ouvrir le PDF avec"));
      } catch (Exception e) {
        runOnUiThread(() -> Toast.makeText(MainActivity.this, "Impossible d'ouvrir le fichier", Toast.LENGTH_LONG).show());
      }
    }
  }
}
