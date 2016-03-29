
(function() {
    
    function loadLog(logDiv) {
        var logUrl = logDiv.getAttribute("data-log-url");
        var request = new XMLHttpRequest();
        request.open('GET', logUrl, true);
        request.onload = function() {
            if (request.status >= 200 && request.status < 400) {
                logDiv.innerText = request.responseText;
            } else {
                logDiv.innerText = "Error loading log"
            }
        };
        request.onerror = function() {
            logDiv.innerText = "Error loading log"
        };
        request.send();
    }
    
    var logDivs = document.getElementsByClassName("log-file");
    var i;
    for (i = 0; i < logDivs.length; i++) {
        loadLog(logDivs[i]);
    }
})();