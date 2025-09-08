package au.com.southsky.jfreesane.jscanimage;

import au.com.southsky.jfreesane.SaneException;
import au.com.southsky.jfreesane.SaneSession;
import com.beust.jcommander.*;
import com.google.common.base.StandardSystemProperty;
import com.google.common.net.HostAndPort;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Supplier;
import org.jline.reader.*;
import org.jline.reader.impl.completer.StringsCompleter;

/** The entry point for jscanimage. */
public class Main {
  private static final Map<String, Supplier<Command>> commands;

  static {
    commands = new LinkedHashMap<>();
    commands.put("auth", AuthCommand::new);
    commands.put("close", CloseDeviceCommand::new);
    commands.put("help", HelpCommand::new);
    commands.put("jpeg", JpegCommand::new);
    commands.put("ls", ListScannersCommand::new);
    commands.put("open", OpenDeviceCommand::new);
    commands.put("options", OptionsCommand::new);
    commands.put("quit", QuitCommand::new);
    commands.put("scan", ScanCommand::new);
    commands.put("set", SetCommand::new);
    commands.put("status", StatusCommand::new);
  }

  private final Session session;

  public static void main(String[] args) throws IOException {
    Session session = new Session();
    JCommander globalOptionsParser = new JCommander();
    GlobalOptions o = new GlobalOptions();
    globalOptionsParser.addObject(o);
    globalOptionsParser.parse(args);

    if (o.help) {
      globalOptionsParser.usage();
      System.exit(0);
    }

    System.out.println("Connecting to " + o.saneServer + "...");
    InetAddress addr = InetAddress.getByName(o.saneServer.getHost());
    SaneSession saneSession;
    if (o.saneServer.hasPort()) {
      saneSession = SaneSession.withRemoteSane(addr, o.saneServer.getPort());
    } else {
      saneSession = SaneSession.withRemoteSane(addr);
    }
    session.setHostName(o.saneServer.toString());
    session.setSaneSession(saneSession);
    System.out.println("Connected.");

    new Main(session).beginInteractiveSession();
  }

  private Main(Session session) {
    this.session = session;
  }

  public void beginInteractiveSession() throws IOException {
    LineReader reader = null;

    do {
      String rawLine;
      JCommander commander = initializeJCommanderWithCommands();
      session.setMainJCommander(commander);

      if (reader == null) {
        reader =
            LineReaderBuilder.builder()
                .variable(
                    LineReader.HISTORY_FILE,
                    Path.of(
                            System.getProperty(StandardSystemProperty.USER_HOME.key()),
                            ".jscanimage.history")
                        .toAbsolutePath())
                .completer(new SessionCompleter())
                .build();
      }
      if (session.shouldQuit()) {
        break;
      }

      String prompt =
          session.getHostName()
              + (session.getCurrentDevice() != null
                  ? (":" + session.getCurrentDevice().getName())
                  : "")
              + "> ";
      try {
        rawLine = reader.readLine(prompt);
      } catch (EndOfFileException | UserInterruptException e) {
        if (userReallyWantsToQuit()) {
          break;
        } else {
          continue;
        }
      }

      session.setWarnedUser(false);
      if (rawLine.isEmpty()) {
        continue;
      }

      ParsedLine parsedLine = reader.getParsedLine();
      if (parsedLine == null) {
        break;
      }
      try {
        String[] command =
            parsedLine.words().stream().filter(s -> !s.isEmpty()).toArray(String[]::new);
        commander.parse(command);
      } catch (MissingCommandException e) {
        System.out.println(
            "Command '" + e.getUnknownCommand() + "' not found (try 'help' for help).");
        continue;
      } catch (ParameterException e) {
        System.out.println("Error running command: " + e.getMessage());
        continue;
      }

      JCommander jc = commander.getCommands().get(session.getMainJCommander().getParsedCommand());
      if (jc == null) {
        continue;
      }
      if (jc.getObjects().size() != 1) {
        throw new IllegalStateException("expected exactly one JCommander object");
      }
      if (!(jc.getObjects().getFirst() instanceof Command c)) {
        throw new IllegalStateException("expected JCommander.getObject(0) to be of type Command");
      }
      c.execute(session, parsedLine.words());

      commander = initializeJCommanderWithCommands();
      session.setMainJCommander(commander);
    } while (true);

    System.out.println("Closing session...");
    session.close();
    System.out.println("Done! Goodbye.");
  }

  private boolean userReallyWantsToQuit() {
    if (!session.getImages().isEmpty() && !session.didWarnUser()) {
      System.out.println(
          session.getImages().size() + " image(s) will be lost, interrupt again to confirm.");
      session.setWarnedUser(true);
      return false;
    }
    return true;
  }

  private static JCommander initializeJCommanderWithCommands() {
    JCommander result = new JCommander();
    for (Map.Entry<String, Supplier<Command>> command : commands.entrySet()) {
      result.addCommand(command.getKey(), command.getValue().get());
    }
    return result;
  }

  private static class GlobalOptions {
    @Parameter(
        names = {"-s", "--sane-server"},
        converter = HostAndPortConverter.class)
    HostAndPort saneServer = HostAndPort.fromHost("localhost");

    @Parameter(names = {"-h", "--help"})
    boolean help = false;
  }

  private static class HostAndPortConverter implements IStringConverter<HostAndPort> {
    @Override
    public HostAndPort convert(String value) {
      return HostAndPort.fromString(value);
    }
  }

  private class SessionCompleter implements Completer {

    private final Completer commandNameCompleter;

    public SessionCompleter() {
      commandNameCompleter =
          new StringsCompleter(session.getMainJCommander().getCommands().keySet());
    }

    @Override
    public void complete(LineReader lineReader, ParsedLine parsedLine, List<Candidate> list) {
      JCommander c = session.getMainJCommander().getCommands().get(parsedLine.words().getFirst());
      if (c != null
          && c.getObjects().size() == 1
          && c.getObjects().getFirst() instanceof Command command) {
        command.getCompleter(session).complete(lineReader, parsedLine, list);
      } else {
        commandNameCompleter.complete(lineReader, parsedLine, list);
      }
    }
  }
}
