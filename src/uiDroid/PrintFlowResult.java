package uiDroid;

import java.io.File;
import java.util.List;

import soot.Body;
import soot.Local;
import soot.NormalUnitPrinter;
import soot.Unit;
import soot.UnitPrinter;
import soot.toolkits.graph.UnitGraph;

public class PrintFlowResult {
	public static void print(UnitGraph cfg, 
			Body body, UiFlowAnalysis ta) {
		for (Unit unit : cfg) {
			List<Local> before = ta.getUILocalsBefore(unit);
			List<Local> after = ta.getUILocalsAfter(unit);
			
				String sep = File.separator;
				UnitPrinter up = new NormalUnitPrinter(body);
				up.setIndent("");
				System.out.println("---------------------------------------");
				unit.toString(up);
				System.out.println(up.output());
				if (!before.isEmpty()) {
					if (unit.toString().contains("sink")) {
						System.out.println("found a sink!");
					}
				}
				System.out.print("Ui event handlers in: {");
				sep = "";
				for (Local l : before) {
					System.out.print(sep);
					System.out.print(l.getName() + ": " + l.getType());
					sep = ", ";
				}
				System.out.println("}");
				System.out.print("Ui event handlers out: {");
				sep = "";
				for (Local l : after) {
					System.out.print(sep);
					System.out.print(l.getName() + ": " + l.getType());
					sep = ", ";
				}
				System.out.println("}");
				System.out.println("---------------------------------------");
		}
	}
}
