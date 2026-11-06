# Proguard rules for Retrofit, Gson and SimpleXML
-dontwarn javax.xml.stream.**
-dontwarn javax.xml.bind.**
-keepattributes *Annotation*
-keepclassmembers class * {
    @org.simpleframework.xml.* *;
    @javax.xml.bind.annotation.* *;
}
-keep class ma.projet.restclient.entities.** { *; }

