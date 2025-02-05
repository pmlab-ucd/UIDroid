/**  
 * Copyright � 2016 Hao Fu. All rights reserved.
 *
 * @Title: AbstractInfoflow.java
 * @Prject: UiDroid
 * @Package: slicer
 * @Description: TODO
 * @author: hao  
 * @date: Jan 20, 2016 11:03:11 AM
 * @version: V1.0  
 */
package slicer;

/**
 * @ClassName: AbstractInfoflow
 * @Description: TODO
 * @author: hao
 * @date: Jan 20, 2016 11:03:11 AM
 */
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import slicer.nativ.DefaultNativeCallHandler;
import slicer.nativ.INativeCallHandler;
import slicer.taintWrappers.ITaintPropagationWrapper;
import soot.PackManager;
import soot.Scene;
import soot.SootClass;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.InfoflowConfiguration.CallgraphAlgorithm;
import soot.jimple.infoflow.cfg.BiDirICFGFactory;
import soot.jimple.infoflow.cfg.DefaultBiDiICFGFactory;
import soot.jimple.infoflow.cfg.LibraryClassPatcher;
import soot.jimple.infoflow.config.IInfoflowConfig;
import soot.jimple.infoflow.data.pathBuilders.DefaultPathBuilderFactory;
import soot.jimple.infoflow.data.pathBuilders.IPathBuilderFactory;
import soot.jimple.infoflow.entryPointCreators.DefaultEntryPointCreator;
import soot.jimple.infoflow.entryPointCreators.IEntryPointCreator;
import soot.jimple.infoflow.handlers.PreAnalysisHandler;
import soot.jimple.infoflow.ipc.DefaultIPCManager;
import soot.jimple.infoflow.ipc.IIPCManager;
import soot.jimple.infoflow.source.DefaultSourceSinkManager;
import soot.jimple.infoflow.source.data.ISourceSinkDefinitionProvider;
import soot.options.Options;

/**
 * Abstract base class for all data/information flow analyses in FlowDroid
 * 
 * @author Steven Arzt
 *
 */
public abstract class AbstractInfoflow implements IInfoflow {
	
	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	protected IPathBuilderFactory pathBuilderFactory;
	protected InfoflowConfiguration config = new InfoflowConfiguration();
	protected ITaintPropagationWrapper taintWrapper;
	protected INativeCallHandler nativeCallHandler = new DefaultNativeCallHandler();
	protected IIPCManager ipcManager = new DefaultIPCManager(new ArrayList<String>());
	
	protected final BiDirICFGFactory icfgFactory;
	protected Collection<? extends PreAnalysisHandler> preProcessors = Collections.emptyList();
	
	protected final String androidPath;
	protected final boolean forceAndroidJar;
	protected IInfoflowConfig sootConfig;
	
    /**
     * Creates a new instance of the abstract info flow problem
     */
    public AbstractInfoflow() {
    	this(null, "", false);
    }

    /**
     * Creates a new instance of the abstract info flow problem
     * @param icfgFactory The interprocedural CFG to be used by the InfoFlowProblem
	 * @param androidPath If forceAndroidJar is false, this is the base directory
	 * of the platform files in the Android SDK. If forceAndroidJar is true, this
	 * is the full path of a single android.jar file.
	 * @param forceAndroidJar True if a single platform JAR file shall be forced,
	 * false if Soot shall pick the appropriate platform version 
     */
    public AbstractInfoflow(BiDirICFGFactory icfgFactory,
    		String androidPath, boolean forceAndroidJar) {
    	if (icfgFactory == null)
    		this.icfgFactory = new DefaultBiDiICFGFactory();
    	else
    		this.icfgFactory = icfgFactory;
		this.androidPath = androidPath;
		this.forceAndroidJar = forceAndroidJar;
		this.pathBuilderFactory = new DefaultPathBuilderFactory();
    }
    
    @Override
	public InfoflowConfiguration getConfig() {
    	return this.config;
    }

    @Override
    public void setConfig(InfoflowConfiguration config) {
    	this.config = config;
    }

    @Override
	public void setTaintWrapper(ITaintPropagationWrapper wrapper) {
		taintWrapper = wrapper;
	}
    
    @Override
    public void setNativeCallHandler(INativeCallHandler handler) {
    	this.nativeCallHandler = handler;
    }
    
