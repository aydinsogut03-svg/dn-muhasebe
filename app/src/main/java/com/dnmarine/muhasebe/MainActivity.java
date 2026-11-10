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
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
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
    private Uri kameraUri;
    private String bekleyenYol;
    private boolean sayfaHazir;
    private View acilis;
    private long acilisZaman;

    @Override
    protected void onCreate(Bundle durum) {
        super.onCreate(durum);
        GeciciDosya.temizle(this, 0);

        FrameLayout kok = new FrameLayout(this);
        kok.setBackgroundColor(getColor(R.color.zemin));
        web = new WebView(this);
        web.setBackgroundColor(getColor(R.color.zemin));
        kok.addView(web, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        // Açılış ekranı: sayfa yüklenene kadar (en az 1,2 sn) firma logosu gösterilir, sonra yumuşakça kaybolur.
        LinearLayout a = new LinearLayout(this);
        a.setOrientation(LinearLayout.VERTICAL);
        a.setGravity(Gravity.CENTER);
        a.setBackgroundColor(0xFFFFFFFF);
        a.setClickable(true);
        float dp = getResources().getDisplayMetrics().density;
        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.acilis_logo);
        logo.setAdjustViewBounds(true);
        int en = (int) Math.min(getResources().getDisplayMetrics().widthPixels * 0.62f, 300 * dp);
        a.addView(logo, new LinearLayout.LayoutParams(en, LinearLayout.LayoutParams.WRAP_CONTENT));
        TextView alt = new TextView(this);
        alt.setText("PowerServ+");
        alt.setTextColor(0xFF5B6675);
        alt.setTextSize(15);
        alt.setLetterSpacing(0.04f);
        alt.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        ap.topMargin = (int) (18 * dp);
        a.addView(alt, ap);
        logo.setAlpha(0f); logo.setScaleX(0.92f); logo.setScaleY(0.92f);
        logo.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(450).start();
        kok.addView(a, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        acilis = a;
        acilisZaman = System.currentTimeMillis();
        setContentView(kok);
        kok.postDelayed(this::acilisiKapat, 4000);   // sayfa geç yüklenirse yine de kapanır

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
                acilisiKapat();
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
                kameraUri = null;
                // Fiş fotoğrafı: <input capture> ise doğrudan kamera açılır (Android 10+, izin gerekmez; foto Resimler/PowerServ'e kaydedilir).
                if (p.isCaptureEnabled() && Build.VERSION.SDK_INT >= 29) {
                    try {
                        ContentValues cv = new ContentValues();
                        cv.put(MediaStore.Images.Media.DISPLAY_NAME, "fis-" + System.currentTimeMillis() + ".jpg");
                        cv.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                        cv.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/PowerServ");
                        kameraUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cv);
                        Intent kamera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        kamera.putExtra(MediaStore.EXTRA_OUTPUT, kameraUri);
                        startActivityForResult(kamera, DOSYA_SEC);
                        return true;
                    } catch (Exception e) {
                        kameraUri = null;
                    }
                }
                Intent sec = new Intent(Intent.ACTION_GET_CONTENT);
                sec.addCategory(Intent.CATEGORY_OPENABLE);
                String[] turler = p.getAcceptTypes();
                sec.setType(turler != null && turler.length == 1 && turler[0].contains("/") ? turler[0] : "*/*");
                // 2.19.1: Service Report'a birden çok fotoğraf
                if (p.getMode() == FileChooserParams.MODE_OPEN_MULTIPLE) sec.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                try {
                    startActivityForResult(Intent.createChooser(sec, "Dosya seç"), DOSYA_SEC);
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
        String ad = "PowerServ+ yedek.json";
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
            if (kameraUri != null) {
                if (sonuc == RESULT_OK) dosyaGeriDonus.onReceiveValue(new Uri[]{kameraUri});
                else { try { getContentResolver().delete(kameraUri, null, null); } catch (Exception e) { } dosyaGeriDonus.onReceiveValue(null); }
                kameraUri = null;
            } else if (sonuc == RESULT_OK && veri != null && veri.getClipData() != null && veri.getClipData().getItemCount() > 0) {
                android.content.ClipData cd = veri.getClipData();
                Uri[] l = new Uri[cd.getItemCount()];
                for (int i = 0; i < l.length; i++) l[i] = cd.getItemAt(i).getUri();
                dosyaGeriDonus.onReceiveValue(l);
            } else dosyaGeriDonus.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(sonuc, veri));
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
        // 2.15: görüntüleyiciden dönülünce şifresi çözülmüş geçici evrak dosyaları silinir
        GeciciDosya.temizle(this, 30000);
        if (web != null) web.evaluateJavascript("window.onPlan && window.onPlan()", null);
    }

    // Geri tuşunu önce sayfaya sor: açık pencereyi kapatır ya da özete döner; sayfa "false" derse uygulama kapanır.
    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        web.evaluateJavascript("window.geriTusu ? window.geriTusu() : false", sonuc -> {
            if (!"true".equals(sonuc)) cikisSor();
        });
    }

    // 2.23.2: geri tuşuyla uygulamadan çıkmadan önce onay sorulur
    private void cikisSor() {
        if (isFinishing()) return;
        new android.app.AlertDialog.Builder(this)
            .setTitle("PowerServ+")
            .setMessage("Çıkmak istiyor musunuz?")
            .setPositiveButton("Evet, çık", (d, w) -> finish())
            .setNegativeButton("Hayır", null)
            .show();
    }

    private void disariAc(Uri u) {
        // Yalnızca web, e-posta, telefon ve WhatsApp bağlantıları açılır (intent:, file:, content: gibi adresler engellenir).
        String sema = u.getScheme() == null ? "" : u.getScheme().toLowerCase(java.util.Locale.ROOT);
        if (!(sema.equals("https") || sema.equals("http") || sema.equals("mailto") || sema.equals("tel") || sema.equals("whatsapp") || sema.equals("geo"))) return;
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, u));
        } catch (Exception e) {
            Toast.makeText(this, "Bu bağlantıyı açacak uygulama bulunamadı", Toast.LENGTH_SHORT).show();
        }
    }

    private void acilisiKapat() {
        final View a = acilis;
        if (a == null) return;
        acilis = null;
        long kalan = Math.max(0, 1200 - (System.currentTimeMillis() - acilisZaman));
        a.postDelayed(() -> a.animate().alpha(0f).setDuration(280).withEndAction(() -> {
            if (a.getParent() instanceof FrameLayout) ((FrameLayout) a.getParent()).removeView(a);
        }).start(), kalan);
    }

    /** Sayfadaki JavaScript'in "Android" adıyla çağırdığı yöntemler. */
    private class Kopru {
        /** Günlük otomatik yedek: İndirilenler/PowerServ+/Yedek klasörüne aynı adla (haftanın günü) üzerine yazar. Yer adını, hata olursa "" döner. */
        @JavascriptInterface
        public String gunlukYedekYaz(String ad, String icerik) {
            if (Build.VERSION.SDK_INT < 29) return "";
            return gunlukYedekDosyasi(ad, icerik) ? "İndirilenler/PowerServ+/Yedek" : "";
        }

        /** 2.20: servis açıklamaları için kısa çeviri (Google Translate). Sonuç window.ceviriGeldi(json) ile döner.
         *  Yalnızca bu sabit adres ve iki harfli dil kodları kullanılır; metin en çok 4000 karakter. */
        @JavascriptInterface
        public void ceviriIste(String metin, String kaynak, String hedef) {
            final String m = metin == null ? "" : (metin.length() > 4000 ? metin.substring(0, 4000) : metin);
            final String sl = kaynak != null && kaynak.matches("auto|[a-z]{2}") ? kaynak : "auto";
            final String tl = hedef != null && hedef.matches("[a-z]{2}") ? hedef : "en";
            new Thread(() -> {
                String yanit = "";
                java.net.HttpURLConnection c = null;
                try {
                    String adres = "https://translate.googleapis.com/translate_a/single?client=gtx&dt=t&sl=" + sl + "&tl=" + tl + "&q=" + java.net.URLEncoder.encode(m, "UTF-8");
                    c = (java.net.HttpURLConnection) new java.net.URL(adres).openConnection();
                    c.setConnectTimeout(10000); c.setReadTimeout(15000);
                    c.setRequestProperty("User-Agent", "Mozilla/5.0");
                    if (c.getResponseCode() == 200) {
                        java.io.InputStream in = c.getInputStream();
                        java.io.ByteArrayOutputStream b = new java.io.ByteArrayOutputStream();
                        byte[] t = new byte[8192]; int n;
                        while ((n = in.read(t)) > 0 && b.size() < 1_000_000) b.write(t, 0, n);
                        in.close();
                        yanit = new String(b.toByteArray(), StandardCharsets.UTF_8);
                    }
                } catch (Exception e) {
                    yanit = "";
                } finally {
                    if (c != null) c.disconnect();
                }
                final String sonuc = yanit;
                runOnUiThread(() -> { if (web != null) web.evaluateJavascript("window.ceviriGeldi && window.ceviriGeldi(" + JSONObject.quote(sonuc) + ")", null); });
            }).start();
        }

        /** TCMB döviz kurlarını (today.xml) arka planda indirir; sonuç window.kurGeldi(xml) ile sayfaya döner.
         *  WebView'deki sayfa TCMB'ye doğrudan bağlanamaz (CORS); bu yüzden uygulama kendi bağlantısını kullanır. */
        @JavascriptInterface
        public void kurIste() {
            new Thread(() -> {
                String xml = "";
                java.net.HttpURLConnection c = null;
                try {
                    c = (java.net.HttpURLConnection) new java.net.URL("https://www.tcmb.gov.tr/kurlar/today.xml").openConnection();
                    c.setConnectTimeout(10000); c.setReadTimeout(10000);
                    c.setRequestProperty("User-Agent", "PowerServ+");
                    if (c.getResponseCode() == 200) {
                        java.io.InputStream in = c.getInputStream();
                        java.io.ByteArrayOutputStream b = new java.io.ByteArrayOutputStream();
                        byte[] t = new byte[8192]; int n;
                        while ((n = in.read(t)) > 0 && b.size() < 2_000_000) b.write(t, 0, n);
                        in.close();
                        xml = new String(b.toByteArray(), StandardCharsets.ISO_8859_1);
                    }
                } catch (Exception e) {
                    xml = "";
                } finally {
                    if (c != null) c.disconnect();
                }
                final String sonuc = xml;
                runOnUiThread(() -> { if (web != null) web.evaluateJavascript("window.kurGeldi && window.kurGeldi(" + JSONObject.quote(sonuc) + ")", null); });
            }).start();
        }

        /** Düğmeye dokununca kısa dokunma titreşimi (telefonun dokunma geri bildirimi ayarına uyar). */
        @JavascriptInterface
        public void titret() {
            runOnUiThread(() -> web.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY));
        }

        /** Bağlantı kodu ve kullanıcı bilgilerini panoya kopyalar (WebView'de navigator.clipboard file:// sayfada çalışmaz). */
        @JavascriptInterface
        public void kopyala(String metin) {
            runOnUiThread(() -> {
                android.content.ClipboardManager p = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                if (p != null) p.setPrimaryClip(ClipData.newPlainText("PowerServ+", metin));
            });
        }

        /** Sayfanın teması değişince durum ve gezinme çubuğunu aynı renge boyar. */
        @JavascriptInterface
        public void tema(boolean koyu) {
            runOnUiThread(() -> {
                int renk = koyu ? 0xFF22252A : 0xFFFFFFFF;
                getWindow().setStatusBarColor(renk);
                getWindow().setNavigationBarColor(renk);
                web.setBackgroundColor(renk);
                ((android.view.View) web.getParent()).setBackgroundColor(renk);
                android.view.View d = getWindow().getDecorView();
                int bayrak = d.getSystemUiVisibility();
                int acik = android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                d.setSystemUiVisibility(koyu ? (bayrak & ~acik) : (bayrak | acik));
            });
        }

        /** Otomatik yedeğin yazılacağı dosyayı seçtirir (Drive, Dropbox ya da telefon klasörü). */
        @JavascriptInterface
        public void yedekKonumSec() {
            runOnUiThread(() -> {
                Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("application/json");
                i.putExtra(Intent.EXTRA_TITLE, "PowerServ+ yedek.json");
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

        /** Dosyayı İndirilenler/PowerServ+ klasörüne yazar ve paylaşım ekranını açar (WhatsApp, e-posta, Drive, Excel...). */
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
            return u != null ? "İndirilenler/PowerServ+" : "paylaşım ekranı";
        }

        /** Ekli belgeyi (base64) İndirilenler/PowerServ+ klasörüne yazar ve uygun uygulamayla açar (PDF görüntüleyici vb.). */
        @JavascriptInterface
        public boolean belgeAc(String ad, String base64, String tur) {
            if (Build.VERSION.SDK_INT < 29) return false;
            try {
                byte[] veri = android.util.Base64.decode(base64, android.util.Base64.DEFAULT);
                ContentValues d = new ContentValues();
                d.put(MediaStore.MediaColumns.DISPLAY_NAME, ad);
                d.put(MediaStore.MediaColumns.MIME_TYPE, tur);
                d.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/PowerServ+");
                Uri u = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, d);
                if (u == null) return false;
                try (OutputStream o = getContentResolver().openOutputStream(u)) {
                    if (o == null) return false;
                    o.write(veri);
                }
                runOnUiThread(() -> {
                    Intent ac = new Intent(Intent.ACTION_VIEW);
                    ac.setDataAndType(u, tur);
                    ac.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    try { startActivity(Intent.createChooser(ac, ad)); }
                    catch (Exception e) { Toast.makeText(MainActivity.this, "İndirilenler/PowerServ+ klasörüne kaydedildi", Toast.LENGTH_LONG).show(); }
                });
                return true;
            } catch (Exception e) {
                return false;
            }
        }

        /** 2.15: şifreli evraktan çözülen dosyayı İndirilenler'e değil, uygulamanın geçici klasörüne yazar ve görüntüleyiciyle açar. */
        @JavascriptInterface
        public boolean gizliBelgeAc(String ad, String base64, String tur) {
            try {
                byte[] veri = android.util.Base64.decode(base64, android.util.Base64.DEFAULT);
                GeciciDosya.temizle(MainActivity.this, 0);
                String klasorAd = Long.toString(System.currentTimeMillis(), 36) + Integer.toString(new java.security.SecureRandom().nextInt(1 << 30), 36);
                java.io.File k = new java.io.File(GeciciDosya.klasor(MainActivity.this), klasorAd);
                if (!k.mkdirs()) return false;
                String temiz = ad == null ? "" : ad.replaceAll("[\\\\/:*?\"<>|\\x00-\\x1f]", "_").trim();
                if (temiz.isEmpty() || temiz.startsWith(".")) temiz = "evrak.pdf";
                java.io.File f = new java.io.File(k, temiz);
                try (java.io.FileOutputStream o = new java.io.FileOutputStream(f)) { o.write(veri); }
                final Uri u = new Uri.Builder().scheme("content").authority(GeciciDosya.YETKI).appendPath(klasorAd).appendPath(temiz).build();
                final String mime = tur == null || tur.isEmpty() ? "application/pdf" : tur;
                final String baslik = temiz;
                runOnUiThread(() -> {
                    Intent ac = new Intent(Intent.ACTION_VIEW);
                    ac.setDataAndType(u, mime);
                    ac.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    try { startActivity(Intent.createChooser(ac, baslik)); }
                    catch (Exception e) { Toast.makeText(MainActivity.this, "PDF görüntüleyici bulunamadı", Toast.LENGTH_LONG).show(); }
                });
                return true;
            } catch (Exception e) {
                return false;
            }
        }

        /** Dosyayı İndirilenler/PowerServ+ klasörüne yazar; başarılıysa konumu, değilse boş metin döner. */
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
            return indirilenlereYaz(ad, icerik, tur) != null ? "İndirilenler/PowerServ+" : "";
        }
    }

    /** Android 10+ : yedek dosyasını adına göre bulur (yoksa oluşturur) ve içeriğini baştan yazar. */
    @android.annotation.TargetApi(29)
    private boolean gunlukYedekDosyasi(String ad, String icerik) {
        String klasor = Environment.DIRECTORY_DOWNLOADS + "/PowerServ+/Yedek/";
        try {
            Uri u = null;
            try (Cursor c = getContentResolver().query(MediaStore.Downloads.EXTERNAL_CONTENT_URI, new String[]{MediaStore.MediaColumns._ID},
                    MediaStore.MediaColumns.DISPLAY_NAME + "=? AND " + MediaStore.MediaColumns.RELATIVE_PATH + "=?", new String[]{ad, klasor}, null)) {
                if (c != null && c.moveToFirst()) u = android.content.ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI, c.getLong(0));
            }
            if (u == null) {
                ContentValues d = new ContentValues();
                d.put(MediaStore.MediaColumns.DISPLAY_NAME, ad);
                d.put(MediaStore.MediaColumns.MIME_TYPE, "application/json");
                d.put(MediaStore.MediaColumns.RELATIVE_PATH, klasor);
                u = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, d);
            }
            if (u == null) return false;
            try (OutputStream o = getContentResolver().openOutputStream(u, "wt")) {
                if (o == null) return false;
                o.write(icerik.getBytes(StandardCharsets.UTF_8));
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** Android 10+ : İndirilenler/PowerServ+ klasörüne yazar, dosyanın adresini döner. */
    @android.annotation.TargetApi(29)
    private Uri indirilenlereYaz(String ad, String icerik, String tur) {
        ContentValues d = new ContentValues();
        d.put(MediaStore.MediaColumns.DISPLAY_NAME, ad);
        d.put(MediaStore.MediaColumns.MIME_TYPE, tur);
        d.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/PowerServ+");
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
