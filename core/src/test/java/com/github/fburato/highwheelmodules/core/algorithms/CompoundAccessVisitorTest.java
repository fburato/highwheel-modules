package com.github.fburato.highwheelmodules.core.algorithms;

import org.junit.Test;
import org.mockito.InOrder;
import org.pitest.highwheel.classpath.AccessVisitor;
import org.pitest.highwheel.model.AccessPoint;
import org.pitest.highwheel.model.AccessType;
import org.pitest.highwheel.model.ElementName;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

public class CompoundAccessVisitorTest {

  private AccessVisitor av1 = mock(AccessVisitor.class);
  private AccessVisitor av2 = mock(AccessVisitor.class);
  private CompoundAccessVisitor testee = new CompoundAccessVisitor(av1, av2);
  private InOrder inOrder = inOrder(av1, av2);
  private AccessPoint ap = AccessPoint.create(ElementName.fromString("a"));
  private AccessType at = AccessType.USES;
  private ElementName en = ElementName.fromString("b");

  @Test
  public void applyShouldCallComposeApplyInOrder() {
    testee.apply(ap, ap, at);

    inOrder.verify(av1).apply(ap, ap, at);
    inOrder.verify(av2).apply(ap, ap, at);
  }

  @Test
  public void newAccessPointShouldCallComposeApplyInOrder() {
    testee.newAccessPoint(ap);

    inOrder.verify(av1).newAccessPoint(ap);
    inOrder.verify(av2).newAccessPoint(ap);
  }

  @Test
  public void newNodePointShouldCallComposeApplyInOrder() {
    testee.newNode(en);

    inOrder.verify(av1).newNode(en);
    inOrder.verify(av2).newNode(en);
  }

  @Test
  public void newEntrytPointShouldCallComposeApplyInOrder() {
    testee.newEntryPoint(en);

    inOrder.verify(av1).newEntryPoint(en);
    inOrder.verify(av2).newEntryPoint(en);
  }
}
