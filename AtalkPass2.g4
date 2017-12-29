grammar AtalkPass2;
@members {
	void print(String str){
      System.out.println(str);
  }
	 void beginScope() {
      SymbolTable.push();
			mips.copySPToFP();
			SymbolTable.top.setOffset(Register.FP,SymbolTable.top.getOffset(Register.SP));
  }
  void endScope(){
    SymbolTable.pop();
		if (SymbolTable.top != null)
			mips.reverseFP(SymbolTable.top.getOffset(Register.FP));
  }
	Translator mips = new Translator();

}
program: { beginScope();} (actor | NL)* {endScope();mips.makeOutput();};

actor:
	{beginScope();} 'actor' ID '<' CONST_NUM '>' NL (
		state
		| receiver
		| NL
	)* 'end' {endScope();} (NL | EOF);

state: type id1=ID
			{	
				SymbolTableItem item = SymbolTable.top.get($id1.text);
				SymbolTableVariableItemBase var =  (SymbolTableVariableItemBase) item;
				if (var.getVariable().getType() instanceof IntType){	
					mips.addGlobalVariable(var.getOffset(),0);
				}
				else if (var.getVariable().getType() instanceof CharType){
					mips.addGlobalVariable(var.getOffset(),'\0');
				}
			} 
				(',' id2=ID{	
				SymbolTableItem item2 = SymbolTable.top.get($id2.text);
				SymbolTableVariableItemBase var2 =  (SymbolTableVariableItemBase) item2;
				if (var2.getVariable().getType() instanceof IntType){	
					mips.addGlobalVariable(var2.getOffset(),0);
				}
				else if (var2.getVariable().getType() instanceof CharType){
					mips.addGlobalVariable(var2.getOffset(),'\0');
				}
			})* NL;

receiver:
	{beginScope();} 'receiver' recName=ID '(' (var1=type ID{SymbolTable.define();} (',' var2=type ID{SymbolTable.define();})*)? ')' NL 
	{
		
		if($recName.text.equals("init") && $var1.text == null){
			SymbolTable.top.isInitEnable();
		}
	
	}statements 'end' NL 
	{	
		endScope();
	};

type
	returns[Type return_type]:
	'char' {$return_type = CharType.getInstance();} (
		'[' size = CONST_NUM {
			int size = Integer.parseInt($size.text);
			
			$return_type= new ArrayType($return_type,size);} ']'
	)*
	| 'int' {$return_type = IntType.getInstance();} (
		'[' size = CONST_NUM {
			int size = Integer.parseInt($size.text);
			
			$return_type= new ArrayType($return_type,size);} ']'
	)*;

block:
	{beginScope();} 'begin' NL statements 'end' NL {endScope();};

statements: (statement | NL)*;

statement:
	stm_vardef
	| stm_assignment
	| stm_foreach
	| stm_if_elseif_else
	| stm_quit
	| stm_break
	| stm_tell
	| stm_write
	| block;

stm_vardef:
	type id1=ID { SymbolTable.define();Tools.addLocalToStack(mips,$id1.text);} ('=' var2=expr[false]
	{
		SymbolTableItem item = SymbolTable.top.get($id1.text);
		if(item instanceof SymbolTableVariableItemBase){
				SymbolTableVariableItemBase var = (SymbolTableVariableItemBase) item;
				Tools.expr_assign_typeCheck(var.getVariable().getType(), $var2.return_type,$id1.getLine());
		}		
	}
	)? (
	',' id2=ID { SymbolTable.define();Tools.addLocalToStack(mips,$id2.text); } ('=' var2=expr[false]
		{
			SymbolTableItem item = SymbolTable.top.get($id2.text);
			if(item instanceof SymbolTableVariableItemBase){
				SymbolTableVariableItemBase var = (SymbolTableVariableItemBase) item;
				Tools.expr_assign_typeCheck(var.getVariable().getType(), $var2.return_type,$id2.getLine());
			}
		}
		)?
	)* NL;

