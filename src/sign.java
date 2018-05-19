import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
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

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.cms.AttributeTable;
import org.bouncycastle.asn1.cms.IssuerAndSerialNumber;
import org.bouncycastle.asn1.smime.SMIMEEncryptionKeyPreferenceAttribute;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.SignerInfoGenerator;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoGeneratorBuilder;
import org.bouncycastle.mail.smime.SMIMESignedGenerator;
import org.bouncycastle.mail.smime.SMIMEUtil;

public class sign {
	private PrivateKey 		key;
	private X509Certificate certificate;

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
	
    private SignerInfoGenerator createSignerInfoGenerator(String algorithm) 
    {
    	SignerInfoGenerator signer = null;
        ASN1EncodableVector encodableVector = null;

        encodableVector = new ASN1EncodableVector();
        try {
	        IssuerAndSerialNumber issuerAndSerialNumberFor = SMIMEUtil.createIssuerAndSerialNumberFor(certificate);
	        encodableVector.add(new SMIMEEncryptionKeyPreferenceAttribute(issuerAndSerialNumberFor));
	
	        AttributeTable attributeTable = new AttributeTable(encodableVector);
	        JcaSimpleSignerInfoGeneratorBuilder simpleSignerInfoGeneratorBuilder = new JcaSimpleSignerInfoGeneratorBuilder();
	        simpleSignerInfoGeneratorBuilder.setSignedAttributeGenerator(attributeTable);
	        if (algorithm == null) {
	        	algorithm = "SHA1withRSA";
	        }
	        signer =  simpleSignerInfoGeneratorBuilder.build(algorithm, key, certificate);
        }
		catch (Exception e) {
         	e.printStackTrace();
      	}
        
        return signer;
    }
    
    public void doSign(	String	strCertFile, 
    					String 	strPassword, 
    					String 	smtpHost, 
    					String 	from, 
    					String 	to, 
    					String 	username, 
    					String 	password, 
    					String 	title,
    					String 	context, 
    					String 	attach,
    					String	algorithm) 
    {
    	KeyStore	keyStore;
    	String 		aliasName = null;
    	MimeMessage message = null;
    	Session 	session = null;
    	String		attachType = "application/pdf";
    	
    	try {
    		InputStream stream = new FileInputStream(strCertFile);
    		keyStore = KeyStore.getInstance("pkcs12");
    		keyStore.load(stream, strPassword.toCharArray());
    		
    		Enumeration<String> aliases = keyStore.aliases();
    		while (aliases.hasMoreElements()) {
    			String alias = aliases.nextElement();
    			if (keyStore.isKeyEntry(alias)) {
    				aliasName = alias;
    				break;
    			}
    		}
    		key = (PrivateKey) keyStore.getKey(aliasName, strPassword.toCharArray());
    		certificate = (X509Certificate) keyStore.getCertificate(aliasName);
    		
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
			
			MimeBodyPart body = new MimeBodyPart();
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
			
			SMIMESignedGenerator generator = new SMIMESignedGenerator();
			generator.addSignerInfoGenerator(createSignerInfoGenerator(algorithm));
			
			List<X509Certificate> certificateList = new ArrayList<>();
			certificateList.add(certificate);
			generator.addCertificates(new JcaCertStore(certificateList));
			MimeMultipart mimePart = generator.generate(allBody);
			
			message.setContent(mimePart);
			message.saveChanges();
			
			Transport.send(message);
			System.out.println("Signed mail will be sent.");
    	}
    	catch (KeyStoreException e) {
    		e.printStackTrace();
    	}
		catch (Exception e) {
         	e.printStackTrace();
      	}
    }
}
