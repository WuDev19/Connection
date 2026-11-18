# Connection

Connection là ứng dụng Android giúp bạn chia sẻ khoảnh khắc bằng ảnh, video và giữ liên lạc với bạn bè. Ứng dụng được viết bằng Java, sử dụng Firebase cho xác thực và dữ liệu, Cloudinary cho nội dung đa phương tiện.

## Tính năng

- **Tài khoản:** đăng ký, đăng nhập bằng email/mật khẩu hoặc số điện thoại qua OTP; đặt lại mật khẩu và cập nhật hồ sơ.
- **Bạn bè:** tìm kiếm người dùng, gửi và xử lý lời mời kết bạn.
- **Chia sẻ khoảnh khắc:** chụp ảnh, quay video, chọn người nhận, thêm trạng thái và nhạc vào bài đăng.
- **Hiệu ứng camera:** chọn bộ lọc màu và hiệu ứng khuôn mặt độc lập; áp dụng hiệu ứng cho ảnh chụp và video.
- **Tương tác:** xem bài đăng, bình luận và thả cảm xúc.
- **Nhắn tin:** trò chuyện, theo dõi tin nhắn chưa đọc và trạng thái đã xem.
- **Thông báo:** nhận thông báo qua Firebase Cloud Messaging (FCM).
- **Widget ảnh:** hiển thị nội dung trên màn hình chính, có nút làm mới và số bài chưa xem.

## Công nghệ

| Thành phần | Công nghệ |
| --- | --- |
| Ngôn ngữ và giao diện | Java, XML, View Binding, Material Components |
| Tổ chức giao diện | ViewModel, LiveData, Repository |
| Xác thực và dữ liệu | Firebase Authentication, Cloud Firestore |
| Thông báo | Firebase Cloud Messaging, Retrofit, Gson |
| Lưu trữ ảnh/video | Cloudinary |
| Camera và hiệu ứng | CameraX, OpenGL ES, MediaPipe Tasks Vision |
| Hiển thị ảnh và phát media | Glide, AndroidX Media3 ExoPlayer |
| Dữ liệu cục bộ và tác vụ nền | Room, WorkManager |
| Kiểm thử | JUnit, AndroidX Test, Espresso |

## Yêu cầu môi trường

- Android Studio có hỗ trợ Android Gradle Plugin 8.6.0.
- JDK 17 để chạy Gradle; mã nguồn Java được cấu hình tương thích Java 8.
- Android SDK Platform 35 (`compileSdk = 35`, `targetSdk = 34`).
- Thiết bị hoặc máy ảo Android 7.0 trở lên (API 24); có camera để thử tính năng chụp/quay.
- Kết nối Internet để tải thư viện và sử dụng dịch vụ trực tuyến.

Dự án có sẵn Gradle Wrapper 8.7, không cần cài Gradle riêng.

## Cài đặt và chạy

### 1. Mở dự án

Tải mã nguồn, mở thư mục gốc `Connection` bằng Android Studio và chọn JDK 17 trong phần cấu hình Gradle.

### 2. Cấu hình Firebase

1. Tạo hoặc sử dụng Firebase project được cấp quyền truy cập.
2. Đăng ký ứng dụng Android với package name `com.example.connection`.
3. Tải cấu hình và đặt tại `app/google-services.json`.
4. Bật Email/Password và Phone trong Firebase Authentication theo phương thức đăng nhập cần sử dụng. Với Phone Auth, cấu hình dấu vân tay chứng chỉ SHA-1/SHA-256 của bản build trong Firebase.
5. Khởi tạo Cloud Firestore và cấu hình quyền truy cập phù hợp cho người dùng đã xác thực. Tạo các index cần thiết theo thông báo của Firestore khi chạy truy vấn.

File `app/google-services.json` được bỏ qua bởi Git nên cần chuẩn bị riêng khi thiết lập máy mới. Cấu hình backend gửi FCM phải dùng cùng Firebase project với ứng dụng.

### 3. Cấu hình Cloudinary và Android SDK

Tạo hoặc cập nhật `local.properties` tại thư mục gốc. Ví dụ trên Windows:

