package fixtures.modulith.violation;

import org.springframework.modulith.Modulithic;

/**
 * Root of a deliberately broken module layout, used by {@code ModularityTest} to prove that
 * boundary violations are detected (spec 0001 AC-6). It lives outside the application's base
 * package so that it never becomes part of the real application.
 */
@Modulithic
public class ViolationFixtureApplication {}
