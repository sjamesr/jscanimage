package au.com.southsky.jfreesane.jscanimage;

import au.com.southsky.jfreesane.SaneDevice;
import com.beust.jcommander.Parameters;

import java.awt.image.BufferedImage;
import java.util.List;

@Parameters(commandNames = "status", commandDescription = "show the status of the current session")
class StatusCommand implements Command {

  @Override
  public void execute(Session session, List<String> parameters) {
    System.out.println("Connected to " + session.getHostName());
    SaneDevice currentDevice = session.getCurrentDevice();
    if (currentDevice == null) {
      System.out.println("No current device (use 'ls' and 'open' to open one).");
    } else {
      System.out.println("Current device is " + currentDevice.getName());
      if (!currentDevice.isOpen()) {
        System.out.println("Device is closed (use 'open' to open it).");
      }
    }

    List<BufferedImage> images = session.getImages();
    if (images.isEmpty()) {
      System.out.println("No images in this session (use 'scan' or 'scanadf' to acquire them)");
    } else {
      System.out.println("Images:");
      for (int i = 0; i < images.size(); i++) {
        System.out.println("#" + i + " acquired " + session.getAcquisitionTimes().get(i));
      }
      System.out.println("End of images list.");
    }
  }
}
