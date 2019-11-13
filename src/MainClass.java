import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.StringTokenizer;

/**
 * A Tiny HTTP Server to get a requested file specified in the address bar of a browser
 * 
 * @author Tommaso Macchioni
 *
 */

public class MainClass {

	static final int PORT = 8080;
	static final int WAIT_TIME = 15000;

	public static void main(String args[]){ 

		ServerSocket server = null;

		try {

			server= new ServerSocket(PORT);
			
			/*After WAIT_TIME milliseconds the accept method will throw a SocketTimeoutException 
			* and will close the server.
			*/
			server.setSoTimeout(WAIT_TIME);

			System.out.println("Server started.\nListening for connections on port : " + PORT + " ...\n");


			Socket connect = null;
			BufferedReader in = null;
			DataOutputStream out = null;

			while(true) {

				try {

					connect = server.accept();
					System.out.println("Connection opened.");

					in = new BufferedReader(
							new InputStreamReader(
									connect.getInputStream()));

					out = new DataOutputStream(
							new BufferedOutputStream(
									connect.getOutputStream()));

					String line = in.readLine();

					StringTokenizer lineTokens = new StringTokenizer(line);

					lineTokens.nextToken();
					String pathFile = lineTokens.nextToken();


					//Replace the %20 that the browser sets instead of the space character
					pathFile = pathFile.replaceAll("%20", " ");

					//Refuse any favicon the browser requests
					if(pathFile.equals("/favicon.ico")) {
						System.out.println("Request of favicon refused");
						connect.close();
						in.close();
						out.close();
						continue;
					}

					//Current directory
					String cwd = new File("").getAbsolutePath();

					File myFile = new File(cwd + pathFile);
					System.out.println("Requested file: "+ myFile.getName());

					
					FileInputStream fis = new FileInputStream(myFile);

					FileChannel fc = fis.getChannel();
					ByteBuffer bb = ByteBuffer.allocate( (int) myFile.length());

					//Write bytes of file in the buffer
					fc.read(bb);

					bb.flip();

					//Write in the out socket stream and flush it
					out.writeBytes("HTTP/1.1 200 OK\r\n\r\n");
					out.write(bb.array());

					out.flush();

					System.out.println("'200 OK' response sent");

					//Close
					fis.close();
					fc.close();
					connect.close();
					in.close();
					out.close();
					
				} catch(NullPointerException ex) {
					continue;
				} catch(FileNotFoundException ex) {
					System.out.println("FileNotFoundException - '404 Not Found' response sent");
					out.writeBytes("HTTP/1.1 404 Not Found\r\n\r\n<h1>404 Not Found</h1>");
					out.flush();
					continue;
				} catch(SocketTimeoutException ex) {
					System.out.println("Closure for inactivity");
					break;
				}catch(IOException ex) {
					System.out.println("IOException - '500 Internal Server Error' response sent");
					out.writeBytes("HTTP/1.1 500 Internal Server Error\r\n\r\n<h1>500 Internal Server Error</h1>");
					out.flush();
					continue;
				} finally {
					if(connect != null) connect.close();
					if(in != null) in.close();
					if(out != null) out.close();
					System.out.println("Connection closed.\n");
				}
			}

		} catch (IOException e) {
			System.err.println("Server Connection error : " + e.getMessage());
		} finally {

			try {
				if(server != null) server.close();
				System.out.println("Server terminated.");
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}

		}

		return;

	}

}
