package me.stuartphillips.plugins;

import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.acs.audiojack.AesTrackData;
import com.acs.audiojack.AudioJackReader;
import com.acs.audiojack.DukptTrackData;
import com.acs.audiojack.Result;
import com.acs.audiojack.TrackData;

import org.apache.cordova.PluginResult;
import org.apache.cordova.CallbackContext;

import java.lang.Thread;

/**
 * This class sets up an independent thread for card polling, and is linked to the
 * <code>setOnPiccResponseApduAvailableListener</code> callback function
 */
public class Transmitter implements Runnable {

    private AudioJackReader mReader;
    private AudioManager mAudioManager;
    private CallbackContext mContext;

    private boolean killMe = false;          /** Stop the polling thread? */
    private int itersWithoutResponse = 0;    /** The number of iterations that have passed with no
                                                 response from the reader */
    private boolean readerConnected = true;  /** Is the reader currently connected? */

    private final int cardType;
    private int timeout;
    private byte[] apdu;

    /**
     * @param mReader: AudioJack reader service
     * @param mAudioManager: system audio service
     * @param mContext: context for plugin results
     * @param timeout: time in <b>seconds</b> to wait for commands to complete
     * @param apdu: byte array containing the command to be sent
     * @param cardType: the integer representing card type
     */
    public Transmitter(AudioJackReader mReader, AudioManager mAudioManager,
                       CallbackContext mContext, int timeout, byte[] apdu, int cardType){
        this.mReader = mReader;
        this.mAudioManager = mAudioManager;
        this.mContext = mContext;
        this.timeout = timeout;
        this.apdu = apdu;
        this.cardType = cardType;
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
     * Stops the polling thread
     */
    public void kill(){
        killMe = true;
    }

    /**
     * Updates the connection status of the reader (links to APDU response callback)
     */
    public void updateStatus(boolean status){
        readerConnected = status;
    }

    /**
     * Sends the APDU command for reading a card UID every second
     */
    @Override
    public void run() {
        try {
            /* Wait one second for stability */
            Thread.sleep(1000);

            while (!killMe) {
                /* If the reader is not connected, increment no. of iterations without response */
                if(!readerConnected){
                    itersWithoutResponse++;
                }
                /* Else, reset the number of iterations without a response */
                else{
                    itersWithoutResponse = 0;
                }
                /* Reset the connection state */
                readerConnected = false;

                /* If we have waited 3 seconds without a response, or the audio jack is not
                 * plugged in, or the device media volume is below 100% */
                if(itersWithoutResponse == 4) {
                    /* Communicate to the Cordova application that the reader is disconnected */
                    mContext.sendPluginResult(new PluginResult(PluginResult.Status.OK,
                            "disconnected"));
                    /* Kill this thread */
                    kill();
                } else if(!mAudioManager.isWiredHeadsetOn()) {
                    /* Communicate to the Cordova application that the reader is unplugged */
                    mContext.sendPluginResult(new PluginResult(PluginResult.Status.OK,
                            "unplugged"));
                    /* Kill this thread */
                    kill();
                } else if(!maxVolume()) {
                    /* Communicate to the Cordova application that the media volume is low */
                    mContext.sendPluginResult(new PluginResult(PluginResult.Status.OK,
                            "low_volume"));
                    /* Kill this thread */
                    kill();
                } else{
                    System.out.println("reading...");
                    /* Power on the PICC */
                    mReader.piccPowerOn(timeout, cardType);
                    /* Transmit the APDU */
                    mReader.piccTransmit(timeout, apdu);
                    /* Repeat every second */
                    Thread.sleep(1000);
                }
            }
            /* Power off the PICC */
            mReader.piccPowerOff();
            /* Set the reader asleep */
            mReader.sleep();
            /* Stop the reader service */
            mReader.stop();
        } catch (InterruptedException e) {
            e.printStackTrace();
            // TODO: add exception handling
        }
    }

}
