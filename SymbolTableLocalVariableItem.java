public class SymbolTableLocalVariableItem extends SymbolTableVariableItemBase {
	
	public SymbolTableLocalVariableItem(Variable variable, int offset) {
		super(variable, offset);
	}
	public SymbolTableLocalVariableItem(Variable variable, int offset, boolean isLvalue) {
		super(variable, offset,isLvalue);
	}
	@Override
	public Register getBaseRegister() {
		return Register.SP;
	}

	@Override
	public boolean useMustBeComesAfterDef() {
		return true;
	}
}