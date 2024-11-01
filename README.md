...More to come!
## Developing

This will tell Temi to go to the location you specified. The string here must match the location name stored in Temi exactly for it to work.
```
}
  "location": "The exact name of the location as set in Temi"
  "command": "goto",
{
```json
#### Going to a location

This is similar to speaking, except Temi will start to recognize user speech saying the question. When the user finishes answering and the recognition is completed, you will get an message back in the form of `ASR_COMPLETED/The user's answer to the question`.
```
}
  "sentence": "The question you want Temi to ask"
  "command": "ask",
{
```json
#### Asking a question

Upon receiving the command, Temi will immediately echo the sentence back to your client, and begin to speak. You can use this to check if the sentences are in order. When Temi finishes speaking, another message will be sent to you in the format of `TTS_COMPLETED/The sentence you want Temi to say`. This will let you know when to proceed to the next action.
```
}
  "sentence": "The sentence you want Temi to say"
  "command": "speak",
{
```json
#### Speaking

Upon receiving the command, Temi will immediately echo the sentence back to your client, and begin to speak. You can use this to check if the sentences are in order. When Temi finishes speaking, another message will be sent to you in the format of `TTS_COMPLETED/The sentence you want Temi to say`. This will let you know when to proceed to the next action.
```
}
  "url": "URL"
  "command": "interface",
{
```json
#### Loading an interface URL

Start by connecting to `ws://YOUR_TEMI_IP_ADDRESS:8175`. The port can be changed by modifying the `int port = 8175;` line in the `onResume()` method in `MainActivity.java`. Upon connection, you should receive a message saying "Temi is ready to receive commands!". Then you can control Temi by sending commands in the format below:

You can also directly connect to the App and send control messages via WebSocket. All messages are in JSON format. 
### Using WebSocket

You might notice that there are more nodes than the three promised above. The nodes about knowledge graph and interfaces are used in the paper to control a custom-built KG interface. You can find the code for the interface [here](https://github.com/tongji-cdi/temi-kg-ui).

![A screenshot of the Node-RED interface](https://gist.githubusercontent.com/shaunabanana/1c70946826b08cb46c49c8e8b105a726/raw/a68029977d63b68806bb839ebe2e3f338be5e00f/screenshot.png)

The easiest way is to [install Node-RED](https://nodered.org/docs/getting-started/local), and use the nodes we developed for this purpose. A JSON file containing the nodes can be found [here](https://gist.github.com/shaunabanana/1c70946826b08cb46c49c8e8b105a726). Once you start Node-RED, import the JSON file and you should see something like this:
### Using Node-RED

There are two ways to control the robot: using pre-made Node-RED nodes and using WebSocket messages. Currently, we support controlling Temi to speak, ask questions, and go to pre-defined locations.
## Controlling the robot
        
```
adb install [option] PATH_OF_APK
adb connect <TEMI_IP_ADDRESS>:5555
```
In short, you should connect to Temi on your local network and push install the APK by running:

The code should be directly usable by importing it into Android Studio and compiling it into an APK. Then you can install the APK onto Temi by following the [official guide](https://github.com/robotemi/sdk/wiki/Installing-and-Uninstalling-temi-Applications).
## Running the code

A Wizard-of-Oz testing application that enables remote-controlling the actions of the Temi robot. Used in our CHI'21 paper "Patterns for Representing Knowledge Graphs to Communicate Situational Knowledge of Service Robots".
# Temi Wizard-of-Oz Testing

> this doc is out-of-date ,check the new api document
> **Warning**
