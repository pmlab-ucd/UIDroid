/**  
 * Copyright � 2016 Hao Fu. All rights reserved.
 *
 * @Title: SetupApplication.java
 * @Prject: UiDroid
 * @Package: slicer
 * @Description: TODO
 * @author: hao  
 * @date: Jan 19, 2016 9:53:26 PM
 * @version: V1.0  
 */
package slicer;

/**
 * @ClassName: SetupApplication
 * @Description: TODO
 * @author: hao
 * @date: Jan 19, 2016 9:53:26 PM
 */
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.activation.UnsupportedDataTypeException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlPullParserException;

import slicer.taintWrappers.ITaintPropagationWrapper;
import soot.Main;
import soot.PackManager;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.jimple.Stmt;
import soot.jimple.infoflow.android.AnalyzeJimpleClass;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration;
import soot.jimple.infoflow.android.data.AndroidMethod;
import soot.jimple.infoflow.android.data.parsers.PermissionMethodParser;
import soot.jimple.infoflow.android.manifest.ProcessManifest;
import soot.jimple.infoflow.android.resources.ARSCFileParser;
import soot.jimple.infoflow.android.resources.ARSCFileParser.AbstractResource;
import soot.jimple.infoflow.android.resources.ARSCFileParser.StringResource;
import soot.jimple.infoflow.android.resources.LayoutControl;
import soot.jimple.infoflow.android.resources.LayoutFileParser;
import soot.jimple.infoflow.android.source.AccessPathBasedSourceSinkManager;
import soot.jimple.infoflow.android.source.parsers.xml.XMLSourceSinkParser;
import soot.jimple.infoflow.cfg.BiDirICFGFactory;
import soot.jimple.infoflow.config.IInfoflowConfig;
import soot.jimple.infoflow.data.SootMethodAndClass;
import soot.jimple.infoflow.data.pathBuilders.DefaultPathBuilderFactory;
import soot.jimple.infoflow.entryPointCreators.AndroidEntryPointCreator;
import soot.jimple.infoflow.handlers.ResultsAvailableHandler;
import soot.jimple.infoflow.ipc.IIPCManager;
import soot.jimple.infoflow.results.InfoflowResults;
import soot.jimple.infoflow.rifl.RIFLSourceSinkDefinitionProvider;
import soot.jimple.infoflow.source.data.ISourceSinkDefinitionProvider;
import soot.jimple.infoflow.source.data.SourceSinkDefinition;
import soot.options.Options;

public class SetupApplication {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private ISourceSinkDefinitionProvider sourceSinkProvider;
	private final Map<String, Set<SootMethodAndClass>> callbackMethods =
			new HashMap<String, Set<SootMethodAndClass>>(10000);

	private InfoflowAndroidConfiguration config = new InfoflowAndroidConfiguration();
	
	private Set<String> entrypoints = null;
	private Set<String> callbackClasses = null;

	private List<ARSCFileParser.ResPackage> resourcePackages = null;
	private String appPackageName = "";

	private final String androidJar;
	private final boolean forceAndroidJar;
	private final String apkFileLocation;
	private final String additionalClasspath;
	private ITaintPropagationWrapper taintWrapper;
	
	private AccessPathBasedSourceSinkManager sourceSinkManager = null;
	private AndroidEntryPointCreator entryPointCreator = null;

	private IInfoflowConfig sootConfig = null;
	private BiDirICFGFactory cfgFactory = null;

	private IIPCManager ipcManager = null;
	
	private long maxMemoryConsumption = -1;
	
	private Set<Stmt> collectedSources = null;
	private Set<Stmt> collectedSinks = null;
	
	/**
	 * Creates a new instance of the {@link SetupApplication} class
	 * 
	 * @param androidJar
	 *            The path to the Android SDK's "platforms" directory if Soot shall automatically select the JAR file to
	 *            be used or the path to a single JAR file to force one.
	 * @param apkFileLocation
	 *            The path to the APK file to be analyzed
	 */
	public SetupApplication(String androidJar, String apkFileLocation) {
		this(androidJar, apkFileLocation, "", null);
	}

