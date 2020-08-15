package com.github.fburato.highwheelmodules.bytecodeparser;

import com.github.fburato.highwheelmodules.model.bytecode.AccessPoint;
import com.github.fburato.highwheelmodules.model.bytecode.AccessPointName;
import com.github.fburato.highwheelmodules.model.bytecode.AccessType;
import com.github.fburato.highwheelmodules.model.bytecode.ElementName;
import com.github.fburato.highwheelmodules.model.classpath.AccessVisitor;
import org.junit.jupiter.api.Test;

import java.util.function.Predicate;

import static org.mockito.Mockito.*;

public class FilteringDecoratorTest {

    interface ElementNamePredicate extends Predicate<ElementName> {
    }

    private final AccessVisitor child = mock(AccessVisitor.class);

    private final Predicate<ElementName> filter = mock(ElementNamePredicate.class);

    private final FilteringDecorator testee = new FilteringDecorator(child, filter);

    private final ElementName fooElement = ElementName.fromString("foo");
    private final ElementName barElement = ElementName.fromString("bar");

    private final AccessPoint foo = AccessPoint.create(this.fooElement, AccessPointName.create("foo", "()V"));

    private final AccessPoint bar = AccessPoint.create(this.barElement, AccessPointName.create("bar", "()V"));

    @Test
    public void shouldNotForwardCallWhenFilterDoesNotMatchSource() {
        when(this.filter.test(this.fooElement)).thenReturn(false);
        when(this.filter.test(this.barElement)).thenReturn(true);
        this.testee.apply(this.foo, this.bar, AccessType.COMPOSED);
        verify(this.child, never()).apply(this.foo, this.bar, AccessType.COMPOSED);
    }

    @Test
    public void shouldNotForwardCallWhenFilterDoesNotMatchDest() {
        when(this.filter.test(this.fooElement)).thenReturn(true);
        when(this.filter.test(this.barElement)).thenReturn(false);
        this.testee.apply(this.foo, this.bar, AccessType.COMPOSED);
        verify(this.child, never()).apply(this.foo, this.bar, AccessType.COMPOSED);
    }

    @Test
    public void shouldForwardCallWhenFilterMatchesSourceAndDest() {
        when(this.filter.test(this.fooElement)).thenReturn(true);
        when(this.filter.test(this.barElement)).thenReturn(true);
        this.testee.apply(this.foo, this.bar, AccessType.COMPOSED);
        verify(this.child).apply(this.foo, this.bar, AccessType.COMPOSED);
    }

    @Test
    public void shouldNotForwardNewNodeWhenFilterDoesNotMatch() {
        when(this.filter.test(this.fooElement)).thenReturn(false);
        this.testee.newNode(this.fooElement);
        verify(this.child, never()).newNode(this.fooElement);
    }

    @Test
    public void shouldForwardNewNodeWhenFilterMatches() {
        when(this.filter.test(this.fooElement)).thenReturn(true);
        this.testee.newNode(this.fooElement);
        verify(this.child).newNode(this.fooElement);
    }
}
