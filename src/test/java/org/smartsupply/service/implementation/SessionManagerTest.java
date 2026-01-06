package org.smartsupply.service.implementation;

import org.junit.jupiter.api.Test;
import org.smartsupply.model.entity.User;

import static org.junit.jupiter.api.Assertions.*;

class SessionManagerTest {

    @Test
    void sessionCreateAndInvalidate_andValidation() {
        SessionManager sm = new SessionManager();
        User u = new User();
        u.setId(2L);

        String sessionId = sm.createSession(u);
        assertNotNull(sessionId);
        assertTrue(sm.isSessionValid(sessionId));
        assertEquals(u, sm.getUserFromSession(sessionId));

        sm.invalidateSession(sessionId);
        assertFalse(sm.isSessionValid(sessionId));
        assertNull(sm.getUserFromSession(sessionId));
    }
}