	/**
	 * Creates a new instance of the {@link SetupApplication} class
	 * 
	 * @param androidJar
	 *            The path to the Android SDK's "platforms" directory if
	 *            Soot shall automatically select the JAR file to
	 *            be used or the path to a single JAR file to force one.
	 * @param apkFileLocation
	 *            The path to the APK file to be analyzed
	 * @param ipcManager
	 *            The IPC manager to use for modelling inter-component and inter-application data flows
	 */
	public SetupApplication(String androidJar, String apkFileLocation,
			IIPCManager ipcManager) {
		this(androidJar, apkFileLocation, "", ipcManager);
	}
	
	/**
	 * Creates a new instance of the {@link SetupApplication} class
	 * 
	 * @param androidJar
	 *            The path to the Android SDK's "platforms" directory if
	 *            Soot shall automatically select the JAR file to
	 *            be used or the path to a single JAR file to force one.
	 * @param apkFileLocation
	 *            The path to the APK file to be analyzed
	 * @param ipcManager
	 *            The IPC manager to use for modelling inter-component and inter-application data flows
	 */
	public SetupApplication(String androidJar, String apkFileLocation,
			String additionalClasspath,
			IIPCManager ipcManager) {
		File f = new File(androidJar);
		this.forceAndroidJar = f.isFile();

		this.androidJar = androidJar;
		this.apkFileLocation = apkFileLocation;

		this.ipcManager = ipcManager;
		this.additionalClasspath = additionalClasspath;
	}
	
	/**
	 * Gets the set of sinks loaded into FlowDroid These are the sinks as
	 * they are defined through the SourceSinkManager.
	 * 
	 * @return The set of sinks loaded into FlowDroid
	 */
	public Set<SourceSinkDefinition> getSinks() {
		return this.sourceSinkProvider == null ? null
				: this.sourceSinkProvider.getSinks();
	}
	
	/**
	 * Gets the concrete instances of sinks that have been collected inside
	 * the app. This method returns null if source and sink logging has not
	 * been enabled (see InfoflowConfiguration.setLogSourcesAndSinks()).
	 * @return The set of concrete sink instances in the app
	 */
	public Set<Stmt> getCollectedSinks() {
		return collectedSinks;
	}

	/**
	 * Prints the list of sinks registered with FlowDroud to stdout
	 */
	public void printSinks() {
		if (this.sourceSinkProvider == null) {
			System.err.println("Sinks not calculated yet");
			return;
		}
		System.out.println("Sinks:");
		for (SourceSinkDefinition am : getSinks()) {
			System.out.println(am.toString());
		}
		System.out.println("End of Sinks");
	}

	/**
	 * Gets the set of sources loaded into FlowDroid. These are the sources as
	 * they are defined through the SourceSinkManager.
	 * 
	 * @return The set of sources loaded into FlowDroid
	 */
	public Set<SourceSinkDefinition> getSources() {
		return this.sourceSinkProvider == null ? null
				: this.sourceSinkProvider.getSources();
	}
	
	/**
	 * Gets the concrete instances of sources that have been collected inside
	 * the app. This method returns null if source and sink logging has not
	 * been enabled (see InfoflowConfiguration.setLogSourcesAndSinks()).
	 * @return The set of concrete source instances in the app
	 */
	public Set<Stmt> getCollectedSources() {
		return collectedSources;
	}

	/**
	 * Prints the list of sources registered with FlowDroud to stdout
	 */
	public void printSources() {
		if (this.sourceSinkProvider == null) {
			System.err.println("Sources not calculated yet");
			return;
		}
		System.out.println("Sources:");
		for (SourceSinkDefinition am : getSources()) {
			System.out.println(am.toString());
		}
		System.out.println("End of Sources");
	}

	/**
	 * Gets the set of classes containing entry point methods for the lifecycle
	 * 
	 * @return The set of classes containing entry point methods for the lifecycle
	 */
	public Set<String> getEntrypointClasses() {
		return entrypoints;
	}

	/**
	 * Prints list of classes containing entry points to stdout
	 */
	public void printEntrypoints() {
		if (this.entrypoints == null)
			System.out.println("Entry points not initialized");
		else {
			System.out.println("Classes containing entry points:");
			for (String className : entrypoints)
				System.out.println("\t" + className);
			System.out.println("End of Entrypoints");
		}
	}

