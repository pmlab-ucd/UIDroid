package playAppContext;


import java.util.ArrayList;
import java.util.List;
import soot.SootMethod;
import soot.jimple.Ref;
import soot.jimple.Stmt;

public class Context
{
  List<Stmt> conditionalStmt;
  List<SootMethod> factorMethod;
  SootMethod entrypoint;
  List<Ref> factorRef;
  
  public SootMethod getEntrypoint()
  {
/* 18 */     return entrypoint;
  }
  
  public void setEntrypoint(SootMethod entrypoint) {
/* 22 */     this.entrypoint = entrypoint;
  }
  
  public List<Stmt> getConditionalStmt() {
/* 26 */     return conditionalStmt;
  }
  
  public void setConditionalStmt(List<Stmt> conditionalStmt) {
/* 30 */     this.conditionalStmt = conditionalStmt;
  }
  
  public void addFactorMethod(SootMethod v) {
/* 34 */     if (factorMethod == null)
/* 35 */       factorMethod = new ArrayList();
/* 36 */     factorMethod.add(v);
  }
  
  public boolean hasFactorMethod(SootMethod v) {
/* 40 */     if (factorMethod == null)
/* 41 */       factorMethod = new ArrayList();
/* 42 */     return factorMethod.contains(v);
  }
  

  public List<SootMethod> getFactorMethod()
  {
/* 48 */     return factorMethod;
  }
  
  public void setFactorMethod(List<SootMethod> factorMethod) {
/* 52 */     this.factorMethod = factorMethod;
  }
  
  public List<Ref> getFactorRef() {
/* 56 */     return factorRef;
  }
  
  public void setFactorRef(List<Ref> factorRef) {
/* 60 */     this.factorRef = factorRef;
  }
  
  public void addFactorRef(Ref r) {
/* 64 */     if (factorRef == null)
/* 65 */       factorRef = new ArrayList();
/* 66 */     factorRef.add(r);
  }
  
/* 69 */   public boolean hasFactorRef(Ref r) { if (factorRef == null)
/* 70 */       factorRef = new ArrayList();
/* 71 */     return factorRef.contains(r);
  }
}

/* Location:           /home/hao/workspace/AppContext/Core/Main.jar
 * Qualified Name:     app.Context
 * Java Class Version: 7 (51.0)
 */
