# Yedekleme ve Geri Yükleme

Uygulamanın yedeği **tek bir JSON dosyasıdır** ve **tüm listeleri** içerir.
Tek liste değil, tüm veri yedeklenir; böylece liste değiştirirken yedek almayı
unutmak veri kaybına yol açmaz.

## Yedek alma

**Yedek** tuşu → **Dosyaya kaydet (.json)**

- Dosya adı: `bagis-yedek-YYYYAAGG-SSDD.json`
- Android 10 ve üzeri: doğrudan **İndirilenler** klasörüne yazılır.
- Android 9 ve altı: sistemin "farklı kaydet" ekranı açılır.
- Dosya içeriği:

```json
{
  "format": "bagis-listesi-backup-v2",
  "app": "BagisListesi",
  "version": 1,
  "savedAt": "2026-11-26T18:50:00.000Z",
  "current": "Kasım 2026",
  "lists": [
    { "name": "Kasım 2026", "items": [ { "n": "Ali", "k": 150000 } ] }
  ]
}
```

`k` alanı **kuruş** cinsinden tam sayıdır (150000 = 1.500,00 ₺). Bu sayede
yuvarlama kaybı olmaz.

## Geri yükleme

**Yedek** tuşu → **Dosyadan geri yükle** → dosyayı seç.

Yedek tarihi ve liste özeti gösterildikten sonra iki seçenek sunulur:

| Seçenek | Ne yapar |
|---|---|
| **Hepsini birleştir** | Aynı adlı liste varsa satırlar isim eşleşmesine göre güncellenir/eklenir, yoksa yeni liste açılır. Mevcut listeler silinmez. |
| **Yedekle değiştir** | Cihazdaki **tüm listeler** yedekteki listelerle birebir değiştirilir. |

## Eski yedekler

- Eski `.txt` yedekleri (`İsim | Tutar` satırları, `## Liste:` başlıkları) hâlâ
  okunur; tek listeli yedek "bu listeye ekle / yeni liste aç" olarak sorulur,
  çok listeli yedek her listeyi kendi adıyla geri yükler.
- Eski sürümün `bagis-listesi-backup-v1` JSON yedeği de okunur.

## Sürüm notu

- 1.1: İki ayrı yedekleme sistemi (MainActivity içindeki eski JSON köprüsü ile
  `index.html` içindeki `.txt` yedeği) birleştirildi. Eski köprü `bagis-listesi-v2`
  anahtarını okuduğu için uygulama verisi `v3`'e geçtikten sonra yedek ve geri
  yükleme tamamen çalışmıyordu; köprü kaldırıldı, tek format kaldı.
