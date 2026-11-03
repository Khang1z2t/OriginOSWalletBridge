# OriginOS Wallet Bridge

[English](README.md)

OriginOS Wallet Bridge là ứng dụng Android độc lập, nhẹ, dành cho thiết bị OriginOS tương thích. Ứng dụng cho phép Android chuyển shortcut nhấn đúp nút nguồn của Vivo sang Google Wallet qua hai implicit Intent đã xác minh:

- `vivo.intent.action.NFC_BASEAPP_SWIPE_ACTIVITY` từ Vivo NFC Base App (`com.vivo.nfcbaseapp`)
- `vivo.intent.action.NFC_BUS_SWINGSPLASH` từ Vivo Wallet (`com.vivo.wallet`)

## Cách Hoạt Động

1. OriginOS gửi implicit Intent khi nhấn đúp nút nguồn.
2. Android chuyển Intent tới handler mặc định đã chọn trong Resolver.
3. OriginOS Wallet Bridge mở Google Wallet trong task riêng và tự `finish()`.

Ứng dụng không sửa NFC payment routing, Wallet Role, Vivo package hay System Settings.

## Tính Năng

- Không AccessibilityService, root, Shizuku, background service, polling hay WakeLock.
- Google Wallet nằm trong task Recents riêng.
- Chỉ dùng một Android framework Activity nhẹ.
- Không cần USB Debugging hoặc Wireless Debugging sau khi cài đặt.
- Adaptive launcher icon: vòng shortcut đỏ và chấm nổi trên nền sáng dịu.

## V1.1

- Nhận cả hai shortcut action OriginOS đã xác minh: NFC Base App và Vivo Wallet.
- Giữ nguyên application ID và Activity Resolver, nên default đã chọn cho action cũ vẫn được bảo toàn.
- Mỗi shortcut action mới cần chọn **Luôn luôn** một lần trong Android Resolver.

## Tương Thích

Đã kiểm chứng: iQOO 15T chạy OriginOS 7.

Các thiết bị OriginOS khác chưa được kiểm chứng. Thiết bị phải gửi một trong hai implicit action đã hỗ trợ và hiện Android Resolver cho handler tương thích.

## Cài Đặt Và Thiết Lập

1. Cài APK release đã ký, hoặc APK debug để thử nghiệm.
2. Nhấn đúp nút nguồn.
3. Trong Android Resolver, chọn **OriginOS Wallet Bridge** rồi chọn **Luôn luôn**.
4. Nếu Vivo Wallet/NFC Base App đổi shortcut action đang gửi, lặp lại bước 3 cho action đó.
5. Nhấn đúp nút nguồn lần nữa để mở Google Wallet.

`applicationId` và Activity handler được giữ ổn định. Cập nhật APK cùng signing key sẽ giữ lựa chọn handler mặc định.

## Sử Dụng Google Wallet

Thêm thẻ và cấu hình thanh toán NFC trong Google Wallet như bình thường. Ứng dụng này chỉ mở Google Wallet, không cấu hình hay thay đổi NFC payment default.

## Khôi Phục Handler Vivo

Mở Android App Info của OriginOS Wallet Bridge và clear defaults. Nhấn đúp nút nguồn, sau đó chọn **NFC Base App** hoặc **Vivo Wallet** trong Resolver. Gỡ cài đặt OriginOS Wallet Bridge cũng xóa handler và trả từng action về handler Vivo còn lại.

## Giới Hạn Màn Hình Khóa

Ứng dụng không bypass Keyguard và không xin quyền hiển thị trên màn hình khóa. Việc Google Wallet có hiện trước khi mở khóa hay không do OriginOS và Google Wallet quyết định. Hãy tự kiểm tra trên thiết bị.

## Build Từ Source

```bash
./gradlew :app:assembleDebug
./gradlew :app:assembleRelease
```

Release bật R8 và resource shrinking. Nếu không đặt biến môi trường signing, APK release sẽ là unsigned. Để tạo APK release có thể cài đặt, đặt `RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, và `RELEASE_KEY_PASSWORD`.

## Quyền Riêng Tư

Ứng dụng không xin runtime permission. Ứng dụng không đọc nội dung màn hình, dữ liệu thanh toán NFC, thẻ, tài khoản hay dữ liệu ứng dụng ngân hàng. Ứng dụng chỉ lưu timestamp đơn điệu để giới hạn spam shortcut. Bất kỳ ứng dụng nào cũng có thể gửi action đã hỗ trợ, nhưng extras đầu vào bị bỏ qua và ứng dụng chỉ mở Google Wallet Activity cố định.

## Tuyên Bố Miễn Trừ

Đây là dự án độc lập, không liên kết, được bảo trợ hay được hỗ trợ bởi Vivo, iQOO, Google hay Google Wallet. Tự chịu trách nhiệm khi sử dụng và hãy kiểm tra trước khi phụ thuộc vào ứng dụng.

## Tác Giả Và Giấy Phép

Tác giả: Đinh Quốc Bảo Khang.

Giấy phép [MIT](LICENSE).
