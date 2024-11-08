# Temi Wizard-of-Oz Testing

A Wizard-of-Oz testing application that enables remote-controlling the actions of the Temi robot. This application allows researchers and developers to simulate robot behaviors and interactions in real-time.

## Features

- Remote control of Temi robot via WebSocket
- Support for basic robot actions (speaking, movement, asking questions)
- Easy integration with Node-RED for visual programming
- Real-time feedback on robot actions
- Customizable interface loading

## Installation

### Prerequisites

- Android Studio
- ADB (Android Debug Bridge)
- Node-RED (optional, for visual programming)

### Setting up the Robot

1. Clone this repository and open it in Android Studio
2. Build the project to generate the APK
3. Connect to Temi on your local network:
```bash
adb connect <TEMI_IP_ADDRESS>:5555
```
4. Install the APK:
```bash
adb install PATH_TO_APK
```

## Development Guide

### 1. Enable Developer Mode on Temi

1. Go to Temi's Settings
2. Tap on the Temi icon in the top right corner 10 times to enable developer settings
3. Navigate to Settings > Developer Tools
4. Enable "ADB Debug" and "Developer Mode"

### 2. Connect to Temi via ADB

1. Make sure your computer and Temi are on the same network
2. Find Temi's IP address:
   - Go to Settings > Network & Internet
   - Select the connected WiFi network
   - Note down the IP address
3. Open terminal/command prompt and connect to Temi:
```bash
adb connect <TEMI_IP_ADDRESS>:5555
```
4. Verify connection:
```bash
adb devices
```
You should see your Temi device listed.

### 3. Build and Install the App

1. Open the project in Android Studio
2. Configure build settings:
   - Set minimum SDK version to match Temi's Android version
   - Ensure the application ID is unique
3. Build the APK:
   - Click Build > Build Bundle(s) / APK(s) > Build APK(s)
   - Or use Build > Generate Signed Bundle / APK for release versions
4. Install the APK on Temi:
```bash
adb install -r path/to/your/app.apk
```
   - Use `-r` flag to replace existing installation if needed

Note: If you encounter connection issues, try these steps:
- Ensure both devices are on the same network
- Try disconnecting and reconnecting ADB:
```bash
adb disconnect
adb connect <TEMI_IP_ADDRESS>:5555
```
- Restart ADB server if needed:
```bash
adb kill-server
adb start-server
```

## Usage

### WebSocket Connection

Connect to Temi using WebSocket at:
```
ws://YOUR_TEMI_IP_ADDRESS:8175
```

Upon successful connection, you'll receive: "Temi is ready to receive commands!"

### Command Structure

All commands are sent as JSON objects. Here are the supported robot commands:

#### Speaking Command
```json
{
  "command": "speak",
  "sentence": "The sentence you want Temi to say",
  "id": "unique_command_id"  
}
```
- Description: Makes Temi speak the given sentence
- Returns: `TTS_COMPLETED/your_sentence` when speech is finished

#### Ask Question Command
```json
{
  "command": "ask",
  "sentence": "The question you want Temi to ask",
  "id": "unique_command_id"  
}
```
- Description: Makes Temi ask a question and wait for user's response
- Returns: `ASR_COMPLETED/user_response` when user answers

#### Go to Location Command
```json
{
  "command": "goto",
  "location": "The exact name of the location as set in Temi",
  "id": "unique_command_id"  
}
```
// id Optional: for tracking command completion
- Description: Commands Temi to navigate to a pre-defined location
- Returns: Navigation status updates

#### Load Interface Command
```json
{
  "command": "interface",
  "url": "URL_TO_LOAD",
  "id": "unique_command_id" 
}
```
- Description: Loads a web interface on Temi's screen



### Using Node-RED

1. [Install Node-RED](https://nodered.org/docs/getting-started/local)
2. Import our custom nodes from [this JSON file](https://gist.github.com/shaunabanana/1c70946826b08cb46c49c8e8b105a726)
3. Configure the nodes with your Temi's IP address
4. Start building your control flows!

## Contributing

Feel free to submit issues and enhancement requests!

## License

MIT License

Copyright (c) 2024 CDI Lab

## Acknowledgments

This project was used in our CHI'21 paper "Patterns for Representing Knowledge Graphs to Communicate Situational Knowledge of Service Robots".