stm_tell:{ArrayList<Type> types = new ArrayList<Type>();}
	(actorId = ID | actorId='sender' | actorId='self') '<<' recName=ID '(' (var1=expr[false]{types.add($var1.return_type);} (',' var2=expr[false]{types.add($var2.return_type);})*)? ')' NL
	{		
			if ($actorId.text.equals("self")){
				if(!SymbolTable.top.hasReceiver($recName.text, types)){
					Tools.pass2Error = true;
					print("line"+ $actorId.getLine()  +": receiver not found");
				}
				
			}
			else if ($actorId.text.equals("sender")){
				if(SymbolTable.top.getIsInit()){
					Tools.pass2Error = true;
					print("line" + $actorId.getLine() + ": Sender in init() is not allowed");
				}
			}
			else{
				SymbolTableActorItem actorItem = SymbolTable.top.getActor($actorId.text);
				
				if (actorItem == null){
					Tools.pass2Error = true;
					print("actor not found");
				}
				else{
					if(!actorItem.getSymbolTable().hasReceiver($recName.text, types))
						print("line"+ $actorId.getLine()  +": receiver not found");
				}
			}
	};

stm_write: writeToken='write' '(' var1=expr[false] ')' NL{mips.write();Tools.checkWriteArgument($var1.return_type,$writeToken.getLine());};

stm_if_elseif_else:
	ifToken='if' var1=expr[false]{Tools.checkConditionType($var1.return_type,$ifToken.getLine());} NL{beginScope();} statements{endScope();}
	 (elseifToken='elseif' var2=expr[false] {Tools.checkConditionType($var2.return_type,$elseifToken.getLine());} NL {beginScope();}statements{endScope();})* (
		'else' NL {beginScope();}statements{endScope();}
	)? 'end' NL;

stm_foreach: {beginScope();} foreachToken='foreach' ID {SymbolTable.define();} 'in' var1=expr[false]{Tools.checkArrayOfForeach($var1.return_type,$foreachToken.getLine());} NL statements 'end' NL{endScope();};

stm_quit: 'quit' NL;

stm_break: 'break' NL;

stm_assignment: expr[false] NL;

expr[boolean isLeft]
	returns[Type return_type, boolean isLvalue, int line]:
	expr_assign[$isLeft] {$line =$expr_assign.line;$isLvalue = $expr_assign.isLvalue;$return_type = $expr_assign.return_type;};

expr_assign[boolean isLeft]
	returns[Type return_type, boolean isLvalue, int line]:
	var1=expr_or[true] op='=' var2=expr_assign[false] {mips.assignCommand();Tools.checkLvalue($var1.isLvalue,$op.getLine());$return_type = Tools.expr_assign_typeCheck($var1.return_type, $var2.return_type,$op.getLine());}
	| var3=expr_or [false]
	{
		$isLvalue = $var3.isLvalue;$return_type = $expr_or.return_type;
		$line = $var3.line;	
	};

expr_or[boolean isLeft]
	returns[Type return_type, boolean isLvalue, int line]: var1=expr_and[$isLeft] var2=expr_or_tmp[$isLeft]
	{$return_type = Tools.expr_mult_typeCheck($var1.return_type, $var2.return_type,$var1.line);
		$isLvalue = $var1.isLvalue && $var2.isLvalue;
	}
	;

expr_or_tmp[boolean isLeft]
	returns[Type return_type, boolean isLvalue]:
	op='or' var1=expr_and[$isLeft] {mips.operationCommand($op.text);} var2=expr_or_tmp[$isLeft]
	{$return_type = Tools.expr_mult_tmp_typeCheck($var1.return_type, $var2.return_type,$op.getLine());
	 $isLvalue = false;
	}
	| {$isLvalue = true;$return_type = null;};

expr_and[boolean isLeft]
	returns[Type return_type, boolean isLvalue, int line]: 
	var1=expr_eq[$isLeft] var2=expr_and_tmp[$isLeft]
		{
		$line = $var1.line;
		$return_type = Tools.expr_mult_typeCheck($var1.return_type, $var2.return_type,$var1.line);
		$isLvalue = $var1.isLvalue && $var2.isLvalue;
		};

