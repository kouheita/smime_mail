import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;

public class smail {
	public static void main(String[] args) {
		// TODO Auto-generated method stub
      	String 	host = null;
		String	send_port = null;
		String	send_type = null;
		String	receive_port = null;
		String	receive_type = null;
      	String	username = null;
      	String 	password = null;
		String	to = null;
		String	from = null;
		String	title = null;
		String	context = null;
		String	attach = null;
		String	cert = null;
		String	cert_type = null;
		String	cert_pass = null;
		String	cert_encrypt = null;
		String	cert_encrypt_type = null;
		String	algorithmSign = null;
		String	algorithmCryption = null;
		int		list_days = 0;
		boolean	bFirst = true;

		try {
			Properties properties = new Properties();
			String strPass = "mail.properties";
			InputStream is = new FileInputStream(strPass);
			properties.load(is);
			host = properties.getProperty("host");
			send_port = properties.getProperty("send_port");
			if ((send_port == null) || (send_port.length() == 0)){
				send_port = "25";
			}
			send_type = properties.getProperty("send_type");
			if ((send_type == null) || (send_type.length() == 0)){
				send_type = "smtp";
			}
			receive_port = properties.getProperty("receive_port");
			if ((receive_port == null) || (receive_port.length() == 0)){
				receive_port = "110";
			}
			receive_type = properties.getProperty("receive_type");
			if ((receive_port == null) || (receive_port.length() == 0)){
				receive_type = "pop3";
			}
			username = properties.getProperty("username");
			password = properties.getProperty("password");
			from = properties.getProperty("from");
			to = properties.getProperty("to");
			cert = properties.getProperty("cert");
			cert_type = properties.getProperty("cert_type");
			cert_pass = properties.getProperty("cert_pass");
			cert_encrypt = properties.getProperty("encrypt_cert");
			cert_encrypt_type = properties.getProperty("encrypt_cert_type");
			
			BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
			boolean bContinue = true;
	      	while (bContinue){
				if (bFirst == true){
		      		System.out.println("$rmail > Command : All List[L], from any days List[L <days>], Send Test[T], Signed Mail[S], Cryption Mail[E],");
		      		System.out.println("$rmail > Command : Sign and Cryption Mail[A], Quit[Q]");
					System.out.print("$> ");
					bFirst = false;
				}
				else{
					System.out.println();
					System.out.println("---------------------------------");
		      		System.out.println("$rmail > Command : All List[L], from any days List[L <days>], Send Test[T], Signed Mail[S], Cryption Mail[E],");
		      		System.out.println("$rmail > Command : Sign and Cryption Mail[A], Quit[Q]");
					System.out.print("$> ");
				}
	      		String line = reader.readLine();
	     		if (0 < line.length()){
	     			if ((line.charAt(0) == 'L') || (line.charAt(0) == 'l')){
				      	//Call method fetch
						String[] lines = line.split(" ");
						if (1 < lines.length){
							list_days = Integer.parseInt(lines[1]);
						}
						else{
							list_days = 0;
						}
						if (0 < list_days){
							if (1 < list_days){
		     					System.out.println(">> Receive Message List Before " + list_days + " days.");
							}
							else{
		     					System.out.println(">> Receive Message List Today.");
							}
						}
						else{
	     					System.out.println(">> Receive Message as All List.");
						}
						System.out.println();
						// if your favorite pop3 change as follows to pop3 class
						if ((receive_type.compareToIgnoreCase("pop3") == 0) || (receive_type.compareToIgnoreCase("pop3s") == 0)){
							pop3 instpop3 = new pop3();
							instpop3.fetch(cert, cert_pass, host, receive_port, username, password, receive_type, list_days);
						}
						else{
							imap instImap = new imap();
							instImap.fetch(cert, cert_pass, host, receive_port, username, password, receive_type, list_days);
						}
		   			}
					else if ((line.charAt(0) == 'F') || (line.charAt(0) == 'f')){
						System.out.print(">> Send to : ");
						to = reader.readLine();
						System.out.print(">> From to : ");
						from = reader.readLine();
						System.out.print(">> Title : ");
						title = reader.readLine();
						System.out.print(">> Context File : ");
						context = reader.readLine();
						System.out.print(">> Attachment File : ");
						attach = reader.readLine();
						System.out.print(">> Should be send mail OK? Yes<Y> or No<N> : ");
						String answer = reader.readLine();
						if ((answer.charAt(0) == 'Y') || (answer.charAt(0) == 'y')){
							smtp instSmtp = new smtp();
							instSmtp.sendmail(host, send_port, send_type, username, password, from, to, title, context, attach);
						}
					}
					else if ((line.charAt(0) == 'T') || (line.charAt(0) == 't')){
						title = "test message";
						context = "context.txt";
						attach = "sample.pdf";
						smtp instSmtp = new smtp();
						instSmtp.sendmail(host, send_port, send_type, username, password, from, to, title, context, attach);
					}
					else if ((line.charAt(0) == 'S') || (line.charAt(0) == 's')){
						title = "test message";
						context = "context.txt";
						attach = "sample.pdf";
						boolean bSelect = true;
						while (bSelect) {
							if (cert_type.compareToIgnoreCase("rsa")== 0) {
								System.out.println("Signed Algorithm : [RSA]");
								System.out.println("[1] SHA1 on RSA");
								System.out.println("[2] SHA256 on RSA");
								System.out.println("[3] SHA384 on RSA");
								System.out.println("[4] SHA512 on RSA");
							}
							else {
								System.out.println("Signed Algorithm : [ECDSA]");
								System.out.println("[1] SHA1 on ECDSA");
								System.out.println("[2] SHA256 on ECDSA");
								System.out.println("[3] SHA384 on ECDSA");
								System.out.println("[4] SHA512 on ECDSA");
							}
							System.out.print("$> ");
							String answer = reader.readLine();
							int number = Integer.parseInt(answer);
							if (number == 1) {
								if (cert_type.compareToIgnoreCase("rsa")== 0) {
									algorithmSign = "SHA1withRSA";
								}
								else {
									algorithmSign = "SHA1withECDSA";
								}
								bSelect = false;
							}
							else if (number == 2) {
								if (cert_type.compareToIgnoreCase("rsa")== 0) {
									algorithmSign = "SHA256withRSA";
								}
								else {
									algorithmSign = "SHA256withECDSA";
								}
								bSelect = false;
							}
							else if (number == 3) {
								if (cert_type.compareToIgnoreCase("rsa")== 0) {
									algorithmSign = "SHA384withRSA";
								}
								else {
									algorithmSign = "SHA384withECDSA";
								}
								bSelect = false;
							}
							else if (number == 4) {
								if (cert_type.compareToIgnoreCase("rsa")== 0) {
									algorithmSign = "SHA512withRSA";
								}
								else {
									algorithmSign = "SHA512withECDSA";
								}
								bSelect = false;
							}
							else {
								System.out.println("Select Number is not correct!");
							}
						}
						sign instSign = new sign();
						instSign.doSign(cert,  cert_pass,  host,  send_port, send_type, from,  to,  username,  password,  title,  context,  attach,  algorithmSign);
					}
					else if ((line.charAt(0) == 'E') || (line.charAt(0) == 'e')){
						title = "test message";
						context = "context.txt";
						attach = "sample.pdf";
						boolean bSelect = true;
						while (bSelect) {
							System.out.println("Encryption Algorithm : ");
							System.out.println("[1] RC2");
							System.out.println("[2] 3DES");
							System.out.println("[3] AES128");
							System.out.println("[4] AES192");
							System.out.println("[5] AES256");
							System.out.print("$> ");
							String answer = reader.readLine();
							int number = Integer.parseInt(answer);
							if (number == 1) {
								algorithmCryption = "RC2";
								bSelect = false;
							}
							else if (number == 2) {
								algorithmCryption = "3DES";
								bSelect = false;
							}
							else if (number == 3) {
								algorithmCryption = "AES128";
								bSelect = false;
							}
							else if (number == 4) {
								algorithmCryption = "AES192";
								bSelect = false;
							}
							else if (number == 5) {
								algorithmCryption = "AES256";
								bSelect = false;
							}
							else {
								System.out.println(">> Select Number is not correct!");
							}
						}
						if ((cert_encrypt_type != null) && (cert_encrypt_type.compareToIgnoreCase("ecc") == 0)) {
							ecies instEcies = new ecies();
							instEcies.doEncrypt(cert_encrypt, send_port, send_type, host, from, to, username, password, title, context, attach, algorithmCryption);
						}
						else {
							encrypt instEncrypt = new encrypt();
							instEncrypt.doEncrypt(cert_encrypt, send_port, send_type, host, from, to, username, password, title, context, attach, algorithmCryption);
						}
					}
					else if ((line.charAt(0) == 'A') || (line.charAt(0) == 'a')){
						title = "test message";
						context = "context.txt";
						attach = "sample.pdf";
						boolean bSelect = true;
						while (bSelect) {
							if (cert_type.compareToIgnoreCase("rsa")== 0) {
								System.out.println("Signed Algorithm : [RSA]");
								System.out.println("[1] SHA1 on RSA");
								System.out.println("[2] SHA256 on RSA");
								System.out.println("[3] SHA384 on RSA");
								System.out.println("[4] SHA512 on RSA");
							}
							else {
								System.out.println("Signed Algorithm : [ECDSA]");
								System.out.println("[1] SHA1 on ECDSA");
								System.out.println("[2] SHA256 on ECDSA");
								System.out.println("[3] SHA384 on ECDSA");
								System.out.println("[4] SHA512 on ECDSA");
							}
							System.out.print("$> ");
							String answer = reader.readLine();
							int number = Integer.parseInt(answer);
							if (number == 1) {
								if (cert_type.compareToIgnoreCase("rsa")== 0) {
									algorithmSign = "SHA1withRSA";
								}
								else {
									algorithmSign = "SHA1withECDSA";
								}
								bSelect = false;
							}
							else if (number == 2) {
								if (cert_type.compareToIgnoreCase("rsa")== 0) {
									algorithmSign = "SHA256withRSA";
								}
								else {
									algorithmSign = "SHA256withECDSA";
								}
								bSelect = false;
							}
							else if (number == 3) {
								if (cert_type.compareToIgnoreCase("rsa")== 0) {
									algorithmSign = "SHA384withRSA";
								}
								else {
									algorithmSign = "SHA384withECDSA";
								}
								bSelect = false;
							}
							else if (number == 4) {
								if (cert_type.compareToIgnoreCase("rsa")== 0) {
									algorithmSign = "SHA512withRSA";
								}
								else {
									algorithmSign = "SHA512withECDSA";
								}
								bSelect = false;
							}
							else {
								System.out.println(">> Select Number is not correct!");
							}
						}
						bSelect = true;
						while (bSelect) {
							System.out.println("Encryption Algorithm : ");
							System.out.println("[1] RC2");
							System.out.println("[2] 3DES");
							System.out.println("[3] AES128");
							System.out.println("[4] AES192");
							System.out.println("[5] AES256");
							System.out.print("$> ");
							String answer = reader.readLine();
							int number = Integer.parseInt(answer);
							if (number == 1) {
								algorithmCryption = "RC2";
								bSelect = false;
							}
							else if (number == 2) {
								algorithmCryption = "3DES";
								bSelect = false;
							}
							else if (number == 3) {
								algorithmCryption = "AES128";
								bSelect = false;
							}
							else if (number == 4) {
								algorithmCryption = "AES192";
								bSelect = false;
							}
							else if (number == 5) {
								algorithmCryption = "AES256";
								bSelect = false;
							}
							else {
								System.out.println(">> Select Number is not correct!");
							}
						}
						sign_encrypt instSignEncrypt = new sign_encrypt();
						instSignEncrypt.doSingEncrypt(cert, cert_pass, cert_encrypt, host, send_port, send_type, from, to, username, password, title, context, attach, algorithmSign, algorithmCryption);
					}
	     			else if ((line.charAt(0) == 'Q') || (line.charAt(0) == 'q')){
						System.out.println(">> smail could be quit.");
	     				bContinue = false;
	     			}
	     			else{
	     				System.out.println(">> Command is not correct!");
	     			}
	     		}
	      	}
		}
		catch (Exception e) {
         	e.printStackTrace();
      	}
	}

	/* (non-Java-doc)
	 * @see java.lang.Object#Object()
	 */
	public smail() {
		super();
	}

}
