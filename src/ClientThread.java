import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Scanner;
import java.io.*;
import java.net.*;

public class ClientThread extends Thread {
    private DatagramSocket socket;
    private byte[] buf = new byte[256];
    private int int_source_port;
    private InetAddress inet_router_address;
    //hold info about the statistics of the thread
    private String
            source_name,		//friendly name of this client
            source_ip,			//ip address of this client
            source_port,		//port of this client
            router_address,		//address of the router this client connects to
            destination_name,	//name of the destination we are trying to get to
            destination_ip,		//ip of destination
            destination_port,	//port of destination,
            file_name,			//name of file
            file_extension,		//type of file
            file_size;			//size (bytes) of file
    private long
            time_create,					//time thread created
            time_router_request_connect,	//time request connection to router
            time_router_response_connect,	//time the router returns with response we connected
            time_start_demo,				//time user starts demo (presses enter)
            time_return_lookup,				//time the lookup returns the address of the destination
            time_start_transfer,			//time we start the TCP transfer
            time_end_transfer;				//time we end the TCP transfer

    private byte[] in_buf;
    private DatagramPacket in_packet;

    public ClientThread(String _source_ip, String _source_name, int _source_port, String _router_address, InetAddress _inet_router_address, String _destination_name) throws Exception {
        int_source_port = _source_port;
        inet_router_address = _inet_router_address;

        source_ip = _source_ip;
        source_port = "" + _source_port;
        source_name = _source_name + _source_port;
        router_address = _router_address;
        destination_name = _destination_name + _source_port;

        socket = new DatagramSocket(int_source_port);
        socket.setSoTimeout(15000);	//set time out to 5 seconds because if anything takes longer, something happened

        time_create = System.nanoTime();

        in_buf = new byte[256];
        in_packet = new DatagramPacket(buf, buf.length);

        //System.out.println("Created socket for client " + source_name);

        //create a new server for this client
        ServerThread st = new ServerThread(_source_port + 5000);
        st.start();
    }

    public void run() {
        try {
            //connect to my router
            byte[] buf;
            String response;
            buf = ("CONNECT " + source_name).getBytes();
            DatagramPacket connectionpacket = new DatagramPacket(buf, buf.length, inet_router_address, SRouter.router_port_number);
            //tell the router we are connecting
            time_router_request_connect = System.nanoTime();
            socket.send(connectionpacket);

            //wait for response
            socket.receive(in_packet);
            time_router_response_connect = System.nanoTime();
            response = new String(in_packet.getData(), 0, in_packet.getLength());
            if(response.toUpperCase().contains("FAILED"))
            {
                throw new Exception("(" + source_name + "): Failed to connect to router... " + router_address);
            }

            //wait for user to press enter
            while(!ClientCluster.running) {
                sleep(100);
            }

            time_start_demo = System.nanoTime();

            //tell the router we are looking for destination
            buf = ("FIND " + destination_name).getBytes();
            DatagramPacket searchingpacket = new DatagramPacket(buf, buf.length, inet_router_address, SRouter.router_port_number);
            socket.send(searchingpacket);

            //wait for response
            socket.receive(in_packet);
            time_return_lookup = System.nanoTime();
            response = new String(in_packet.getData(), 0, in_packet.getLength());
            response = response.replace("FOUND", "").trim();
            if(response.toUpperCase().contains("FAIL")) {
                throw new Exception("(" + source_name + "): Failed to connect to destination... received response " + response);
            }

            //System.out.println("(" + source_name + "): Found " + destination_name + " at... " + response);
            //get the ip and the port number of the destination we found
            destination_ip = response.substring(1, response.indexOf(":"));
            destination_port = response.substring(response.indexOf(":") + 1);

            //create TCP connection to other client's server to receive the file
            Socket s = new Socket(destination_ip, Integer.parseInt(destination_port) + 5000);
            s.setSoTimeout(15000);
            InputStream is = s.getInputStream();

            time_start_transfer = System.nanoTime();

            //first get the metadata of the file we are pulling
            byte[] meta_buf = new byte[256];
            is.read(meta_buf, 0, meta_buf.length);
            String metadata = new String(meta_buf, 0, meta_buf.length);
            String[] metas = metadata.split(",");	//name, extension, size (bytes)
            file_name = metas[0];
            file_extension = metas[1];
            file_size = metas[2];

            //then write the file
            byte[] data_buf = new byte[1024];
            FileOutputStream os = new FileOutputStream("_OutputFiles/(" + source_name + ") " + file_name);
            int read_status = is.read(data_buf);
            //write to output stream until the input stream is done
            while(read_status != -1) {
                os.write(data_buf);
                data_buf = new byte[1024];
                read_status = is.read(data_buf);
            }

            time_end_transfer = System.nanoTime();
            System.out.println("(" +source_name + ") Done receiving file...");

            //close the sockets and streams created
            s.close();
            is.close();
            os.close();

            //write to output document storing the times
            String output = "";
            output += source_name + ",";
            output += source_ip + ",";
            output += source_port + ",";
            output += router_address + ",";
            output += destination_name + ",";
            output += destination_ip + ",";
            output += destination_port + ",";
            output += file_name + ",";
            output += file_extension + ",";
            output += file_size + ",";
            output += time_create + ",";
            output += time_router_request_connect + ",";
            output += time_router_response_connect + ",";
            output += time_start_demo + ",";
            output += time_return_lookup + ",";
            output += time_start_transfer + ",";
            output += time_end_transfer + ",";
            ClientCluster.WriteToOutputCSV(output);
        }
        //if the socket timeout is reached
        catch (SocketTimeoutException so_ex) {
            System.out.println("(" + source_name + ") ERROR: thread timed out when looking for other ip address...");
            //so_ex.printStackTrace();
        }
        catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (Exception e) {
            System.out.println("(" + source_name + ") ERROR: " + e.getMessage());
        }
        socket.close();
    }
}