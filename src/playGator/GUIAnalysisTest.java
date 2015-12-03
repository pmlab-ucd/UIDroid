package playGator;

import static org.junit.Assert.*;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import presto.android.Configs;
import presto.android.gui.clients.GUIHierarchyPrinterClient;
import presto.android.gui.graph.NIdNode;
import presto.android.gui.graph.NNode;
import presto.android.gui.graph.NObjectNode;
import presto.android.gui.graph.NOpNode;
import presto.android.gui.graph.NWidgetIdNode;
//import presto.android.xml.AndroidView;




import presto.android.gui.rep.GUIHierarchy.Activity;
import presto.android.gui.rep.GUIHierarchy.Dialog;
import presto.android.gui.rep.GUIHierarchy.EventAndHandler;
import presto.android.gui.rep.GUIHierarchy.View;
import soot.Scene;
import soot.SootMethod;

import com.google.common.collect.Sets;

public class GUIAnalysisTest {
	GUIAnalysis ga;
	FixpointSolver solver;
	DefaultGUIAnalysisOutput output;
	String tag = "[GUITest]";

	@Before
	public void setup() {
		Main.parseArgs(new String[1]);
		ga = GUIAnalysis.v();
		// creep, fail when put ga.run() here
	}

	public void getListener() {
		Set<NNode> listeners = Sets.newHashSet();
		assertNotEquals(solver, null);
		assertNotEquals(solver.solutionListeners, null);
		for (Map.Entry<NOpNode, Set<NNode>> entry : solver.solutionListeners
				.entrySet()) {
			if (entry.getKey().artificial) {
				continue;
			}
			listeners.addAll(entry.getValue());
			// System.out.println(tag + entry + ":: ");
			for (NNode node : entry.getValue()) {
				System.out.print(tag + entry + ":: ");
				System.out.println(node);
			}
		}
		System.out.println("Size: " + listeners.size());
	}

	public void getViewIds() {
		for (Entry<NOpNode, Set<NIdNode>> entry : solver.reachingViewIds
				.entrySet()) {
			for (NIdNode node : entry.getValue()) {
				System.out.print(tag + entry + ":: ");
				System.out.println(node);
				// System.out.println(output.getExplicitEventsAndTheirHandlers(node);
			}
		}
	}

	public void getViews() {
		for (Entry<NOpNode, Set<NOpNode>> entry : solver.reachedReceiverViews
				.entrySet()) {
			if (entry.getKey().artificial) {
				continue;
			}
			for (NNode node : entry.getValue()) {
				//if (node instanceof NObjectNode) {
				System.out.print(tag + entry + ":: ");
				System.out.println(node);
				try {
					System.out
							.println(output
									.getExplicitEventsAndTheirHandlers((NObjectNode) node));
				} catch (Exception e) {
					System.err.println(e.getStackTrace());
				}
				//}
			}
		}
	}
	
	  private PrintStream out;
	  private int indent;
	  StaticGUIHierarchy guiHier;
	public void getHier() {
	   guiHier = new StaticGUIHierarchy(output);
	    // Init the file io
	    try {
	      File file = File.createTempFile(Configs.benchmarkName + "-", ".xml");
	      log("XML file: " + file.getAbsolutePath());
	      out = new PrintStream(file);
	    } catch (Exception e) {
	      throw new RuntimeException(e);
	    }

	    // Start printing
	    GUIHierarchyPrinterClient printer = new GUIHierarchyPrinterClient();
	    printf("<GUIHierarchy app=\"%s\">\n", guiHier.app);
	    printActivities();
	    printDialogs();
	    printf("</GUIHierarchy>\n");
	    
	    // Finish
	    out.flush();
	    out.close();
	}
	
	public void printRootViewAndHierarchy(ArrayList<View> roots) {
	    indent += 2;
	    for (View rootView : roots) {
	      printView(rootView);
	    }
	    indent -= 2;
	  }

