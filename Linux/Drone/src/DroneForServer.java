// Drone
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
/*import java.security.SecureRandom;
import java.math.BigInteger;*/
public class DroneForServer{
	

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
	private static BufferedOutputStream out;
	private static int dataPortNumber; // port number for data socket
	
	public static ArrayList<String> accessKeys;
	public static String base = "../Storage/";
	public static String clientBase;
	
	public static void main(String args[]) throws Exception {
		 
		// initialization of variables
		 address= InetAddress.getByName("172.20.10.2"); // for local use server ip is client ip	
		 //address = InetAddress.getLocalHost();
		 s = null; dataSocket = null; line = null; br = null; is = null; os = null; mapOutputStream = null; in2= null;
		 out = null;
		 accessKeys =new ArrayList<String>();
		// connecting server socket
		while(true){
			try {	
				
				readKeysAndCreateFolders();
				s = new Socket(address, 4445);
				os = new PrintWriter(s.getOutputStream());
			    os.println("ImDrone");
				os.flush();
				// You can use static final constant
				br = new BufferedReader(new InputStreamReader(System.in));
				is = new BufferedReader(new InputStreamReader(s.getInputStream()));
			
	         
				// for open datasocket get new port number from server
				String number = is.readLine();
				dataPortNumber = Integer.parseInt(number);
			
				// connection data socket
				dataSocket = new Socket(address, dataPortNumber);
				mapOutputStream = new ObjectOutputStream(dataSocket.getOutputStream());
				in2 = new BufferedInputStream(dataSocket.getInputStream(), 8096);
				out = new BufferedOutputStream(dataSocket.getOutputStream(),8096);
				// info
				System.out.println("Client Address : " + address);
				System.out.println("Enter Data to echo Server ( Enter QUIT to end):");
				for(int i = 0; i < accessKeys.size() ; i++){
					clientBase = base + accessKeys.get(i)+"/";
					syncAll(accessKeys.get(i)+"/");
				}
			
				break;
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("Socket read Error");
			} 
		}
	
			is.close();
			os.close();
			br.close();
			mapOutputStream.close();
			in2.close();
			dataSocket.close();
			s.close();
			System.out.println("Connection Closed");
		
		
		

	}

	public static void readKeysAndCreateFolders() throws IOException{
		File keyfile = new File("../ServerKeys.txt");
		Scanner sc = new Scanner(keyfile);
	    while (sc.hasNextLine()) {
	            accessKeys.add(sc.next());        
	    }
	    sc.close();
	    for(int i = 0 ; i < accessKeys.size(); i++){
	    	 File theDir = new File(base + accessKeys.get(i));
	    	 if (!theDir.exists()){  // Checks that Directory/Folder Doesn't Exists!  
		         boolean result = theDir.mkdir();    
		         if(result){  
		        	System.out.println("FolderCreated"); 
		         }
		      }  

	    }
	    
	     // Defining Directory/Folder Name  

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
	private static boolean sendFile(File thisFile) throws IOException, ClassNotFoundException {
		FileInputStream in = new FileInputStream(thisFile);
		byte[] bytes = new byte[8096];
		int count;
		while ((count = in.read(bytes)) > 0) {
			out.write(bytes, 0, count);
			out.flush();
		}
		// out.close();
		line = is.readLine();
		in.close();

		if (line.compareTo("done") == 0) {
			return true;
		} else {
			return false;
		}

		// System.out.println("Bytes Sent :" + bytecount);
	}
	
	public static void syncAll(String clientbase) throws Exception {
		String line = "";

		String fileName = "";
		String operation = "";
		long fileSize = 0;
		os.println("sync all");
		os.flush();
	    os.println(clientbase);
	    os.flush();
		System.out.println("sync starting");
		mapOutputStream.writeObject(hashAllFiles());
		mapOutputStream.flush();
		mapOutputStream.writeObject(lastEdits());
		mapOutputStream.flush();
		System.out.println("Im here");
		line = is.readLine();
		if (line.compareTo("finished") != 0) {
			fileName = is.readLine();
			operation = is.readLine();
			if (operation.compareTo("delete") != 0 && operation.compareTo("upload")!=0)
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
				} else if (operation.compareTo("upload")==0){
					os.println(getFile(fileName).length());
					os.flush();
					sendFile(getFile(fileName));
					System.out.println(fileName + " uploaded to server" + getAsString(getFile(fileName).length()));
				}else if (operation.compareTo("delete") == 0) {
					getFile(fileName).setWritable(true);
					getFile(fileName).delete();
					System.out.println(fileName + " deleted");
				} else if (operation.compareTo("add") == 0) {
					File thisfile = new File(base + clientbase + fileName);

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
					if (operation.compareTo("delete") != 0 && operation.compareTo("upload")!=0)
						fileSize = Long.valueOf(is.readLine()).longValue();
				}
			}
		}
		line = is.readLine();
		System.out.println("The total size of updates is " + line);
		System.out.println("Syncing finished");
	}
	
	public static HashMap<String,Long> lastEdits(){
		File folder = new File(clientBase);
		File[] listOfFiles = folder.listFiles();
		HashMap ClientFiles = new HashMap<String, String>();
		for (int i = 0; i < listOfFiles.length; i++) {
			ClientFiles.put(listOfFiles[i].getName(), listOfFiles[i].lastModified());
		}
		return ClientFiles;
	}

