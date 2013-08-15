package org.renjin.cran;


import io.airlift.command.Cli;
import io.airlift.command.Help;

public class Main {

  public static void main(String[] args) {

    Cli.CliBuilder<Runnable> builder = Cli.<Runnable>builder("renjin-repo")
      .withDescription("Renjin Repo Build Tool")
      .withDefaultCommand(Help.class)
      .withCommands(Help.class, BuildCommand.class);

    Cli<Runnable> gitParser = builder.build();

    gitParser.parse(args).run();
  }
}
