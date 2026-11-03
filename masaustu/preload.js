// Sayfaya yalnızca güvenli, dar bir köprü açar: yazdırma alanını gerçek metinli PDF olarak kaydetme.
const {contextBridge, ipcRenderer} = require('electron');
contextBridge.exposeInMainWorld('PowerServ', {
  pdfKaydet: ad => ipcRenderer.invoke('pdf-kaydet', String(ad || 'belge.pdf'))
});
