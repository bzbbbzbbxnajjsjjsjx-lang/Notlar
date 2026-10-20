# Sludge EQ

Stoner rock, doom/sludge, psychedelic rock, noise rock, harsh noise ve drone (Yellow Swans gibi) dinleyenler için
Material 3 Expressive **parametrik ekolayzır**. Her şeyi sen ayarlarsın; uygulama kendi kendine bir şey değiştirmez.
İzin istemez: bildirim erişimi yok, internet yok. Hiçbir şey cihazından çıkmaz.

## Ne yapar

- **Parametrik ekolayzır**: en çok 10 filtre; her biri tepe, alçak raf ya da yüksek raf olabilir. Frekans (20 Hz–20 kHz),
  kazanç (±12 dB) ve Q (darlık) ayarlanır. Üstte filtrelerin toplam frekans eğrisi çizilir.
- **Tür ön ayarları** (her biri gerçek bir filtre seti, elle seçilir): Stoner Rock, Kyuss, Doom / Sludge, Psychedelic Rock, Noise Rock,
  High Rise, Harsh Noise, Drone / Yellow Swans, Shoegaze, Düz. Bas güçlendirici, sanallaştırıcı ve ses yükseltici de ön ayarla gelir; seçtikten
  sonra filtreleri istediğin gibi değiştirirsin.
- **JBL Tune 520BT otomatik EQ**: AutoEq'in Rtings (B&K 5128) ölçümünden Harman over-ear 2018 hedefine ürettiği 10 filtrelik
  parametrik düzeltme, yayımlandığı haliyle uygulanır (filtre toplamı AutoEq'in eğrisiyle 0,25 dB içinde uyuşur, testle
  doğrulanır). Bu kulaklık 3–4 kHz'de +10 dB'lik keskin bir tepe verir ve 10 kHz üstü ile alt bas zayıftır; düzeltme tepeyi alır,
  üstü ve basın altını yükseltir. Çalamayan 35 Hz altı için artış +3 dB ile sınırlıdır, böylece ses AutoEq'in −6,4 dB'si yerine
  yaklaşık −3 dB kısılır. Ekranda düzeltmenin eğrisi çizilir; "Düzeltme gücü" ile azaltabilirsin. Yalnızca o kulaklık bağlıyken
  (Bluetooth adından anlaşılır) uygulanır; hoparlörde ve başka kulaklıklarda uygulanmaz, ad okunamıyorsa "Her çıkışta uygula" ile
  zorlanır. Tür ön ayarı düzeltmenin üstüne eklenir. Ayrıntı ve kaynaklar: `docs/eq-research.md`.
- **Kurulum**: pil ayarı ve hangi çalarların bağlandığı.

Harsh noise ön ayarı kulak ve hoparlör için tizi kısar ve ses yükseltiyi kapalı tutar.

## Ses nasıl işleniyor

Önce Android'in **DynamicsProcessing** efekti denenir: 31 üçte-bir oktav bant (20 Hz–20 kHz) ve bir sınırlayıcı. Parametrik
filtrelerin (kulaklık düzeltmesi ve bas rafı dahil) toplam yanıtı bu bantlarda örneklenir; ses yükseltme sınırlayıcının kazancıdır, bu yüzden
bozulmadan yükseltir. Bas düğmesi alt bantlara raf ekler. Telefon DynamicsProcessing'i kabul etmezse klasik ekolayzır ve ses
yükseltici kullanılır; eğri o ekolayzırın kendi bantlarında okunur. Hangisinin çalıştığı Kurulum sekmesinde her çalar için yazar.

Ekolayzır, çalar uygulamanın ses oturumuna bağlanır. Spotify, Poweramp, VLC gibi "harici ekolayzır" destekleyen çalarlar
oturumlarını `OPEN_AUDIO_EFFECT_CONTROL_SESSION` yayınıyla bildirir; bu izin istemez. Genel ses karışımına (oturum 0)
bağlanma da denenir ama Android 10+ çoğunda çalışmaz; Kurulum sekmesi sonucu gösterir. Desteklemeyen bir çalarda
ekolayzır sessiz kalır; bunu Android engeller, uygulama aşamaz.

## Gizlilik

Uygulamanın internet izni ve bildirim erişimi yoktur; hangi şarkının çaldığını bilmez ve hiçbir veri göndermez. Eski sürümlerde
kaydedilmiş API anahtarı, dinleme geçmişi ya da ayar notları varsa açılışta silinir.

## Derleme

```bash
./gradlew testDebugUnitTest recordRoborazziDebug verifyRoborazziDebug detekt lintDebug assembleDebug assembleRelease
```

Bulut oturumunda: `-I .claude/gradle/maven-central-mirror.gradle` ekle ve `ANDROID_HOME` ayarla.
Eskiz: `docs/m3e-canvas/sludge-eq.json` (https://lnkiai.github.io/m3e-canvas/ → *Open project*).
