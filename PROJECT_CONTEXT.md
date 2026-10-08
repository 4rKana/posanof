# Project Context: Aplikasi Kasir (POS) Android UMKM Berorientasi Paperless

- **Role**: Senior Android Developer
- **Project**: Aplikasi Kasir (POS) Android UMKM Berorientasi Paperless
- **Architecture**: MVVM (Model-View-ViewModel) + Offline-First Architecture

## Tech Stack
- **UI**: Jetpack Compose
- **Language**: Kotlin + Coroutines & StateFlow
- **Local Database**: Room Database (SQLite)
- **Barcode Scanner**: Google ML Kit Vision + CameraX
- **Security**: AES Encryption & EncryptedSharedPreferences (PIN Auth)
- **Export & Storage**: Apache POI (Excel), PDF Library, Storage Access Framework (SAF)

## Database Tables (Snake Case)
1. **product**
   - `id`
   - `name`
   - `price`
   - `stock`
   - `barcode`
   - `min_stock`
   - `created_at`

2. **transaction**
   - `id`
   - `date`
   - `total_amount`
   - `payment_method`
   - `cash_amount`
   - `change_amount`
   - `cashier_name`

3. **transaction_detail**
   - `id`
   - `transaction_id`
   - `product_id`
   - `quantity`
   - `price_at_purchase`
   - `sub_total`

4. **credit**
   - `id`
   - `transaction_id`
   - `customer_name`
   - `customer_phone`
   - `remaining_debt`
   - `status`
   - `created_at`
