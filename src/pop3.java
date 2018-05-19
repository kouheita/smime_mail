import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Properties;

import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.NoSuchProviderException;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;

import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cms.RecipientId;
import org.bouncycastle.cms.RecipientInformation;
import org.bouncycastle.cms.RecipientInformationStore;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.SignerInformationStore;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.cms.jcajce.JceKeyTransEnvelopedRecipient;
import org.bouncycastle.cms.jcajce.JceKeyTransRecipientId;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.mail.smime.SMIMEEnveloped;
import org.bouncycastle.mail.smime.SMIMESigned;
import org.bouncycastle.mail.smime.SMIMEUtil;

public class pop3 {
	String	strCertFile = null;
	String 	strPassword = null;
	
	public void writeEnvelope(Message m)
	throws Exception
	{
      	System.out.println("This is the message envelope");
      	System.out.println("---------------------------");
      	Address[] a;

      	// FROM
      	if ((a = m.getFrom()) != null) {
         	for (int j = 0; j < a.length; j++){
         		System.out.println("FROM: " + a[j].toString());
			}
      	}

      	// TO
      	if ((a = m.getRecipients(Message.RecipientType.TO)) != null) {
         	for (int j = 0; j < a.length; j++){
         		System.out.println("TO: " + a[j].toString());
			}
      	}

      	// SUBJECT
      	if (m.getSubject() != null){
         	System.out.println("SUBJECT: " + m.getSubject());
		}
	}
	
	private static void verify(SMIMESigned s)
	throws Exception
	{
		org.bouncycastle.util.Store 	certs = s.getCertificates();
		SignerInformationStore  		signers = s.getSignerInfos();
		Collection<SignerInformation>   c = signers.getSigners();
		Iterator<SignerInformation>     it = c.iterator();
		
		while (it.hasNext()) {
			SignerInformation   signer = (SignerInformation)it.next();
			Collection          certCollection = certs.getMatches(signer.getSID());
			Iterator        	certIt = certCollection.iterator();
			X509Certificate 	cert = new JcaX509CertificateConverter().setProvider("BC").getCertificate((X509CertificateHolder)certIt.next());
			
			if (signer.verify(new JcaSimpleSignerInfoVerifierBuilder().setProvider("BC").build(cert))) {
				System.out.println("signature verified");
			}
			else {
				System.out.println("signature failed!");
			}
		}
	}

