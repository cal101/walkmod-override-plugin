package org.walkmod.override.visitors;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.walkmod.javalang.ast.MethodSymbolData;
import org.walkmod.javalang.ast.SymbolData;
import org.walkmod.javalang.ast.body.MethodDeclaration;
import org.walkmod.javalang.ast.body.Parameter;
import org.walkmod.javalang.ast.expr.AnnotationExpr;
import org.walkmod.javalang.ast.expr.MarkerAnnotationExpr;
import org.walkmod.javalang.ast.expr.NameExpr;
import org.walkmod.javalang.compiler.reflection.MethodInspector;
import org.walkmod.javalang.compiler.symbols.RequiresSemanticAnalysis;
import org.walkmod.javalang.visitors.VoidVisitorAdapter;
import org.walkmod.walkers.VisitorContext;

@RequiresSemanticAnalysis
public class OverrideVisitor extends VoidVisitorAdapter<VisitorContext> {

	@Override
	public void visit(MethodDeclaration md, VisitorContext arg) {
		MethodSymbolData sdata = md.getSymbolData();
		if (sdata != null) {

			Method method = sdata.getMethod();

			if (!method.isAnnotationPresent(Override.class)) {

				Class<?> declaringClass = method.getDeclaringClass();
				Class<?> parentClass = declaringClass.getSuperclass();

				if (parentClass != null) {

					// it should be initialized after resolving the method

					List<Parameter> params = md.getParameters();
					SymbolData[] args = null;
					if (params != null) {
						args = new SymbolData[params.size()];
						int i = 0;
						for (Parameter param : params) {
							args[i] = param.getType().getSymbolData();
							i++;
						}
					} else {
						args = new SymbolData[0];
					}

					List<Class<?>> scopesToCheck = new LinkedList<Class<?>>();
					scopesToCheck.add(parentClass);
					Class<?>[] interfaces = declaringClass.getInterfaces();
					for (int i = 0; i < interfaces.length; i++) {
						scopesToCheck.add(interfaces[i]);
					}
					Iterator<Class<?>> it = scopesToCheck.iterator();
					boolean found = false;
					while (it.hasNext() && !found) {
						found = (MethodInspector.findMethod(it.next(), args,
								md.getName()) != null);
					}
					if (found) {
						List<AnnotationExpr> annotations = md.getAnnotations();
						if (annotations == null) {
							annotations = new LinkedList<AnnotationExpr>();
							md.setAnnotations(annotations);
						}

						annotations.add(new MarkerAnnotationExpr(new NameExpr(
								"Override")));
					}
				}
			}
		}
	}
}
