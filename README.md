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

[Brief description of the project in your own words]

## Key Features

- [Feature 1]: Brief description
- [Feature 2]: Brief description
- [Feature 3]: Brief description

## Setup Instructions

1. [Step 1]
2. [Step 2]
3. [Step 3]

## Test Instructions

To run the tests make sure that you have `google-services.json` file in your `code/app` directory

Add `GOOGLE_MAPS_API_KEY` in your `local.properties`

Set `FIREBASE_PROJECT_ID` as `bread-2259c` in your `local.properties` file

(Both the secrets are available in the team discord)

Before you can run the tests make sure that you have firebase emulators running. To start the emulator:
```
firebase emulators:start
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

## Documentation

- https://github.com/cmput301-w25/project-bread/wiki
- https://github.com/orgs/cmput301-w25/projects/10
- https://github.com/cmput301-w25/project-bread/wiki/Sentio-Mood-Sharing-App-UI
- https://github.com/cmput301-w25/project-bread/wiki/UML-Diagram
