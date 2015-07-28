var cardType = "143";  // Default value inside the ACR35 AudioJackDemo application
var lastUid = "";
var showDuplicates = true;

function reader_start(){
    acr35.read(cardType, function(message){
        if(message == "disconnected"){
            window.plugins.toast.showShortBottom("ERROR: could not connect to the reader. Is it charged?");
        } else if(message == "unplugged"){
            window.plugins.toast.showShortBottom("ERROR: no device is plugged into the audio socket");
        } else if(message == "low_volume"){
            window.plugins.toast.showShortBottom("ERROR: your device media volume is not set to 100%");
        } else if((message != "initialised") && (message.substring(0,5) != "90 00") && (message != lastUid)){
            lastUid = message;
            window.plugins.toast.showShortBottom("UID READ: " + message);
            document.getElementById("uid_log").innerHTML += "<b>UID READ:</b> " + message + "<br>";
        } else if((message == lastUid) && showDuplicates){
            document.getElementById("uid_log").innerHTML += "<b>DUPLICATE UID READ:</b> " + message + "<br>";
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
