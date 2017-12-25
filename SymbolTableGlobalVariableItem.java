public class SymbolTableGlobalVariableItem extends SymbolTableVariableItemBase {

  public SymbolTableGlobalVariableItem(Variable variable, int offset) {
    super(variable, offset);
  }

  @Override
  public Register getBaseRegister() {
    return Register.GP;
  }

  @Override
  public boolean useMustBeComesAfterDef() {
    return false;
  }
}