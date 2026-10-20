# Sludge EQ

Stoner rock, doom/sludge, psychedelic rock, noise rock, harsh noise ve drone (Yellow Swans gibi) dinleyenler için
Material 3 Expressive **parametrik ekolayzır**. Çalan şarkının meta verisinden türünü tanır ve o türe uygun filtre
setini kendisi uygular. İnternet izni yoktur; hiçbir şey cihazından çıkmaz.

## Ne yapar

- **Parametrik ekolayzır**: en çok 10 filtre; her biri tepe, alçak raf ya da yüksek raf olabilir. Frekans (20 Hz–20 kHz),
  kazanç (±12 dB) ve Q (darlık) ayarlanır. Üstte filtrelerin toplam frekans eğrisi çizilir.
- **Tür ön ayarları** (her biri gerçek bir filtre seti): Stoner Rock, Doom / Sludge, Psychedelic Rock, Noise Rock,
  Harsh Noise, Drone / Yellow Swans, Shoegaze, Düz. Bas güçlendirici, sanallaştırıcı ve ses yükseltici de ön ayarla gelir.
- **Otomatik tür tanıma**: şarkı değişince çaların verdiği tür etiketine, sanatçı listesine, sonra albüm ve şarkı adındaki
  anahtar kelimelere bakar; bulduğu türün ön ayarını uygular ve nedenini yazar. Tanıyamazsa Düz kalır; ön ayar çiplerinden kendin seçersin. Otomatik ayar anahtarla kapatılabilir.
- **Kulaklık düzeltmesi**: JBL Tune 520BT için Harman over-ear 2018 hedefine göre auto-EQ. Sayılar AutoEq'in Rtings ölçümünden
  ürettiği 10 bantlık düzeltmedir; seçtiğin eğrinin üstüne eklenir. Uçlardaki (31 Hz, 16 kHz) büyük artışlar +3 dB ile
  sınırlıdır ve ön kazanç gerçekte uygulanan artışa göre hesaplanır (yaklaşık −3 dB); "Düzeltme gücü" ile daha da azaltabilirsin.
  Düzeltme yalnızca o kulaklık bağlıyken (Bluetooth adından anlaşılır) uygulanır; telefon hoparlöründe ve başka kulaklıklarda
  uygulanmaz. Ad okunamıyorsa "Her çıkışta uygula" ile zorlanır.
- **Dinleme**: son 30 günde hangi türleri dinlediğini halka grafikte gösterir; kayıt yalnızca telefonunda kalır.
- **Kurulum**: bildirim erişimi, kısıtlanmış ayar yardımı, pil, yakalanan ses oturumları.

Harsh noise ayarı kulak ve hoparlör için tizi kısar ve ses yükseltiyi kapalı tutar.

## Bildirim erişimi neden gerekli, nasıl verilir

Android, başka bir uygulamanın ne çaldığını yalnızca **bildirim erişimi** olan uygulamaya gösterir
(`MediaSessionManager.getActiveSessions`). Bu izin uygulama içinden verilemez; kullanıcı ayarlarda açar. Uygulama
bildirim metinlerini okumaz, yalnızca medya oturumlarına bakar.

1. Kurulum sekmesi → **Erişimi ver** → "Sludge EQ" anahtarını aç.
2. Anahtar gri ve açılmıyorsa (Android 13+, APK ile kurulum): Uygulama bilgisi → ⋮ → **Kısıtlanmış ayarlara izin ver**,
   sonra 1. adıma dön.
3. Bilgisayarla: 
   ```
   adb shell appops set com.vardiya.sludge ACCESS_RESTRICTED_SETTINGS allow
   adb shell cmd notification allow_listener com.vardiya.sludge/com.vardiya.sludge.audio.SludgeListenerService
   ```

## Ses nasıl işleniyor

Önce Android'in **DynamicsProcessing** efekti denenir: 31 üçte-bir oktav bant (20 Hz–20 kHz) ve bir sınırlayıcı. Parametrik
filtrelerin (ya da grafik eğrinin) toplam yanıtı bu bantlarda örneklenir; ses yükseltme sınırlayıcının kazancıdır, bu yüzden
bozulmadan yükseltir. Bas düğmesi alt bantlara raf ekler. Telefon DynamicsProcessing'i kabul etmezse klasik ekolayzır ve ses
yükseltici kullanılır; eğri o ekolayzırın kendi bantlarında okunur. Hangisinin çalıştığı Kurulum sekmesinde her çalar için yazar.

Ekolayzır, çalar uygulamanın ses oturumuna bağlanır. Spotify, Poweramp, VLC gibi "harici ekolayzır" destekleyen çalarlar
oturumlarını `OPEN_AUDIO_EFFECT_CONTROL_SESSION` yayınıyla bildirir; bu izin istemez. Genel ses karışımına (oturum 0)
bağlanma da denenir ama Android 10+ çoğunda çalışmaz; Kurulum sekmesi sonucu gösterir. Desteklemeyen bir çalarda
ekolayzır sessiz kalır; bunu Android engeller, uygulama aşamaz.

## Gizlilik

Uygulamanın internet izni yoktur. Tür tanıma tamamen cihazda yapılır; şarkı bilgisi hiçbir yere gönderilmez. Eski sürümlerde
kaydedilmiş bir API anahtarı varsa açılışta silinir.

## Derleme

```bash
./gradlew testDebugUnitTest recordRoborazziDebug verifyRoborazziDebug detekt lintDebug assembleDebug assembleRelease
```

Bulut oturumunda: `-I .claude/gradle/maven-central-mirror.gradle` ekle ve `ANDROID_HOME` ayarla.
Eskiz: `docs/m3e-canvas/sludge-eq.json` (https://lnkiai.github.io/m3e-canvas/ → *Open project*).
