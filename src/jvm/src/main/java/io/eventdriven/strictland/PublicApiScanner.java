package io.eventdriven.strictland;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import java.lang.reflect.*;
import java.util.Arrays;
import java.util.Comparator;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Lists a package's public types, fields, constructors, and methods as plain text, so you can pin
 * your public API the same way you pin a message: snapshot the text, and any accidental change to the
 * surface — a method gone, a parameter added, a return type changed — fails the build.
 *
 * <p>Point it at a package, optionally trim what you don't want to track, and call {@link #generate()}
 * for the text to hand to your approval check.</p>
 *
 * <pre>
 * String api = PublicApiScanner.forPackage("com.myapp.events.v1").generate();
 * // compare api against a committed approved file with your approval check of choice
 * </pre>
 */
public class PublicApiScanner {
    private final String packageName;
    private Predicate<Method> methodFilter = m -> true;
    private boolean excludeConstructors;

    private PublicApiScanner(String packageName) {
        this.packageName = packageName;
    }

    /**
     * Starts a scan of the given package, your entry point for capturing its public API. Chain the
     * {@code exclude...} methods to trim the surface, then call {@link #generate()}.
     *
     * @param packageName the package to scan, e.g. {@code "com.myapp.events.v1"}
     * @return a scanner you refine and then {@link #generate()} from
     */
    public static PublicApiScanner forPackage(String packageName) {
        return new PublicApiScanner(packageName);
    }

    /**
     * Leaves out any method your predicate matches, for trimming calls you don't count as part of the
     * contract.
     *
     * @param filter picks the methods to drop; one it matches won't appear
     * @return this scanner, so you can keep chaining
     */
    public PublicApiScanner excludingMethods(Predicate<Method> filter) {
        this.methodFilter = this.methodFilter.and(filter.negate());
        return this;
    }

    /**
     * Leaves constructors out, for when you track the methods and fields a caller uses but not how
     * instances get built.
     *
     * @return this scanner, so you can keep chaining
     */
    public PublicApiScanner excludeConstructors() {
        this.excludeConstructors = true;
        return this;
    }

    /**
     * Narrows the surface to public fields and no-argument, value-returning methods — the read-only
     * shape of a data type — dropping constructors, void and parameterized methods, and {@code
     * equals}/{@code hashCode}/{@code toString}. Handy for pinning the contract of records or DTOs.
     *
     * @return this scanner, so you can keep chaining
     */
    public PublicApiScanner onlyGettersAndFields() {
        return excludeConstructors()
                .excludeStandardObjectMethods()
                .excludingMethods(m -> m.getParameterCount() > 0 || m.getReturnType() == void.class);
    }

    /**
     * Leaves out {@code equals}, {@code hashCode}, and {@code toString}, so generated boilerplate
     * doesn't clutter the captured surface or churn it every time you add a field.
     *
     * @return this scanner, so you can keep chaining
     */
    public PublicApiScanner excludeStandardObjectMethods() {
        return excludingMethods(m -> (m.getName().equals("equals") && m.getParameterCount() == 1)
                || (m.getName().equals("hashCode") && m.getParameterCount() == 0)
                || (m.getName().equals("toString") && m.getParameterCount() == 0));
    }

    /**
     * Scans the package and returns its public surface as text, sorted so the same API always renders
     * the same way, ready to hand to your approval check. Only public types and members appear, minus
     * anything you excluded.
     *
     * @return the package's public API rendered as text
     */
    public String generate() {
        try (var scanResult =
                new ClassGraph().enableAllInfo().acceptPackages(packageName).scan()) {

            return scanResult.getAllClasses().stream()
                    .map(ClassInfo::loadClass)
                    .filter(c -> Modifier.isPublic(c.getModifiers()))
                    .sorted(Comparator.comparing(Class::getName))
                    .map(this::describeType)
                    .collect(Collectors.joining("\n\n"));
        }
    }

    private String describeType(Class<?> clazz) {
        var sb = new StringBuilder();
        sb.append(typeDeclaration(clazz)).append(" {\n");

        Arrays.stream(clazz.getDeclaredFields())
                .filter(f -> Modifier.isPublic(f.getModifiers()))
                .sorted(Comparator.comparing(Field::getName))
                .forEach(f -> sb.append("  ").append(f.toGenericString()).append(";\n"));

        if (!excludeConstructors) {
            Arrays.stream(clazz.getDeclaredConstructors())
                    .filter(c -> Modifier.isPublic(c.getModifiers()))
                    .sorted(Comparator.comparing(Constructor::toGenericString))
                    .forEach(c -> sb.append("  ").append(formatConstructor(c)).append(";\n"));
        }

        Arrays.stream(clazz.getDeclaredMethods())
                .filter(m -> Modifier.isPublic(m.getModifiers()) && !m.isSynthetic())
                .filter(methodFilter)
                .sorted(Comparator.comparing(Method::getName).thenComparing(Method::toGenericString))
                .forEach(m -> sb.append("  ").append(formatMethod(m)).append(";\n"));

        sb.append("}");
        return sb.toString();
    }

    private static String formatConstructor(Constructor<?> c) {
        var params = Arrays.stream(c.getGenericParameterTypes())
                .map(Type::getTypeName)
                .collect(Collectors.joining(", "));
        return Modifier.toString(c.getModifiers()) + " " + c.getDeclaringClass().getSimpleName() + "(" + params + ")";
    }

    private static String formatMethod(Method m) {
        var params = Arrays.stream(m.getGenericParameterTypes())
                .map(Type::getTypeName)
                .collect(Collectors.joining(", "));
        return Modifier.toString(m.getModifiers()) + " "
                + m.getGenericReturnType().getTypeName() + " " + m.getName() + "(" + params + ")";
    }

    private static String typeDeclaration(Class<?> clazz) {
        var parts = new StringBuilder();

        parts.append("public ");
        if (Modifier.isStatic(clazz.getModifiers())) parts.append("static ");
        if (Modifier.isFinal(clazz.getModifiers()) && !clazz.isEnum()) parts.append("final ");

        if (clazz.isRecord()) parts.append("record");
        else if (clazz.isInterface()) parts.append("interface");
        else if (clazz.isEnum()) parts.append("enum");
        else parts.append("class");

        parts.append(" ").append(clazz.getName());

        var interfaces = clazz.getInterfaces();
        if (interfaces.length > 0) {
            var keyword = clazz.isInterface() ? " extends " : " implements ";
            parts.append(keyword)
                    .append(Arrays.stream(interfaces).map(Class::getName).collect(Collectors.joining(", ")));
        }

        return parts.toString();
    }
}
