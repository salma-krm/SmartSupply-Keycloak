package org.smartsupply.service.implementation;

import org.smartsupply.model.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserContext {

    private static final ThreadLocal<User> currentUser = new ThreadLocal<>();

    public void setCurrentUser(User user) {
        currentUser.set(user);
    }

    public User getCurrentUser() {
        return currentUser.get();
    }

    public void clear() {
        currentUser.remove();
    }
}