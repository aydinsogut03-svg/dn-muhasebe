# DN Muhasebe

Profil tabanlı, tek dosyalık muhasebe uygulaması. Tek kod; ekranlar ve modüller seçilen profile göre açılır.

| Profil | Varsayılan modüller |
|---|---|
| Bireysel | Özet, Gelir/Harcama, Bütçe, Raporlar (Borç/Alacak isteğe bağlı) |
| Esnaf | Özet, Kasa, Veresiye, Stok, Raporlar |
| Küçük işletme | Özet, Gelir/Gider, Veresiye, Stok, Fatura, Personel, Raporlar |
| Şirket | Özet, Gelir/Gider, Cari, Stok, Fatura, Personel, KDV, Raporlar |

Her modül Ayarlar'dan profil bazında açılıp kapatılabilir. Modüller birbirine bağlıdır: kesilen fatura cariye borç yazar ve stoktan düşer, ödenince kasaya tahsilat girer; maaş ve stok hareketleri kasaya gider/gelir olarak işlenir.

Arayüz iOS tarzındadır: büyük başlıklar, gruplu listeler, alt sekme çubuğu (Özet + ilk üç modül + Menü), alttan açılan form pencereleri.

## Dosyalar

- `web/index.html`: uygulamanın tamamı (HTML + CSS + JS). Tarayıcıda doğrudan açılabilir.
- `app/`: `index.html`'i WebView içinde açan küçük Android kabuğu.
- `.github/workflows/build.yml`: her `main` gönderiminde APK derler ve **Releases > Son sürüm** sayfasına `dn-muhasebe.apk` olarak koyar.

## Veriler

Veriler cihazda (localStorage) saklanır, internete gönderilmez. Ayarlar > Yedekleme ile JSON yedeği alınıp geri yüklenebilir; Android'de yedek `İndirilenler/DN Muhasebe` klasörüne yazılır.

## Yeni modül eklemek

`web/index.html` içinde `MODULLER` listesine bir kayıt ekleyin (`id`, `varsayilan` profiller, profile göre `ad`), `hazir: true` yapın ve `ciz()` içinde ekran fonksiyonunu bağlayın.
