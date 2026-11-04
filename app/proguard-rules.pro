# 1. Tối ưu mã bytecode và làm mờ sâu
-repackageclasses ''
-allowaccessmodification
-mergeinterfacesaggressively

# 2. Xóa sạch toàn bộ câu lệnh Log và System.out trong bản Release
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
}
-assumenosideeffects class java.io.PrintStream {
    public void println(...);
    public void print(...);
}

# 3. Bảo vệ các Model Firestore & Room (không làm mờ để parse JSON / Firestore reflection an toàn)
-keepclassmembers class com.example.connection.model.** {
    private <fields>;
    public <methods>;
    public <init>();
}
-keep class com.example.connection.data.local.** { *; }

# 4. Bảo vệ WorkManager Workers (được tạo qua reflection bởi WorkerFactory)
-keep class * extends androidx.work.Worker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class com.example.connection.data.worker.** { *; }

# 5. Bảo vệ thư viện bên thứ ba
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keep public class com.google.firebase.** { *; }
-keep class com.google.android.gms.tasks.** { *; }
-keep class com.firebase.ui.** { *; }
-keep public class com.cloudinary.** { *; }
-keep class com.bumptech.glide.** { *; }
-keep public class * extends com.bumptech.glide.module.AppGlideModule
