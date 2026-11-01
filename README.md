# PowerServ+

DN Marine Makine ve Güç Sistemleri Ltd.Şti. için servis ve işletme yönetim programı: servis/iş emri ve servis formu, stok, cari, fatura, banka ve kasa, personel, vergi ve finans raporları. Tek dosyalık uygulama; tek profil (firma) vardır, bireysel/esnaf profilleri 2.1.0'da kaldırıldı ve eski hesaplar firma profiline taşınır. Açık modüller: Özet, Servis, Banka ve kasa, Gelir/Gider, Cari, Stok, Fatura, Personel, Vergi, Raporlar.

Arayüz telefon, tablet ve masaüstüne göre yerleşir; 960 px üstünde kenar menü sabit kalır ve formlar menünün sağında açılır. Ekran ölçeği otomatik ayarlanır (geniş ekranda büyür), Ayarlar > Görünüm'den sabitlenebilir. Doldurulmuş alanların adı üstünde küçük etiketle görünür.

Her modül Ayarlar'dan profil bazında açılıp kapatılabilir. Modüller birbirine bağlıdır: kesilen fatura cariye borç yazar ve stoktan düşer, ödenince kasaya tahsilat girer; maaş ve stok hareketleri kasaya gider/gelir olarak işlenir.

Personel modülü puantaj (takvim, rapor/izin/devamsızlık, fazla mesai), brüt ⇄ net bordro (2026 parametreleri: SGK, işsizlik, gelir vergisi dilimleri, damga, asgari ücret istisnası, işveren teşviki) ve yazdırılabilir bordro pusulası / icmal içerir. Vergi modülü dönemin KDV, muhtasar ve SGK yükünü, geçici ve yıllık gelir/kurumlar vergisi tahminini, stopaj ve vergi hesaplayıcılarını ve vergi takvimini gösterir. Bordro parametreleri `BORDRO` sabitinde yıl bazında tutulur.

Fatura modülünde satış faturası, alınan (alış) fatura ve teklif vardır; e-Fatura/e-Arşiv entegrasyonu yoktur. KDV ve gelir/kurumlar vergisi tahmini kesilen ve alınan faturalardan, faturasız kasa kayıtlarıyla birlikte hesaplanır.

Faturalara kısmi tahsilat/ödeme girilebilir; fatura ekranı tahsil edilen, kalan ve vade gününü gösterir. Cari ekranındaki Tahsilat, tutarı açık faturalara en eski vadeden başlayarak dağıtır; cari ekstresi PDF alınabilir. Raporlarda alacak yaşlandırma, müşteriye göre satış ve en çok satan kalemler vardır.

Raporlar PDF olarak yazdırılabilir veya CSV olarak paylaşılabilir.

Uygulamanın Android paket kimliği (`com.dnmarine.muhasebe`) ve veri anahtarları eski adla kalır; böylece güncellemede mevcut veriler korunur.

Görsel dil iOS tarzı yumuşak görünüm (2.2): açık zemin, beyaz kartlar, buzlu cam üst çubuk ve kenar menü, dolu renkli simge kareleri, belirgin rozetler, iOS eğrisiyle kayan sayfa ve pencere geçişleri. Tema Ayarlar > Görünüm'den seçilir (Açık varsayılan, Koyu, Sisteme göre); koyu temada da yüzeyler saf siyah değil, gri tondadır. Yazı tipi cihazın sistem yazısıdır (iPhone/Mac'te SF, Windows'ta Segoe UI, Android'de Roboto); Manrope yedek olarak gömülüdür. Arayüz: gruplu listeler, soldan açılan menü (kenardan kaydırarak da açılır; geniş ekranda sabit kenar çubuğu), tam ekran form sayfaları.

**Teklif formu (2.3):** Teklif PDF'i firmanın Excel şablonunun (TEKLIF_FORMU_STANDART) birebir karşılığıdır: solda firma bilgileri, ortada DN Marine logosu ve hizmet simgeleri, sağda karekod ile Tarih / Teklif No / ID, lacivert bantlı FİRMA BİLGİLERİ, 16 satırlık PARÇA NO / BİRİM / AÇIKLAMA / ADET / BİRİM FİYAT / TOPLAM tablosu, teklif şartları, toplamlar ve TEKLİF VEREN / MÜŞTERİ ONAYI alanı. Logo ve simgeler `index.html` içine gömülüdür. Servis formunda iş emri no (IE-yıl-sıra) kendiliğinden verilir, teklif ref. no müşterinin son teklifinden dolar. Geniş ekranda form alanları iki sütun yan yana dizilir.

