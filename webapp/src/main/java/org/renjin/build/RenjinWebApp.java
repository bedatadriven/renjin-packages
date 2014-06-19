package org.renjin.build;

import com.sun.jersey.api.core.DefaultResourceConfig;
import org.renjin.build.util.FreemarkerViewProcessor;

import java.util.logging.Logger;


public class RenjinWebApp extends DefaultResourceConfig {

  private static final Logger LOGGER = Logger.getLogger(RenjinWebApp.class.getName());

  public RenjinWebApp() {
    super(RootResources.class, FreemarkerViewProcessor.class);

    LOGGER.info("Starting RenjinWebApp...");

  }

}
