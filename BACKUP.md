# Yedekleme ve Geri Yükleme

Yedek **tek bir JSON dosyasıdır** ve **tüm listeleri** içerir.

## Yedek alma

**Yedek** → **Dosyaya kaydet (.json)**

- Dosya: `bagis-yedek-YYYYAAGG-SSDD.json` → **İndirilenler** klasörü.
- İşlem bitince ekranda `Yedek kaydedildi: … (N liste, M kişi)` yazar.
- **Sayfa yenilenmez**, uygulama kapanmaz; buton tekrar tekrar kullanılabilir.
- Kaydetme başarısız olursa sessiz kalmaz, nedenini söyler; son çare olarak
  "Metin olarak kopyala" vardır.

```json
{
  "format": "bagis-listesi-backup-v2",
  "app": "BagisListesi",
  "version": 1,
  "savedAt": "2026-11-26T19:21:00.000Z",
  "current": "Kasım 2026",
  "lists": [ { "name": "Kasım 2026", "items": [ { "n": "Ali", "k": 125000 } ] } ]
}
```

`k` = kuruş (125000 → 1.250,00 ₺).

## Geri yükleme

**Yedek** → **Dosyadan geri yükle** → dosyayı seç.

Yedek tarihi ve liste özeti gösterilir, sonra üç seçenek:

| Buton | Ne yapar |
|---|---|
| **Vazgeç** | Hiçbir şey değişmez. |
| **Yedekle değiştir** | Cihazdaki tüm listeler yedektekiyle birebir değişir. |
| **Hepsini birleştir** | Aynı adlı liste varsa satırlar isim eşleşmesiyle güncellenir, yoksa yeni liste açılır. |

Eski `.txt` yedekleri (`İsim | Tutar`, `## Liste:` başlıkları) ve eski
`bagis-listesi-backup-v1` JSON yedekleri de okunur.

## Genel toplam listesi

Alt kısımdaki **Genel toplam** satırına dokununca kişi bazlı toplam listesi açılır:

- Aynı isimli kişiler **tüm listelerde toplanır** (büyük/küçük harf ve fazla boşluk
  farkı yok sayılır; "Ayşe", "ayşe " ve "AYŞE" aynı kişidir).
- Her satırda kişinin toplamı ve hangi listede ne kadar verdiği yazar:
  `Ayşe — 8.510,00 ₺` / `Kasım 2026: 3.500,00 ₺ • Aralık 2026: 5.000,00 ₺`
- Tutara göre büyükten küçüğe sıralıdır, içinde arama kutusu vardır.
- **Listeyi kopyala** ile tüm toplamlar panoya alınır.

## Sürüm notları

- **1.3** — Genel toplam listesi eklendi (kişi bazlı, tüm listeleri toplar).

- **1.2** — Yedek butonunun "bir kez çalışıp susması" düzeltildi: modal artık her
  açılışta sıfırdan kuruluyor, butonlara sabit `id` verilmiyor (aynı `id`'nin iki kez
  kullanılması `getElementById`'i ilk butona bağlıyor ve sonraki butonlar ölüyordu).
  Geri yükleme için ayrı `BagisFile.pick` köprüsü eklendi, sonuç `bagisRestore` ile
  dönüyor; iptal/okuma hatası ekranda bildiriliyor.
- **1.1** — İki ayrı yedekleme sistemi birleştirildi; eski köprü `bagis-listesi-v2`
  okuduğu için `v3` verisiyle hiç çalışmıyordu.
