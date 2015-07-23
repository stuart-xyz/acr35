module.exports = {
    read: function(cardType, successCallback, failureCallback) {
        cordova.exec(successCallback,
            failureCallback,
            "acr35",
            "read",
            [cardType]);
    },
    sleep: function(successCallback, failureCallback) {
        cordova.exec(successCallback,
            failureCallback,
            "acr35",
            "sleep",
            []);
    }
};
