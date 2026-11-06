package com.aniplato.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The words a message uses for a post. */
class PostTest {

    @Test
    void aListingIsCalledAListingAndARequestARequest() {
        Post listing = new Post();
        listing.kind = Post.Kind.LISTING;
        Post request = new Post();
        request.kind = Post.Kind.REQUEST;
        assertTrue(listing.isListing());
        assertFalse(request.isListing());
        assertEquals("listing", listing.noun());
        assertEquals("request", request.noun());
    }
}
