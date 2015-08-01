/********* acr35.h Cordova Plugin Header *******/

#import <Cordova/CDV.h>
#import "AudioJack.h"

@interface acr35 : CDVPlugin <ACRAudioJackReaderDelegate>

/**
 * Sets the ACR35 reader to continuously poll for the presence of a card. If a card is found,
 * the UID will be returned to the Apache Cordova application
 *
 * @param command: the command sent from Cordova
 */
- (void)read:(CDVInvokedUrlCommand*)command;

/**
 * Sends the reader to sleep after stopping the polling thread
 * 
 * @param command: the command sent from Cordova
*/
- (void)sleep:(CDVInvokedUrlCommand*)command;

/**
 * Converts raw data into a hexidecimal string
 *
 * @param buffer: raw data in the form of a byte array
 * @param length: the length of the byte array
 * @return a string containing the data in hexidecimal form
 */
- (NSString *)hexStringFromByteArray:(const uint8_t *)buffer length:(NSUInteger)length;

/**
 * Transmits a PICC command to the reader
 */
- (void)transmit;

/**
 * Callback for when the reader has successfully reset
 *
 * @param reader: SDK reader object
 */
- (void)readerDidReset:(ACRAudioJackReader *)reader;

/**
 * Callback for PICC response APDU
 *
 * @param reader: SDK reader object
 * @param responseApdu: byte array containing the response
 * @param length: the length of the response
 */
- (void)reader:(ACRAudioJackReader *)reader didSendPiccResponseApdu:(const uint8_t *)responseApdu
        length:(NSUInteger)length;

@end