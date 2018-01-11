package au.com.southsky.jfreesane.jscanimage;

import au.com.southsky.jfreesane.SanePasswordProvider;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import java.util.List;

@Parameters(
  commandNames = "auth",
  commandDescription = "use the given username and password authentication"
)
public class AuthCommand implements Command {
  @Parameter(description = "<user> <password>")
  List<String> userAndPassword;

  @Override
  public void execute(Session session, List<String> parameters) {
    if (userAndPassword.size() != 2) {
      System.out.println("You must specify both a username and a password.");
      return;
    }

    session
        .getSaneSession()
        .setPasswordProvider(
            SanePasswordProvider.forUsernameAndPassword(
                userAndPassword.get(0), userAndPassword.get(1)));
  }
}
