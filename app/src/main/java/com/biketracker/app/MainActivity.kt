package com.biketracker.app

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.webkit.GeolocationPermissions
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import android.content.Intent
import android.provider.Settings
import android.location.LocationManager

class MainActivity : Activity() {

    private lateinit var webView: WebView

    private var geoCallback: GeolocationPermissions.Callback? = null
    private var geoOrigin: String? = null

    companion object {
        private const val LOCATION_REQUEST_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webView = WebView(this)
        setContentView(webView)

        configureWebView()
        webView.loadDataWithBaseURL(
            "https://appassets.androidplatform.net/",
            appHtml,
            "text/html",
            "UTF-8",
            null
        )
    }

    private fun configureWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            setGeolocationEnabled(true)
            cacheMode = WebSettings.LOAD_DEFAULT
        }

        webView.webViewClient = WebViewClient()

        webView.webChromeClient = object : WebChromeClient() {
            override fun onGeolocationPermissionsShowPrompt(
                origin: String,
                callback: GeolocationPermissions.Callback
            ) {
                val fineGranted = checkSelfPermission(
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED

                val coarseGranted = checkSelfPermission(
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED

                if (fineGranted || coarseGranted) {
                    callback.invoke(origin, true, false)
                } else {
                    geoOrigin = origin
                    geoCallback = callback

                    requestPermissions(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ),
                        LOCATION_REQUEST_CODE
                    )
                }
            }
        }
    }

    @Deprecated("Deprecated in Android API")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )

        if (requestCode == LOCATION_REQUEST_CODE) {
            val granted = grantResults.any {
                it == PackageManager.PERMISSION_GRANTED
            }

            geoCallback?.invoke(
                geoOrigin,
                granted,
                false
            )

            geoCallback = null
            geoOrigin = null

            if (!granted) {
                Toast.makeText(
                    this,
                    "Izin lokasi diperlukan untuk menggunakan GPS.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private val appHtml: String
        get() = """
<!DOCTYPE html>
<html lang="id">
<head>
<meta charset="UTF-8">
<meta name="viewport"
      content="width=device-width, initial-scale=1.0,
      maximum-scale=1.0, user-scalable=no">

<title>RideTrack Pro</title>

<link rel="stylesheet"
      href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css">

<style>
* {
    box-sizing: border-box;
}

body {
    margin: 0;
    font-family: Arial, sans-serif;
    background: #0A0F19;
    color: #FFFFFF;
}

header {
    padding: 18px;
    background: #111827;
    border-bottom: 1px solid #263244;
}

.brand {
    color: #00DCAA;
    font-size: 23px;
    font-weight: 800;
}

.subtitle {
    color: #9CA3AF;
    font-size: 12px;
    margin-top: 5px;
}

.status {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-top: 14px;
    color: #D1D5DB;
    font-size: 13px;
}

.dot {
    width: 9px;
    height: 9px;
    border-radius: 50%;
    background: #64748B;
}

.stats {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 10px;
    padding: 14px;
}

.stat {
    padding: 16px;
    border-radius: 14px;
    background: #151F2E;
    border: 1px solid #263244;
}

.value {
    font-size: 27px;
    font-weight: 800;
    color: #00DCAA;
}

.unit {
    font-size: 12px;
    color: #9CA3AF;
    margin-top: 5px;
}

.label {
    color: #D1D5DB;
    font-size: 12px;
    margin-bottom: 10px;
}

.map-title {
    padding: 4px 16px 10px;
    font-weight: 700;
}

#map {
    height: 34vh;
    min-height: 230px;
    background: #1F2937;
}

.controls {
    display: flex;
    gap: 10px;
    padding: 14px;
}

button {
    flex: 1;
    border: none;
    border-radius: 12px;
    padding: 15px 8px;
    font-size: 14px;
    font-weight: 800;
}

#start {
    background: #00DCAA;
    color: #07111B;
}

#stop {
    background: #263244;
    color: white;
}

#reset {
    background: #263244;
    color: white;
}

.note {
    padding: 0 16px 18px;
    color: #9CA3AF;
    font-size: 11px;
    line-height: 1.5;
}
</style>
</head>

<body>
<header>
    <div class="brand">RideTrack Pro</div>
    <div class="subtitle">GPS CYCLING TRACKER</div>
    <div class="status">
        <span class="dot" id="dot"></span>
        <span id="status">GPS belum diaktifkan</span>
    </div>
</header>

<section class="stats">
    <div class="stat">
        <div class="label">JARAK</div>
        <div class="value" id="distance">0.00</div>
        <div class="unit">kilometer</div>
    </div>

    <div class="stat">
        <div class="label">KECEPATAN</div>
        <div class="value" id="speed">0.0</div>
        <div class="unit">km/jam</div>
    </div>

    <div class="stat">
        <div class="label">DURASI</div>
        <div class="value" id="duration">00:00:00</div>
        <div class="unit">waktu berkendara</div>
    </div>

    <div class="stat">
        <div class="label">AKURASI GPS</div>
        <div class="value" id="accuracy">--</div>
        <div class="unit">meter</div>
    </div>
</section>

<div class="map-title">Peta perjalanan</div>
<div id="map"></div>

<div class="controls">
    <button id="start" onclick="startRide()">MULAI</button>
    <button id="stop" onclick="stopRide()">SELESAI</button>
    <button id="reset" onclick="resetRide()">RESET</button>
</div>

<div class="note" id="note">
    Aktifkan lokasi perangkat dan izinkan akses lokasi untuk mulai merekam.
    Peta memerlukan koneksi internet.
</div>

<script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>

<script>
let map;
let marker = null;
let routeLine;
let watchId = null;
let recording = false;
let totalDistance = 0;
let lastPosition = null;
let startTime = 0;
let elapsedBeforePause = 0;
let timerId = null;
let routePoints = [];

function setStatus(text, color) {
    document.getElementById('status').textContent = text;
    document.getElementById('dot').style.background = color;
}

function initMap() {
    map = L.map('map').setView([-6.2, 106.816], 12);

    L.tileLayer(
        'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',
        {
            maxZoom: 19,
            attribution: '&copy; OpenStreetMap contributors'
        }
    ).addTo(map);

    routeLine = L.polyline([], {
        color: '#00DCAA',
        weight: 5
    }).addTo(map);

    setTimeout(function() {
        map.invalidateSize();
    }, 300);
}

function distanceBetween(a, b) {
    const earthRadius = 6371000;
    const rad = Math.PI / 180;

    const dLat = (b.latitude - a.latitude) * rad;
    const dLon = (b.longitude - a.longitude) * rad;

    const lat1 = a.latitude * rad;
    const lat2 = b.latitude * rad;

    const h =
        Math.sin(dLat / 2) ** 2 +
        Math.cos(lat1) * Math.cos(lat2) *
        Math.sin(dLon / 2) ** 2;

    return 2 * earthRadius *
        Math.atan2(Math.sqrt(h), Math.sqrt(1 - h));
}

function onPosition(position) {
    const coords = position.coords;
    const point = {
        latitude: coords.latitude,
        longitude: coords.longitude
    };

    document.getElementById('accuracy').textContent =
        Math.round(coords.accuracy);

    if (coords.accuracy > 100) {
        setStatus('Sinyal GPS lemah', '#F59E0B');
    } else {
        setStatus(
            recording ? 'Merekam perjalanan' : 'GPS terhubung',
            '#00DCAA'
        );
    }

    if (!marker) {
        marker = L.marker([
            point.latitude,
            point.longitude
        ]).addTo(map).bindPopup('Posisi Anda');
    } else {
        marker.setLatLng([
            point.latitude,
            point.longitude
        ]);
    }

    if (recording && coords.accuracy <= 50) {
        if (lastPosition) {
            const segment = distanceBetween(lastPosition, point);

            // Abaikan lompatan lokasi yang tidak masuk akal.
            if (segment < 200 && segment > 1) {
                totalDistance += segment;
                document.getElementById('distance').textContent =
                    (totalDistance / 1000).toFixed(2);

                routePoints.push([
                    point.latitude,
                    point.longitude
                ]);

                routeLine.setLatLngs(routePoints);
            }
        } else {
            routePoints.push([
                point.latitude,
                point.longitude
            ]);

            routeLine.setLatLngs(routePoints);
        }

        lastPosition = point;
    }

    // Ikuti posisi GPS saat pertama kali mendapat lokasi.
    if (routePoints.length <= 1) {
        map.setView([
            point.latitude,
            point.longitude
        ], 16);
    } else if (recording) {
        map.panTo([
            point.latitude,
            point.longitude
        ]);
    }

    document.getElementById('note').textContent =
        'Koordinat diterima. Akurasi GPS: ' +
        Math.round(coords.accuracy) + ' meter.';
}

function onLocationError(error) {
    let message = 'Lokasi tidak tersedia.';

    if (error.code === 1) {
        message = 'Izin lokasi ditolak. Aktifkan izin lokasi aplikasi.';
    } else if (error.code === 2) {
        message = 'Posisi belum tersedia. Periksa GPS perangkat.';
    } else if (error.code === 3) {
        message = 'GPS terlalu lama merespons. Coba lagi di area terbuka.';
    }

    setStatus('GPS bermasalah', '#EF4444');
    document.getElementById('note').textContent = message;
}

function startRide() {
    if (!navigator.geolocation) {
        setStatus('GPS tidak didukung', '#EF4444');
        document.getElementById('note').textContent =
            'Perangkat atau WebView tidak mendukung geolocation.';
        return;
    }

    if (recording) return;

    recording = true;
    startTime = Date.now() - elapsedBeforePause;

    setStatus('Mencari sinyal GPS...', '#F59E0B');

    watchId = navigator.geolocation.watchPosition(
        onPosition,
        onLocationError,
        {
            enableHighAccuracy: true,
            maximumAge: 0,
            timeout: 20000
        }
    );

    if (timerId) clearInterval(timerId);

    timerId = setInterval(updateDuration, 1000);

    document.getElementById('start').textContent = 'MEREKAM';
}

function stopRide() {
    recording = false;

    if (watchId !== null) {
        navigator.geolocation.clearWatch(watchId);
        watchId = null;
    }

    if (timerId) {
        clearInterval(timerId);
        timerId = null;
    }

    elapsedBeforePause = startTime
        ? Date.now() - startTime
        : elapsedBeforePause;

    setStatus('Perjalanan dihentikan', '#64748B');
    document.getElementById('start').textContent = 'LANJUTKAN';
}

function resetRide() {
    stopRide();

    totalDistance = 0;
    lastPosition = null;
    startTime = 0;
    elapsedBeforePause = 0;
    routePoints = [];

    document.getElementById('distance').textContent = '0.00';
    document.getElementById('speed').textContent = '0.0';
    document.getElementById('duration').textContent = '00:00:00';
    document.getElementById('accuracy').textContent = '--';

    routeLine.setLatLngs([]);

    if (marker) {
        map.removeLayer(marker);
        marker = null;
    }

    setStatus('GPS belum diaktifkan', '#64748B');
    document.getElementById('start').textContent = 'MULAI';
    document.getElementById('note').textContent =
        'Data perjalanan di layar sudah direset.';
}

function updateDuration() {
    const elapsed = Date.now() - startTime;
    const seconds = Math.floor(elapsed / 1000);

    const hours = String(Math.floor(seconds / 3600)).padStart(2, '0');
    const minutes = String(Math.floor((seconds % 3600) / 60)).padStart(2, '0');
    const secs = String(seconds % 60).padStart(2, '0');

    document.getElementById('duration').textContent =
        hours + ':' + minutes + ':' + secs;
}

if (window.L) {
    initMap();
} else {
    setStatus('Peta tidak tersedia', '#EF4444');
    document.getElementById('note').textContent =
        'Peta gagal dimuat. Periksa koneksi internet.';
}
</script>
</body>
</html>
        """.trimIndent()

    override fun onDestroy() {
        if (::webView.isInitialized) {
            webView.stopLoading()
            webView.destroy()
        }
        super.onDestroy()
    }

    @Deprecated("Deprecated in Android API")
    override fun onBackPressed() {
        if (::webView.isInitialized && webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}