// Sayfaya yalnızca güvenli, dar bir köprü açar: yazdırma alanını gerçek metinli PDF olarak kaydetme ve TCMB döviz kurunu alma.
const {contextBridge, ipcRenderer} = require('electron');
contextBridge.exposeInMainWorld('PowerServ', {
  pdfKaydet: ad => ipcRenderer.invoke('pdf-kaydet', String(ad || 'belge.pdf')),
  kurGetir: () => ipcRenderer.invoke('kur-getir'),
  gunlukYedek: (ad, icerik) => ipcRenderer.invoke('gunluk-yedek', String(ad || ''), String(icerik || '')),
  yedekKlasoru: () => ipcRenderer.invoke('yedek-klasoru'),
  gizliAc: (ad, b64) => ipcRenderer.invoke('gizli-ac', String(ad || ''), String(b64 || ''))
});
