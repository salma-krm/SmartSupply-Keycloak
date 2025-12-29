package org.smartsupply.service.implementation;

import org.junit.jupiter.api.Test;
import org.smartsupply.model.entity.User;

import static org.junit.jupiter.api.Assertions.*;

class UserContextTest {

    @Test
    void threadLocal_setGetClear_behaviour() {
        UserContext ctx = new UserContext();
        User u = new User();
        u.setId(1L);
        ctx.setCurrentUser(u);

        User fetched = ctx.getCurrentUser();
        assertNotNull(fetched);
        assertEquals(1L, fetched.getId());

        ctx.clear();
        assertNull(ctx.getCurrentUser());
    }
}