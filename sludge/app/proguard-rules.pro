# kotlinx.serialization keeps its generated serializers via bundled consumer rules.
-keepattributes *Annotation*, InnerClasses

# Glance starts WorkManager at launch; it opens its Room database by reflection on the generated
# class, which R8 otherwise removes and the app dies before showing anything.
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-keep class androidx.work.impl.WorkDatabase_Impl { <init>(); }
