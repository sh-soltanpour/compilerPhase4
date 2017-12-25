public class ArrayType extends Type {
	ArrayType (Type type , int width ){
        this.type = type;
        this.width = width;
    }
	public int size() {
		return width * type.size();
	}
	@Override
	public boolean equals(Object other) {
		if(other instanceof ArrayType && type.equals(((ArrayType)other).getType()) && ((ArrayType) other).getWidth() == width)
			return true;
		return false;
	}

	@Override
	public String toString() {
		return String.valueOf(width) +"ArrayOf " + type.toString();
    }
    public Type getType(){
        return type;
	}
	public int getWidth(){
		return width;
	}


    Type type;
    int width;

}