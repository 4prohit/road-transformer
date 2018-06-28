package com.hackdays.aws.transformer.road.services;

import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private AmazonSimpleEmailService amazonSimpleEmailService;

    private static final String UTF_8 = "UTF-8";

    @Autowired
    public EmailService(AmazonSimpleEmailService amazonSimpleEmailService) {
        this.amazonSimpleEmailService = amazonSimpleEmailService;
    }

    public void sendEmail(String fromEmail, String toEmail, String subject, String htmlBody, String textBody) {
        try {
            SendEmailRequest request = new SendEmailRequest()
                    .withDestination(new Destination()
                            .withToAddresses(toEmail))
                    .withMessage(new Message()
                            .withBody(new Body()
                                    .withHtml(new Content()
                                            .withCharset(UTF_8).withData(htmlBody))
                                    .withText(new Content()
                                            .withCharset(UTF_8).withData(textBody)))
                            .withSubject(new Content()
                                    .withCharset(UTF_8).withData(subject)))
                    .withSource(fromEmail);

            amazonSimpleEmailService.sendEmail(request);
            logger.info(String.format("Sent email to %s with subject %s", toEmail, subject));
        } catch (Exception ex) {
            logger.error("The email was not sent. Error message: " + ex.getMessage());
        }
    }
}