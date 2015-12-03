/*
 * Gui analysis
 * reference: gator
 */
package playGator;

import java.util.Set;

import com.google.common.collect.Sets;

import presto.android.Hierarchy;
import presto.android.xml.XMLParser;


public class GUIAnalysis {
	public XMLParser xmlParser;
	public Hierarchy hier;
	
	public Set<Integer> allLayoutIds = Sets.newHashSet();
	public Set<Integer> allMenuIds = Sets.newHashSet();
	public Set<Integer> allWidgetIds = Sets.newHashSet();
	public Set<Integer> allStringIds = Sets.newHashSet();
	
	// The nested class to implement singleton
	private static class SingletonHolder {
		private static final GUIAnalysis instance = new GUIAnalysis(Hierarchy.v(), XMLParser.Factory.getXMLParser());
	}
	
	// Get THE instance
	public static final GUIAnalysis v() {
		return SingletonHolder.instance;
	}
	
	public GUIAnalysis(Hierarchy hier, XMLParser xmlParser) {
		this.hier = hier;
		this.xmlParser = xmlParser;
	}
	
	public void retrieveIds() {
		// the layout ids 
		//System.out.println("Activities: ");
		for (int id : xmlParser.getApplicationLayoutIdValues()) {
			//System.out.println(id);
			allLayoutIds.add(id);
		}
		
		for (int id : xmlParser.getSystemLayoutIdValues()) {
			//System.out.println(id);
			allLayoutIds.add(id);
		}
		
		// The menu ids
		//System.out.println("Menus: ");
		for (int id : xmlParser.getApplicationMenuIdValues()) {
			//System.out.println(id);
			allMenuIds.add(id);
		}
	    for (int id : xmlParser.getSystemMenuIdValues()) {
	        allMenuIds.add(id);
	    }
		
		// The widget ids
		//System.out.println("Widgets: ");
		for (int id : xmlParser.getApplicationRIdValues()) {
			//System.out.println(id);
			allWidgetIds.add(id);
		}
	    for (int id : xmlParser.getSystemRIdValues()) {
	        allWidgetIds.add(id);
	    }
	    for (int id : xmlParser.getStringIdValues()) {
	        allStringIds.add(id);
	    }
		
		System.out.println("[XML] Layout Ids: " + allLayoutIds.size()
		        + ", Menu Ids: " + allMenuIds.size() + ", Widget Ids: "
		        + allWidgetIds.size() + ", String Ids: " + allStringIds.size());
		    System.out.println("[XML] MainActivity: " + xmlParser.getMainActivity());
	}
	
	public Flowgraph flowgraph;
	public FixpointSolver fixpointSolver;
	public DemandVariableValueQuery variableValueQueryInterface;
	public DefaultGUIAnalysisOutput output;
	
	public void run() {
		System.out.println("[GUIAnalysis] Start");
		//long startTime = System.nanoTime();
		
		// 0. Retrieve ui ids 
		retrieveIds();
		
		// 1. Construct constraint flow graph 
	    flowgraph = new Flowgraph(hier, allLayoutIds, allMenuIds, allWidgetIds,
	            allStringIds);
	    flowgraph.build();
	    flowgraph.printNodes();
	    
	    // 2. Fix-point computation
	    fixpointSolver = new FixpointSolver(flowgraph);
	    fixpointSolver.solve();
	    
	    // 3. Variable value query interface
	    variableValueQueryInterface = DemandVariableValueQuery.v(flowgraph, fixpointSolver);

	    // 4. Construct the output
	    output = new DefaultGUIAnalysisOutput(this);
	    System.out.println(output.getAppPackageName());
	}
	
}
