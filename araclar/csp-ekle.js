// Derlemede (APK ve Windows) web/index.html'in kopyasına İçerik Güvenlik Politikası (CSP) ekler.
// Sayfadaki tek satır içi <script> bloğunun SHA-256 özeti izin listesine yazılır; böylece veritabanından gelen
// zararlı bir metin sayfaya HTML olarak karışsa bile (onerror=..., <script>, javascript:) çalışamaz.
// Kullanım: node araclar/csp-ekle.js web/index.html
const fs = require('fs'), crypto = require('crypto');
const dosya = process.argv[2];
if (!dosya) { console.error('Kullanım: node araclar/csp-ekle.js <index.html>'); process.exit(2); }
// Windows'ta (git autocrlf) satır sonları CRLF gelir; tarayıcı özeti LF'ye çevrilmiş metinden hesaplar.
// Özet uyuşmazsa betik engellenir ve pencere boş kalır, bu yüzden önce LF'ye çevrilir.
let h = fs.readFileSync(dosya, 'utf8').replace(/\r\n?/g, '\n');
const dur = m => { console.error('CSP eklenemedi: ' + m); process.exit(1); };
if (h.includes('http-equiv="Content-Security-Policy"')) dur('dosyada zaten CSP var');
const acik = [...h.matchAll(/<script\b([^>]*)>/gi)];
if (acik.length !== 1) dur(`tek bir <script> bekleniyordu, ${acik.length} bulundu`);
if (/\bsrc\s*=/.test(acik[0][1])) dur('dış script dosyası beklenmiyordu');
const bas = acik[0].index + acik[0][0].length, son = h.indexOf('</script>', bas);
if (son < 0) dur('</script> bulunamadı');
const kod = h.slice(bas, son);
// HTML'e yazılan satır içi olay öznitelikleri (onclick="..."), CSP altında çalışmaz: derlemeyi durdur ki sessizce bozulmasın.
const olay = (h.slice(0, bas) + kod + h.slice(son)).match(/[\s"'`]on[a-z]{3,20}\s*=\s*["'][^"']*["']/i);
if (olay) dur('satır içi olay özniteliği bulundu, .onclick = ile bağlayın: ' + olay[0].trim().slice(0, 80));
const ozet = crypto.createHash('sha256').update(kod, 'utf8').digest('base64');
const csp = [
  "default-src 'none'",
  `script-src 'sha256-${ozet}'`,
  "style-src 'unsafe-inline'",
  "img-src data: blob:",
  "font-src data:",
  "media-src data: blob:",
  "connect-src https://firestore.googleapis.com https://identitytoolkit.googleapis.com https://securetoken.googleapis.com",
  "frame-src 'none'",
  "object-src 'none'",
  "base-uri 'none'",
  "form-action 'none'"
].join('; ');
const meta = `<meta http-equiv="Content-Security-Policy" content="${csp}">`;
const charset = h.match(/<meta charset="[^"]*">/i);
if (!charset) dur('<meta charset> bulunamadı');
h = h.replace(charset[0], charset[0] + '\n' + meta);
fs.writeFileSync(dosya, h);
console.log('CSP eklendi: sha256-' + ozet);
