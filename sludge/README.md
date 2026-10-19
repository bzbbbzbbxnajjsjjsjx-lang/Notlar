# Sludge EQ

Stoner rock, doom/sludge, psychedelic rock, noise rock, harsh noise ve drone (Yellow Swans gibi) dinleyenler için
Material 3 Expressive ekolayzır. Çalan şarkının meta verisini okur, yapay zekâya sorar, ekolayzırı kendisi ayarlar.

## Ne yapar

- **Ekolayzır**: 10 ince sütun (31 Hz–16 kHz, ±12 dB), bas güçlendirici, sanallaştırıcı,
  ses yükseltici. Anahtarların hepsi tikli.
- **Yapay Zekâ**: Claude, OpenAI ya da OpenRouter; kendi API anahtarını girersin. OpenRouter'da model adı yazmadan
  canlı listeden seçilir (aranabilir, fiyatı ve "ücretsiz" etiketiyle). Şarkı değişince model adı,
  sanatçı ve albüme bakıp eğriyi seçer, nedenini Türkçe yazar. Sonuç saklanır, aynı şarkı için tekrar istek atılmaz.
- **Ön ayarlar** (ana sayfada): Stoner Rock, Doom / Sludge, Psychedelic Rock, Noise Rock, Harsh Noise, Drone / Yellow Swans, Shoegaze, Düz.
- **Kulaklık düzeltmesi**: JBL Tune 520BT için Harman over-ear 2018 hedefine göre auto-EQ. Sayılar AutoEq'in Rtings ölçümünden
  ürettiği 10 bantlık düzeltmedir; seçtiğin eğrinin üstüne eklenir. Uçlardaki (31 Hz, 16 kHz) büyük artışlar +3 dB ile
  sınırlıdır ve ön kazanç gerçekte uygulanan artışa göre hesaplanır (AutoEq'in −6,1 dB'i yerine yaklaşık −3 dB); "Düzeltme gücü"
  ile daha da azaltabilirsin. Aynı modelin başka bir çifti bir iki dB
  farklı çıkabilir. Düzeltme yalnızca o kulaklık bağlıyken (Bluetooth adından anlaşılır) uygulanır; telefon hoparlöründe ve başka
  kulaklıklarda uygulanmaz. Ad okunamıyorsa "Her çıkışta uygula" ile zorlanır.
- **Last.fm**: çalan şarkının tür etiketlerini (stoner rock, doom metal…) Last.fm'den alır. Ücretsiz anahtar:
  last.fm/api/account/create. Etiketler hem yerel tür tanımayı hem yapay zekâyı besler.
- **Kurulum**: bildirim erişimi, kısıtlanmış ayar yardımı, pil, yakalanan ses oturumları.

Anahtar yoksa ya da ağ yoksa **yerel tür tanıma** (sanatçı listesi + tür etiketi) devreye girer. Harsh noise ayarı kulak ve
hoparlör için tizi kısar ve ses yükseltiyi kapalı tutar; yapay zekâ cevapları da aynı sınırlara sıkıştırılır.

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

Önce Android'in **DynamicsProcessing** efekti denenir: 10 gerçek bant ve bir sınırlayıcı (ses yükseltme, sınırlayıcının kazancıdır,
bu yüzden bozulmadan yükseltir). Bas düğmesi ekolayzırın alt bantlarını yükseltir. Telefon DynamicsProcessing'i kabul etmezse klasik
ekolayzır ve ses yükseltici kullanılır. Hangisinin çalıştığı Kurulum sekmesinde her çalar için yazar.

Ekolayzır, çalar uygulamanın ses oturumuna bağlanır. Spotify, Poweramp, VLC gibi "harici ekolayzır" destekleyen çalarlar
oturumlarını `OPEN_AUDIO_EFFECT_CONTROL_SESSION` yayınıyla bildirir; bu izin istemez. Genel ses karışımına (oturum 0)
bağlanma da denenir ama Android 10+ çoğunda çalışmaz; Kurulum sekmesi sonucu gösterir. Desteklemeyen bir çalarda
ekolayzır sessiz kalır; bunu Android engeller, uygulama aşamaz.

## Gizlilik

Yapay zekâ sağlayıcısına yalnızca şarkı adı, sanatçı, albüm, tür etiketleri ve çalar uygulamanın adı gider; Last.fm'e yalnızca
şarkı adı ile sanatçı. API anahtarları Android
Keystore (AES-GCM) ile şifrelenir, yedeğe girmez, yalnızca `https://` adresine (ya da yerel ağa) gönderilir.

## Derleme

```bash
./gradlew testDebugUnitTest recordRoborazziDebug verifyRoborazziDebug detekt lintDebug assembleDebug assembleRelease
```

Bulut oturumunda: `-I .claude/gradle/maven-central-mirror.gradle` ekle ve `ANDROID_HOME` ayarla.
Eskiz: `docs/m3e-canvas/sludge-eq.json` (https://lnkiai.github.io/m3e-canvas/ → *Open project*).
