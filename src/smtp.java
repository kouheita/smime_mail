import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.Properties;
import java.util.stream.Collectors;

import javax.activation.DataHandler;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeUtility;
import javax.mail.util.ByteArrayDataSource;

public class smtp {

	private String readAll(final String path) 
	throws IOException 
	{
		String lines = null;

	    lines = Files.lines(Paths.get(path), Charset.forName("UTF-8"))
	        .collect(Collectors.joining(System.getProperty("line.separator")));

		return lines;
	}
	
	public void sendmail(String smtpHost, String username, String password, String from, String to, String title, String context, String attach)
	{
		Session 		session = null;
		Transport		transport = null;
		MimeMessage		message = null;
		MimeBodyPart 	body = null;
		MimeBodyPart 	pdf = null;
		String			attachType = "application/pdf";
		MimeMultipart	multipart = null;

    	try {
			System.setProperty("mail.mime.foldencodedwords", "true");

			Properties properties = new Properties();
			properties.put("mail.transport.protocol", "smtps");
			properties.put("mail.smtp.auth", "true");
			properties.put("mail.smtp.starttls.enable", "true");
			properties.put("mail.smtp.starttls.required", "true");
			properties.put("mail.smtp.host", smtpHost);
			properties.put("mail.smtp.port", "465");
            properties.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            properties.put("mail.smtp.socketFactory.fallback", "true");

			session = Session.getInstance(properties,
								new javax.mail.Authenticator() {
									protected PasswordAuthentication getPasswordAuthentication() {
										return new PasswordAuthentication(username, password);
									}
								}
							);
			//session.setDebug(true);
			message = new MimeMessage(session);
			message.setFrom(new InternetAddress(from));
			message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
			message.setSubject(MimeUtility.encodeText(title, "iso-2022-jp", "B"));
			message.setSentDate(new Date());

			multipart = new MimeMultipart();
			message.setContent(multipart);

			String lines = readAll(context);
			if (lines == null){
				lines = "This message is empty. for perpose test only.";
			}
			body = new MimeBodyPart();
			body.setText(lines, "iso-2022-jp");
			body.setHeader("Content-Transfer-Encoding", "7bit");
			multipart.addBodyPart(body);

			pdf = new MimeBodyPart();
            pdf.setFileName(MimeUtility.encodeText(attach, "iso-2022-jp", "B"));

			FileSystem	fs = FileSystems.getDefault();
			Path 		p = fs.getPath(attach);
			byte[] 		bytes = Files.readAllBytes(p);

        	pdf.setDataHandler(new DataHandler(new ByteArrayDataSource(new ByteArrayInputStream(bytes), attachType)));
            multipart.addBodyPart(pdf);

			transport = session.getTransport();
			transport.send(message);
			transport.close();

			System.out.println("Sent message successfully....");
		}
    	catch (MessagingException e) {
    		e.printStackTrace();
    	}
		catch (Exception e) {
         	e.printStackTrace();
      	}
	}
}
