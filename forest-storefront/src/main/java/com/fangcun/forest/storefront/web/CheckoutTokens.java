package com.fangcun.forest.storefront.web;

import com.fangcun.forest.storefront.common.BusinessException;
import jakarta.servlet.http.HttpSession;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class CheckoutTokens {
    private static final String KEY = "forest.checkout.drafts";
    public String store(HttpSession session, CheckoutDraft draft) {
        synchronized (session) {
            Map<String, CheckoutDraft> drafts = drafts(session);
            drafts.entrySet().removeIf(entry -> expired(entry.getValue()));
            while (drafts.size() >= 6) drafts.remove(drafts.keySet().iterator().next());
            String token = UUID.randomUUID().toString();
            drafts.put(token, draft);
            session.setAttribute(KEY, drafts);
            return token;
        }
    }
    public CheckoutDraft require(HttpSession session, String token, long userId) {
        synchronized (session) {
            CheckoutDraft draft = drafts(session).get(token);
            if (draft == null || draft.userId() != userId || expired(draft)) {
                throw new BusinessException(HttpStatus.CONFLICT, "结算页面已过期，请返回购物车重新结算。");
            }
            return draft;
        }
    }
    private boolean expired(CheckoutDraft draft) { return draft.createdAt().plusSeconds(1800).isBefore(java.time.Instant.now()); }
    @SuppressWarnings("unchecked")
    private Map<String, CheckoutDraft> drafts(HttpSession session) {
        Object existing = session.getAttribute(KEY);
        return existing instanceof Map<?, ?> ? new LinkedHashMap<>((Map<String, CheckoutDraft>) existing) : new LinkedHashMap<>();
    }
}
