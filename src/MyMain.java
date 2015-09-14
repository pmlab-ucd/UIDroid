/* A small example of using Soot for intra-procedure data-flow analysis framework.
 * The example is extracted from Soot survivor pdf.
 * Implemented by Hao Fu.
 */

/* Soot - a J*va Optimization Framework
 * Copyright (C) 2008 Eric Bodden
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA 02111-1307, USA.
 */
import java.util.Map;

import soot.Body;
import soot.BodyTransformer;
import soot.G;
import soot.PackManager;
import soot.Transform;
import soot.Unit;
import soot.toolkits.graph.DirectedGraph;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.scalar.ArraySparseSet;
import soot.toolkits.scalar.BackwardFlowAnalysis;
import soot.toolkits.scalar.FlowSet;

public class MyMain {

	public static void main(String[] args) {
		PackManager.v().getPack("jtp").add(
				new Transform("jtp.myTransform", new BodyTransformer() {
					protected void internalTransform(Body body, String phase, Map options) {
						new VeryBusyExprAnalysis(new ExceptionalUnitGraph(body));
						// use G.v().out instead of System.out so that Soot can
						// redirect this output to the Eclipse console
						G.v().out.println(body.getMethod());
					}
					
				}));
		
		soot.Main.main(args);
	}
	
	/*
	 * perform a very busy expr analysis using Soot.
	 * nature: backward, must 
	 */
	public static class VeryBusyExprAnalysis extends BackwardFlowAnalysis<Object, Object>  {

		@SuppressWarnings("unchecked")
		public VeryBusyExprAnalysis(DirectedGraph<?> g) {
			// use superclass's constructor
			super((DirectedGraph<Object>) g);
			doAnalysis();
		}
		
		/*
		 * real work of the analysis happens, the actual flowing info through nodes(prog point: Unit in Soot) in the cfg.
		 * 1. kill: move info from IN set to the OUT set, excluding the info killed by the node
		 * 2. gen: add info to the OUT set that the node generates
		 * @see soot.toolkits.scalar.FlowAnalysis#flowThrough(java.lang.Object, java.lang.Object, java.lang.Object)
		 */
		@Override
		protected void flowThrough(Object in, Object node, Object out) {
			FlowSet inSet = (FlowSet)in,
					outSet = (FlowSet)out;
			Unit unit = (Unit)node;
			kill(inSet, unit, outSet);
			gen(outSet, unit);
		}
		
		private void kill(Object in, Object node, Object out) {
			FlowSet inSet = (FlowSet)in,
					outSet = (FlowSet)out;
			Unit unit = (Unit)node;
			
		}
		
		/*
		 * init contents of the lattice element for the nodes except the entry point
		 * init with the empty set
		 * @see soot.toolkits.scalar.AbstractFlowAnalysis#newInitialFlow()
		 */
		@Override
		protected Object newInitialFlow() {
			return new ValueArraySparseSet();
		}

		/*
		 * init contents of the lattice element for the entry point
		 * here the entry point is the last statement 
		 * init with the empty set
		 * @see soot.toolkits.scalar.AbstractFlowAnalysis#entryInitialFlow()
		 */
		@Override
		protected Object entryInitialFlow() {
			return new ValueArraySparseSet();
		}
		
		/*
		 * determine whether is a may (union) or must (intersect) analysis.
		 * the abstract method "merge" does not assume any representation format of lattice element,
		 * so the parameters are Object.
		 * we normally use FlowSet to represent a set and use it as the element 
		 * @see soot.toolkits.scalar.AbstractFlowAnalysis#merge(java.lang.Object, java.lang.Object, java.lang.Object)
		 */
		@Override
		protected void merge(Object in1, Object in2, Object out) {
			FlowSet inSet1 = (FlowSet)in1,
					inSet2 = (FlowSet)in2,
					outSet = (FlowSet)out;
			inSet1.intersection(inSet2, outSet);
		}
		
		/*
		 * since merge() leverage Object, we need a way to copy elements between lattice.
		 * As we use FlowSet, we directly use copy method designed for set inside Soot.
		 * @see soot.toolkits.scalar.AbstractFlowAnalysis#copy(java.lang.Object, java.lang.Object)
		 */
		@Override
		protected void copy(Object source, Object dest) {
			FlowSet srcSet = (FlowSet)source,
					destSet = (FlowSet)dest;
			// copy elements in srcSet to destSet.
			srcSet.copy(destSet);
		}

	}
	

}