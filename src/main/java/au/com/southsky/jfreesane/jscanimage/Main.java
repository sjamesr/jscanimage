package au.com.southsky.jfreesane.jscanimage;

import au.com.southsky.jfreesane.SaneException;
import au.com.southsky.jfreesane.SaneSession;
import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.MissingCommandException;
import com.beust.jcommander.Parameter;
import com.google.common.net.HostAndPort;
import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import org.jline.reader.*;
import org.jline.reader.impl.completer.StringsCompleter;

/**
 * The entry point for jscanimage.
 */
public class Main {

  private Session session;

  public static void main(String[] args) throws IOException, SaneException {
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
    session.setMainJCommander(initializeJCommanderWithCommands());
    LineReader reader = LineReaderBuilder.builder().completer(new SessionCompleter()).build();

    String rawLine;
    do {
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
        session.getMainJCommander().parse(parsedLine.words().toArray(new String[0]));
      } catch (MissingCommandException e) {
        System.out.println(
            "Command '" + e.getUnknownCommand() + "' not found (try 'help' for help).");
        continue;
      }

      JCommander jc = session.getMainJCommander()
          .getCommands()
          .get(session.getMainJCommander().getParsedCommand());
      if (jc.getObjects().size() != 1) {
        throw new IllegalStateException("expected exactly one JCommander object");
      }
      if (!(jc.getObjects().getFirst() instanceof Command c)) {
        throw new IllegalStateException("expected JCommander.getObject(0) to be of type Command");
      }
      c.execute(session, parsedLine.words());
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
    result.addCommand("help", new HelpCommand());
    result.addCommand("quit", new QuitCommand());
    result.addCommand("open", new OpenDeviceCommand());
    result.addCommand("ls", new ListScannersCommand());
    result.addCommand("options", new OptionsCommand());
    result.addCommand("close", new CloseDeviceCommand());
    result.addCommand("scan", new ScanCommand());
    result.addCommand("jpeg", new JpegCommand());
    result.addCommand("status", new StatusCommand());
    result.addCommand("auth", new AuthCommand());
    return result;
  }

  private static class GlobalOptions {
    @Parameter(
      names = {"-s", "--sane-server"},
      converter = HostAndPortConverter.class
    )
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
      commandNameCompleter = new StringsCompleter(session.getMainJCommander().getCommands().keySet());
    }

    @Override
    public void complete(LineReader lineReader, ParsedLine parsedLine, List<Candidate> list) {
      JCommander c = session.getMainJCommander()
              .getCommands()
              .get(parsedLine.words().getFirst());
      if (c != null && c.getObjects().size() == 1 && c.getObjects().getFirst() instanceof Command command) {
        command.getCompleter(session).complete(lineReader, parsedLine, list);
      } else {
        commandNameCompleter.complete(lineReader, parsedLine, list);
      }
    }
  }
}
