package au.com.southsky.jfreesane.jscanimage;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import org.jline.builtins.Completers;
import org.jline.reader.Completer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

@Parameters(
  commandNames = "jpeg",
  commandDescription = "convert all of the images in the session to JPEGs"
)
class JpegCommand implements Command {

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
  public void execute(Session session, List<String> parameters) {
    List<BufferedImage> images = session.getImages();
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

    session.clearImages();
  }

  @Override
  public Completer getCompleter(Session session) {
    return new Completers.FileNameCompleter();
  }
}