	/**
	 * Sets the class names of callbacks.
	 *  If this value is null, it automatically loads the names from AndroidCallbacks.txt as the default behavior.
	 * @param callbackClasses
	 * 	        The class names of callbacks or null to use the default file.
	 */
	public void setCallbackClasses(Set<String> callbackClasses) {
		this.callbackClasses = callbackClasses;
	}

	public Set<String> getCallbackClasses() {
		return callbackClasses;
	}

	/**
	 * Sets the taint wrapper to be used for propagating taints over unknown (library) callees. If this value is null,
	 * no taint wrapping is used.
	 * 
	 * @param taintWrapper
	 *            The taint wrapper to use or null to disable taint wrapping
	 */
	public void setTaintWrapper(ITaintPropagationWrapper taintWrapper) {
		this.taintWrapper = taintWrapper;
	}

	/**
	 * Gets the taint wrapper to be used for propagating taints over unknown (library) callees. If this value is null,
	 * no taint wrapping is used.
	 * 
	 * @return The taint wrapper to use or null if taint wrapping is disabled
	 */
	public ITaintPropagationWrapper getTaintWrapper() {
		return this.taintWrapper;
	}

	/**
	 * Calculates the sets of sources, sinks, entry points, and callbacks methods for the given APK file.
	 * 
	 * @param sources
	 *            The methods that shall be considered as sources
	 * @param sinks
	 *            The methods that shall be considered as sinks
	 * @throws IOException
	 *             Thrown if the given source/sink file could not be read.
	 * @throws XmlPullParserException
	 *             Thrown if the Android manifest file could not be read.
	 */
	public void calculateSourcesSinksEntrypoints(Set<AndroidMethod> sources,
			Set<AndroidMethod> sinks) throws IOException, XmlPullParserException {
		final Set<SourceSinkDefinition> sourceDefs = new HashSet<>(sources.size());
		final Set<SourceSinkDefinition> sinkDefs = new HashSet<>(sinks.size());
		
		for (AndroidMethod am : sources)
			sourceDefs.add(new SourceSinkDefinition(am));
		for (AndroidMethod am : sinks)
			sinkDefs.add(new SourceSinkDefinition(am));
		
		ISourceSinkDefinitionProvider parser = new ISourceSinkDefinitionProvider() {
			
			@Override
			public Set<SourceSinkDefinition> getSources() {
				return sourceDefs;
			}
			
			@Override
			public Set<SourceSinkDefinition> getSinks() {
				return sinkDefs;
			}

			@Override
			public Set<SourceSinkDefinition> getAllMethods() {
				Set<SourceSinkDefinition> sourcesSinks = new HashSet<>(sourceDefs.size()
						+ sinkDefs.size());
				sourcesSinks.addAll(sourceDefs);
				sourcesSinks.addAll(sinkDefs);
				return sourcesSinks;
			}
			
		};
		
		calculateSourcesSinksEntrypoints(parser);
	}
	
	/**
	 * Calculates the sets of sources, sinks, entry points, and callback methods
	 * for the given APK file.
	 * 
	 * @param sourceSinkFile
	 *            The full path and file name of the file containing the sources and sinks
	 * @throws IOException
	 *             Thrown if the given source/sink file could not be read.
	 * @throws XmlPullParserException
	 *             Thrown if the Android manifest file could not be read.
	 */
	public void calculateSourcesSinksEntrypoints(String sourceSinkFile)
			throws IOException, XmlPullParserException {
		ISourceSinkDefinitionProvider parser = null;
		
		String fileExtension = sourceSinkFile.substring(sourceSinkFile.lastIndexOf("."));
		fileExtension = fileExtension.toLowerCase();
		
		try {
			if (fileExtension.equals(".xml"))
				parser = XMLSourceSinkParser.fromFile(sourceSinkFile);
			else if (fileExtension.equals(".txt"))
				parser = PermissionMethodParser.fromFile(sourceSinkFile);
			else if (fileExtension.equals(".rifl"))
				parser = new RIFLSourceSinkDefinitionProvider(sourceSinkFile);
			else
				throw new UnsupportedDataTypeException("The Inputfile isn't a .txt or .xml file.");
			
			calculateSourcesSinksEntrypoints(parser);
		}
		catch (SAXException ex) {
			throw new IOException("Could not read XML file", ex);
		}
	}

