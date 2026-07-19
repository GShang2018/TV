# libtorrent4j (SWIG JNI bindings)
-keep class org.libtorrent4j.** { *; }
-keep class org.libtorrent4j.swig.** { *; }
-keepclassmembers class org.libtorrent4j.swig.libtorrent_jni {
    static *** SwigDirector_*(*);
}
-keepclassmembers class org.libtorrent4j.swig.libtorrent_jni {
    *** SwigDirector_*(*);
}
-keepclassmembers class org.libtorrent4j.swig.alert {
    *** SwigDirector_*(*);
}
-keepclassmembers class org.libtorrent4j.swig.alert_notify_callback {
    *** SwigDirector_*(*);
}

# btengine module
-keep class com.fongmi.btengine.** { *; }
