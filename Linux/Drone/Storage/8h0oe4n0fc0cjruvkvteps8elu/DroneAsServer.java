import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.security.SecureRandom;
import java.math.BigInteger;

public class DroneAsServer {
	public static int totalClient = 10;
	public static ArrayList<String> accessKeys = new ArrayList<String>();
	public static String base = "./Storage/";
	
	public static void main(String args[]) {
		Socket s = null;
		ServerSocket ss = null;
		System.out.println("Server Listening......");
		try {
			readKeysAndCreateFolders();
			ss = new ServerSocket(4445); // can also use static final PORT_NUM	when define
			int clientId = 1; // it will be client ID
			while (true) {
				try {
					s = ss.accept();
					System.out.println("Connection established for client: " + clientId);
					BufferedReader is = new BufferedReader(new InputStreamReader(s.getInputStream()));
					String clientorDrone = is.readLine();
			
					if( clientorDrone.compareTo("ImClient") == 0) {
						String clientKey = is.readLine();
						if(accessKeys.contains(clientKey)){
							DroneServerThread st = new DroneServerThread(s, clientId,is,clientKey);
							st.start();
						}else{
							
						}
					}else{
						DroneThread dt = new DroneThread(s, clientId,is);
						dt.start();	
					}
					clientId = clientId + 1;
				} catch (Exception e) {
					e.printStackTrace();
					System.out.println("Connection Error");
					break;
				}
			}
			
			s.close();
			ss.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Server error");
		}
	}
	
	public void createKeys(){
		SecureRandom randomNumber = new SecureRandom();
		try{
			PrintWriter output = new PrintWriter("ServerKeys.txt", "UTF-8");
			for(int i = 0 ; i < totalClient-1; i++){
				String key = new BigInteger(130, randomNumber).toString(32);
				output.println(key);
			}
			output.print(new BigInteger(130, randomNumber).toString(32));
			output.close();
		}catch(Exception e){
			e.getMessage();
		}	
	}
	
	public static void readKeysAndCreateFolders() throws IOException{
		File keyfile = new File("./ServerKeys.txt");
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
}
class DroneThread extends Thread {
	
	int clientId = 1; ServerSocket dataStream = null; String line = null; BufferedReader is = null; PrintWriter os = null; Socket s = null; ObjectInputStream mapInputStream = null; BufferedOutputStream out = null; Socket dataPort = null;
	static String clientBase = "";
	public DroneThread(Socket s, int clientId, BufferedReader Is)
	{
		this.s = s;
		this.clientId = clientId;
		this.is = Is;
	}

	public void run() {

		try {
			//is = new BufferedReader(new InputStreamReader(s.getInputStream()));
			os = new PrintWriter(s.getOutputStream());
			// mapInputStream = new ObjectInputStream(s.getInputStream())
			int portNumber = s.getLocalPort() + clientId;
			this.dataStream = new ServerSocket(portNumber);
	
			os.println(portNumber);
			os.flush();
			
			dataPort = dataStream.accept();
			mapInputStream = new ObjectInputStream(dataPort.getInputStream());
			out = new BufferedOutputStream(dataPort.getOutputStream(), 8096);
			
			System.out.println("Data connection established for client: " + clientId);
			
			line = is.readLine();
			while (line.compareTo("QUIT") != 0) {

				if (line.compareTo("sync check") == 0) {

					syncCheck();

				} else if (line.compareTo("sync all") == 0) {
					clientBase = is.readLine();
					clientBase = DroneAsServer.base + clientBase;
					System.out.println(clientBase+"base");
					syncAll();

				} else if (line.substring(0, 4).compareTo("sync") == 0) {

					syncFile(line.substring(5));

				} else {
					// Do nothing
				}
				line = is.readLine();
			}
		} catch (IOException e) {
			line = this.getName();
			int number = Integer.parseInt(line.substring(line.indexOf('-') + 1)) + 1;
			System.out.println("IO Error/ Client " + number + " terminated abruptly");
		} catch (NullPointerException e) {
			line = this.getName();
			int number = Integer.parseInt(line.substring(line.indexOf('-') + 1)) + 1;
			System.out.println("Client " + number + " Closed");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		} finally {
			String clientString = " for client: " + clientId;
			try {
				System.out.println("Connection closing " + clientString);
				if (is != null) {
					is.close();
					System.out.println("Command Socket Input Stream closed" + clientString);
				}
				if (os != null) {
					os.close();
					System.out.println("Command Socket Out closed" + clientString);
				}
				if (out != null) {
					out.close();
					System.out.println("FileOutputStrem Closed" + clientString);
				}
				if (mapInputStream != null) {
					out.close();
					System.out.println("MapInputStream closed" + clientString);
				}
				if (dataPort != null) {
					dataPort.close();
					System.out.println("Dataport Socket closed" + clientString);
				}
				if (dataStream != null) {
					dataStream.close();
					System.out.println("DataStream Server Socket closed" + clientString);
				}
				if (s != null) {
					s.close();
					System.out.println("Command Socket Closed");
				}

			} catch (IOException ie) {
				System.out.println("Socket Close Error");
			}
		} // end finally
	}

