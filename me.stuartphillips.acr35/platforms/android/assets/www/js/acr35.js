var cardType = 143;  // Default value inside the ACR35 AudioJackDemo application

function reader_start(){
    acr35.read(cardType, function(message){
        if(message == "disconnected"){
            window.plugins.toast.showShortBottom("ERROR: could not connect to the reader. Is it charged?");
        } else if(message == "unplugged"){
            window.plugins.toast.showShortBottom("ERROR: no device is plugged into the audio socket");
        } else if(message == "low_volume"){
            window.plugins.toast.showShortBottom("ERROR: your device media volume is not set to 100%");
        } else if(message != "initialised"){
            window.plugins.toast.showShortBottom("UID READ: " + message);
        }
    }, function(error){
        window.plugins.toast.showShortBottom("ERROR: an application error occurred while polling");
    });
}

function reader_stop(){
    acr35.sleep(function(){
        window.plugins.toast.showShortBottom("INFO: stopped polling successfully");
    }, function(){
        window.plugins.toast.showShortBottom("ERROR: polling could not be stopped");
    });
}