	/**
	 * Calculates the sets of sources, sinks, entry points, and callbacks methods for the given APK file.
	 * 
	 * @param sourcesAndSinks
	 *            A provider from which the analysis can obtain the list of
	 *            sources and sinks
	 * @throws IOException
	 *             Thrown if the given source/sink file could not be read.
	 * @throws XmlPullParserException
	 *             Thrown if the Android manifest file could not be read.
	 */
	public void calculateSourcesSinksEntrypoints(ISourceSinkDefinitionProvider sourcesAndSinks)
			throws IOException, XmlPullParserException {
		// To look for callbacks, we need to start somewhere. We use the Android
		// lifecycle methods for this purpose.
		this.sourceSinkProvider = sourcesAndSinks;
		ProcessManifest processMan = new ProcessManifest(apkFileLocation);
		this.appPackageName = processMan.getPackageName();
		this.entrypoints = processMan.getEntryPointClasses();

		// Parse the resource file
		long beforeARSC = System.nanoTime();
		ARSCFileParser resParser = new ARSCFileParser();
		resParser.parse(apkFileLocation);
		logger.info("ARSC file parsing took " + (System.nanoTime() - beforeARSC) / 1E9 + " seconds");
		this.resourcePackages = resParser.getPackages();

		// Add the callback methods
		LayoutFileParser lfp = null;
		if (config.getEnableCallbacks()) {
			lfp = new LayoutFileParser(this.appPackageName, resParser);
			calculateCallbackMethods(resParser, lfp);

			// Some informational output
			System.out.println("Found " + lfp.getUserControls() + " layout controls");
		}
		
		System.out.println("Entry point calculation done.");

		// Clean up everything we no longer need
		soot.G.reset();

		// Create the SourceSinkManager
		{
			Set<SootMethodAndClass> callbacks = new HashSet<>();
			for (Set<SootMethodAndClass> methods : this.callbackMethods.values())
				callbacks.addAll(methods);

			sourceSinkManager = new AccessPathBasedSourceSinkManager(
					this.sourceSinkProvider.getSources(),
					this.sourceSinkProvider.getSinks(),
					callbacks,
					config.getLayoutMatchingMode(),
					lfp == null ? null : lfp.getUserControlsByID());

			sourceSinkManager.setAppPackageName(this.appPackageName);
			sourceSinkManager.setResourcePackages(this.resourcePackages);
			sourceSinkManager.setEnableCallbackSources(this.config.getEnableCallbackSources());
		}

		entryPointCreator = createEntryPointCreator();
	}

	/**
	 * Adds a method to the set of callback method
	 * 
	 * @param layoutClass
	 *            The layout class for which to register the callback
	 * @param callbackMethod
	 *            The callback method to register
	 */
	private void addCallbackMethod(String layoutClass, AndroidMethod callbackMethod) {
		Set<SootMethodAndClass> methods = this.callbackMethods.get(layoutClass);
		if (methods == null) {
			methods = new HashSet<SootMethodAndClass>();
			this.callbackMethods.put(layoutClass, methods);
		}
		methods.add(new AndroidMethod(callbackMethod));
	}

