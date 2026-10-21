package com.aniplato.service;

import java.time.LocalDate;

/**
 * The automatic checks of spec 6.6, run in the spec's order. It only calls other services (A4) and owns no
 * rule: each check lives in the service that owns the status it changes. A feature that builds a check adds
 * one line here, at the position 6.6 gives it, and a test in MaintenanceServiceTest.
 */
public class MaintenanceService {

    private final AccountStatusService accountStatusService = new AccountStatusService();

    /** Runs every check that exists. A check that fails throws to the caller; the shell decides what to do. */
    public void runMaintenance() {
        // Still to add, in this order (FLOWS.md module in brackets):
        //   1. Accepted offer past its pickup grace: Lapsed, post Expired   [8]
        //   2. Open listing past its use-by: Expired, its offers expire     [5]
        //   3. Open request past its needed-by: Expired, its offers expire  [9]
        //   4. Offered offer past its pickup time: Expired                  [6]
        //   5. Completed handoff with an overdue report: flag, once         [10]
        accountStatusService.suspendAccountsWithExpiredDocuments(LocalDate.now());   // 6. expired document: Suspended
    }
}
