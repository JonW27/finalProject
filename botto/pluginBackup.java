
import org.openqa.selenium.WebDriver;
class Plugin0 extends Controller{/*can also extend Discord and Messenger
    maybe able to extend other plugins*/
    Plugin0(int index,WebDriver driver){
	super(index,"PluginNameGoesHere",driver);
    }
    Plugin0(int index,int parentIndex, WebDriver driver){
	super(index,parentIndex,"PluginNameGoesHere",driver);
    }
    boolean startup(){
	/*insert startup code here*/
	return true;
    }
    boolean tick(){
	/*insert tick code here*/
	return true;
    }
    void runPluginDash(){
	try{
	    /*
	    if(commandCheck(String commandName,Boolean unlimitedInputs?,int minInputs,int maxInputs)){
	    commandThatsSupposedToRun();
	    }
	    else if(commandCheck...........
	    you get the point
	    */
	}
	catch(Exception e){
	    makeErrorReport(e);
	}
    }
    void runPlugin(){
	try{
	    /*same as runPluginDash() except this is for commands that start without a "-"*/
	}
	catch(Exception e){
	    makeErrorReport(e);
	}
    }
    Controller nextPlugin(int index,WebDriver driver){
	Controller plugin = new Plugin1(index,driver);
	return plugin;
    }
    /*only used for extension type plugins*/
    Controller nextPlugin(int index,int parentIndex,WebDriver driver){
	Controller plugin = new Plugin1(index,parentIndex,driver);
	return plugin;
    }
}
