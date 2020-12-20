package com.github.fburato.highwheelmodules.bytecodeparser;

import com.example.*;
import com.example.annotated.*;
import com.example.classliterals.HasFieldOfTypeClassFoo;
import com.example.classliterals.MethodAccessFooClassLiteral;
import com.example.classliterals.StoresFooArrayClassLiteralAsField;
import com.example.classliterals.StoresFooClassLiteralAsField;
import com.example.generics.BoundedByFoo;
import com.example.generics.HasCollectionOfFooParameter;
import com.example.generics.ImplementsGenericisedInterface;
import com.example.generics.ReturnsCollectionOfFoo;
import com.example.innerclasses.CallsMethodFromFooWithinInnerClass;
import com.github.fburato.highwheelmodules.bytecodeparser.classpath.SpecificClassPathRoot;
import com.github.fburato.highwheelmodules.model.bytecode.AccessPoint;
import com.github.fburato.highwheelmodules.model.bytecode.AccessPointName;
import com.github.fburato.highwheelmodules.model.bytecode.AccessType;
import com.github.fburato.highwheelmodules.model.bytecode.ElementName;
import com.github.fburato.highwheelmodules.model.classpath.AccessVisitor;
import com.github.fburato.highwheelmodules.model.classpath.ClassParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.objectweb.asm.Type;

