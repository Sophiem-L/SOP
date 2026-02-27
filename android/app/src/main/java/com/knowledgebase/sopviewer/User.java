package com.knowledgebase.sopviewer;

import java.util.List;

public class User {
    private int id;
    private String name;
    private String full_name;
    private String email;
    private String firebase_uid;
    private String phone;
    private String job_title;
    private String profile_photo_url;
    private List<Role> roles;
    private Department department;
    private String avatar_url;

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getFullName() {
        return full_name;
    }

    public String getPhone() {
        return phone;
    }

    public String getJobTitle() {
        return job_title;
    }

    public String getEmail() {
        return email;
    }

    public String getFirebaseUid() {
        return firebase_uid;
    }

    public String getProfilePhotoUrl() {
        return profile_photo_url;
    }

    public List<Role> getRoles() {
        return roles;
    }

    public Department getDepartment() {
        return department;
    }

    public String getAvatarUrl() {
        return avatar_url;
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

    public static class Department {
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
