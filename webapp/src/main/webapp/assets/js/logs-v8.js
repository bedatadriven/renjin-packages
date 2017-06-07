
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
        var buildId = logDiv.getAttribute("data-build-id");
        if(logUrl) {
            if(!logDiv.getAttribute("data-loading")) {
                logDiv.setAttribute("data-loading", "true");

                var request = new XMLHttpRequest();
                request.open('GET', logUrl, true);
                request.onload = function () {
                    if (request.status >= 200 && request.status < 400) {
                        logDiv.innerHTML = formatLog(request.responseText, buildId);
                    } else {
                        logDiv.innerHTML = "Error loading log"
                    }
                };
                request.onerror = function () {
                    logDiv.innerText = "Error loading log"
                };
                request.send();
            }
        }
    }

    function formatLog(log, buildId) {
        var lines = log.split(/[\r\n]+/g);
        var html = "";

        lines.forEach(function (line) {
            html += "<div class=\"line\">" + formatLine(line, buildId) + "</div>";
        });

        return html;

    }

    // Should match "	at org.renjin.sexp.PairList$Node.getTag(PairList.java:223)"
    var javaLineRe = /^(\s*at\s*)([A-Za-z0-9_.$]+)\(([^:]+):(\d+)\)$/;


    function formatLine(line, buildId) {
        var javaLineMatch = javaLineRe.exec(line);
        if(javaLineMatch) {
            var indent = javaLineMatch[1];
            var method = javaLineMatch[2];
            var file = javaLineMatch[3];
            var lineNum = javaLineMatch[4];
            return indent + method + "(<a class=\"srcref\" href=\"/source/java?" +
                "method=" + encodeURIComponent(method) +
                "&file=" + encodeURIComponent(file) +
                "&line=" + encodeURIComponent(lineNum) +
                "&build=" + encodeURIComponent(buildId) +  "\">" + file + ":" + lineNum + "</a>)"
        } else {
            return escapeHtml(line);
        }
    }

    function escapeHtml(unsafe) {
        return unsafe
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#039;");
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