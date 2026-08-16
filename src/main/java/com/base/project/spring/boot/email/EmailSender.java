package com.base.project.spring.boot.email;

public interface EmailSender {

    void send(String to, String subject, String body);

}
