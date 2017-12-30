package au.com.southsky.jfreesane.jscanimage;

import au.com.southsky.jfreesane.SaneDevice;
import com.beust.jcommander.Parameters;

import java.io.IOException;
import java.util.List;

@Parameters(
  commandNames = "close",
  commandDescription = "closes the currently opened device (if any)"
)
class CloseDeviceCommand implements Command {

  @Override
  public void execute(Session session, List<String> parameters) {
    SaneDevice currentDevice = session.getCurrentDevice();

    if (currentDevice == null) {
      System.out.println("There is no currently opened device, use 'ls' and 'open' to open one.");
    } else if (!currentDevice.isOpen()) {
      System.out.println(currentDevice.getName() + " is not open.");
    } else {
      try {
        currentDevice.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
      session.setCurrentDevice(null);
    }
  }
}
