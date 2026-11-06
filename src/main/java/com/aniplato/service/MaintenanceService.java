package com.aniplato.service;

import com.aniplato.model.Post;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * The automatic checks of spec 6.6, run in the spec's order. It only calls other services (A4) and owns no
 * rule: each check lives in the service that owns the status it changes. A feature that builds a check adds
 * one line here, at the position 6.6 gives it, and a test in MaintenanceServiceTest.
 */
public class MaintenanceService {

    private final HandoffService handoffService = new HandoffService();
    private final PostService postService = new PostService();
    private final OfferService offerService = new OfferService();
    private final ReportService reportService = new ReportService();
    private final AccountStatusService accountStatusService = new AccountStatusService();

    /** Runs every check. A check that fails throws to the caller; the shell decides what to do. */
    public void runMaintenance() {
        LocalDateTime now = LocalDateTime.now();
        handoffService.lapseMissedPickups(now);                            // 1. accepted offer past its pickup grace: Lapsed, post Expired
        postService.expireOpenPosts(Post.Kind.LISTING, now);               // 2. open listing past its use-by: Expired, its offers expire
        postService.expireOpenPosts(Post.Kind.REQUEST, now);               // 3. open request past its needed-by: Expired, its offers expire
        offerService.expireUnansweredOffers(now);                          // 4. offered offer past its pickup time: Expired
        reportService.flagOverdueReports(now);                             // 5. completed handoff with an overdue report: flag once
        accountStatusService.suspendAccountsWithExpiredDocuments(LocalDate.now());   // 6. expired document: Suspended
    }
}
