## Notlar

Material 3 Expressive not ve yapılacaklar uygulaması.

**Kurulum:** Aşağıdaki `Notlar-*.apk` dosyasını telefona indir ve aç (bilinmeyen kaynaklardan yüklemeye izin vermen gerekebilir). Android 8.0 ve üzeri.

### Yenilikler (1.7.2)
- **Açılış hatası düzeltildi:** Uygulama açılmadan kapanıyordu (1.7.0 ve 1.7.1). Artık normal açılıyor.

### Yenilikler (1.7.1)
- **Daha büyük alt ikonlar:** Alttaki Notlar, Arşiv ve Çöp kutusu ikonları büyütüldü, artık daha rahat görünüyor ve basılıyor.

### Yenilikler (1.7.0)
- **Expressive yazı tipi:** Uygulama artık Material 3 Expressive'in yazı tipi Google Sans Flex ile yazıyor. Büyük başlıklar geniş, yuvarlak ve kalın; not metni sade ve okunaklı.
- **Yazı boyutu:** Yazma ekranındaki *Aa* düğmesiyle (ya da Ayarlar > Görünüm'den) not yazısını *Küçük*, *Normal*, *Büyük* ya da *Dev* yap. İstersen kartlarda da büyüsün.
- **Hatırlatıcı:** Yazma ekranının üstündeki alarm düğmesiyle *Bugün 18:00*, *Yarın 09:00*, *Gelecek hafta* ya da istediğin tarih ve saat. Günlük, haftalık ya da aylık tekrarlanabilir. Zamanı gelince bildirim gelir, dokununca not açılır. Kartta küçük bir saat çipi görünür. Telefon yeniden başlasa da hatırlatıcılar kaybolmaz.
- **Etiketler:** Notlara *İş*, *Ev*, *Alışveriş* gibi etiketler ver (yazma ekranında ⋮ > Etiketler ya da başlığın altındaki çip). Ana ekranda etikete göre süz; *Düzenle* ile etiketleri yeniden adlandır ya da sil. Aramada etiketler de bulunur.
- **Ana ekran widget'ları:** *Yapılacak listesi* widget'ı seçtiğin listeyi ana ekranda gösterir, maddeler oradan işaretlenir. *Yeni not* widget'ı tek dokunuşla not ya da liste açar.
- **Gizlilik:** Uygulama kilidi açıkken widget'ta liste görünmez, "Notlar kilitli" yazar; bildirimde notun içeriği değil yalnızca "Bir hatırlatıcın var" görünür.

### Yenilikler (1.6.0)
- **Uygulama kilidi:** Ayarlar > Gizlilik'ten PIN (4–8 rakam) belirle; Notlar açılışta PIN sorar. Expressive kilit ekranı: dolunca şekil değiştiren noktalar, büyük tuşlar, yanlış PIN'de sallanma.
- **Biyometrik:** Parmak izi veya yüz ile açma (telefon destekliyorsa).
- **Otomatik kilit:** Uygulamadan çıkınca *Hemen*, *1 dk* ya da *5 dk* sonra yeniden kilitlenir.
- **Deneme sınırı:** 5 yanlış PIN'den sonra 30 saniye bekleme; her 5 yanlışta süre ikiye katlanır.
- **PIN'imi unuttum:** Telefonun kendi ekran kilidiyle doğrulayıp açarsın, notlar silinmez; yeni PIN'i Ayarlar'dan belirlersin. Biyometrik kapalıysa yalnızca desen/PIN/parola kabul edilir.
- **Kilidi korumak:** Kilidi kapatmak ya da PIN'i değiştirmek için önce mevcut PIN sorulur. Kilit açıkken son uygulamalar ekranında notlar görünmez.
- **Şifreli depolama:** Notlar ve ayarlar cihazda AES-256 ile şifreli saklanır; anahtar Android Keystore'da. Eski kayıtlar ilk açılışta otomatik şifrelenir.
- **Ekranı gizle:** Son uygulamalarda içerik görünmez, ekran görüntüsü alınamaz.
- **Şifreli yedek:** Dışa aktarırken parola verirsen (iki kez, en az 8 karakter) yedek AES-256 ile şifrelenir; içe aktarırken parola sorulur.
- **Veri güvenliği:** Notlar bir gün okunamazsa (örneğin şifre anahtarı kaybolursa) hiçbir şey üzerine yazılmaz; uygulama bunu açıkça söyler.
- **Not:** Şifreli veriler başka cihaza taşınamadığı için Android'in bulut yedeği ve cihazdan cihaza aktarımı kapatıldı; yeni telefona geçerken *Dışa aktar*ı kullan.
- **Ayarlar:** Gruplu, simgeli satırlar; anahtarlar açıkken ✓ gösterir.

### Yenilikler (1.5.0)
- **Expressive arama çubuğu:** Notlar ve Arşiv'de en üstte hap biçimli arama çubuğu. Dokununca tam ekran arama açılır: son aramalar (silinebilir), *Tümü / Notlar / Yapılacaklar* süzgeçleri ve yazdıkça vurgulu sonuçlar.
- **Bölünmüş Paylaş düğmesi:** Yazma ekranında Paylaş'ın sol yarısı hemen paylaşır, oku ise *Metin olarak paylaş*, *Markdown olarak paylaş* ve *Panoya kopyala* seçeneklerini açar.
- **Şekil değiştiren kartlar:** Kartın köşeleri basınca ve seçilince yumuşakça yuvarlanır; seçim rozeti daireden Expressive kurabiye şekline dönüşerek belirir.
- **Seçim modu:** Üstte küçük bir "N seçildi" çubuğu.

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
