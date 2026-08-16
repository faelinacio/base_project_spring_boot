package com.base.project.spring.boot.email;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Dev-only stand-in for a real mail provider: logs the email instead of sending it, so verification/2FA links can be
 * copied straight out of the console without configuring SMTP credentials.
 */
@Component
@Profile("dev")
@Slf4j
public class LoggingEmailSender implements EmailSender {

    @Override
    public void send(String to, String subject, String body) {
        log.info("[dev email] to={} subject='{}'\n{}", to, subject, body);
    }

}
