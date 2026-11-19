# 🚀 Quy Trình Làm Việc Git & GitHub Dành Cho Đội Ngũ 2 Người (MoneyTracker)

> **Mục tiêu:** Giúp 2 lập trình viên làm việc song song hiệu quả, không dẫm chân lên nhau, **triệt tiêu 95% nguy cơ conflict code**, tự động chạy **Unit Test & Adaptation Test** trên GitHub Actions, và giữ cho nhánh `main` luôn ổn định.

---

## 📌 1. Mô Hình Phân Nhánh (GitHub Flow)

Đối với nhóm 2 người, mô hình **GitHub Flow (Feature Branch + Pull Request)** là phương pháp tối ưu nhất: nhẹ nhàng, không rườm rà nhưng vẫn đảm bảo tính an toàn và minh bạch tuyệt đối.

### Nguyên tắc bất di bất dịch:
1. **Nhánh `main` là "Thánh địa":** Luôn luôn build được, test pass, không ai được phép dùng `git push origin main` trực tiếp.
2. **Mọi thay đổi đều qua nhánh con (Feature Branch):** Mỗi tính năng hoặc sửa lỗi phải nằm trên một nhánh riêng biệt (`feature/`, `fix/`, `refactor/`).
3. **Mọi code vào `main` đều phải qua Pull Request (PR):** Người làm tính năng mở PR, người còn lại review và ấn Approve trước khi merge.
4. **CI Tự Động Kiểm Tra (Automated Testing):** Mỗi khi mở PR, GitHub Actions sẽ tự động chạy **Unit Tests** và **Device Adaptation Tests** trên máy ảo. Chỉ khi nào tất cả test đều xanh (PASS) thì mới được phép merge.

---

## 🏷️ 2. Quy Tắc Đặt Tên Nhánh (Branch Naming)

Tên nhánh phải rõ ràng, viết thường, ngăn cách bằng dấu gạch ngang `-`:

| Tiền tố | Mục đích | Ví dụ |
| :--- | :--- | :--- |
| `feature/` | Tính năng mới | `feature/subscription-hunter`, `feature/gamified-savings` |
| `fix/` | Sửa lỗi | `fix/dark-mode-crash`, `fix/currency-format` |
| `refactor/` | Cải tiến code / tối ưu | `refactor/modular-navigation`, `refactor/room-dao` |
| `test/` | Thêm / sửa test case | `test/adaptation-screen-sizes`, `test/coach-nudges` |

---

## 🛡️ 3. 5 Bí Quyết Tránh Conflict Đặc Thù Cho Android & Jetpack Compose

Phần lớn conflict trong dự án Android xảy ra ở một số file "điểm nóng". Áp dụng ngay 5 bí quyết sau:

### Bí quyết 1: Tách nhỏ file `strings.xml` theo tính năng
Trong Android, compiler sẽ **tự động gộp** mọi file XML trong thư mục `res/values/`. Đừng dồn tất cả text vào một file `strings.xml` duy nhất!
* Thay vì cả 2 cùng sửa `strings.xml`:
  * Dev A tạo: `res/values/strings_transactions.xml` & `res/values-vi/strings_transactions.xml`
  * Dev B tạo: `res/values/strings_chat.xml` & `res/values-vi/strings_chat.xml`
  * Các chuỗi dùng chung (Common): giữ ở `strings.xml`.
👉 **Kết quả:** Không bao giờ bị conflict chuỗi text ở cuối file.

### Bí quyết 2: Module hóa Navigation (`NavGraph.kt`)
Thay vì viết toàn bộ Composable screen vào một file `NavGraph.kt`, hãy tách từng màn hình thành các **Navigation Extension Function**:
```kotlin
// Trong ui/screens/transaction/TransactionNav.kt
fun NavGraphBuilder.transactionGraph(navController: NavController) {
    composable(Screen.Transactions.route) {
        TransactionsScreen(...)
    }
}
```
Trong `NavGraph.kt`, chỉ cần gọi:
```kotlin
NavHost(...) {
    dashboardGraph(navController)
    transactionGraph(navController)
    chatGraph(navController)
}
```
👉 Khi Dev A sửa màn hình Transaction, Dev B thêm màn hình Chat, cả 2 **không bao giờ đụng vào cùng một block code**.

