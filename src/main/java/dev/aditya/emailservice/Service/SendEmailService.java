package dev.aditya.emailservice.Service;

import dev.aditya.emailservice.Dto.EmailResponseDto;
import dev.aditya.emailservice.Util.EmailUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;

@Service
public class SendEmailService {
    @Autowired
    ObjectMapper objectMapper;

    @KafkaListener(topics = "notification-signup",groupId = "email-service-group")
    public void sendSignupEmail(String message){

        System.out.println("Received message from Kafka topic notification-signup: " + message);

        EmailResponseDto emailResponseDto = objectMapper.readValue(message,EmailResponseDto.class);

        //@Value("${email.signup.email}") -> can't use this for local vars. SPRING only works for class level var to maintain singleton.
        final String fromEmail = System.getenv("Signup_Email"); //requires valid gmail id

        //not final cause passwords can change or expire, don't want to keep on restarting service everytime
        String password= System.getenv("Signup_Password"); //correct password for gmail id

        sendEmail(fromEmail,password,emailResponseDto);

    }
    @KafkaListener(topics = {"notification-login","notification-update-details","notification-reset-password"},groupId = "email-service-group")
    public void sendSecurityEmail(String message){

        System.out.println("Received message from Kafka topic notification-login or notification-update-details or notification-reset-password:" + message);

        EmailResponseDto emailResponseDto = objectMapper.readValue(message,EmailResponseDto.class);

        final String fromEmail = System.getenv("Security_Email"); //requires valid gmail id

        String password = System.getenv("Security_Password"); // correct password for gmail id

        sendEmail(fromEmail,password,emailResponseDto);

    }

    //This method actually sends the email
    private void sendEmail(String fromEmail, String password, EmailResponseDto emailResponseDto){
        /**
         Outgoing Mail (SMTP) Server
         requires TLS or SSL: smtp.gmail.com (use authentication)
         Use Authentication: Yes
         Port for TLS/STARTTLS: 587
         */
        System.out.println("TLSEmail Start");
        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com"); //SMTP Host
        props.put("mail.smtp.port", "587"); //TLS Port
        props.put("mail.smtp.auth", "true"); //enable authentication
        props.put("mail.smtp.starttls.enable", "true"); //enable STARTTLS

        //create Authenticator object to pass in Session.getInstance argument
        Authenticator auth = new Authenticator() {
            //override the getPasswordAuthentication method
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(fromEmail, password);
            }
        };
        Session session = Session.getInstance(props, auth);

        EmailUtil.sendEmail(session, emailResponseDto.getTo(),emailResponseDto.getSubject(), emailResponseDto.getBody());

    }

}

