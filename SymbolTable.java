import java.util.*;

public class SymbolTable {

	// Static members region

	public static SymbolTable top;
	
	private static Stack<SymbolTable> stack = new Stack<SymbolTable>();
	
	private static Queue<SymbolTable> queue = new LinkedList<SymbolTable>();

	private static int definitionsCount = 0;

	// Use it in pass1 scope start
	public static void push(SymbolTable symbolTable) {
		if(top != null)
			stack.push(top);
		top = symbolTable;
		queue.offer(symbolTable);
	}

	// Use it in pass1, pass2 scope end
	public static void pop() {
		top = stack.pop();
	}

	// Use it in pass2 scope start
	public static void push() {
		push(queue.remove());
	}

	// Use it in pass2, where an item with property "defMustBeComesBeforeUse == true" is defined
	public static void define() {
		++definitionsCount;
	}

	// End of static members region

	public SymbolTable() {
		this(null);
	}

	public SymbolTable(SymbolTable pre) {
		this.pre = pre;
		this.items = new HashMap<String, SymbolTableItem>();
		this.offsets = new HashMap<Register, Integer>();
		this.isInit = false;
	}

	public void put(SymbolTableItem item) throws ItemAlreadyExistsException {
		if(items.containsKey(item.getKey()))
			throw new ItemAlreadyExistsException();
		items.put(item.getKey(), item);
		SymbolTableItem myItem = items.get(item.getKey());

		if(item instanceof SymbolTableVariableItemBase) {
			SymbolTableVariableItemBase castedItem = (SymbolTableVariableItemBase) item;
			int oldOffset = getOffset(castedItem.getBaseRegister());
			int newOffset = getOffset(castedItem.getBaseRegister()) + castedItem.getSize();
			setOffset(
				castedItem.getBaseRegister(),newOffset	
			);

			Variable variable = castedItem.getVariable();
			String name = variable.getName();
			String type = variable.getType().toString();
			String localOrGlobal = castedItem instanceof SymbolTableLocalVariableItem ? "local" : "global";
			Tools.messages.add(localOrGlobal + " variable name: "+name +" with type: " + type +" offset: " + String.valueOf(oldOffset)
			+ ", size: " + String.valueOf(variable.getType().size()));
		}

	}

	public int getOffset(Register baseRegister) {
		if(!offsets.containsKey(baseRegister))
		   return 0;
		return offsets.get(baseRegister);
	}

	public void setOffset(Register baseRegister, int value) {
		offsets.put(baseRegister, value);
	}

	public SymbolTableItem getInCurrentScope(String key) {
		return items.get(key);
	}

	public SymbolTableItem get(String key) {
		// System.out.println("Start");
		// for (String mykey : items.keySet()) {
		// 	System.out.println(mykey);
		// }
		// System.out.println("Finished");
		SymbolTableItem value = items.get(key);
		if(value == null && pre != null){
			SymbolTableItem returnedValue = pre.get(key);
			return pre.get(key);
		}
		if(value != null && value.useMustBeComesAfterDef() &&
				SymbolTable.definitionsCount  <= value.getDefinitionNumber()) {
			if(pre != null) 
				return pre.get(key);
			else 
				return null;
		}
		return value;
	}
	public SymbolTableActorItem getActor(String key){
		SymbolTable current = this;
		while (current.pre != null){
			current = current.pre;
		}
		if (current.get(key) instanceof SymbolTableActorItem)
			return (SymbolTableActorItem)current.get(key);
		else 
			return null;
	}
	public SymbolTable getPreSymbolTable() {
		return pre;
	}
	public boolean hasReceiver(String recName , ArrayList<Type> types){
		String key = recName + "#";
    for(int i = 0 ; i < types.size(); i++){
      key += types.get(i).toString(); 
    }
    SymbolTableItem item = this.get(key);
    if (item != null){
			return true;
    }
    else 
    	return false;
	}
	public void isInitEnable(){
		this.isInit = true;
	}
	public boolean getIsInit(){
		return isInit;
	}
	SymbolTable pre;
	HashMap<String, SymbolTableItem> items;
	HashMap<Register, Integer> offsets;
	boolean isInit;
}