import java.util.*;
public class Receiver {
  public Receiver(String name, ArrayList<Type> argumentTypes) {
    this.name = name;
    this.argumentTypes = argumentTypes;
  }

  public String getName() {
    return name;
  }

  public ArrayList<Type> getArgumentTypes() {
    return argumentTypes;
  }

  String name;
  ArrayList<Type> argumentTypes;
}