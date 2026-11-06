package com.aniplato.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 6.3: a Suspended account cannot post, offer or accept; everything else still works. */
class AccountTest {

    @Test
    void onlyAnActiveAccountMayPostOrOffer() {
        Admin account = new Admin();

        account.status = Account.Status.ACTIVE;
        assertTrue(account.canPostOrOffer());
        assertFalse(account.isSuspended());

        account.status = Account.Status.SUSPENDED;
        assertFalse(account.canPostOrOffer());
        assertTrue(account.isSuspended());
    }
}
