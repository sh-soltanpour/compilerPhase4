import java.util.ArrayList;

public class SymbolTableReceiverItem extends SymbolTableItem {
  public SymbolTableReceiverItem(Receiver receiver, int offset) {
    this.receiver = receiver;
    this.offset = offset;
  }

  @Override
  public String getKey() {
    ArrayList <Type> argumentTypes = receiver.getArgumentTypes();
    String name = receiver.getName();
    String key = name+'#';
    for(int i = 0 ; i < argumentTypes.size(); i++){
      key += argumentTypes.get(i).toString(); 
    }
    return key;
  }

  public int getOffset() {
    return offset;
  }

  Receiver receiver;
  int offset;
}