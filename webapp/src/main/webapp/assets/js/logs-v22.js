
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
        var logType = logDiv.getAttribute("data-log-type");
        var buildId = logDiv.getAttribute("data-build-id");
        if(logUrl) {
            if(!logDiv.getAttribute("data-loading")) {
                logDiv.setAttribute("data-loading", "true");

                var request = new XMLHttpRequest();
                request.open('GET', logUrl, true);
                request.onload = function () {
                    if (request.status >= 200 && request.status < 400) {
                        var replOutput = isReplOutput(request.responseText);
                        if(replOutput) {
                            logDiv.classList.add("repl");
                        }
                        logDiv.innerHTML = formatLog(request.responseText, replOutput, buildId);
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

    function isReplOutput(log) {
        return log.startsWith("Renjin ");
    }

    function formatLog(log, repl, buildId) {
        var lines = log.split(/[\r\n]+/g);
        var html = "";
        var context = { };

        if(repl) {
            context.prompt = 1;
        }

        if(repl) {
            html += "<a class=\"copy-log\" href='#'>Copy</a>";
        }

        lines.forEach(function (line) {
            html += "<div class=\"line\">" + formatLine(context, line, buildId) + "</div>";
        });

        return html;

    }

    // Should match "	at org.renjin.sexp.PairList$Node.getTag(PairList.java:223)"
    var javaLineRe = /^(\s*at\s*)([A-Za-z0-9_.$]+)\(([^:]+):(\d+)\)$/;

    // Should match "  at validObject()"
    var renjinLineRe = /^(\s*at\s*)([A-Za-z0-9._]+)\(\)/;

    function formatLine(context, line, buildId) {

        if(context.prompt === 1 && line.startsWith("> ")) {
            // Transition to state prompt 1 and format the first line of a repl input
            context.prompt = 2;
            return "<span class='prompt'>&gt; </span><span class='stmt'>" +
                escapeHtml(line.substr(2)) + "</span>";
        }
        if(context.prompt === 2) {
            // previous line was >
            if (line.startsWith("> ") || line.startsWith("+ ")) {
                return "<span class='prompt'>" + escapeHtml(line.substr(0, 2)) + "</span>" +
                       "<span class='stmt'>" + escapeHtml(line.substr(2)) + "</span>";
            } else {
                context.prompt = 1;
            }
        }

        var javaLineMatch = javaLineRe.exec(line);
        if(javaLineMatch) {
            var indent = javaLineMatch[1];
            var method = javaLineMatch[2];
            var file = javaLineMatch[3];
            var lineNum = javaLineMatch[4];
            return indent + method + "(<a href=\"/source/redirect/java?" +
                "method=" + encodeURIComponent(method) +
                "&file=" + encodeURIComponent(file) +
                "&line=" + encodeURIComponent(lineNum) +
                "&build=" + encodeURIComponent(buildId) + "\">" + file + ":" + lineNum + "</a>)";
        }

        var renjinLineMatch = renjinLineRe.exec(line);
        if(renjinLineMatch) {
            var indent = renjinLineMatch[1];
            var functionName = renjinLineMatch[2];

            return indent + "<a href=\"/source/redirect/R?" +
                "function=" + encodeURIComponent(functionName) +
                "&build=" + encodeURIComponent(buildId) + "\">" + functionName + "</a>()";
        }

        return escapeHtml(line);
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

    function handleCopyClick(event) {
        if(event.target.classList.contains("copy-log")) {
            event.preventDefault();

            var logDiv = event.target.parentElement;
            var stmts = logDiv.getElementsByClassName("stmt");
            var code = "";
            var i;
            for(i = 0; i < stmts.length; ++i) {
               code += stmts.item(i).innerText + "\n";
            }
            console.log("Code:\n" + code);
            var clipboardTarget = document.getElementById("clipboard-target");
            clipboardTarget.value = code;
            clipboardTarget.select();
            document.execCommand("copy");
        }
    }

    window.addEventListener("load", loadLogsInView);
    window.addEventListener("scroll", loadLogsInView);
    window.addEventListener("click", handleCopyClick);
})();