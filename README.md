# Bağış Listesi

Bağışları listeleyen, tutarları toplayan ve yedekleyen basit Android uygulaması.
Arayüz tek bir HTML dosyasıdır (`app/src/main/assets/index.html`), uygulama bunu
WebView içinde açar ve verileri cihazda saklar.

## Özellikler
- Çoklu liste, satır ekleme, satıra dokunup düzenleme, silme
- İsimde arama ve sıralama (isim / tutar)
- Aynı isim girildiğinde uyarı: güncelle veya yeni satır ekle
- `.txt` olarak yedekleme ve yedekten geri yükleme
- Toplam kişi sayısı ve tutar

## APK indirme
1. `Actions` sekmesi → `APK derle` iş akışı
2. En son çalıştırmayı aç → `Artifacts` → `bagis-listesi-apk` indir
3. Zip'i aç, içindeki `app-debug.apk` dosyasını telefona kur

## Notlar
- İmza anahtarı `bagis.keystore` depoda bulunur; her derleme aynı anahtarla
  imzalandığı için yeni APK eskisinin üstüne kurulur ve veriler korunur.
- Halka açık bir depoda imza anahtarının bulunması güvenlik açısından önerilmez.
