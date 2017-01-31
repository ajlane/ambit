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
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ErrorType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.NullType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.TypeVisitor;
import javax.lang.model.type.UnionType;
import javax.lang.model.type.WildcardType;
import javax.tools.Diagnostic;

import com.sun.codemodel.ClassType;
import com.sun.codemodel.CodeWriter;
import com.sun.codemodel.JClassAlreadyExistsException;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JExpr;
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
        JCodeModel codeModel = new JCodeModel();

        final Set<? extends Element> moduleElements = roundEnv.getElementsAnnotatedWith(Module.class);
        for (Element moduleElement : moduleElements)
        {
            Iterable<? extends AnnotationMirror> moduleAnnotations = moduleElement.getAnnotationMirrors()
                .stream()
                .filter(m -> m.getAnnotationType()
                    .asElement()
                    .getSimpleName()
                    .toString()
                    .equals("Module"))
                .collect(Collectors.toList());
            for (AnnotationMirror moduleAnnotation : moduleAnnotations)
            {
                for (DeclaredType scopeClass : moduleAnnotation.getElementValues()
                    .entrySet()
                    .stream()
                    .filter(e -> e.getKey()
                        .getSimpleName()
                        .toString()
                        .equalsIgnoreCase("value"))
                    .flatMap(e -> Stream.of(e.getValue()))
                    .map(v -> (List<?>) v.getValue())
                    .flatMap(Collection::stream)
                    .map(o -> (AnnotationValue) o)
                    .map(a -> a.accept(new AbstractAnnotationValueVisitor<DeclaredType, Void>()
                    {
                        @Override
                        public DeclaredType visitType(TypeMirror t, Void p)
                        {
                            return (DeclaredType) t;
                        }
                    }, null))
                    .collect(Collectors.toList()))
                {
                    final String packageName = "au.id.ajlane.ambit.example";
                    final String simpleName = moduleElement.getSimpleName()
                        .toString()
                        .replaceAll("Module$", "") + "Application";
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
                        processingEnv.getMessager()
                            .printMessage(
                                Diagnostic.Kind.WARNING,
                                "Could not generate " + fullName + " because it already exists."
                            );
                        continue;
                    }

                    impl._implements(codeModel.ref(scopeClass.toString()));

                    for (Element element : scopeClass.asElement()
                        .getEnclosedElements())
                    {
                        switch (element.getKind())
                        {
                            case METHOD:
                                ExecutableElement methodElement = (ExecutableElement) element;
                                methodElement.getReturnType()
                                    .accept(new TypeVisitor<Void, Void>()
                                    {
                                        @Override
                                        public Void visit(final TypeMirror t, final Void aVoid)
                                        {
                                            return null;
                                        }

                                        @Override
                                        public Void visit(final TypeMirror t)
                                        {
                                            return null;
                                        }

                                        @Override
                                        public Void visitArray(final ArrayType t, final Void aVoid)
                                        {
                                            return null;
                                        }

                                        @Override
                                        public Void visitDeclared(final DeclaredType t, final Void aVoid)
                                        {
                                            impl.method(
                                                JMod.PUBLIC,
                                                codeModel.ref(
                                                    t.asElement()
                                                        .toString()),
                                                element.getSimpleName()
                                                    .toString()
                                            )
                                                .body()
                                                ._return(JExpr._null());
                                            return null;
                                        }

                                        @Override
                                        public Void visitError(final ErrorType t, final Void aVoid)
                                        {
                                            return null;
                                        }

                                        @Override
                                        public Void visitExecutable(final ExecutableType t, final Void aVoid)
                                        {
                                            return null;
                                        }

                                        @Override
                                        public Void visitIntersection(final IntersectionType t, final Void aVoid)
                                        {
                                            return null;
                                        }

                                        @Override
                                        public Void visitNoType(final NoType t, final Void aVoid)
                                        {
                                            return null;
                                        }

                                        @Override
                                        public Void visitNull(final NullType t, final Void aVoid)
                                        {
                                            return null;
                                        }

                                        @Override
                                        public Void visitPrimitive(final PrimitiveType t, final Void aVoid)
                                        {
                                            return null;
                                        }

                                        @Override
                                        public Void visitTypeVariable(final TypeVariable t, final Void aVoid)
                                        {
                                            return null;
                                        }

                                        @Override
                                        public Void visitUnion(final UnionType t, final Void aVoid)
                                        {
                                            return null;
                                        }

                                        @Override
                                        public Void visitUnknown(final TypeMirror t, final Void aVoid)
                                        {
                                            return null;
                                        }

                                        @Override
                                        public Void visitWildcard(final WildcardType t, final Void aVoid)
                                        {
                                            return null;
                                        }
                                    }, null);
                                break;
                            default:
                                // Ignore
                                break;
                        }
                    }
                }
            }
        }

        try
        {
            codeModel.build(new CodeWriter()
            {
                private Element[] originatingElements = moduleElements.toArray(new Element[moduleElements.size()]);

                @Override
                public void close() throws IOException
                {
                }

                @Override
                public OutputStream openBinary(final JPackage jPackage, final String s) throws IOException
                {
                    return processingEnv.getFiler()
                        .createSourceFile(jPackage.name() + "." + s.replaceAll(".java", ""), originatingElements)
                        .openOutputStream();
                }
            });

            return true;
        }
        catch (IOException ex)
        {
            final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            try (PrintWriter bufferWriter = new PrintWriter(buffer))
            {
                ex.printStackTrace(bufferWriter);
            }
            processingEnv.getMessager()
                .printMessage(
                    Diagnostic.Kind.ERROR,
                    "Could not write generated source files: " + ex.getMessage() + "\n" + buffer.toString()
                );
            return false;
        }
    }
}
