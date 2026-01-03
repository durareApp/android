<div align="center">
   
   <img alt="Feature graphic" src="https://github.com/user-attachments/assets/824e2c6c-aa5f-476d-9f7d-9a5bc60103f9" />

   <h1>
     <img src="https://github.com/user-attachments/assets/13284f01-93cf-4524-a9f1-76cc79afab3e" alt="App Icon" width="30" style="vertical-align:middle; margin-right:10px;">
     Durare - Ai Pushup Counter
   </h1>

  <h4>An Android application that uses face detection to count push-ups in real-time. The app detects your face position through the front camera and counts push-ups based on how close your face is to the camera.</h4>

  <a href="https://www.producthunt.com/products/durare-ai-push-up-counter?embed=true&amp;utm_source=badge-featured&amp;utm_medium=badge&amp;utm_campaign=badge-durare-ai-push-up-counter" 
     target="_blank" rel="noopener noreferrer">
     <img 
        alt="Durare - Ai Push Up Counter - AI-powered push-up counter &amp; fitness tracker | Product Hunt" 
        width="250" 
        height="54" 
        src="https://api.producthunt.com/widgets/embed-image/v1/featured.svg?post_id=1052541&amp;theme=light&amp;t=1767027018675"
      >
  </a>
  
  <p>
     <img alt="Static Badge" src="https://img.shields.io/badge/1.0.0-0?style=for-the-badge&logo=android&label=Version&labelColor=%2310140e&color=%233A761D">
     <img alt="Static Badge" src="https://img.shields.io/badge/1.0.0-0?style=for-the-badge&logo=github&label=Version&labelColor=%2310140e&color=%233A761D">
  </p>
  
  <img src="https://ziadoua.github.io/m3-Markdown-Badges/badges/Android/android3.svg">
  <img src="https://ziadoua.github.io/m3-Markdown-Badges/badges/Firebase/firebase3.svg">
  <img src="https://ziadoua.github.io/m3-Markdown-Badges/badges/Kotlin/kotlin3.svg">
  <img src="https://ziadoua.github.io/m3-Markdown-Badges/badges/LicenceGPLv3/licencegplv33.svg">
  <img src="https://m3-markdown-badges.vercel.app/stars/2/3/subhajit-rajak/durare">
  
</div>

## Download

<p align="left">
   <a href="https://play.google.com/store/apps/details?id=com.subhajitrajak.durare">
      <img 
         alt="Google Play" 
         src="https://github.com/user-attachments/assets/672a8eaa-e089-47fa-b097-685787aeeb23" 
         width="250" /> 
   </a>
   <a href="https://github.com/subhajit-rajak/durare/releases">
      <img 
         alt="Github" 
         src="https://github.com/user-attachments/assets/33a17f11-9ff0-4ed2-9ef1-0c168fdbe063" 
         width="250" /> 
   </a>
</p>


## Features

- **Real-time face detection** using ML Kit
- **Automatic push-up counting** based on face position
- **High sensitivity detection** for easy testing
- **Visual feedback** with face overlay and position indicators
- **Live counter display** showing total push-ups completed

## How It Works

The app uses the following logic to detect push-ups:

1. **Down Position**: When your face takes more than 50% of the screen (face close to camera)
2. **Up Position**: When your face takes more than 25% of the screen (face at medium distance)
3. **Push-up Count**: A complete push-up is counted when you transition from down to up position

## Usage Instructions

1. **Grant Camera Permission**: Allow the app to access your front camera when prompted
2. **Grant Notifcation Permission**: Allow the app to prompt a notification for the rest timer
3. **Position Yourself**: Stand in front of your device with the front camera facing you
4. **Start Push-ups**: 
   - Move your face close to the camera (down position)
   - Push up to move your face further from the camera (up position)
   - Repeat to count more push-ups

## Technical Details

### Dependencies
- **ML Kit Face Detection**: For real-time face detection
- **CameraX**: For camera preview and image analysis
- **AndroidX**: For modern Android development

### Sensitivity Settings
- **Down Threshold**: 50% (face takes more than 50% of screen)
- **Up Threshold**: 25% (face takes more than 25% of screen)
- **Frame Threshold**: 3 consecutive frames required to confirm position
- **Min Face Size**: 0.1f (detects very small faces for high sensitivity)

**Note**: You can customize the `Down` and `Up` Threshold in `settings/personalize`

## Building and Running

1. Open the project in Android Studio
2. Sync Gradle files to download dependencies
3. Connect your project to firebase and add the `google_services.json` file
4. Enable `Authentication`, `Firestore Database` and `Ai Logic` in Firebase and edit the `default_web_client_id` in `strings.xml` in `app/src/main/res/values` folder
5. Connect an Android device or start an emulator
6. Build and run the app
7. Grant camera permissions when prompted

## Troubleshooting

- **No face detected**: Make sure you're in a well-lit area and your face is clearly visible
- **Inconsistent counting**: Try adjusting your distance from the camera
- **App crashes**: Ensure you have granted camera permissions
- **Poor detection**: Clean your camera lens and ensure good lighting

## License

This project is open source and available under the GPL-3.0 license.

## Screenshots

<p align="center">
   <img alt="a" src="https://github.com/user-attachments/assets/668e631c-9e3e-4199-a060-df93df8706fb" width="24%%"/>
   <img alt="b" src="https://github.com/user-attachments/assets/cafea59d-132c-4a89-892d-0c09b78eb17a" width="24%%"/>
   <img alt="c" src="https://github.com/user-attachments/assets/d463e523-75e7-459f-8ae6-ab42d7e19427" width="24%%"/>
   <img alt="d" src="https://github.com/user-attachments/assets/bb8a83a8-dee9-4c3c-8ab9-093b8fbddf1b" width="24%%"/>
</p>

<p align="center">
   <img alt="e" src="https://github.com/user-attachments/assets/72a1c230-22f6-4d7c-8db2-f91280b376b2" width="24%%"/>
   <img alt="f" src="https://github.com/user-attachments/assets/d294b55e-70fc-4d25-b147-1e518ffa1aec" width="24%%"/>
   <img alt="g" src="https://github.com/user-attachments/assets/78cbc23f-fa34-4c6b-83c2-70909327afd8" width="24%%"/>
   <img alt="h" src="https://github.com/user-attachments/assets/3fd7c2b4-489a-4e53-84d0-87b83653fa44" width="24%%"/>
</p>

## Share stats to social media stories

<p align="center">
  <img src="https://github.com/user-attachments/assets/ccab1d52-34d5-4978-9bd9-4e3783e6a9e5" width="24%%">
  <img src="https://github.com/user-attachments/assets/2dcbd196-19df-4723-8aea-ff08b10999ea" width="24%%">
  <img src="https://github.com/user-attachments/assets/ea83be08-9728-4596-914a-94e28d8dcfa7" width="24%%">
  <img src="https://github.com/user-attachments/assets/4dbf3329-12a6-418e-b319-178a9f525c6f" width="24%%">
</p>

