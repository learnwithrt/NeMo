
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
/**
 *
 * @author Rahul Talreja
 */
public class CheckStatus 
{
    public static void main(String args[])
    {
        Socket socket=new Socket();
        while (true)
	{
            try
            {
                    socket = new Socket("192.168.129.2", 5010);
                    if (socket.isBound())
                    {
                    	break;
                    }
            }
            catch (ConnectException se)
            {
                try 
                {
                    socket.setSoTimeout(100000);
                } 
                catch (SocketException ex) 
                {
                    Logger.getLogger(CheckStatus.class.getName()).log(Level.SEVERE, null, ex);
                }
                continue;
            }
            catch(IOException ie)
            {
                
            }
            
	}
	BufferedReader inFromServer; 
        String data="";
        try 
        {
            inFromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            data = inFromServer.readLine();
        } 
        catch (IOException ex) 
        {
            Logger.getLogger(CheckStatus.class.getName()).log(Level.SEVERE, null, ex);
        }
	
	if(data.equals("Shutdown"))
        {
						 	 	Runtime r = Runtime.getRuntime();
                                                                
            Process p = null;
            try {
                p = r.exec("check.bat");
            } catch (IOException ex) {
                Logger.getLogger(CheckStatus.class.getName()).log(Level.SEVERE, null, ex);
            }
           
        }
        try {
            socket.close();
        } catch (IOException ex) {
            Logger.getLogger(CheckStatus.class.getName()).log(Level.SEVERE, null, ex);
        }	
						  		}
        
    }
