package au.com.southsky.jfreesane.jscanimage;

import au.com.southsky.jfreesane.SaneDevice;
import au.com.southsky.jfreesane.SaneException;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import org.jline.reader.Completer;
import org.jline.reader.impl.completer.StringsCompleter;

import java.io.IOException;
import java.util.List;

@Parameters(commandNames = "open", commandDescription = "open the named scanner")
class OpenDeviceCommand implements Command {
  @Parameter(description = "the name of the device to open (use 'ls' to see a list)")
  private List<String> scannerName;

  @Override
  public void execute(Session session, List<String> parameters) {
    if (scannerName.isEmpty()) {
      System.out.println("No scanner name specified.");
      return;
    }

    SaneDevice currentDevice = session.getCurrentDevice();
    try {
      if (currentDevice != null && currentDevice.isOpen()) {
        System.out.println("Closing " + currentDevice.getName() + "...");
        currentDevice.close();
        session.setCurrentDevice(null);
        System.out.println("Closed.");
      }
      System.out.println("Opening " + scannerName.getFirst() + "...");
      currentDevice = session.getSaneSession().getDevice(scannerName.getFirst());
      currentDevice.open();
      System.out.println("Opened, fetching options...");
      currentDevice.listOptions();
      System.out.println("Done!");
      session.setCurrentDevice(currentDevice);
    } catch (IOException | SaneException e) {
      e.printStackTrace();
    }
  }

  @Override
  public Completer getCompleter(Session session) {
    return new StringsCompleter(
        session.getDevicesSeen().stream().map(SaneDevice::getName).toList());
  }
}
