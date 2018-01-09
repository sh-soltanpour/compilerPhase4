
/**
 * Created by vrasa on 12/26/2016.
 */

import java.util.*;

import javafx.scene.chart.PieChart.Data;

import java.io.*;

public class Translator {

  private File output;
  private ArrayList<String> instructions;
  private ArrayList<String> initInstructions;
  private ArrayList<String> dataSegment;
  private ArrayList<String> schedulerInstructions;

  public Stack<Integer> stackPointers;
  private Stack<Integer> ifIndex;
  private Stack<Integer> ifsNumber;
  private Stack<Integer> ifJumpIndex;
  int labelCounter;

  public Translator() {
    instructions = new ArrayList<String>();
    initInstructions = new ArrayList<String>();
    dataSegment = new ArrayList<String>();
    schedulerInstructions = new ArrayList<String>();

    stackPointers = new Stack<Integer>();
    ifIndex = new Stack<Integer>();
    ifsNumber = new Stack<Integer>();
    ifJumpIndex = new Stack<Integer>();

    labelCounter = 0;
    output = new File("out.asm");
    try {
      output.createNewFile();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void addInstruction(String inst) {
    instructions.add(inst);
  }

  public void addActorQueue(String name, int mailboxSize) {
    dataSegment.add(name + ": .space " + ((mailboxSize * 2 + 2) * 4));
    initInstructions.add("# start of initializing actor queue: " + name);
    initInstructions.add("la $a0, " + name);
    initInstructions.add("li $a1, 0");
    // initInstructions.add("sw $a1, 0($a0)");
    // initInstructions.add("sw $a1, 4($a0)");
    initInstructions.add("sw $a1, 0($a0)");
    initInstructions.add("sw $a1, 4($a0)");

    initInstructions.add("la $a1, scheduler");
    initInstructions.add("lw $a2 ,0($a1)"); // a2 = counter

    initInstructions.add("li $a3, 4");
    initInstructions.add("mul $a3, $a2, $a3");
    initInstructions.add("add $a3, $a3, $a1");
    initInstructions.add("sw $a0, 4($a3)");

    initInstructions.add("addi $a2, $a2, 1");
    initInstructions.add("sw $a2, 0($a1)");

    initInstructions.add("# end of initializing actor queue: " + name);
    // addMessageToActorQueue(name, "init#");
  }

  public void addMessageToActorQueue(String actorName, SymbolTableReceiverItem receiverItem) {
    String recKeys = receiverItem.getKey();
    int hashIndex = recKeys.indexOf('#');
    StringBuilder recKey = new StringBuilder(recKeys);
    recKey.setCharAt(hashIndex, '_');

    instructions.add("#start of adding message to actor queue, actorName : " + actorName + " rec Key : " + recKey);
    instructions.add("la $a0," + actorName); //a0 = sare PCB
    instructions.add("la $a1," + actorName + "_" + recKey); // sare receiver

    instructions.add("li $a3, 4");
    instructions.add("lw $a2, 0($a0)"); //a2 = counter
    
    //update kardane counter(front of queue)
    instructions.add("addi $a2,$a2, 1");
    instructions.add("sw $a2, 0($a0)");
    instructions.add("addi $a2, $a2, -1");
    //end of update kardane counter(front of queue)

    instructions.add("mul $a2, $a2, $a3");
    instructions.add("add $a2, $a0, $a2");
    
    instructions.add("sw $a1, 8($a2)"); // addresse receivero rikhtim
    instructions.add("sw $t7, 12($a2)"); // addresse avvale parametera

    addReceiverArgumentsToHeap(receiverItem);

    instructions.add("#end of adding message to actor queue, actorName : " + actorName + " rec Key : " + recKey);
  }

  public void schedulerInit() {
    dataSegment.add("scheduler: .space  200");
    initInstructions.add("#start initialize scheduler counter");
    initInstructions.add("li $a0, 0");
    initInstructions.add("la $a1, scheduler");
    initInstructions.add("sw $a0, 0($a1)");
    initInstructions.add("li $t6, 0");
    initInstructions.add("#end initialize scheduler counter");

    schedulerInstructions.add("schedulerCode: ");
    schedulerInstructions.add("la $a0, scheduler");
    schedulerInstructions.add("lw $a1, 0($a0)"); // a1 = scheduler space counter 

    schedulerInstructions.add("rem $t6, $t6, $a1"); // t6 = t6 % a1
    schedulerInstructions.add("li $a2, 4");
    schedulerInstructions.add("mul $a2,$t6,$a2 ");
    schedulerInstructions.add("add $a2, $a2, $a0");
    schedulerInstructions.add("lw $a3, 4($a2)");
    //$a3 = address of actor label 
    schedulerInstructions.add("lw $t0, 0($a3)");
    schedulerInstructions.add("lw $t1, 4($a3)");

    String noNewMessage = getLabel();
    schedulerInstructions.add("beq $t1,$t0," + noNewMessage);
    schedulerInstructions.add("li $a2, 4");
    schedulerInstructions.add("mul $a2, $t1, $a2");
    schedulerInstructions.add("add $a2, $a2, $a3");
    schedulerInstructions.add("lw $t3, 8($a2)");
    //updating actor rear
    schedulerInstructions.add("addi $t1,$t1,1");
    schedulerInstructions.add("sw, $t1,4($a3)");
    //end of updating actor rear
    // $t3 = first message to run
    schedulerInstructions.add("lw $t5, 12($a2)");
    schedulerInstructions.add("jalr $t3");
    schedulerInstructions.add(noNewMessage + ":");
    schedulerInstructions.add("addi $t6, $t6, 1");
    schedulerInstructions.add("j schedulerCode");
  }

  public void addInitToActorQueue(String actorName, SymbolTableReceiverItem receiverItem) {
    String recKey = receiverItem.getKey();
    int hashIndex = recKey.indexOf('#');
    recKey = recKey.substring(0, hashIndex) + '_' + recKey.substring(hashIndex);

    initInstructions.add("#start of adding init message to actor queue, actorName : " + actorName + " rec Key : " + recKey);
    initInstructions.add("la $a0, " + actorName); //a0 = sare PCB
    initInstructions.add("la $a1, " + actorName + "_" + recKey); // sare receiver

    initInstructions.add("li $a3, 4");
    initInstructions.add("lw $a2, 0($a0)"); //a2 = queue rare counter
    initInstructions.add("mul $a3, $a2, $a3");
    initInstructions.add("add $a3, $a0, $a3");

    initInstructions.add("sw $a1, 8($a3)"); // addresse receivero rikhtim
    initInstructions.add("sw $t7, 12($a3)"); // addresse avvale parametera

    initInstructions.add("addi $a2, $a2, 1");
    initInstructions.add("sw $a2, 0($a0)");
    

    addReceiverArgumentsToHeap(receiverItem);

    initInstructions.add("#end of adding init message to actor queue, actorName : " + actorName + " rec Key : " + recKey);
  }

  public void addReceiverArgumentsToHeap(SymbolTableReceiverItem receiverItem) {
    instructions.add("#start of adding receiver arguments to stack");
    ArrayList<Type> argumentTypes = receiverItem.getReceiver().getArgumentTypes();
    int size = 1;
    for (Type type : argumentTypes) {
      size *= type.size();
    }
    int offset = size * 4;
    instructions.add("addi $sp, $sp, " + offset);
    for (int i = 0; i < size; i++) {
      instructions.add("lw $a0, 0($sp)");
      instructions.add("sw $a0, 0($t7)");
      instructions.add("addi $t7, $t7, -4");
      instructions.add("addi $sp, $sp, -4");
    }
    instructions.add("addi $sp, $sp, " + (offset + 4));
    instructions.add("#end of adding receiver arguments to stack");
  }
  public void addReturnInstruction(){
    instructions.add("jr $ra");
  }
  public String getLabel() {
    String returnValue = "LABEL" + labelCounter;
    ++labelCounter;
    return returnValue;
  }

  public void reverseSP(int addr) {
    instructions.add("addi $sp, $fp," + addr);
    // instructions.add("li $sp, " + addr);
  }

  public void makeOutput() {
    this.addSystemCall(10);
    try {
      PrintWriter writer = new PrintWriter(output);
      writer.println(".data");
      for (int i = 0; i < dataSegment.size(); i++)
        writer.println(dataSegment.get(i));
      writer.println(".text");
      writer.println("main:");
      writer.println("move $fp, $sp");
      writer.println("move $t7, $fp");
      writer.println("addi $t7, $t7, -32768");

      for (int i = 0; i < initInstructions.size(); i++) {
        writer.println(initInstructions.get(i));
      }
      for (int i = 0; i < schedulerInstructions.size(); i++) {
        writer.println(schedulerInstructions.get(i));
      }
      for (int i = 0; i < instructions.size(); i++) {
        writer.println(instructions.get(i));
      }
      writer.println("END :");
      writer.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void addToStack(int x) {
    instructions.add("# adding a number to stack");
    instructions.add("li $a0, " + x);
    instructions.add("sw $a0, 0($sp)");
    instructions.add("addiu $sp, $sp, -4");
    instructions.add("# end of adding a number to stack");

  }

  public void addIntToStack(int x) {
    instructions.add("# start of adding integer to stack");
    instructions.add("li $a0, " + x);
    instructions.add("sw $a0, 0($sp)");
    instructions.add("addiu $sp, $sp, -4");
    instructions.add("# end of adding integer to stack");
  }

  public void addCharToStack(char c) {
    int x = c - '\0';
    instructions.add("# start of adding character to stack");
    instructions.add("li $a0, " + x);
    instructions.add("sw $a0, 0($sp)");
    instructions.add("addiu $sp, $sp, -4");
    instructions.add("# end of adding character to stack");
  }

  public void addToStack(String s, int adr) {
    //        int adr = table.getAddress(s)*(-1);
    instructions.add("# start of adding variable to stack");
    instructions.add("lw $a0, " + adr + "($fp)");
    instructions.add("sw $a0, 0($sp)");
    instructions.add("addiu $sp, $sp, -4");
    instructions.add("# end of adding variable to stack");
  }

  public void addAddressToStack(String s, int adr) {
    //        int adr = table.getAddress(s)*(-1);
    instructions.add("# start of adding address to stack");
    instructions.add("addiu $a0, $fp, " + adr);
    instructions.add("sw $a0, 0($sp)");
    instructions.add("addiu $sp, $sp, -4");
    instructions.add("# end of adding address to stack");
  }

  public void addGlobalAddressToStack(String s, int adr) {
    //        int adr = table.getAddress(s)*(-1);
    instructions.add("# start of adding global address to stack");
    instructions.add("addiu $a0, $gp, " + adr);
    instructions.add("sw $a0, 0($sp)");
    instructions.add("addiu $sp, $sp, -4");
    instructions.add("# end of adding global address to stack");
  }

  public void popStack() {
    instructions.add("# pop stack");
    instructions.add("addiu $sp, $sp, 4");
    instructions.add("# end of pop stack");
  }

  public void addSystemCall(int x) {
    instructions.add("# start syscall " + x);
    instructions.add("li $v0, " + x);
    instructions.add("syscall");
    instructions.add("# end syscall");
  }

  public void assignCommand(boolean isLeftMost) {
    instructions.add("# start of assign");
    instructions.add("lw $a0, 4($sp)");
    popStack();
    instructions.add("lw $a1, 4($sp)");
    popStack();
    instructions.add("sw $a0, 0($a1)");
    instructions.add("sw $a0, 0($sp)");
    instructions.add("addiu $sp, $sp, -4");
    if (isLeftMost) {
      popStack();
    }
    instructions.add("# end of assign");
  }

  public void assignCommandInVardef() {
    instructions.add("# start of assign in vardef");
    instructions.add("lw $a1, 4($sp)");
    popStack();
    instructions.add("lw $a0, 4($sp)");
    popStack();
    instructions.add("sw $a0, 0($a1)");
    instructions.add("sw $a0, 0($sp)");
    instructions.add("addiu $sp, $sp, -4");
    popStack();
    instructions.add("# end of assign in vardef");

  }

  public void operationCommand(String s) {
    instructions.add("# operation " + s);
    if (s.equals("*")) {
      instructions.add("lw $a0, 4($sp)");
      popStack();
      instructions.add("lw $a1, 4($sp)");
      popStack();
      instructions.add("mul $a0, $a0, $a1");
      instructions.add("sw $a0, 0($sp)");
      instructions.add("addiu $sp, $sp, -4");
    } else if (s.equals("/")) {
      instructions.add("lw $a0, 4($sp)");
      popStack();
      instructions.add("lw $a1, 4($sp)");
      popStack();
      instructions.add("div $a0, $a1, $a0");
      instructions.add("sw $a0, 0($sp)");
      instructions.add("addiu $sp, $sp, -4");
    } else if (s.equals("+")) {
      instructions.add("lw $a0, 4($sp)");
      popStack();
      instructions.add("lw $a1, 4($sp)");
      popStack();
      instructions.add("add $a0, $a0, $a1");
      instructions.add("sw $a0, 0($sp)");
      instructions.add("addiu $sp, $sp, -4");
    } else if (s.equals("-")) {
      instructions.add("lw $a0, 4($sp)");
      popStack();
      instructions.add("lw $a1, 4($sp)");
      popStack();
      instructions.add("sub $a0, $a1, $a0");
      instructions.add("sw $a0, 0($sp)");
      instructions.add("addiu $sp, $sp, -4");
    } else if (s.equals("--")) {
      instructions.add("lw $a0, 4($sp)");
      popStack();
      instructions.add("neg $a0, $a0");
      instructions.add("sw $a0, 0($sp)");
      instructions.add("addiu $sp, $sp, -4");
    } else if (s.equals("notnot")) {
      instructions.add("lw $a0, 4($sp)");
      popStack();
      instructions.add("not $a0, $a0");
      instructions.add("sw $a0, 0($sp)");
      instructions.add("addiu $sp, $sp, -4");
    } else if (s.equals(">") || s.equals("<")) {
      instructions.add("lw $a0, 4($sp)");
      popStack();
      instructions.add("lw $a1, 4($sp)");
      popStack();
      if (s.equals(">"))
        instructions.add("slt $a2, $a0, $a1");
      else
        instructions.add("slt $a2, $a1, $a0");
      instructions.add("sw $a2, 0($sp)");
      instructions.add("addiu $sp, $sp, -4");
    }
    // else if (s.equals("==") || s.equals("<>")){
    //   instructions.add("lw $a0, 4($sp)");
    //   popStack();
    //   instructions.add("lw $a1, 4($sp)");
    //   popStack();
    //   instructions.add("slt $a2, $a0, $a1");
    //   instructions.add("slt $a3, $a1, $a0");
    //   if(s.equals("=="))instructions.add("nor $a0, $a2, $a3");
    //   else instructions.add("or $a0, $a2, $a3");

    //   instructions.add("sw $a0, 0($sp)");
    //   instructions.add("addiu $sp, $sp, -4");
    // }
    else if (s.equals("==") || s.equals("<>")) {
      instructions.add("lw $a0, 4($sp)");
      popStack();
      instructions.add("lw $a1, 4($sp)");
      popStack();
      instructions.add("sub $a2, $a1, $a0");
      instructions.add("li $a3, 0");
      String label1 = this.getLabel();
      String label2 = this.getLabel();
      instructions.add("beq, $a2, $a3, " + label1);
      if (s.equals("=="))
        this.addIntToStack(0);
      else
        this.addIntToStack(1);

      instructions.add("j " + label2);
      instructions.add(label1 + ":");
      if (s.equals("=="))
        this.addIntToStack(1);
      else
        this.addIntToStack(0);
      instructions.add(label2 + ":");
    } else if (s.equals("and") || s.equals("or")) {
      instructions.add("lw $a0, 4($sp)");
      popStack();
      instructions.add("lw $a1, 4($sp)");
      popStack();
      instructions.add(s + " $a2, $a0, $a1");

      instructions.add("sw $a0, 0($sp)");
      instructions.add("addiu $sp, $sp, -4");
    }

    instructions.add("# end of operation " + s);
  }

  public void write(Type type) {

    instructions.add("# writing");

    if (type instanceof IntType) {
      instructions.add("lw $a0, 4($sp)");
      popStack();
      this.addSystemCall(1);
    } else if (type instanceof CharType) {
      instructions.add("lw $a0, 4($sp)");
      popStack();
      this.addSystemCall(11);
    } else if (type instanceof ArrayType) {
      int numOfElements = ((ArrayType) type).getWidth();
      int offset = numOfElements * (4);
      instructions.add("addiu $sp, $sp," + offset);
      for (int i = 0; i < numOfElements; i++) {
        instructions.add("lw $a0, 0($sp)");
        instructions.add("addiu $sp, $sp,-4");
        this.addSystemCall(11);
      }
      for (int i = 0; i < numOfElements; i++) {
        popStack();
      }
    }
    instructions.add("addi $a0, $zero, 10");
    this.addSystemCall(11);
    instructions.add("# end of writing");
  }

  public void addGlobalToStack(int adr) {
    //        int adr = table.getAddress(s)*(-1);
    instructions.add("# start of adding global variable to stack");
    instructions.add("lw $a0, " + adr + "($gp)");
    instructions.add("sw $a0, 0($sp)");
    instructions.add("addiu $sp, $sp, -4");
    instructions.add("# end of adding global variable to stack");
  }

  public void addGlobalVariable(int adr, int x) {
    //        int adr = table.getAddress(s)*(-1);
    initInstructions.add("# adding a global variable");
    initInstructions.add("li $a0, " + x);
    initInstructions.add("sw $a0, " + adr + "($gp)");
    initInstructions.add("# end of adding a global variable");
  }

  public void addGlobalVariable(int adr, char x) {
    int asciiCode = x - '\0';
    addGlobalVariable(adr, asciiCode);
  }

  public void copySPToFP() {
    instructions.add("#start of copy sp to fp");
    instructions.add("move $fp, $sp");
    instructions.add("#end of copy sp to fp");
  }

  public void reverseFP(int x) {
    instructions.add("#start of reverseFP");
    instructions.add("li $fp," + x);
    instructions.add("#end of reverseFP");
  }

  public void ifCondition() {
    instructions.add("#ifCondition start");
    ifsNumber.push(1);
    instructions.add("lw $a0, 4($sp)");
    this.popStack();
    instructions.add("li $a1, 0");
    instructions.add("beq $a0, $a1, ");
    this.ifIndex.push(instructions.size() - 1);
    instructions.add("#ifCondition finish");
  }

  public void elsifCondition() {
    instructions.add("#elsifCondition start");
    int number = ifsNumber.pop();
    ifsNumber.push(number + 1);
    instructions.add("lw $a0, 4($sp)");
    this.popStack();
    instructions.add("li $a1, 0");
    instructions.add("beq $a0, $a1, ");
    this.ifIndex.push(instructions.size() - 1);
    instructions.add("#elsifCondition finish");

  }

  public void addLabel() {
    instructions.add("#addLabel start");
    String label = this.getLabel();
    instructions.add(label + ":");
    int index = ifIndex.pop();
    instructions.set(index, instructions.get(index) + label);
    instructions.add("#addLabel finish");
  }

  public void addJumpLabel() {
    instructions.add("#addJumpLabel start");
    String label = this.getLabel();
    instructions.add(label + ":");
    int number = ifsNumber.pop();
    for (int i = 0; i < number; i++) {
      int index = ifJumpIndex.pop();
      instructions.set(index, instructions.get(index) + label);
    }
    instructions.add("#addJumpfinish start");
  }

  public void addJumpInst() {
    instructions.add("#addJumpInst start");
    instructions.add("j ");
    this.ifJumpIndex.push(instructions.size() - 1);
    instructions.add("#addJumpInst finish");
  }

  public void addElementToStack(int adr, int numOfElements) {
    instructions.add("# start of adding element to stack");
    instructions.add("addi $a0 ,$a1 ," + adr);
    instructions.add("add $a1, $a0, $fp");
    for (int i = 0; i < numOfElements; i++) {
      instructions.add("lw $a0, 0($a1)");
      instructions.add("sw $a0, 0($sp)");
      instructions.add("addiu $sp, $sp, -4");
      instructions.add("addiu $a1, $a1, -4");
    }
    instructions.add("# end of adding element to stack");
  }

  public void addElementAddressToStack(int adr) {
    instructions.add("# start of adding element to stack");
    instructions.add("addi $a0 ,$a1 ," + adr);
    instructions.add("add $a1, $a0, $fp");
    instructions.add("sw $a1 , 0($sp)");
    instructions.add("addiu $sp, $sp, -4");
    instructions.add("# end of adding element to stack");
  }

  public void addGlobalElementToStack(int adr, int numOfElements) {
    instructions.add("# start of adding global element to stack");
    instructions.add("addi $a0 ,$a1 ," + adr);
    instructions.add("add $a1, $a0, $gp");
    for (int i = 0; i < numOfElements; i++) {
      instructions.add("lw $a0, 0($a1)");
      instructions.add("sw $a0, 0($sp)");
      instructions.add("addiu $sp, $sp, -4");
      instructions.add("addiu $a1, $a1, -4");
    }
    instructions.add("# end of adding global element to stack");
  }

  public void addGlobalElementAddressToStack(int adr) {
    instructions.add("# start of adding global element address to stack");
    instructions.add("addi $a0 ,$a1 ," + adr);
    instructions.add("add $a1, $a0, $gp");
    instructions.add("sw $a1 , 0($sp)");
    instructions.add("addiu $sp, $sp, -4");
    instructions.add("# end of adding element address to stack");
  }

  public void assignCommandArray(int numOfElements, boolean isLeftMost) {
    int offset = (numOfElements + 1) * 4;
    instructions.add("# start of assign command in Array");
    instructions.add("addi $sp, $sp," + offset);
    instructions.add("lw $a1, 0($sp)");
    instructions.add("addi $sp, $sp,-4");
    // instructions.add("addi $sp, $sp," + ((-1)*offset));
    for (int i = 0; i < numOfElements; i++) {
      instructions.add("lw $a0, 0($sp)");
      instructions.add("addi $sp, $sp, -4");

      // instructions.add("lw $a0, 4($sp)");
      // popStack();
      instructions.add("sw $a0, 0($a1)");
      instructions.add("addi $a1, $a1, -4");
    }
    if (isLeftMost) {
      for (int i = 0; i < numOfElements + 1; i++)
        popStack();
    } else {
      int offset2 = (numOfElements) * 4;
      instructions.add("addi $sp, $sp," + offset2);
      for (int i = 0; i < numOfElements; i++) {
        instructions.add("lw $a0, 0($sp)");
        instructions.add("sw $a0, 4($sp)");
        instructions.add("addi $sp, $sp, -4");
      }
      instructions.add("addi $sp, $sp, 4");
    }
    instructions.add("# end of assign command in Array");
  }

  public void assignCommandArrayVardef(int numOfElements) {
    int offset = (numOfElements) * 4;
    instructions.add("# start of assign command in Array var def");
    // instructions.add("addi $sp, $sp," + offset);
    instructions.add("lw $a1, 4($sp)");
    popStack();
    instructions.add("addi $sp, $sp," + offset);
    for (int i = 0; i < numOfElements; i++) {
      instructions.add("lw $a0, 0($sp)");
      instructions.add("addi $sp, $sp, -4");
      instructions.add("sw $a0, 0($a1)");
      instructions.add("addi $a1, $a1, -4");
    }
    instructions.add("# end of assign command in Array var def");
  }

  public void read(int num) {
    for (int i = 0; i < num; i++) {
      this.addSystemCall(12);
      instructions.add("sw $v0 , 0($sp)");
      instructions.add("addiu $sp, $sp, -4");
    }
  }

  public void addStringToStack(String str) {
    for (int i = 1; i < str.length() - 1; i++) {
      this.addCharToStack(str.charAt(i));
    }
  }

  public void equalityCheckArray(int size, String op) {
    String label1 = this.getLabel();
    String label2 = this.getLabel();
    instructions.add("move $a2, $sp");
    for (int i = 0; i < size; i++) {
      instructions.add("lw $a0, 4($sp)");
      instructions.add("lw $a1, " + ((size + 1) * 4) + "($sp)");
      instructions.add("bne $a0, $a1, " + label1);
      popStack();
    }
    instructions.add("move $sp, $a2");
    for (int i = 0; i < 2 * size; i++) {
      popStack();
    }
    if (op.equals("=="))
      instructions.add("li $a0, 1");
    else
      instructions.add("li $a0, 0");
    instructions.add("sw $a0, 0($sp)");
    instructions.add("addiu $sp, $sp, -4");
    instructions.add("j " + label2);

    instructions.add(label1 + ":");
    if (op.equals("=="))
      instructions.add("li $a0, 0");
    else
      instructions.add("li $a0, 1");
    instructions.add("sw $a0, 0($sp)");
    instructions.add("addiu $sp, $sp, -4");
    instructions.add(label2 + ":");
  }
}
