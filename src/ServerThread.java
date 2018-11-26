import java.io.*;
import java.net.*;
import java.util.Random;

public class ServerThread extends Thread {

    ServerSocket ss;
    public ServerThread(int _port) throws Exception {
        ss = new ServerSocket(_port);
    }

    public void run() {
        boolean running = true;
        try {
            while(running)
            {
                //wait for connection then accept
                Socket s = ss.accept();

                //get the possible input files
                File folder = new File("./_InputFiles");
                File[] files = folder.listFiles();

                //select one at random
                File selected = null;
                Random r = new Random();
                while(selected == null) {
                    selected = files[r.nextInt(files.length)];
                    //if the selected is not file (directory), try again
                    if(!selected.isFile())
                        selected = null;
                }

                //get metadata of the file selected and send that in first 256 bytes
                String metadata = selected.getName() + "," + selected.getName().substring(selected.getName().lastIndexOf(".")) + "," + selected.length() + ",";//name, extension, size (bytes)
                byte[] meta_bytes = metadata.getBytes();
                byte[] push_bytes = new byte[256];
                //copy the metadata to the first part of the push_bytes
                for(int i = 0; i < meta_bytes.length; i++) {
                    push_bytes[i] = meta_bytes[i];
                }
                OutputStream os = s.getOutputStream();
                os.write(push_bytes, 0, 256);

                //write the file
                FileInputStream fr = new FileInputStream(selected);
                byte[] data_buf = new byte[1024];
                int read_status = fr.read(data_buf);
                while(read_status != -1) {
                    os.write(data_buf);
                    data_buf = new byte[1024];
                    read_status = fr.read(data_buf);
                }

                //close streams and sockets
                fr.close();
                os.close();
                s.close();
            }
            ss.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}