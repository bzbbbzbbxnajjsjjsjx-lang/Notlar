# Parametrik EQ araştırması

Bu not, Sludge EQ'nun ön ayarlarının ve JBL Tune 520BT düzeltmesinin neye dayandığını ve neye dayanmadığını ayırır.
Dayanak ile mühendislik yargısı bilerek ayrı yazıldı: türlere özel yayımlanmış bir EQ eğrisi bulunamadı.

## 1. Bulunanlar ve bulunamayanlar

| Konu | Sonuç |
|---|---|
| JBL Tune 520BT için hazır düzeltme | **Var.** AutoEq deposunda Rtings'in Bruel & Kjaer 5128 ölçümünden üretilmiş 10 filtrelik parametrik sonuç, 10 bantlık sabit-bant sonucu ve ham ölçüm CSV'si var. |
| Doom / stoner / sludge için yayımlanmış EQ eğrisi | **Yok.** Aramalarda yalnızca gitar/amfi/pedal ayarı ve genel miksaj tavsiyesi çıktı; frekans sayısı veren bir kaynak yoktu. |
| Harsh noise için dinleme EQ'su | **Yok.** Kulak hassasiyeti (ISO 226) konusunda genel bilgi var, türe özel öneri yok. |
| Harman hedefinin kişiden kişiye değişmesi | **Var (ikinci el).** Dinleyicilerin yaklaşık %64'ü Harman eğrisini, %15'i daha çok, %21'i daha az bas istiyor. Rakamlar özet yazılardan, makalenin kendisine bağlanamadı. |

Bu yüzden ön ayarlar aşağıdaki mantıkla yazılmış **mühendislik yargısıdır**, ölçüm değil. Kulakla ayarlanmak üzere
başlangıç noktalarıdır; filtreleri düzenleyebilirsin.

## 2. JBL Tune 520BT: ölçülen ne, düzeltme ne yapıyor

Kaynak: AutoEq, `results/Rtings/Bruel & Kjaer 5128 over-ear/JBL Tune 520BT`, hedef Harman over-ear 2018.
Uygulamadaki 10 filtre AutoEq'in `ParametricEQ.txt` dosyasıyla birebir aynıdır (Fc, kazanç, Q). Testler,
filtrelerin toplamının AutoEq'in CSV'deki `parametric_eq` sütunuyla 0,25 dB içinde uyuştuğunu doğrular.

Ham ölçüm (hedefle karşılaştırma):

| Bölge | Ölçüm | Ne demek |
|---|---|---|
| 20–30 Hz | −6,4 … −4 dB | Alt bas neredeyse çalmıyor; AutoEq burada +6,4 dB'ye kadar yükseltiyor. |
| 50–100 Hz | +2 … +3 dB | Bas kısmı zaten dolgun. |
| 160–500 Hz | −2 … −3 dB | Hedefin kendi çukuruyla uyumlu. |
| 3–4 kHz | **+9 … +11 dB** | En büyük sorun: kulağın en duyarlı olduğu bölgede keskin tepe. Sert müzikte yorucu. |
| 10–16 kHz | −1,5 … −7,6 dB | Üst tiz zayıf, hava yok. |