expr_and_tmp[boolean isLeft]
	returns[Type return_type, boolean isLvalue, int line]:
	op='and' var1=expr_eq[$isLeft]{mips.operationCommand($op.text);} var2=expr_and_tmp[$isLeft] 
		{
		$return_type = Tools.expr_mult_tmp_typeCheck($var1.return_type, $var2.return_type,$op.line);
		$isLvalue = false;
		}
	| {$isLvalue = true;$return_type = null;};

expr_eq[boolean isLeft]
	returns[Type return_type, boolean isLvalue, int line]: 
	var1=expr_cmp[$isLeft] var2=expr_eq_tmp[$isLeft]{
		$line = $var1.line;
		$isLvalue = $var1.isLvalue && $var2.isLvalue;
		$return_type = Tools.expr_eq_tmp_typeCheck($var1.return_type, $var2.return_type,$var1.line);
	};

expr_eq_tmp[boolean isLeft]
	returns[Type return_type, boolean isLvalue]: 
	op=('==' | '<>') var1=expr_cmp[$isLeft]{mips.operationCommand($op.text);} var2=expr_eq_tmp[$isLeft] {$isLvalue = false;$return_type = Tools.expr_eq_tmp_typeCheck($var1.return_type, $var2.return_type,$op.getLine());}
	| {$isLvalue = true;$return_type = null;};

expr_cmp[boolean isLeft]
	returns[Type return_type, boolean isLvalue, int line]:
	var1 = expr_add[$isLeft] var2 = expr_cmp_tmp[$isLeft] 
	{
		$line = $var1.line;
		$return_type = Tools.expr_mult_typeCheck($var1.return_type, $var2.return_type,$var1.line);
		$isLvalue = $var1.isLvalue && $var2.isLvalue;
	};

expr_cmp_tmp[boolean isLeft]
	returns[Type return_type, boolean isLvalue]: 
	op=('<' | '>') var1 = expr_add[$isLeft] {mips.operationCommand($op.text);} var2 = expr_cmp_tmp[$isLeft] {$isLvalue = false;$return_type = Tools.expr_mult_tmp_typeCheck($var1.return_type, $var2.return_type,$op.getLine());}
	| {$isLvalue = true;$return_type = null;};

expr_add[boolean isLeft]
	returns[Type return_type, boolean isLvalue, int line]:
	var1 = expr_mult[$isLeft] var2 = expr_add_tmp[$isLeft] {
		$line = $var1.line;
		$isLvalue = $var1.isLvalue && $var2.isLvalue;
		$return_type = Tools.expr_mult_typeCheck($var1.return_type, $var2.return_type,$var1.line);
	
	};

expr_add_tmp[boolean isLeft]
	returns[Type return_type, boolean isLvalue]: 
	op=('+' | '-') var1 = expr_mult[$isLeft] {mips.operationCommand($op.text);} var2 = expr_add_tmp[$isLeft] {$isLvalue = false;$return_type = Tools.expr_mult_tmp_typeCheck($var1.return_type, $var2.return_type,$op.getLine());
		}
	| {$isLvalue = true;$return_type = null;};

expr_mult[boolean isLeft]
	returns[Type return_type, boolean isLvalue, int line]:
	var1 = expr_un[$isLeft] var2 = expr_mult_tmp[$isLeft] 
	{
		$line = $var1.line;
		$isLvalue=$var1.isLvalue && $var2.isLvalue;$return_type = Tools.expr_mult_typeCheck($var1.return_type, $var2.return_type,$var1.line);
	};

expr_mult_tmp[boolean isLeft]
	returns[Type return_type, boolean isLvalue]: 
	op=('*' | '/') var1 = expr_un[$isLeft] {mips.operationCommand($op.text);} var2 = expr_mult_tmp[$isLeft] {$isLvalue =false;$return_type = Tools.expr_mult_tmp_typeCheck($var1.return_type, $var2.return_type,$op.getLine());
		}
	| {$isLvalue = true;$return_type = null;};

