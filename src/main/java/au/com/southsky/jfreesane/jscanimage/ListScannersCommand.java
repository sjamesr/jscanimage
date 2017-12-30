package au.com.southsky.jfreesane.jscanimage;

import au.com.southsky.jfreesane.SaneDevice;
import au.com.southsky.jfreesane.SaneException;
import com.beust.jcommander.Parameters;

import java.io.IOException;
import java.util.List;

@Parameters(commandNames = "ls", commandDescription = "list scanners attached to this host")
class ListScannersCommand implements Command {

  @Override
  public void execute(Session session, List<String> parameters) {
    System.out.println("Getting device list...");
    try {
      List<SaneDevice> devices = session.getSaneSession().listDevices();
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
