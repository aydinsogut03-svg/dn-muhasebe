package com.dnmarine.muhasebe;

import android.app.Activity;
import android.content.ClipData;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Insets;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.print.PrintAttributes;
import android.print.PrintManager;
import android.provider.MediaStore;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/** web/index.html dosyasını tam ekran WebView içinde açar; yedekleme, dosya seçme ve dış bağlantılar için köprü sağlar. */
public class MainActivity extends Activity {

    private static final String ADRES = "file:///android_asset/index.html";
    private static final int DOSYA_SEC = 1;

    private WebView web;
    private ValueCallback<Uri[]> dosyaGeriDonus;

    @Override
    protected void onCreate(Bundle durum) {
        super.onCreate(durum);

        FrameLayout kok = new FrameLayout(this);
        kok.setBackgroundColor(getColor(R.color.zemin));
        web = new WebView(this);
        web.setBackgroundColor(getColor(R.color.zemin));
        kok.addView(web, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        setContentView(kok);

        // Android 15+ uygulamayı kenardan kenara çizer: sistem çubukları ve klavye için boşluk bırak.
        if (Build.VERSION.SDK_INT >= 35) {
            kok.setOnApplyWindowInsetsListener((v, ic) -> {
                Insets i = ic.getInsets(android.view.WindowInsets.Type.systemBars() | android.view.WindowInsets.Type.ime());
                v.setPadding(i.left, i.top, i.right, i.bottom);
                return android.view.WindowInsets.CONSUMED;
            });
        }

        WebSettings ayar = web.getSettings();
        ayar.setJavaScriptEnabled(true);
        ayar.setDomStorageEnabled(true);
        ayar.setAllowFileAccess(true);
        ayar.setTextZoom(100);
        // Uygulama dosyaları APK içinde; güncellemeden sonra eski sayfanın önbellekten gelmemesi için önbellek kullanılmaz.
        ayar.setCacheMode(WebSettings.LOAD_NO_CACHE);
        web.clearCache(true);

        web.addJavascriptInterface(new Kopru(), "Android");
        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest istek) {
                Uri u = istek.getUrl();
                if ("file".equals(u.getScheme())) return false;
                disariAc(u);
                return true;
            }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> geriDonus, FileChooserParams p) {
                if (dosyaGeriDonus != null) dosyaGeriDonus.onReceiveValue(null);
                dosyaGeriDonus = geriDonus;
                Intent sec = new Intent(Intent.ACTION_GET_CONTENT);
                sec.addCategory(Intent.CATEGORY_OPENABLE);
                sec.setType("*/*");
                try {
                    startActivityForResult(Intent.createChooser(sec, "Yedek dosyasını seç"), DOSYA_SEC);
                    return true;
                } catch (Exception e) {
                    dosyaGeriDonus = null;
                    return false;
                }
            }
        });

        if (durum != null) web.restoreState(durum);
        else web.loadUrl(ADRES);
    }

    @Override
    protected void onSaveInstanceState(Bundle durum) {
        super.onSaveInstanceState(durum);
        web.saveState(durum);
    }

    @Override
    protected void onActivityResult(int istek, int sonuc, Intent veri) {
        if (istek == DOSYA_SEC && dosyaGeriDonus != null) {
            dosyaGeriDonus.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(sonuc, veri));
            dosyaGeriDonus = null;
            return;
        }
        super.onActivityResult(istek, sonuc, veri);
    }

    // Sayfaya arka plana gidip dönüldüğünü bildirir (PIN kilidi için).
    @Override
    protected void onPause() {
        super.onPause();
        if (web != null) web.evaluateJavascript("window.arkaPlan && window.arkaPlan()", null);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (web != null) web.evaluateJavascript("window.onPlan && window.onPlan()", null);
    }

    // Geri tuşunu önce sayfaya sor: açık pencereyi kapatır ya da özete döner; sayfa "false" derse uygulama kapanır.
    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        web.evaluateJavascript("window.geriTusu ? window.geriTusu() : false", sonuc -> {
            if (!"true".equals(sonuc)) finish();
        });
    }

    private void disariAc(Uri u) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, u));
        } catch (Exception e) {
            Toast.makeText(this, "Bu bağlantıyı açacak uygulama bulunamadı", Toast.LENGTH_SHORT).show();
        }
    }

    /** Sayfadaki JavaScript'in "Android" adıyla çağırdığı yöntemler. */
    private class Kopru {
        @JavascriptInterface
        public void ac(String adres) {
            runOnUiThread(() -> disariAc(Uri.parse(adres)));
        }

        /** Yazdırma ekranını açar; kullanıcı "PDF olarak kaydet" seçebilir. Sayfa yazdırılacak alanı önceden hazırlar. */
        @JavascriptInterface
        public void yazdir(String baslik) {
            runOnUiThread(() -> {
                PrintManager pm = (PrintManager) getSystemService(PRINT_SERVICE);
                if (pm != null) pm.print(baslik, web.createPrintDocumentAdapter(baslik), new PrintAttributes.Builder().build());
            });
        }

        /** Dosyayı İndirilenler/DN Muhasebe klasörüne yazar ve paylaşım ekranını açar (WhatsApp, e-posta, Drive, Excel...). */
        @JavascriptInterface
        public String paylas(String ad, String icerik, String tur) {
            Uri u = Build.VERSION.SDK_INT >= 29 ? indirilenlereYaz(ad, icerik, tur) : null;
            runOnUiThread(() -> {
                Intent p = new Intent(Intent.ACTION_SEND);
                p.setType(tur);
                p.putExtra(Intent.EXTRA_SUBJECT, ad);
                if (u != null) {
                    p.putExtra(Intent.EXTRA_STREAM, u);
                    p.setClipData(ClipData.newRawUri(ad, u));
                    p.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } else {
                    p.setType("text/plain");
                    p.putExtra(Intent.EXTRA_TEXT, icerik);
                }
                try {
                    startActivity(Intent.createChooser(p, ad));
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Paylaşılacak uygulama bulunamadı", Toast.LENGTH_SHORT).show();
                }
            });
            return u != null ? "İndirilenler/DN Muhasebe" : "paylaşım ekranı";
        }

        /** Dosyayı İndirilenler/DN Muhasebe klasörüne yazar; başarılıysa konumu, değilse boş metin döner. */
        @JavascriptInterface
        public String dosyaKaydet(String ad, String icerik, String tur) {
            if (Build.VERSION.SDK_INT < 29) {
                runOnUiThread(() -> {
                    Intent paylas = new Intent(Intent.ACTION_SEND);
                    paylas.setType("text/plain");
                    paylas.putExtra(Intent.EXTRA_SUBJECT, ad);
                    paylas.putExtra(Intent.EXTRA_TEXT, icerik);
                    startActivity(Intent.createChooser(paylas, ad));
                });
                return "paylaşım ekranı";
            }
            return indirilenlereYaz(ad, icerik, tur) != null ? "İndirilenler/DN Muhasebe" : "";
        }
    }

    /** Android 10+ : İndirilenler/DN Muhasebe klasörüne yazar, dosyanın adresini döner. */
    @android.annotation.TargetApi(29)
    private Uri indirilenlereYaz(String ad, String icerik, String tur) {
        ContentValues d = new ContentValues();
        d.put(MediaStore.MediaColumns.DISPLAY_NAME, ad);
        d.put(MediaStore.MediaColumns.MIME_TYPE, tur);
        d.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/DN Muhasebe");
        try {
            Uri u = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, d);
            if (u == null) return null;
            try (OutputStream o = getContentResolver().openOutputStream(u)) {
                if (o == null) return null;
                o.write(icerik.getBytes(StandardCharsets.UTF_8));
            }
            return u;
        } catch (Exception e) {
            return null;
        }
    }
}
