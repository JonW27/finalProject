//place data and file reading functions here
import java.io.*;
import java.util.*;
public class Model{
  public static boolean yesNoPrompt(String prompt){
    Scanner reader = new Scanner(System.in);
    System.out.println(prompt+" (y/n)");
    String result = reader.nextLine();
    if(result.equals("y")){
      return true;
    }
    else if(result.equals("n")){
      return false;
    }
    else{
      System.out.println("Not in proper format... please try again.");
      yesNoPrompt(prompt);
    }
    return false;
  }
  public static void addWhichOne(Boolean discord, Boolean messenger, Boolean slack, PrintWriter writer){
    Scanner reader = new Scanner(System.in);
    System.out.println("Which credentials would you like to add? "+" (discord/messenger/slack)");
    String result = reader.nextLine();
    String record = "";
    if(result.equals("discord") && !discord){
      record += "discord:";
      discord = true;
    }
    else if(result.equals("messenger") && !messenger){
      record += "messenger:";
      messenger = true;
    }
    else if(result.equals("slack") && !slack){
      record += "slack:";
      slack = true;
    }
    else{
      System.out.println("Not recognized... or already have credentials for channel.");
      addWhichOne(discord, messenger, slack, writer);
      return;
    }
    Scanner reader2 = new Scanner(System.in);
    System.out.println("Username of account:");
    result += ":";
    result += reader.nextLine();
    result += ":";
    Scanner reader3 = new Scanner(System.in);
    System.out.println("Password of account:");
    result += reader.nextLine();
    writer.println(result);
    if(yesNoPrompt("Would you like to repeat for a different channel?")){
      addWhichOne(discord, messenger, slack, writer);
    }

  }
  public static void checkForSettings(){
    File metadata = new File("mnf.botto");
    if(!metadata.exists()){
      System.out.println("A mnf.botto file was not detected... is this your first time setting up botto?");
      if(yesNoPrompt("Would you like me to create a mnf.botto file for you?")){
        try{
          PrintWriter writer = new PrintWriter("mnf.botto", "UTF-8");
          addWhichOne(false, false, false, writer);
          writer.close();
        }
        catch(IOException e){
          e.printStackTrace();
        }

      }
    }
  }
}
