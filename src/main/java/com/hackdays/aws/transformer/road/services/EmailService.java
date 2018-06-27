package com.hackdays.aws.transformer.road.services;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder;
import com.amazonaws.services.simpleemail.model.Body;
import com.amazonaws.services.simpleemail.model.Content;
import com.amazonaws.services.simpleemail.model.Destination;
import com.amazonaws.services.simpleemail.model.Message;
import com.amazonaws.services.simpleemail.model.SendEmailRequest;
import com.hackdays.aws.transformer.road.domains.StreetDetail; 

@Service
public class EmailService {
  private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

  // Replace sender@example.com with your "From" address.
  // This address must be verified with Amazon SES.
  private static final String FROM = "gohhuimay@gmail.com";
  
  // The subject line for the email.
  private static final String SUBJECT = "RoadTransformers - %s: Bad Road Conditions (%.1f%%)";
  
  // The HTML body for the email.
  private static final String HTMLBODY = "<h1>%s</h1>"
	  + "<p>%s</p>"
      + "<p>This email was sent with <a href='https://aws.amazon.com/ses/'>"
      + "Amazon SES</a> using the <a href='https://aws.amazon.com/sdk-for-java/'>" 
      + "AWS SDK for Java</a></p>";

  // The email body for recipients with non-HTML email clients.
  private static final String TEXTBODY = "%s \n%s";
  
  public static void sendBadRoadEmail(String toEmail, StreetDetail streetDetail, float confidence) {
	  String subject = String.format(SUBJECT, streetDetail.getRoadName(), confidence);
	  String header = String.format("%s: Bad Road Conditions (%.1f%%)", streetDetail.getRoadName(), confidence);
	  String body = String.format("The road condition at %s, %s is bad (%.1f%%), please take another route.",
			  streetDetail.getRoadName(), streetDetail.getCountry(), confidence);
			  
	  String htmlBody = String.format(HTMLBODY, header, body);
	  String textBody = String.format(TEXTBODY, header, body);
	  sendEmail(FROM, toEmail, subject, htmlBody, textBody);
  }

  public static void sendEmail(String fromEmail, String toEmail, String subject, String htmlBody, String textBody) {

    try {
      AmazonSimpleEmailService client = 
          AmazonSimpleEmailServiceClientBuilder.standard()
          // Replace US_WEST_2 with the AWS Region you're using for
          // Amazon SES.
            .withRegion(Regions.US_EAST_1).build();
      SendEmailRequest request = new SendEmailRequest()
          .withDestination(
              new Destination().withToAddresses(toEmail))
          .withMessage(new Message()
              .withBody(new Body()
                  .withHtml(new Content()
                      .withCharset("UTF-8").withData(htmlBody))
                  .withText(new Content()
                      .withCharset("UTF-8").withData(textBody)))
              .withSubject(new Content()
                  .withCharset("UTF-8").withData(subject)))
          .withSource(fromEmail);
      client.sendEmail(request);
      logger.info(String.format("Sent email to %s with subject %s",toEmail, subject));
    } catch (Exception ex) {
      logger.error("The email was not sent. Error message: " 
          + ex.getMessage());
    }
  }
}