# Add project specific ProGuard rules here.
-keep class com.game.puzzle2048.** { *; }
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}
