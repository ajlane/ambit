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
import javax.lang.model.element.PackageElement;
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
                    final DeclaredType moduleType = (DeclaredType) moduleElement.asType();
                    final JCodeModel codeModel = new JCodeModel();

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
                                    final DeclaredType interfaceType =
                                        annotationValue.accept(new AbstractAnnotationValueVisitor<DeclaredType, Void>()
                                        {
                                            @Override
                                            public DeclaredType visitType(TypeMirror t, Void p)
                                            {
                                                return (DeclaredType) t;
                                            }
                                        }, null);
                                    final Element interfaceElement = interfaceType.asElement();
                                    final PackageElement interfacePackage = processingEnv.getElementUtils()
                                        .getPackageOf(interfaceElement);
                                    final String implSimpleName = moduleElement.getSimpleName()
                                        .toString()
                                        .replaceAll("Module$", "")
                                        .replaceAll("(?=.)" + interfaceElement.getSimpleName() + "$", "")
                                        + interfaceType.asElement()
                                        .getSimpleName()
                                        .toString();
                                    final String implFullName = interfacePackage + "." + implSimpleName;

                                    final JDefinedClass impl;
                                    try
                                    {
                                        impl = codeModel._class(
                                            JMod.PUBLIC,
                                            implFullName,
                                            ClassType.CLASS
                                        );
                                    }
                                    catch (JClassAlreadyExistsException e)
                                    {
                                        warn(
                                            "Could not generate " + implFullName + " because it already exists.",
                                            moduleElement,
                                            moduleAnnotation,
                                            annotationValue
                                        );
                                        return;
                                    }

                                    impl._implements(codeModel.ref(interfaceType.toString()));
                                    impl._implements(codeModel.ref(AutoCloseable.class));

                                    final JFieldVar moduleField = impl.field(
                                        JMod.PRIVATE | JMod.FINAL,
                                        codeModel.ref(moduleElement.toString()),
                                        "module"
                                    );

                                    final JMethod implCloseMethod = impl.method(JMod.PUBLIC, codeModel.VOID, "close");
                                    final List<? extends ExecutableElement> moduleCloseMethodElements =
                                        moduleElement.getEnclosedElements()
                                            .stream()
                                            .filter(e -> e.getKind() == ElementKind.METHOD)
                                            .filter(e -> e.getSimpleName()
                                                .toString()
                                                .equals("close"))
                                            .map(e -> (ExecutableElement) e)
                                            .collect(Collectors.toList());
                                    if (moduleCloseMethodElements.isEmpty())
                                    {
                                        warn("There are several close methods on " + moduleElement + ".");
                                        // TODO: Think about crashing instead of just warning
                                    }
                                    if (moduleCloseMethodElements.size() > 0)
                                    {
                                        final ExecutableElement moduleCloseMethodElement =
                                            moduleCloseMethodElements.get(0);
                                        moduleCloseMethodElement.getThrownTypes()
                                            .forEach(moduleCloseMethodExceptionType ->
                                                implCloseMethod._throws(codeModel.ref(moduleCloseMethodExceptionType
                                                    .toString())));
                                        final JInvocation implModuleCloseMethodCall = JExpr._this()
                                            .ref(moduleField)
                                            .invoke(moduleCloseMethodElement.getSimpleName()
                                                .toString());
                                        for (int i = 0;
                                             i < moduleCloseMethodElement.getParameters()
                                                 .size();
                                             i++)
                                        {
                                            implModuleCloseMethodCall.arg(JExpr._this()
                                                .ref(moduleCloseMethodElement.getParameters()
                                                    .get(i)
                                                    .getSimpleName()
                                                    .toString()));
                                        }
                                        implCloseMethod.body()
                                            .add(implModuleCloseMethodCall);
                                    }

                                    final JMethod implConstructor = impl.constructor(JMod.PUBLIC);
                                    final JInvocation invokeModuleConstructor =
                                        JExpr._new(codeModel.ref(moduleElement.toString()));
                                    implConstructor.body()
                                        .assign(
                                            JExpr._this()
                                                .ref(moduleField),
                                            invokeModuleConstructor
                                        );
                                    final List<ExecutableElement> moduleConstructorElements =
                                        moduleElement.getEnclosedElements()
                                            .stream()
                                            .filter(e -> e.getKind() == ElementKind.CONSTRUCTOR)
                                            .map(c -> (ExecutableElement) c)
                                            .collect(Collectors.toList());
                                    if (moduleConstructorElements.size() > 1)
                                    {
                                        warn("There are several constructors on " + moduleElement + ".");
                                        // TODO: Think about crashing instead of just warning.
                                    }
                                    if (moduleConstructorElements.size() > 0)
                                    {
                                        final ExecutableElement moduleConstructorElement =
                                            moduleConstructorElements.get(0);
                                        final ExecutableType moduleConstructorType =
                                            (ExecutableType) moduleConstructorElement.asType();
                                        for (int i = 0;
                                             i < moduleConstructorElement.getParameters()
                                                 .size();
                                             i++)
                                        {
                                            invokeModuleConstructor.arg(implConstructor.param(
                                                JMod.FINAL,
                                                codeModel.ref(moduleConstructorType.getParameterTypes()
                                                    .get(i)
                                                    .toString()),
                                                moduleConstructorElement.getParameters()
                                                    .get(i)
                                                    .getSimpleName()
                                                    .toString()
                                            ));
                                        }
                                    }

                                    for (Element interfaceMemberElement : interfaceType.asElement()
                                        .getEnclosedElements())
                                    {
                                        switch (interfaceMemberElement.getKind())
                                        {
                                            case METHOD:
                                                ExecutableElement interfaceMethodElement =
                                                    (ExecutableElement) interfaceMemberElement;
                                                Set<Modifier> interfaceMethodModifiers =
                                                    interfaceMethodElement.getModifiers();
                                                boolean isStatic = interfaceMethodModifiers.contains(Modifier.STATIC);
                                                boolean hasDefault =
                                                    interfaceMethodModifiers.contains(Modifier.DEFAULT);
                                                if (isStatic)
                                                {
                                                    continue;
                                                }
                                                interfaceMethodElement.getReturnType()
                                                    .accept(new AbstractTypeVisitor<JMethod, JDefinedClass>()
                                                    {
                                                        @Override
                                                        public JMethod visitDeclared(
                                                            final DeclaredType interfaceMethodReturnType,
                                                            final JDefinedClass impl
                                                        )
                                                        {
                                                            final JFieldVar implGetterField = impl.field(
                                                                JMod.PRIVATE | JMod.FINAL,
                                                                codeModel.ref(
                                                                    interfaceMethodReturnType.asElement()
                                                                        .toString()),
                                                                interfaceMemberElement.getSimpleName()
                                                                    .toString()
                                                            );

                                                            implConstructor.body()
                                                                .assign(
                                                                    JExpr._this()
                                                                        .ref(implGetterField),
                                                                    JExpr._this()
                                                                        .ref(moduleField)
                                                                        .invoke(interfaceMemberElement.getSimpleName()
                                                                            .toString())
                                                                );

                                                            final JMethod implGetterMethod = impl.method(
                                                                JMod.PUBLIC,
                                                                codeModel.ref(
                                                                    interfaceMethodReturnType.asElement()
                                                                        .toString()),
                                                                interfaceMemberElement.getSimpleName()
                                                                    .toString()
                                                            );
                                                            implGetterMethod.body()
                                                                ._return(JExpr._this()
                                                                    .ref(implGetterField));

                                                            return implGetterMethod;
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
                                                                    implFullName + " cannot meaningfully implement "
                                                                        + interfaceMethodElement.getSimpleName()
                                                                        + " because it does not return a value.",
                                                                    interfaceMethodElement
                                                                );
                                                                final JMethod implEmptyMethod = impl.method(
                                                                    JMod.PUBLIC,
                                                                    codeModel.VOID,
                                                                    interfaceMemberElement.getSimpleName()
                                                                        .toString()
                                                                );
                                                                return implEmptyMethod;
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
                            public OutputStream openBinary(final JPackage jPackage, final String fileName)
                                throws IOException
                            {
                                return processingEnv.getFiler()
                                    .createSourceFile(
                                        jPackage.name() + "." + fileName.replaceAll(".java", ""),
                                        moduleElement
                                    )
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
