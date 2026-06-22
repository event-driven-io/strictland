package io.eventdriven.strictland.tests.contracts.apishapes;

import java.util.UUID;

public final class ReportView {
    public final UUID reportId;

    public ReportView(UUID reportId) {
        this.reportId = reportId;
    }

    public UUID reportId() {
        return reportId;
    }

    public void refresh() {}

    public String toString(String format) {
        return format + ":" + reportId;
    }

    public int hashCode(int seed) {
        return seed * 31;
    }
}
