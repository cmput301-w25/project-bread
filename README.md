# CMPUT 301 W25 - Team Bread

## Team Members

| Name        | CCID   | GitHub Username |
| ----------- | ------ | --------------- |
| Armaan Katyal | katyal1 | @ArmaanKatyal   |
| Landys Lepine | landys | @lndslpn     |
| Animesh Mittal | animesh1 | @animeshm19|
| Maulik Srivastava | maulik2 | @mauliksamit |
| Kartik Samir Saxena | kartiksa | @karizzz     |
| Farhan Khan | fkhan3 | @khanfsk |

## Project Description

Sentio is a mood-sharing app designed to help users express and track their emotional well-being. At its core, it's a social platform where you can share recent mood events, follow friends or people nearby, and engage with their experiences through comments and interactions.

Whether you're looking to connect with others or simply reflect on your own journey, Sentio offers a flexible space for both. The app doubles as a personal mood journal, letting you privately log your moods without the need to interact socially.

Sentio also features a powerful analytics dashboard that highlights mood trends over time, offering meaningful insights that can help you better understand your emotional patterns and promote mental well-being.

Overall, Sentio provides a welcoming and intuitive environment for emotional expression, connection, and self-awareness.

## Key Features

- Post public and private events: Share mood events with optional image and location, either publicly or just for yourself.

- Follow user profiles: View recent mood events shared by people you follow.

- Mood analytics dashboard: Track your mood trends with overall distribution, monthly breakdowns, daily averages, and 7-day weighted averages.

- Engage with public events: Leave comments on public mood events of people you follow.

- Location-based discovery: Find random mood events shared within a 5km radius of your current location.

- User search and follow: Search for new users on the platform and follow them to see their activity.

- Event filtering: Filter mood events based on different criteria to explore relevant content.

## Setup Instructions

Before you can run the app make sure that you have `google-services.json` file in your `code/app` directory

Add `GOOGLE_MAPS_API_KEY` in your `local.properties`

(Both secrets are available in the team discord)

It is preferred that you use `Medium Phone API 35` with `Android 15 (VanillaIceCream)` to acheieve the best and most identical results, as all of our work
and testing is done on that specific emulator.

## Test Instructions

To run the tests make sure that you have `google-services.json` file in your `code/app` directory

Add `GOOGLE_MAPS_API_KEY` in your `local.properties`

Set `FIREBASE_PROJECT_ID` as `testdb-123` in your `local.properties` file, and gradle sync the project

Override `project_id` in `google-services.json` file as `testdb-123`

(Both the secrets are available in the team discord)

Before you can run the tests make sure that you have firebase emulators running. To start the emulator:
```
firebase emulators:start --project testdb-123
```
This will launch emulators for firestore and FirebaseAuth that are required to run Android UI tests

After all these steps are complete and the build is successful you can run the tests by going into `code` directory:

To run Android tests:
```
./gradlew connectedAndroidTest
```

To run Unit tests:
```
./gradlew test
```
If the above steps doesn't execute the tests, you can open the `code` directory in Android Studio and run the tests manually
to achieve the same result.

While testing we have found that UI tests can be flaky sometimes and might require re-runs.

### Extra Setup

To run the `MapFragmentTest` it is required that Sentio is selected as mock location app in developer settings.

Follow these steps to make sure that test runs:
1. Enable Developer options on your Android device/Emulator
2. Goto Settings -> Developer Options -> Select mock location app (pick Sentio) (Requires a app build to be present on device first)

OR

execute this command if you have adb installed:
```
adb shell appops set com.example.bread android:mock_location allow
```

After this setup process map ui tests should pass as expected and we have verified these steps to work.

## Documentation

- [Wiki](https://github.com/cmput301-w25/project-bread/wiki)
- [Project Board](https://github.com/orgs/cmput301-w25/projects/10)
- [UI Mockups](https://github.com/cmput301-w25/project-bread/wiki/Sentio-Mood-Sharing-App-UI)
- [UML](https://github.com/cmput301-w25/project-bread/wiki/UML-Diagram)