    @Override
    public ITaintPropagationWrapper getTaintWrapper() {
    	return taintWrapper;
    }
    
	@Override
	public void setPreProcessors(Collection<? extends PreAnalysisHandler> preprocessors) {
        this.preProcessors = preprocessors;
	}

	@Override
	public void computeInfoflow(String appPath, String libPath,
			IEntryPointCreator entryPointCreator,
			List<String> sources, List<String> sinks) {
		this.computeInfoflow(appPath, libPath, entryPointCreator,
				new DefaultSourceSinkManager(sources, sinks));
	}
	
	public void computeInfoflow(String appPath, String libPath,
			IEntryPointCreator entryPointCreator,
			List<String> sources, List<String> sinks, ISourceSinkDefinitionProvider sourceSinkProvider) {
		this.computeInfoflow(appPath, libPath, entryPointCreator,
				new DefaultSourceSinkManager(sources, sinks), sourceSinkProvider);
	}

	@Override
	public void computeInfoflow(String appPath, String libPath,
			Collection<String> entryPoints, 
			Collection<String> sources,
			Collection<String> sinks) {
		this.computeInfoflow(appPath, libPath, new DefaultEntryPointCreator(entryPoints),
				new DefaultSourceSinkManager(sources, sinks));
	}

	@Override
	public void computeInfoflow(String libPath, String appPath,
			String entryPoint, Collection<String> sources, Collection<String> sinks) {
		this.computeInfoflow(appPath, libPath, entryPoint, new DefaultSourceSinkManager(sources, sinks));
	}
	
	/**
	 * Appends two elements to build a classpath
	 * @param appPath The first entry of the classpath
	 * @param libPath The second entry of the classpath
	 * @return The concatenated classpath
	 */
	private String appendClasspath(String appPath, String libPath) {
		String s = (appPath != null && !appPath.isEmpty()) ? appPath : "";
		
		if (libPath != null && !libPath.isEmpty()) {
			if (!s.isEmpty())
				s += File.pathSeparator;
			s += libPath;
		}
		return s;
	}
	
	/**
	 * Initializes Soot.
	 * @param appPath The application path containing the analysis client
	 * @param libPath The Soot classpath containing the libraries
	 * @param classes The set of classes that shall be checked for data flow
	 * analysis seeds. All sources in these classes are used as seeds.
	 * @param sourcesSinks The manager object for identifying sources and sinks
	 */
	protected void initializeSoot(String appPath, String libPath, Collection<String> classes) {
		initializeSoot(appPath, libPath, classes,  "");
	}
	
