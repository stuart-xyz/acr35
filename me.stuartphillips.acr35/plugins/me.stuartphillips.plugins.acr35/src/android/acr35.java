package me.stuartphillips.plugins;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.acs.audiojack.AudioJackReader;
import com.acs.audiojack.ReaderException;

import android.media.AudioManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;

import java.lang.Override;
import java.lang.Runnable;
import java.lang.System;
import java.lang.Thread;
import java.util.Locale;

/**
 * This class allows control of the ACR35 reader sleep state and PICC commands
 */
public class acr35 extends CordovaPlugin {

    private Transmitter transmitter;
    private AudioManager mAudioManager;
    private AudioJackReader mReader;
    private Context mContext;

    private boolean firstRun = true;    /** Is this plugin being initialised? */
    private boolean firstReset = true;  /** Is this the first reset of the reader? */

    /** APDU command for reading a card's UID */
    private final byte[] apdu = { (byte) 0xFF, (byte) 0xCA, (byte) 0x00, (byte) 0x00, (byte) 0x00 };
    /** Timeout for APDU response (in <b>seconds</b>) */
    private final int timeout = 1;

    /**
     * Converts raw data into a hexidecimal string
     *
     * @param buffer: raw data in the form of a byte array
     * @return a string containing the data in hexidecimal form
     */
    private String bytesToHex(byte[] buffer) {
        String bufferString = "";
        if (buffer != null) {
            for(int i = 0; i < buffer.length; i++) {
                String hexChar = Integer.toHexString(buffer[i] & 0xFF);
                if (hexChar.length() == 1) {
                    hexChar = "0" + hexChar;
                }
                bufferString += hexChar.toUpperCase(Locale.US) + " ";
            }
        }
        return bufferString;
    }

    /**
     * Checks if the device media volume is set to 100%
     *
     * @return true if media volume is at 100%
     */
    private boolean maxVolume() {
        int currentVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        int maxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

        if (currentVolume < maxVolume) {
            return false;
        }
        else{
            return true;
        }
    }

    /**
     * Sets the ACR35 reader to continuously poll for the presence of a card. If a card is found,
     * the UID will be returned to the Apache Cordova application
     *
     * @param callbackContext: the callback context provided by Cordova
     * @param cardType: the integer representing card type
     */
    private void read(final CallbackContext callbackContext, final int cardType){
        System.out.println("setting up for reading...");
        firstReset = true;

        /* If no device is plugged into the audio socket or the media volume is < 100% */
        if(!mAudioManager.isWiredHeadsetOn()){
            /* Communicate to the Cordova application that the reader is unplugged */
            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK,
                    "unplugged"));
            return;
        } else if(!maxVolume()) {
            /* Communicate to the Cordova application that the media volume is low */
            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK,
                    "low_volume"));
            return;
        }

        /* Set the PICC response APDU callback */
        mReader.setOnPiccResponseApduAvailableListener
                (new AudioJackReader.OnPiccResponseApduAvailableListener() {
                    @Override
                    public void onPiccResponseApduAvailable(AudioJackReader reader,
                                                            byte[] responseApdu) {
                        /* Update the connection status of the transmitter */
                        transmitter.updateStatus(true);
                        /* Send the card UID to the Cordova application */
                        PluginResult result = new PluginResult(PluginResult.Status.OK,
                                bytesToHex(responseApdu));
                        result.setKeepCallback(true);
                        callbackContext.sendPluginResult(result);

                        /* Print out the UID */
                        System.out.println(bytesToHex(responseApdu));
                    }
                });

        /* Set the reset complete callback */
        mReader.setOnResetCompleteListener(new AudioJackReader.OnResetCompleteListener() {
            @Override
            public void onResetComplete(AudioJackReader reader) {
                System.out.println("reset complete");

                /* If this is the first reset, the ACR35 reader must be turned off and back on again
                   to work reliably... */
                if(firstReset){
                    cordova.getThreadPool().execute(new Runnable() {
                        public void run() {
                            try{
                                /* Set the reader asleep */
                                mReader.sleep();
                                /* Wait one second */
                                Thread.sleep(1000);
                                /* Reset the reader */
                                mReader.reset();

                                firstReset = false;
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                                // TODO: add exception handling
                            }
                        }
                    });
                } else {
                    /* Create a new transmitter for the UID read command */
                    transmitter = new Transmitter(mReader, mAudioManager, callbackContext, timeout,
                            apdu, cardType);
                    /* Cordova has its own thread management system */
                    cordova.getThreadPool().execute(transmitter);
                }
            }
        });

        mReader.start();
        mReader.reset();
        System.out.println("setup complete");
    }

    /**
     * This method acts as the bridge between Cordova and native Android code. The Cordova
     * application will invoke this method from JavaScript
     *
     * @param action: the command sent by the Cordova application
     * @param args: the command arguments sent by the Cordova application
     * @param callbackContext: the callback context provided by Cordova
     * @return a boolean that notifies whether the command execution was successful
     * @throws JSONException
     */
    @Override
    public boolean execute(String action, JSONArray args, final CallbackContext callbackContext)
            throws JSONException {

        /* Class variables require initialisation on first launch */
        if(firstRun){
            /* Context is acquired using cordova.getActivity() */
            mAudioManager = (AudioManager) this.cordova.getActivity().getApplicationContext()
                    .getSystemService(Context.AUDIO_SERVICE);
            mReader = new AudioJackReader(mAudioManager);
            firstRun = false;
        }

        if (action.equals("read")) {
            System.out.println("reading...");
            /* Use args.getString to retrieve arguments sent by the Cordova application */
            read(callbackContext, Integer.parseInt(args.getString(0)));
            /* Required so that a result can be returned asynchronously from another thread */
            PluginResult result = new PluginResult(PluginResult.Status.NO_RESULT);
            result.setKeepCallback(true);
            callbackContext.sendPluginResult(result);
            return true;
        } else if (action.equals("sleep")) {
            System.out.println("sleeping...");
            /* Kill the polling thread */
            if(transmitter != null){
                transmitter.kill();
            }
            /* Send a success message back to Cordova */
            callbackContext.success();
            return true;
        }
        /* Else, an invalid command was sent */
        else {
            System.out.println("invalid command");
            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.INVALID_ACTION));
            return false;
        }
    }

}
