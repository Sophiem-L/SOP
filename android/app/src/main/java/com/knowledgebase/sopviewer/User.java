package com.knowledgebase.sopviewer;

import java.util.List;

public class User {
    private int id;
    private String name;
    private String full_name;
    private String email;
    private String firebase_uid;
    private List<Role> roles;

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getFullName() {
        return full_name;
    }

    public String getEmail() {
        return email;
    }

    public String getFirebaseUid() {
        return firebase_uid;
    }

    public List<Role> getRoles() {
        return roles;
    }

    public static class Role {
        private int id;
        private String name;

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }
    }
}
