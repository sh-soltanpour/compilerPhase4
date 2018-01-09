public class SymbolTableForeachItem extends SymbolTableLocalVariableItem {
  public SymbolTableForeachItem(Variable variable, int offset) {
    super(variable, offset, false); 
    this.offset = offset;
  }
  @Override
  public String getKey() {
    return this.variable.getName();
  }
  public int getOffset() {
    return offset;
  }
  public void setType(Type type){
    this.type = type;
  }
  public void setTraversingArrayOffset(int offset){
    this.traversingArrayOffset = offset;
  }
  public int getTraversingArrayOffset(){
    return traversingArrayOffset;
  }
  public Type getType(){
    return type;
  }
  int offset;
  Type type; 
  int traversingArrayOffset;
  
}