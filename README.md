# DN Muhasebe

Profil tabanlı, tek dosyalık muhasebe uygulaması. Tek kod; ekranlar ve modüller seçilen profile göre açılır.

| Profil | Varsayılan modüller |
|---|---|
| Bireysel | Özet, Gelir/Harcama (Borç/Alacak isteğe bağlı) |
| Esnaf | Özet, Kasa, Veresiye |
| Küçük işletme | Özet, Gelir/Gider, Veresiye |
| Şirket | Özet, Gelir/Gider, Cari |

Sıradaki modüller (Ayarlar'da "Yakında" olarak görünür): Bütçe, Stok, Fatura, Personel, KDV, Raporlar.

## Dosyalar

- `web/index.html`: uygulamanın tamamı (HTML + CSS + JS). Tarayıcıda doğrudan açılabilir.
- `app/`: `index.html`'i WebView içinde açan küçük Android kabuğu.
- `.github/workflows/build.yml`: her `main` gönderiminde APK derler ve **Releases > Son sürüm** sayfasına `dn-muhasebe.apk` olarak koyar.

## Veriler

Veriler cihazda (localStorage) saklanır, internete gönderilmez. Ayarlar > Yedekleme ile JSON yedeği alınıp geri yüklenebilir; Android'de yedek `İndirilenler/DN Muhasebe` klasörüne yazılır.

## Yeni modül eklemek

`web/index.html` içinde `MODULLER` listesine bir kayıt ekleyin (`id`, `varsayilan` profiller, profile göre `ad`), `hazir: true` yapın ve `ciz()` içinde ekran fonksiyonunu bağlayın.
