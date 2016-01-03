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
	public boolean debug = false;

	public Set<Integer> allLayoutIds = Sets.newHashSet();
	public Set<Integer> allMenuIds = Sets.newHashSet();
	public Set<Integer> allWidgetIds = Sets.newHashSet();
	public Set<Integer> allStringIds = Sets.newHashSet();

	public void print(String str) {
		System.err.println("[GUIAnalysis] Debug: " + str);
	}

	// The nested class to implement singleton
	private static class SingletonHolder {
		private static final GUIAnalysis instance = new GUIAnalysis(
				Hierarchy.v(), XMLParser.Factory.getXMLParser());
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
		if (debug) {
			print("Activities: ");
		}
		for (int id : xmlParser.getApplicationLayoutIdValues()) {
			if (debug) {
				print("id: " + id);
			}
			allLayoutIds.add(id);
		}

		for (int id : xmlParser.getSystemLayoutIdValues()) {
			// print(id);
			allLayoutIds.add(id);
		}

		// The menu ids
		if (debug)
			print("Menus: ");
		for (int id : xmlParser.getApplicationMenuIdValues()) {
			if (debug)
				print("id: " + id);
			allMenuIds.add(id);
		}
		for (int id : xmlParser.getSystemMenuIdValues()) {
			allMenuIds.add(id);
		}

		// The widget ids
		if (debug)
			print("Widgets: ");
		for (int id : xmlParser.getApplicationRIdValues()) {
			if (debug)
				print("id: " + id);
			allWidgetIds.add(id);
		}
		for (int id : xmlParser.getSystemRIdValues()) {
			allWidgetIds.add(id);
		}
		for (int id : xmlParser.getStringIdValues()) {
			allStringIds.add(id);
		}

		print("[XML] Layout Ids: " + allLayoutIds.size() + ", Menu Ids: "
				+ allMenuIds.size() + ", Widget Ids: " + allWidgetIds.size()
				+ ", String Ids: " + allStringIds.size());
		print("[XML] MainActivity: " + xmlParser.getMainActivity());
	}

	public Flowgraph flowgraph;
	public FixpointSolver fixpointSolver;
	public DemandVariableValueQuery variableValueQueryInterface;
	public DefaultGUIAnalysisOutput output;

	public void run() {
		// try {
		print("Start");
		// long startTime = System.nanoTime();

		// 0. Retrieve ui ids
		retrieveIds();

		// 1. Construct constraint flow graph
		flowgraph = new Flowgraph(hier, allLayoutIds, allMenuIds, allWidgetIds,
				allStringIds);
		flowgraph.build();
		// print("FlowGraph Build: ");
		// flowgraph.printNodes();

		// 2. Fix-point computation
		fixpointSolver = new FixpointSolver(flowgraph);

		fixpointSolver.solve();

		// 3. Variable value query interface
		variableValueQueryInterface = DemandVariableValueQuery.v(flowgraph,
				fixpointSolver);

		// 4. Construct the output
		output = new DefaultGUIAnalysisOutput(this);
		print(output.getAppPackageName());
		// } catch (Exception e) {
		// System.err.println(e.getStackTrace());
		// }
	}

}
