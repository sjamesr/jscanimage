package au.com.southsky.jfreesane.jscanimage;

import au.com.southsky.jfreesane.SaneDevice;
import au.com.southsky.jfreesane.SaneException;
import com.beust.jcommander.Parameters;
import java.io.IOException;
import java.util.List;
import org.jline.reader.Completer;
import org.jline.reader.impl.completer.ArgumentCompleter;
import org.jline.reader.impl.completer.StringsCompleter;

@Parameters(commandNames = "ls", commandDescription = "list scanners attached to this host")
class ListScannersCommand implements Command {

  @Override
  public void execute(Session session, List<String> parameters) {
    System.out.println("Getting device list...");
    try {
      List<SaneDevice> devices = session.getSaneSession().listDevices();
      session.clearDevicesSeen();
      if (devices.isEmpty()) {
        System.out.println("No devices found.");
      } else {
        for (SaneDevice device : devices) {
          session.addSeenDevice(device);
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

  @Override
  public Completer getCompleter(Session session) {
    return new ArgumentCompleter(
        new StringsCompleter("ls"), new ArgumentCompleter(new StringsCompleter("foo")));
  }
}