Düzeltme bunu şöyle çözer: 3,5 kHz tepesini yaklaşık −4 dB (kısılma payı dahil −7 dB'ye kadar) alır, 10 kHz üstünü
+3 dB'ye kadar yükseltir, alt basa hafif destek verir.

### Neden AutoEq'in önerdiği −6,4 dB değil de yaklaşık −3 dB kısılma

AutoEq'in ön kazancı en büyük artışa göre hesaplanır; burada o artış 20 Hz'deki +6,4 dB'dir. Ama bu kulaklık 20–30 Hz'i
zaten çalamıyor (ölçüm −6 dB), yani o boost sese değil yalnızca ses kısılmasına yarar. Uygulama toplam düzeltmenin
artışını **+3 dB ile sınırlar** ve kısılma payını gerçekte uygulanan en büyük artıştan hesaplar. Sonuç yaklaşık −3 dB;
duyulabilir bölgedeki (40 Hz üstü) şekil AutoEq'inkiyle aynıdır. "Düzeltme gücü" kaydırıcısı hem düzeltmeyi hem de
kısılmayı orantılı azaltır.

### Sınırlar

- Tek bir cihazın ölçümü; aynı modelin başka bir çifti 1–2 dB sapabilir.
- Bluetooth (SBC) sıkıştırması ve telefonun ses seviyesi ölçüme dahil değil.
- Düzeltme yalnızca kulaklık bağlıyken (Bluetooth adından anlaşılır) uygulanır; hoparlörde uygulanmaz.

## 3. Tür ön ayarlarının dayanağı

Kullanılan fiziksel ve işitsel gerçekler:

- **Alçak akort temel sesleri:** 4 telli bas E1 ≈ 41 Hz, 5. tel B ≈ 31 Hz; doom/stoner gitarları sık sık C–A arasına
  akortlu (alçak C ≈ 65 Hz, A ≈ 55 Hz). Bas davul ağırlığı 50–100 Hz. Bu yüzden ağır türlerde **alçak raf** 100–160 Hz
  köşesinde kullanılır. Rafın kazancına köşenin çok altında ulaşılır (köşede yarısı), bu yüzden köşe, hedeflenen basın
  üstüne konur.
- **Fuzz gövdesi ve çamur:** fuzz'lı gitar/bas gövdesi 100–250 Hz, yoğun karışımlarda çamurlaşma 200–500 Hz. Orta
  Q'lu küçük kesim ya da artış buraya yazılır.
- **Kulak duyarlılığı:** insan kulağı 2–5 kHz arasında en duyarlıdır (ISO 226, kulak kanalı rezonansı). Harsh noise ve
  noise rock'ta bu bölge hem en yorucu hem en keskin yerdir; harsh noise ön ayarı burayı kısar.
- **Hava ve parıltı:** 8–16 kHz psychedelic ve shoegaze'de ses duvarının parlaklığı, doom ve harsh noise'ta ise
  yorucu cızırtıdır.

Ön ayarların sonuçta verdiği eğri (dB, 520BT düzeltmesi hariç):

| Tür | 40 Hz | 100 | 250 | 500 | 1k | 2k | 3,5k | 6k | 10k | 16k | Bas | Sanal. | Ses yük. |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| Stoner Rock | +5,0 | +4,6 | +2,4 | +0,1 | −1,2 | +0,1 | +1,4 | +0,3 | −0,8 | −1,4 | 450 | 150 | 300 |
| Doom / Sludge | +7,0 | +6,2 | +2,9 | −0,5 | −2,4 | −1,1 | −1,5 | −2,0 | −2,9 | −3,0 | 750 | 100 | 200 |
| Psychedelic Rock | +2,2 | +1,9 | +0,5 | +0,6 | +1,1 | +1,1 | +2,2 | +1,7 | +2,9 | +3,5 | 200 | 600 | 200 |
| Noise Rock | +2,0 | +1,5 | −0,6 | −1,2 | −0,1 | +1,1 | +3,5 | +1,2 | +0,8 | +1,0 | 250 | 100 | 300 |
| Harsh Noise | +2,0 | +1,3 | −0,1 | −1,0 | −1,7 | −2,1 | −4,5 | −4,3 | −2,9 | −3,1 | 100 | 0 | 0 |
| Drone / Yellow Swans | +4,0 | +3,4 | +1,7 | +0,4 | −0,1 | −0,9 | −1,3 | −0,2 | +1,1 | +1,9 | 500 | 750 | 100 |
| Shoegaze | +2,2 | +1,9 | +0,1 | −1,7 | −1,6 | +0,5 | +1,3 | +1,0 | +2,3 | +3,0 | 250 | 500 | 250 |

Tasarım kuralları (testle korunur): hiçbir ön ayar +8 dB'yi geçmez, en çok 10 filtre kullanır, harsh noise 2 kHz üstünde
hiç yükseltme yapmaz ve ses yükseltici kapalıdır.

520BT düzeltmesiyle birlikte kullanıldığında ön ayarlar düzeltmenin **üstüne** eklenir: düzeltme kulaklığı nötr
hedefe getirir, ön ayar türün tadını verir. Örneğin harsh noise + 520BT, 3,5 kHz'de yaklaşık −11 dB'dir (ön ayarın −4,5 dB'si artı düzeltmenin −7 dB'si); bu kasıtlıdır, çünkü 520BT bu bölgede zaten +10 dB tepe verir.

## 4. Tür tanıma neye bakar

Çalardan gelen tür etiketi (ilk tür adı veren etiket kazanır), sonra sanatçı listesi, sonra albüm ve şarkı adındaki
anahtar kelimeler. Hepsi telefonda; ağ yok. Tanınmayan sanatçı Düz alır ve ön ayarı elle seçersin. Bu, kapsamın
sanatçı listesiyle sınırlı olduğu anlamına gelir; liste `domain/GenreDetector.kt` içinde.

## Kaynaklar

- AutoEq, JBL Tune 520BT sonuçları: <https://github.com/jaakkopasanen/AutoEq/tree/master/results/Rtings/Bruel%20%26%20Kjaer%205128%20over-ear/JBL%20Tune%20520BT>
- Rtings, EQ ölçümleri hakkında: <https://www.rtings.com/headphones/learn/research/eq-remeasurements>
- Harman hedefi ve dinleyici gruplarına dair özetler: <https://www.headphonesty.com/2024/10/harman-target-curve-alive/>, <https://audiosolace.com/harman-target-curve-explained/>
- Eş-şiddet eğrileri (ISO 226): <https://www.sfu.ca/sonic-studio-webdav/cmns/Handbook/Equal_Loudness_Contours.html>
- Alçak raf / tepe filtresi ve Q kullanımı üzerine genel kılavuz: <https://guitar.soundshed.com/docs/effects/eq-parametric>
- Filtre formülleri: Robert Bristow-Johnson, "Audio EQ Cookbook" (uygulamadaki `Biquad`).
