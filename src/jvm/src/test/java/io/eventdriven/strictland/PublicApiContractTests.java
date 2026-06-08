package io.eventdriven.strictland;

import org.approvaltests.Approvals;
import org.junit.jupiter.api.Test;

final class PublicApiContractTests {

    @Test
    void givenAContractsPackage_whenIScanItsPublicApi_thenItMatchesTheApprovedSurface() {
        var api = PublicApiScanner.forPackage("io.eventdriven.strictland.tests.contracts.v1")
                .generate();

        Approvals.verify(api);
    }

    @Test
    void givenAnEvolvedContractsPackage_whenIScanItsPublicApi_thenItMatchesTheApprovedSurface() {
        var api = PublicApiScanner.forPackage("io.eventdriven.strictland.tests.contracts.v2")
                .generate();

        Approvals.verify(api);
    }

    @Test
    void givenAContractsPackage_whenIScanExcludingTheCompareToMethod_thenItIsHiddenFromTheSurface() {
        var api = PublicApiScanner.forPackage("io.eventdriven.strictland.tests.contracts.v2")
                .excludingMethods(m -> m.getName().equals("compareTo"))
                .generate();

        Approvals.verify(api);
    }

    @Test
    void givenAContractsPackage_whenIScanExcludingConstructors_thenTheSurfaceFocusesOnFieldsAndMethods() {
        var api = PublicApiScanner.forPackage("io.eventdriven.strictland.tests.contracts.v2")
                .excludeConstructors()
                .generate();

        Approvals.verify(api);
    }

    @Test
    void givenAContractsPackage_whenIScanExcludingStandardObjectMethods_thenEqualsHashCodeAndToStringAreHidden() {
        var api = PublicApiScanner.forPackage("io.eventdriven.strictland.tests.contracts.v2")
                .excludeStandardObjectMethods()
                .generate();

        Approvals.verify(api);
    }

    @Test
    void givenAContractsPackage_whenIScanOnlyGettersAndFields_thenTheSurfaceSummarisesObservableState() {
        var api = PublicApiScanner.forPackage("io.eventdriven.strictland.tests.contracts.v2")
                .onlyGettersAndFields()
                .generate();

        Approvals.verify(api);
    }

    @Test
    void givenAnApiShapesPackage_whenIScanOnlyGettersAndFields_thenVoidActionsAndParameterisedMethodsAreHidden() {
        var api = PublicApiScanner.forPackage("io.eventdriven.strictland.tests.contracts.apishapes")
                .onlyGettersAndFields()
                .generate();

        Approvals.verify(api);
    }

    @Test
    void givenAnApiShapesPackage_whenIScanExcludingStandardObjectMethods_thenOnlyTrueObjectOverridesWouldBeHidden() {
        var api = PublicApiScanner.forPackage("io.eventdriven.strictland.tests.contracts.apishapes")
                .excludeStandardObjectMethods()
                .generate();

        Approvals.verify(api);
    }
}
