/*
 * Location:           /home/hao/workspace/AppContext/Instrument/IfInstrument.jar
 * Qualified Name:     app.IfStmtInstrument
 * Java Class Version: 7 (51.0)
 * JD-Core Version:    0.7.1
 */

package playAppContext;

import java.io.File;
import java.io.FilenameFilter;
import java.io.PrintStream;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import soot.ArrayType;
import soot.Body;
import soot.BodyTransformer;
import soot.BooleanType;
import soot.Local;
import soot.Pack;
import soot.PackManager;
import soot.PatchingChain;
import soot.PrimType;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Transform;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.jimple.AbstractStmtSwitch;
import soot.jimple.IfStmt;
import soot.jimple.Jimple;
import soot.jimple.LookupSwitchStmt;
import soot.jimple.TableSwitchStmt;
import soot.jimple.internal.JimpleLocal;
import soot.options.Options;
import soot.util.Chain;

public class IfStmtInstrument {
	public static void main(String[] args) {
		Options.v().set_src_prec(5);
		// apk file path
		String apkFileLocation = args[0];
		// platform path
		String androidJar = args[1];
		// libdir
		String extraJar = args[2];
		// output dir path
		String outputDir = args[3];

		Options.v().set_process_dir(Collections.singletonList(apkFileLocation));
		Options.v().set_android_jars(androidJar);
		Options.v().set_output_dir(outputDir);
		Options.v().set_allow_phantom_refs(true);
		// 10是什么类型？
		Options.v().set_output_format(10);
		// 不知道DummyClass是搞毛的
		Scene.v().addBasicClass("app.DummyClass", 2);
		// jimple transform (-> IR) package (phase)
		PackManager.v().getPack("jtp")
				.add(new Transform("jtp.myInstrumenter", new BodyTransformer() {
					protected void internalTransform(final Body b,
							String phaseName, Map options) {
						final PatchingChain<Unit> units = b.getUnits();

						for (Iterator<Unit> iter = units.snapshotIterator(); iter
								.hasNext();) {
							final Unit u = (Unit) iter.next();
							u.apply(new AbstractStmtSwitch() {
								public void caseIfStmt(IfStmt stmt) {
									Value condition = stmt.getCondition();
									List<ValueBox> values = condition
											.getUseBoxes();
									int size = values.size();

									SootClass klass = Scene.v().getSootClass(
											"app.DummyClass");
									SootMethod boolCall = klass
											.getMethod("void invokeIfStmt(boolean)");
									SootMethod objCall = klass
											.getMethod("void invokeIfStmt(java.lang.Object)");

									SootMethod primCall = Scene
											.v()
											.getSootClass("app.DummyClass")
											.getMethod(
													"void invokeIfStmt(double)");

									for (int i = 0; i < values.size(); i++) {
										ValueBox vBox = (ValueBox) values
												.get(i);
										Value v = vBox.getValue();
										System.out.println("Value: " + v);
										System.out.println("Class: "
												+ v.getClass());
										if ((v instanceof JimpleLocal)) {
											if ((v.getType() instanceof BooleanType)) {
												System.out.println("Type: "
														+ v.getType());
												units.insertBefore(
														Jimple.v()
																.newInvokeStmt(
																		Jimple.v()
																				.newStaticInvokeExpr(
																						boolCall.makeRef(),
																						v)),
														u);
											} else if ((v.getType() instanceof PrimType)) {
												System.out.println("Type: "
														+ v.getType());
												units.insertBefore(
														Jimple.v()
																.newInvokeStmt(
																		Jimple.v()
																				.newStaticInvokeExpr(
																						primCall.makeRef(),
																						v)),
														u);
											} else {
												units.insertBefore(
														Jimple.v()
																.newInvokeStmt(
																		Jimple.v()
																				.newStaticInvokeExpr(
																						objCall.makeRef(),
																						v)),
														u);
											}
										}
									}

									b.validate();
								}

								public void caseTableSwitchStmt(
										TableSwitchStmt stmt) {
									SootMethod objCall = Scene
											.v()
											.getSootClass("app.DummyClass")
											.getMethod(
													"void invokeIfStmt(java.lang.Object)");
									SootMethod boolCall = Scene
											.v()
											.getSootClass("app.DummyClass")
											.getMethod(
													"void invokeIfStmt(boolean)");
									SootMethod primCall = Scene
											.v()
											.getSootClass("app.DummyClass")
											.getMethod(
													"void invokeIfStmt(double)");

									Value v = stmt.getKey();
									System.out.println("Value: " + v);
									System.out.println("Class: " + v.getClass());
									if ((v instanceof JimpleLocal)) {
										if ((v.getType() instanceof BooleanType)) {
											System.out.println("Type: "
													+ v.getType());
											units.insertBefore(
													Jimple.v()
															.newInvokeStmt(
																	Jimple.v()
																			.newStaticInvokeExpr(
																					boolCall.makeRef(),
																					v)),
													u);
										} else if ((v.getType() instanceof PrimType)) {
											System.out.println("Type: "
													+ v.getType());
											units.insertBefore(
													Jimple.v()
															.newInvokeStmt(
																	Jimple.v()
																			.newStaticInvokeExpr(
																					primCall.makeRef(),
																					v)),
													u);
										} else {
											units.insertBefore(
													Jimple.v()
															.newInvokeStmt(
																	Jimple.v()
																			.newStaticInvokeExpr(
																					objCall.makeRef(),
																					v)),
													u);
										}
									}

									b.validate();
								}

								public void caseLookupSwitchStmt(
										LookupSwitchStmt stmt) {
									SootMethod objCall = Scene
											.v()
											.getSootClass("app.DummyClass")
											.getMethod(
													"void invokeIfStmt(java.lang.Object)");
									SootMethod boolCall = Scene
											.v()
											.getSootClass("app.DummyClass")
											.getMethod(
													"void invokeIfStmt(boolean)");
									SootMethod primCall = Scene
											.v()
											.getSootClass("app.DummyClass")
											.getMethod(
													"void invokeIfStmt(double)");

									Value v = stmt.getKey();
									System.out.println("Value: " + v);
									System.out.println("Class: " + v.getClass());
									if ((v instanceof JimpleLocal)) {
										if ((v.getType() instanceof BooleanType)) {
											System.out.println("Type: "
													+ v.getType());
											units.insertBefore(
													Jimple.v()
															.newInvokeStmt(
																	Jimple.v()
																			.newStaticInvokeExpr(
																					boolCall.makeRef(),
																					v)),
													u);
										} else if ((v.getType() instanceof PrimType)) {
											System.out.println("Type: "
													+ v.getType());
											units.insertBefore(
													Jimple.v()
															.newInvokeStmt(
																	Jimple.v()
																			.newStaticInvokeExpr(
																					primCall.makeRef(),
																					v)),
													u);
										} else {
											units.insertBefore(
													Jimple.v()
															.newInvokeStmt(
																	Jimple.v()
																			.newStaticInvokeExpr(
																					objCall.makeRef(),
																					v)),
													u);
										}
									}

									b.validate();

								}

							});
						}

					}

				}));
		String class_path = findJarfiles(extraJar) + File.pathSeparator
				+ androidJar + File.pathSeparator
				+ System.getProperty("java.class.path") + File.pathSeparator
				+ System.getProperty("java.home") + File.separator + "lib";

		String dummyclasspath = Scene.v().defaultClassPath()
				+ File.pathSeparator + class_path;
		System.out.println(dummyclasspath);
		Options.v().set_soot_classpath(dummyclasspath);
		Scene.v().loadNecessaryClasses();

		Scene.v().forceResolve("org.bouncycastle.asn1.DERObjectIdentifier", 3);
		PackManager.v().runPacks();
		PackManager.v().writeOutput();
	}

