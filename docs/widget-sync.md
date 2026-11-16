# Widget ảnh và đồng bộ dữ liệu

Widget dùng `RemoteViews`, bo góc 24dp và có nút làm mới. `WidgetUpdater` là nơi duy nhất ghi nội dung widget; mỗi lần đổi ảnh dùng `updateAppWidget` đầy đủ để tránh giữ lại ảnh cũ trong các lần cập nhật từng phần.

## Luồng hoạt động

- Khi thêm widget: khởi tạo trạng thái tải/đăng nhập, lên lịch tải ngay và lịch dự phòng 15 phút.
- Khi app đang hiển thị: `WidgetSyncCoordinator` theo dõi Firestore. Chỉ những thay đổi ảnh, nội dung, người đăng hoặc badge mới yêu cầu tải lại; thay đổi liên tục được gom trong 700ms.
- Khi app vào nền: gỡ listener sau 700ms để tránh ngắt/kết nối lại khi chuyển Activity. Công việc đã lên lịch vẫn tiếp tục.
- Khi xem bài: đợi thao tác cập nhật `seen` kết thúc rồi yêu cầu Worker tính lại badge từ Firestore.
- Khi có bài mới: backend Go theo dõi Firestore và gửi FCM dạng data. Android kiểm tra tài khoản nhận rồi lên lịch Worker; sự kiện không hiển thị thông báo.
- Khi đăng xuất/đổi tài khoản: xóa ảnh cũ khỏi widget, hủy yêu cầu cũ và tăng số phiên. Kết quả tải thuộc tài khoản hoặc phiên trước không được phép ghi lại.
- Khi mất mạng hoặc tải ảnh thất bại: giữ nội dung tốt gần nhất, thử lại theo WorkManager. Widget mới chưa có ảnh sẽ giữ trạng thái tải.

## Backend

Phần backend nằm trong repository `D:\FCM-Connection`:

- `internal/firebase/client.go`: tạo Firestore client bằng cấu hình Firebase hiện có.
- `internal/widget/sync.go`: theo dõi 100 bài mới nhất, bỏ qua lịch sử lúc khởi động và hạn chế sự kiện trùng khi kết nối lại.
- `main.go`: khởi chạy watcher cùng HTTP server.
- `internal/widget/sync_test.go`: kiểm tra người nhận, loại sự kiện, lịch sử và gửi dữ liệu.

Backend đang chạy cần được khởi động lại để nạp mã mới. Việc sửa mã không tự triển khai hoặc khởi động lại dịch vụ.

FCM payload:

```json
{
  "type": "WIDGET_REFRESH",
  "recipient_id": "firebase-user-id",
  "post_id": "firestore-post-id"
}
```

Payload không chứa ảnh, nội dung bài hay `notification`. Android luôn tải dữ liệu bằng quyền của tài khoản đang đăng nhập. Ưu tiên FCM là `normal`, có collapse key và thời hạn 15 phút. Android có thể trì hoãn khi tiết kiệm pin; không cam kết cập nhật ngay khi buộc dừng app. Lịch 15 phút cũng là lịch tối thiểu yêu cầu, không phải thời điểm chạy chính xác.

Watcher không phát lại lịch sử khi backend vừa khởi động. Lịch dự phòng và tải khi mở app giúp bù các sự kiện bỏ lỡ khi server ngừng hoạt động hoặc có lượng bài vượt cửa sổ theo dõi.

## Kiểm tra

- Unit test: `WidgetContentTest` kiểm tra badge, dữ liệu thiếu, ảnh đại diện video, bỏ qua thay đổi không liên quan và sự kiện gửi cho tài khoản cũ.
- Kiểm tra thiết bị: `WidgetRemoteViewsTest` kiểm tra RemoteViews có thể inflate, root có clipping, bán kính góc đúng và nút làm mới/badge tồn tại.
- Backend: `go test ./...`; các bài test dùng sender giả, không gửi FCM hoặc ghi Firestore thật.

Kiểm tra tích hợp bằng hai tài khoản thật sau khi khởi động lại backend: đăng ảnh/video, đưa app người nhận xuống nền, xem badge giảm sau khi xem bài, thêm hai widget, đổi kích thước, bật/tắt mạng và đăng xuất/đăng nhập tài khoản khác. Không tạo dữ liệu thử nghiệm trong Firestore production tự động.