	public void writePart(Part p)
	throws Exception
	{
      	if (p instanceof Message){
         	//Call methos writeEnvelope
         	writeEnvelope((Message) p);
		}
      	System.out.println("----------------------------");
      	System.out.println("CONTENT-TYPE: " + p.getContentType());

      	//check if the content is plain text
      	if (p.isMimeType("text/plain")) {
         	System.out.println("This is plain text");
         	System.out.println("---------------------------");
         	System.out.println((String) p.getContent());
      	} 
      	//check if the content has attachment
		else if (p.isMimeType("multipart/signed")) {
      		SMIMESigned		s = new SMIMESigned((MimeMultipart)p.getContent());
      		MimeBodyPart	content = s.getContent();
      		
      		System.out.println("Content:");
      		
      		Object  cont = content.getContent();
      		if (cont instanceof String) {
      			System.out.println((String)cont);
      		}
      		else if (cont instanceof Multipart) {
      			Multipart   mp = (Multipart)cont;
      			int count = mp.getCount();
      			for (int i = 0; i < count; i++) {
      				writePart(mp.getBodyPart(i));
      				//BodyPart    m = mp.getBodyPart(i);
      				//Object      part = m.getContent();
      				
      				//System.out.println("Part " + i);
      				//System.out.println("---------------------------");
      				//if (part instanceof String){
      				//	System.out.println((String)part);
      				//}
      				//else {
      				//	System.out.println("can't print...");
      				//}
      			}
      		}
      		System.out.println("Status:");
      		verify(s);
      	}
		else if (p.isMimeType("application/pkcs7-mime") || p.isMimeType("application/x-pkcs7-mime")) {
			SMIMESigned             s = new SMIMESigned(p);
			MimeBodyPart            content = s.getContent();
			
			System.out.println("Content:");
			
			Object  cont = content.getContent();
			if (cont instanceof String) {
				System.out.println((String)cont);
			}
			System.out.println("Status:");
			verify(s);
		}
		else if (p.isMimeType("multipart/encrypted")) {
			SMIMEEnveloped       		m = new SMIMEEnveloped((MimeBodyPart) p.getContent());
			
			KeyStore    ks = KeyStore.getInstance("PKCS12", "BC");
			ks.load(new FileInputStream(strCertFile), strPassword.toCharArray());

			Enumeration<String> e = ks.aliases();
			String      keyAlias = null;
			while (e.hasMoreElements()) {
				String  alias = (String)e.nextElement();
				if (ks.isKeyEntry(alias)) {
					keyAlias = alias;
					break;
				}
			}
			if (keyAlias != null) {
				X509Certificate cert = (X509Certificate)ks.getCertificate(keyAlias);
				RecipientId     recId = new JceKeyTransRecipientId(cert);
				
				RecipientInformationStore   recipients = m.getRecipientInfos();
				RecipientInformation        recipient = recipients.get(recId);
				
				MimeBodyPart	res = SMIMEUtil.toMimeBodyPart(recipient.getContent(new JceKeyTransEnvelopedRecipient((PrivateKey)ks.getKey(keyAlias, null)).setProvider("BC")));
				Multipart mp = (Multipart) res.getContent();
				int count = mp.getCount();
	         	for (int i = 0; i < count; i++){
	            	writePart(mp.getBodyPart(i));
				}
			}
		}
      	else if (p.isMimeType("multipart/*")) {
         	System.out.println("This is a Multipart");
         	System.out.println("---------------------------");
         	Multipart mp = (Multipart) p.getContent();

         	int count = mp.getCount();

         	for (int i = 0; i < count; i++){
            	writePart(mp.getBodyPart(i));
			}
      	} 
      	//check if the content is a nested message
      	else if (p.isMimeType("message/rfc822")) {
         	System.out.println("This is a Nested Message");
         	System.out.println("---------------------------");
         	writePart((Part) p.getContent());
      	} 
      	//check if the content is an inline image
      	else if (p.isMimeType("image/jpeg")) {
         	System.out.println("--------> image/jpeg");
         	Object o = p.getContent();

         	InputStream x = (InputStream) o;
         	// Construct the required byte array
         	System.out.println("x.length = " + x.available());
       		byte[] bArray = new byte[x.available()];
   			int i = 0;
         	while ((i = (int) ((InputStream) x).available()) > 0) {
            	int result = (int) (((InputStream) x).read(bArray));
            	if (result == -1){
	            	break;
	            }
         	}
         	FileOutputStream f2 = new FileOutputStream("/tmp/image.jpg");
         	f2.write(bArray);
      	} 
      	else if (p.getContentType().contains("image/")) {
         	System.out.println("content type" + p.getContentType());
         	File f = new File("image" + new Date().getTime() + ".jpg");
         	DataOutputStream output = new DataOutputStream(
            new BufferedOutputStream(new FileOutputStream(f)));
            com.sun.mail.util.BASE64DecoderStream test = (com.sun.mail.util.BASE64DecoderStream) p.getContent();
         	byte[] buffer = new byte[1024];
         	int bytesRead;
         	while ((bytesRead = test.read(buffer)) != -1) {
            	output.write(buffer, 0, bytesRead);
         	}
      	} 
      	else {
         	Object o = p.getContent();
         	if (o instanceof String) {
            	System.out.println("This is a string");
            	System.out.println("---------------------------");
            	System.out.println((String) o);
         	} 
         	else if (o instanceof InputStream) {
            	System.out.println("This is just an input stream");
            	System.out.println("---------------------------");
            	InputStream is = (InputStream) o;
            	is = (InputStream) o;
            	int c;
            	while ((c = is.read()) != -1){
               		System.out.write(c);
				}
         	} 
         	else {
            	System.out.println("This is an unknown type");
            	System.out.println("---------------------------");
            	System.out.println(o.toString());
         	}
      	}
	}
	
	public void fetch(String	strCertFile, 
			  		  String 	strPassword,
			  		  String	pop3Host, 
					  String 	storeType, 
					  String 	user, 
					  String 	password)
	{
		try {
			if (Security.getProvider("BC") == null) {
				Security.addProvider(new BouncyCastleProvider());
			}
			this.strCertFile = strCertFile;
			this.strPassword = strPassword;
		
			Properties properties = new Properties();
			properties.put("mail.store.protocol", "pop3");
			properties.put("mail.pop3.host", pop3Host);
			properties.put("mail.pop3.port", "110");
			properties.put("mail.pop3.starttls.enable", "true");
			Session emailSession = Session.getDefaultInstance(properties);
			Store store = emailSession.getStore("pop3s");
			store.connect(pop3Host, user, password);
			Folder emailFolder = store.getFolder("INBOX");
			emailFolder.open(Folder.READ_ONLY);
			BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
			Message[] messages = emailFolder.getMessages();
			System.out.println("messages.length---" + messages.length);
			for (int i = 0; i < messages.length; i++) {
				Message message = messages[i];
				System.out.println("---------------------------------");
				writeEnvelope(message);
			}
			emailFolder.close(false);
			store.close();
		}
		catch (NoSuchProviderException e) {
         	e.printStackTrace();
      	} 
		catch (MessagingException e) {
         	e.printStackTrace();
      	} 
		catch (Exception e) {
         	e.printStackTrace();
      	}
	}
}
