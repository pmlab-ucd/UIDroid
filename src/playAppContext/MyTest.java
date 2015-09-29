package playAppContext;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;

import soot.jimple.infoflow.android.TestApps.Test;



public class MyTest extends Test {
	// the list of sensitive methods: got from Pscout
	private static List<String> PscoutMethod;
	private static String csvName;
	
	public static void main(String[] args) 
			throws IOException, InterruptedException {
		myTestMain(args);
	}
	
	public static void myTestMain(String[] args) 
			throws IOException, InterruptedException {
		// 读入Pscout
		PscoutMethod = FileUtils.readLines(new File("./jellybean_publishedapimapping_parsed.txt"));
		// 参数过少， 打印帮助
		if (args.length < 2) {
			printUsage();
			return;
		}
		// 输出成csv格式
		csvName = args[2];
		// 额外的lib
		String extraJar = args[3];
		
		File outputDir = new File("JimpleOutput");
		if (outputDir.isDirectory()) {
			boolean success = true;
			File[] arrayOfFile;
			// 获得文件夹下文件数目
			int j = (arrayOfFile = outputDir.listFiles()).length; 
			// 遍历文件
			for (int i = 0; i < j; i++) {
				File f = arrayOfFile[i];
			}
		}
	}
	
	private static void printUsage()
	   {
	     System.out.println("FlowDroid (c) Secure Software Engineering Group @ EC SPRIDE");
	     System.out.println();
	     System.out.println("Incorrect arguments: [0] = apk-file, [1] = android-jar-directory");
	     System.out.println("Optional further parameters:");
	     System.out.println("\t--TIMEOUT n Time out after n seconds");
	     System.out.println("\t--SYSTIMEOUT n Hard time out (kill process) after n seconds, Unix only");
	     System.out.println("\t--SINGLEFLOW Stop after finding first leak");
	     System.out.println("\t--IMPLICIT Enable implicit flows");
	     System.out.println("\t--NOSTATIC Disable static field tracking");
	     System.out.println("\t--NOEXCEPTIONS Disable exception tracking");
	     System.out.println("\t--APLENGTH n Set access path length to n");
	     System.out.println("\t--CGALGO x Use callgraph algorithm x");
	     System.out.println("\t--NOCALLBACKS Disable callback analysis");
	     System.out.println("\t--LAYOUTMODE x Set UI control analysis mode to x");
	     System.out.println("\t--ALIASFLOWINS Use a flow insensitive alias search");
	     System.out.println("\t--NOPATHS Do not compute result paths");
	     System.out.println("\t--AGGRESSIVETW Use taint wrapper in aggressive mode");
	     System.out.println("\t--PATHALGO Use path reconstruction algorithm x");
	     System.out.println("\t--LIBSUMTW Use library summary taint wrapper");
	     System.out.println("\t--SUMMARYPATH Path to library summaries");
	     System.out.println("\t--SYSFLOWS Also analyze classes in system packages");
	     System.out.println("\t--NOTAINTWRAPPER Disables the use of taint wrappers");
	     System.out.println("\t--NOTYPETIGHTENING Disables the use of taint wrappers");
	     System.out.println();
	     System.out.println("Supported callgraph algorithms: AUTO, CHA, RTA, VTA, SPARK");
	     System.out.println("Supported layout mode algorithms: NONE, PWD, ALL");
	     System.out.println("Supported path algorithms: CONTEXTSENSITIVE, CONTEXTINSENSITIVE, SOURCESONLY");
	   }
}
