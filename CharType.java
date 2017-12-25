public class CharType extends Type {

  public int size() {
    return Type.WORD_BYTES / 4;
  }

  @Override
  public boolean equals(Object other) {
    if (other instanceof CharType)
      return true;
    return false;
  }

  @Override
  public String toString() {
    return "char";
  }

  private static CharType instance;

  public static CharType getInstance() {
    if (instance == null)
      return instance = new CharType();
    return instance;
  }
}