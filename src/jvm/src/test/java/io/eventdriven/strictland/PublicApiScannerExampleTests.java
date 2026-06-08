package io.eventdriven.strictland;

import org.approvaltests.Approvals;
import org.junit.jupiter.api.Test;

final class PublicApiScannerExampleTests {

    @Test
    void contractsV1Package_hasNoPublicApiChanges() {
        var api = PublicApiScanner.forPackage("io.eventdriven.strictland.tests.contracts.v1")
                .generate();

        Approvals.verify(api);
    }
}
