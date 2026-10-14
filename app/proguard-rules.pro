# DTO-классы парсятся Gson'ом через рефлексию — их имена полей трогать нельзя.
-keep class com.example.pokeexplorer.data.remote.dto.** { *; }
-keepattributes Signature
-keepattributes *Annotation*

# Retrofit
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
