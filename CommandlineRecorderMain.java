import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
public class CommandlineRecorderMain 
{
    public static void main(String[] args) throws Exception 
    {
        GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
        ScreenRecorder sr = new ScreenRecorder(gc);
        sr.start();
        Thread.sleep(5000);
        sr.stop();
    }
}