### Bí quyết 3: Phân chia Task theo Chiều Dọc (Vertical Slice)
* **Tránh:** Dev A làm toàn bộ UI cho 3 màn hình, Dev B làm toàn bộ Repository/Database cho 3 màn hình đó (rất dễ dẫm chân lên nhau khi ghép nối).
* **Nên làm:** 
  * Dev A sở hữu trọn vẹn Feature A (Domain, Data, UI của Feature A).
  * Dev B sở hữu trọn vẹn Feature B (Domain, Data, UI của Feature B).

### Bí quyết 4: Phối hợp khi sửa file cấu hình chung (`libs.versions.toml`, `build.gradle.kts`)
* Khi cần thêm thư viện mới (dependency), hãy **nhắn tin cho bạn cùng nhóm trước**: *"Tao sắp add dependency Coil 2.6 vào libs.versions.toml nhé"*.
* Sau khi add xong, đẩy PR sớm để người kia cập nhật ngay.

### Bí quyết 5: Không commit file rác / file sinh tự động
* Đảm bảo `.gitignore` đã chặn: `.idea/`, `local.properties`, `build/`, `.gradle/`, `google-services.json`.
* Tuyệt đối không force-add các file cấu hình máy cá nhân.

---

## 🧪 4. Hệ Thống Automated Testing Trên GitHub (CI/CD)

