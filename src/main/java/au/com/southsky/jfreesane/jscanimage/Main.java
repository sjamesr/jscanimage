package au.com.southsky.jfreesane.jscanimage;

import au.com.southsky.jfreesane.RateLimitingScanListeners;
import au.com.southsky.jfreesane.SaneDevice;
import au.com.southsky.jfreesane.SaneException;
import au.com.southsky.jfreesane.SaneSession;
import au.com.southsky.jfreesane.ScanListenerAdapter;
import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.MissingCommandException;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.common.net.HostAndPort;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.ParsedLine;
import org.jline.reader.UserInterruptException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * The entry point for jscanimage.
 */
public class Main {

  private static JCommander mainJCommander;
  private static Map<String, Command> commands;
  private static boolean warnedUser = false;
  private static boolean shouldQuit = false;
  private static String hostName;
  private static SaneSession session;
  private static SaneDevice currentDevice;
  private static List<BufferedImage> images = new ArrayList<>();
  private static List<Instant> acquisitionTimes = new ArrayList<>();

  public static void main(String[] args) throws IOException, SaneException {
    mainJCommander = new JCommander();
    GlobalOptions o = new GlobalOptions();
    mainJCommander.addObject(o);
    mainJCommander.parse(args);

    if (o.help) {
      mainJCommander.usage();
      System.exit(0);
    }

    System.out.println("Connecting to " + o.saneServer + "...");
    hostName = o.saneServer.toString();
    InetAddress addr = InetAddress.getByName(o.saneServer.getHost());
    if (o.saneServer.hasPort()) {
      session = SaneSession.withRemoteSane(addr, o.saneServer.getPort());
    } else {
      session = SaneSession.withRemoteSane(addr);
    }
    System.out.println("Connected.");

    LineReader reader = LineReaderBuilder.builder().build();
    String rawLine;
    do {
      if (shouldQuit) {
        break;
      }

      String prompt =
          hostName + (currentDevice != null ? (":" + currentDevice.getName()) : "") + "> ";
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

      warnedUser = false;
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
      c.execute(parsedLine.words());
    } while (true);

    System.out.println("Closing session...");
    session.close();
    System.out.println("Done! Goodbye.");
  }