	/**
	 * Initializes Soot.
	 * @param appPath The application path containing the analysis client
	 * @param libPath The Soot classpath containing the libraries
	 * @param classes The set of classes that shall be checked for data flow
	 * analysis seeds. All sources in these classes are used as seeds. If a
	 * non-empty extra seed is given, this one is used too.
	 */
	protected void initializeSoot(String appPath, String libPath, Collection<String> classes,
			String extraSeed) {
		// reset Soot:
		logger.info("Resetting Soot...");
		soot.G.reset();
				
		Options.v().set_no_bodies_for_excluded(true);
		Options.v().set_allow_phantom_refs(true);
		if (logger.isDebugEnabled())
			Options.v().set_output_format(Options.output_format_jimple);
		else
			Options.v().set_output_format(Options.output_format_none);
		
		// We only need to distinguish between application and library classes
		// if we use the OnTheFly ICFG
		if (config.getCallgraphAlgorithm() == CallgraphAlgorithm.OnDemand) {
			Options.v().set_soot_classpath(libPath);
			if (appPath != null) {
				List<String> processDirs = new LinkedList<String>();
				for (String ap : appPath.split(File.pathSeparator))
					processDirs.add(ap);
				Options.v().set_process_dir(processDirs);
			}
		}
		else
			Options.v().set_soot_classpath(appendClasspath(appPath, libPath));
		
		// Configure the callgraph algorithm
		switch (config.getCallgraphAlgorithm()) {
			case AutomaticSelection:
				// If we analyze a distinct entry point which is not static,
				// SPARK fails due to the missing allocation site and we fall
				// back to CHA.
				if (extraSeed == null || extraSeed.isEmpty()) {
					Options.v().setPhaseOption("cg.spark", "on");
					Options.v().setPhaseOption("cg.spark", "string-constants:true");
				}
				else
					Options.v().setPhaseOption("cg.cha", "on");
				break;
			case CHA:
				Options.v().setPhaseOption("cg.cha", "on");
				break;
			case RTA:
				Options.v().setPhaseOption("cg.spark", "on");
				Options.v().setPhaseOption("cg.spark", "rta:true");
				Options.v().setPhaseOption("cg.spark", "string-constants:true");
				break;
			case VTA:
				Options.v().setPhaseOption("cg.spark", "on");
				Options.v().setPhaseOption("cg.spark", "vta:true");
				Options.v().setPhaseOption("cg.spark", "string-constants:true");
				break;
			case SPARK:
				Options.v().setPhaseOption("cg.spark", "on");
				Options.v().setPhaseOption("cg.spark", "string-constants:true");
				break;
			case OnDemand:
				// nothing to set here
				break;
			default:
				throw new RuntimeException("Invalid callgraph algorithm");
		}
		
		// Specify additional options required for the callgraph
		if (config.getCallgraphAlgorithm() != CallgraphAlgorithm.OnDemand) {
			Options.v().set_whole_program(true);
			Options.v().setPhaseOption("cg", "trim-clinit:false");
		}

		// do not merge variables (causes problems with PointsToSets)
		Options.v().setPhaseOption("jb.ulp", "off");
		
		if (!this.androidPath.isEmpty()) {
			Options.v().set_src_prec(Options.src_prec_apk_class_jimple);
			if (this.forceAndroidJar)
				soot.options.Options.v().set_force_android_jar(this.androidPath);
			else
				soot.options.Options.v().set_android_jars(this.androidPath);
		} else
			Options.v().set_src_prec(Options.src_prec_java);
		
		//at the end of setting: load user settings:
		if (sootConfig != null)
			sootConfig.setSootOptions(Options.v());
		
		// load all entryPoint classes with their bodies
		for (String className : classes)
			Scene.v().addBasicClass(className, SootClass.BODIES);
		Scene.v().loadNecessaryClasses();
		logger.info("Basic class loading done.");
		
		boolean hasClasses = false;
		for (String className : classes) {
			SootClass c = Scene.v().forceResolve(className, SootClass.BODIES);
			if (c != null){
				c.setApplicationClass();
				if(!c.isPhantomClass() && !c.isPhantom())
					hasClasses = true;
			}
		}
		if (!hasClasses) {
			logger.error("Only phantom classes loaded, skipping analysis...");
			return;
		}
	}
	
	@Override
	public void setSootConfig(IInfoflowConfig config) {
		sootConfig = config;
	}
	
	@Override
	public void setIPCManager(IIPCManager ipcManager) {
	    this.ipcManager = ipcManager;
	}
	
	@Override
	public void setPathBuilderFactory(IPathBuilderFactory factory) {
		this.pathBuilderFactory = factory;
	}
	
	/**
	 * Constructs the callgraph
	 */
	protected void constructCallgraph() {
		// Allow the ICC manager to change the Soot Scene before we continue
		ipcManager.updateJimpleForICC();

		// Run the preprocessors
        for (PreAnalysisHandler tr : preProcessors)
            tr.onBeforeCallgraphConstruction();
        
        // Patch the system libraries we need for callgraph construction
        LibraryClassPatcher patcher = new LibraryClassPatcher();
        patcher.patchLibraries();
		
        // To cope with broken APK files, we convert all classes that are still
        // dangling after resolution into phantoms
        for (SootClass sc : Scene.v().getClasses())
        	if (sc.resolvingLevel() == SootClass.DANGLING) {
        		sc.setResolvingLevel(SootClass.BODIES);
        		sc.setPhantomClass();
        	}
        
		// We explicitly select the packs we want to run for performance
        // reasons. Do not re-run the callgraph algorithm if the host
        // application already provides us with a CG.
		if (config.getCallgraphAlgorithm() != CallgraphAlgorithm.OnDemand
				&& !Scene.v().hasCallGraph()) {
	        PackManager.v().getPack("wjpp").apply();
	        PackManager.v().getPack("cg").apply();
		}
		
		// Run the preprocessors
        for (PreAnalysisHandler tr : preProcessors)
            tr.onAfterCallgraphConstruction();
	}

}