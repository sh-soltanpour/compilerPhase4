grammar AtalkPass1;
@members{
	void print(String str){
        System.out.println(str);
    }
	void beginScope() {
    	int localOffset = 0;
			int globalOffset = 0;
    	if(SymbolTable.top != null){
        	localOffset = SymbolTable.top.getOffset(Register.SP);
					globalOffset = SymbolTable.top.getOffset(Register.GP);

			}
	    SymbolTable.push(new SymbolTable(SymbolTable.top));
      SymbolTable.top.setOffset(Register.SP, localOffset);
			SymbolTable.top.setOffset(Register.GP, globalOffset);
    }
	void endScope(){
		if(SymbolTable.top.getPreSymbolTable() != null) {
        SymbolTable.top.getPreSymbolTable().setOffset(
          Register.GP,
        	SymbolTable.top.getOffset(Register.GP)
          );
        }
        SymbolTable.pop();
	}
}
program:
		{beginScope();}(actor | NL)*{endScope(); 
		if (!Tools.oneActorDefined)
			print("At least one actor should be defined");
		else if(Tools.codeIsValid)
			Tools.printMessages();
		print("Pass1 finished----------------------------------------------------------------");
		}
		
	;

actor:
		'actor' name=ID '<' mailboxSize=CONST_NUM '>' NL
		{
			int mailboxSize = Integer.parseInt($mailboxSize.text);
			if (mailboxSize <= 0){
				print("line "+ String.valueOf($name.getLine())+":Actore kamtar az 0");
				mailboxSize = 0;
				Tools.codeIsValid = false;
			}
			boolean error; 
			error = Tools.putActor($name.text, mailboxSize);
			if (error){
				Tools.codeIsValid = false;
				print("line"+ String.valueOf($name.getLine())+":actor " + $name.text + " darim:)");
			}
			else {
				Tools.messages.add("actor:"+$name.text + " with mailboxSize:"+$mailboxSize.text);
			}
			beginScope();
		}
			(state | receiver | NL)*
		'end' (NL | EOF)
		{Tools.oneActorDefined = true;
			SymbolTableActorItem item = SymbolTable.top.getActor($name.text);
			if (item != null){
				item.setSymbolTable(SymbolTable.top);
			}
			endScope();
		}
	;
state:
		{ArrayList<String> names = new ArrayList<String>();} 
		type name = ID {names.add($name.text);}
		(',' name2 = ID {names.add($name2.text);} )* NL 
			{
				for(int i = 0 ; i < names.size() ; i++){
					boolean error = Tools.putGlobalVar(names.get(i),$type.return_type );
					if(error){
						Tools.codeIsValid = false;
							print("line "+ String.valueOf($name.getLine())+":global "+ names.get(i) + " darim:P");
					}
				}
			} 
	;

receiver:
		{
			ArrayList<Type> arguments = new ArrayList<Type>();
			ArrayList<String> argumentsNames = new ArrayList<String>();
		}
		'receiver' name = ID '(' ( type  name2=ID {argumentsNames.add($name2.text);arguments.add($type.return_type);}(','  type name3=ID{argumentsNames.add($name3.text);arguments.add($type.return_type);})*)? ')' NL
		{
			boolean error;
			error = Tools.putReceiver($name.text,arguments);
				if (error){
					Tools.codeIsValid = false;
					print("line "+ String.valueOf($name.getLine())+":receiver "+ $name.text +" darim:D");
				}
				else {
					String message = "Receiver Name : " + $name.text + " argumentTypes :";
					for (int i = 0; i < arguments.size(); i++){
						message += arguments.get(i).toString() + ",";
					}
					Tools.messages.add(message);
				}
				beginScope();
				for(int i = 0 ; i < argumentsNames.size() ; i++){
					error = Tools.putLocalVar(argumentsNames.get(i),arguments.get(i));
					if(error){
						Tools.codeIsValid = false;
								print("line "+ String.valueOf($name.getLine())+":Receiver Argument "+ argumentsNames.get(i)  +" darim:|");
					}
				}
			}
			statements[false]
		'end' NL
		{endScope();}

	;

type returns [Type return_type]:{ArrayList <Integer> sizes = new ArrayList<Integer>();}
		'char' {$return_type = CharType.getInstance();} ('[' size=CONST_NUM{
			int size = Integer.parseInt($size.text);
			if(size <= 0){
				Tools.codeIsValid = false;
				print("line "+ String.valueOf($size.getLine())+":araye kochiktar az 0 eh:|");
				size = 0;
			}
			sizes.add(size);
			} ']')* 
			{
			 for (int i = sizes.size()-1; i >= 0; i--){
				 $return_type = new ArrayType($return_type,sizes.get(i));
		 		}
				 
			 } 