**Alt menüler ve raporlar (2.4):** Her modülün kenar menüde açılan alt menüsü vardır (İşlemler ve Raporlar); aynı öğeler ekran üstünde şerit olarak da durur. Liste raporları: eldeki stok, kritik stok, stok hareketleri, grup bazında stok değeri; cari bakiye, borçlu/alacaklı cariler, cari hareketler, tahsilat listesi; gelir/gider listesi, kategori özeti; hesap bakiyeleri ve hareketleri; satış/alış faturaları, teklifler, vadesi geçenler; servis listesi, açık iş emirleri, tekne ve makineler, yaklaşan bakımlar, kullanılan parçalar, teknisyen işçilik; personel listesi ve ödemeleri. Raporlar aranır, tarih aralığıyla süzülür, sütuna tıklayınca sıralanır, satıra tıklayınca kaydın detayı açılır; yazdırılır (PDF) ve Excel'e (CSV) aktarılır. Geniş ekranda detay ve rapor ekranlarının düğmeleri sağda dikey küçük düğmelerdir.

**Kurumsal tasarım ve düzeltmeler (2.5):** Keskin köşeli, ince kenarlıklı, gölgesiz ticari görünüm; kenar menüde grup başlıkları yok, açık modüle tekrar tıklamak alt menüyü kapatır. Tutar ve miktar alanları Türkçe (1.250,50) ve İngilizce (1,250.50) yazımı doğru okur; binlik noktalı miktar (1.500) artık 1,5 sayılmaz. Stoğa işlenmeyen alış faturası kalemleri kâr/zarar raporunda gider sayılır. Aynı fatura/teklif numarası ikinci kez kullanılamaz.

**Fişler (2.4):** Gelir fişi (GLR-yıl-sıra), gider fişi (GDR) ve tahsilat makbuzu (THS): firma bilgisi ve logo, ödeyen/ödenen, ödeme şekli, hesap, belge, tutar ve yazıyla tutar, imza alanları. Her gelir/gider kaydı ve her tahsilat tıklanınca fiş ekranı açılır; numara ilk görüntülemede verilir ve saklanır.

## Sürüm 2.8

- Üst çubukta her ekranda **Eşitle** düğmesi (ortak veritabanına bağlıyken).
- **Personel**: tabloda satıra dokununca kart, "Düzenle" ile doğrudan form. Kimlik, iletişim, acil durum, SGK, sözleşme, banka, eğitim alanları; yıllık izin (4857 m.53 hak hesabı), zimmet, personel kartı çıktısı. Raporlar: iletişim, izin kayıtları, izin bakiyeleri, zimmet, doğum günü ve kıdem, aylık maliyet.
- **Teklif** ayrı modül: gönderilen teklifler listesi, durum (yanıt bekliyor, kabul, red, süresi doldu, faturalandı), sade okuma sayfası; resmî form "Teklif formu > Göster" veya PDF ile.
- **Kullanıcılar**: her modül için erişim seviyesi (kapalı, görüntüle, ekle ve düzenle, tam). Rol üst sınırdır; seviye uygulamada uygulanır, gizli finans verilerinin servis rolüne inmemesi sunucu kuralıyla korunur.
- **Modüller** sayfasında her modül için aç/kapat anahtarı. Ayarlar sadeleşti: firma bilgileri ve servis formu ayrı formda.
- 2.8.1: Sol menü gruplu: Satış (Teklifler, Faturalar, Satışlar), Servis (Servis fişleri, Stoklar), Muhasebe (Fatura, Banka ve kasa, Gelir/Gider, Cari, Vergi, Raporlar), Personel. Masaüstünde yana açılan kırılım (grup › modül › işlemler ve raporlar), telefonda aynı ağaç aşağı açılır. Sayfa üstündeki düğme satırları kaldırıldı.

