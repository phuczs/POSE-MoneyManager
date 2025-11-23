<<<<<<< HEAD
# MoneyManager

A modern Android application for personal finance management built with Jetpack Compose and Firebase.

## 📱 Features

- **User Authentication** - Secure login/register with Firebase Auth
  - Email/Password authentication
  - Google Sign-In authentication
- **Transaction Management** - Add, edit, delete income and expense transactions
- **Category Management** - Organize transactions with custom categories
- **Balance Tracking** - Real-time balance calculation and display
- **Data Visualization** - Income vs expense summaries
- **Cloud Sync** - All data backed up to Firebase Firestore

## 🏗️ Architecture

This app follows **Clean Architecture** principles with MVVM pattern:

- **Presentation Layer**: Jetpack Compose UI + ViewModels
- **Domain Layer**: Use cases and business logic
- **Data Layer**: Repositories, Firebase integration

### Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architecture**: MVVM + Clean Architecture
- **Dependency Injection**: Hilt
- **Backend**: Firebase (Auth + Firestore)
- **Build Tool**: Gradle (Kotlin DSL)

## 🚀 Getting Started

### Prerequisites

- Android Studio Hedgehog | 2023.1.1 or newer
- JDK 11
- Android SDK API 36
- Minimum SDK: API 24 (Android 7.0)

### Setup

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd MoneyManager
   ```

2. **Firebase Setup**
   - Create a new Firebase project at [Firebase Console](https://console.firebase.google.com)
   - Enable Authentication (Email/Password and Google)
   - Enable Firestore Database
   - Download `google-services.json` and place it in `app/` directory
   - **For Google Sign-In**: Follow [Google Sign-In Setup Guide](GOOGLE_SIGNIN_SETUP.md)

3. **Build and Run**
   ```bash
   ./gradlew assembleDebug
   ./gradlew installDebug
   ```

## 📁 Project Structure

```
app/src/main/java/com/example/moneymanager/
├── data/
│   ├── model/          # Data classes (User, Transaction, Category)
│   └── repository/     # Repository implementations
├── di/                 # Dependency injection modules
├── ui/
│   ├── navigation/     # Navigation setup
│   ├── screens/        # Compose screens
│   ├── theme/          # App theming
│   └── viewmodel/      # ViewModels
├── MainActivity.kt
└── MoneyManagerApp.kt
```

## 🔥 Firebase Configuration

### Required Firestore Indexes

The app requires these composite indexes in Firestore:

1. **Transactions by User and Type**
   - Collection: `transactions`
   - Fields: `userId` (Ascending), `type` (Ascending), `date` (Descending)

2. **Transactions by User and Date Range**
   - Collection: `transactions` 
   - Fields: `userId` (Ascending), `date` (Ascending)

3. **Categories by User and Type**
   - Collection: `categories`
   - Fields: `userId` (Ascending), `type` (Ascending)

### Firestore Security Rules

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Users can only access their own transactions
    match /transactions/{transactionId} {
      allow read, write: if request.auth != null && request.auth.uid == resource.data.userId;
      allow create: if request.auth != null && request.auth.uid == request.resource.data.userId;
    }
    
    // Users can only access their own categories
    match /categories/{categoryId} {
      allow read, write: if request.auth != null && request.auth.uid == resource.data.userId;
      allow create: if request.auth != null && request.auth.uid == request.resource.data.userId;
    }
  }
}
```

## 🧪 Testing

```bash
# Run unit tests
./gradlew test

# Run instrumentation tests  
./gradlew connectedAndroidTest
```

## 📦 Build Variants

- **Debug**: Development build with debug symbols
- **Release**: Production build with ProGuard/R8 optimization

## 🔧 Development

### Adding New Features

1. Create feature branch: `git checkout -b feature/new-feature`
2. Implement changes following Clean Architecture
3. Add tests for new functionality
4. Update documentation
5. Create pull request

### Code Style

- Follow Kotlin coding conventions
- Use meaningful names for classes and functions
- Add KDoc comments for public APIs
- Keep functions small and focused

## 📋 Known Issues

- Limited offline support (Firebase dependent)
- Requires active internet connection for full functionality

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🆘 Support

If you encounter any issues:

1. Check existing [GitHub Issues](../../issues)
2. Create a new issue with detailed description
3. Include logs and steps to reproduce

## 📱 Screenshots

*Add screenshots of your app here*

## 🔄 Version History

- **v1.0.0** - Initial release
  - Basic transaction management
  - Category system
  - Firebase integration
  - Balance calculation

---

**Built with ❤️ using Jetpack Compose and Firebase**
=======
# POSE-MoneyManager
>>>>>>> c31cd1fab3c60390854d51ade04cccda3f774d09
