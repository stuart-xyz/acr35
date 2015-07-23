# ACR35
Android + iOS ***Apache Cordova*** demo for the ACR35 NFC reader

## What does this do? / Who is this for?

This project contains a basic demo application that explains how to use the ACR35 reader to read the UID of NFC tags, using the cross-platform mobile development framework Apache Cordova. It uses the ACR35 SDK that was produced by Advanced Card Systems (ACS). In addition, on Android only, the application checks two connection indicators: device media volume (must be set to 100% for operation) and device audio socket plugged-in status.

***Current functionality:***

* Set reader to poll for nearby NFC tags **every second**
* Upon tag read, send its UID to the Cordova application and display in a toast
* Send reader into sleep mode (saves battery)
* Connection status checking (based on a timeout for command response)
* (Android only) Additional connection status checking based on device media volume and device audio socket plugged-in status

This project is aimed towards anyone who is interested in using the ACR35 reader with NFC-less Android and iOS devices, and would like to quickly get the SDK up and running. **In particular, this will save you going through the arduous process of writing a Cordova plugin that works with the reader**. The advantage of using a framework like Cordova is that native code can mostly be replaced by JavaScript, which works the same across most devices. This can greatly accelerate the process of developing an application for multiple mobile operating systems.

### What if I don't want to use Apache Cordova?

The native code written for the plugin can be easily transferred into a fully native application. Simply replace all usage of Cordova-specific code and paste into your project.

### Why does [ACR35 development](http://flomio.com/forums/forum/ask-the-flomies/) link to 'Flomio'?

