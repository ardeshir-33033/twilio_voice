# twilio_voice

Provides an interface to Twilio's Programmable Voice SDK to allow voice-over-IP (VoIP) calling into
your Flutter applications.
This plugin was taken from the original flutter_twilio_voice, as it seems that plugin is no longer
maintained, this one is.

#### Live Example/Samples:
- [Twilio Voice Web](https://twilio-voice-web.web.app/#/)

## Features

- Receive and place calls from iOS devices, uses callkit to receive calls.
- Receive and place calls from Android devices, uses ~~custom UI~~ Native call screen to receive calls.
- Receive and place calls from Web (FCM push notification integration not yet supported by Twilio Voice Web, see [here](https://github.com/twilio/twilio-voice.js/pull/159#issuecomment-1551553299) for discussion)
- Receive and place calls from MacOS devices, uses custom UI to receive calls (in future & macOS
  13.0+, we'll be using CallKit).

## Feature addition schedule:
- Audio device selection support (select input/output audio devices, on-hold)
- Update plugin to Flutter federated packages (step 1 of 2 with Web support merge)
- Desktop platform support (implementation as JS wrapper/native implementation, Windows/Linux to start development)

Got a feature you want to add, suggest? File a feature request or PR.

### Android Limitations

~~As iOS has CallKit, an Apple provided UI for answering calls, there is no default UI for android to
receive calls, for this reason a default UI was made. To increase customization, the UI will use a
splash_icon.png registered on your res/drawable folder. I haven't found a way to customize colors,
if you find one, please submit a pull request.~~

Android provides a native UI by way of the `ConnectionService`. Twilio has made an attempt at this implementation
however it is fully realized in this package.

### macOS Limitations

1. CallKit support is found in macOS 13.0+ which there is no support for yet. In future, this will
   be taken into consideration for feature development.
2. Twilio Voice does not offer a native SDK for macOS, so we're using the Twilio Voice Web SDK (
   twilio-voice.js, v2.4.1-dev) to provide the functionality. This is a temporary solution until (or
   even if) Twilio Voice SDK for macOS is released.

This limits macOS to not support remote push notifications `.voip` and `.apns` as the web SDK does
not support this. Instead, it uses a web socket connection to listen for incoming calls, arguably
more efficient vs time but forces the app to be open at all times to receive incoming calls.

### Setup

Please follow Twilio's quickstart setup for each platform, you don't need to write the native code
but it will help you understand the basic functionality of setting up your server, registering your
iOS app for VOIP, etc.

### iOS Setup

To customize the icon displayed on a CallKit call, Open XCode and add a png icon named '
callkit_icon' to your assets.xassets folder

see [[Notes]](https://github.com/diegogarciar/twilio_voice/blob/master/NOTES.md#ios--macos) for more information

### macOS Setup

Drop in addition.

see [[Limitations]](https://github.com/diegogarciar/twilio_voice/blob/master/NOTES.md#macos) and [[Notes]](https://github.com/diegogarciar/twilio_voice/blob/master/NOTES.md#ios--macos) for more information.

### Android Setup:

register in your `AndroidManifest.xml` the service in charge of displaying incoming call
notifications:

``` xml
<Application>
 .....
 <service
 android:name="com.twilio.twilio_voice.fcm.VoiceFirebaseMessagingService"
 android:stopWithTask="false">
<intent-filter> <action android:name="com.google.firebase.MESSAGING_EVENT" />
</intent-filter> </service>
```

With the implementation of runtime permissions, Android has made several changes over the SDK versions. Since Android 13, to allow showing notifications, the permissions `android.permission.POST_NOTIFICATIONS` has been added and integrated into the plugin.  This is required to show notifications on Android 13+ devices. Read more about it [here](https://developer.android.com/develop/ui/views/notifications/notification-permission)

There are a few (additional) permissions added to use the `ConnectionService`, several permissions are required to enable this functionality (see example app). There permissions are:
- `android.permission.BIND_TELECOM_CONNECTION_SERVICE`: required to bind to the `ConnectionService`
- `android.permission.READ_PHONE_STATE`: required to read the phone state (to determine if a call is active)
- `android.permission.CALL_PHONE`: required to access the ConnectionService and make/receive calls

Further, registering of [PhoneAccount](https://developer.android.com/reference/android/telecom/PhoneAccount)'s are essential to the functionality. This is integrated into the plugin - however it should be noted if the `PhoneAccount` is not registered nor the `Call Account` is not activated, the plugin will not work. See [here](https://developer.android.com/reference/android/telecom/PhoneAccount) for more information regarding `PhoneAccount`s and [here]() for `Calling Account`s.

To open the `Call Account` settings, use the following code:
```dart
   TwilioVoice.instance.openPhoneAccountSettings();
```

alternatively, this could be found in Phone App settings -> Other/Advanced Call Settings -> Calling Accounts -> Twilio Voice (toggle switch)

(if there is a method to programmatically open this, please submit a PR)

see [[Notes]](https://github.com/diegogarciar/twilio_voice/blob/master/NOTES.md#android) for more information.

### Web Setup:

There are 2 important files for Twilio incoming/missed call notifications to work:
- `notifications.js` is the main file, it handles the notifications and the service worker.
- `twilio-sw.js` is the service worker, it is used to handle the incoming calls.

Also, the twilio javascript SDK itself, `twilio.min.js` is needed.

To ensure proper/as intended setup:
1. Get all 3 files (`notifications.js`, `twilio.min.js` and `twilio-sw.js`) from `example/web` folder
2. Copy all 3 into your own project,
3. (optional) Review & change the `notifications.js`, `twilio-sw.js) files to match your needs.

Finally, add the following code to your `index.html` file, **at the end of body tag**:

``` html
    <body>        
        <!--twilio native js library-->
        <script type="text/javascript" src="./twilio.min.js"></script>
        <!--twilio native js library-->
        <script type="text/javascript" src="./notifications.js"></script>
        
        <script>
        if ('serviceWorker' in navigator) {
            window.addEventListener('load', async () => {
                await navigator.serviceWorker.register('twilio-sw.js').then(value => {
                    console.log('Twilio Voice Service worker registered successfully.');
                }).catch((error) => {
                    console.warn('Error registering Twilio Service Worker: ' + error.message + '. This prevents notifications from working natively');
                });
            });
        }
        </script>
    </body>
```

#### Web Considerations
Notice should be given to using `firebase-messaging-sw.js` in addition to `twilio-sw.js` since these may cause conflict in service worker events being handled.

_If you need to debug the service worker, open up Chrome Devtools, go to Application tab, and select Service Workers from the left menu. There you can see the service workers and their status.
To review service worker `notificationclick`, `notificationclose`, `message`, etc events - do this using Chrome Devtools (Sources tab, left panel below 'site code' the service workers are listed)_

### MacOS Setup:

The plugin is essentially a [WKWebView](https://developer.apple.com/documentation/webkit/wkwebview)
wrapper. This makes macOS integration a drop-in solution.

However, you'll need to:

1. add the following to your `Info.plist` file:
    ``` xml
    <key>NSMicrophoneUsageDescription</key>
    <string>Allow microphone access to make calls</string>
    ```
2. include Hardened Runtime entitlements (this is required for App Store distributed MacOS apps):
    ``` xml
   <key>com.apple.security.audio-input</key>
   <true/>

    <!--Optionally for bluetooth support/permissions-->
   <key>com.apple.security.device.bluetooth</key>
   <true/>
    ```

3. Lastly and most importantly, ensure the `index.html` and `twilio.min.js` is bundled inside of `twilio_voice` package (this
   shouldn't be a problem, but just in case). Found in `twilio_voice.../.../Classes/Resources/*`.

See [this](https://developer.apple.com/documentation/security/notarizing_macos_software_before_distribution#3087727)
for more information on preparing for publishing your macOS app

### Usage

The plugin was separated into two classes, the `TwilioVoice.instance`
and `TwilioVoice.instance.call`, the first one is in charge of general configuration and the second
one is in charge of managing calls.

Register iOS capabilities

- Add Audio and Voice over IP in background modes

### TwilioVoice.instance

#### Setting the tokens

call `TwilioVoice.instance.setTokens` as soon as your app starts.

- `accessToken` provided from your server, you can see an example cloud
  function [here](https://github.com/diegogarciar/twilio_voice/blob/master/functions.js).
- `deviceToken` is automatically handled on iOS, for android you need to pass a FCM token.

call `TwilioVoice.instance.unregister` to unregister from Twilio, if no access token is passed, it
will use the token provided in `setTokens` at the same session.

### Call Identifier

As incoming call UI is shown in background and the App can even be closed when receiving the calls,
you can map call identifiers such as `firebaseAuth` userIds to real names, this operation must be
done before actually receiving the call. So if you have a chat app, and know the members names,
register them so when they call, the call UI can display their names and not their userIds.

#### Registering a client

```
TwilioVoice.instance.registerClient(String clientId, String clientName)
```

#### Unregistering a client

```
TwilioVoice.instance.unregisterClient(String clientId)
```

#### Default caller

You can also set a default caller, such as "unknown number" or "chat friend" in case a call comes in
from an unregistered client.

```
TwilioVoice.instance.setDefaultCallerName(String callerName)
```

### Call Events

use stream `TwilioVoice.instance.callEventsListener` to receive events from the TwilioSDK such as
call events and logs, it is a broadcast so you can listen to it on different parts of your app. Some
events might be missed when the app has not launched, please check out the example project to find
the workarounds.

The events sent are the following

- ringing
- connected
- callEnded
- unhold
- hold
- unmute
- mute
- speakerOn
- speakerOff
- log
- answer

## showMissedCallNotifications

By default a local notification will be shown to the user after missing a call, clicking on the
notification will call back the user. To remove this feature, set `showMissedCallNotifications`
to `false`.

### Calls

#### Make a Call

`from` your own identifier
`to` the id you want to call
use `extraOptions` to pass additional variables to your server callback function.

```
 await TwilioVoice.instance.call.place(from:myId, to: clientId, extraOptions)
                   ;

```

#### Mute a Call

```
 TwilioVoice.instance.call.toggleMute(isMuted: true);

```

#### Toggle Speaker

```
 TwilioVoice.instance.call.toggleSpeaker(speakerIsOn: true);

```

#### Hang Up

```
 TwilioVoice.instance.call.hangUp();

```

#### Send Digits

```
 TwilioVoice.instance.call.sendDigits(String digits);

```

### Permissions

#### Microphone

To receive and place calls you need Microphone permissions, register the microphone permission in
your info.plist for iOS.

You can use `TwilioVoice.instance.hasMicAccess` and `TwilioVoice.instance.requestMicAccess` to check
and request the permission. Permissions is also automatically requested when receiving a call.

#### Background calls (Android only on some devices)

~~Xiaomi devices, and maybe others, need a special permission to receive background calls.
use `TwilioVoice.instance.requiresBackgroundPermissions` to check if your device requires a special
permission, if it does, show a rationale explaining the user why you need the permission. Finally
call
`TwilioVoice.instance.requestBackgroundPermissions` which will take the user to the App Settings
page to enable the permission.~~

Deprecated in 0.10.0, as it is no longer needed. Custom UI has been replaced with native UI.

### Localization

Because some of the UI is in native code, you need to localize those strings natively in your
project. You can find in the example project localization for spanish, PRs are welcome for other
languages.

---

## Twilio Setup/Quickstart Help

Twilio makes use of cloud functions to generate access tokens and sends them to your app. Further,
Twilio makes use of their own apps called TwiML apps to handle calling functions, etc

There are 2 major components to get Twilio Setup.

1. Cloud functions (facility generating **access tokens** and then **handling call requests**)
2. Mobile app that receives/updates tokens and performs the actual calls (see above)

---

### 1) Cloud Functions

Cloud functions can be separated or grouped together. The main 2 components are:

- generate access tokens
- `make-call` endpoint to actually place the call

You can host both in firebase, in TwiML apps or a mixture. The setup below assumes a mixture, where
Firebase Functions hosts the `access-token` for easy integration into Flutter and TwiML hosting
the `make-call` function.

## Cloud-Functions-Step-1: Create your TwiML app

This will allow you to actually place the call

Prerequisites
---

* A Twilio Account. Don't have one? [Sign up](https://www.twilio.com/try-twilio) for free!

## Setting up the Application

Grab [this](https://github.com/twilio/voice-quickstart-server-node) project from github, the sample
TwiML app.

```bash
cp .env.example .env
```

Edit `.env` with the three configuration parameters we gathered from above.

**See configure environment below for details**

Next, we need to install our dependencies from npm:

```bash
npm install
```

To make things easier for you, go into the `src/` folder, rename the `server.js` file to `make-call`
. This assumes each function will have its own file which for a new project isn't a bad idea.

Then add the following code:

```javascript
const AccessToken = require('twilio').jwt.AccessToken;
const VoiceGrant = AccessToken.VoiceGrant;
const VoiceResponse = require('twilio').twiml.VoiceResponse;

/**
 * Creates an endpoint that can be used in your TwiML App as the Voice Request Url.
 * <br><br>
 * In order to make an outgoing call using Twilio Voice SDK, you need to provide a
 * TwiML App SID in the Access Token. You can run your server, make it publicly
 * accessible and use `/makeCall` endpoint as the Voice Request Url in your TwiML App.
 * <br><br>
 *
 * @returns {Object} - The Response Object with TwiMl, used to respond to an outgoing call
 * @param context
 * @param event
 * @param callback
 */
exports.handler = function(context, event, callback) {
    // The recipient of the call, a phone number or a client

    console.log(event);
    const from = event.From;
    let to = event.to;
    if(isEmptyOrNull(to)) {
        to = event.To;
        if(isEmptyOrNull(to)) {
            console.error("Could not find someone to call");
            to = undefined;
        }
    }


    const voiceResponse = new VoiceResponse();

    if (!to) {
        voiceResponse.say("Welcome, you made your first call.");
    } else if (isNumber(to)) {
      const dial = voiceResponse.dial({callerId : callerNumber});
      dial.number(to);
  } else {
        console.log(`Calling [${from}] -> [${to}]`)

        const dial = voiceResponse.dial({callerId: to, timeout: 30, record: "record-from-answer-dual", trim: "trim-silence"});
        dial.client(to);
    }

    callback(null, voiceResponse);
}

const isEmptyOrNull = (s) => {
    return !s || s === '';
}
```

### Setup Twilio CLI

Ensure you are logged into `twilio-cli`. First, install `twilio-cli` with

```javascript
npm i twilio-cli -g
```

Afterwards, login to twilio using: (b sure to provide Twilio account SID and auth token for login):

```javascript
twilio login
```

We need to generate an app, this will give us an App SID to use later in firebase functions, (
see [this](https://github.com/twilio/voice-quickstart-ios#3-create-a-twiml-application-for-the-access-token)
more info)

### Create TwiML app

We need to create a TwiML app that will allow us to host a `make-call` function:

```bash
twilio api:core:applications:create \
--friendly-name=my-twiml-app \
--voice-method=POST \
--voice-url="https://my-quickstart-dev.twil.io/make-call"
```

This will present you with a application SID in the format ```APxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx```,
we will use this later in firebase config and generating push credential keys.

**Very Important!** The URL given here `https://my-quickstart-dev.twil.io/make-call` won't work for
you. Once you deployed your TwiML application (later), a URL is given to you (on first deploy) which
you need to copy and paste as your **Request URL** call. If you don't do this, calling won't work!

### Configure environment

ensure you have a `.env` file in the root of your project in the same directory as `package.json`

next, edit the `.env` file in the format

```bash
ACCOUNT_SID=(insert account SID)
APP_SID=(insert App SID, found on TwiML app or the APxxxxx key above)
```

`API_KEY` and `API_KEY_SECRET` aren't necessary here since we won't be using them

#### Get Push Credential:

**We will generate them a bit later**

- Android
  FCM: [Android instructions](https://github.com/twilio/voice-quickstart-android#7-create-a-push-credential-using-your-fcm-server-key)
- Apple
  APNS: [Apple instructions](https://github.com/twilio/voice-quickstart-ios#6-create-a-push-credential-with-your-voip-service-certificate)

You will get a Push Credential SID in the format: `CRxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx`, use this
in `PUSH_CREDENTIAL_SID`

### Deploying

Now lets deploy.

#### Please note:  Check you have configured your environment first

Navigate to root directory, and deploy using

```javascript
twilio serverless:deploy
```

**Very Important!**: once complete (if you haven't done so), make sure to add the `make-call`
endpoint your Twilio app's `Request URL` in the main Twilio page. This URL will be shown as part of
the deployment text. If this isn't done, calling won't work!

### Cloud-Functions-Step-2: Setup Firebase & Configuration

Twilio's configurations are stored in `.runtimeconfig.json` which contains:

    "auth_token": "",
    "account_sid": "",
    "app_sid": "",
    "phone": "",
    "api_key": "",
    "api_key_secret": "",
    "android_push_credential": "",
    "apple_push_credential_debug": "",
    "apple_push_credential_release": ""

_**Note:** this is used for local emulator testing, but you need to deploy these to your firebase
function application once you are ready to go live. If you don't, this won't work!_

**Push Credentials** are created once (for iOS, Android) and used to generate `access-token`s, a
callback function for all Twilio apps to use for their communication.

---

Below are the 3 operations you need to run to generate push credentials that should be added into
the `.runtimeconfig.json` above

##### Android

To generate Android push credentials, get the Cloud Messaging server key from Firebase FCM, and add
it to the following:

```
twilio api:chat:v2:credentials:create \
--type=fcm \
--friendly-name="voice-push-credential-fcm" \
--secret=SERVER_KEY_VALUE
```

and then place into the field: `android_push_credential` above

This generated a push credential SID in the format `CRxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx` which must
be used to generate access tokens for android devices.

see for more
info: https://github.com/twilio/voice-quickstart-android#7-create-a-push-credential-using-your-fcm-server-key

##### iOS

Similar to Android, but more steps including using .p12 certificates. To get these certificates,
login into [Apple's developer site](https://developer.apple.com/) and go to
the [certificates page](https://developer.apple.com/account/resources/certificates/list). You need
to generate a VoIP Services certificate as shown below.

![voip_services.png](images/voip_services.png)

**Please note:** there are 2 different modes: sandbox and production.

**- SandBox Mode**

Using sandbox VoIP certificate:

> Export your VoIP Service Certificate as a .p12 file from Keychain Access and extract the
> certificate and private key from the .p12 file using the openssl command.

```
$ openssl pkcs12 -in PATH_TO_YOUR_SANDBOX_P12 -nokeys -out sandbox_cert.pem -nodes
$ openssl pkcs12 -in PATH_TO_YOUR_SANDBOX_P12 -nocerts -out sandbox_key.pem -nodes
$ openssl rsa -in sandbox_key.pem -out sandbox_key.pem
```

Using sandbox certificates, generate credential:

```
twilio api:chat:v2:credentials:create \
--type=apn \
--sandbox \
--friendly-name="voice-push-credential (sandbox)" \
--certificate="$(cat PATH_TO_SANDBOX_CERT_PEM)" \
--private-key="$(cat PATH_TO_SANDBOX_KEY_PEM)"
```

then place it into the field `apple_push_credential_debug`

**- Production Mode**

Using production VoIP certificate:

> Export your VoIP Service Certificate as a .p12 file from Keychain Access and extract the
> certificate and private key from the .p12 file using the openssl command.

```
$ openssl pkcs12 -in PATH_TO_YOUR_P12 -nokeys -out prod_cert.pem -nodes
$ openssl pkcs12 -in PATH_TO_YOUR_P12 -nocerts -out prod_key.pem -nodes
$ openssl rsa -in prod_key.pem -out prod_key.pem
```

Using production certificates, generate credential:

```
twilio api:chat:v2:credentials:create \
--type=apn \
--friendly-name="voice-push-credential (production)" \
--certificate="$(cat PATH_TO_PROD_CERT_PEM)" \
--private-key="$(cat PATH_TO_PROD_KEY_PEM)"
```

then place it into the field `apple_push_credential_release`

see for more
info: https://github.com/twilio/voice-quickstart-ios#6-create-a-push-credential-with-your-voip-service-certificate

---

## Cloud-Functions-Step-3: Generate access tokens via cloud function

`HTTP/GET api-voice-accessToken`

To generate **access-tokens**, the following firebase function is used:

_**Please Note** the default time is 1 hour token validity._

See for more
info: https://github.com/twilio/voice-quickstart-android/blob/master/Docs/access-token.md

**Firebase Cloud Function: access-token**

```javascript
const { AccessToken } = require('twilio').jwt;
const functions = require('firebase-functions');

const { VoiceGrant } = AccessToken;

/**
 * Creates an access token with VoiceGrant using your Twilio credentials.
 *
 * @param {Object} request - POST or GET request that provides the recipient of the call, a phone number or a client
 * @param {Object} response - The Response Object for the http request
 * @returns {string} - The Access Token string and expiry date in milliseconds
 */
exports.accessToken = functions.https.onCall((payload, context) => {
    // Check user authenticated
    if (typeof (context.auth) === 'undefined') {
        throw new functions.https.HttpsError('unauthenticated', 'The function must be called while authenticated');
    }
    let userId = context.auth.uid;

    console.log('creating access token for ', userId);

    //configuration using firebase environment variables
    const twilioConfig = functions.config().twilio;
    const accountSid = twilioConfig.account_sid;
    const apiKey = twilioConfig.api_key;
    const apiSecret = twilioConfig.api_key_secret;
    const outgoingApplicationSid = twilioConfig.app_sid;

    // Used specifically for creating Voice tokens, we need to use seperate push credentials for each platform. 
    // iOS has different APNs environments, so we need to distinguish between sandbox & production as the one won't work in the other.
    let pushCredSid;
    if (payload.isIOS === true) {
        console.log('creating access token for iOS');
        pushCredSid = payload.production ? twilioConfig.apple_push_credential_release
            : (twilioConfig.apple_push_credential_debug || twilioConfig.apple_push_credential_release);
    } else if (payload.isAndroid === true) {
        console.log('creating access token for Android');
        pushCredSid = twilioConfig.android_push_credential;
    } else {
        throw new functions.https.HttpsError('unknown_platform', 'No platform specified');
    }

    // generate token valid for 24 hours - minimum is 3min, max is 24 hours, default is 1 hour
    const dateTime = new Date();
    dateTime.setDate(dateTime.getDate()+1);
    // Create an access token which we will sign and return to the client,
    // containing the grant we just created
    const voiceGrant = new VoiceGrant({
        outgoingApplicationSid,
        pushCredentialSid: pushCredSid,
    });

    // Create an access token which we will sign and return to the client,
    // containing the grant we just created
    const token = new AccessToken(accountSid, apiKey, apiSecret);
    token.addGrant(voiceGrant);

    // use firebase ID for identity
    token.identity = userId;
    console.log(`Token:${token.toJwt()}`);

    // return json object
    return {
        "token": token.toJwt(),
        "identity": userId,
        "expiry_date": dateTime.getTime()
    };
});
```

Add the function above to your Firebase Functions application,
see [this](https://firebase.google.com/docs/functions/get-started) for more help on creating a
firebase functions project

After you are done, deploy your `.runtimeconfig.json`,
see [this](https://firebase.google.com/docs/functions/config-env) for more help.

Once done with everything above, deploy your firebase function with this:

```bash
firebase deploy --only functions
```

##### Done!

Calling should work naturally - just make sure to fetch the token from the endpoint and you can call

See [example](https://github.com/diegogarciar/twilio_voice/blob/master/example/lib/main.dart#L51)
code, make sure to change the `voice-accessToken` to your function name, given to you by firebase
when deploying (as part of the deploy text)


## Future Work
- Move package to `federated plugin` structure (see [here](https://flutter.dev/go/federated-plugins) for more info), see reduced overhead advantages covered as motivation (see [here](https://medium.com/flutter/how-to-write-a-flutter-web-plugin-part-2-afdddb69ece6) for more info))

## Updating Twilio Voice JS SDK
`twilio.js` is no longer hosted via CDNs (see [reference](https://github.com/twilio/twilio-voice.js/blob/master/README.md#cdn)), instead it is hosted via npm / github. See instructions found [here](https://github.com/twilio/twilio-voice.js/blob/master/README.md#github)
This is automatically added to your `web/index.html` file, as a `<script/>` tag during runtime. See [here](./lib/_internal/twilio_loader.dart) for more info.);