	/**
	 * Calculates the set of callback methods declared in the XML resource files or the app's source code
	 * 
	 * @param resParser
	 *            The binary resource parser containing the app resources
	 * @param lfp
	 *            The layout file parser to be used for analyzing UI controls
	 * @throws IOException
	 *             Thrown if a required configuration cannot be read
	 */
	private void calculateCallbackMethods(ARSCFileParser resParser, LayoutFileParser lfp) throws IOException {
		AnalyzeJimpleClass jimpleClass = null;

		boolean hasChanged = true;
		while (hasChanged) {
			hasChanged = false;

			// Create the new iteration of the main method
			soot.G.reset();
			initializeSoot();
			createMainMethod();

			if (jimpleClass == null) {
				// Collect the callback interfaces implemented in the app's
				// source code
				if (callbackClasses == null) {
					jimpleClass = new AnalyzeJimpleClass(config, entrypoints);
				} else {
					jimpleClass = new AnalyzeJimpleClass(config, entrypoints, callbackClasses);
				}
				jimpleClass.collectCallbackMethods();

				// Find the user-defined sources in the layout XML files. This
				// only needs to be done once, but is a Soot phase.
				lfp.parseLayoutFile(apkFileLocation, entrypoints);
			} else
				jimpleClass.collectCallbackMethodsIncremental();

			// Run the soot-based operations
			PackManager.v().getPack("wjpp").apply();
			PackManager.v().getPack("cg").apply();
			PackManager.v().getPack("wjtp").apply();

			// Collect the results of the soot-based phases
			for (Entry<String, Set<SootMethodAndClass>> entry : jimpleClass.getCallbackMethods().entrySet()) {
				if (this.callbackMethods.containsKey(entry.getKey())) {
					if (this.callbackMethods.get(entry.getKey()).addAll(entry.getValue()))
						hasChanged = true;
				} else {
					this.callbackMethods.put(entry.getKey(), new HashSet<>(entry.getValue()));
					hasChanged = true;
				}
			}
			
			if (entrypoints.addAll(jimpleClass.getDynamicManifestComponents()))
				hasChanged = true;
		}

		// Collect the XML-based callback methods
		for (Entry<String, Set<Integer>> lcentry : jimpleClass.getLayoutClasses().entrySet()) {
			final SootClass callbackClass = Scene.v().getSootClass(lcentry.getKey());

			for (Integer classId : lcentry.getValue()) {
				AbstractResource resource = resParser.findResource(classId);
				if (resource instanceof StringResource) {
					final String layoutFileName = ((StringResource) resource).getValue();

					// Add the callback methods for the given class
					Set<String> callbackMethods = lfp.getCallbackMethods().get(layoutFileName);
					if (callbackMethods != null) {
						for (String methodName : callbackMethods) {
							final String subSig = "void " + methodName + "(android.view.View)";

							// The callback may be declared directly in the
							// class
							// or in one of the superclasses
							SootClass currentClass = callbackClass;
							while (true) {
								SootMethod callbackMethod = currentClass.getMethodUnsafe(subSig);
								if (callbackMethod != null) {
									addCallbackMethod(callbackClass.getName(), new AndroidMethod(callbackMethod));
									break;
								}
								if (!currentClass.hasSuperclass()) {
									System.err.println("Callback method " + methodName + " not found in class "
											+ callbackClass.getName());
									break;
								}
								currentClass = currentClass.getSuperclass();
							}
						}
					}

					// For user-defined views, we need to emulate their
					// callbacks
					Set<LayoutControl> controls = lfp.getUserControls().get(layoutFileName);
					if (controls != null)
						for (LayoutControl lc : controls)
							registerCallbackMethodsForView(callbackClass, lc);
				} else
					System.err.println("Unexpected resource type for layout class");
			}
		}

		// Add the callback methods as sources and sinks
		{
			Set<SootMethodAndClass> callbacksPlain = new HashSet<SootMethodAndClass>();
			for (Set<SootMethodAndClass> set : this.callbackMethods.values())
				callbacksPlain.addAll(set);
			System.out.println("Found " + callbacksPlain.size() + " callback methods for "
					+ this.callbackMethods.size() + " components");
		}
	}
	
	/**
	 * Registers the callback methods in the given layout control so that they
	 * are included in the dummy main method
	 * @param callbackClass The class with which to associate the layout
	 * callbacks
	 * @param lc The layout control whose callbacks are to be associated with
	 * the given class
	 */
	private void registerCallbackMethodsForView(SootClass callbackClass, LayoutControl lc) {
		// Ignore system classes
		if (callbackClass.getName().startsWith("android."))
			return;
		if (lc.getViewClass().getName().startsWith("android."))
			return;
		
		// Check whether the current class is actually a view
		{
			SootClass sc = lc.getViewClass();
			boolean isView = false;
			while (sc.hasSuperclass()) {
				if (sc.getName().equals("android.view.View")) {
					isView = true;
					break;
				}
				sc = sc.getSuperclass();
			}
			if (!isView)
				return;
		}

		// There are also some classes that implement interesting callback
		// methods.
		// We model this as follows: Whenever the user overwrites a method in an
		// Android OS class, we treat it as a potential callback.
		SootClass sc = lc.getViewClass();
		Set<String> systemMethods = new HashSet<String>(10000);
		for (SootClass parentClass : Scene.v().getActiveHierarchy().getSuperclassesOf(sc)) {
			if (parentClass.getName().startsWith("android."))
				for (SootMethod sm : parentClass.getMethods())
					if (!sm.isConstructor())
						systemMethods.add(sm.getSubSignature());
		}

		// Scan for methods that overwrite parent class methods
		for (SootMethod sm : sc.getMethods())
			if (!sm.isConstructor())
				if (systemMethods.contains(sm.getSubSignature()))
					// This is a real callback method
					addCallbackMethod(callbackClass.getName(), new AndroidMethod(sm));
	}

