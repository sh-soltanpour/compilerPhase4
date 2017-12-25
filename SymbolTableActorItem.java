public class SymbolTableActorItem extends SymbolTableItem {
  public SymbolTableActorItem(Actor actor, int offset) {
    this.actor = actor;
    this.offset = offset;
  }

  @Override
  public String getKey() {
    return actor.getName();
  }

  public int getOffset() {
    return offset;
  }
  public void setSymbolTable(SymbolTable symbolTable){
    this.symbolTable = symbolTable;
  }
  public SymbolTable getSymbolTable(){
    return symbolTable;
  }
  Actor actor;
  int offset;
  SymbolTable symbolTable;
}