expr_un[boolean isLeft]
	returns[Type return_type, boolean isLvalue, int line]: 
	op=('not' | '-') expr_un_var = expr_un[$isLeft] 
	{
		$isLvalue = false;
		$return_type = Tools.expr_un_typeCheck($expr_un_var.return_type,$op.getLine());
		$line = $op.getLine();
		mips.operationCommand($op.text + $op.text);
	}
	| var1=expr_mem[$isLeft] 
	{$isLvalue = $var1.isLvalue;
	$return_type = $expr_mem.return_type;
	$line=$var1.line;};

expr_mem[boolean isLeft]
	returns[Type return_type, boolean isLvalue, int line]:
	var1=expr_other[$isLeft] expr_mem_tmp[$isLeft] 
	{
		$line = $var1.line;
		$return_type = Tools.expr_mem_typeCheck($expr_other.return_type,$expr_mem_tmp.count,$var1.line);
		$isLvalue = $var1.isLvalue;
	};

expr_mem_tmp[boolean isLeft]
	returns[int count]:
	'[' expr1 = expr[$isLeft] ']' expr2 = expr_mem_tmp[$isLeft] {$count = $expr2.count + 1;}
	| {$count = 0;};

expr_other[boolean isLeft]
	returns[Type return_type, boolean isLvalue, int line]:
	num=CONST_NUM {mips.addToStack(Integer.parseInt($num.text));$return_type = IntType.getInstance();$isLvalue = false;$line = $num.getLine(); }
	| character=CONST_CHAR {$return_type = CharType.getInstance();$isLvalue = false;$line=$character.getLine();}
	| str = CONST_STR {$return_type = new ArrayType(CharType.getInstance(),$str.text.length()-2 );$isLvalue = false;$line=$str.getLine();}
	| id = ID { 
						$isLvalue = true;
						$line = $id.getLine();
            SymbolTableItem item = SymbolTable.top.get($id.text);
	          if(!(item instanceof SymbolTableVariableItemBase)) {
								Tools.putLocalVar($id.text, NoType.getInstance());
								SymbolTable.define();
								$return_type = NoType.getInstance();
                print("line" + $id.line + ": Item " + $id.text + " doesn't exist.");
            }
            else {
                SymbolTableVariableItemBase var = (SymbolTableVariableItemBase) item;
								$return_type = var.getVariable().getType();
								$isLvalue = var.isLvalue();
								if (var.getBaseRegister() == Register.SP){
                    if (!$isLeft) mips.addToStack($id.text, var.getOffset()*-1);
                    else mips.addAddressToStack($id.text, var.getOffset()*-1);
                }
                else {
                    if ($isLvalue == false) mips.addGlobalToStack(var.getOffset());
                    else mips.addGlobalAddressToStack($id.text, var.getOffset());
                }
						}
  }
	|{$isLvalue = false;ArrayList <Type> types = new ArrayList<Type>();} openBr='{' var1=expr[$isLeft]{types.add($var1.return_type);}
	 (',' var2=expr[$isLeft]{types.add($var2.return_type);})* '}' {$return_type = Tools.arrayInitTypeCheck(types,$openBr.getLine());$line = $openBr.getLine();}
	
	| 'read' openPr='(' num = CONST_NUM ')' {$isLvalue = false;$return_type = new ArrayType(CharType.getInstance(),Integer.parseInt($num.text));$line=$openPr.getLine();}
	| openPr='(' var1=expr[$isLeft] ')' {$isLvalue = $var1.isLvalue;$return_type = $var1.return_type;$isLvalue = true;$line=$openPr.getLine();} ;

CONST_NUM: [0-9]+;

CONST_CHAR: '\'' . '\'';

CONST_STR: '"' ~('\r' | '\n' | '"')* '"';

NL: '\r'? '\n' { setText("new_line"); };

ID: [a-zA-Z_][a-zA-Z0-9_]*;

COMMENT: '#' (~[\r\n])* -> skip;

WS: [ \t] -> skip;