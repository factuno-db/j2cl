/*
 * Copyright 2015 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.j2cl.ast.visitors;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.j2cl.ast.AbstractRewriter;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.Node;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.TypeDescriptors;
import com.google.j2cl.ast.TypeReference;
import com.google.j2cl.ast.common.JsUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Normalizes the static native js method calls with the real native method calls.
 *
 * <p>For example, <pre>
 * class A {
 *   {@literal @}JsMethod(namespace="Math")
 *   static native double abs(double x);
 * }
 * </pre>
 *
 * <p>A.abs() really refers to Javascript built-in Math.abs(). This pass replaces all method calls
 * to A.abs() with Math.abs().
 */
public class NormalizeNativeMethodCalls extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(new Rewriter());
  }

  private static class Rewriter extends AbstractRewriter {
    @Override
    public Node rewriteMethodCall(MethodCall methodCall) {
      MethodDescriptor methodDescriptor = methodCall.getTarget();
      if (!methodDescriptor.isStatic()
          || !methodDescriptor.isNative()
          || !methodDescriptor.hasJsNamespace()) {
        return methodCall;
      }
      String qualifiedName = methodDescriptor.getJsNamespace();
      TypeDescriptor nativeTypeDescriptor;
      if (JsUtils.isGlobal(qualifiedName)) {
        nativeTypeDescriptor =
            TypeDescriptors.createNative(
                Arrays.asList(JsUtils.JS_GLOBAL),
                Arrays.asList(""),
                Collections.emptyList(),
                JsUtils.JS_GLOBAL,
                "");
      } else {
        List<String> nameComponents = Splitter.on('.').splitToList(qualifiedName);
        int size = nameComponents.size();
        // Fill in JS_GLOBAL as the namespace if the namespace is empty.
        List<String> namespaceComponents =
            size == 1 ? Arrays.asList(JsUtils.JS_GLOBAL) : nameComponents.subList(0, size - 1);
        nativeTypeDescriptor =
            TypeDescriptors.createNative(
                namespaceComponents,
                nameComponents.subList(size - 1, size),
                Collections.emptyList(),
                Joiner.on(".").join(namespaceComponents),
                nameComponents.get(size - 1));
      }
      // A.abs() -> Math.abs().
      MethodDescriptor newMethodDescriptor =
          MethodDescriptor.Builder.from(methodDescriptor)
              .setEnclosingClassTypeDescriptor(nativeTypeDescriptor)
              .build();
      Preconditions.checkArgument(methodCall.getQualifier() instanceof TypeReference);
      return MethodCall.createMethodCall(null, newMethodDescriptor, methodCall.getArguments());
    }
  }
}
