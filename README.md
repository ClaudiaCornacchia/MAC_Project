
# Boxly - Box Management



## Overview
Boxly is an inventory management solution that removes the stress of moving and storage handling by creating a digital twin for every physical box. 
Users can catalog box contents using text, speech recognition, or photos, and track each box’s location.   
For every box, Boxly generates a unique QR code via a custom API service, which can be printed and attached to the physical box. Users can search for a specific item within the app to instantly identify which box it is stored in, or scan the QR code on the physical box to view its contents directly on their device.
The app also supports real-time sharing, enabling families to collaborate.


## Key Features

* **Authentication:** sign-up and sign-in using **Firebase Authentication** with Email/Password and Google Sign-In options.
### Box Management
* **Detailed cataloging:** add boxes with a title, description and other details which are stored in **Firebase Firestore**.
* Integrated **Speech-to-Text:** allows users to dictate box contents quickly, with visual 2D graphic waveforms representing audio input levels.
* **Photos:** capture and upload a photo of the box contents directly to **Firebase Storage**.
* **Fill status:** visual 2D graphic indicators (Green/Yellow/Red) to represent if a box is Empty, Half-Full, or Full.
* **Search functionality:** to find boxes by title or description keywords.
* **Edit and Delete:** modify box details or remove boxes entirely.
* **Unused Box :** automatically filters boxes that haven't been accessed in over 12 months.
* **Fragile alerts:** Haptic feedback (vibration) triggers when interacting with boxes marked as "Fragile."

### QR Code Integration
* **Generation:** custom QR codes are generated via a Node.js REST API (hosted on **Google Cloud Run** with Docker).
* **Scanning:** built-in scanner using **Google ML Kit** instantly decodes Box IDs and retrieves data from Firestore.

### Location
* Save where a box is stored using the FusedLocationProvider.
* **Interactive maps:** view box locations on **Google Maps**.
* **Smart suggestions:** uses **Google Places API** to validate and suggest addresses during box creation.
* **Ambient light sensor:** the app detects low-light environments using the device's light sensor and automatically suggests enabling the flashlight.

### Collaborative
* **Real-time sharing:** Share boxes with other users via email. Shared boxes appear instantly in the boxes screen. When a box is updated by any user, changes reflect in real-time for all collaborators.




## Requirements Implemented


| Requirement | Implementation in Boxly |
| :--- | :--- |
| **Public Cloud Services** | **Firebase** (Firestore, Auth, Storage), **Google Cloud Run** (hosting the Docker container) and **Google Places API**, **Google Maps SDK** |
| **Authentication** | **Firebase Auth** supports Email/Password and Federated Identity (Google Sign-In). |
| **2D Graphics** | Custom drawn UI elements for **Fill Status** (colored indicators) and visual 2D graphic **waveforms** representing audio input levels. |
| **Sensors** | **Light Sensor** (to detect ambient light levels), **Microphone** (Speech-to-Text). |
| **GPS** | **Google Maps SDK** and **FusedLocationProvider** for saving and viewing box coordinates. |
| **Camera** | **CameraX** for taking photos and **Google ML Kit** for scanning QR codes. |
| **Concurrency** | Use of **Kotlin Coroutines** and **Flow** to handle background tasks without blocking the UI. |
| **REST API (Remote Server)** | A custom **Node.js** server wrapped in **Docker**, hosted on **Google Cloud Run**, handles QR Code generation. |

---


## 📂 Project Structure