	public static String findJarfiles(String dirName) {
		File dir = new File(dirName);

		File[] jarFiles = dir.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String filename) {
				return filename.endsWith(".jar");
			}

		});
		StringBuffer sb = new StringBuffer();
		File[] arrayOfFile1;
		int j = (arrayOfFile1 = jarFiles).length;
		for (int i = 0; i < j; i++) {
			File f = arrayOfFile1[i];
			sb.append(f.getAbsolutePath());
			sb.append(File.pathSeparator);
		}
		return sb.toString();
	}

	public static void instrumentMain(String[] args) {
		Options.v().set_src_prec(5);

		String apkFileLocation = args[1];
		String androidJar = args[2];

		Options.v().set_process_dir(Collections.singletonList(apkFileLocation));
		Options.v().set_android_jars(androidJar);
		Options.v().set_output_dir(args[3]);

		Options.v().set_output_format(10);

		Scene.v().addBasicClass("app.DummyClass", 2);

		PackManager.v().getPack("jtp")
				.add(new Transform("jtp.myInstrumenter", new BodyTransformer() {
					protected void internalTransform(final Body b,
							String phaseName, Map options) {
						final PatchingChain<Unit> units = b.getUnits();

						for (Iterator<Unit> iter = units.snapshotIterator(); iter
								.hasNext();) {
							final Unit u = (Unit) iter.next();
							u.apply(new AbstractStmtSwitch() {
								public void caseIfStmt(IfStmt stmt) {
									Value condition = stmt.getCondition();
									List<ValueBox> values = condition
											.getUseBoxes();
									int size = values.size();

									SootMethod objCall = Scene
											.v()
											.getSootClass("app.DummyClass")
											.getMethod(
													"void invokeIfStmt(java.lang.Object)");
									SootMethod boolCall = Scene
											.v()
											.getSootClass("app.DummyClass")
											.getMethod(
													"void invokeIfStmt(boolean)");
									SootMethod primCall = Scene
											.v()
											.getSootClass("app.DummyClass")
											.getMethod(
													"void invokeIfStmt(double)");

									for (int i = 0; i < values.size(); i++) {
										ValueBox vBox = (ValueBox) values
												.get(i);
										Value v = vBox.getValue();
										System.out.println("Value: " + v);
										System.out.println("Class: "
												+ v.getClass());
										if ((v instanceof JimpleLocal)) {
											if ((v.getType() instanceof BooleanType)) {
												System.out.println("Type: "
														+ v.getType());
												units.insertBefore(
														Jimple.v()
																.newInvokeStmt(
																		Jimple.v()
																				.newStaticInvokeExpr(
																						boolCall.makeRef(),
																						v)),
														u);
											} else if ((v.getType() instanceof PrimType)) {
												System.out.println("Type: "
														+ v.getType());
												units.insertBefore(
														Jimple.v()
																.newInvokeStmt(
																		Jimple.v()
																				.newStaticInvokeExpr(
																						primCall.makeRef(),
																						v)),
														u);
											} else {
												units.insertBefore(
														Jimple.v()
																.newInvokeStmt(
																		Jimple.v()
																				.newStaticInvokeExpr(
																						objCall.makeRef(),
																						v)),
														u);
											}
										}
									}

									b.validate();
								}

								public void caseTableSwitchStmt(
										TableSwitchStmt stmt) {
									SootMethod objCall = Scene
											.v()
											.getSootClass("app.DummyClass")
											.getMethod(
													"void invokeIfStmt(java.lang.Object)");
									SootMethod boolCall = Scene
											.v()
											.getSootClass("app.DummyClass")
											.getMethod(
													"void invokeIfStmt(boolean)");
									SootMethod primCall = Scene
											.v()
											.getSootClass("app.DummyClass")
											.getMethod(
													"void invokeIfStmt(double)");

									Value v = stmt.getKey();
									System.out.println("Value: " + v);
									System.out.println("Class: " + v.getClass());
									if ((v instanceof JimpleLocal)) {
										if ((v.getType() instanceof BooleanType)) {
											System.out.println("Type: "
													+ v.getType());
											units.insertBefore(
													Jimple.v()
															.newInvokeStmt(
																	Jimple.v()
																			.newStaticInvokeExpr(
																					boolCall.makeRef(),
																					v)),
													u);
										} else if ((v.getType() instanceof PrimType)) {
											System.out.println("Type: "
													+ v.getType());
											units.insertBefore(
													Jimple.v()
															.newInvokeStmt(
																	Jimple.v()
																			.newStaticInvokeExpr(
																					primCall.makeRef(),
																					v)),
													u);
										} else {
											units.insertBefore(
													Jimple.v()
															.newInvokeStmt(
																	Jimple.v()
																			.newStaticInvokeExpr(
																					objCall.makeRef(),
																					v)),
													u);
										}
									}

									b.validate();
								}

								public void caseLookupSwitchStmt(
										LookupSwitchStmt stmt) {
									SootMethod objCall = Scene
											.v()
											.getSootClass("app.DummyClass")
											.getMethod(
													"void invokeIfStmt(java.lang.Object)");
									SootMethod boolCall = Scene
											.v()
											.getSootClass("app.DummyClass")
											.getMethod(
													"void invokeIfStmt(boolean)");
									SootMethod primCall = Scene
											.v()
											.getSootClass("app.DummyClass")
											.getMethod(
													"void invokeIfStmt(double)");

									Value v = stmt.getKey();
									System.out.println("Value: " + v);
									System.out.println("Class: " + v.getClass());
									if ((v instanceof JimpleLocal)) {
										if ((v.getType() instanceof BooleanType)) {
											System.out.println("Type: "
													+ v.getType());
											units.insertBefore(
													Jimple.v()
															.newInvokeStmt(
																	Jimple.v()
																			.newStaticInvokeExpr(
																					boolCall.makeRef(),
																					v)),
													u);
										} else if ((v.getType() instanceof PrimType)) {
											System.out.println("Type: "
													+ v.getType());
											units.insertBefore(
													Jimple.v()
															.newInvokeStmt(
																	Jimple.v()
																			.newStaticInvokeExpr(
																					primCall.makeRef(),
																					v)),
													u);
										} else {
											units.insertBefore(
													Jimple.v()
															.newInvokeStmt(
																	Jimple.v()
																			.newStaticInvokeExpr(
																					objCall.makeRef(),
																					v)),
													u);
										}
									}

									b.validate();

								}

							});
						}

					}

				}));
		String class_path = androidJar + File.pathSeparator
				+ System.getProperty("java.class.path") + File.pathSeparator
				+ System.getProperty("java.home") + File.separator + "lib"
				+ File.separator + "rt.jar";

		String dummyclasspath = Scene.v().defaultClassPath()
				+ File.pathSeparator + class_path;
		System.out.println(dummyclasspath);
		Options.v().set_soot_classpath(dummyclasspath);
		Scene.v().loadNecessaryClasses();

		PackManager.v().runPacks();
		PackManager.v().writeOutput();
	}

	private static Local addTmpArray(Body body) {
		Local tmpArray = Jimple.v().newLocal("tmpArray",
				ArrayType.v(RefType.v("java.lang.Object"), 1));
		body.getLocals().add(tmpArray);
		return tmpArray;
	}
}
