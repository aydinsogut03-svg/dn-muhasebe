// PowerServ+ Windows uygulaması: web/index.html'i kendi penceresinde açar.
// Veriler bu bilgisayarda saklanır; ortak veritabanına (Firebase) e-posta ve şifreyle girilince diğer cihazlarla eşitlenir.
const {app, BrowserWindow, shell, Menu} = require('electron');
const path = require('path');

if (!app.requestSingleInstanceLock()) app.quit();

let pencere = null;
function ac() {
  pencere = new BrowserWindow({
    width: 1366, height: 860, minWidth: 900, minHeight: 600,
    show: false, backgroundColor: '#ffffff', title: 'PowerServ+',
    icon: path.join(__dirname, 'app', 'icon.png'),
    autoHideMenuBar: true,
    webPreferences: {contextIsolation: true, nodeIntegration: false, sandbox: true, spellcheck: false}
  });
  Menu.setApplicationMenu(null);
  pencere.once('ready-to-show', () => { pencere.maximize(); pencere.show(); });
  // Dış bağlantılar (web sitesi, e-posta, harita) varsayılan tarayıcıda açılır.
  pencere.webContents.setWindowOpenHandler(({url}) => {
    if (/^(https?|mailto|tel):/i.test(url)) shell.openExternal(url);
    return {action: 'deny'};
  });
  pencere.webContents.on('will-navigate', (e, url) => {
    if (!url.startsWith('file:')) { e.preventDefault(); shell.openExternal(url); }
  });
  // İndirilen dosyalar (ekli fatura PDF'i, Excel, yedek) İndirilenler/PowerServ+ klasörüne kaydedilir; PDF varsayılan uygulamayla açılır.
  pencere.webContents.session.on('will-download', (e, oge) => {
    const klasor = path.join(app.getPath('downloads'), 'PowerServ+');
    require('fs').mkdirSync(klasor, {recursive: true});
    const yol = path.join(klasor, oge.getFilename());
    oge.setSavePath(yol);
    oge.once('done', (_, durum) => { if (durum === 'completed' && /\.pdf$/i.test(yol)) shell.openPath(yol); });
  });
  // Ctrl+R yenile, F11 tam ekran, Ctrl+P yazdır sayfanın kendisinde; F12 geliştirici araçları kapalı.
  pencere.webContents.on('before-input-event', (e, g) => {
    if (g.type !== 'keyDown') return;
    if (g.key === 'F11') { pencere.setFullScreen(!pencere.isFullScreen()); e.preventDefault(); }
    if ((g.control && g.key.toLowerCase() === 'r') || g.key === 'F5') { pencere.reload(); e.preventDefault(); }
  });
  pencere.loadFile(path.join(__dirname, 'app', 'index.html'));
}

app.on('second-instance', () => { if (pencere) { if (pencere.isMinimized()) pencere.restore(); pencere.focus(); } });
app.whenReady().then(ac);
app.on('window-all-closed', () => app.quit());