## Servis modülü

Müşterinin tekne ve makineleri (ana makine, jeneratör, şanzıman, pervane/şaft, elektrik) cari kartında tutulur: marka, model, seri no, motor no, çalışma saati. Servis kaydı (iş emri) müşteri ve makineyi seçerek açılır; şikâyet, yapılan işlemler, kullanılan parçalar (stoktan seçilen parçalar stoktan düşer, müşteri malı düşmez), teknisyen işçilik saatleri, test ve teslim durumu, sonraki bakım girilir. Durumlar: açık, devam ediyor, parça bekleniyor, onay bekleniyor, tamamlandı, iptal.

Müşteri ve teknisyen imzası telefonda parmakla alınır. "Servis formu" düğmesi fiyatsız, yasal bilgilendirmeli A4 servis formunu PDF olarak verir (DN Marine logosu gömülüdür; Ayarlar > Servis formu'ndan başka logo yüklenebilir, garanti süresi ve yetkili mahkeme oradan girilir). "Faturala" servisten taslak fatura oluşturur: parçalar satış fiyatıyla, işçilik ayarlardaki saat ücretiyle gelir; stok servisten düştüğü için fatura stok düşmez. Sonraki bakım tarihi yaklaşan makineler servis ekranında ve özette görünür.

## Banka ve kasa

Kasa, banka hesapları, kredi kartları ve POS hesapları ayrı hesaplar olarak tanımlanır (açılış bakiyesi ve tarihi; kartta limit, kesim ve son ödeme günü). Gelir/gider, fatura tahsilatı/ödemesi, cari tahsilatı ve personel ödemesi formlarında ödeme şekli yerine hesap seçilir. Hesap seçilmemiş eski kayıtlar ödeme şekline göre varsayılan hesaba sayılır (nakit → kasa, havale → banka, kartla gelir → POS, kartla gider → kredi kartı). Hesaplar arası aktarım ve kart borcu ödemesi virman olarak girilir; gelir/gider sayılmaz. Her hesabın hareketleri yürüyen bakiyeyle listelenir ve PDF ekstresi alınır. Kart son ödeme gününe 7 gün kala özet ekranında hatırlatılır.

## Detaylı stok

Ürün kartında parça no (OEM), marka, grup, muadil parça noları, uyumlu motor, raf yeri, barkod, tedarikçi, kritik seviye ve sipariş miktarı tutulur; arama bunların hepsinde çalışır. Stok değeri fiyatlı girişlerden hesaplanan ağırlıklı ortalama maliyetle bulunur. Sayım ekranı sayılan miktarla kayıt arasındaki farkı sayım hareketi olarak yazar. Stok listesi (raf ve değerle) ve kritik seviyedeki ürünler için tedarikçili sipariş listesi PDF alınır; ürün ekranından o ürünle alış faturası açılır.

## Detaylı cari

Şirket ve küçük işletme profillerinde cari müşteri, tedarikçi ya da ikisi olarak işaretlenir; liste türe göre süzülür, alacak ve borç toplamları ayrı görünür. Cari kartında vergi dairesi, adres, ikinci telefon, e-posta, IBAN, birden fazla yetkili kişi (görev, telefon, e-posta), vade günü, risk limiti ve iskonto tutulur. Vade günü faturaların vade tarihini doldurur; risk limiti aşılınca fatura keserken uyarı verilir. Cari ekranında bu yılki satış/alış, son tahsilat, risk kullanımı, tekne ve makineler, servis geçmişi, faturalar ve hareketler birlikte görünür.

## Finans raporları

Raporlar ekranında dört sekme vardır. Gelir-gider: dönem özeti, 12 aylık grafik, kategori dağılımları, alacak yaşlandırma, müşteriye ve kaleme göre satış. Kâr/zarar: fatura esaslı, KDV hariç; satış hasılatı, satılan malın maliyeti (stok çıkışı × ortalama maliyet), brüt kâr, faturasız gelir ve giderler, net kâr ve PDF raporu. Nakit: hesap bakiyeleri, 30/60/90 günlük beklenen tahsilat ve ödemeler (açık faturalar, kart son ödemeleri, maaşlar), aylık nakit akışı, hesaplara göre giren/çıkan ve borç yaşlandırma. Servis: servis sayısı, işçilik saati, servisten faturalanan tutar, teknisyen, servis türü, makine markası ve parça kullanımı.

## Dosyalar

- `web/index.html`: uygulamanın tamamı (HTML + CSS + JS). Tarayıcıda doğrudan açılabilir.
- `app/`: `index.html`'i WebView içinde açan küçük Android kabuğu.
- `firestore.rules`: ortak veritabanının Firebase güvenlik kuralları (uygulamada Ayarlar > Ortak veritabanı > Güvenlik kuralları'ndan da kopyalanır).
- `.github/workflows/build.yml`: her `main` gönderiminde APK derler ve **Releases > Son sürüm** sayfasına `powerserv.apk` olarak koyar.

## Veriler

Veriler cihazda (localStorage) saklanır; ortak veritabanı kurulmadıkça internete gönderilmez. Her profil ayrı bir hesaptır: profil değiştirince o profilin hesabına geçilir, yoksa boş hesap açılır. Her hesabın verisi ayrı anahtarda tutulur (`dnmuhasebe.v1.<hesap>`), hesap listesi `dnmuhasebe.hesaplar` içindedir. Yedek yalnızca açık hesabı içerir. Ayarlar > Yedekleme ile JSON yedeği alınıp geri yüklenebilir; Android'de yedek `İndirilenler/PowerServ+` klasörüne yazılır.

## Ortak veritabanı (2.6, 2.7)

Masaüstü ve telefonlar aynı firmaya bağlanıp aynı veriyi kullanır. Altyapı ücretsiz başlayan bir Firebase projesidir (Authentication + Firestore); uygulama Firebase'in REST uç noktalarıyla konuşur, ek kütüphane yüklemez.

- **Çevrimdışı önce:** veri yine cihazda tutulur. Her kayıt (fatura, cari, servis, ürün, gelir/gider...) Firestore'da `firmalar/{firma}/kayit/{koleksiyon~id}` belgesidir; yalnızca değişen ve silinen kayıtlar gönderilir. Kayıttan 1,5 sn sonra, 20 sn'de bir, uygulamaya dönünce ve internet gelince eşitlenir. Gönderilmemiş yerel değişiklik, gelen sürümle ezilmez.
- **Kullanıcılar ve yetkiler:** her kullanıcı kendi e-posta ve şifresiyle girer. Roller: Yönetici (her şey ve kullanıcı yönetimi), Muhasebe (tüm modüller), Servis (servis, stok, cari, fatura/teklif), İzleyici (yalnızca görüntüler). Yetkiler hem uygulamada hem `firestore.rules` güvenlik kurallarında uygulanır.
- **İşlem kaydı:** her ekleme, değişiklik ve silme kim/ne zaman/hangi kayıt olarak `log` altına yazılır; kurallar gereği silinemez ve değiştirilemez. Ayarlar > Ortak veritabanı > İşlem kaydı (Yönetici ve Muhasebe).
- **Belge kilidi:** bir kaydın düzenleme formu açılınca kayıt 3 dakika kilitlenir (form açık kaldıkça uzar). Aynı kaydı açan diğer kullanıcı formu salt okunur görür.

### Güvenlik (2.7)

- **Gizli bölüm:** gelir/gider, banka ve kasa, virman, personel, bordro, vergi, bütçe ve finans ayarları `firmalar/{firma}/ozel/` altında durur. Servis rolü bu bölümü hiç indiremez (kural düzeyinde okuma yasağı); yalnız Yönetici ve Muhasebe yazar, İzleyici okur. Servisin gördüğü kayıtlar (servis, tekne/makine, ürün, stok, cari, fatura/teklif, firma bilgileri) `kayit/` altında kalır. 2.6'dan güncellenen ilk Yönetici/Muhasebe cihazı eski kopyaları boşaltıp gizli bölüme taşır.
- **Kurallar:** her kayıtta yalnız bilinen alanlar kabul edilir, `u` alanı oturumdaki kullanıcı ve `z` sunucu saati olmak zorundadır, kayıt boyutu 900 KB ile sınırlıdır, rol adı dört rolden biri olmalıdır, firma sahibi değiştirilemez, başkasının süren belge kilidi ezilemez. `firestore.rules` 56 emülatör testiyle doğrulanır.
- **Ekran kilidi:** varsayılan 15 dakika hareketsizlikte (5/15/30/60 dk ya da kapalı) ekran kilitlenir ve hesap şifresi sorulur. Şifre saklanmaz; cihazda yalnız tuzlanmış PBKDF2-SHA256 özeti (150.000 tur) tutulur, böylece kilit internetsiz de açılır. Kilit sayfa yenilense de sürer; 5 yanlış denemede oturum kapanır.
- **Şifre ve giriş:** yeni şifreler en az 8 karakter, harf ve rakam içermelidir; geçici şifreler kriptografik rastgele üretilir. 5 yanlış girişten sonra 60 sn (her 5 denemede ikiye katlanarak en çok 15 dk) beklenir. Her giriş, cihaz türüyle birlikte işlem kaydına yazılır.

Kurulum: Firebase konsolunda proje açılır, Authentication'da E-posta/Şifre girişi etkinleştirilir, Firestore veritabanı oluşturulur ve Kurallar sekmesine `firestore.rules` içeriği yapıştırılır. Proje ayarlarından Web uygulaması eklenip `firebaseConfig` kopyalanır. PowerServ+ > Ayarlar > Ortak veritabanı > Ortak veritabanını kur ekranına yapıştırılır; bu cihazın verisi yüklenir ve kuran kişi yönetici olur. Diğer cihazlar Kullanıcılar ekranından eklenir ve Bağlantı kodu ile "Mevcut firmaya bağlan" der (karşılama ekranında da vardır); firma verisi o cihazda yeni bir hesap olarak açılır.

## Bulut yedeği ve bildirimler

Ayarlar > Bulut yedeği'nden bir kez yedek dosyası seçilir (Android dosya seçicide Google Drive, Dropbox ya da telefon klasörü). Her değişiklikten birkaç saniye sonra ve uygulama arka plana geçince tüm hesapların yedeği bu dosyanın üzerine yazılır; Drive uygulaması dosyayı eşitler. Google hesabı ya da API anahtarı gerekmez. Yeni telefonda "Yedekten geri yükle" ile bu dosya seçilince tüm hesaplar geri gelir.

Açık faturaların vadesinden 3 gün önce ve vade günü, vergi takvimindeki beyan ve ödeme günlerinden 3 gün önce ve son gün sabah 09.00'da telefon bildirimi gelir (`Hatirlatici.java`). Bildirime dokununca ilgili fatura ya da vergi ekranı açılır.

## Yeni modül eklemek

`web/index.html` içinde `MODULLER` listesine bir kayıt ekleyin (`id`, `varsayilan` profiller, profile göre `ad`), `ciz()` içinde ekran fonksiyonunu bağlayın.

## Örnek veri ve PIN kilidi

Örnek veri özelliği 2.5'te kaldırıldı; daha önce açılmış örnek hesaplar ilk açılışta silinir (gerçek hesaplara dokunulmaz). Ayarlar > Güvenlik'ten 4 haneli PIN kilidi açılabilir: uygulama açılışta ve arka planda 30 saniyeden uzun kaldıktan sonra PIN sorar.

## Windows uygulaması

`masaustu/` klasörü programı Electron ile kurulabilir bir Windows uygulamasına çevirir (`PowerServ-Kurulum-<sürüm>.exe`). Kurulum masaüstüne ve Başlat menüsüne "PowerServ+" kısayolu ekler, simge DN logosudur. Uygulama `web/index.html`'in aynısını açar; her bilgisayarda Ayarlar > Ortak veritabanı > Mevcut firmaya bağlan ile e-posta ve şifreyle girilir, veriler Firebase üzerinden diğer bilgisayar ve telefonlarla eşitlenir. Kurulum dosyası CI'da `windows-latest` üzerinde derlenir. Dosya imzasız olduğundan Windows ilk açılışta "Windows bilgisayarınızı korudu" uyarısı verebilir: "Ek bilgi > Yine de çalıştır".
