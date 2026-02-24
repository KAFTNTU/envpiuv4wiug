-keep class com.robocar.app.ui.blockly.AndroidBridge { *; }
-keepclassmembers class com.robocar.app.ui.blockly.AndroidBridge {
    @android.webkit.JavascriptInterface <methods>;
}
