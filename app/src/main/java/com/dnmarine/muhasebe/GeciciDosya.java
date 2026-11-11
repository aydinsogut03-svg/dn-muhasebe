package com.dnmarine.muhasebe;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Şifreli evrak arşivinden açılan dosyalar (2.15): çözülen PDF İndirilenler'e yazılmaz, uygulamanın kendi önbellek
 * klasörüne (cache/evrak) yazılır ve PDF görüntüleyiciye yalnız okuma izniyle bu sağlayıcıdan verilir.
 * Klasör uygulama açılınca ve uygulamaya dönülünce temizlenir. Sağlayıcı dışarıya kapalıdır (exported=false);
 * yalnız izin verilen tek dosya, izin verilen uygulamaca okunabilir.
 */
public class GeciciDosya extends ContentProvider {
    static final String YETKI = "com.dnmarine.muhasebe.gecici";
    /** 2.25.6: e-posta ekleri bu önekli klasörde durur. Posta uygulaması eki gönderirken okuyabilsin diye bir gün tutulur. */
    static final String EPOSTA_ON = "e-";
    private static final long EPOSTA_SURE = 24L * 60 * 60 * 1000;

    static File klasor(Context c) {
        File k = new File(c.getCacheDir(), "evrak");
        if (!k.isDirectory()) k.mkdirs();
        return k;
    }

    /** Belirtilen süreden eski açılmış dosyaları siler (0: hepsi). */
    static void temizle(Context c, long yasMs) {
        File[] l = klasor(c).listFiles();
        if (l == null) return;
        long simdi = System.currentTimeMillis();
        for (File d : l) if (simdi - d.lastModified() >= (d.getName().startsWith(EPOSTA_ON) ? Math.max(yasMs, EPOSTA_SURE) : yasMs)) sil(d);
    }

    static void sil(File f) {
        File[] l = f.isDirectory() ? f.listFiles() : null;
        if (l != null) for (File x : l) sil(x);
        f.delete();
    }

    private File dosya(Uri u) throws FileNotFoundException {
        List<String> s = u.getPathSegments();
        if (s == null || s.size() != 2) throw new FileNotFoundException();
        try {
            File kok = klasor(getContext()).getCanonicalFile();
            File f = new File(new File(kok, s.get(0)), s.get(1)).getCanonicalFile();
            File ust = f.getParentFile();
            if (ust == null || !kok.equals(ust.getParentFile()) || !f.isFile()) throw new FileNotFoundException();
            return f;
        } catch (IOException e) {
            throw new FileNotFoundException();
        }
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri u, String mod) throws FileNotFoundException {
        return ParcelFileDescriptor.open(dosya(u), ParcelFileDescriptor.MODE_READ_ONLY);
    }

    @Override
    public String getType(Uri u) {
        String n = u.getLastPathSegment();
        n = n == null ? "" : n.toLowerCase(Locale.ROOT);
        if (n.endsWith(".pdf")) return "application/pdf";
        if (n.endsWith(".jpg") || n.endsWith(".jpeg")) return "image/jpeg";
        if (n.endsWith(".xlsx")) return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        return "application/octet-stream";
    }

    @Override
    public Cursor query(Uri u, String[] projeksiyon, String secim, String[] secimArg, String sira) {
        File f;
        try { f = dosya(u); } catch (FileNotFoundException e) { return null; }
        MatrixCursor c = new MatrixCursor(new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE});
        c.addRow(new Object[]{f.getName(), f.length()});
        return c;
    }

    @Override
    public Uri insert(Uri u, ContentValues v) {
        return null;
    }

    @Override
    public int update(Uri u, ContentValues v, String s, String[] a) {
        return 0;
    }

    @Override
    public int delete(Uri u, String s, String[] a) {
        return 0;
    }
}