//	public static void syncAll(String clientbase) throws Exception {
//		String line = "";
//
//		String fileName = "";
//		String operation = "";
//		long fileSize = 0;
//		os.println("sync all");
//		os.flush();
//		os.println(clientbase);
//		os.flush();
//		System.out.println("sync starting");
//		mapOutputStream.writeObject(hashAllFiles());
//		mapOutputStream.flush();
//		line = is.readLine();
//		if (line.compareTo("finished") != 0) {
//			fileName = is.readLine();
//			operation = is.readLine();
//			if (operation.compareTo("delete") != 0)
//				fileSize = Long.valueOf(is.readLine()).longValue();
//
//			while (line.compareTo("finished") != 0) {
//				System.out.println(line);
//				if (operation.compareTo("update") == 0) {
//					FileOutputStream inFile = new FileOutputStream(getFile(fileName));
//					byte[] bytes = new byte[8096];
//					int count;
//					while (fileSize > 0 && (count = in2.read(bytes)) > 0) {
//						inFile.write(bytes, 0, count);
//						fileSize -= count;
//						inFile.flush();
//					}
//					inFile.close();
//					os.println("done");
//					os.flush();
//					System.out.println(fileName + " updated");
//				} else if (operation.compareTo("delete") == 0) {
//					getFile(fileName).setWritable(true);
//					getFile(fileName).delete();
//					System.out.println(fileName + " deleted");
//				} else if (operation.compareTo("add") == 0) {
//					File thisfile = new File(clientBase + fileName);
//
//					FileOutputStream inFile = new FileOutputStream(thisfile);
//					byte[] bytes = new byte[8096];
//					int count;
//					while (fileSize > 0 && (count = in2.read(bytes)) > 0) {
//						inFile.write(bytes, 0, count);
//						fileSize -= count;
//						inFile.flush();
//					}
//					inFile.close();
//					os.println("done");
//					os.flush();
//					System.out.println(fileName + " added");
//
//				} else {
//					System.out.print("Very interesting");
//				}
//
//				line = is.readLine();
//				if (line.compareTo("finished") != 0) {
//					fileName = is.readLine();
//					operation = is.readLine();
//					if (operation.compareTo("delete") != 0)
//						fileSize = Long.valueOf(is.readLine()).longValue();
//				}
//			}
//		}
//		line = is.readLine();
//		System.out.println("The total size of updates is " + line);
//		System.out.println("Syncing finished");
//	}
	public static boolean isFileExists(String filename) {

		File folder = new File(clientBase);
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

	
	public static void createFolders() throws IOException{
		for(int i = 0 ; i < accessKeys.size() ; i ++) {
		File theDir = new File(base +  accessKeys.get(i));  // Defining Directory/Folder Name  
		try{   
		    if (!theDir.exists()){  // Checks that Directory/Folder Doesn't Exists!  
		     boolean result = theDir.mkdir();    
		     	if(result){  
		     		System.out.println("Folder created");
		     	}  
		    }  
		}catch(Exception e){  
		   e.printStackTrace();
		} 
		}
	}
	
	



	public static String getHash(String filename) throws Exception {

		String path = clientBase + filename;
		String digest = "";
		try {
			digest = checkSum(path);

		} catch (Exception e) {
			System.out.println(e);
		}

		return digest;
	}

	
	public static File getFile(String filename) {
		File file = new File(clientBase + filename);
		return file;
	}
	
	private boolean downloadAFile (File output,long fileSize) throws IOException{
		FileOutputStream inFile = new FileOutputStream(output);
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
		return true;
	}

	

	public static HashMap<String, String> hashAllFiles() throws Exception {
		File folder = new File(clientBase);
		File[] listOfFiles = folder.listFiles();
		HashMap<String, String> ServerFiles = new HashMap<String,String>();
		String path = "";
		String digest = "";
		for (int i = 0; i < listOfFiles.length; i++) {
			path = clientBase + listOfFiles[i].getName();			
			digest = checkSum(path);
			ServerFiles.put(listOfFiles[i].getName(), digest);
		}
		return ServerFiles;
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

	private static final String[] Q = new String[] { "Bytes", "Kb", "Mb", "Gb", "T", "P", "E" };

	public static String getAsString(long bytes) {
		for (int i = 6; i >= 0; i--) {
			double step = Math.pow(1024, i);

			if (bytes > step) {
				return String.format("%3.1f %s", bytes / step, Q[i]);

			}

		}

		return Long.toString(bytes);
	}
}


