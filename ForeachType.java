public class ForeachType extends Type {
	
	public int size() {
		return Type.WORD_BYTES;
	}

	@Override
	public boolean equals(Object other) {
		if(other instanceof ForeachType)
			return true;
		return false;
	}

	@Override
	public String toString() {
		return "foreach";
	}

	private static ForeachType instance;

	public static ForeachType getInstance() {
		if(instance == null)
			return instance = new ForeachType();
		return instance;
	}
}