Dự án đã được tích hợp quy trình kiểm thử tự động tại [.github/workflows/android-ci.yml](file:///.github/workflows/android-ci.yml):

### 1. Job 1: Unit Tests (JVM)
* **Thời gian chạy:** ~2 - 3 phút (rất nhanh nhờ Gradle dependency cache).
* **Kích hoạt:** Tự động chạy mỗi khi có commit `push` hoặc tạo `pull_request` vào nhánh `main`.
* **Phạm vi kiểm tra:** Chạy toàn bộ 13+ test suites trong `app/src/test/` (Financial DNA, Anomaly Detective, AI Coach, Budget, Two-Way Sync, Recurring Transactions, v.v.).
* **Báo cáo:** Tự động tạo bảng tổng kết kết quả test (Test Summary) ngay trong giao diện PR của GitHub.

### 2. Job 2: Device Adaptation Tests (Android Emulator)
* **Thời gian chạy:** ~8 - 15 phút.
* **Kích hoạt:** Tự động chạy khi mở `pull_request` vào `main`, hoặc bấm thủ công qua nút **Run workflow**.
* **Phạm vi kiểm tra:**
  * **Dark & Light Mode Adaptation:** Kiểm tra UI Compose hiển thị đúng màu nền, surface, chữ khi đổi Dark/Light theme.
  * **Screen Size Adaptation:** Kiểm tra màn hình nhỏ (Compact 320dp) và màn hình lớn/tablet (Expanded 720dp) không bị tràn viền hay crash.
  * **Accessibility Font Scale:** Kiểm tra layout khi người dùng tăng font chữ hệ thống lên 1.5x.
  * **Locale / Language Adaptation:** Kiểm tra chuỗi dịch tiếng Việt (`vi`) và tiếng Anh (`en`) nạp chính xác.

### Lệnh chạy kiểm tra trên máy trước khi đẩy code:
```bash
# Chạy Unit Tests trên máy
./gradlew testDebugUnitTest

# Chạy Adaptation Tests trên máy ảo Android Studio
./gradlew connectedDebugAndroidTest
```

---

## ⚡ 5. Quy Trình Làm Việc Hàng Ngày (Daily Git Cheatsheet)

Dưới đây là chu trình chuẩn từ lúc bắt đầu làm việc đến khi code lên `main`:

### Bước 1: Trước khi bắt tay vào làm việc mới
Luôn luôn lấy code mới nhất từ `main` về máy:
```bash
git checkout main
git pull origin main
```

### Bước 2: Tạo nhánh mới cho tính năng
```bash
git checkout -b feature/gamified-savings
```

### Bước 3: Code và Commit thường xuyên
* Commit theo từng bước logic nhỏ, viết commit message rõ ràng (chuẩn Conventional Commits):
```bash
git add .
git commit -m "feat(savings): add SavingsGoal data model and Room entity"
```
* **Mẹo tiền tố commit:** `feat:` (tính năng), `fix:` (sửa lỗi), `refactor:` (tái cấu trúc), `test:` (viết test).

### Bước 4: Đồng bộ code từ `main` trước khi tạo Pull Request
Trước khi mở PR, hãy cập nhật nhánh của mình:
```bash
# 1. Fetch dữ liệu mới nhất từ remote
git fetch origin

# 2. Rebase nhánh của bạn lên trên đầu main mới nhất
git rebase origin/main
```

### Bước 5: Đẩy nhánh lên GitHub
```bash
git push -u origin feature/gamified-savings
```

### Bước 6: Tạo Pull Request (PR) trên GitHub
1. Vào repository trên GitHub -> Click **Compare & pull request**.
2. Điền thông tin theo template có sẵn: Tóm tắt thay đổi, checklist, ảnh chụp màn hình UI.
3. Chờ GitHub Actions chạy kiểm tra Unit Test & Adaptation Test (hiện dấu tích xanh ✅).
4. Gán bạn của bạn vào mục **Reviewers**.
5. Sau khi bạn của bạn bấm **Approve** -> Nhấn **Squash and merge** để gộp vào `main`.

### Bước 7: Dọn dẹp sau khi merge
```bash
git checkout main
git pull origin main
git branch -d feature/gamified-savings
```

---

## 🛠️ 6. Cẩm Nang Xử Lý Khi Bị Conflict (Conflict Resolution)

Đừng hoảng sợ khi thấy thông báo `CONFLICT (content): Merge conflict in ...`.

### Giải quyết bằng công cụ trực quan của Android Studio:
1. Khi Git báo conflict trong quá trình `git rebase origin/main`:
2. Mở Android Studio -> Menu góc trên: **Git** -> **Resolve Conflicts...**
3. Android Studio sẽ mở giao diện so sánh 3 cột (**3-Way Merge Tool**):
   - **Cột Trái (Yours):** Code hiện tại của bạn.
   - **Cột Phải (Theirs):** Code từ nhánh `main` mới nhất của bạn mình.
   - **Cột Giữa (Result):** Kết quả cuối cùng sau khi gộp.
4. Click dấu mũi tên `>>` hoặc `<<` để nhận đoạn code mong muốn, hoặc chỉnh sửa trực tiếp ở cột giữa.
5. Sau khi giải quyết xong tất cả các file -> Click **Apply**.
6. Mở Terminal và gõ:
   ```bash
   git add .
   git rebase --continue
   ```
*(Nếu muốn huỷ thao tác quay về lúc trước khi rebase an toàn: `git rebase --abort`).*

---

## 🔒 7. Thiết Lập Bảo Vệ Nhánh Trên GitHub (Branch Protection)

1. Vào GitHub repo: `https://github.com/phuczs/POSE-MoneyManager` -> Tab **Settings** -> Mục **Branches**.
2. Bấm **Add branch protection rule** -> Điền Branch name pattern: `main`.
3. Tích chọn:
   - ✅ **Require a pull request before merging**
   - ✅ **Require approvals** (chọn `1`)
   - ✅ **Require status checks to pass before merging** -> Tìm và chọn check: `Run Unit Tests`.
4. Nhấn **Save changes**. Từ lúc này, nhánh `main` được bảo vệ an toàn 100%!

