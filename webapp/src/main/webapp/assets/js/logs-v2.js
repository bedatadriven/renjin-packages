
(function() {
    function isElementInViewport (el) {
        var rect = el.getBoundingClientRect();

        return (
            rect.top >= 0 &&
            rect.bottom <= (window.innerHeight || document.documentElement.clientHeight)
        );
    }
    
    function loadLog(logDiv) {
        var logUrl = logDiv.getAttribute("data-log-url");
        if(logUrl) {
            if(!logDiv.getAttribute("data-loading")) {
                logDiv.setAttribute("data-loading", "true");

                var request = new XMLHttpRequest();
                request.open('GET', logUrl, true);
                request.onload = function () {
                    if (request.status >= 200 && request.status < 400) {
                        logDiv.innerText = request.responseText;
                    } else {
                        logDiv.innerText = "Error loading log"
                    }
                };
                request.onerror = function () {
                    logDiv.innerText = "Error loading log"
                };
                request.send();
            }
        }
    }
    
    function loadLogsInView() {
        var logDivs = document.getElementsByClassName("log");
        var i;
        for (i = 0; i < logDivs.length; i++) {
            if(isElementInViewport(logDivs[i])) {
                loadLog(logDivs[i]);
            }
        }    
    }
    
    window.addEventListener("load", loadLogsInView);
    window.addEventListener("scroll", loadLogsInView);
})();