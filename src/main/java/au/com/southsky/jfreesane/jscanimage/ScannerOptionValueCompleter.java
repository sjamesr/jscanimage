package au.com.southsky.jfreesane.jscanimage;

import au.com.southsky.jfreesane.OptionValueConstraintType;
import au.com.southsky.jfreesane.OptionValueType;
import au.com.southsky.jfreesane.SaneOption;
import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;
import org.jline.reader.impl.completer.StringsCompleter;

import java.io.IOException;
import java.util.List;

public class ScannerOptionValueCompleter implements Completer {
  private final Session session;

  public ScannerOptionValueCompleter(Session session) {
    this.session = session;
  }

  @Override
  public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
    if (line.words().size() < 2) {
      return;
    }

    if (session.getCurrentDevice() == null) {
      return;
    }

    SaneOption saneOption;
    try {
      saneOption = session.getCurrentDevice().getOption(line.words().get(1));
    } catch (IOException e) {
      return;
    }

    if (saneOption == null) {
      return;
    }

    if (saneOption.getType() == OptionValueType.BOOLEAN) {
      new StringsCompleter("yes", "no").complete(reader, line, candidates);
      return;
    }

    if (saneOption.getType() == OptionValueType.STRING && saneOption.getConstraintType() == OptionValueConstraintType.STRING_LIST_CONSTRAINT) {
      new StringsCompleter(saneOption.getStringConstraints()).complete(reader, line, candidates);
      return;
    }
  }
}