Taken from the [Flomio forums](http://flomio.com/forums/topic/difference-between-acr35-and-flojack/):
> The FloJack (v2) and the ACR35 are the exact same hardware. They are both manufactured by Advanced Card Systems, a company specialized in smart card readers that we’ve been partners with for several years now.

> The FloJack v1 was developed internally at Flomio for a Kickstarter campaign in 2012. After dealing with issues supporting the many mobile operating systems we abandoned this project for the FloBLE (now production ready). In June of 2014 we were approached by Advanced Card Systems to carry their newest reader the ACR35. We agreed to rebrand it FloJack with the understanding that we would build out the software stack similar to what we offered with the FloJack v1. This included connect/disconnect management, audio routed alerts, NDEF parsing, read/write data blocks, authentication routines, etc. These features are crucial to allowing the reader to perform plug and play very much like an Android NFC enabled phone would.

Essentially, Flomio are very helpful in providing support for the ACR35 reader but, at least in name, they are not linked to the 'unbranded' **ACR35 reader**.

### What devices are compatible with the ACR35 reader?

Please see [this document](http://www.acs.com.hk/download-manual/6498/ACR3x-Supported-Mobile-Devices-1.05.pdf).

## Brief technical explanation

The host mobile device communicates with the ACR35 reader through the 3.5mm audio socket. Audio signals are sent through the input jack to be received and interpreted by the reader. APDU commands are sent to the reader, which perform different functions. To read the UID of a ***Mifare*** NFC tag, the following APDU command should be sent:
```
0xFF 0xCA 0x00 0x00 0x00
```

***NOTE: if you are not using Mifare NFC tags and this APDU command does not work for you, please refer to this [forum post](http://flomio.com/forums/topic/list-of-apdu-codes/) and the technical documentation in the [ACR35 SDK](http://www.acs.com.hk/download-driver-unified/6934/ACR3x-EVK-Android-1.00.zip) to find the correct APDU command. Then, edit `acr35/plugin/src/android/acr35.java` and `acr35/plugin/src/ios/UIDReader.m` to update the `apdu` variable. After this, remove the plugin and re-add it as per the usage example below.***

When the `read` function of the Cordova plugin is called, a new polling thread is started which sends out this APDU command every second. Within the native code, a callback is assigned for when a response to the command comes through from the reader. This response can then be processed, including conversion from a byte array to a string. Read the [ACR35 SDK](http://www.acs.com.hk/download-driver-unified/6934/ACR3x-EVK-Android-1.00.zip) documentation for details of the different possible response codes that are not UIDs.

## Dependencies

* Apache Cordova
    * `nl.x-services.plugins.toast 2.0.4` "Toast"
* **Android development:** Android SDK tools
* **iOS development:**
    * Latest version of Mac OS X
    * Xcode
    * Apple developer account (paid)

## Installation

1. `git clone https://github.com/stuart-xyz/acr35`
2. `cd acr35/me.stuartphillips.acr35`
3. `cordova prepare`
4. **iOS only:** In Xcode, open `platforms/ios/ACR35_Reader_Bootstrap.xcodeproj`
   <br> Ensure that `AudioToolbox.framework` is added to the project *build phases*

## Usage

To run the demo application on Android:
```
cordova run android
```

<br> To run the demo application on iOS:

1. `cordova prepare`
2. In Xcode, open `platforms/ios/ACR35_Reader_Bootstrap.xcodeproj`
3. Click *play* to run the application on the chosen device

<br> To remove / add the companion plugin:
```
cordova plugin rm me.stuartphillips.plugins.acr35
cordova plugin add ../plugin
```

## Plugin

### Methods

* `acr35.read`
* `acr35.sleep`

### Details

* `acr35.read(cardType, successCallback, failureCallback)`
  <br> Starts a polling thread where the reader sends out the APDU command for reading tag UIDs every second. In this loop, the connection status of the reader is automatically periodically checked
    * **cardType**: a string containing the card type to be read (global variable `cardType` is set to '143' - this is the default in the demo application provided by ACS). Card types provided in the [ACR35 SDK](http://www.acs.com.hk/download-driver-unified/6934/ACR3x-EVK-Android-1.00.zip) documentation:
      <br> ![Card types](https://raw.githubusercontent.com/stuart-xyz/acr35/master/card_types.png "Card types")
    * **successCallback(message)**: JavaScript function to be called when the reader sends feedback. Possible messages:
        * *A string containing the UID of a tag that has been read*
        * "disconnected": cannot communicate with the reader (3 second timeout has been reached with no response to read UID APDU command)
        * (Android only) "low_volume": the device media volume is less than 100%
        * (Android only) "unplugged": the reader is unplugged from the audio socket
        * (iOS only) "initialised": placeholder message that is sent when the reader has been initialised
    * **failureCallback()**: JavaScript function to be called when an internal application error occurs while polling
* `acr35.sleep(successCallback, failureCallback)`
  <br> Stops the polling thread and then sends the reader to sleep
    * **successCallback()**: called when the sleep method executed successfully
    * **failureCallback()**: called when an internal application error occurred

### Generate documentation

To generate documentation for Android and iOS code, execute `acr35/plugin/src/android/generate_docs.sh` or `acr35/plugin/src/ios/generate_docs.sh`. This will output html documentation to `docs`. Android source code has been documented with *JavaDoc* and iOS source code with *Doxygen*.

## Quirks

For the ACR35 reader to function correctly, it must be put to sleep, re-woken and reset again before starting the polling thread (this has been implemented in the native plugin code). In general, the reader can be unstable unless given sufficient time in between successive operations.

When sending the APDU command for reading a UID, the reader will respond with either `90 00` or another response starting with `90 00` when there is no nearby card. In the demo Cordova application, the JavaScript `successCallback` filters out responses like this so that processing only occurs when a card is actually nearby. Read the [ACR35 SDK](http://www.acs.com.hk/download-driver-unified/6934/ACR3x-EVK-Android-1.00.zip) documentation for details of the different possible response codes (that are not UIDs).

## TODO

* **iOS**: Add additional connection status checking based on device media volume and device audio socket plugged-in status
* Integrate more functionality from the ACR35 SDK into the Cordova plugin (more than just UID reading)

## Relevant Documentation

* [Apache Cordova](http://cordova.apache.org/docs/en/5.0.0/)
    * [Plugin development guide](http://cordova.apache.org/docs/en/5.0.0/guide_hybrid_plugins_index.md.html#Plugin%20Development%20Guide)
* ACR35 ([Android](http://www.acs.com.hk/download-driver-unified/6934/ACR3x-EVK-Android-1.00.zip), [iOS](http://www.acs.com.hk/download-driver-unified/6935/ACR3x-EVK-iOS-1.00.zip)) SDK (includes a demo application)
* [ACR35 development](http://flomio.com/forums/forum/ask-the-flomies/)
* [Android development](https://developer.android.com/guide/index.html)
* [iOS development](https://developer.apple.com/library/ios/navigation/)

## Glossary

* ***PICC:*** Proximity Inductive Coupling Card
  <br> A transponder that can be read or written by a proximity reader. Theses tags are based on the ISO14443 standard. Such tags do not have a power supply like a battery, but are powered by the electromagnetic field of the reader (PCD)
* ***APDU:*** Application Protocol Data Unit
  <br> The communication unit between a smart card reader and a smart card

## License

The MIT License (MIT)

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

## Third Party Licenses

### Advanced Card Systems ACR35 SDK
<http://www.acs.com.hk/>

Copyright (c) 2013-2015, Advanced Card Systems Ltd.
Copyright (c) 2011, CSE Division, EECS Department, University of Michigan.
All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted provided that the following conditions are met:

    Redistributions of source code must retain the above copyright notice, this
    list of conditions and the following disclaimer.

    Redistributions in binary form must reproduce the above copyright notice,
    this list of conditions and the following disclaimer in the documentation
    and/or other materials provided with the distribution.

    Neither the name of the University of Michigan nor the names of its
    contributors may be used to endorse or promote products derived from this
    software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (
INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

Android is a trademark of Google Inc.

### Apache Cordova Toast Plugin
<https://github.com/EddyVerbruggen/Toast-PhoneGap-Plugin>

The MIT License (MIT)

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
