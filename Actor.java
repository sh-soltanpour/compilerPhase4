public class Actor {
  public Actor(String name, int mailboxSize) {
    this.name = name;
    this.mailboxSize = mailboxSize;
  }

  public String getName() {
    return this.name;
  }

  public int getMailboxSize() {
    return this.mailboxSize;
  }

  String name;
  int mailboxSize;
}