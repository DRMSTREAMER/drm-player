const { contextBridge, ipcRenderer } = require('electron');

// Expose protected methods to renderer
contextBridge.exposeInMainWorld('electron', {
    getAppPath: () => ipcRenderer.invoke('get-app-path'),
    getVersion: () => ipcRenderer.invoke('get-version'),
    platform: process.platform
});