```text
app/src/main/java/com/example/mobile_app/
│
├── data/                          # [DATA LAYER] 
│   ├── remote/                    # Remote data sources
│   │   ├── QrApiService.kt        # Retrofit API for QR generation
│   │   └── QrModels.kt            # Data models for QR API
│   │
│   └── repository/                
│       ├── AccountRepository.kt   # Auth & account-related data
│       ├── LocationRepository.kt  # GPS & location data handling
│       ├── StorageRepository.kt   # Box related data operations
│       └── SpeechService.kt       # Speech-to-text 
│
├── di/                            
│   ├── AppModule.kt               
│   ├── FirebaseModule.kt          # Firebase configuration
│   └── NetworkModule.kt           # Retrofit / network configuration
│
├── domain/
│   └── model/                     # [DOMAIN LAYER] 
│       ├── Box.kt                 # Domain model for a Box
│       └── User.kt                # Domain model for a User
│
├── presentation/                  # [PRESENTATION LAYER] UI & ViewModels
│   │
│   ├── authentication/            # Authentication flow
│   │   ├── sign_in/
│   │   │   ├── SignInScreen.kt
│   │   │   └── SignInViewModel.kt
│   │   └── sign_up/
│   │       ├── SignUpScreen.kt
│   │       ├── SignUpViewModel.kt
│   │       ├── CredentialExt.kt   # Credential helpers
│   │       └── ValidationsExt.kt  # Input validation logic
│   │
│   ├── account_center/            # Account management
│   │   ├── AccountCenterScreen.kt
│   │   └── AccountCenterViewModel.kt
│   │
│   ├── box/                       # Box-related features
│   │   ├── boxes/                 # All boxes listing
│   │   │   ├── BoxesScreen.kt
│   │   │   └── BoxesViewModel.kt
│   │   ├── box_detail/           # Individual box details
│   │   │   ├── BoxDetailScreen.kt
│   │   │   └── BoxDetailViewModel.kt
│   │   └── new_box/               # Box creation flow
│   │       ├── NewBoxScreen.kt
│   │       └── NewBoxViewModel.kt
│   │
│   ├── scan_qr/                   # QR scanning feature
│   │   ├── ScanQrScreen.kt
│   │   ├── ScanQrViewModel.kt
│   │   └── QrCodeAnalyzer.kt      # CameraX QR analyzer
│   │
│   ├── splash/                    # App startup logic
│   │   ├── SplashScreen.kt
│   │   └── SplashViewModel.kt
│   │
│   ├── BoxActivity.kt             # Single-activity entry point
│   ├── BoxApp.kt                  # Navigation graph & root composable
│   ├── BoxAppState.kt             # Global UI state holder
│   ├── BoxAppViewModel.kt         # App-level ViewModel
│   └── SnackbarManager.kt         # Centralized snackbar handling
│
├── ui/theme/                      # UI theme (Color, Typography, Shapes)
│
├── BoxRoutes.kt                   # Navigation constants
└── BoxHiltApp.kt                  # Application class (Hilt setup)
```


## How to Run

### Prerequisites
1.  Android Studio 
3.  A Firebase Project with Auth, Firestore, and Storage enabled
4.  A Google Cloud Project with Maps SDK and Places SDK enabled
5.  Docker Desktop (optional, for running the local server)

### Setup Steps (Android App)

1.  Clone the Repository

2.  Firebase Configuration
    * Download the `google-services.json` file from your Firebase Console.
    * Place it in the `app/` directory of the project.

3.  API Keys Configuration
    * To keep sensitive keys secure, this project uses `local.properties`.
    * Open `local.properties` in the root directory and add your Google Maps API Key. Note: The `build.gradle` file is configured to inject this key into the Android Manifest automatically.

4.  Run the App
    * Open the project in Android Studio and sync Gradle files.
    * Run the app on an emulator or physical device.

### Server Setup (QR Code Generation)

The Android app connects to a REST API for generating QR codes. You can run this server locally using Docker.

1.  Navigate to the Server Directory
    ```bash
    cd Server-api
    ```

2.  Install Dependencies
    ```bash
    npm install express qrcode firebase-admin
    ```

3.  Build the Docker Image
    Run the following command to create the image named `qr-server`:
    ```bash
    docker build -t qr-server .
    ```

4.  Run the Container
    Start the server on port 3000:
    ```bash
    docker run -p 3000:3000 qr-server
    ```

5.  Connect Android Emulator
    * Update the API BASE_URL in your Android project (in `NetworkModule.kt`) to point to the local emulator address:`http://10.0.2.2:3000` (If using emulator) or use your local IP address if you are using a physical device
