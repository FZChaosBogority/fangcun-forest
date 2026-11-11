package com.fangcun.forest.storefront;

import com.fangcun.forest.storefront.common.BusinessException;
import com.fangcun.forest.storefront.web.CheckoutDraft;
import com.fangcun.forest.storefront.web.CheckoutTokens;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import static org.junit.jupiter.api.Assertions.*;

class CheckoutTokensTests {
    private final CheckoutTokens tokens = new CheckoutTokens();
    @Test
    void draftBelongsToTheOriginalAccountAndSession() {
        MockHttpSession session = new MockHttpSession();
        CheckoutDraft draft = new CheckoutDraft(7,"FCtest",Instant.now(),List.of());
        String token = tokens.store(session, draft);
        assertEquals(draft, tokens.require(session, token, 7));
        assertThrows(BusinessException.class, () -> tokens.require(session, token, 8));
        assertThrows(BusinessException.class, () -> tokens.require(new MockHttpSession(), token, 7));
    }
    @Test
    void expiredDraftCannotBeSubmitted() {
        MockHttpSession session = new MockHttpSession();
        String token = tokens.store(session, new CheckoutDraft(7,"FCtest",Instant.now().minusSeconds(1801),List.of()));
        assertThrows(BusinessException.class, () -> tokens.require(session, token, 7));
    }
}
