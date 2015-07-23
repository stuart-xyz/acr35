cordova.define("me.stuartphillips.plugins.acr35.acr35", function(require, exports, module) { module.exports = {
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

});