  private static boolean userReallyWantsToQuit() {
    if (!images.isEmpty() && !warnedUser) {
      System.out.println(images.size() + " image(s) will be lost, interrupt again to confirm.");
      warnedUser = true;
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

  @Parameters(commandNames = "help", commandDescription = "prints this handy help message")
  private static class HelpCommand implements Command {
    @Override
    public void execute(List<String> parameters) {
      for (String commandName : mainJCommander.getCommands().keySet()) {
        System.out.println(commandName + "\t");
        mainJCommander.usage(commandName);
      }
    }
  }

  @Parameters(commandNames = "quit", commandDescription = "close the session and quit")
  private static class QuitCommand implements Command {
    @Override
    public void execute(List<String> parameters) {
      shouldQuit = true;
    }
  }

  @Parameters(commandNames = "ls", commandDescription = "list scanners attached to this host")
  private static class ListScannersCommand implements Command {

    @Override
    public void execute(List<String> parameters) {
      System.out.println("Getting device list...");
      try {
        List<SaneDevice> devices = session.listDevices();
        if (devices.isEmpty()) {
          System.out.println("No devices found.");
        } else {
          for (SaneDevice device : devices) {
            System.out.println(
                device.getName()
                    + " ("
                    + device.getVendor()
                    + " "
                    + device.getModel()
                    + " "
                    + device.getType()
                    + ")");
          }
          System.out.println("End of device list.");
        }
      } catch (IOException | SaneException e) {
        e.printStackTrace();
      }
    }
  }

  @Parameters(commandNames = "open", commandDescription = "open the named scanner")
  private static class OpenDeviceCommand implements Command {
    @Parameter(description = "the name of the device to open (use 'ls' to see a list)")
    private List<String> scannerName;

    @Override
    public void execute(List<String> parameters) {
      if (scannerName.isEmpty()) {
        System.out.println("No scanner name specified.");
      }
      try {
        if (currentDevice != null && currentDevice.isOpen()) {
          System.out.println("Closing " + currentDevice.getName() + "...");
          currentDevice.close();
          System.out.println("Closed.");
        }
        System.out.println("Opening " + scannerName + "...");
        currentDevice = session.getDevice(scannerName.get(0));
        currentDevice.open();
        System.out.println("Opened.");
      } catch (IOException | SaneException e) {
        e.printStackTrace();
      }
    }
  }

  @Parameters(
    commandNames = "close",
    commandDescription = "closes the currently opened device (if any)"
  )
  private static class CloseDeviceCommand implements Command {

    @Override
    public void execute(List<String> parameters) {
      if (currentDevice == null) {
        System.out.println("There is no currently opened device, use 'ls' and 'open' to open one.");
      } else if (!currentDevice.isOpen()) {
        System.out.println(currentDevice.getName() + " is not open.");
      }

      try {
        currentDevice.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
      currentDevice = null;
    }
  }

  @Parameters(
    commandNames = "scan",
    commandDescription = "acquire an image and add it to the current session"
  )
  private static class ScanCommand implements Command {

    @Override
    public void execute(List<String> parameters) {
      if (currentDevice == null) {
        System.out.println("There is no currently opened device, use 'ls' and 'open' to open one.");
      } else if (!currentDevice.isOpen()) {
        System.out.println("Opening " + currentDevice);
        try {
          currentDevice.open();
        } catch (IOException | SaneException e) {
          e.printStackTrace();
        }
      } else {
        try {
          images.add(
              currentDevice.acquireImage(
                  RateLimitingScanListeners.noMoreFrequentlyThan(
                      new ScanListenerAdapter() {
                        @Override
                        public void recordRead(
                            SaneDevice device, int totalBytesRead, int imageSize) {
                          if (imageSize > 0) {
                            System.out.println(
                                String.format(
                                    "Acquiring %2.2f%% done",
                                    (totalBytesRead / (double) imageSize) * 100));
                          }
                        }

                        @Override
                        public void scanningFinished(SaneDevice device) {
                          System.out.println("Done!");
                        }
                      },
                      200,
                      TimeUnit.MILLISECONDS)));
          acquisitionTimes.add(Instant.now());
          currentDevice.cancel();
        } catch (IOException | SaneException e) {
          e.printStackTrace();
        }
      }
    }
  }

  @Parameters(
    commandNames = "jpeg",
    commandDescription = "convert all of the images in the session to JPEGs"
  )
  private static class JpegCommand implements Command {

    @Parameter(
      names = {"-o", "--out-dir"},
      description = "The directory to which output will be written"
    )
    private String outDir = System.getProperty("user.dir");

    @Parameter(
      names = {"-n", "--name"},
      description = "The file name template to use"
    )
    private String outputNameTemplate = "image-%04d.jpg";

    @Override
    public void execute(List<String> parameters) {
      if (images.isEmpty()) {
        System.out.println("No images yet, use the 'scan' or 'scanadf' commands to get some.");
      } else {
        int i = 0;
        for (BufferedImage img : images) {
          File outputFile =
              new File(outDir, String.format(outputNameTemplate, i, Locale.getDefault()));
          try {
            ImageIO.write(img, "JPEG", outputFile);
            System.out.println("Wrote " + outputFile.getAbsolutePath());
          } catch (IOException e) {
            System.out.println("Could not write image " + i + " to " + outputFile);
            e.printStackTrace();
            break;
          }
          i++;
        }
      }

      images.clear();
      acquisitionTimes.clear();
    }
  }

  @Parameters(
    commandNames = "status",
    commandDescription = "show the status of the current session"
  )
  private static class StatusCommand implements Command {

    @Override
    public void execute(List<String> parameters) {
      System.out.println("Connected to " + hostName);
      if (currentDevice == null) {
        System.out.println("No current device (use 'ls' and 'open' to open one).");
      } else {
        System.out.println("Current device is " + currentDevice.getName());
        if (!currentDevice.isOpen()) {
          System.out.println("Device is closed (use 'open' to open it).");
        }
      }

      if (images.isEmpty()) {
        System.out.println("No images in this session (use 'scan' or 'scanadf' to acquire them)");
      } else {
        System.out.println("Images:");
        for (int i = 0; i < images.size(); i++) {
          System.out.println("#" + i + " acquired " + acquisitionTimes.get(i));
        }
        System.out.println("End of images list.");
      }
    }
  }

  private interface Command {
    void execute(List<String> parameters);
  }
}
