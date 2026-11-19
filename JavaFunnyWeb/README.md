# JavaFunnyWeb — link công khai, chỉ chủ trang được tải ảnh

Website Java Spring Boot cho IntelliJ IDEA. Khách xem ảnh và chơi trò chơi mà không cần đăng nhập. Chỉ chủ trang có mật khẩu mới tải ảnh lên.

## Yêu cầu
- JDK 17+
- IntelliJ IDEA
- Maven (IntelliJ có thể tự tải Maven)

## Chạy local bằng IntelliJ
1. Giải nén ZIP và mở thư mục có `pom.xml`.
2. Chờ Maven tải thư viện.
3. Cấu hình biến môi trường trong Run Configuration:
   - `OWNER_PASSWORD`: mật khẩu quản trị mạnh, riêng tư.
   - `UPLOAD_DIR`: `./uploads` (tuỳ chọn; mặc định này đã có sẵn).
4. Chạy `FunnyWebApplication.java`.
5. Mở `http://localhost:8080`, đăng nhập trong “Khu vực chủ trang web” rồi tải ảnh.

Thiết lập biến môi trường trước khi mở IntelliJ từ terminal:

Windows PowerShell:
```powershell
$env:OWNER_PASSWORD="MatKhauManh_RiengCuaBan"
$env:UPLOAD_DIR=".\uploads"
```

macOS/Linux:
```bash
export OWNER_PASSWORD='MatKhauManh_RiengCuaBan'
export UPLOAD_DIR='./uploads'
```

## Deploy Internet công khai
Chạy `localhost` không tạo link Internet. Cần deploy ứng dụng lên dịch vụ hosting hỗ trợ Docker hoặc Java 17. Dự án có sẵn Dockerfile.

Quy trình tổng quát:
1. Tạo repository GitHub riêng và đẩy source code lên.
2. Tạo dịch vụ mới từ repository trên nền tảng hosting hỗ trợ Docker (ví dụ Railway hoặc Render).
3. Chọn build bằng Dockerfile.
4. Cấu hình biến môi trường:
   - `OWNER_PASSWORD`: mật khẩu mạnh của bạn.
   - `UPLOAD_DIR=/app/uploads`
5. Tạo persistent volume/disk và mount chính xác vào `/app/uploads`.
6. Deploy; hosting cấp URL HTTPS công khai để gửi cho bạn bè.
7. Mở URL trên điện thoại dùng mạng di động để kiểm tra.
8. Đăng nhập bằng mật khẩu chủ trang để tải ảnh. Mở URL ở cửa sổ ẩn danh để kiểm tra khách chỉ xem được ảnh.

## Bảo mật và lưu trữ
- Ảnh tải lên được công khai cho bất kỳ ai có link website. Không tải ảnh riêng tư/nhạy cảm.
- Ảnh lưu trong `UPLOAD_DIR`. Nếu hosting không có persistent disk, ảnh có thể mất khi deploy hoặc khởi động lại.
- Không ghi mật khẩu vào source code hoặc commit lên GitHub.
- Phiên quản trị dùng cookie HttpOnly và mã CSRF cho thao tác upload.
- Bản mẫu chưa có giới hạn thử mật khẩu, chức năng xoá ảnh hoặc sao lưu tự động. Nếu sử dụng lâu dài, nên bổ sung các biện pháp này.
