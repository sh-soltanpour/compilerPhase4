public abstract class SymbolTableVariableItemBase extends SymbolTableItem {
	
	public SymbolTableVariableItemBase(Variable variable, int offset) {
		this.variable = variable;
		this.offset = offset;
		this.isLvalue = true;
	}
	public SymbolTableVariableItemBase(Variable variable, int offset, boolean isLvalue) {
		this.variable = variable;
		this.offset = offset;
		this.isLvalue = isLvalue;
	}
	public void isNotLvalue(){
		isLvalue = false;
	}
	public boolean isLvalue(){
		return isLvalue;
	}
	public int getSize() {
		return variable.size();
	}

	public int getOffset() {
		return offset;
	}

	public Variable getVariable() {
		return variable;
	}

	@Override
	public String getKey() {
		return variable.getName();
	}
	

	public abstract Register getBaseRegister();

	int offset;
	Variable variable;
	boolean isLvalue;
}