	  public void printActivities() {
	    for (Activity act : guiHier.activities) {
	      indent += 2;
	      printf("<Activity name=\"%s\">\n", act.name);

	      // Roots & view hierarchy (including OptionsMenu)
	      printRootViewAndHierarchy(act.views);

	      printf("</Activity>\n");
	      indent -= 2;
	    }
	  }

	  public void printDialogs() {
	    for (Dialog dialog : guiHier.dialogs) {
	      indent += 2;
	      printf("<Dialog name=\"%s\" allocLineNumber=\"%d\" allocStmt=\"%s\" allocMethod=\"%s\">\n",
	          dialog.name, dialog.allocLineNumber,
	          xmlSafe(dialog.allocStmt), xmlSafe(dialog.allocMethod));
	      printRootViewAndHierarchy(dialog.views);
	      printf("</Dialog>\n");
	      indent -= 2;
	    }
	  }

	  public String xmlSafe(String s) {
	    return s
	        .replaceAll("&", "&amp;")
	        .replaceAll("\"", "&quot;")
	        .replaceAll("'", "&apos;")
	        .replaceAll("<", "&lt;")
	        .replaceAll(">", "&gt;");

	  }

	  // WARNING: remember to remove the node before exit. Very prone to error!!!
	  public void printView(View view) {
	    // <View type=... id=... idName=... text=... title=...>
	    //   <View ...>
	    //     ...
	    //   </View>
	    //   <EventAndHandler event=... handler=... />
	    // </View>

	    String type = String.format(" type=\"%s\"", view.type);
	    String id = String.format(" id=\"%d\"", view.id);
	    String idName = String.format(" idName=\"%s\"", view.idName);
	    // TODO(tony): add the text attribute for TextView and so on
	    String text = "";
	    // title for MenuItem
	    String title = "";
	    if (view.title != null) {
	      if (!type.contains("MenuItem")) {
	        throw new RuntimeException(type + " has a title field!");
	      }
	      title = String.format(" title=\"%s\"", xmlSafe(view.title));
	    }
	    String head =
	        String.format("<View%s%s%s%s%s>\n", type, id, idName, text, title);
	    printf(head);

	    {
	      // This includes both children and context menus
	      for (View child : view.views) {
	        indent += 2;
	        printView(child);
	        indent -= 2;
	      }
	      // Events and handlers
	      for (EventAndHandler eventAndHandler : view.eventAndHandlers) {
	        indent += 2;
	        String handler = eventAndHandler.handler;
	        String safeRealHandler = "";
	        if (handler.startsWith("<FakeName_")) {
	          SootMethod fake = Scene.v().getMethod(handler);
	          SootMethod real = output.getRealHandler(fake);
	          safeRealHandler = String.format(
	              " realHandler=\"%s\"", xmlSafe(real.getSignature()));
	        }
	        printf("<EventAndHandler event=\"%s\" handler=\"%s\"%s />\n",
	            eventAndHandler.event, xmlSafe(eventAndHandler.handler), safeRealHandler);
	        indent -= 2;
	      }
	    }

	    String tail = "</View>\n";
	    printf(tail);
	  }
	  
	  void printf(String format, Object... args) {
		    for (int i = 0; i < indent; i++) {
		      out.print(' ');
		    }
		    out.printf(format, args);
		  }

		  void log(String s) {
		    System.out.println(
		        "\033[1;31m[GUIHierarchyPrinterClient] " + s + "\033[0m");
		  }

	/*
	 * @Test public void testRetrieveIds() { ga.retrieveIds(); AndroidView view
	 * = ga.xmlParser.findViewById(2131230720); assertNotEquals(view, null);
	 * System.out.println("text:" + view.getText()); }
	 */

	@Test
	public void testRun() {
		ga.run();
		output = ga.output;
		solver = output.getSolver();
		assertNotEquals(output, null);
		assertNotEquals(solver, null);
		//getListener();
		// getViews();
		getHier();
	}
}