	/**
	 * Creates the main method based on the current callback information, injects it into the Soot scene.
	 */
	private void createMainMethod() {
		// Always update the entry point creator to reflect the newest set
		// of callback methods
		SootMethod entryPoint = createEntryPointCreator().createDummyMain();
		Scene.v().setEntryPoints(Collections.singletonList(entryPoint));
		if (Scene.v().containsClass(entryPoint.getDeclaringClass().getName()))
			Scene.v().removeClass(entryPoint.getDeclaringClass());
		Scene.v().addClass(entryPoint.getDeclaringClass());
	}

	/**
	 * Gets the source/sink manager constructed for FlowDroid. Make sure to call calculateSourcesSinksEntryPoints()
	 * first, or you will get a null result.
	 * 
	 * @return FlowDroid's source/sink manager
	 */
	public AccessPathBasedSourceSinkManager getSourceSinkManager() {
		return sourceSinkManager;
	}
	
	/**
	 * Builds the classpath for this analysis
	 * @return The classpath to be used for the taint analysis
	 */
	private String getClasspath() {
		String classpath = forceAndroidJar ? androidJar
				: Scene.v().getAndroidJarPath(androidJar, apkFileLocation);
		if (this.additionalClasspath != null && !this.additionalClasspath.isEmpty())
			classpath += File.pathSeparator + this.additionalClasspath;
		return classpath;
	}

	/**
	 * Initializes soot for running the soot-based phases of the application metadata analysis
	 */
	private void initializeSoot() {
		Options.v().set_no_bodies_for_excluded(true);
		Options.v().set_allow_phantom_refs(true);
		Options.v().set_output_format(Options.output_format_none);
		Options.v().set_whole_program(true);
		Options.v().set_process_dir(Collections.singletonList(apkFileLocation));
		Options.v().set_soot_classpath(getClasspath());
		if (forceAndroidJar)
			Options.v().set_force_android_jar(androidJar);
		else
			Options.v().set_android_jars(androidJar);
		Options.v().set_src_prec(Options.src_prec_apk_class_jimple);
		Options.v().set_keep_line_number(false);
		Options.v().set_keep_offset(false);
		Main.v().autoSetOptions();

		// Configure the callgraph algorithm
		switch (config.getCallgraphAlgorithm()) {
		case AutomaticSelection:
			Options.v().setPhaseOption("cg.spark", "on");
			break;
		case RTA:
			Options.v().setPhaseOption("cg.spark", "on");
			Options.v().setPhaseOption("cg.spark", "rta:true");
			break;
		case VTA:
			Options.v().setPhaseOption("cg.spark", "on");
			Options.v().setPhaseOption("cg.spark", "vta:true");
			break;
		default:
			throw new RuntimeException("Invalid callgraph algorithm");
		}

		// Load whetever we need
		Scene.v().loadNecessaryClasses();
	}

	/**
	 * Runs the data flow analysis
	 * 
	 * @return The results of the data flow analysis
	 */
	public InfoflowResults runInfoflow() {
		return runInfoflow(null);
	}

