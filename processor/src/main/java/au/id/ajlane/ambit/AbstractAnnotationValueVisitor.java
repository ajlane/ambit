package au.id.ajlane.ambit;

import java.util.List;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.AnnotationValueVisitor;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.AbstractAnnotationValueVisitor8;

/**
 * A convenient implementation of {@link AnnotationValueVisitor}
 * <p>
 * Only compatible with Java 8 or later.
 *
 * @param <R>
 *     The type of value returned by the visitor.
 * @param <P>
 *     The type of the parameter given to the visitor.
 */
public class AbstractAnnotationValueVisitor<R, P> extends AbstractAnnotationValueVisitor8<R, P>
{
    @Override
    public R visitAnnotation(final AnnotationMirror a, final P p)
    {
        return visitOther(a, p);
    }

    @Override
    public R visitArray(final List<? extends AnnotationValue> vals, final P p)
    {
        return visitOther(vals, p);
    }

    @Override
    public R visitBoolean(final boolean b, final P p)
    {
        return visitOther(b, p);
    }

    @Override
    public R visitByte(final byte b, final P p)
    {
        return visitOther(b, p);
    }

    @Override
    public R visitChar(final char c, final P p)
    {
        return visitOther(c, p);
    }

    @Override
    public R visitDouble(final double d, final P p)
    {
        return visitOther(d, p);
    }

    @Override
    public R visitEnumConstant(final VariableElement c, final P p)
    {
        return visitOther(c, p);
    }

    @Override
    public R visitFloat(final float f, final P p)
    {
        return visitOther(f, p);
    }

    @Override
    public R visitInt(final int i, final P p)
    {
        return visitOther(i, p);
    }

    @Override
    public R visitLong(final long i, final P p)
    {
        return visitOther(i, p);
    }

    /**
     * Visits an otherwise unhandled value.
     *
     * @param v
     *     The value as an object.
     * @param p
     *     A parameter provided to the visitor.
     *
     * @return A return value.
     */
    protected R visitOther(final Object v, final P p)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public R visitShort(final short s, final P p)
    {
        return visitOther(s, p);
    }

    @Override
    public R visitString(final String s, final P p)
    {
        return visitOther(s, p);
    }

    @Override
    public R visitType(final TypeMirror t, final P p)
    {
        return visitOther(t, p);
    }

    @Override
    public R visitUnknown(final AnnotationValue av, final P p)
    {
        return visitOther(av, p);
    }
}
