package au.com.southsky.jfreesane.jscanimage;

import au.com.southsky.jfreesane.SaneDevice;
import au.com.southsky.jfreesane.SaneException;
import au.com.southsky.jfreesane.SaneOption;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.common.base.Joiner;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.jline.reader.Completer;
import org.jline.reader.impl.completer.ArgumentCompleter;
import org.jline.reader.impl.completer.StringsCompleter;

@Parameters(commandNames = "set", commandDescription = "set scanner options")
public class SetCommand implements Command {

  @Parameter(description = "the option name and desired value")
  private List<String> optionNames = new ArrayList<>();

  @Override
  public void execute(Session session, List<String> parameters) {
    if (parameters.size() < 3) {
      System.out.println("Insufficient parameters for set command, need at least 2");
      return;
    }

    SaneDevice device = session.getCurrentDevice();
    if (device == null) {
      System.out.println("There is no currently opened device, use 'ls' and 'open' to open one.");
      return;
    } else if (!device.isOpen()) {
      System.out.println("Opening " + device);
      try {
        device.open();
      } catch (IOException | SaneException e) {
        System.out.println("Could not open device: " + e.getMessage());
        return;
      }
    }

    String optionName = parameters.get(1);
    SaneOption option;
    try {
      option = device.getOption(optionName);

      if (option == null) {
        System.out.println("Option " + optionName + " not found.");
        return;
      }
    } catch (IOException e) {
      System.out.println("Could not get option " + optionName + ": " + e.getMessage());
      return;
    }

    if (option.getValueCount() != 1) {
      System.out.println("Only single-valued options are currently supported");
    }

    String value = Joiner.on(" ").join(parameters.subList(2, parameters.size())).trim();
    try {
      switch (option.getType()) {
        case STRING:
          {
            String newValue = option.setStringValue(value);
            if (!newValue.equals(value)) {
              System.out.println(
                  "The operation was successful, but the server chose '"
                      + newValue
                      + "' instead of '"
                      + value
                      + "'");
            }
            break;
          }
        case INT:
          {
            try {
              int desiredValue = Integer.parseInt(value);
              int actualNewValue = option.setIntegerValue(desiredValue);

              if (desiredValue != actualNewValue) {
                System.out.println(
                    "The operation was successful, but the server chose "
                        + actualNewValue
                        + " instead of "
                        + desiredValue);
              }
            } catch (NumberFormatException nfe) {
              System.out.println("Could not interpret '" + value + "' as in integer");
            }
            break;
          }
        case FIXED:
          {
            try {
              double desiredValue = Double.parseDouble(value);
              double actualNewValue = option.setFixedValue(desiredValue);

              if (desiredValue != actualNewValue) {
                System.out.println(
                    "The operation was successful, but the server chose "
                        + actualNewValue
                        + " instead of "
                        + desiredValue);
              }
            } catch (NumberFormatException nfe) {
              System.out.println("Could not interpret '" + value + "' as in integer");
            }
            break;
          }
        case BOOLEAN:
          {
            String param = value.toLowerCase();
            boolean desiredValue;
            if (param.startsWith("yes")) {
              desiredValue = true;
            } else if (value.startsWith("no")) {
              desiredValue = false;
            } else {
              desiredValue = Boolean.parseBoolean(value);
            }
            boolean actualNewValue = option.setBooleanValue(desiredValue);

            if (desiredValue != actualNewValue) {
              System.out.println(
                  "The operation was successful, but the server chose "
                      + actualNewValue
                      + " instead of "
                      + desiredValue);
            }
            break;
          }
        default:
          System.out.println("Option type " + option.getType() + " is not currently supported");
      }
    } catch (IOException | SaneException e) {
      System.out.println("Could not set option value: " + e.getMessage());
    }

    System.out.println("Done.");
  }

  @Override
  public Completer getCompleter(Session session) {
    return new ArgumentCompleter(
        new StringsCompleter("set"),
        new ScannerOptionNameCompleter(session),
        new ScannerOptionValueCompleter(session));
  }
}
