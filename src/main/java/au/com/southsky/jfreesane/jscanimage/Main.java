package au.com.southsky.jfreesane.jscanimage;

import au.com.southsky.jfreesane.SaneException;
import au.com.southsky.jfreesane.SaneSession;
import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.MissingCommandException;
import com.beust.jcommander.Parameter;
import com.google.common.net.HostAndPort;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.ParsedLine;
import org.jline.reader.UserInterruptException;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Map;

/**
 * The entry point for jscanimage.
 */
public class Main {

  private static Map<String, Command> commands;
  private static Session session;

  public static void main(String[] args) throws IOException, SaneException {
    session = new Session();
    JCommander mainJCommander = new JCommander();
    GlobalOptions o = new GlobalOptions();
    mainJCommander.addObject(o);
    mainJCommander.parse(args);

    if (o.help) {
      mainJCommander.usage();
      System.exit(0);
    }

    session.setMainJCommander(mainJCommander);

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

    LineReader reader = LineReaderBuilder.builder().build();
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
        mainJCommander = initializeJCommanderWithCommands();
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
        mainJCommander.parse(parsedLine.words().toArray(new String[0]));
      } catch (MissingCommandException e) {
        System.out.println(
            "Command '" + e.getUnknownCommand() + "' not found (try 'help' for help).");
        continue;
      }

      JCommander jc = mainJCommander.getCommands().get(mainJCommander.getParsedCommand());
      if (jc.getObjects().size() != 1) {
        throw new IllegalStateException("expected exactly one JCommander object");
      }
      if (!(jc.getObjects().get(0) instanceof Command)) {
        throw new IllegalStateException("expected JCommander.getObject(0) to be of type Command");
      }
      Command c = (Command) jc.getObjects().get(0);
      c.execute(session, parsedLine.words());
    } while (true);

    System.out.println("Closing session...");
    session.close();
    System.out.println("Done! Goodbye.");
  }

  private static boolean userReallyWantsToQuit() {
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
    result.addCommand("close", new CloseDeviceCommand());
    result.addCommand("scan", new ScanCommand());
    result.addCommand("jpeg", new JpegCommand());
    result.addCommand("status", new StatusCommand());
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
}
