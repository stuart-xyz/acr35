/********* acr35.m Cordova Plugin Implementation *******/

#import "acr35.h"

/**
 * This class allows control of the ACR35 reader sleep state and PICC commands
 */
@implementation acr35

/** AudioJackReader object */
ACRAudioJackReader *_reader;
/** The ID corresponding to the command sent by Apache Cordova */
NSString *myCallbackId;
 
/** Is this plugin being initialised? */
bool firstRun = true;
/** Is this the first reset of the reader? */
bool firstReset = true;
 
/** APDU command for reading a card's UID */
uint8_t commandApdu[] = { 0xFF, 0xCA, 0x00, 0x00, 0x00 };
/** the integer representing card type */
NSUInteger cardType;
/** Timeout for APDU response (in <b>seconds</b>) */
NSUInteger timeout = 1;
 
/** Stop the polling thread? */
bool killThread = false;
/** Is the reader currently connected? */
bool readerConnected = true;
/** The number of iterations that have passed with no response from the reader */
int itersWithoutResponse = 0;

- (NSString *)hexStringFromByteArray:(const uint8_t *)buffer length:(NSUInteger)length {
    
    NSString *hexString = @"";
    NSUInteger i = 0;
    
    for (i = 0; i < length; i++) {
        if (i == 0) {
            hexString = [hexString stringByAppendingFormat:@"%02X", buffer[i]];
        } else {
            hexString = [hexString stringByAppendingFormat:@" %02X", buffer[i]];
        }
    }
    
    return hexString;
}

- (void)sleep:(CDVInvokedUrlCommand*)command {
	/* Kill the polling thread */
	killThread = true;
	/* Send a success message back to Cordova */
    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:@"asleep"];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)read:(CDVInvokedUrlCommand*)command {

	/* Class variables require initialisation on first launch */
	if(firstRun){
	    // Initialize ACRAudioJackReader object.
	    _reader = [[ACRAudioJackReader alloc] init];
	    [_reader setDelegate:self];
	    firstRun = false;
	}
	firstReset = true;

	/* Set the card type */
	cardType = [[command.arguments objectAtIndex:0] intValue];

	/* Get the callback ID of the current command */
    myCallbackId = command.callbackId;
    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:@"initialised"];
    /* Required so that a result can be returned asynchronously from another thread */
    [pluginResult setKeepCallback:[NSNumber numberWithBool:YES]];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:myCallbackId];
	
    /* Reset the reader */
    [_reader reset];
}

- (void)transmit {
	NSLog(@"iteration");
    // Power on the PICC.
    [_reader piccPowerOnWithTimeout:timeout cardType:cardType];
    // Transmit the APDU.
    [_reader piccTransmitWithTimeout:timeout commandApdu:commandApdu length:sizeof(commandApdu)];
}

#pragma mark - Audio Jack Reader

- (void)readerDidReset:(ACRAudioJackReader *)reader {
    [self.commandDelegate runInBackground:^{

		if (firstReset){
			/* Set the reader asleep */
			[_reader sleep];
			/* Wait one second */
		    [NSThread sleepForTimeInterval:1.0];
		    /* Reset the reader */
		    [_reader reset];
			
			firstReset = false;
		} 
		/* Sends the APDU command for reading a card UID every second */
		else {
			/* Wait one second for stability */
            [NSThread sleepForTimeInterval:1.0];

	        while(!killThread){
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

				/* If we have waited 3 seconds without a response */
				// TODO: check media volume and whether any device is plugged into audio socket
				if(itersWithoutResponse == 4) {
					/* Communicate to the Cordova application that the reader is disconnected */
					CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:@"disconnected"];
					[self.commandDelegate sendPluginResult:pluginResult callbackId:myCallbackId];
					/* Kill this thread */
					killThread = true;

					NSLog(@"disconnected");
					itersWithoutResponse = 0;
				}
				else{
					[self transmit];
	            	/* Repeat every second */
		            [NSThread sleepForTimeInterval:1.0];
				}
	        }
		    /* Power off the PICC */
		    [_reader piccPowerOff];
			/* Set the reader asleep */
			[_reader sleep];

			killThread = false;
		}
    }];
}

- (void)reader:(ACRAudioJackReader *)reader didSendPiccResponseApdu:(const uint8_t *)responseApdu
        length:(NSUInteger)length {
	/* Update the connection status */
	readerConnected = true;
    NSString *uid = [self hexStringFromByteArray:responseApdu length:(sizeof(responseApdu)*2) - 1];

    /* Send the card UID to the Cordova application */
    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:uid];
    [pluginResult setKeepCallback:[NSNumber numberWithBool:YES]];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:myCallbackId];

    /* Print out the UID */
    NSLog(uid);
}

@end