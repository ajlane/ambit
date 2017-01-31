package au.id.ajlane.ambit;

import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ErrorType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.NullType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.UnionType;
import javax.lang.model.type.WildcardType;
import javax.lang.model.util.AbstractTypeVisitor8;

/**
 * A convenient implementation of {@link javax.lang.model.type.TypeVisitor}
 * <p>
 * Only compatible with Java 8 or later.
 *
 * @param <R>
 *     The type of value returned by the visitor.
 * @param <P>
 *     The type of the parameter given to the visitor.
 */
public abstract class AbstractTypeVisitor<R, P> extends AbstractTypeVisitor8<R, P>
{
    @Override
    public R visitArray(final ArrayType t, final P p)
    {
        return visitOther(t, p);
    }

    @Override
    public R visitDeclared(final DeclaredType t, final P p)
    {
        return visitOther(t, p);
    }

    @Override
    public R visitError(final ErrorType t, final P p)
    {
        return visitOther(t, p);
    }

    @Override
    public R visitExecutable(final ExecutableType t, final P p)
    {
        return visitOther(t, p);
    }

    @Override
    public R visitIntersection(final IntersectionType t, final P p)
    {
        return visitOther(t, p);
    }

    @Override
    public R visitNoType(final NoType t, final P p)
    {
        return visitOther(t, p);
    }

    @Override
    public R visitNull(final NullType t, final P p)
    {
        return visitOther(t, p);
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
    public R visitPrimitive(final PrimitiveType t, final P p)
    {
        return visitOther(t, p);
    }

    @Override
    public R visitTypeVariable(final TypeVariable t, final P p)
    {
        return visitOther(t, p);
    }

    @Override
    public R visitUnion(final UnionType t, final P p)
    {
        return visitOther(t, p);
    }

    @Override
    public R visitWildcard(final WildcardType t, final P p)
    {
        return visitOther(t, p);
    }
}
