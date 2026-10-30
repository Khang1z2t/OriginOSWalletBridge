# OriginOS Wallet Bridge

[English](README.md)

OriginOS Wallet Bridge la ung dung Android doc lap, nhe, danh cho thiet bi OriginOS tuong thich. Ung dung cho phep Android chuyen shortcut nhan dup nut nguon cua Vivo sang Google Wallet qua implicit Intent da xac minh: `vivo.intent.action.NFC_BASEAPP_SWIPE_ACTIVITY`.

## Cach Hoat Dong

1. OriginOS gui implicit Intent khi nhan dup nut nguon.
2. Android chuyen Intent toi handler mac dinh da chon trong Resolver.
3. OriginOS Wallet Bridge mo Google Wallet trong task rieng va tu `finish()`.

Ung dung khong sua NFC payment routing, Wallet Role, Vivo package hay System Settings.

## Tinh Nang

- Khong AccessibilityService, root, Shizuku, background service, polling hay WakeLock.
- Google Wallet nam trong task Recents rieng.
- Chi dung mot Android framework Activity nhe.
- Khong can USB Debugging hoac Wireless Debugging sau khi cai dat.

## Tuong Thich

Da kiem chung: iQOO 15T chay OriginOS 7.

Cac thiet bi OriginOS khac chua duoc kiem chung. Thiet bi phai gui cung implicit action cua Vivo va hien Android Resolver cho handler tuong thich.

## Cai Dat Va Thiet Lap

1. Cai APK release da ky, hoac APK debug de thu nghiem.
2. Nhan dup nut nguon.
3. Trong Android Resolver, chon **OriginOS Wallet Bridge** roi chon **Luon luon**.
4. Nhan dup nut nguon lan nua de mo Google Wallet.

`applicationId` va Activity handler duoc giu on dinh. Cap nhat APK cung signing key se giu lua chon handler mac dinh.

## Su Dung Google Wallet

Them the va cau hinh thanh toan NFC trong Google Wallet nhu binh thuong. Ung dung nay chi mo Google Wallet, khong cau hinh hay thay doi NFC payment default.

## Khoi Phuc Vivo NFC Base App

Mo Android App Info cua OriginOS Wallet Bridge va clear defaults. Nhan dup nut nguon, sau do chon **NFC Base App** trong Resolver. Go cai dat OriginOS Wallet Bridge cung xoa handler cua no va tra action ve Vivo khi Vivo la handler con lai.

## Gioi Han Man Hinh Khoa

Ung dung khong bypass Keyguard va khong xin quyen hien thi tren man hinh khoa. Viec Google Wallet co hien truoc khi mo khoa hay khong do OriginOS va Google Wallet quyet dinh. Hay tu kiem tra tren thiet bi.

## Build Tu Source

```bash
./gradlew :app:assembleDebug
./gradlew :app:assembleRelease
```

Release bat R8 va resource shrinking. Neu khong dat bien moi truong signing, APK release se la unsigned. De tao APK release co the cai dat, dat `RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, va `RELEASE_KEY_PASSWORD`.

## Quyen Rieng Tu

Ung dung khong xin runtime permission va khong luu du lieu nguoi dung. Ung dung khong doc noi dung man hinh, du lieu thanh toan NFC, the, tai khoan hay du lieu ung dung ngan hang. Ung dung chi nhan Vivo shortcut action va mo Google Wallet Activity da xac minh.

## Tuyen Bo Mien Tru

Day la du an doc lap, khong lien ket, duoc bao tro hay duoc ho tro boi Vivo, iQOO, Google hay Google Wallet. Tu chiu trach nhiem khi su dung va hay kiem tra truoc khi phu thuoc vao ung dung.

## Tac Gia Va Giay Phep

Tac gia: Đinh Quốc Bảo Khang.

Giay phep [MIT](LICENSE).
