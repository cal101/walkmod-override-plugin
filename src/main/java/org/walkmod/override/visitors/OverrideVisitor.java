/* 
  Copyright (C) 2015 Raquel Pau
 
 Walkmod is free software: you can redistribute it and/or modify
 it under the terms of the GNU Lesser General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.
 
 Walkmod is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Lesser General Public License for more details.
 
 You should have received a copy of the GNU Lesser General Public License
 along with Walkmod.  If not, see <http://www.gnu.org/licenses/>.*/
package org.walkmod.override.visitors;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.walkmod.javalang.ast.MethodSymbolData;
import org.walkmod.javalang.ast.SymbolData;
import org.walkmod.javalang.ast.body.MethodDeclaration;
import org.walkmod.javalang.ast.body.ModifierSet;
import org.walkmod.javalang.ast.expr.AnnotationExpr;
import org.walkmod.javalang.ast.expr.MarkerAnnotationExpr;
import org.walkmod.javalang.ast.expr.NameExpr;
import org.walkmod.javalang.compiler.analyze.OverrideAnalyzer;
import org.walkmod.javalang.compiler.symbols.RequiresSemanticAnalysis;
import org.walkmod.javalang.visitors.VoidVisitorAdapter;
import org.walkmod.walkers.VisitorContext;

@RequiresSemanticAnalysis
public class OverrideVisitor extends VoidVisitorAdapter<VisitorContext> {

   private boolean containsOverrideAsAnnotationExpr(MethodDeclaration md) {
      List<AnnotationExpr> mAnnotations = md.getAnnotations();
      boolean containsOverride = false;
      if (mAnnotations != null) {
         // To avoid a JDK8 bug
         // (http://stackoverflow.com/questions/26515016/annotations-on-an-overridden-method-are-ignored-in-java-8-propertydescriptor)
         Iterator<AnnotationExpr> it = mAnnotations.iterator();
         while (it.hasNext() && !containsOverride) {
            AnnotationExpr ae = it.next();
            SymbolData sd = ae.getSymbolData();
            if (sd != null) {
               Class<?> clazz = sd.getClazz();
               containsOverride = clazz.equals(Override.class);
            }
            else{
               containsOverride = "Override".equals(ae.getName().getName());
            }
         }
      }
      return containsOverride;
   }

   private boolean containsOverrideInByteCode(MethodDeclaration md) {
      boolean isAnnotationPresent = true;
      MethodSymbolData sdata = md.getSymbolData();
      Method method = sdata.getMethod();
      try {
         isAnnotationPresent = method.isAnnotationPresent(Override.class);
      } catch (Throwable t) {
         //maybe there are private classes that cannot be accessed by reflection
         //http://stackoverflow.com/questions/8512207/jetty-guice-illegalaccesserror
      }
      return isAnnotationPresent;
   }

   private boolean containsAnEquivalentParentMethod(MethodDeclaration md) {
      MethodSymbolData sdata = md.getSymbolData();
      boolean result = false;
      if (sdata != null) {
         if (!containsOverrideAsAnnotationExpr(md) && !containsOverrideInByteCode(md)) {
            result = OverrideAnalyzer.isMethodOverride(md);
         }
      }
      return result;
   }

   @Override
   public void visit(MethodDeclaration md, VisitorContext arg) {
      if (!ModifierSet.isStatic(md.getModifiers()) && !ModifierSet.isPrivate(md.getModifiers())) {

         if (containsAnEquivalentParentMethod(md)) {
            List<AnnotationExpr> annotations = md.getAnnotations();
            if (annotations == null) {
               annotations = new LinkedList<AnnotationExpr>();
               md.setAnnotations(annotations);
            }

            annotations.add(new MarkerAnnotationExpr(new NameExpr("Override")));
         }

      }

      super.visit(md, arg);
   }
}
