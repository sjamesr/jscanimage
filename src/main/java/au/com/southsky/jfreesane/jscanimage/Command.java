package au.com.southsky.jfreesane.jscanimage;

import java.util.List;

interface Command {
  void execute(Session session, List<String> parameters);
}
