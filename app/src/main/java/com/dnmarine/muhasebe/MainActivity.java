package com.dnmarine.muhasebe;

import android.app.Activity;
import android.content.ClipData;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Insets;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.print.PrintAttributes;
import android.print.PrintManager;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
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

import org.json.JSONObject;
import java.nio.charset.StandardCharsets;

/** web/index.html dosyasını tam ekran WebView içinde açar; yedekleme, dosya seçme ve dış bağlantılar için köprü sağlar. */
public class MainActivity extends Activity {

    private static final String ADRES = "file:///android_asset/index.html";
    private static final int DOSYA_SEC = 1;
    private static final int YEDEK_SEC = 2;
    private static final int BILDIRIM_IZNI = 3;

    private WebView web;
    private ValueCallback<Uri[]> dosyaGeriDonus;
    private String bekleyenYol;
    private boolean sayfaHazir;

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
            public void onPageFinished(WebView v, String adres) {
                sayfaHazir = true;
                bekleyenYoluAc();
            }

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

        bekleyenYol = getIntent().getStringExtra("yol");
        if (durum != null) web.restoreState(durum);
        else web.loadUrl(ADRES);
        bildirimIzniIste(false);
    }

    // Bildirime dokunulunca açılan sayfa: uygulama zaten açıksa buraya gelir.
    @Override
    protected void onNewIntent(Intent i) {
        super.onNewIntent(i);
        bekleyenYol = i.getStringExtra("yol");
        bekleyenYoluAc();
    }

    private void bekleyenYoluAc() {
        if (bekleyenYol == null || !sayfaHazir) return;
        web.evaluateJavascript("window.bildirimAc && window.bildirimAc(" + JSONObject.quote(bekleyenYol) + ")", null);
        bekleyenYol = null;
    }

    /** Android 13+ bildirim izni; ilk açılışta bir kez sorulur, ayarlardan açılınca yeniden sorulabilir. */
    private void bildirimIzniIste(boolean zorla) {
        if (Build.VERSION.SDK_INT < 33) return;
        if (checkSelfPermission("android.permission.POST_NOTIFICATIONS") == PackageManager.PERMISSION_GRANTED) return;
        SharedPreferences t = getSharedPreferences("uygulama", MODE_PRIVATE);
        if (!zorla && t.getBoolean("bildirimSoruldu", false)) return;
        t.edit().putBoolean("bildirimSoruldu", true).apply();
        requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, BILDIRIM_IZNI);
    }

    private SharedPreferences yedekTercih() {
        return getSharedPreferences("yedek", MODE_PRIVATE);
    }

    /** Seçilen yedek dosyasının adı ve bulunduğu yer (Google Drive, Dropbox, telefon). */
    private JSONObject yedekBilgisi(Uri u) {
        JSONObject o = new JSONObject();
        String ad = "DN Muhasebe yedek.json";
        try (Cursor c = getContentResolver().query(u, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (c != null && c.moveToFirst()) ad = c.getString(0);
        } catch (Exception e) {
            // ad bilinmiyor
        }
        String yetki = u.getAuthority() == null ? "" : u.getAuthority();
        String yer = yetki.contains("com.google.android.apps.docs") ? "Google Drive"
                : yetki.contains("dropbox") ? "Dropbox"
                : yetki.contains("onedrive") || yetki.contains("skydrive") ? "OneDrive"
                : "Telefon";
        try {
            o.put("ad", ad);
            o.put("yer", yer);
        } catch (Exception e) {
            // yok say
        }
        return o;
    }

    @Override
    protected void onSaveInstanceState(Bundle durum) {
        super.onSaveInstanceState(durum);
        web.saveState(durum);
    }

    @Override
    protected void onActivityResult(int istek, int sonuc, Intent veri) {
        if (istek == YEDEK_SEC) {
            if (sonuc == RESULT_OK && veri != null && veri.getData() != null) {
                Uri u = veri.getData();
                try {
                    getContentResolver().takePersistableUriPermission(u, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                } catch (Exception e) {
                    // Bazı sağlayıcılar kalıcı izin vermez; bu oturumda yine de yazılır.
                }
                yedekTercih().edit().putString("uri", u.toString()).apply();
                web.evaluateJavascript("window.yedekKonumSecildi && window.yedekKonumSecildi(" + yedekBilgisi(u) + ")", null);
            }
            return;
        }
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
        /** Otomatik yedeğin yazılacağı dosyayı seçtirir (Drive, Dropbox ya da telefon klasörü). */
        @JavascriptInterface
        public void yedekKonumSec() {
            runOnUiThread(() -> {
                Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("application/json");
                i.putExtra(Intent.EXTRA_TITLE, "DN Muhasebe yedek.json");
                try {
                    startActivityForResult(i, YEDEK_SEC);
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Dosya seçici açılamadı", Toast.LENGTH_SHORT).show();
                }
            });
        }

        /** Seçili yedek dosyası; yoksa boş metin. */
        @JavascriptInterface
        public String yedekBilgi() {
            String s = yedekTercih().getString("uri", null);
            return s == null ? "" : yedekBilgisi(Uri.parse(s)).toString();
        }

        /** Yedeği seçili dosyanın üzerine yazar; başarılıysa boş, değilse hata metni döner. */
        @JavascriptInterface
        public String yedekYaz(String icerik) {
            String s = yedekTercih().getString("uri", null);
            if (s == null) return "yedek dosyası seçilmedi";
            byte[] b = icerik.getBytes(StandardCharsets.UTF_8);
            for (String kip : new String[]{"wt", "w"}) {
                try (OutputStream o = getContentResolver().openOutputStream(Uri.parse(s), kip)) {
                    if (o == null) continue;
                    o.write(b);
                    return "";
                } catch (SecurityException e) {
                    return "dosyaya erişim izni yok, yeniden seçin";
                } catch (Exception e) {
                    // diğer kipi dene
                }
            }
            return "dosyaya yazılamadı";
        }

        @JavascriptInterface
        public void yedekKaldir() {
            String s = yedekTercih().getString("uri", null);
            if (s != null) {
                try {
                    getContentResolver().releasePersistableUriPermission(Uri.parse(s), Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                } catch (Exception e) {
                    // zaten yok
                }
            }
            yedekTercih().edit().remove("uri").apply();
        }

        /** Yaklaşan hatırlatmaları (JSON dizi) kaydeder ve günlük alarmı kurar. */
        @JavascriptInterface
        public void hatirlaticiKur(String json) {
            Hatirlatici.listeKaydet(MainActivity.this, json);
        }

        @JavascriptInterface
        public void bildirimIzni() {
            runOnUiThread(() -> bildirimIzniIste(true));
        }

        @JavascriptInterface
        public void bildirimDene(String baslik, String metin) {
            runOnUiThread(() -> bildirimIzniIste(true));
            Hatirlatici.goster(MainActivity.this, 1, baslik, metin, "");
        }

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
