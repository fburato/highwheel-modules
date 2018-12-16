package com.github.fburato.highwheelmodules.model.bytecodeparser;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import com.github.fburato.highwheelmodules.model.classpath.AccessVisitor;
import com.github.fburato.highwheelmodules.model.bytecode.AccessPoint;
import com.github.fburato.highwheelmodules.model.bytecode.AccessPointName;
import com.github.fburato.highwheelmodules.model.bytecode.AccessType;
import com.github.fburato.highwheelmodules.model.bytecode.ElementName;

import java.util.function.Predicate;

public class FilteringDecoratorTest {

  private FilteringDecorator testee;

  @Mock
  private AccessVisitor      child;

  @Mock
  private Predicate<ElementName> filter;

  private final ElementName  fooElement = ElementName.fromString("foo");
  private final ElementName  barElement = ElementName.fromString("bar");

  private final AccessPoint  foo        =  AccessPoint.create(this.fooElement,
      AccessPointName.create("foo", "()V"));

  private final AccessPoint  bar        =  AccessPoint.create(this.barElement,
      AccessPointName.create("bar", "()V"));

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    this.testee = new FilteringDecorator(this.child, this.filter);
  }

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
