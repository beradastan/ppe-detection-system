import React, { useState, useRef, useEffect, useCallback } from 'react';
import jsQR from 'jsqr';

// Backend URL konfigürasyonu
const BACKEND_URL = process.env.REACT_APP_BACKEND_URL || 'http://localhost:8000';

// Türkçe çeviriler
const translations = {
  'helmet': 'Kask',
  'vest': 'Yelek',
  'boots': 'Bot',
  'glove': 'Eldiven',
  'Dust Mask': 'Toz Maskesi',
  'Eye Wear': 'Güvenlik Gözlüğü',
  'Shield': 'Koruyucu Kalkan',
  'person': 'Kişi'
};

const PPEDetectionApp = () => {
  const [isConnected, setIsConnected] = useState(false);
  const [isCameraActive, setIsCameraActive] = useState(false);
  const [detectionResult, setDetectionResult] = useState(null);
  const [, setStatusMessage] = useState('Sistem hazır');
  const [error, setError] = useState(null);
  const [availableCameras, setAvailableCameras] = useState([]);
  const [selectedCamera, setSelectedCamera] = useState('');
  const [logs, setLogs] = useState([]);
  const [logStats, setLogStats] = useState(null);
  const [showLogs, setShowLogs] = useState(false);
  const [activeLogTab, setActiveLogTab] = useState('all'); // 'all', 'success', 'denied'
  const [qrUser, setQrUser] = useState(null);
  const [qrRole, setQrRole] = useState(null);
  const [isQrVerified, setIsQrVerified] = useState(false);
  // kaldırıldı: videoSize ekran içinde kullanılmıyor
  // const [videoSize, setVideoSize] = useState({ width: 640, height: 480 });
  const [token, setToken] = useState(() => localStorage.getItem('access_token') || '');
  const [authUser, setAuthUser] = useState(() => {
    try {
      const raw = localStorage.getItem('auth_user');
      return raw ? JSON.parse(raw) : null;
    } catch (e) { return null; }
  });
  const [usernameInput, setUsernameInput] = useState('');
  const [passwordInput, setPasswordInput] = useState('');
  const [qrImageUrl, setQrImageUrl] = useState('');
  // kaldırıldı: UI'da kayıt formu bulunmuyor
  // const [showRegister, setShowRegister] = useState(false);
  // kaldırıldı: UI'da kayıt formu bulunmuyor
  // const [registerData, setRegisterData] = useState({
  //   username: '',
  //   password: '',
  //   email: '',
  //   full_name: '',
  //   role: 'worker'
  // });
  const [showCameraConfig, setShowCameraConfig] = useState(false);
  const [showUserMenu, setShowUserMenu] = useState(false);
  const [showQrModal, setShowQrModal] = useState(false);
  
  const videoRef = useRef(null);
  const canvasRef = useRef(null);
  const streamRef = useRef(null);
  const intervalRef = useRef(null);
  const qrScanRef = useRef(null);
  const userMenuRef = useRef(null);
  const passCountdownIntervalRef = useRef(null);
  const failCountdownIntervalRef = useRef(null);
  const failStartTimeoutRef = useRef(null);
  const autoLogoutScheduledRef = useRef(false);
  const [countdown, setCountdown] = useState(null);
  const [countdownType, setCountdownType] = useState(null); // 'pass' | 'fail'
  const [liveMessages, setLiveMessages] = useState([]); // Video üstü eksik ekipman mesajları
  const prevMissingRef = useRef(new Set());

  const clearAllTimers = useCallback(() => {
    if (passCountdownIntervalRef.current) {
      clearInterval(passCountdownIntervalRef.current);
      passCountdownIntervalRef.current = null;
    }
    if (failCountdownIntervalRef.current) {
      clearInterval(failCountdownIntervalRef.current);
      failCountdownIntervalRef.current = null;
    }
    if (failStartTimeoutRef.current) {
      clearTimeout(failStartTimeoutRef.current);
      failStartTimeoutRef.current = null;
    }
    setCountdown(null);
    setCountdownType(null);
    autoLogoutScheduledRef.current = false;
  }, []);

  // Backend bağlantısını kontrol et
  useEffect(() => {
    // URL query'den QR kullanıcı bilgisini al
    const params = new URLSearchParams(window.location.search);
    const u = params.get('user');
    const r = params.get('user_role');
    if (u && r) {
      setQrUser(u);
      setQrRole(r);
    }

    const checkBackendConnection = async () => {
      try {
        const response = await fetch(`${BACKEND_URL}/health`);
        if (response.ok) {
          setIsConnected(true);
          setStatusMessage('Sunucuya bağlanıldı');
          setError(null);
        } else {
          setIsConnected(false);
          setStatusMessage('Sunucu bağlantısı yok');
        }
      } catch (err) {
        console.error('Backend bağlantı hatası:', err);
        setIsConnected(false);
        setStatusMessage('Sunucu bağlantısı yok');
        setError('Backend bağlantısı kurulamadı');
      }
    };

    checkBackendConnection();
    
    // Her 5 saniyede bir bağlantıyı kontrol et
    const interval = setInterval(checkBackendConnection, 5000);
    
    return () => clearInterval(interval);
  }, []);

  // Mevcut kameraları yükle
  useEffect(() => {
    const loadCameras = async () => {
      try {
        const devices = await navigator.mediaDevices.enumerateDevices();
        const videoDevices = devices.filter(device => device.kind === 'videoinput');
        setAvailableCameras(videoDevices);
        
        if (videoDevices.length > 0) {
          setSelectedCamera(videoDevices[0].deviceId);
        }
      } catch (err) {
        console.error('Kamera listesi yüklenemedi:', err);
      }
    };

    loadCameras();
  }, []);

  // Kamera izinlerini kontrol et
  const checkCameraPermissions = useCallback(async () => {
    try {
      const result = await navigator.permissions.query({ name: 'camera' });
      console.log('Kamera izin durumu:', result.state);
      
      if (result.state === 'denied') {
        setError('Kamera izni reddedildi. Lütfen tarayıcı ayarlarından izin verin.');
      } else if (result.state === 'prompt') {
        setStatusMessage('Kamera izni istenecek...');
      }
      
      return result.state;
    } catch (err) {
      console.error('İzin kontrolü hatası:', err);
      return 'unknown';
    }
  }, []);

  // Frame yakalama ve gönderme (QR doğrulandıktan sonra)
  const startFrameCapture = useCallback((userOverride = null, roleOverride = null) => {
    if (!canvasRef.current || !videoRef.current) return;

    let isProcessing = false;
    const currentUser = userOverride || qrUser || authUser?.username;
    const currentRole = roleOverride || qrRole || authUser?.role;
    
    console.log('🎬 Frame capture başlatıldı:', { currentUser, currentRole });

    intervalRef.current = setInterval(async () => {
      if (isProcessing) return; // Önceki istek devam ediyorsa atla
      
      const canvas = canvasRef.current;
      const video = videoRef.current;
      
      if (video.readyState === video.HAVE_ENOUGH_DATA) {
        isProcessing = true;
        
        try {
          const ctx = canvas.getContext('2d', { willReadFrequently: true });
          canvas.width = video.videoWidth;
          canvas.height = video.videoHeight;
          ctx.drawImage(video, 0, 0, canvas.width, canvas.height);
          
          const dataUrl = canvas.toDataURL('image/jpeg', 0.6); // Kaliteyi düşür
          
          // Güncel kullanıcı bilgilerini al (parametre ile geçilenler öncelikli)
          const finalUser = userOverride || qrUser || authUser?.username;
          const finalRole = roleOverride || qrRole || authUser?.role;
          
          // Debug: State değerlerini kontrol et
          console.log('Debug state in frame capture:', {
            qrUser: qrUser,
            qrRole: qrRole,
            authUser: authUser,
            isQrVerified: isQrVerified,
            finalUser: finalUser,
            finalRole: finalRole,
            userOverride: userOverride,
            roleOverride: roleOverride
          });
          
          const userData = {
            frame: dataUrl,
            user: finalUser || undefined,
            user_role: finalRole || undefined
          };
          
          console.log('Frame gönderiliyor:', {
            user: userData.user,
            user_role: userData.user_role,
            frameSize: dataUrl.length,
            hasUser: !!userData.user,
            hasRole: !!userData.user_role
          });
          
          const response = await fetch(`${BACKEND_URL}/process_frame`, {
            method: 'POST',
            headers: {
              'Content-Type': 'application/json',
            },
            body: JSON.stringify(userData)
          });
          
          if (response.ok) {
            const result = await response.json();
            setDetectionResult(result);
            setStatusMessage(result.analysis.message);
            setError(null);
          } else {
            const text = await response.text().catch(() => '');
            console.error('Frame işleme hatası:', response.status, text);
            setError(`Frame işleme hatası: ${response.status}`);
          }
        } catch (err) {
          console.error('Frame gönderme hatası:', err);
          setError(`Frame gönderme hatası: ${err?.message || err}`);
        } finally {
          isProcessing = false;
        }
      }
    }, 200); // 5 FPS'e çıkar
  }, [qrUser, qrRole, authUser, isQrVerified]);

  // QR taraması (tespit başlamadan önce)
  const startQrScan = useCallback(() => {
    if (!canvasRef.current || !videoRef.current) return;
    setStatusMessage('QR bekleniyor...');
    if (qrScanRef.current) {
      clearInterval(qrScanRef.current);
      qrScanRef.current = null;
    }
    qrScanRef.current = setInterval(() => {
      const canvas = canvasRef.current;
      const video = videoRef.current;
      const ctx = canvas.getContext('2d', { willReadFrequently: true });
      canvas.width = video.videoWidth || 640;
      canvas.height = video.videoHeight || 480;
      ctx.drawImage(video, 0, 0, canvas.width, canvas.height);
      try {
        const imageData = ctx.getImageData(0, 0, canvas.width, canvas.height);
        const code = jsQR(imageData.data, canvas.width, canvas.height, { inversionAttempts: 'dontInvert' });
        if (code && code.data) {
          try {
            console.log('📱 QR kodu okundu:', code.data);
            const payload = JSON.parse(code.data);
            console.log('📋 QR payload:', payload);
            if (payload && payload.user && payload.user_role) {
              console.log('✅ QR doğrulandı:', { user: payload.user, role: payload.user_role });
              console.log('🔧 State güncelleniyor...');
              setQrUser(payload.user);
              setQrRole(payload.user_role);
              setIsQrVerified(true);
              setStatusMessage(`QR okundu: ${payload.user} (${payload.user_role}). Tespit başlatılıyor...`);
              
              // State güncellemesinden sonra hemen kontrol et
              setTimeout(() => {
                console.log('🔍 State güncelleme sonrası:', {
                  qrUser: payload.user, // Yeni değer
                  qrRole: payload.user_role, // Yeni değer
                  message: 'State güncellendi, frame capture başlatılıyor'
                });
              }, 100);
              
              if (qrScanRef.current) {
                clearInterval(qrScanRef.current);
                qrScanRef.current = null;
              }
              // QR verilerini direkt parametre olarak geç (state güncellenmesini beklemeden)
              startFrameCapture(payload.user, payload.user_role);
            } else {
              console.log('QR formatı hatalı:', payload);
            }
          } catch (e) {
            console.log('QR parse hatası:', e);
            console.log('Raw QR data:', code.data);
          }
        }
      } catch (e) {
        // canvas getImageData hatası olabilir; sessiz geç
      }
    }, 300);
  }, [startFrameCapture]);

  // Kamera başlatma
  const startCamera = useCallback(async () => {
    try {
      setStatusMessage('Kamera erişimi isteniyor...');
      
      // Önce izinleri kontrol et
      const permissionState = await checkCameraPermissions();
      if (permissionState === 'denied') {
        return;
      }
      
      // MacBook Pro M4 Pro için özel kamera ayarları
      const constraints = {
        video: {
          width: { ideal: 1280, min: 640 },
          height: { ideal: 720, min: 480 },
          facingMode: 'user', // Ön kamera
          frameRate: { ideal: 30, min: 15 },
          // Seçili kamera varsa kullan
          deviceId: selectedCamera ? { exact: selectedCamera } : undefined,
          aspectRatio: { ideal: 16/9 },
          // M4 Pro için optimize edilmiş ayarlar
          resizeMode: 'crop-and-scale'
        },
        audio: false // Ses kapalı
      };

      // Önce mevcut kameraları listele
      const devices = await navigator.mediaDevices.enumerateDevices();
      const videoDevices = devices.filter(device => device.kind === 'videoinput');
      console.log('Mevcut kameralar:', videoDevices);

      // Kamera erişimi iste
      const stream = await navigator.mediaDevices.getUserMedia(constraints);
      
      streamRef.current = stream;
      
      if (videoRef.current) {
        const video = videoRef.current;
        video.srcObject = stream;
        // Safari için hızlı aktivasyon
        try { video.play().catch(() => {}); } catch (e) {}
        setIsCameraActive(true);
        setStatusMessage('Kamera başlatıldı. QR bekleniyor...');
        
        video.onloadedmetadata = () => {
          try { video.play(); } catch (e) {}
          setIsCameraActive(true);
          startQrScan();
        };
        video.oncanplay = () => {
          // Bazı tarayıcılarda onloadedmetadata tetiklenmezse
          setIsCameraActive(true);
          if (!qrScanRef.current) startQrScan();
        };
        
        // Hata durumları için event listener'lar
        video.onerror = (error) => {
          console.error('Video yükleme hatası:', error);
          setError('Video yüklenirken hata oluştu');
        };
        
        video.onstalled = () => {
          console.warn('Video akışı durdu');
          setStatusMessage('Video akışı durdu, yeniden başlatılıyor...');
        };
      }
    } catch (err) {
      console.error('Kamera erişim hatası:', err);
      
      // Detaylı hata mesajları
      let errorMessage = 'Kamera erişim hatası';
      
      if (err.name === 'NotAllowedError') {
        errorMessage = 'Kamera izni reddedildi. Lütfen tarayıcı ayarlarından kamera iznini verin.';
      } else if (err.name === 'NotFoundError') {
        errorMessage = 'Kamera bulunamadı. Lütfen kamera bağlantısını kontrol edin.';
      } else if (err.name === 'NotReadableError') {
        errorMessage = 'Kamera başka bir uygulama tarafından kullanılıyor. Lütfen diğer uygulamaları kapatın.';
      } else if (err.name === 'OverconstrainedError') {
        errorMessage = 'Kamera ayarları desteklenmiyor. Farklı bir kamera deneyin.';
      } else if (err.name === 'TypeError') {
        errorMessage = 'Kamera erişimi desteklenmiyor. HTTPS kullanmayı deneyin.';
      } else {
        errorMessage = `Kamera hatası: ${err.message}`;
      }
      
      setError(errorMessage);
      setStatusMessage('Kamera başlatılamadı');
    }
  }, [selectedCamera, checkCameraPermissions, startQrScan]);

  // Kamera durdurma
  const stopCamera = useCallback(() => {
    if (streamRef.current) {
      streamRef.current.getTracks().forEach(track => track.stop());
      streamRef.current = null;
    }
    
    if (intervalRef.current) {
      clearInterval(intervalRef.current);
      intervalRef.current = null;
    }
    if (qrScanRef.current) {
      clearInterval(qrScanRef.current);
      qrScanRef.current = null;
    }
    clearAllTimers();
    
    setIsCameraActive(false);
    setStatusMessage('Kamera durduruldu');
    setDetectionResult(null);
    setIsQrVerified(false);
  }, []);

  // QR çıkışı (yalnızca QR oturumu)
  const handleQrLogout = useCallback(() => {
    clearAllTimers();
    // PPE gönderimini durdur
    if (intervalRef.current) {
      clearInterval(intervalRef.current);
      intervalRef.current = null;
    }
    // QR taramasını sıfırla
    if (qrScanRef.current) {
      clearInterval(qrScanRef.current);
      qrScanRef.current = null;
    }
    setQrUser(null);
    setQrRole(null);
    setIsQrVerified(false);
    setDetectionResult(null);
    setStatusMessage('Çıkış yapıldı. QR bekleniyor...');
    if (isCameraActive) {
      // Kamera açıksa tekrar QR taramaya başla
      startQrScan();
    }
  }, [isCameraActive, startQrScan, clearAllTimers]);

  // Auth çıkışı
  const handleLogout = useCallback(() => {
    clearAllTimers();
    // Kamerayı da kapat
    try { stopCamera(); } catch (e) {}
    setToken('');
    setAuthUser(null);
    localStorage.removeItem('access_token');
    localStorage.removeItem('auth_user');
    setQrImageUrl('');
    // QR oturumu da sıfırlansın
    if (intervalRef.current) {
      clearInterval(intervalRef.current);
      intervalRef.current = null;
    }
    setDetectionResult(null);
    setIsQrVerified(false);
    setQrUser(null);
    setQrRole(null);
    if (isCameraActive) {
      startQrScan();
      setStatusMessage('Çıkış yapıldı. QR bekleniyor...');
    }
  }, [isCameraActive, startQrScan, stopCamera]);

  // QR doğrulandıktan sonra: 10 sn içinde geçiş onayı gelmezse 5 sn geri sayım başlat ve otomatik çıkış yap
  useEffect(() => {
    if (!isQrVerified) {
      clearAllTimers();
      return;
    }
    // Yeni QR oturumu: tüm timer'ları sıfırla ve 10 sn bekleme başlat
    clearAllTimers();
    failStartTimeoutRef.current = setTimeout(() => {
      if (autoLogoutScheduledRef.current) return;
      setCountdownType('fail');
      setCountdown(5);
      failCountdownIntervalRef.current = setInterval(() => {
        setCountdown((prev) => {
          if (prev === null) return null;
          if (prev <= 1) {
            clearInterval(failCountdownIntervalRef.current);
            failCountdownIntervalRef.current = null;
            setCountdown(null);
            setCountdownType(null);
            handleQrLogout();
            return null;
          }
          return prev - 1;
        });
      }, 1000);
      autoLogoutScheduledRef.current = true;
    }, 10000);
  }, [isQrVerified, clearAllTimers, handleQrLogout]);

  // Geçiş onayı alınır alınmaz: 3 sn geri sayım ve otomatik çıkış
  useEffect(() => {
    if (!isQrVerified || !detectionResult?.analysis?.can_pass || autoLogoutScheduledRef.current) {
      return;
    }
    
    // Başarılı geçiş: fail timer'ını durdur ve pass timer'ını başlat
    if (failStartTimeoutRef.current) {
      clearTimeout(failStartTimeoutRef.current);
      failStartTimeoutRef.current = null;
    }
    if (failCountdownIntervalRef.current) {
      clearInterval(failCountdownIntervalRef.current);
      failCountdownIntervalRef.current = null;
    }
    
    setCountdownType('pass');
    setCountdown(3);
    autoLogoutScheduledRef.current = true;
    passCountdownIntervalRef.current = setInterval(() => {
      setCountdown((prev) => {
        if (prev === null) return null;
        if (prev <= 1) {
          clearInterval(passCountdownIntervalRef.current);
          passCountdownIntervalRef.current = null;
          setCountdown(null);
          setCountdownType(null);
          handleQrLogout();
          return null;
        }
        return prev - 1;
      });
    }, 1000);
  }, [detectionResult?.analysis?.can_pass, isQrVerified]);

  // Bileşen unmount olduğunda tüm timer'ları temizle
  useEffect(() => {
    return () => {
      clearAllTimers();
    };
  }, []);

  // QR görselini getir
  const fetchQrImage = useCallback(async () => {
    if (!token) {
      setError('Önce giriş yapın');
      return;
    }
    try {
      const resp = await fetch(`${BACKEND_URL}/user/qr`, {
        headers: { 'Authorization': `Bearer ${token}` }
      });
      if (!resp.ok) throw new Error('QR alınamadı');
      const blob = await resp.blob();
      const url = URL.createObjectURL(blob);
      setQrImageUrl(url);
    } catch (err) {
      console.error('QR getirme hatası:', err);
      setError(err.message || 'QR getirme hatası');
    }
  }, [token]);

  // Eksik ekipman mesajı ekle (video üzerinde kayan bildirim)
  const addLiveMessage = useCallback((text) => {
    const id = `${Date.now()}_${Math.random().toString(36).slice(2, 7)}`;
    const newMsg = { id, text, visible: false };
    setLiveMessages(prev => [...prev, newMsg].slice(-8));
    // Görünür hale getir (fade-in + translate)
    setTimeout(() => {
      setLiveMessages(prev => prev.map(m => m.id === id ? { ...m, visible: true } : m));
    }, 50);
    // Kaybolma animasyonu
    setTimeout(() => {
      setLiveMessages(prev => prev.map(m => m.id === id ? { ...m, visible: false } : m));
    }, 2000);
    // Listeden çıkar
    setTimeout(() => {
      setLiveMessages(prev => prev.filter(m => m.id !== id));
    }, 2300);
  }, []);

  // Tespit sonucuna göre eksik ekipmanlar için canlı mesaj üret
  useEffect(() => {
    const missingReq = detectionResult?.analysis?.missing_required || [];
    const missingOpt = detectionResult?.analysis?.missing_optional || [];
    const currentMissing = new Set([...(Array.isArray(missingReq) ? missingReq : []), ...(Array.isArray(missingOpt) ? missingOpt : [])]);
    // İlk kez veya değişim olduğunda yeni eksikler için mesaj üret
    const prev = prevMissingRef.current;
    for (const item of currentMissing) {
      if (!prev.has(item)) {
        const label = translations[item] || item;
        addLiveMessage(`Eksik: ${label}`);
      }
    }
    prevMissingRef.current = currentMissing;
  }, [detectionResult, addLiveMessage]);

  // Header kullanıcı menüsü: dışarı tıklayınca kapanması
  useEffect(() => {
    function handleClickOutside(e) {
      if (userMenuRef.current && !userMenuRef.current.contains(e.target)) {
        setShowUserMenu(false);
      }
    }
    if (showUserMenu) {
      document.addEventListener('mousedown', handleClickOutside);
    }
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, [showUserMenu]);

  // Rastgele JWT (Ahmet) al ve oturum aç
  // kaldırıldı: UI’da tetikleyen buton yok
  // const getRandomJwt = useCallback(async () => { ... }, [qrUser, qrRole]);

  // Logları getir
  const fetchLogs = useCallback(async (tab) => {
    try {
      const response = await fetch(`${BACKEND_URL}/logs?limit=50`, {
        headers: token ? { 'Authorization': `Bearer ${token}` } : {}
      });
      if (response.ok) {
        const data = await response.json();
        let incoming = data.logs || [];
        // Aktif sekmeye göre filtreyi burada da uygula (tab parametresi varsa onu kullan)
        const key = tab || activeLogTab;
        if (key === 'passed') incoming = incoming.filter(l => !!l.can_pass);
        else if (key === 'denied') incoming = incoming.filter(l => !l.can_pass);
        setLogs(incoming);
      } else {
        const text = await response.text().catch(() => '');
        console.error('Logs fetch failed:', response.status, text);
        setError(`Loglar alınamadı (${response.status})`);
      }
    } catch (err) {
      console.error('Log getirme hatası:', err);
      setError(err?.message || 'Log getirme hatası');
    }
  }, [activeLogTab, token]);

  // Log istatistiklerini getir
  const fetchLogStats = useCallback(async () => {
    try {
      const response = await fetch(`${BACKEND_URL}/logs/stats`, {
        headers: token ? { 'Authorization': `Bearer ${token}` } : {}
      });
      if (response.ok) {
        const data = await response.json();
        setLogStats(data);
      } else {
        const text = await response.text().catch(() => '');
        console.error('Stats fetch failed:', response.status, text);
        setError(`İstatistik alınamadı (${response.status})`);
      }
    } catch (err) {
      console.error('İstatistik getirme hatası:', err);
      setError(err?.message || 'İstatistik getirme hatası');
    }
  }, [token]);

  // Log panelini aç/kapat
  const toggleLogs = useCallback(() => {
    if (!showLogs) {
      if (!token) {
        setError('Önce giriş yapın');
        return;
      }
      setActiveLogTab('all');
      fetchLogs('all');
      fetchLogStats();
      setShowLogs(true);
      return;
    }
    setShowLogs(false);
  }, [showLogs, fetchLogs, fetchLogStats, token]);

  // Log paneli açıkken periyodik güncelleme
  useEffect(() => {
    if (!showLogs) return;
    const interval = setInterval(() => {
      fetchLogs();
      fetchLogStats();
    }, 3000);
    return () => clearInterval(interval);
  }, [showLogs, fetchLogs, fetchLogStats]);

  // Token değiştiğinde log paneli açıksa anında yenile
  useEffect(() => {
    if (showLogs && token) {
      fetchLogs('all');
      fetchLogStats();
    }
  }, [token, showLogs, fetchLogs, fetchLogStats]);

  // Kimlik doğrulama: giriş
  const handleLogin = useCallback(async () => {
    try {
      const resp = await fetch(`${BACKEND_URL}/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username: usernameInput, password: passwordInput })
      });
      if (!resp.ok) {
        const e = await resp.json().catch(() => ({}));
        throw new Error(e.detail || 'Giriş başarısız');
      }
      const data = await resp.json();
      if (!data?.user) {
        setError('Geçersiz kullanıcı bilgisi');
        return;
      }
      setToken(data.access_token);
      localStorage.setItem('access_token', data.access_token);
      setAuthUser(data.user);
      localStorage.setItem('auth_user', JSON.stringify(data.user));
      if (!qrUser && !qrRole) {
        setQrUser(data.user.username);
        setQrRole(data.user.role);
      }
      setStatusMessage('Giriş başarılı');
      setError(null);
    } catch (err) {
      console.error('Giriş hatası:', err);
      setError(err.message || 'Giriş hatası');
    }
  }, [usernameInput, passwordInput, qrUser, qrRole]);

  // Kayıt işlemi
  // kaldırıldı: UI’da kayıt formu bulunmuyor
  // const handleRegister = useCallback(async () => { ... }, [registerData, qrUser, qrRole]);

  // Filtrelenmiş logları getir
  const getFilteredLogs = useCallback(() => {
    if (activeLogTab === 'all') return logs;
    if (activeLogTab === 'passed') return logs.filter(log => !!log.can_pass);
    if (activeLogTab === 'denied') return logs.filter(log => !log.can_pass);
    return logs;
  }, [logs, activeLogTab]);

  // Durum rengi belirleme
  // kaldırıldı: kullanılmıyor
  // const getStatusColor = (status) => { ... };

  // Log mesajı oluşturma (DB'de message kaldırıldı, frontende türetiyoruz)
  const getLogMessage = useCallback((log) => {
    if (!log) return '';
    if (log.can_pass) {
      if (log.status === 'full_compliance') {
        return 'Tüm ekipmanlar mevcut. Geçiş onaylandı.';
      }
      return 'Opsiyonel eksikler mevcut. Geçiş onaylandı.';
    }
    const req = Array.isArray(log.missing_required) ? log.missing_required : [];
    const opt = Array.isArray(log.missing_optional) ? log.missing_optional : [];
    const reqText = req.length > 0 ? `Eksik Zorunlu: ${req.map(item => translations[item] || item).join(', ')}` : '';
    const optText = opt.length > 0 ? `Eksik Opsiyonel: ${opt.map(item => translations[item] || item).join(', ')}` : '';
    return ['Geçiş reddedildi.', reqText, optText].filter(Boolean).join(' ');
  }, []);

  // Kapı durumu rengi - eksik ekipmanla geçiş yapanlar sarı
  // kaldırıldı: kapı durumu kartı yok
  // const getDoorStatusColor = (canPass, status) => { ... };

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-50 to-slate-100">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        
        {/* Professional Header */}
        <div className="app-header mb-8 p-6 shadow-lg">
          <div className="flex flex-col lg:flex-row lg:items-center lg:justify-between gap-4">
            <div className="flex items-center gap-4">
              <div className="w-12 h-12 bg-blue-600 flex items-center justify-center">
                <svg className="w-8 h-8 text-white" fill="currentColor" viewBox="0 0 20 20">
                  <path fillRule="evenodd" d="M2.166 4.999A11.954 11.954 0 0010 1.944 11.954 11.954 0 0017.834 5c.11.65.166 1.32.166 2.001 0 5.225-3.34 9.67-8 11.317C5.34 16.67 2 12.225 2 7c0-.682.057-1.35.166-2.001zm11.541 3.708a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clipRule="evenodd" />
                </svg>
              </div>
              <div>
                <h1 className="text-2xl font-bold text-white mb-1">PPE Detection System</h1>
                <p className="text-slate-300 text-sm font-medium">Kişisel Koruyucu Ekipman Tespit Sistemi</p>
              </div>
            </div>
            <div className="flex items-center gap-4">
              <div className={`connection-indicator ${isConnected ? 'connected' : 'disconnected'}`}>
                <div className={`w-2 h-2 ${isConnected ? 'bg-green-500' : 'bg-red-500'}`}></div>
                <span className="font-medium">
                  {isConnected ? 'Sistem Aktif' : 'Bağlantı Hatası'}
                </span>
              </div>
              {authUser && (
                <div className="relative" ref={userMenuRef}>
                  <button
                    onClick={() => setShowUserMenu(v => !v)}
                    className="flex items-center gap-2 text-white/90 hover:text-white"
                  >
                    <span className="text-sm font-semibold">{authUser.username}</span>
                    <span className="text-xs px-2 py-0.5 bg-white/20 text-white rounded">{authUser.role}</span>
                    <svg className="w-4 h-4 opacity-80" fill="currentColor" viewBox="0 0 20 20">
                      <path fillRule="evenodd" d="M5.293 7.293a1 1 0 011.414 0L10 10.586l3.293-3.293a1 1 0 111.414 1.414l-4 4a1 1 0 01-1.414 0l-4-4a1 1 0 010-1.414z" clipRule="evenodd" />
                    </svg>
                  </button>
                  {showUserMenu && (
                    <div className="absolute right-0 mt-2 w-48 business-card p-2 z-20">
                      <button
                        onClick={async () => {
                          try { await fetchQrImage(); } catch (e) {}
                          setShowQrModal(true);
                          setShowUserMenu(false);
                        }}
                        className="w-full text-left px-3 py-2 text-sm rounded hover:bg-slate-50 text-slate-800"
                      >
                        QR'ımı Göster
                      </button>
                      <button
                        onClick={() => { setShowUserMenu(false); handleLogout(); }}
                        className="w-full text-left px-3 py-2 text-sm rounded hover:bg-slate-50 text-red-600"
                      >
                        Çıkış Yap
                      </button>
                    </div>
                  )}
                </div>
              )}
            </div>
          </div>
        </div>

        {/* Error Alert */}
        {error && (
          <div className="business-card status-error mb-6 p-4 border-l-4">
            <div className="flex items-center gap-2">
              <svg className="w-5 h-5 text-red-600" fill="currentColor" viewBox="0 0 20 20">
                <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clipRule="evenodd" />
              </svg>
              <span className="font-semibold">Sistem Hatası:</span>
              <span>{error}</span>
            </div>
          </div>
        )}

        {/* Admin Login Panel */}
        {!authUser && (
          <div className="max-w-md mx-auto mb-8">
            <div className="business-card p-8">
              <div className="text-center mb-6">
                <h2 className="text-xl font-semibold text-slate-800 mb-2">Sistem Girişi</h2>
                <p className="text-slate-600 text-sm">Yönetici bilgilerinizle giriş yapın</p>
              </div>
              <div className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-slate-700 mb-2">Kullanıcı Adı</label>
                  <input
                    type="text"
                    placeholder="Kullanıcı adınızı girin"
                    value={usernameInput}
                    onChange={(e) => setUsernameInput(e.target.value)}
                    className="form-input w-full px-4 py-3 text-sm"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-slate-700 mb-2">Şifre</label>
                  <input
                    type="password"
                    placeholder="Şifrenizi girin"
                    value={passwordInput}
                    onChange={(e) => setPasswordInput(e.target.value)}
                    className="form-input w-full px-4 py-3 text-sm"
                  />
                </div>
                <button
                  onClick={handleLogin}
                  className="btn-primary w-full px-4 py-3 text-sm font-medium"
                >
                  Sisteme Giriş Yap
                </button>
              </div>
            </div>
          </div>
        )}

        {/* Camera Selection Panel */}
        {authUser?.role === 'admin' && availableCameras.length > 0 && (
          <div className="max-w-2xl mx-auto mb-4">
            <div className="business-card p-4">
              <div className="flex items-center justify-between">
                <h3 className="text-sm font-semibold text-slate-800">Kamera Konfigürasyonu</h3>
                <button
                  onClick={() => setShowCameraConfig(v => !v)}
                  className="text-xs text-blue-600 hover:underline"
                >
                  {showCameraConfig ? 'Gizle' : 'Göster'}
                </button>
              </div>
              {showCameraConfig && (
                <div className="mt-3 flex items-center gap-3">
                  <label className="text-xs font-medium text-slate-700 min-w-fit">Aktif Kamera:</label>
                  <select
                    value={selectedCamera}
                    onChange={(e) => setSelectedCamera(e.target.value)}
                    disabled={isCameraActive}
                    className="form-input flex-1 px-3 py-1.5 text-sm disabled:bg-slate-50 disabled:text-slate-500"
                  >
                    {availableCameras.map(camera => (
                      <option key={camera.deviceId} value={camera.deviceId}>
                        {camera.label || `Kamera ${camera.deviceId.slice(0, 8)}...`}
                      </option>
                    ))}
                  </select>
                </div>
              )}
            </div>
          </div>
        )}

        {/* Control Panel */}
        {authUser?.role === 'admin' && (
          <div className="max-w-4xl mx-auto mb-8">
            <div className="business-card p-6">
              <h3 className="text-lg font-semibold text-slate-800 mb-6">Sistem Kontrolü</h3>
              <div className="grid grid-cols-2 md:grid-cols-2 gap-4">
                <button
                  onClick={startCamera}
                  disabled={isCameraActive || !isConnected}
                  className="btn-primary px-4 py-3 text-sm font-medium disabled:bg-slate-400 disabled:cursor-not-allowed flex items-center gap-2 justify-center"
                >
                  <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
                    <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM9.555 7.168A1 1 0 008 8v4a1 1 0 001.555.832l3-2a1 1 0 000-1.664l-3-2z" clipRule="evenodd" />
                  </svg>
                  Kamera Başlat
                </button>
                <button
                  onClick={stopCamera}
                  disabled={!isCameraActive}
                  className="btn-secondary px-4 py-3 text-sm font-medium disabled:bg-slate-100 disabled:text-slate-400 disabled:cursor-not-allowed flex items-center gap-2 justify-center"
                >
                  <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
                    <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8 7a1 1 0 012 0v6a1 1 0 11-2 0V7zM12 7a1 1 0 012 0v6a1 1 0 11-2 0V7z" clipRule="evenodd" />
                  </svg>
                  Kamera Durdur
                </button>
                
              </div>
            </div>
          </div>
        )}

        {/* Kullanıcı QR Modalı */}
        {showQrModal && (
          <div className="fixed inset-0 z-30 flex items-center justify-center">
            <div className="absolute inset-0 bg-black/40" onClick={() => setShowQrModal(false)}></div>
            <div className="relative business-card p-6 z-40 w-[320px]">
              <div className="flex items-center justify-between mb-3">
                <h3 className="text-sm font-semibold text-slate-800">Kullanıcı QR Bilgisi</h3>
                <button onClick={() => setShowQrModal(false)} className="text-slate-500 hover:text-slate-700 text-sm">Kapat</button>
              </div>
              {qrImageUrl ? (
                <div className="flex items-center justify-center">
                  <img src={qrImageUrl} alt="QR Kod" className="w-48 h-48 object-contain border p-2 bg-white" />
                </div>
              ) : (
                <div className="text-xs text-slate-600">QR yükleniyor...</div>
              )}
            </div>
          </div>
        )}

        {/* Kapı durumu kartı kaldırıldı; video çerçevesi üzerinde gösteriliyor */}

        {/* Main Content Area */}
        <div className="max-w-7xl mx-auto">
          <div className="grid grid-cols-1 gap-8">
            
            {/* Video Stream Section */}
            <div>
              <div className="business-card">
                <div className="p-6 border-b border-slate-200">
                  <div className="flex items-center justify-between">
                    <h3 className="text-lg font-semibold text-slate-800">Canlı Görüntü</h3>
                  </div>
                </div>
                
                <div className="p-6">
                  <div className="relative w-full bg-slate-50 overflow-visible rounded-lg">
                    <video
                      ref={videoRef}
                      autoPlay
                      muted
                      playsInline
                      className="w-full h-auto object-cover rounded-[6px]"
                      style={{ aspectRatio: '21/9', maxHeight: '520px' }}
                      onLoadedMetadata={() => {}}
                    />
                    {/* Instagram-live tarzı eksik ekipman bildirimleri */}
                    <div className="pointer-events-none absolute bottom-3 left-3 right-3 flex flex-col-reverse gap-2">
                      {liveMessages.map(msg => (
                        <div
                          key={msg.id}
                          className={`max-w-[80%] md:max-w-[60%] self-start px-3 py-2 rounded-lg shadow-md backdrop-blur-sm bg-white/70 border border-slate-200 text-slate-800 text-xs md:text-sm transition-all duration-300 ${msg.visible ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-2'}`}
                          style={{}}
                        >
                          {msg.text}
                        </div>
                      ))}
                    </div>
                    {/* Geçiş durumu – üst kenar kalın metinli, diğer kenarlar ince (boşluksuz) */}
                    {detectionResult && (() => {
                      const isPass = !!detectionResult.analysis?.can_pass;
                      const isPartial = detectionResult.analysis?.status === 'partial_compliance';
                      const decisionText = isPass ? (isPartial ? 'Eksikle Kabul' : 'Kabul Edildi') : 'Reddedildi';
                      const colorClass = isPass ? (isPartial ? 'bg-yellow-500' : 'bg-green-600') : 'bg-red-600';
                      const stripTop = `pointer-events-none ${colorClass} text-white z-10`;
                      const stripThin = `pointer-events-none ${colorClass} z-10`;
                      return (
                        <>
                          {/* Üst şerit (kalın ve metinli) - dışa 3px taşır, iç kenar sabit */}
                          <div className={`absolute top-[-3px] left-[-3px] right-[-3px] h-[19px] flex items-center justify-center rounded-t-[6px] ${stripTop}`}>
                            <span className="text-[10px] md:text-xs font-bold tracking-wider uppercase select-none">{decisionText}</span>
                          </div>
                          {/* Alt şerit (ince) - dışa 3px taşır, iç kenar sabit */}
                          <div className={`absolute bottom-[-3px] left-[-3px] right-[-3px] h-[7px] rounded-b-[6px] ${stripThin}`}></div>
                          {/* Sol şerit (ince) - dışa 3px taşır, iç kenar sabit */}
                          <div className={`absolute top-[-3px] bottom-[-3px] left-[-3px] w-[7px] rounded-l-[6px] ${stripThin}`}></div>
                          {/* Sağ şerit (ince) - dışa 3px taşır, iç kenar sabit */}
                          <div className={`absolute top-[-3px] bottom-[-3px] right-[-3px] w-[7px] rounded-r-[6px] ${stripThin}`}></div>
                        </>
                      );
                    })()}
                    {isCameraActive && (
                      <div className="absolute top-6 left-3 px-3 py-1 rounded-full bg-red-50 border border-red-400 text-red-700 text-xs font-semibold flex items-center gap-2 shadow-sm">
                        <span className="w-2 h-2 bg-red-500 rounded-full animate-pulse"></span>
                        CANLI
                      </div>
                    )}
                    {/* Küçük QR Bekleniyor Rozeti */}
                    {!isQrVerified && isCameraActive && (
                      <div className="absolute top-6 right-3 px-3 py-1 rounded-full bg-yellow-50 border border-yellow-400 text-yellow-700 text-xs font-medium shadow-sm">
                        QR bekleniyor
                      </div>
                    )}
                    {!isCameraActive && (
                      <div className="absolute inset-0 flex items-center justify-center text-slate-400 bg-slate-50">
                        <div className="text-center">
                          <svg className="w-16 h-16 mx-auto mb-4 text-slate-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M15 10l4.553-2.276A1 1 0 0121 8.618v6.764a1 1 0 01-1.447.894L15 14M5 18h8a2 2 0 002-2V8a2 2 0 00-2-2H5a2 2 0 00-2 2v8a2 2 0 002 2z" />
                          </svg>
                          <p className="text-lg font-medium mb-2">Kamera Aktif Değil</p>
                          <p className="text-sm">Görüntü akışını başlatmak için kamerayı başlatın</p>
                        </div>
                      </div>
                    )}
                    {/* Detection Canvas */}
                    <canvas ref={canvasRef} className="hidden" />
                  </div>
                </div>
              </div>
              
              {/* System Status - compact (only show if not duplicate of connection) */}
              {/* statusMessage kaldırıldı: bağlantı durumu üst barda gösteriliyor */}
            </div>

            {/* Detection Statistics Panel kaldırıldı */}
          </div>
        </div>

        {/* QR Session and Status Messages */}
        <div className="max-w-7xl mx-auto mt-6 space-y-4">
          
          {/* QR Session Info */}
          {(qrUser && qrRole) && (
            <div className="business-card p-4">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-3">
                  <div className="w-2 h-2 bg-green-500 rounded-full"></div>
                  <span className="text-sm font-medium text-slate-600">QR Oturumu</span>
                </div>
                <div className="flex items-center gap-4">
                  <span className="text-sm font-semibold text-slate-800">{qrUser}</span>
                  <span className="text-xs px-2 py-1 bg-slate-100 text-slate-600 font-medium">{qrRole}</span>
                  <button
                    onClick={handleQrLogout}
                    className="btn-secondary px-3 py-1 text-xs font-medium"
                  >
                    Çıkış
                  </button>
                </div>
              </div>
            </div>
          )}

          {/* QR bekleniyor büyük kart kaldırıldı; küçük rozet video üstünde gösteriliyor */}

          {/* Countdown Timer */}
          {countdownType && countdown !== null && (
            <div className={`business-card p-4 border-l-4 ${countdownType === 'pass' ? 'border-green-500 bg-green-50' : 'border-orange-500 bg-orange-50'}`}>
              <div className="flex items-center gap-3">
                <div className={`w-8 h-8 rounded-full flex items-center justify-center text-sm font-bold ${countdownType === 'pass' ? 'bg-green-500 text-white' : 'bg-orange-500 text-white'}`}>
                  {countdown}
                </div>
                <div>
                  <div className={`font-medium ${countdownType === 'pass' ? 'text-green-800' : 'text-orange-800'}`}>
                    {countdownType === 'pass' ? 'Geçiş Onayı Alındı' : 'Geçiş Onayı Alınamadı'}
                  </div>
                  <div className={`text-sm ${countdownType === 'pass' ? 'text-green-700' : 'text-orange-700'}`}>
                    {countdown} saniye içinde otomatik çıkış yapılacak.
                  </div>
                </div>
              </div>
            </div>
          )}
        </div>

        {/* Equipment Information */}
        <div className="max-w-7xl mx-auto mt-8">
          <div className="business-card p-6">
            <h3 className="text-lg font-semibold text-slate-800 mb-4 text-center">Ekipman Gereksinimleri</h3>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6 text-sm">
              <div>
                <h4 className="font-semibold text-red-700 mb-2">Zorunlu Ekipmanlar</h4>
                <p className="text-slate-600">Kask, Yelek, Bot (eksikse kapı açılmaz)</p>
              </div>
              <div>
                <h4 className="font-semibold text-yellow-700 mb-2">Opsiyonel Ekipmanlar</h4>
                <p className="text-slate-600">Eldiven, Toz Maskesi, Güvenlik Gözlüğü, Koruyucu Kalkan (uyarı verir)</p>
              </div>
            </div>
          </div>
        </div>

        {/* Log Panel */}
        {showLogs && (
          <div className="max-w-7xl mx-auto mt-8">
            <div className="business-card">
              <div className="p-6 border-b border-slate-200">
                <h2 className="text-xl font-semibold text-slate-800">Geçiş Logları ve İstatistikler</h2>
              </div>
              
              <div className="p-6">
                {/* Log Frequency Info */}
                <div className="mb-6 status-card p-4 bg-blue-50 border-blue-200">
                  <h3 className="text-sm font-semibold text-blue-800 mb-2">Log Sıklığı Ayarları</h3>
                  <div className="text-sm text-blue-700 space-y-1">
                    <div><strong>Durum değişikliği</strong>: Anında log kaydı</div>
                    <div><strong>Genel aralık</strong>: Her 2 saniyede bir log (durum aynı olsa bile)</div>
                    <div><strong>Sonuç</strong>: Gereksiz tekrarlı loglar önlenir</div>
                  </div>
                </div>
                
                {/* Statistics */}
                {logStats && (
                  <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-8">
                    <div className="status-card p-4 bg-green-50 border-green-200 text-center">
                      <div className="text-2xl font-bold text-green-700">{logStats.total_passed}</div>
                      <div className="text-sm text-green-600">Toplam Geçiş</div>
                    </div>
                    <div className="status-card p-4 bg-red-50 border-red-200 text-center">
                      <div className="text-2xl font-bold text-red-700">{logStats.total_denied}</div>
                      <div className="text-sm text-red-600">Toplam Reddedilen</div>
                    </div>
                    <div className="status-card p-4 bg-blue-50 border-blue-200 text-center">
                      <div className="text-2xl font-bold text-blue-700">{logStats.today_passed}</div>
                      <div className="text-sm text-blue-600">Bugün Geçiş</div>
                    </div>
                    <div className="status-card p-4 bg-orange-50 border-orange-200 text-center">
                      <div className="text-2xl font-bold text-orange-700">{logStats.today_denied}</div>
                      <div className="text-sm text-orange-600">Bugün Reddedilen</div>
                    </div>
                  </div>
                )}

                {/* Log Filter Tabs */}
                <div className="mb-6">
                  <div className="flex border-b border-slate-200">
                    {['all', 'passed', 'denied'].map(tab => (
                      <button
                        key={tab}
                        onClick={() => {
                          setActiveLogTab(tab);
                          fetchLogs(tab);
                        }}
                        className={`px-6 py-3 text-sm font-medium transition-colors ${
                          activeLogTab === tab
                            ? 'border-b-2 border-blue-600 text-blue-600'
                            : 'text-slate-600 hover:text-slate-800'
                        }`}
                      >
                        {tab === 'all' ? 'Tümü' : tab === 'passed' ? 'Geçenler' : 'Reddedilenler'}
                        {tab === 'all' && getFilteredLogs().length > 0 && ` (${getFilteredLogs().length})`}
                        {tab === 'passed' && logStats && ` (${logStats.total_passed})`}
                        {tab === 'denied' && logStats && ` (${logStats.total_denied})`}
                      </button>
                    ))}
                  </div>
                </div>

                {/* Logs Table */}
                <div className="overflow-hidden">
                  {getFilteredLogs().length > 0 ? (
                    <div className="space-y-2">
                      {getFilteredLogs().slice(0, 50).map((log, index) => (
                        <div 
                          key={index} 
                          className={`status-card p-4 ${
                            log.can_pass 
                              ? 'bg-green-50 border-green-200' 
                              : 'bg-red-50 border-red-200'
                          }`}
                        >
                          <div className="flex items-center justify-between mb-2">
                            <div className="flex items-center gap-3">
                              <div className={`w-3 h-3 rounded-full ${
                                log.can_pass ? 'bg-green-500' : 'bg-red-500'
                              }`}></div>
                              <span className="font-medium text-slate-800">
                                {log.username || 'Bilinmeyen'}
                              </span>
                              <span className="text-xs px-2 py-1 bg-slate-100 text-slate-600">
                                {log.user_role || 'Rol yok'}
                              </span>
                            </div>
                            <span className="text-sm text-slate-500">
                              {new Date(log.timestamp).toLocaleString('tr-TR')}
                            </span>
                          </div>
                          <div className={`text-sm ${
                            log.can_pass ? 'text-green-700' : 'text-red-700'
                          }`}>
                            <strong>Karar:</strong> {log.can_pass ? 'İzin Verildi' : 'Reddedildi'}
                          </div>
                          <div className="text-sm text-slate-600 mt-1">
                            <strong>Detaylar:</strong> {getLogMessage({
                              can_pass: log.can_pass,
                              status: log.status,
                              missing_required: log.missing_required,
                              missing_optional: log.missing_optional
                            })}
                          </div>
                        </div>
                      ))}
                    </div>
                  ) : (
                    <div className="text-center py-8 text-slate-500">
                      Gösterilecek log bulunamadı
                    </div>
                  )}
                </div>
              </div>
            </div>
          </div>
        )}

        {/* Logları Göster Toggle - full width */}
        {authUser?.role === 'admin' && (
          <div className="max-w-7xl mx-auto mt-8">
            <button
              onClick={toggleLogs}
              className="w-full btn-secondary px-4 py-4 text-sm font-medium flex items-center gap-2 justify-center"
            >
              <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
                <path fillRule="evenodd" d="M3 4a1 1 0 011-1h12a1 1 0 110 2H4a1 1 0 01-1-1zm0 4a1 1 0 011-1h12a1 1 0 110 2H4a1 1 0 01-1-1zm0 4a1 1 0 011-1h12a1 1 0 110 2H4a1 1 0 01-1-1zm0 4a1 1 0 011-1h12a1 1 0 110 2H4a1 1 0 01-1-1z" clipRule="evenodd" />
              </svg>
              {showLogs ? 'İstatistikleri Gizle' : 'İstatistikleri Göster'}
            </button>
          </div>
        )}
      </div>
    </div>
  );
};

export default PPEDetectionApp;
