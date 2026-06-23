# Hệ thống theo dõi sức khỏe cá nhân

Đây là mã nguồn của đề tài:

**“Xây dựng hệ thống Theo dõi sức khoẻ cá nhân trên nền tảng di động”**

## Thành phần hệ thống

- Thiết bị IoT sử dụng ESP32-C3 Mini
- Cảm biến MAX30102 đo nhịp tim và SpO2
- MPU6050 hỗ trợ đếm bước chân và nhận biết cử chỉ
- OLED SSD1306 hiển thị dữ liệu
- NEO-6M theo dõi hoạt động GPS
- Firebase Realtime Database
- Firebase Authentication
- Firebase Cloud Messaging và Cloud Functions
- Ứng dụng Android sử dụng Kotlin và Jetpack Compose

## Cấu trúc mã nguồn

- `android-app/`: mã nguồn ứng dụng Android
- `esp32-firmware/`: mã nguồn chương trình ESP32-C3

## Cài đặt ứng dụng Android

1. Mở thư mục `android-app` bằng Android Studio.
2. Tạo project Firebase hoặc sử dụng project đã được cấp quyền.
3. Tải file `google-services.json` và đặt vào thư mục `android-app/app`.
4. Cấu hình API key bản đồ nếu chức năng bản đồ được sử dụng.
5. Sync Gradle và chạy ứng dụng.

## Cài đặt chương trình ESP32

1. Mở file `.ino` bằng Arduino IDE.
2. Cài đặt các thư viện được yêu cầu.
3. Sao chép `secrets.example.h` thành `secrets.h`.
4. Điền WiFi và cấu hình Firebase vào `secrets.h`.
5. Chọn đúng board ESP32-C3 và cổng COM.
6. Biên dịch và nạp chương trình.

## Lưu ý

Các chỉ số sức khỏe trong hệ thống phục vụ mục đích học tập, nghiên cứu và thử nghiệm, không thay thế thiết bị y tế hoặc chẩn đoán của bác sĩ.