```properties
sdk.dir=C:/Users/YOUR_USER/AppData/Local/Android/Sdk
CLOUDINARY_CLOUD_NAME=your_cloud_name
```

Thay đường dẫn SDK và cloud name bằng giá trị của bạn. Trong Cloudinary, tạo **unsigned upload preset** tên `post_upload`, cho phép tải lên ảnh và video theo nhu cầu của ứng dụng. Mã nguồn hiện dùng preset này khi upload media.

`local.properties` được bỏ qua bởi Git. Không đặt Cloudinary API secret hoặc khóa Firebase Admin vào mã nguồn Android.

### 4. Cấu hình backend thông báo

Ứng dụng gọi backend qua [ApiClient.java](app/src/main/java/com/example/connection/data/api/ApiClient.java). Có thể đổi địa chỉ mặc định trong file này hoặc gọi `ApiClient.setBaseUrl(...)` trước khi khởi tạo API client.

Ví dụ khi backend chạy trên máy phát triển ở cổng 8080:

```java
// Android Emulator truy cập máy phát triển.
ApiClient.setBaseUrl("http://10.0.2.2:8080/");

// Thiết bị thật cùng mạng Wi-Fi: thay bằng IP LAN của máy phát triển.
// ApiClient.setBaseUrl("http://192.168.1.10:8080/");
```

Backend gửi FCM và theo dõi bài đăng để cập nhật widget nằm trong repository riêng `FCM-Connection`

### 5. Build và khởi chạy

Trong Android Studio, đồng bộ Gradle, chọn cấu hình `app`, chọn thiết bị rồi nhấn **Run**.

Hoặc chạy trong PowerShell tại thư mục gốc:

```powershell
# Build APK debug.
.\gradlew.bat :app:assembleDebug

# Cài lên thiết bị hoặc máy ảo đang kết nối.
.\gradlew.bat :app:installDebug
```

APK debug được tạo tại `app/build/outputs/apk/debug/app-debug.apk`.

Trên macOS/Linux, dùng `./gradlew` thay cho `.\gradlew.bat` (cấp quyền thực thi bằng `chmod +x gradlew` nếu cần).

## Cấu trúc thư mục

```text
Connection/
├── app/
│   ├── src/main/
│   │   ├── java/com/example/connection/
│   │   │   ├── ConnectionApp.java   # Khởi tạo ứng dụng
│   │   │   ├── constants/          # Hằng số
│   │   │   ├── data/               # API, lưu trữ, tiện ích và worker
│   │   │   ├── model/              # Mô hình dữ liệu
│   │   │   ├── repository/         # Interface và triển khai truy cập dữ liệu
│   │   │   ├── service/            # Xử lý Firebase Messaging
│   │   │   └── ui/                 # View, ViewModel, adapter và camera
│   │   ├── assets/                # Model nhận diện khuôn mặt
│   │   ├── res/                   # Layout, drawable, chuỗi và tài nguyên
│   │   └── AndroidManifest.xml
│   ├── src/test/                  # Unit test
│   ├── src/androidTest/           # Kiểm thử trên thiết bị
│   └── build.gradle.kts
├── docs/                          # Tài liệu camera và widget
├── gradle/                        # Wrapper và danh mục phiên bản thư viện
├── build.gradle.kts
└── settings.gradle.kts
```

Luồng tổ chức chính: **Activity/Fragment → ViewModel → Repository → Firebase/dịch vụ dữ liệu**. `ConnectionApp` khởi tạo Cloudinary và bộ điều phối đồng bộ widget.

## Kiểm tra dự án

```powershell
# Unit test, Android Lint và build APK debug.
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug

# Instrumentation test: cần thiết bị hoặc máy ảo đang kết nối.
.\gradlew.bat :app:connectedDebugAndroidTest
```

Các bài kiểm thử hiện có bao gồm kiểm tra model, trạng thái phát media, zoom, hiệu ứng camera và nội dung widget. Nên kiểm tra thêm camera trước/sau, quay video có âm thanh, OTP, thông báo và đồng bộ giữa hai tài khoản trên thiết bị thật.