package au.com.southsky.jfreesane.jscanimage;

import au.com.southsky.jfreesane.RateLimitingScanListeners;
import au.com.southsky.jfreesane.SaneDevice;
import au.com.southsky.jfreesane.SaneException;
import au.com.southsky.jfreesane.ScanListenerAdapter;
import com.beust.jcommander.Parameters;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Parameters(
  commandNames = "scan",
  commandDescription = "acquire an image and add it to the current session"
)
class ScanCommand implements Command {

  @Override
  public void execute(Session session, List<String> parameters) {
    SaneDevice currentDevice = session.getCurrentDevice();
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
        session.addImage(
            currentDevice.acquireImage(
                RateLimitingScanListeners.noMoreFrequentlyThan(
                    new ScanListenerAdapter() {
                      @Override
                      public void recordRead(SaneDevice device, int totalBytesRead, int imageSize) {
                        if (imageSize > 0) {
                          System.out.printf(
                                  "Acquiring %2.2f%% done%n",
                              (totalBytesRead / (double) imageSize) * 100);
                        }
                      }

                      @Override
                      public void scanningFinished(SaneDevice device) {
                        System.out.println("Done!");
                      }
                    },
                    200,
                    TimeUnit.MILLISECONDS)));
        currentDevice.cancel();
      } catch (IOException | SaneException e) {
        e.printStackTrace();
      }
    }
  }
}
