package au.com.southsky.jfreesane.jscanimage;

import au.com.southsky.jfreesane.*;
import com.beust.jcommander.Parameters;
import com.google.common.base.Joiner;
import java.io.IOException;
import java.util.List;

/** Lists options for the current device. */
@Parameters(commandNames = "options", commandDescription = "list device options")
public class OptionsCommand implements Command {
  @Override
  public void execute(Session session, List<String> parameters) {
    SaneDevice device = session.getCurrentDevice();
    if (device == null) {
      System.out.println("No device is open, use 'ls' and 'open' to open one.");
      return;
    }

    try {
      List<SaneOption> options = device.listOptions();

      for (SaneOption option : options) {
        if ("".equals(option.getName())) {
          continue;
        }

        System.out.println(
            option.getName()
                + " ("
                + printOptionValues(option)
                + ")\n\t"
                + option.getDescription());
        System.out.println(printValidValues(option) + "\n");
      }
    } catch (IOException | SaneException e) {
      System.out.println("Could not list options: " + e.getMessage());
    }
  }

  private String printOptionValues(SaneOption option) throws SaneException, IOException {
    if (!option.isActive()) {
      return "";
    }
    switch (option.getType()) {
      case STRING:
        return option.getStringValue();
      case INT:
        if (option.getValueCount() == 1) {
          return "" + option.getIntegerValue();
        } else {
          return Joiner.on(",").join(option.getIntegerArrayValue());
        }
      case FIXED:
        if (option.getValueCount() == 1) {
          return "" + option.getFixedValue();
        } else {
          return Joiner.on(",").join(formatDoubles(option.getFixedArrayValue()));
        }
      case BOOLEAN:
        return option.getBooleanValue() ? "yes" : "no";
      default:
        return "";
    }
  }

  private String printValidValues(SaneOption option) {
    String result = "";
    if (option.getType() == OptionValueType.BOOLEAN) {
      return "\n\tValid values are 'yes' or 'no'";
    }
    switch (option.getConstraintType()) {
      case STRING_LIST_CONSTRAINT:
        result += "\n\tValid values are: " + Joiner.on("|").join(option.getStringConstraints());
        break;
      case RANGE_CONSTRAINT:
        result += "\n\tValid values are " + renderRangeConstraint(option);
        break;
      case VALUE_LIST_CONSTRAINT:
        result += "\n\tValid values are " + renderValueListConstraint(option);
    }
    return result;
  }

  private String renderRangeConstraint(SaneOption option) {
    RangeConstraint constraint = option.getRangeConstraints();
    String units = renderUnits(option.getUnits());
    switch (option.getType()) {
      case INT:
        {
          int step = constraint.getQuantumInteger();
          return "from "
              + constraint.getMinimumInteger()
              + " to "
              + constraint.getMaximumInteger()
              + units
              + (step != 0 ? (" in steps of " + step + units) : "");
        }
      case FIXED:
        {
          double step = constraint.getQuantumFixed();
          return "from "
              + formatDouble(constraint.getMinimumFixed())
              + " to "
              + formatDouble(constraint.getMaximumFixed())
              + units
              + (step != 0 ? (" in steps of " + formatDouble(step) + units) : "");
        }
    }
    return "";
  }

  private String renderValueListConstraint(SaneOption option) {
    String units = renderUnits(option.getUnits());
    return switch (option.getType()) {
      case INT -> Joiner.on("|").join(option.getIntegerValueListConstraint()) + units;
      case FIXED -> Joiner.on("|").join(formatDoubles(option.getFixedValueListConstraint())) + units;
      default -> "";
    };
  }

  private String renderUnits(SaneOption.OptionUnits units) {
    return switch (units) {
      case UNIT_MM -> "mm";
      case UNIT_DPI -> "dpi";
      case UNIT_BIT -> "bits";
      case UNIT_MICROSECOND -> "µs";
      case UNIT_PERCENT -> "%";
      default -> "";
    };
  }

  private List<String> formatDoubles(List<Double> doubles) {
    return doubles.stream().map(d -> String.format("%3.3f", d)).toList();
  }

  private String formatDouble(Double d) {
    return String.format("%3.3f", d);
  }
}