	private void syncFile(String filename) throws Exception {

		File folder = new File(clientBase);
		File[] listOfFiles = folder.listFiles();
		File ourFile = null;
		boolean isFileExists = false;

		for (int i = 0; i < listOfFiles.length; i++) {
			if (listOfFiles[i].getName().compareTo(filename) == 0) {
				isFileExists = true;
				ourFile = listOfFiles[i];
				break;
			}
		}

		if (isFileExists) {

			os.println("200");
			os.flush();
			String hash = "";
			hash = is.readLine();
			if (hash.compareTo(getHash(filename)) == 0) {
				os.println("OK");
				os.flush();
			} else {
				os.println(filename + " " + getAsString(ourFile.length()) + " sending");
				os.flush();
				os.println("update");
				os.flush();
				os.println(ourFile.length());
				os.flush();
				sendFile(ourFile);
			}

		} else {
			os.println(404);
			os.flush();
			System.out.println("Response to Client  :  " + "404");
		}

	}

	private boolean sendFile(File thisFile) throws IOException, ClassNotFoundException {
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

	private void syncAll() throws Exception {
		
		final Map<String, String> yourMap = (Map) mapInputStream.readObject();
		// System.out.println(yourMap.get("Presentation1.pptx"));
		HashMap<String, String> serverFiles = hashAllFiles();
		long updateSize = 0;
		if (yourMap.size() != 0) {
			for (String key : yourMap.keySet()) {

				if (serverFiles.get(key) != null) {

					if (serverFiles.get(key).compareTo(yourMap.get(key)) != 0) {

						os.println(key + " update " + getAsString(getFile(key).length()));
						os.flush();
						os.println(key);
						os.flush();
						os.println("update");
						os.flush();
						os.println(getFile(key).length());
						os.flush();
						sendFile(getFile(key));
						updateSize += getFile(key).length();
					} else {

						// do nothing
					}
				} else {
					os.println(key + " delete " + getAsString(getFile(key).length()));
					os.flush();
					os.println(key);
					os.flush();
					os.println("delete");
					os.flush();
					updateSize += +getFile(key).length();
				}
				serverFiles.remove(key);
			}
			for (String key : serverFiles.keySet()) {
				os.println(key + " add " + getAsString(getFile(key).length()));
				os.flush();
				os.println(key);
				os.flush();
				os.println("add");
				os.flush();
				os.println(getFile(key).length());
				os.flush();
				sendFile(getFile(key));
				updateSize += +getFile(key).length();
			}
		} else {
			if (serverFiles.size() != 0) {
				for (String key : serverFiles.keySet()) {
					os.println(key + " add " + getAsString(getFile(key).length()));
					os.flush();
					os.println(key);
					os.flush();
					os.println("add");
					os.flush();
					os.println(getFile(key).length());
					os.flush();
					sendFile(getFile(key));
					updateSize += +getFile(key).length();
				}
			} else {

			}
		}
		os.println("finished");
		os.flush();

		os.println(getAsString(updateSize));
		os.flush();

	}

	public File getFile(String filename) {
		File file = new File(clientBase + filename);
		return file;
	}

	private void syncCheck() throws Exception {

		final Map<String, String> yourMap = (Map) mapInputStream.readObject();

		// System.out.println(yourMap.get("Presentation1.pptx"));
		HashMap<String, String> serverFiles = hashAllFiles();
		long updateSize = 0;
		if (yourMap.size() != 0) {

			for (String key : yourMap.keySet()) {
				if (serverFiles.get(key) != null) {

					if (serverFiles.get(key).compareTo(yourMap.get(key)) != 0) {

						os.println(key + " update " + getAsString(getFile(key).length()));
						os.flush();
						updateSize += getFile(key).length();
					} else {

						// do nothing
					}
				} else {

					os.println(key + " delete " + getAsString(getFile(key).length()));
					os.flush();
					updateSize += +getFile(key).length();
				}
				serverFiles.remove(key);
			}
			for (String key : serverFiles.keySet()) {
				os.println(key + " add " + getAsString(getFile(key).length()));
				os.flush();
				updateSize += +getFile(key).length();
			}
		} else {
			if (serverFiles.size() != 0) {
				for (String key : serverFiles.keySet()) {
					os.println(key + " add " + getAsString(getFile(key).length()));
					os.flush();
					updateSize += +getFile(key).length();
				}
			} else {
				// do nothing
			}
		}
		os.println("finished");
		os.flush();
		os.println(getAsString(updateSize));
		os.flush();
	}

	public static HashMap<String, String> hashAllFiles() throws Exception {
		File folder = new File(clientBase);
		File[] listOfFiles = folder.listFiles();
		HashMap ServerFiles = new HashMap<String, byte[]>();
		String path = "";
		String digest = "";
		for (int i = 0; i < listOfFiles.length; i++) {
			path = clientBase + listOfFiles[i].getName();

			try {
				digest = checkSum(path);
				
			} catch (Exception e) {
				System.out.println("Error");
			}

			// System.out.println(digest);
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

	public String getAsString(long bytes) {
		for (int i = 6; i >= 0; i--) {
			double step = Math.pow(1024, i);

			if (bytes > step) {
				return String.format("%3.1f %s", bytes / step, Q[i]);

			}

		}

		return Long.toString(bytes);
	}

}
class DroneServerThread extends Thread {
	
	int clientId = 1; ServerSocket dataStream = null; String line = null; BufferedReader is = null; PrintWriter os = null; Socket s = null; ObjectInputStream mapInputStream = null; BufferedOutputStream out = null; Socket dataPort = null;
	String clientKey;static String clientBase;
	
	public DroneServerThread(Socket s, int clientId, BufferedReader Is, String KEY)
	{
		this.s = s;
		this.clientId = clientId;
		this.is = Is;
		this.clientKey = KEY;
		this.clientBase = DroneAsServer.base + this.clientKey + "/";
	}

	public void run() {

		try {
			//is = new BufferedReader(new InputStreamReader(s.getInputStream()));
			os = new PrintWriter(s.getOutputStream());
			// mapInputStream = new ObjectInputStream(s.getInputStream())
			int portNumber = s.getLocalPort() + clientId;
			this.dataStream = new ServerSocket(portNumber);
	
			os.println(portNumber);
			os.flush();
			
			dataPort = dataStream.accept();
			mapInputStream = new ObjectInputStream(dataPort.getInputStream());
			out = new BufferedOutputStream(dataPort.getOutputStream(), 8096);
			
			System.out.println("Data connection established for client: " + clientId);
			
			line = is.readLine();
			while (line.compareTo("QUIT") != 0) {

				if (line.compareTo("sync check") == 0) {

					syncCheck();

				} else if (line.compareTo("sync all") == 0) {

					syncAll();

				} else if (line.substring(0, 4).compareTo("sync") == 0) {

					syncFile(line.substring(5));

				} else {
					// Do nothing
				}
				line = is.readLine();
			}
		} catch (IOException e) {
			line = this.getName();
			int number = Integer.parseInt(line.substring(line.indexOf('-') + 1)) + 1;
			System.out.println("IO Error/ Client " + number + " terminated abruptly");
		} catch (NullPointerException e) {
			line = this.getName();
			int number = Integer.parseInt(line.substring(line.indexOf('-') + 1)) + 1;
			System.out.println("Client " + number + " Closed");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		} finally {
			String clientString = " for client: " + clientId;
			try {
				System.out.println("Connection closing " + clientString);
				if (is != null) {
					is.close();
					System.out.println("Command Socket Input Stream closed" + clientString);
				}
				if (os != null) {
					os.close();
					System.out.println("Command Socket Out closed" + clientString);
				}
				if (out != null) {
					out.close();
					System.out.println("FileOutputStrem Closed" + clientString);
				}
				if (mapInputStream != null) {
					out.close();
					System.out.println("MapInputStream closed" + clientString);
				}
				if (dataPort != null) {
					dataPort.close();
					System.out.println("Dataport Socket closed" + clientString);
				}
				if (dataStream != null) {
					dataStream.close();
					System.out.println("DataStream Server Socket closed" + clientString);
				}
				if (s != null) {
					s.close();
					System.out.println("Command Socket Closed");
				}

			} catch (IOException ie) {
				System.out.println("Socket Close Error");
			}
		} // end finally
	}

	private void syncFile(String filename) throws Exception {

		File folder = new File(clientBase);
		File[] listOfFiles = folder.listFiles();
		File ourFile = null;
		boolean isFileExists = false;

		for (int i = 0; i < listOfFiles.length; i++) {
			if (listOfFiles[i].getName().compareTo(filename) == 0) {
				isFileExists = true;
				ourFile = listOfFiles[i];
				break;
			}
		}

		if (isFileExists) {

			os.println("200");
			os.flush();
			String hash = "";
			hash = is.readLine();
			if (hash.compareTo(getHash(filename)) == 0) {
				os.println("OK");
				os.flush();
			} else {
				os.println(filename + " " + getAsString(ourFile.length()) + " sending");
				os.flush();
				os.println("update");
				os.flush();
				os.println(ourFile.length());
				os.flush();
				sendFile(ourFile);
			}

		} else {
			os.println(404);
			os.flush();
			System.out.println("Response to Client  :  " + "404");
		}

	}

	private boolean sendFile(File thisFile) throws IOException, ClassNotFoundException {
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

	private void syncAll() throws Exception {
		final Map<String, String> yourMap = (Map) mapInputStream.readObject();
		// System.out.println(yourMap.get("Presentation1.pptx"));
		HashMap<String, String> serverFiles = hashAllFiles();
		long updateSize = 0;
		if (yourMap.size() != 0) {
			for (String key : yourMap.keySet()) {

				if (serverFiles.get(key) != null) {

					if (serverFiles.get(key).compareTo(yourMap.get(key)) != 0) {

						os.println(key + " update " + getAsString(getFile(key).length()));
						os.flush();
						os.println(key);
						os.flush();
						os.println("update");
						os.flush();
						os.println(getFile(key).length());
						os.flush();
						sendFile(getFile(key));
						updateSize += getFile(key).length();
					} else {

						// do nothing
					}
				} else {
					os.println(key + " delete " + getAsString(getFile(key).length()));
					os.flush();
					os.println(key);
					os.flush();
					os.println("delete");
					os.flush();
					updateSize += +getFile(key).length();
				}
				serverFiles.remove(key);
			}
			for (String key : serverFiles.keySet()) {
				os.println(key + " add " + getAsString(getFile(key).length()));
				os.flush();
				os.println(key);
				os.flush();
				os.println("add");
				os.flush();
				os.println(getFile(key).length());
				os.flush();
				sendFile(getFile(key));
				updateSize += +getFile(key).length();
			}
		} else {
			if (serverFiles.size() != 0) {
				for (String key : serverFiles.keySet()) {
					os.println(key + " add " + getAsString(getFile(key).length()));
					os.flush();
					os.println(key);
					os.flush();
					os.println("add");
					os.flush();
					os.println(getFile(key).length());
					os.flush();
					sendFile(getFile(key));
					updateSize += +getFile(key).length();
				}
			} else {

			}
		}
		os.println("finished");
		os.flush();

		os.println(getAsString(updateSize));
		os.flush();

	}

	public File getFile(String filename) {
		File file = new File(clientBase + filename);
		return file;
	}

	private void syncCheck() throws Exception {

		final Map<String, String> yourMap = (Map) mapInputStream.readObject();

		// System.out.println(yourMap.get("Presentation1.pptx"));
		HashMap<String, String> serverFiles = hashAllFiles();
		long updateSize = 0;
		if (yourMap.size() != 0) {

			for (String key : yourMap.keySet()) {
				if (serverFiles.get(key) != null) {

					if (serverFiles.get(key).compareTo(yourMap.get(key)) != 0) {

						os.println(key + " update " + getAsString(getFile(key).length()));
						os.flush();
						updateSize += getFile(key).length();
					} else {

						// do nothing
					}
				} else {

					os.println(key + " delete " + getAsString(getFile(key).length()));
					os.flush();
					updateSize += +getFile(key).length();
				}
				serverFiles.remove(key);
			}
			for (String key : serverFiles.keySet()) {
				os.println(key + " add " + getAsString(getFile(key).length()));
				os.flush();
				updateSize += +getFile(key).length();
			}
		} else {
			if (serverFiles.size() != 0) {
				for (String key : serverFiles.keySet()) {
					os.println(key + " add " + getAsString(getFile(key).length()));
					os.flush();
					updateSize += +getFile(key).length();
				}
			} else {
				// do nothing
			}
		}
		os.println("finished");
		os.flush();
		os.println(getAsString(updateSize));
		os.flush();
	}

	public static HashMap<String, String> hashAllFiles() throws Exception {
		File folder = new File(clientBase);
		File[] listOfFiles = folder.listFiles();
		HashMap ServerFiles = new HashMap<String, byte[]>();
		String path = "";
		String digest = "";
		for (int i = 0; i < listOfFiles.length; i++) {
			path = clientBase + listOfFiles[i].getName();

			try {
				digest = checkSum(path);
			} catch (Exception e) {
				System.out.println("Error");
			}

			// System.out.println(digest);
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

	public String getAsString(long bytes) {
		for (int i = 6; i >= 0; i--) {
			double step = Math.pow(1024, i);

			if (bytes > step) {
				return String.format("%3.1f %s", bytes / step, Q[i]);

			}

		}

		return Long.toString(bytes);
	}

}