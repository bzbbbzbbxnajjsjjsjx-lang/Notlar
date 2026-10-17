## Notlar

Material 3 Expressive not ve yapılacaklar uygulaması.

**Kurulum:** Aşağıdaki `Notlar-*.apk` dosyasını telefona indir ve aç (bilinmeyen kaynaklardan yüklemeye izin vermen gerekebilir). Android 8.0 ve üzeri.

### Yenilikler (1.4.0)
- **Expressive FAB menüsü:** Sağ alttaki + düğmesine basınca düğme şekil değiştirip ✕ olur ve üstünde *Not*, *Yapılacak listesi* ve *Panodan not* açılır. Arkadaki liste kararır; dışarı dokununca ya da geri tuşuyla menü kapanır.
- **Panodan not:** Kopyaladığın metin tek dokunuşla yeni not olur. Panoda metin yoksa uyarı çıkar.
- **Dalgalı ilerleme:** Yapılacak kartlarındaki ilerleme çubuğu artık Expressive'in dalgalı çubuğu.
- **Boş ekran:** Simge Expressive "kurabiye" şeklinin üstünde.
- **Yükleniyor göstergesi:** Notlar yüklenirken şekil değiştiren gösterge görünür.

### Yenilikler (1.3.0)
- **Yeni alt menü:** Daha yüksek alt menü; seçili sekmenin simgesi geniş bir hapın içinde, etiketi altında.
- **Kaydırarak işlem:** Bir notu sağa kaydır, arşivlensin (arşivde: arşivden çıksın); sola kaydır, çöp kutusuna gitsin. İkisi de "Geri al" ile notun önceki hâline döner. Çöp kutusunda sağa kaydırınca not geri yüklenir.
- **Arama vurgusu:** Aradığın kelime kartlarda başlıkta, metinde ve yapılacaklarda işaretlenir.
- **Geri al / Yinele:** Yazma ekranında ilk değişiklikten sonra üstte geri al ve yinele düğmeleri belirir. Art arda yazılan harfler tek adımda geri alınır.

### Yenilikler (1.2.0)
- **Nottan editöre geçiş:** Bir nota dokununca kart büyüyerek yazma ekranına dönüşür, geri dönünce kartına küçülür. Yeni not ortadan büyüyerek açılır.
- **Alt menü animasyonları:** Notlar simgesi zıplar, Arşiv kutusu içine bir şey konmuş gibi düşüp esner, Çöp kutusu kapağı sallanır.
- **Bölüm geçişleri:** Liste, dokunduğun sekmenin yönünden kayarak gelir.
- **Seçim modu:** Üst çubuk yumuşakça değişir, alt menü aşağı kayar, "Not" düğmesi küçülerek kaybolur.
- **Küçük dokunuşlar:** Kartlar basınca hafifçe küçülür, yapılacak ilerleme çubuğu dolarak ilerler, sabitleme iğnesi takılırken döner, yapılacak satırları eklenip silinirken yumuşakça yer değiştirir.

### Yenilikler (1.1.0)
- **Dinamik renkler:** Ayarlar'da açılınca uygulama renklerini duvar kağıdından alır (Android 12 ve üzeri). Sarı, Kum, Yeşil ve Kırmızı notlar kendi renklerinde kalır.
- **Ayarlar:** Üstteki dişli simgesinden açılır. Tema (Sistem / Açık / Koyu), sıralama (Düzenlenme / Oluşturulma / Başlık) ve yedekleme burada.
- **Yedek:** Tüm notlar tek bir JSON dosyasına dışa aktarılır, aynı dosya geri içe aktarılabilir. Aynı not iki tarafta da varsa en son düzenlenen kalır.
- **Liste görünümü:** Üstteki düğmeyle iki sütunlu ızgara ile tek sütunlu liste arasında geçilir.
- **Toplu seçim:** Bir nota uzun basınca seçim başlar. Seçilen notlar birlikte sabitlenir, arşivlenir ya da çöpe taşınır ("Geri al" ile).
- **Yapılacaklar:** Kartlarda ilerleme çubuğu ve "1/4" sayısı. Yazma ekranındaki ⋮ menüsünden "Tüm işaretleri kaldır" ve "Tamamlananları sil".
- **Yazma ekranı:** ⋮ menüsünden "Kopyasını oluştur". Altta kelime sayısı görünür.
- **Paylaşımdan not:** Başka bir uygulamadan paylaşılan metin, Notlar seçilince yeni not olarak açılır.
- **Kısayollar:** Uygulama simgesine uzun basınca "Yeni not" ve "Yeni liste".

### Yenilikler (1.0.0)
- **Notlar:** İki sütunlu kartlar; sabitlenenler üstte, sonra en son düzenlenenler. Başlık, metin ve yapılacaklar içinde arama (Türkçe büyük/küçük harf duyarsız). *Tümü / Notlar / Yapılacaklar* süzgeçleri.
- **Yazma ekranı:** Başlık ve metin; yazdıkça otomatik kaydedilir, boş bırakılan not tutulmaz. Altta yüzen araç çubuğu: yapılacak listesine çevir / düz metne çevir, renk (Varsayılan, Sarı, Kum, Yeşil, Kırmızı), arşivle, paylaş, çöp kutusuna taşı. Üstte sabitleme.
- **Yapılacaklar:** İşaretlenen öğeler "Tamamlananlar" altına iner; klavyede ileri ile yeni öğe eklenir.
- **Arşiv ve çöp kutusu:** Arşivlenen notlar ana listeden kalkar. Silinen notlar 30 gün çöp kutusunda kalır; geri yüklenebilir, tek tek ya da toplu kalıcı silinebilir. Arşivleme ve silme "Geri al" ile geri alınabilir.
- **Görünüm:** Material 3 Expressive bileşenleri (büyük esnek başlık çubuğu, kısa alt menü, yüzen araç çubuğu, basınca şekil değiştiren düğmeler), kehribar renk teması, açık ve koyu mod.

> APK debug anahtarıyla imzalıdır; Play Store'a yüklemek için kendi imza anahtarınla imzalanması gerekir.
