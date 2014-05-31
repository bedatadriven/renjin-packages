package org.renjin.build.agent;


import io.airlift.command.Cli;
import io.airlift.command.Help;
import org.renjin.build.agent.build.BuildCommand;

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
