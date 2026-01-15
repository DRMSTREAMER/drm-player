/**
 * DRM Player - Shaka Player Integration
 * Supports Widevine and ClearKey DRM
 */

class DRMPlayer {
    constructor() {
        this.player = null;
        this.video = null;
        this.currentDrmType = 'none';

        this.init();
    }

    init() {
        // Install polyfills
        shaka.polyfill.installAll();

        // Check browser support
        if (!shaka.Player.isBrowserSupported()) {
            console.error('Browser not supported for Shaka Player');
            alert('Your browser does not support encrypted video playback');
            return;
        }

        this.video = document.getElementById('video');
        this.setupEventListeners();
        this.setupSampleStreams();
    }

    setupEventListeners() {
        // Play button
        document.getElementById('playBtn').addEventListener('click', () => this.play());

        // Back button
        document.getElementById('backBtn').addEventListener('click', () => this.goBack());

        // Fullscreen button
        document.getElementById('fullscreenBtn').addEventListener('click', () => this.toggleFullscreen());

        // DRM type change
        document.getElementById('drmType').addEventListener('change', (e) => {
            this.showDrmConfig(e.target.value);
        });

        // Load sample button
        document.getElementById('loadSampleBtn').addEventListener('click', () => {
            const samples = document.getElementById('sampleList');
            samples.style.display = samples.style.display === 'none' ? 'flex' : 'none';
        });

        // Video events
        this.video.addEventListener('loadedmetadata', () => this.updateVideoInfo());
        this.video.addEventListener('playing', () => this.hideLoading());
        this.video.addEventListener('waiting', () => this.showLoading());
    }

    setupSampleStreams() {
        document.querySelectorAll('.sample-item').forEach(item => {
            item.addEventListener('click', () => {
                const url = item.dataset.url;
                const drm = item.dataset.drm;
                const license = item.dataset.license;

                document.getElementById('mpdUrl').value = url;
                document.getElementById('drmType').value = drm;
                this.showDrmConfig(drm);

                if (drm === 'widevine' && license) {
                    document.getElementById('licenseUrl').value = license;
                }
            });
        });
    }

    showDrmConfig(type) {
        document.getElementById('widevineConfig').style.display = type === 'widevine' ? 'block' : 'none';
        document.getElementById('clearkeyConfig').style.display = type === 'clearkey' ? 'block' : 'none';
    }

    async play() {
        const mpdUrl = document.getElementById('mpdUrl').value.trim();
        if (!mpdUrl) {
            alert('Please enter a stream URL');
            return;
        }

        const drmType = document.getElementById('drmType').value;
        this.currentDrmType = drmType;

        // Show player section
        document.getElementById('inputSection').style.display = 'none';
        document.getElementById('playerSection').style.display = 'block';
        this.showLoading();
        this.updateStatus('Loading...');

        try {
            // Create player
            if (this.player) {
                await this.player.destroy();
            }

            this.player = new shaka.Player(this.video);

            // Error handling
            this.player.addEventListener('error', (e) => this.onError(e));

            // Configure DRM
            const config = this.getDrmConfig(drmType);
            this.player.configure(config);

            // Listen for adaptation events
            this.player.addEventListener('adaptation', () => this.updateVideoInfo());

            // Load the stream
            this.updateStatus('Connecting...');
            await this.player.load(mpdUrl);

            this.updateStatus('Playing');
            this.updateDrmBadge(drmType);
            this.hideLoading();

            console.log('Playback started successfully');

        } catch (error) {
            console.error('Playback error:', error);
            this.updateStatus('Error: ' + error.message);
            this.hideLoading();
        }
    }

    getDrmConfig(drmType) {
        const config = {
            streaming: {
                bufferingGoal: 30,
                rebufferingGoal: 2,
                bufferBehind: 30
            }
        };

        if (drmType === 'widevine') {
            const licenseUrl = document.getElementById('licenseUrl').value.trim();
            let headers = {};

            try {
                const headersText = document.getElementById('licenseHeaders').value.trim();
                if (headersText) {
                    headers = JSON.parse(headersText);
                }
            } catch (e) {
                console.warn('Invalid license headers JSON');
            }

            config.drm = {
                servers: {
                    'com.widevine.alpha': licenseUrl
                }
            };

            // Add license request filter for headers
            if (Object.keys(headers).length > 0) {
                this.player.getNetworkingEngine().registerRequestFilter((type, request) => {
                    if (type === shaka.net.NetworkingEngine.RequestType.LICENSE) {
                        for (const [key, value] of Object.entries(headers)) {
                            request.headers[key] = value;
                        }
                    }
                });
            }

        } else if (drmType === 'clearkey') {
            const keyId = document.getElementById('clearKeyId').value.trim();
            const keyValue = document.getElementById('clearKeyValue').value.trim();

            if (keyId && keyValue) {
                config.drm = {
                    clearKeys: {
                        [keyId]: keyValue
                    }
                };
            }
        }

        return config;
    }

    onError(event) {
        const error = event.detail;
        console.error('Shaka Player Error:', error.code, error.message);

        let errorMsg = 'Playback error';

        switch (error.code) {
            case shaka.util.Error.Code.LICENSE_REQUEST_FAILED:
                errorMsg = 'License request failed - check DRM settings';
                break;
            case shaka.util.Error.Code.EXPIRED:
                errorMsg = 'License expired';
                break;
            case shaka.util.Error.Code.LOAD_INTERRUPTED:
                errorMsg = 'Load interrupted';
                break;
            default:
                errorMsg = `Error ${error.code}: ${error.message}`;
        }

        this.updateStatus('Error: ' + errorMsg);
    }

    updateVideoInfo() {
        if (!this.player) return;

        const stats = this.player.getStats();
        const tracks = this.player.getVariantTracks();
        const activeTracks = tracks.filter(t => t.active);

        if (activeTracks.length > 0) {
            const track = activeTracks[0];
            document.getElementById('resolutionText').textContent =
                `${track.width}x${track.height}`;
            document.getElementById('bitrateText').textContent =
                `${Math.round(track.bandwidth / 1000)} kbps`;
        }

        document.getElementById('drmText').textContent =
            this.currentDrmType === 'none' ? 'Clear' : this.currentDrmType.toUpperCase();
    }

    updateStatus(status) {
        document.getElementById('statusText').textContent = status;
    }

    updateDrmBadge(type) {
        const badge = document.getElementById('drmBadge');
        badge.className = 'drm-badge';

        switch (type) {
            case 'widevine':
                badge.textContent = 'Widevine';
                badge.classList.add('widevine');
                break;
            case 'clearkey':
                badge.textContent = 'ClearKey';
                badge.classList.add('clearkey');
                break;
            default:
                badge.textContent = 'Clear';
        }
    }

    showLoading() {
        document.getElementById('playerOverlay').classList.add('visible');
    }

    hideLoading() {
        document.getElementById('playerOverlay').classList.remove('visible');
    }

    goBack() {
        if (this.player) {
            this.player.destroy();
            this.player = null;
        }

        document.getElementById('playerSection').style.display = 'none';
        document.getElementById('inputSection').style.display = 'block';
        document.getElementById('drmBadge').textContent = 'No DRM';
        document.getElementById('drmBadge').className = 'drm-badge';
    }

    toggleFullscreen() {
        if (!document.fullscreenElement) {
            document.getElementById('playerSection').requestFullscreen();
        } else {
            document.exitFullscreen();
        }
    }
}

// Initialize player when DOM is ready
document.addEventListener('DOMContentLoaded', () => {
    window.drmPlayer = new DRMPlayer();
});
