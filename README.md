# DN Muhasebe

Profil tabanlı, tek dosyalık muhasebe uygulaması. Tek kod; ekranlar ve modüller seçilen profile göre açılır.

| Profil | Varsayılan modüller |
|---|---|
| Bireysel | Özet, Gelir/Harcama, Bütçe, Raporlar (Borç/Alacak isteğe bağlı) |
| Esnaf | Özet, Kasa, Veresiye, Stok, Raporlar |
| Küçük işletme | Özet, Gelir/Gider, Veresiye, Stok, Fatura, Personel, Raporlar |
| Şirket | Özet, Gelir/Gider, Cari, Stok, Fatura, Personel, Vergi, Raporlar |

Her modül Ayarlar'dan profil bazında açılıp kapatılabilir. Modüller birbirine bağlıdır: kesilen fatura cariye borç yazar ve stoktan düşer, ödenince kasaya tahsilat girer; maaş ve stok hareketleri kasaya gider/gelir olarak işlenir.

Personel modülü puantaj (takvim, rapor/izin/devamsızlık, fazla mesai), brüt ⇄ net bordro (2026 parametreleri: SGK, işsizlik, gelir vergisi dilimleri, damga, asgari ücret istisnası, işveren teşviki) ve yazdırılabilir bordro pusulası / icmal içerir. Vergi modülü dönemin KDV, muhtasar ve SGK yükünü, geçici ve yıllık gelir/kurumlar vergisi tahminini, stopaj ve vergi hesaplayıcılarını ve vergi takvimini gösterir. Bordro parametreleri `BORDRO` sabitinde yıl bazında tutulur.

Fatura modülünde satış faturası, alınan (alış) fatura ve teklif vardır; e-Fatura/e-Arşiv entegrasyonu yoktur. KDV ve gelir/kurumlar vergisi tahmini kesilen ve alınan faturalardan, faturasız kasa kayıtlarıyla birlikte hesaplanır.

Raporlar PDF olarak yazdırılabilir veya CSV olarak paylaşılabilir.

Arayüz iOS tarzındadır: büyük başlıklar, gruplu listeler, soldan açılan menü (kenardan kaydırarak da açılır; geniş ekranda sabit kenar çubuğu), alttan açılan form pencereleri.

## Dosyalar

- `web/index.html`: uygulamanın tamamı (HTML + CSS + JS). Tarayıcıda doğrudan açılabilir.
- `app/`: `index.html`'i WebView içinde açan küçük Android kabuğu.
- `.github/workflows/build.yml`: her `main` gönderiminde APK derler ve **Releases > Son sürüm** sayfasına `dn-muhasebe.apk` olarak koyar.

## Veriler

Veriler cihazda (localStorage) saklanır, internete gönderilmez. Her profil ayrı bir hesaptır: profil değiştirince o profilin hesabına geçilir, yoksa boş hesap açılır. Her hesabın verisi ayrı anahtarda tutulur (`dnmuhasebe.v1.<hesap>`), hesap listesi `dnmuhasebe.hesaplar` içindedir. Yedek yalnızca açık hesabı içerir. Ayarlar > Yedekleme ile JSON yedeği alınıp geri yüklenebilir; Android'de yedek `İndirilenler/DN Muhasebe` klasörüne yazılır.

## Yeni modül eklemek

`web/index.html` içinde `MODULLER` listesine bir kayıt ekleyin (`id`, `varsayilan` profiller, profile göre `ad`), `ciz()` içinde ekran fonksiyonunu bağlayın.