import java.util.function.Predicate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class ClassPathParserSystemTest {

    private ClassParser testee;

    @Mock
    private AccessVisitor v;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void shouldDetectAnInheritanceDepedencyWhenOneClassExtendsAnother() {
        parseClassPath(ExtendsFoo.class, Foo.class);
        verify(this.v).apply(accessAType(ExtendsFoo.class), accessAType(Foo.class), AccessType.INHERITANCE);
    }

    @Test
    public void shouldDetectAnImplementsDepedencyWhenClassImplementsInterface() {
        parseClassPath(ImplementsAnInterface.class, AnInterface.class);
        verify(this.v).apply(accessAType(ImplementsAnInterface.class), accessAType(AnInterface.class),
                AccessType.IMPLEMENTS);
    }

    @Test
    public void shouldDetectACompositionDependencyWhenClassIncludesAnother() {
        parseClassPath(HasFooAsMember.class, Foo.class);
        verify(this.v).apply(accessAType(HasFooAsMember.class), accessAType(Foo.class), AccessType.COMPOSED);
    }

    @Test
    public void shouldDetectACompositionDependencyWhenClassIncludesArrayField() {
        parseClassPath(HasArrayOfFooAsMember.class, Foo.class);
        verify(this.v).apply(accessAType(HasArrayOfFooAsMember.class), accessAType(Foo.class), AccessType.COMPOSED);
    }

    @Test
    public void shouldDetectSignatureDependencyWhenMethodReturnsAType() {
        parseClassPath(ReturnsAFoo.class, Foo.class);
        verify(this.v).apply(access(ReturnsAFoo.class, method("foo", Foo.class)), accessAType(Foo.class),
                AccessType.SIGNATURE);
    }

    @Test
    public void shouldDetectACompositionDependencyWhenClassReturnsAnArray() {
        parseClassPath(ReturnsArrayOfFoo.class, Foo.class);
        verify(this.v).apply(access(ReturnsArrayOfFoo.class, method("foo", Foo[].class)), accessAType(Foo.class),
                AccessType.SIGNATURE);
    }

    @Test
    public void shouldDetectSignatureDependencyWhenMethodHasParameterOfType() {
        parseClassPath(HasFooAsParameter.class, Foo.class);
        verify(this.v).apply(access(HasFooAsParameter.class, methodWithParameter("foo", Foo.class)),
                accessAType(Foo.class), AccessType.SIGNATURE);
    }

    @Test
    public void shouldDetectSignatureDependencyWhenMethodHasArrayParameter() {
        parseClassPath(HasFooArrayAsParameter.class, Foo.class);
        verify(this.v).apply(access(HasFooArrayAsParameter.class, methodWithParameter("foo", Foo[].class)),
                accessAType(Foo.class), AccessType.SIGNATURE);
    }

    @Test
    public void shouldDetectASignatureDependencyWhenDeclaresAnException() {
        parseClassPath(DeclaresAnException.class, AnException.class);
        verify(this.v).apply(access(DeclaresAnException.class, method("foo", "()V")), accessAType(AnException.class),
                AccessType.SIGNATURE);
    }

    @Test
    public void shouldDetectUsesDependencyWhenConstructsAType() {
        parseClassPath(ConstructsAFoo.class, Foo.class);
        verify(this.v).apply(access(ConstructsAFoo.class, method("foo", Object.class)),
                access(Foo.class, method("<init>", "()V")), AccessType.USES);
    }

    @Test
    public void shouldDetectUsesDependencyWhenCallsMethodOnType() {
        parseClassPath(CallsFooMethod.class, Foo.class);
        verify(this.v).apply(access(CallsFooMethod.class, method("foo", Object.class)),
                access(Foo.class, method("aMethod", Object.class)), AccessType.USES);
    }

    @Test
    public void shouldDetectWhenAnnotatedAtClassLevel() {
        parseClassPath(AnnotatedAtClassLevel.class, AnAnnotation.class);
        verify(this.v).apply(accessAType(AnnotatedAtClassLevel.class), accessAType(AnAnnotation.class),
                AccessType.ANNOTATED);
    }

    @Test
    public void shouldDetectWhenAnnotatedAtMethodLevel() {
        parseClassPath(AnnotatedAtMethodLevel.class, AnAnnotation.class);
        verify(this.v).apply(access(AnnotatedAtMethodLevel.class, method("foo", "()V")),
                accessAType(AnAnnotation.class), AccessType.ANNOTATED);
    }

    @Test
    public void shouldDetectWhenAnnotatedAtParameterLevel() {
        parseClassPath(AnnotatedAtParameterLevel.class, AnAnnotation.class);
        verify(this.v).apply(access(AnnotatedAtParameterLevel.class, method("foo", "(I)V")),
                accessAType(AnAnnotation.class), AccessType.ANNOTATED);
    }

    @Test
    public void shouldDetectWhenAnnotatedAtFieldLevel() {
        parseClassPath(AnnotatedAtFieldLevel.class, AnAnnotation.class);
        verify(this.v).apply(accessAType(AnnotatedAtFieldLevel.class), accessAType(AnAnnotation.class),
                AccessType.ANNOTATED);
    }

    @Test
    public void willNotDetectWhenAnnotatedAtVariableLevel() {
        parseClassPath(AnnotatedAtVariableLevel.class, AnAnnotation.class);
        verify(this.v, never()).apply(accessAType(AnnotatedAtVariableLevel.class), accessAType(AnAnnotation.class),
                AccessType.ANNOTATED);
    }

    @Test
    public void shouldDetectAUsesRelationshipForParentClassMethodWhenNestedClassCallsMethod() {
        parseClassPath(CallsMethodFromFooWithinInnerClass.class, Foo.class);
        verify(this.v).apply(access(CallsMethodFromFooWithinInnerClass.class, method("foo", "()V")),
                access(Foo.class, method("aMethod", Object.class)), AccessType.USES);
    }

    @Test
    public void shouldDetectAUsesRealtionshipWhenWritesToClassField() {
        parseClassPath(UsesFieldOnFoo.class, Foo.class);
        verify(this.v).apply(access(UsesFieldOnFoo.class, method("foo", "()V")),
                access(Foo.class, method("aField", "I")), AccessType.USES);
    }

    @Test
    public void shouldDetectAUsesRealtionshipWhenStoresClassLiteralAsField() {
        parseClassPath(StoresFooClassLiteralAsField.class, Foo.class);
        verify(this.v).apply(access(StoresFooClassLiteralAsField.class, method("<init>", "()V")),
                accessAType(Foo.class), AccessType.USES);
    }

    @Test
    public void shouldDetectAUsesRealtionshipWhenStoresClassArrayLiteralAsField() {
        parseClassPath(StoresFooArrayClassLiteralAsField.class, Foo.class);
        verify(this.v).apply(access(StoresFooArrayClassLiteralAsField.class, method("<init>", "()V")),
                accessAType(Foo.class), AccessType.USES);
    }

    @Test
    public void shouldDetectAUsesRelationshipWhenUsesFooClassLiteralInMethod() {
        parseClassPath(MethodAccessFooClassLiteral.class, Foo.class);
        verify(this.v).apply(access(MethodAccessFooClassLiteral.class, method("foo", Class.class)),
                accessAType(Foo.class), AccessType.USES);
    }

    @Test
    public void shouldDetectCompositionRelationshipWhenDeclaresFieldOfClassFoo() {
        parseClassPath(HasFieldOfTypeClassFoo.class, Foo.class);
        verify(this.v).apply(accessAType(HasFieldOfTypeClassFoo.class), accessAType(Foo.class), AccessType.COMPOSED);
    }

    @Test
    public void shouldDetectSignatureRelationshipWhenImplementsInterfaceParameterisedByFoo() {
        parseClassPath(ImplementsGenericisedInterface.class, Foo.class);
        verify(this.v).apply(accessAType(ImplementsGenericisedInterface.class), accessAType(Foo.class),
                AccessType.SIGNATURE);

    }

    @Test
    public void shouldDetectSignatureRelationshipWhenReturnsCollectionOfFoo() {
        parseClassPath(ReturnsCollectionOfFoo.class, Foo.class);
        verify(this.v).apply(accessAType(ReturnsCollectionOfFoo.class), accessAType(Foo.class), AccessType.SIGNATURE);

    }

    @Test
    public void shouldDetectSignatureRelationshipWhenHasCollectionOfFooParameter() {
        parseClassPath(HasCollectionOfFooParameter.class, Foo.class);
        verify(this.v).apply(accessAType(HasCollectionOfFooParameter.class), accessAType(Foo.class),
                AccessType.SIGNATURE);

    }

    @Test
    public void shouldDetectSignatureRelationshipWhenBoundedByFoo() {
        parseClassPath(BoundedByFoo.class, Foo.class);
        verify(this.v).apply(accessAType(BoundedByFoo.class), accessAType(Foo.class), AccessType.SIGNATURE);

    }

    @Test
    public void shouldDetectUnConnectedClasses() {
        parseClassPath(Unconnected.class);
        verify(this.v).newNode(ElementName.fromClass(Unconnected.class));
    }

    @Test
    public void shouldDetectUnConnectedMethods() {
        parseClassPath(Foo.class);
        verify(this.v).newAccessPoint(access(Foo.class, method("aMethod", "()Ljava/lang/Object;")));
    }

    @Test
    public void shouldDetectEntryPointsInClassesWithMainMethod() {
        parseClassPath(HasMainMethod.class);
        verify(this.v).newEntryPoint(ElementName.fromClass(HasMainMethod.class));
    }

    @Test
    public void shouldNotDetectEntryPointsInClassesWithoutMainMethod() {
        parseClassPath(Foo.class);
        verify(this.v, never()).newEntryPoint(any(ElementName.class));
    }

    @Test
    public void shouldDetectUsesAnInteraceInMethod() {
        parseClassPath(UsesAnInterfaceInMethod.class);
        verify(this.v).apply(access(UsesAnInterfaceInMethod.class, method("foo", "()V")),
                accessAType(AnInterface.class), AccessType.USES);
    }

    @Test
    public void shouldDetectUsageOfMethodReference() {
        parseClassPath(UsesMethodReference.class);
        verify(this.v).apply(access(UsesMethodReference.class, method("foo", "()V")),
                access(Foo.class, method("aMethod", "()Ljava/lang/Object;")), AccessType.USES);
    }

    private final Predicate<ElementName> matchOnlyExampleDotCom() {
        return item -> item.asJavaName().startsWith("com.example");
    }

    private void parseClassPath(final Class<?>... classes) {
        this.testee = makeToSeeOnlyExampleDotCom();
        this.testee.parse(new SpecificClassPathRoot(classes), this.v).get();
    }

    private ClassParser makeToSeeOnlyExampleDotCom() {
        return new ClassPathParser(matchOnlyExampleDotCom());
    }

    private AccessPoint accessAType(final Class<?> type) {
        return AccessPoint.create(ElementName.fromClass(type));
    }

    private AccessPoint access(final Class<?> type, final AccessPointName method) {
        return AccessPoint.create(ElementName.fromClass(type), method);
    }

    private AccessPointName methodWithParameter(String name, Class<?> paramType) {
        return AccessPointName.create(name, Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(paramType)));
    }

    private AccessPointName method(String name, String desc) {
        return AccessPointName.create(name, desc);
    }

    private AccessPointName method(String name, Class<?> retType) {
        return AccessPointName.create(name, Type.getMethodDescriptor(Type.getType(retType)));
    }

}
