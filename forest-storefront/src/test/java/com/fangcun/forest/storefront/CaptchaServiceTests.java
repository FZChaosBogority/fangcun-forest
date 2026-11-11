package com.fangcun.forest.storefront;

import com.fangcun.forest.storefront.service.CaptchaService;
import java.io.ByteArrayInputStream;
import java.lang.reflect.Method;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import static org.junit.jupiter.api.Assertions.*;

class CaptchaServiceTests {
    private final CaptchaService service = new CaptchaService();
    // Read only the server-side challenge fixture; the production endpoint never returns this value.
    private String answer(MockHttpSession session, String purpose) throws Exception {
        Object challenge = session.getAttribute("forest.captcha." + purpose);
        Method accessor = challenge.getClass().getDeclaredMethod("answer");
        accessor.setAccessible(true); return (String) accessor.invoke(challenge);
    }
    @Test
    void pngIsUsableAndAChallengeCanOnlyBeConsumedOnce() throws Exception {
        MockHttpSession session = new MockHttpSession();
        var image = ImageIO.read(new ByteArrayInputStream(service.create(session, "login")));
        assertEquals(144, image.getWidth()); assertEquals(52, image.getHeight());
        String code = answer(session, "login");
        assertTrue(service.verify(session, "login", code.toLowerCase(java.util.Locale.ROOT)));
        assertFalse(service.verify(session, "login", code));
    }
    @Test
    void wrongAnswerConsumesTheChallengeAndPurposesStaySeparate() throws Exception {
        MockHttpSession session = new MockHttpSession();
        service.create(session, "login"); service.create(session, "register");
        String login = answer(session, "login"), registration = answer(session, "register");
        assertFalse(service.verify(session, "login", "wrong"));
        assertFalse(service.verify(session, "login", login));
        assertTrue(service.verify(session, "register", registration));
    }
    @Test
    void missingSessionCannotPass() { assertFalse(service.verify(null, "login", "ABCD")); }
}
