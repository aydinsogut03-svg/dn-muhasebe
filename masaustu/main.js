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

app.on('second-instance', () => { if (pencere) { if (pencere.isMinimized()) pencere.restore(); pencere.focus(); } });
app.whenReady().then(ac);
app.on('window-all-closed', () => app.quit());
