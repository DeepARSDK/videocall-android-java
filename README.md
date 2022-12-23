# videocall-android-java

This example demonstrates how to use DeepAR SDK to add face filters and masks to your video call using [Agora](https://www.agora.io/) video calling SDK

To run the example

* Go to https://developer.deepar.ai, sign up, create the project and the Android app, copy the license key and paste it to MainActivity.java (instead of your_license_key_goes_here string)
* Download the SDK from https://developer.deepar.ai and copy the deepar.aar into videocall-android-java/deepar
* Sign up on [Agora](https://www.agora.io/).
  * Create a project and get the App ID.
  * Generate a temp RTC token. Get the token and the channel name associated with it.
* Open MainActivity.java.
  * Replace your_agora_app_id_here with your Agora App ID.
  * Replace your_agora_token_here with your temp RTC token.
  * Replace your_agora_channel_name_here with your channel name.
* Run the app on 2 devices and start the call on both of them. The remote views should appear on both devices.
