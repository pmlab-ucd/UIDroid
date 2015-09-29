package playAppContext;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
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
	
	/*
	 * 参考自Test.java中的main()
	 */
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
				// 用于删除目录下所有文件
				success = (success) && (f.delete());
				if (!success) {
					System.err.println("Cleanup of output dir " + outputDir + " failed@");
				}
				outputDir.delete();
			}
		}
		
		// 读入额外的参数
		if (!parseAdditionalOptions(args)) {
			return;
		}
		if (!validateAdditionalOptions()) {
			return;
		}
		// 读入文件并判断是否是apk文件
		List<String> apkFiles = new ArrayList();
		File apkFile = new File(args[0]);
		if (apkFile.isDirectory()) {
			String[] dirFiles = apkFile.list(new FilenameFilter() {
			
				@Override
				public boolean accept(File dir, String name) {
					return (name.endsWith(".apk"));
				}
			
			});
			for (String s : dirFiles)
				apkFiles.add(s);
		} else {
			// 获得文件类型
			String extension = apkFile.getName().substring(apkFile.getName().lastIndexOf("."));
			if (extension.equalsIgnoreCase(".txt")) {
				BufferedReader rdr = new BufferedReader(new FileReader(apkFile));
				String line = null;
				while ((line = rdr.readLine()) != null)
					apkFiles.add(line);
				rdr.close();
			}
			else if (extension.equalsIgnoreCase(".apk"))
				apkFiles.add(args[0]);
			else {
				System.err.println("Invalid input file format: " + extension);
				return;
			}
		}
		
		
		for (final String fileName : apkFiles) {
			final String fullFilePath;
			System.gc();
			
			// Directory handling
			if (apkFiles.size() > 1) {
				if (apkFile.isDirectory())
					fullFilePath = args[0] + File.separator + fileName;
				else
					fullFilePath = fileName;
				System.out.println("Analyzing file " + fullFilePath + "...");
				File flagFile = new File("_Run_" + new File(fileName).getName());
				if (flagFile.exists())
					continue;
				flagFile.createNewFile();
			}
			else
				fullFilePath = fileName;

			// Run the analysis
				System.gc();
				if (timeout > 0) {
					runAnalysisTimeout(fullFilePath, args[1]);
				} else if (sysTimeout > 0) {
					runAnalysisSysTimeout(fullFilePath, args[1]);
				} else {
					// 这里多了个extraJar参数
					runAnalysis(fullFilePath, args[1], extraJar);
				}
				// AppContext独有的
				permissionAnalysis(fullFilePath, args[1], extraJar);
			
			System.gc();
		}				
	}
	
	private static void permissionAnalysis(String apkDir, String plarformDir,
			String extraJar) {
		
	}
	
	

}
