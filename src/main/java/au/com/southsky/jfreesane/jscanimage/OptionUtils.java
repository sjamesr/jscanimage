package au.com.southsky.jfreesane.jscanimage;

import au.com.southsky.jfreesane.SaneOption;

import java.util.List;

class OptionUtils {
  static String renderUnits(SaneOption.OptionUnits units) {
    return switch (units) {
      case UNIT_MM -> "mm";
      case UNIT_DPI -> "dpi";
      case UNIT_BIT -> "bits";
      case UNIT_MICROSECOND -> "µs";
      case UNIT_PERCENT -> "%";
      default -> "";
    };
  }

  static List<String> formatDoubles(List<Double> doubles) {
    return doubles.stream().map(d -> String.format("%3.3f", d)).toList();
  }

  static String formatDouble(Double d) {
    return String.format("%3.3f", d);
  }
}
