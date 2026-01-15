const { app, BrowserWindow, ipcMain } = require('electron');
const path = require('path');

// Enable Widevine CDM support
app.commandLine.appendSwitch('no-sandbox');

let mainWindow;

function createWindow() {
    mainWindow = new BrowserWindow({
        width: 1280,
        height: 800,
        minWidth: 800,
        minHeight: 600,
        backgroundColor: '#0a0a0a',
        webPreferences: {
            nodeIntegration: false,
            contextIsolation: true,
            preload: path.join(__dirname, 'preload.js'),
            // Enable Widevine
            plugins: true,
            webSecurity: true
        },
        titleBarStyle: 'hiddenInset',
        frame: process.platform === 'darwin' ? false : true,
        icon: path.join(__dirname, 'assets', 'icon.png')
    });

    mainWindow.loadFile('src/index.html');

    // Open DevTools in development
    if (process.env.NODE_ENV === 'development') {
        mainWindow.webContents.openDevTools();
    }

    mainWindow.on('closed', () => {
        mainWindow = null;
    });
}

// Widevine CDM setup for different platforms
function setupWidevine() {
    // Electron includes Widevine by default in recent versions
    // For custom CDM path, use:
    // app.commandLine.appendSwitch('widevine-cdm-path', '/path/to/widevinecdm');
    // app.commandLine.appendSwitch('widevine-cdm-version', '4.10.2557.0');
}

app.whenReady().then(() => {
    setupWidevine();
    createWindow();

    app.on('activate', () => {
        if (BrowserWindow.getAllWindows().length === 0) {
            createWindow();
        }
    });
});

app.on('window-all-closed', () => {
    if (process.platform !== 'darwin') {
        app.quit();
    }
});

// IPC handlers for renderer communication
ipcMain.handle('get-app-path', () => {
    return app.getAppPath();
});

ipcMain.handle('get-version', () => {
    return app.getVersion();
});