	/**
	 * Runs the data flow analysis. Make sure to populate the sets of sources, sinks, and entry points first.
	 * 
	 * @param onResultsAvailable
	 *            The callback to be invoked when data flow results are available
	 * @return The results of the data flow analysis
	 */
	public InfoflowResults runInfoflow(ResultsAvailableHandler onResultsAvailable) {
		if (this.sourceSinkProvider == null)
			throw new RuntimeException("Sources and/or sinks not calculated yet");

		System.out.println("Running data flow analysis on " + apkFileLocation + " with " + getSources().size()
				+ " sources and " + getSinks().size() + " sinks...");
		Infoflow info;
		if (cfgFactory == null)
			info = new Infoflow(androidJar, forceAndroidJar, null,
					new DefaultPathBuilderFactory(config.getPathBuilder(),
							config.getComputeResultPaths()));
		else
			info = new Infoflow(androidJar, forceAndroidJar, cfgFactory,
					new DefaultPathBuilderFactory(config.getPathBuilder(),
							config.getComputeResultPaths()));

		final String path;
		if (forceAndroidJar)
			path = androidJar;
		else
			path = Scene.v().getAndroidJarPath(androidJar, apkFileLocation);

		info.setTaintWrapper(taintWrapper);
		if (onResultsAvailable != null)
			info.addResultsAvailableHandler(onResultsAvailable);

		System.out.println("Starting infoflow computation...");
		info.setConfig(config);
		info.setSootConfig(sootConfig);
		
		if (null != ipcManager) {
			info.setIPCManager(ipcManager);
		}

		info.computeInfoflow(apkFileLocation, path, entryPointCreator, sourceSinkManager, sourceSinkProvider);
		this.maxMemoryConsumption = info.getMaxMemoryConsumption();
		this.collectedSources = info.getCollectedSources();
		this.collectedSinks = info.getCollectedSinks();

		return info.getResults();
	}

	private AndroidEntryPointCreator createEntryPointCreator() {
		AndroidEntryPointCreator entryPointCreator = new AndroidEntryPointCreator(new ArrayList<String>(
				this.entrypoints));
		Map<String, List<String>> callbackMethodSigs = new HashMap<String, List<String>>();
		for (String className : this.callbackMethods.keySet()) {
			List<String> methodSigs = new ArrayList<String>();
			callbackMethodSigs.put(className, methodSigs);
			for (SootMethodAndClass am : this.callbackMethods.get(className))
				methodSigs.add(am.getSignature());
		}
		entryPointCreator.setCallbackFunctions(callbackMethodSigs);
		return entryPointCreator;
	}

	/**
	 * Gets the entry point creator used for generating the dummy main method emulating the Android lifecycle and the
	 * callbacks. Make sure to call calculateSourcesSinksEntryPoints() first, or you will get a null result.
	 * 
	 * @return The entry point creator
	 */
	public AndroidEntryPointCreator getEntryPointCreator() {
		return entryPointCreator;
	}
	
	/**
	 * Gets the extra Soot configuration options to be used when running the analysis
	 * 
	 * @return The extra Soot configuration options to be used when running the analysis, null if the defaults shall be
	 *         used
	 */
	public IInfoflowConfig getSootConfig() {
		return this.sootConfig;
	}

	/**
	 * Sets the extra Soot configuration options to be used when running the analysis
	 * 
	 * @param config
	 *            The extra Soot configuration options to be used when running the analysis, null if the defaults shall
	 *            be used
	 */
	public void setSootConfig(IInfoflowConfig config) {
		this.sootConfig = config;
	}

	/**
	 * Sets the factory class to be used for constructing interprocedural control flow graphs
	 * 
	 * @param factory
	 *            The factory to be used. If null is passed, the default factory is used.
	 */
	public void setIcfgFactory(BiDirICFGFactory factory) {
		this.cfgFactory = factory;
	}
	
	/**
	 * Gets the maximum memory consumption during the last analysis run
	 * 
	 * @return The maximum memory consumption during the last analysis run if available, otherwise -1
	 */
	public long getMaxMemoryConsumption() {
		return this.maxMemoryConsumption;
	}
	
	/**
	 * Gets the data flow configuration
	 * @return The current data flow configuration
	 */
	public InfoflowAndroidConfiguration getConfig() {
		return this.config;
	}
	
	/**
	 * Sets the data flow configuration
	 * @param config The new data flow configuration
	 */
	public void setConfig(InfoflowAndroidConfiguration config) {
		this.config = config;
	}
	
}