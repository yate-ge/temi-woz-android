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
- WebSocket Response: `{"command":"speak", "id":"unique_command_id", "status":"completed"}`

#### Ask Question Command
```json
{
  "command": "ask",
  "sentence": "The question you want Temi to ask",
  "id": "unique_command_id"  
}
```
- Description: Makes Temi ask a question and wait for user's response
- WebSocket Response: `{"command":"ask", "id":"unique_command_id", "response":"user's answer here"}`

#### Go to Location Command
```json
{
  "command": "goto",
  "location": "The exact name of the location as set in Temi",
  "id": "unique_command_id"  
}
```
- Description: Commands Temi to navigate to a pre-defined location
- WebSocket Response: 
  - Success: `{"command":"goto", "id":"unique_command_id", "status":"completed", "location":"kitchen"}`
  - Failure: `{"command":"goto", "id":"unique_command_id", "status":"failed", "error":"Path blocked"}`

#### Load Interface Command
```json
{
  "command": "interface",
  "url": "URL_TO_LOAD",
  "id": "unique_command_id" 
}
```
- Description: Loads a web interface on Temi's screen
- WebSocket Response: `{"command":"interface", "id":"unique_command_id", "status":"loaded"}`

#### Stop Command
```json
{
  "command": "stop",
  "id": "unique_command_id"
}
```
- Description: Stops any current movement or action
- Returns: `STOP_COMPLETED`

#### Turn By Command
```json
{
  "command": "turnBy",
  "angle": 90,
  "id": "unique_command_id"
}
```
- Description: Turns robot by specified angle (degrees, positive=right, negative=left)
- Returns: `TURN_COMPLETED` or `TURN_ABORTED/error_message`

#### Tilt By Command
```json
{
  "command": "tiltBy",
  "angle": 45,
  "id": "unique_command_id"
}
```
- Description: Tilts robot's head by specified angle (degrees, positive=up, negative=down)
- Returns: `TILT_COMPLETED` or `TILT_ABORTED/error_message`

#### Follow Command
```json
{
  "command": "follow",
  "action": "start",  // or "stop"
  "id": "unique_command_id"
}
```
- Description: Starts or stops following mode
- Returns: `FOLLOW_STARTED`, `FOLLOW_STOPPED`, or `FOLLOW_TARGET_LOST`

#### Get Locations Command
```json
{
  "command": "getLocations",
  "id": "unique_command_id"
}
```
- Description: Gets list of all saved locations
- Returns: `LOCATIONS_LIST/{"locations":["location1","location2",...]}`

#### Take Photo Command
```json
{
  "command": "takePhoto",
  "id": "unique_command_id"
}
```
- Description: Takes a photo using Temi's camera
- Returns: `PHOTO_TAKEN/base64_image_data` or `PHOTO_FAILED/error_message`

#### Start Video Stream Command
```json
{
  "command": "startVideo",
  "id": "unique_command_id"
}
```
- Description: Starts video streaming from Temi's camera
- Returns: `VIDEO_STARTED` followed by binary video frames, or `VIDEO_ERROR/error_message`

#### Stop Video Stream Command
```json
{
  "command": "stopVideo",
  "id": "unique_command_id"
}
```
- Description: Stops the video stream
- Returns: `VIDEO_STOPPED`

#### Get Battery Status Command
```json
{
  "command": "getBattery",
  "id": "unique_command_id"
}
```
- Description: Gets current battery status
- Returns: `BATTERY_STATUS/{"level":85,"is_charging":false}`

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
