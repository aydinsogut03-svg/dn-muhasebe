// PowerServ+ Windows uygulaması: web/index.html'i kendi penceresinde açar.
// Veriler bu bilgisayarda saklanır; ortak veritabanına (Firebase) e-posta ve şifreyle girilince diğer cihazlarla eşitlenir.
const {app, BrowserWindow, shell, Menu, ipcMain} = require('electron');
const path = require('path');

if (!app.requestSingleInstanceLock()) app.quit();

let pencere = null;
function ac() {
  pencere = new BrowserWindow({
    width: 1366, height: 860, minWidth: 900, minHeight: 600,
    show: false, backgroundColor: '#ffffff', title: 'PowerServ+',
    icon: path.join(__dirname, 'app', 'icon.png'),
    autoHideMenuBar: true,
    webPreferences: {contextIsolation: true, nodeIntegration: false, sandbox: true, spellcheck: false, preload: path.join(__dirname, 'preload.js')}
  });
  Menu.setApplicationMenu(null);
  pencere.once('ready-to-show', () => { pencere.maximize(); pencere.show(); });
  // Dış bağlantılar (web sitesi, e-posta, harita) varsayılan tarayıcıda açılır.
  const disari = url => { if (/^(https?|mailto|tel):/i.test(url)) shell.openExternal(url); };
  pencere.webContents.setWindowOpenHandler(({url}) => {
    disari(url);
    return {action: 'deny'};
  });
  pencere.webContents.on('will-navigate', (e, url) => {
    if (!url.startsWith('file:')) { e.preventDefault(); disari(url); }
  });
  // İndirilen dosyalar (ekli fatura PDF'i, Excel, yedek) İndirilenler/PowerServ+ klasörüne kaydedilir; PDF varsayılan uygulamayla açılır.
  pencere.webContents.session.on('will-download', (e, oge) => {
    const klasor = path.join(app.getPath('downloads'), 'PowerServ+');
    require('fs').mkdirSync(klasor, {recursive: true});
    const yol = path.join(klasor, oge.getFilename());
    oge.setSavePath(yol);
    oge.once('done', (_, durum) => { if (durum === 'completed' && /\.(pdf|xlsx)$/i.test(yol)) shell.openPath(yol); });
  });
  // Ctrl+R yenile, F11 tam ekran, Ctrl+P yazdır sayfanın kendisinde; F12 geliştirici araçları kapalı.
  pencere.webContents.on('before-input-event', (e, g) => {
    if (g.type !== 'keyDown') return;
    if (g.key === 'F11') { pencere.setFullScreen(!pencere.isFullScreen()); e.preventDefault(); }
    if ((g.control && g.key.toLowerCase() === 'r') || g.key === 'F5') { pencere.reload(); e.preventDefault(); }
  });
  pencere.loadFile(path.join(__dirname, 'app', 'index.html'));
}

// Yazdırma alanındaki form gerçek metinli (seçilip kopyalanabilen) A4 PDF olarak İndirilenler/PowerServ+ klasörüne yazılır ve açılır.
ipcMain.handle('pdf-kaydet', async (e, ad) => {
  const fs = require('fs');
  const klasor = path.join(app.getPath('downloads'), 'PowerServ+');
  fs.mkdirSync(klasor, {recursive: true});
  const temiz = path.basename(ad).replace(/[<>:"/\\|?*\x00-\x1f]/g, '_').replace(/\.pdf$/i, '') || 'belge';
  let yol = path.join(klasor, temiz + '.pdf');
  for (let i = 2; fs.existsSync(yol); i++) yol = path.join(klasor, `${temiz} (${i}).pdf`);
  const veri = await e.sender.printToPDF({pageSize: 'A4', printBackground: true, preferCSSPageSize: true, margins: {top: 0.4, bottom: 0.4, left: 0.4, right: 0.4}});
  fs.writeFileSync(yol, veri);
  shell.openPath(yol);
  return yol;
});

// TCMB döviz kurları: sayfa TCMB'ye doğrudan bağlanamaz (CORS, içerik güvenlik politikası); yalnız bu sabit adres okunur.
ipcMain.handle('kur-getir', async () => {
  try {
    const {net} = require('electron');
    const c = await net.fetch('https://www.tcmb.gov.tr/kurlar/today.xml', {headers: {'User-Agent': 'PowerServ+'}});
    if (!c.ok) return '';
    return Buffer.from(await c.arrayBuffer()).toString('latin1').slice(0, 2000000);
  } catch (e) { return ''; }
});

// Günlük otomatik yedek: Belgeler\PowerServ+\Yedekler klasörüne yazılır, en yeni 30 yedek tutulur.
const yedekKlasor = () => path.join(app.getPath('documents'), 'PowerServ+', 'Yedekler');
ipcMain.handle('gunluk-yedek', async (e, ad, icerik) => {
  const fs = require('fs');
  try {
    if (!/^powerserv-yedek-\d{4}-\d{2}-\d{2}\.json$/.test(ad) || !icerik) return '';
    const k = yedekKlasor();
    fs.mkdirSync(k, {recursive: true});
    fs.writeFileSync(path.join(k, ad), icerik, 'utf8');
    const eski = fs.readdirSync(k).filter(x => /^powerserv-yedek-\d{4}-\d{2}-\d{2}\.json$/.test(x)).sort().reverse().slice(30);
    for (const x of eski) fs.unlinkSync(path.join(k, x));
    return 'Belgeler\\PowerServ+\\Yedekler';
  } catch (err) { return ''; }
});
ipcMain.handle('yedek-klasoru', async () => { const fs = require('fs'); const k = yedekKlasor(); fs.mkdirSync(k, {recursive: true}); return shell.openPath(k); });

app.on('second-instance', () => { if (pencere) { if (pencere.isMinimized()) pencere.restore(); pencere.focus(); } });
// 2.15: şifreli evrak arşivinden açılan PDF: İndirilenler'e değil geçici klasöre yazılır, varsayılan uygulamayla açılır.
// Klasör uygulama açılırken ve kapanırken silinir; böylece çözülmüş evrak bilgisayarda kalmaz.
const geciciKlasor = () => path.join(app.getPath('temp'), 'PowerServ-evrak');
const geciciTemizle = () => { try { require('fs').rmSync(geciciKlasor(), {recursive: true, force: true}); } catch (e) {} };
ipcMain.handle('gizli-ac', async (e, ad, b64) => {
  const fs = require('fs');
  try {
    const temiz = path.basename(String(ad || '')).replace(/[<>:"/\\|?*\x00-\x1f]/g, '_').replace(/^\.+/, '') || 'evrak.pdf';
    if (!/\.pdf$/i.test(temiz) || typeof b64 !== 'string' || !b64) return '';
    const k = path.join(geciciKlasor(), Date.now().toString(36) + Math.random().toString(36).slice(2, 8));
    fs.mkdirSync(k, {recursive: true});
    const yol = path.join(k, temiz);
    fs.writeFileSync(yol, Buffer.from(b64, 'base64'));
    const hata = await shell.openPath(yol);
    return hata ? '' : yol;
  } catch (x) { return ''; }
});
app.on('will-quit', geciciTemizle);
app.whenReady().then(() => { geciciTemizle(); ac(); });
app.on('window-all-closed', () => app.quit());
