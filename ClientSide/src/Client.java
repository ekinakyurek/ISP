import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.security.MessageDigest;
import java.util.HashMap;

public class Client {

	private static InetAddress address; // IP adres of Server
	private static Socket s; // tcp socket to connect Server
	private static BufferedReader br; // input stream for reading system inputs(console
								// inputs)
	private static BufferedReader is; // input stream for socket
	private static PrintWriter os; // output stream to socket
	private static String line; // for command strings that are we read from either
							// socket stream or system input.
	private static ObjectOutputStream mapOutputStream;  // outputs stream for hash files, when comparing files status 
	private static 	BufferedInputStream in2;  // input stream for files
	private static Socket dataSocket; // new socket for files
	
	private static int dataPortNumber; // port number for data socket
	
	public static void main(String args[]) throws Exception {
		 
		// initialization of variables
		 address= InetAddress.getLocalHost(); // for local use server ip is client ip								
		 s = null; dataSocket = null; line = null; br = null; is = null; os = null; mapOutputStream = null; in2= null; 
		// connecting server socket
		try {	
			s = new Socket(address, 4445); // You can use static final constant
			br = new BufferedReader(new InputStreamReader(System.in));
			is = new BufferedReader(new InputStreamReader(s.getInputStream()));
			os = new PrintWriter(s.getOutputStream());

			// for open datasocket get new port number from server
			String number = is.readLine();
			dataPortNumber = Integer.parseInt(number);

			// connection data socket
			dataSocket = new Socket(address, dataPortNumber);
			mapOutputStream = new ObjectOutputStream(dataSocket.getOutputStream());
			in2 = new BufferedInputStream(dataSocket.getInputStream(), 8096);

			// info
			System.out.println("Client Address : " + address);
			System.out.println("Enter Data to echo Server ( Enter QUIT to end):");

			line = br.readLine();
			while (line.compareTo("QUIT") != 0) {
				if (line.compareTo("sync check") == 0) {			
					syncCheck();		
				
				} else if (line.compareTo("sync all") == 0) {	
					syncAll();
				
				} else if (line.length() > 5 && line.substring(0, 4).compareTo("sync") == 0) {				
					String fileName = line.substring(line.indexOf(" ") + 1, line.length());
					
					syncFile(fileName);
				} else {
					
					System.out.println("Please enter a valid command");
				}
				line = br.readLine();
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Socket read Error");
		} finally {
			is.close();
			os.close();
			br.close();
			mapOutputStream.close();
			in2.close();
			dataSocket.close();
			s.close();
			System.out.println("Connection Closed");
		}

	}

	public static void syncFile(String fileName)
			throws Exception {
	
		String response = "";
		long fileSize = 0;

		if (isFileExists(fileName)) {
			System.out.println("Start Syncing");
			os.println("sync " + fileName);
			os.flush();
			response = is.readLine();

			if (response.compareTo("200") == 0) {
				os.println(getHash(fileName));
				os.flush();
				response = is.readLine();
				if (response.compareTo("OK") == 0) {
					System.out.println(fileName + " has already updated");
				} else {
					System.out.println(response);

					is.readLine(); // for operation
					fileSize = Long.valueOf(is.readLine()).longValue();
					FileOutputStream inFile = new FileOutputStream(getFile(fileName));

					byte[] bytes = new byte[8096];
					int count;
					while (fileSize > 0 && (count = in2.read(bytes)) > 0) {
						inFile.write(bytes, 0, count);
						fileSize -= count;
						inFile.flush();
					}

					inFile.close();
					os.println("done");
					os.flush();
					System.out.println(fileName + " updated");
					
				}
			} else {
				File thisFile = getFile(fileName);
				System.out.println("deleting " + fileName + " " + thisFile.getTotalSpace());
				if (thisFile.delete())
					System.out.println(fileName + " deleted succesfully");
				else
					System.out.println("Encountered a problem");
			}

		} else {
			System.out.println("Please make a sync request for existing file ");
		}

	}

	public static void syncCheck()
			throws Exception {
		String line = "";

		os.println("sync check");
		os.flush();
		System.out.println("You have to update your storage with the following files:");
		mapOutputStream.writeObject(hashAllFiles());
		mapOutputStream.flush();
		line = is.readLine();
		while (line.compareTo("finished") != 0) {
			System.out.println(line);
			line = is.readLine();
		}
		line = is.readLine();
		System.out.println("The total size of updates is " + line);
		System.out.println("Sync check finished");

	}

	public static void syncAll() throws Exception {
		String line = "";

		String fileName = "";
		String operation = "";
		long fileSize = 0;
		os.println("sync all");
		os.flush();
		System.out.println("sync starting");
		mapOutputStream.writeObject(hashAllFiles());
		mapOutputStream.flush();
		line = is.readLine();
		if (line.compareTo("finished") != 0) {
			fileName = is.readLine();
			operation = is.readLine();
			if (operation.compareTo("delete") != 0)
				fileSize = Long.valueOf(is.readLine()).longValue();

			while (line.compareTo("finished") != 0) {
				System.out.println(line);
				if (operation.compareTo("update") == 0) {
					FileOutputStream inFile = new FileOutputStream(getFile(fileName));
					byte[] bytes = new byte[8096];
					int count;
					while (fileSize > 0 && (count = in2.read(bytes)) > 0) {
						inFile.write(bytes, 0, count);
						fileSize -= count;
						inFile.flush();
					}
					inFile.close();
					os.println("done");
					os.flush();
					System.out.println(fileName + " updated");
				} else if (operation.compareTo("delete") == 0) {
					getFile(fileName).setWritable(true);
					getFile(fileName).delete();
					System.out.println(fileName + " deleted");
				} else if (operation.compareTo("add") == 0) {
					File thisfile = new File("./Local/" + fileName);

					FileOutputStream inFile = new FileOutputStream(thisfile);
					byte[] bytes = new byte[8096];
					int count;
					while (fileSize > 0 && (count = in2.read(bytes)) > 0) {
						inFile.write(bytes, 0, count);
						fileSize -= count;
						inFile.flush();
					}
					inFile.close();
					os.println("done");
					os.flush();
					System.out.println(fileName + " added");

				} else {
					System.out.print("Very interesting");
				}

				line = is.readLine();
				if (line.compareTo("finished") != 0) {
					fileName = is.readLine();
					operation = is.readLine();
					if (operation.compareTo("delete") != 0)
						fileSize = Long.valueOf(is.readLine()).longValue();
				}
			}
		}
		line = is.readLine();
		System.out.println("The total size of updates is " + line);
		System.out.println("Syncing finished");
	}

	public static boolean isFileExists(String filename) {

		File folder = new File("./Local");
		File[] listOfFiles = folder.listFiles();
		boolean isFileExists = false;
		for (int i = 0; i < listOfFiles.length; i++) {
			if (listOfFiles[i].getName().compareTo(filename) == 0) {
				isFileExists = true;
				break;
			}
		}
		return isFileExists;
	}

	public static File getFile(String filename) {
		File file = new File("./Local/" + filename);
		return file;
	}

	public static String getHash(String filename) throws Exception {

		String path = "./Local/" + filename;
		String digest = "";
		try {
			digest = checkSum(path);

		} catch (Exception e) {
			System.out.println(e);
		}

		return digest;
	}

	public static HashMap<String, String> hashAllFiles() throws Exception {
		File folder = new File("./Local");
		File[] listOfFiles = folder.listFiles();
		HashMap ClientFiles = new HashMap<String, byte[]>();
		String path = "";
		String digest = "";
		for (int i = 0; i < listOfFiles.length; i++) {
			path = "./Local/" + listOfFiles[i].getName();
			try {
				digest = checkSum(path);
				/*
				 * MessageDigest md = MessageDigest.getInstance("SHA1");
				 * FileInputStream is = new
				 * FileInputStream("./Local/"+listOfFiles[i].getName());
				 * DigestInputStream dis = new DigestInputStream(is, md);
				 * is.close(); dis.close(); digest= md.digest().toString();
				 */
			} catch (Exception e) {
				System.out.println("Error");
			}

			// System.out.println(digest);
			ClientFiles.put(listOfFiles[i].getName(), digest);
		}

		return ClientFiles;
	}

	public static String checkSum(String path) {
		String digest = "";
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			FileInputStream fis = new FileInputStream(path);

			byte[] dataBytes = new byte[1024];

			int nread = 0;
			while ((nread = fis.read(dataBytes)) != -1) {
				md.update(dataBytes, 0, nread);
			}
			byte[] mdbytes = md.digest();

			StringBuffer hexString = new StringBuffer();
			for (int i = 0; i < mdbytes.length; i++) {
				String hex = Integer.toHexString(0xff & mdbytes[i]);
				if (hex.length() == 1)
					hexString.append('0');
				hexString.append(hex);
			}
			digest = hexString.toString();
			fis.close();
		} catch (Exception e) {
			System.out.println(e);
		}

		return digest;

	}
}