| {ArrayList <Integer> sizes = new ArrayList<Integer>();} 'int'  {$return_type = IntType.getInstance();}  ('[' size=CONST_NUM {
			int size = Integer.parseInt($size.text);
			if(size <= 0){
				Tools.codeIsValid = false;
				print("line "+ String.valueOf($size.getLine())+":araye kochiktar az 0 eh:|");
				size = 0;
			}
			sizes.add(size);
			}
		 ']')* 	
		 {
			 for (int i = sizes.size()-1; i >= 0; i--){
				 $return_type = new ArrayType($return_type,sizes.get(i));
		 		}
		 }
	;

block[boolean foreach]:
		'begin' NL{beginScope();}
			statements[foreach]
		'end'{endScope();} NL
	;

statements[boolean foreach]:
		(statement[foreach] | NL)*
	;

statement[boolean foreach]:
		stm_vardef
	|	stm_assignment
	|	stm_foreach[foreach]
	|	stm_if_elseif_else[foreach]
	|	stm_quit
	|	stm_break[foreach]
	|	stm_tell
	|	stm_write
	|	block[foreach]
	;

stm_vardef:
	{
		ArrayList<String> names = new ArrayList<String>();
	}
	 type name = ID{names.add($name.text);} ('=' expr)? (',' name2 = ID{names.add($name2.text);} ('=' expr)?)* NL 
	{
		for(int i = 0 ; i < names.size() ; i++){
			boolean error = Tools.putLocalVar(names.get(i),$type.return_type );
			if(error){
				Tools.codeIsValid = false;
				print("line "+ String.valueOf($name.getLine())+":Local " +  names.get(i) + " darim:|");
			}
		}
	}
	;


stm_tell:
		(ID | 'sender' | 'self') '<<' ID '(' (expr (',' expr)*)? ')' NL
	;

stm_write:
		'write' '(' expr ')' NL
	;

stm_if_elseif_else[boolean foreach]:
	
		'if' expr NL{beginScope();} statements[foreach] {endScope();}
		('elseif' expr NL{beginScope();} statements[foreach]{endScope();})*
		('else' NL {beginScope();}statements[foreach]{endScope();})?
		'end' NL
	;

stm_foreach[boolean foreach]:
		'foreach' id=ID 'in' expr NL
			{
				beginScope();
				Tools.putLocalVarForeach($id.text,NoType.getInstance());
			}statements[true]{endScope();}
		'end' NL
	;

stm_quit:
		'quit' NL
	;

stm_break[boolean foreach]:
		'break' newline = NL{
			if(!$foreach){
				Tools.codeIsValid = false;
				int line = $newline.getLine();
				print("line "+ String.valueOf(line)+":break biroone X(");	
			}
		
		}
	;

stm_assignment:
		expr NL
	;

expr:
		expr_assign
	;

expr_assign:
		expr_or '=' expr_assign
	|	expr_or
	;

expr_or:
		expr_and expr_or_tmp
	;

expr_or_tmp:
		'or' expr_and expr_or_tmp
	|
	;

expr_and:
		expr_eq expr_and_tmp
	;

expr_and_tmp:
		'and' expr_eq expr_and_tmp
	|
	;

expr_eq:
		expr_cmp expr_eq_tmp
	;

expr_eq_tmp:
		('==' | '<>') expr_cmp expr_eq_tmp
	|
	;

expr_cmp:
		expr_add expr_cmp_tmp
	;

expr_cmp_tmp:
		('<' | '>') expr_add expr_cmp_tmp
	|
	;

expr_add:
		expr_mult expr_add_tmp
	;

expr_add_tmp:
		('+' | '-') expr_mult expr_add_tmp
	|
	;

expr_mult:
		expr_un expr_mult_tmp
	;

expr_mult_tmp:
		('*' | '/') expr_un expr_mult_tmp
	|
	;

expr_un:
		('not' | '-') expr_un
	|	expr_mem
	;

expr_mem:
		expr_other expr_mem_tmp
	;

expr_mem_tmp:
		'[' expr ']' expr_mem_tmp
	|
	;

expr_other:
		CONST_NUM
	|	CONST_CHAR
	|	CONST_STR
	|	ID
	|	'{' expr (',' expr)* '}'
	|	'read' '(' CONST_NUM ')'
	|	'(' expr ')'
	;

CONST_NUM:
		[0-9]+
	;

CONST_CHAR:
		'\'' . '\''
	;

CONST_STR:
		'"' ~('\r' | '\n' | '"')* '"'
	;

NL:
		'\r'? '\n' { setText("new_line");}
	;

ID:
		[a-zA-Z_][a-zA-Z0-9_]*
	;

COMMENT:
		'#'(~[\r\n])* -> skip
	;

WS:
    	[ \t] -> skip
    ;