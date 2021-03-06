/*
 * Copyright (C) 2015 The Dagger Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dagger.internal.codegen;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.TypeSpec.anonymousClassBuilder;
import static dagger.internal.codegen.TypeNames.dependencyMethodProducerOf;
import static dagger.internal.codegen.TypeNames.listenableFutureOf;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.TypeName;
import dagger.internal.codegen.FrameworkFieldInitializer.FrameworkInstanceCreationExpression;

/**
 * A {@link dagger.producers.Producer} creation expression for a production method on a production
 * component's {@linkplain dagger.producers.ProductionComponent#dependencies()} dependency} that
 * returns a {@link com.google.common.util.concurrent.ListenableFuture}.
 */
// TODO(dpb): Resolve with DependencyMethodProviderCreationExpression.
final class DependencyMethodProducerCreationExpression
    implements FrameworkInstanceCreationExpression {
  private final ContributionBinding binding;
  private final ComponentImplementation componentImplementation;
  private final ComponentRequirementFields componentRequirementFields;
  private final BindingGraph graph;

  DependencyMethodProducerCreationExpression(
      ContributionBinding binding,
      ComponentImplementation componentImplementation,
      ComponentRequirementFields componentRequirementFields,
      BindingGraph graph) {
    this.binding = checkNotNull(binding);
    this.componentImplementation = checkNotNull(componentImplementation);
    this.componentRequirementFields = checkNotNull(componentRequirementFields);
    this.graph = checkNotNull(graph);
  }

  @Override
  public CodeBlock creationExpression() {
    ComponentRequirement dependency =
        graph
            .componentDescriptor()
            .dependenciesByDependencyMethod()
            .get(binding.bindingElement().get());
    FieldSpec dependencyField =
        FieldSpec.builder(
                ClassName.get(dependency.typeElement()), dependency.variableName(), PRIVATE, FINAL)
            .initializer(
                componentRequirementFields.getExpressionDuringInitialization(
                    dependency, componentImplementation.name()))
            .build();
    // TODO(b/70395982): Explore using a private static type instead of an anonymous class.
    TypeName keyType = TypeName.get(binding.key().type());
    return CodeBlock.of(
        "$L",
        anonymousClassBuilder("")
            .superclass(dependencyMethodProducerOf(keyType))
            .addField(dependencyField)
            .addMethod(
                methodBuilder("callDependencyMethod")
                    .addAnnotation(Override.class)
                    .addModifiers(PUBLIC)
                    .returns(listenableFutureOf(keyType))
                    .addStatement(
                        "return $N.$L()",
                        dependencyField,
                        binding.bindingElement().get().getSimpleName())
                    .build())
            .build());
  }
}
