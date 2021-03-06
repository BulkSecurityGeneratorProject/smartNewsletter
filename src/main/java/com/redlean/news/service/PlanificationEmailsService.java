package com.redlean.news.service;
import com.redlean.news.domain.CompteConfig;
import com.redlean.news.domain.Planification_emails;
import com.redlean.news.repository.CompteConfigRepository;
import com.redlean.news.repository.Planification_emailsRepository;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.*;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.util.List;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;


/**
 * Service Implementation for managing PlanificationEmails.
 */
@Service
@Transactional
//@Configuration
//@EnableAsync
@EnableScheduling
public class PlanificationEmailsService {

    private final Logger log = LoggerFactory.getLogger(PlanificationEmailsService.class);
    private Planification_emailsRepository planificationEmailsRepository;
    private CompteConfigRepository compteConfigRepository;
   // @Autowired
    public PlanificationEmailsService(Planification_emailsRepository planificationEmailsRepository, CompteConfigRepository compteConfigRepository) {
        this.planificationEmailsRepository = planificationEmailsRepository;
        this.compteConfigRepository = compteConfigRepository;
    }

    /**
     * Save a planificationEmails.
     *
     * @param planificationEmails the entity to save
     * @return the persisted entity
     */
    public Planification_emails save(Planification_emails planificationEmails) {
        log.debug("Request to save PlanificationEmails : {}", planificationEmails);
        return planificationEmailsRepository.save(planificationEmails);
    }

    @Scheduled(cron = "0 * * ? * *",zone = "Europe/Paris")
    public void SendMailInDate() {
        List<Planification_emails> ListePlanif = planificationEmailsRepository.findAll();
      //  System.out.println(ListePlanif);
        for (Planification_emails planificationEmails : ListePlanif) {
            String date = planificationEmails.getDatePlanif();
            LocalDateTime dateTime = LocalDateTime.parse(date);
            LocalDateTime today=LocalDateTime.now();
            ZoneId timeZone=ZoneId.of("Europe/Paris");
            ZonedDateTime todayWithTimeZone=ZonedDateTime.of(today, timeZone);
            ZonedDateTime dateTimeWithTimeZone=ZonedDateTime.of(dateTime, timeZone);
            // DateTime now = org.joda.time.DateTime.now();
     //       System.out.println("date d'envoi avec timezone" + planificationEmails.getPlanifName() + dateTimeWithTimeZone);
         //   System.out.println("Current time with timezone" + planificationEmails.getPlanifName() + todayWithTimeZone);

            CompteConfig compteConfig = compteConfigRepository.findAll().get(0);

            Session session = new AppConfig().getAppconfig(compteConfig );

            if ((dateTimeWithTimeZone.isBefore(todayWithTimeZone)) && (planificationEmails.getStatus().equals("Non envoyée"))) {
                try {
                    MimeMessage msg = new MimeMessage(session);
                  //  msg.setFrom(planificationEmails.getExpediteur());
                    msg.setRecipients(Message.RecipientType.TO, planificationEmails.getDestinataire());
                    msg.setSubject(planificationEmails.getPlanifForEmail().getObjet());
                    Multipart multipart = new MimeMultipart();

                    MimeBodyPart htmlPart = new MimeBodyPart();
                    String htmlContent = planificationEmails.getPlanifForEmail().getContenu();
                    htmlPart.setContent(htmlContent, "text/html");
                    multipart.addBodyPart(htmlPart);

//                    MimeBodyPart attachementPart = new MimeBodyPart();
//                    attachementPart.attachFile(new File("/home/redlean/boitier.png"));
//                    multipart.addBodyPart(attachementPart);

                    msg.setContent(multipart);
                    Transport.send(msg);
                    System.out.println("---Done---");
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                planificationEmails.setStatus("Envoyée");
                planificationEmailsRepository.save(planificationEmails);
            }
            }
        }
    /**
     * Get all the planificationEmails.
     *
     * @param pageable the pagination information
     * @return the list of entities
     */


    @Transactional(readOnly = true)
    public Page<Planification_emails> findAll(Pageable pageable) {
        log.debug("Request to get all PlanificationEmails");
        return planificationEmailsRepository.findAll(pageable);
    }

    /**
     * Get one planificationEmails by id.
     *
     * @param id the id of the entity
     * @return the entity
     */
    @Transactional(readOnly = true)
    public Planification_emails findOne(Long id) {
        log.debug("Request to get PlanificationEmails : {}", id);
        return planificationEmailsRepository.findOne(id);
    }

    /**
     * Delete the planificationEmails by id.
     *
     * @param id the id of the entity
     */
    public void delete(Long id) {
        log.debug("Request to delete PlanificationEmails : {}", id);
        planificationEmailsRepository.delete(id);
    }

    public static String encrypt(String key, String initVector, String value) {
        try {
            IvParameterSpec iv = new IvParameterSpec(initVector.getBytes("UTF-8"));
            SecretKeySpec skeySpec = new SecretKeySpec(key.getBytes("UTF-8"), "AES");

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv);

            byte[] encrypted = cipher.doFinal(value.getBytes());
            return Base64.encodeBase64String(encrypted);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }

    public static String decrypt(String key, String initVector, String encrypted) {
        try {
            IvParameterSpec iv = new IvParameterSpec(initVector.getBytes("UTF-8"));
            SecretKeySpec skeySpec = new SecretKeySpec(key.getBytes("UTF-8"), "AES");

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);

            byte[] original = cipher.doFinal(Base64.decodeBase64(encrypted));

            return new String(original);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }

}
  /* AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
                ctx.register(AppConfig.class);
                ctx.refresh();
                JavaMailSenderImpl mailSender = ctx.getBean(JavaMailSenderImpl.class);

                try {
                    MimeMessage mimeMessage = mailSender.createMimeMessage();
                    //Pass true flag for multipart message
                    MimeMessageHelper mailMsg = new MimeMessageHelper(mimeMessage,
                        MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                        StandardCharsets.UTF_8.name());
                    mailMsg.setFrom(planificationEmails.getExpediteur());
                    mailMsg.setTo(planificationEmails.getDestinataire());
                    mailMsg.setSubject(planificationEmails.getPlanifForEmail().getObjet());
                    String Contenu;
                    Contenu = planificationEmails.getPlanifForEmail().getContenu().replaceAll("(?s)<[^>]*>(\\s*<[^>]*>)*", " ");
                    System.out.println(Contenu.toString());
                    mailMsg.setText(Contenu.toString());
//                    FileSystemResource file = new FileSystemResource(new File("/home/redlean/boitier.png"));
//                    mailMsg.addAttachment("boitier.png", file);
                    mailSender.send(mimeMessage);


                } catch (MessagingException e) {
                    e.printStackTrace();
                }*/

