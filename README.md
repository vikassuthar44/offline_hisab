# 📱 Offline Hisab

**Offline Hisab** is a modern Android app built with **Jetpack Compose** and **Kotlin**, designed to help users manage their Hisab, maintain records, and back up data securely to Google Drive using Google authentication.

---

## 🚀 Features
- 📊 Manage records locally (offline-first)  
- ☁️ Backup and restore data to Google Drive  
- 📅 Simple, clean, and modern UI using Jetpack Compose  

---

## 🛠️ Technologies Used
- **Kotlin**  
- **Jetpack Compose** for modern UI  
- **Google Drive API** (for backup/restore)  
- **Room Database** for local storage  

---

## ⚙️ Project Setup

### 1. Clone the repository
```bash
git clone https://github.com/<your-username>/OfflineHisab.git
cd OfflineHisab

### 2. After Cloning — Firebase & Google Drive Setup

Once you’ve cloned the repository, follow these steps to set up Firebase and Google Drive integration.

---

#### 🧩 Step 1: Create a Firebase Project
1. Go to [Firebase Console](https://console.firebase.google.com/).  
2. Click **Add project** → give it a name (for example, `OfflineHisab`).  
3. Once the project is created, open **Project Settings → General**.  
4. Click **Add App** → select **Android**.

---

#### 🧱 Step 2: Register your Android app
1. Enter your app’s package name (must match `applicationId` in `app/build.gradle`, for example:  
2. (Optional) Add an app nickname (e.g., “Offline Hisab Android”).  
3. (Recommended) Add your **SHA-1 fingerprint** for Google Sign-In and Drive API authentication:
```bash
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android


##### 🧩 Enable Google Drive API
1. Go to [Google Cloud Console](https://console.cloud.google.com/).  
2. Make sure you’re using the same project that’s linked to your Firebase app.  
3. In the left sidebar, go to  
   **APIs & Services → Library**.  
4. Search for **Google Drive API**.  
5. Click **Enable**.

