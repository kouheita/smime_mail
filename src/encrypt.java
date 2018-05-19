import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Enumeration;
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

import org.bouncycastle.cms.CMSAlgorithm;
import org.bouncycastle.cms.jcajce.JceCMSContentEncryptorBuilder;
import org.bouncycastle.cms.jcajce.JceKeyTransRecipientInfoGenerator;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.mail.smime.SMIMEEnvelopedGenerator;


public class encrypt {
	
	private String readAll(final String path) 
	{
		String lines = null;

		try {
		    lines = Files.lines(Paths.get(path), Charset.forName("UTF-8"))
		        .collect(Collectors.joining(System.getProperty("line.separator")));
		}	
		catch (Exception e) {
         	e.printStackTrace();
      	}
		return lines;
	}
	
	public void doEncrypt(String	strCertFile,
						  String 	smtpHost,
	    				  String 	from, 
	    				  String 	to, 
	    				  String 	username, 
	    				  String 	password, 
	    				  String 	title,
						  String 	context,
						  String 	attach,
						  String	algorithm) {
		
    	MimeMessage message = null;
    	Session 	session = null;
		String		attachType = "application/pdf";

		try {
			if (Security.getProvider("BC") == null) {
				Security.addProvider(new BouncyCastleProvider());
			}
				
			FileInputStream inputStream = new FileInputStream(strCertFile);
			CertificateFactory cf = CertificateFactory.getInstance("X.509");
			X509Certificate cert = (X509Certificate)cf.generateCertificate(inputStream);
			
			SMIMEEnvelopedGenerator  gen = new SMIMEEnvelopedGenerator();
			gen.addRecipientInfoGenerator(new JceKeyTransRecipientInfoGenerator(cert).setProvider("BC"));
			
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
			message = new MimeMessage(session);
			message.setFrom(new InternetAddress(from));
			message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
			message.setSubject(MimeUtility.encodeText(title, "iso-2022-jp", "B"));
			message.setSentDate(new Date());
			
			MimeBodyPart    body = new MimeBodyPart();
			String lines = readAll(context);
			if (lines == null){
				lines = "This message is empty. for perpose test only.";
			}
			body.setText(lines, "iso-2022-jp");
			body.setHeader("Content-Transfer-Encoding", "8bit");

			MimeBodyPart pdf = new MimeBodyPart();
			pdf.setFileName(MimeUtility.encodeText(attach, "iso-2022-jp", "B"));
			FileSystem	fs = FileSystems.getDefault();
			Path 		p = fs.getPath(attach);
			byte[] 		bytes = Files.readAllBytes(p);
        	pdf.setDataHandler(new DataHandler(new ByteArrayDataSource(new ByteArrayInputStream(bytes), attachType)));
			
			MimeMultipart multipart = new MimeMultipart();
			multipart.addBodyPart(body);
			multipart.addBodyPart(pdf);
			
			MimeBodyPart allBody = new MimeBodyPart();
			allBody.setContent(multipart);

			MimeBodyPart mp = null;
			if (algorithm.compareToIgnoreCase("RC2") == 0) {
				mp = gen.generate(allBody, new JceCMSContentEncryptorBuilder(CMSAlgorithm.RC2_CBC).setProvider("BC").build());
			}
			else if (algorithm.compareToIgnoreCase("3DES") == 0) {
				mp = gen.generate(allBody, new JceCMSContentEncryptorBuilder(CMSAlgorithm.DES_EDE3_CBC).setProvider("BC").build());
			}
			else if (algorithm.compareToIgnoreCase("AES128") == 0) {
				mp = gen.generate(allBody, new JceCMSContentEncryptorBuilder(CMSAlgorithm.AES128_CBC).setProvider("BC").build());
			}
			else if (algorithm.compareToIgnoreCase("AES192") == 0) {
				mp = gen.generate(allBody, new JceCMSContentEncryptorBuilder(CMSAlgorithm.AES192_CBC).setProvider("BC").build());
			}
			else if (algorithm.compareToIgnoreCase("AES256") == 0) {
				mp = gen.generate(allBody, new JceCMSContentEncryptorBuilder(CMSAlgorithm.AES256_CBC).setProvider("BC").build());
			}
			message.setContent(mp.getContent(), mp.getContentType());
			message.saveChanges();

			Transport.send(message);
			System.out.println("Encrypted mail will be sent.");
		}
		catch (Exception e) {
         	e.printStackTrace();
      	}
	}
}
