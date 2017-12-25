import java.util.ArrayList;

public class Tools {
  static ArrayList<String> messages = new ArrayList<String> ();
  static boolean codeIsValid = true;
  static boolean oneActorDefined = false;
  static boolean pass2Error = false;
  static public void printMessages(){
		for(int i = 0 ; i < messages.size() ; i++){
			System.out.println(messages.get(i));
		}
	}
  static public boolean putActor(String name, int mailboxSize )  {
    boolean error = false;
    try {
      SymbolTable.top.put(new SymbolTableActorItem(new Actor(name, mailboxSize), SymbolTable.top.getOffset(Register.GP)));
    } catch (ItemAlreadyExistsException e) {
      error = true;
      Tools.putActor(name+"_temp_", mailboxSize);
    }
    return error;
  }
  
  static public boolean putReceiver(String name, ArrayList <Type> arguments)  {
    boolean error = false;
    try {
      SymbolTable.top.put(new SymbolTableReceiverItem(new Receiver(name, arguments), SymbolTable.top.getOffset(Register.SP)));    } 
      catch (ItemAlreadyExistsException e) {
      error = true;
      Tools.putReceiver(name+"_temp_", arguments);
    }
    return error;
  }
 static public boolean putLocalVar(String name, Type type){
    boolean error = false;
    try{
      SymbolTable.top.put(
              new SymbolTableLocalVariableItem(
                  new Variable(name, type),
                  SymbolTable.top.getOffset(Register.SP)
              )
          );
    }
    catch (ItemAlreadyExistsException e) {
      error = true;
      Tools.putLocalVar(name+"_temp_", type);
    }
    return error;
  }
  static public void putLocalVarForeach(String name, Type type){
    try{
      SymbolTable.top.put(
              new SymbolTableLocalVariableItem(
                  new Variable(name, type),
                  SymbolTable.top.getOffset(Register.SP),
                  false
              )
          );
    }
    catch (ItemAlreadyExistsException e) {
      System.out.println("injaiim");
      Tools.putLocalVar(name+"_temp_", type);
    }
  }
  static public boolean putGlobalVar(String name, Type type){
    boolean error = false;
    try{
      SymbolTable.top.put(
        new SymbolTableGlobalVariableItem(
            new Variable(name, type),
            SymbolTable.top.getOffset(Register.GP)
        )
      );
  }  
   catch (ItemAlreadyExistsException e) {
      error = true;
      Tools.putGlobalVar(name+"_temp_", type);
    }
    return error;
  }

  // Tools.expr_mem_typeCheck($expr_other.return_type,$expr_mem_tmp.count)
  static Type expr_mem_typeCheck(Type type , int count, int line){    
    Type result = type;
    while (count > 0 && result instanceof ArrayType){
      ArrayType castedItem = (ArrayType) result;
      result = castedItem.getType();
      count --;
    }
    if (count > 0){
      pass2Error = true;
      System.out.println("line"+line+": calling dimensions more than array's size");
      return NoType.getInstance();
    }
    return result;
  }
  static Type expr_un_typeCheck(Type type, int line){
    if (type == null){
      return null;
    }
    else if(type instanceof IntType){
      return type;
    }
    else if (!(type instanceof NoType)){
      pass2Error = true;
      System.out.println("Line"+line+": Error in unary operand use");
      return NoType.getInstance();
    }
    else // type is NoType
      return NoType.getInstance();
  }
  static Type expr_mult_tmp_typeCheck(Type type1 , Type type2, int line){
    if(type2 == null){
      return type1;
    }
    else if(type1 instanceof IntType && type2 instanceof IntType){
      return IntType.getInstance();
    }
    else if (type1 instanceof NoType || type2 instanceof NoType){
      return NoType.getInstance();
    }
   else {
     pass2Error = true;
     System.out.println("line"+line+": invalid operand types in expression");
     return NoType.getInstance();
   } 
  }
  static Type expr_eq_tmp_typeCheck(Type type1, Type type2, int line){
    if (type2 == null){
      return type1;
    }
    else if (type1 instanceof NoType || type2 instanceof NoType){
      return NoType.getInstance();    
    }
    else if (type1.equals(type2)){
      return IntType.getInstance();
    }
    else {
      pass2Error = true;
      System.out.println("line"+line+": Error in equality operator usage");
      return NoType.getInstance();
    }
  }
  static Type expr_mult_typeCheck(Type type1, Type type2, int line){
    return Tools.expr_mult_tmp_typeCheck(type1, type2, line);
  }
  static Type expr_add_tmp_typeCheck(Type type1, Type type2, int line){
    return Tools.expr_mult_tmp_typeCheck(type1, type2,line);
  }
  static Type expr_add_typeCheck(Type type1, Type type2, int line){
    return Tools.expr_mult_tmp_typeCheck(type1, type2, line);
  }
  static Type expr_assign_typeCheck(Type type1, Type type2, int line){
    if (type2 == null){
      return type1;
    }
    if (type1 instanceof NoType || type2 instanceof NoType){
      return NoType.getInstance();
    }
    else if (!type1.equals(type2)){
      pass2Error = true;
      System.out.println("line"+line+": Error in assignment");
      return NoType.getInstance();
    }
    else 
      return type1;
  }
  static void checkConditionType(Type type1, int line){

    if (!(type1 instanceof IntType || type1 instanceof NoType)){
      pass2Error = true;
      System.out.println("line"+line+": Error in if condition type");
    }
  }
  static void checkWriteArgument(Type type1, int line){
    if (!(type1 instanceof IntType || type1 instanceof CharType)){
      if( !(type1 instanceof ArrayType && ((ArrayType)type1).getType() instanceof CharType)) {
        pass2Error = true;
        System.out.println("line"+line+": Invalid write argument type");
        }
    }
  }
  static Type arrayInitTypeCheck(ArrayList<Type> types, int line){
      for (int i = 0; i < types.size(); i++){
        if (!types.get(i).equals(types.get(0))){
          pass2Error = true;
          System.out.println("line"+line+": Error in array init");
          return NoType.getInstance();
        }
      }
      return new ArrayType(types.get(0),types.size());
  }
  static void checkLvalue(boolean isLvalue, int line){
    if (!isLvalue){
      pass2Error = true;
      System.out.println("line"+line+": Left side of assignment is not lvalue");
    }
  }
  static void checkArrayOfForeach(Type type1, int line){
    if (!(type1 instanceof ArrayType) && !(type1 instanceof NoType)){
      pass2Error = true;
      System.out.println("line"+line+": foreach parameter is not iterable");
    }
  }
}
