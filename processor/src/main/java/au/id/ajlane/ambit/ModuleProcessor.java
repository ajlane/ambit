package au.id.ajlane.ambit;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;

import com.sun.codemodel.ClassType;
import com.sun.codemodel.CodeWriter;
import com.sun.codemodel.JClassAlreadyExistsException;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JInvocation;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JPackage;


@SupportedOptions({})
@SupportedAnnotationTypes("au.id.ajlane.ambit.Module")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class ModuleProcessor extends AbstractProcessor
{
    @Override
    public boolean process(
        final Set<? extends TypeElement> annotations,
        final RoundEnvironment roundEnv
    )
    {
        try
        {
            return roundEnv.getElementsAnnotatedWith(Module.class)
                .parallelStream()
                .map(moduleElement ->
                {
                    JCodeModel codeModel = new JCodeModel();

                    moduleElement.getAnnotationMirrors()
                        .stream()
                        .filter(m -> m.getAnnotationType()
                            .asElement()
                            .toString()
                            .equals(Module.class.getName()))
                        .forEach(moduleAnnotation ->
                            moduleAnnotation.getElementValues()
                                .entrySet()
                                .stream()
                                .filter(e -> e.getKey()
                                    .getSimpleName()
                                    .toString()
                                    .equals("value"))
                                .flatMap(e -> Stream.of(e.getValue()))
                                .map(v -> (List<?>) v.getValue())
                                .flatMap(Collection::stream)
                                .map(o -> (AnnotationValue) o)
                                .forEach(annotationValue ->
                                {
                                    final DeclaredType scopeClass =
                                        annotationValue.accept(new AbstractAnnotationValueVisitor<DeclaredType, Void>()
                                        {
                                            @Override
                                            public DeclaredType visitType(TypeMirror t, Void p)
                                            {
                                                return (DeclaredType) t;
                                            }
                                        }, null);
                                    final String packageName = scopeClass.asElement()
                                        .toString()
                                        .replaceAll("\\." + scopeClass.asElement()
                                            .getSimpleName()
                                            .toString() + "$", "");
                                    final String simpleName = moduleElement.getSimpleName()
                                        .toString()
                                        .replaceAll("Module$", "") + scopeClass.asElement()
                                        .getSimpleName()
                                        .toString();
                                    final String fullName = packageName + "." + simpleName;

                                    final JDefinedClass impl;
                                    try
                                    {
                                        impl = codeModel._class(
                                            JMod.PUBLIC,
                                            fullName,
                                            ClassType.CLASS
                                        );
                                    }
                                    catch (JClassAlreadyExistsException e)
                                    {
                                        warn(
                                            "Could not generate " + fullName + " because it already exists.",
                                            moduleElement,
                                            moduleAnnotation,
                                            annotationValue
                                        );
                                        return;
                                    }

                                    impl._implements(codeModel.ref(scopeClass.toString()));

                                    final JFieldVar moduleField = impl.field(
                                        JMod.PRIVATE | JMod.FINAL,
                                        codeModel.ref(moduleElement.toString()),
                                        "module"
                                    );

                                    final JMethod implConstructor = impl.constructor(JMod.PUBLIC);
                                    final JInvocation invokeModuleConstructor =
                                        JExpr._new(codeModel.ref(moduleElement.toString()));
                                    implConstructor.body()
                                        .assign(
                                            JExpr._this()
                                                .ref(moduleField),
                                            invokeModuleConstructor
                                        );
                                    final List<ExecutableElement> constructors = moduleElement.getEnclosedElements()
                                        .stream()
                                        .filter(e -> e.getKind() == ElementKind.CONSTRUCTOR)
                                        .map(c -> (ExecutableElement) c)
                                        .collect(Collectors.toList());
                                    if (constructors.size() > 1)
                                    {
                                        warn("There are several constructors on " + moduleElement + ".");
                                        // TODO: Think about crashing instead of just warning.
                                    }
                                    if (constructors.size() > 0)
                                    {
                                        ExecutableElement constructor = constructors.get(0);
                                        ExecutableType constructorType = (ExecutableType) constructor.asType();
                                        for (int i = 0;
                                             i < constructor.getParameters()
                                                 .size();
                                             i++)
                                        {
                                            invokeModuleConstructor.arg(implConstructor.param(
                                                JMod.FINAL,
                                                codeModel.ref(constructorType.getParameterTypes()
                                                    .get(i)
                                                    .toString()),
                                                constructor.getParameters()
                                                    .get(i)
                                                    .getSimpleName()
                                                    .toString()
                                            ));
                                        }
                                    }

                                    for (Element element : scopeClass.asElement()
                                        .getEnclosedElements())
                                    {
                                        switch (element.getKind())
                                        {
                                            case METHOD:
                                                ExecutableElement methodElement = (ExecutableElement) element;
                                                Set<Modifier> methodModifiers = methodElement.getModifiers();
                                                boolean isStatic = methodModifiers.contains(Modifier.STATIC);
                                                boolean hasDefault = methodModifiers.contains(Modifier.DEFAULT);
                                                if (isStatic)
                                                {
                                                    continue;
                                                }
                                                methodElement.getReturnType()
                                                    .accept(new AbstractTypeVisitor<JMethod, JDefinedClass>()
                                                    {
                                                        @Override
                                                        public JMethod visitDeclared(
                                                            final DeclaredType t,
                                                            final JDefinedClass impl
                                                        )
                                                        {
                                                            final JFieldVar field = impl.field(
                                                                JMod.PRIVATE | JMod.FINAL,
                                                                codeModel.ref(
                                                                    t.asElement()
                                                                        .toString()),
                                                                element.getSimpleName()
                                                                    .toString()
                                                            );

                                                            implConstructor.body()
                                                                .assign(
                                                                    JExpr._this()
                                                                        .ref(field),
                                                                    JExpr._this()
                                                                        .ref(moduleField)
                                                                        .invoke(element.getSimpleName()
                                                                            .toString())
                                                                );

                                                            final JMethod method = impl.method(
                                                                JMod.PUBLIC,
                                                                codeModel.ref(
                                                                    t.asElement()
                                                                        .toString()),
                                                                element.getSimpleName()
                                                                    .toString()
                                                            );
                                                            method.body()
                                                                ._return(JExpr._this()
                                                                    .ref(field));
                                                            return method;
                                                        }

                                                        @Override
                                                        public JMethod visitNoType(
                                                            final NoType t,
                                                            final JDefinedClass impl
                                                        )
                                                        {
                                                            if (!hasDefault)
                                                            {
                                                                warn(
                                                                    fullName + " cannot meaningfully implement "
                                                                        + methodElement.getSimpleName()
                                                                        + " because it does not return a value.",
                                                                    methodElement
                                                                );
                                                                return impl.method(
                                                                    JMod.PUBLIC,
                                                                    codeModel.VOID,
                                                                    element.getSimpleName()
                                                                        .toString()
                                                                );
                                                            }
                                                            return null;
                                                        }
                                                    }, impl);
                                                break;
                                            default:
                                                // Ignore
                                                break;
                                        }
                                    }
                                }));
                    try
                    {
                        codeModel.build(new CodeWriter()
                        {
                            @Override
                            public void close() throws IOException
                            {
                            }

                            @Override
                            public OutputStream openBinary(final JPackage jPackage, final String s) throws IOException
                            {
                                return processingEnv.getFiler()
                                    .createSourceFile(jPackage.name() + "." + s.replaceAll(".java", ""), moduleElement)
                                    .openOutputStream();
                            }
                        });
                        return true;
                    }
                    catch (IOException ex)
                    {
                        error("Could not write generated source files.", moduleElement, ex);
                        return false;
                    }
                })
                .allMatch(t -> t);
        }
        catch (Throwable ex)
        {
            error("The module processor has failed.", ex);
            return false;
        }
    }

    private void error(String message)
    {
        log(Diagnostic.Kind.ERROR, message);
    }

    private void error(String message, Throwable cause)
    {
        log(Diagnostic.Kind.ERROR, message, cause);
    }

    private void error(String message, Element element)
    {
        log(Diagnostic.Kind.ERROR, message, element);
    }

    private void error(String message, Element element, Throwable cause)
    {
        log(Diagnostic.Kind.ERROR, message, element, cause);
    }

    private void error(String message, Element element, AnnotationMirror annotation)
    {
        log(Diagnostic.Kind.ERROR, message, element, annotation);
    }

    private void error(String message, Element element, AnnotationMirror annotation, Throwable cause)
    {
        log(Diagnostic.Kind.ERROR, message, element, annotation, cause);
    }

    private void error(String message, Element element, AnnotationMirror annotation, AnnotationValue annotationValue)
    {
        log(Diagnostic.Kind.ERROR, message, element, annotation, annotationValue);
    }

    private void error(
        String message,
        Element element,
        AnnotationMirror annotation,
        AnnotationValue annotationValue,
        Throwable cause
    )
    {
        log(Diagnostic.Kind.ERROR, message, element, annotation, annotationValue, cause);
    }

    private void log(Diagnostic.Kind kind, String message)
    {
        log(kind, message, (Element) null);
    }

    private void log(Diagnostic.Kind kind, String message, Throwable cause)
    {
        log(kind, message, null, cause);
    }

    private void log(Diagnostic.Kind kind, String message, Element element)
    {
        log(kind, message, element, (AnnotationMirror) null);
    }

    private void log(
        Diagnostic.Kind kind,
        String message,
        Element element,
        Throwable cause
    )
    {
        log(kind, message, element, null, cause);
    }

    private void log(Diagnostic.Kind kind, String message, Element element, AnnotationMirror annotation)
    {
        log(kind, message, element, annotation, (AnnotationValue) null);
    }

    private void log(
        Diagnostic.Kind kind,
        String message,
        Element element,
        AnnotationMirror annotation,
        Throwable cause
    )
    {
        log(kind, message, element, annotation, null, cause);
    }

    private void log(
        Diagnostic.Kind kind,
        String message,
        Element element,
        AnnotationMirror annotation,
        AnnotationValue annotationValue
    )
    {
        log(kind, message, element, annotation, annotationValue, null);
    }

    private void log(
        Diagnostic.Kind kind,
        String message,
        Element element,
        AnnotationMirror annotation,
        AnnotationValue annotationValue,
        Throwable cause
    )
    {
        if (cause != null)
        {
            final ByteArrayOutputStream stackTrace = new ByteArrayOutputStream();
            try (PrintWriter stackTraceWriter = new PrintWriter(stackTrace))
            {
                cause.printStackTrace(stackTraceWriter);
            }
            message = message + " " + cause.getClass()
                .getName() + ": " + cause.getMessage() + "\n" + stackTrace.toString();
        }
        processingEnv.getMessager()
            .printMessage(
                kind,
                message,
                element,
                annotation,
                annotationValue
            );
    }

    private void warn(String message)
    {
        log(Diagnostic.Kind.WARNING, message);
    }

    private void warn(String message, Throwable cause)
    {
        log(Diagnostic.Kind.WARNING, message, cause);
    }

    private void warn(String message, Element element)
    {
        log(Diagnostic.Kind.WARNING, message, element);
    }

    private void warn(String message, Element element, Throwable cause)
    {
        log(Diagnostic.Kind.WARNING, message, element, cause);
    }

    private void warn(String message, Element element, AnnotationMirror annotation)
    {
        log(Diagnostic.Kind.WARNING, message, element, annotation);
    }

    private void warn(String message, Element element, AnnotationMirror annotation, Throwable cause)
    {
        log(Diagnostic.Kind.WARNING, message, element, annotation, cause);
    }

    private void warn(String message, Element element, AnnotationMirror annotation, AnnotationValue annotationValue)
    {
        log(Diagnostic.Kind.WARNING, message, element, annotation, annotationValue);
    }

    private void warn(
        String message,
        Element element,
        AnnotationMirror annotation,
        AnnotationValue annotationValue,
        Throwable cause
    )
    {
        log(Diagnostic.Kind.WARNING, message, element, annotation, annotationValue, cause);
    }
}
