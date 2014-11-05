package org.renjin.ci.worker;

import org.codehaus.jackson.map.ObjectMapper;
import org.renjin.ci.task.PackageBuildTask;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TaskServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(TaskServlet.class.getName());

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            PackageBuildTask task = objectMapper.readValue(req.getParameter("task"), PackageBuildTask.class);
            PackageBuilder builder = new PackageBuilder(task);
            builder.build();
        } catch(Exception e) {
            LOGGER.log(Level.SEVERE, "Build task failed", e);
        }
    }
}
