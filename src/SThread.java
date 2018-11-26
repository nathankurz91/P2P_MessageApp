import java.io.*;
import java.net.*;
import java.lang.Exception;
import java.util.ArrayList;


public class SThread extends Thread
{
    private DatagramSocket socket;
    private boolean running;
    private DatagramPacket out_packet;

    private byte[] in_buf = new byte[256];
    private DatagramPacket in_packet;

    private ArrayList<String> routers;

    public SThread(int port_number, ArrayList<String> _routers) throws SocketException {
        routers = new ArrayList<String>();
        routers.addAll(_routers);
        //create the socket on the local machine on given port number
        socket = new DatagramSocket(port_number);
        in_packet = new DatagramPacket(in_buf, in_buf.length);
    }

    public void run() {
        running = true;
        //keep running until we say to stop
        while (running) {
            //make a new datagram packet, ready to receive the info from socket
            byte[] response = new byte[256];
            try {

                socket.receive(in_packet);
                //get the ip and port of the sender
                InetAddress address = in_packet.getAddress();
                int port = in_packet.getPort();

                String received = new String(in_packet.getData(), 0, in_packet.getLength());

                //System.out.println("----------------------------------------");
                //System.out.println("IP: " + address);
                //System.out.println("Port: " + port);
                //System.out.println("Received: " + received);
                System.out.println("Received message from " + address + ":" + port + ".... " + received);

                //if the string is in the format "CONNECT Bob123", "Bob123" is the name
                if(received.toUpperCase().contains("CONNECT")) {
                    received = received.replace("CONNECT", "").trim();
                    //System.out.println("Name: " + received);
                    if(SRouter.AddClient(received, address.toString(), "" + port))
                    {
                        response = ("Connected to router " + SRouter.my_ip + "...").getBytes();
                    }
                    else
                    {
                        response = ("Failed to connect...").getBytes();
                    }

                }
                //if the string is in the format "FIND Jill456", "Jill456" is the name we are trying to find
                else if(received.toUpperCase().contains("FIND")) {
                    received = received.replace("FIND", "").trim();
                    String destination = SRouter.FindClient(received);
                    //we found the destination
                    if(destination != null) {
                        response = destination.getBytes();
                    }
                    //we did not find, need to search other known routers
                    else {
                        DatagramSocket searchSocket = new DatagramSocket();
                        String searchMessage = "SEARCH " + received;
                        byte[] searchBytes = searchMessage.getBytes();
                        boolean searchFound = false;
                        for(String r : routers) {
                            DatagramPacket searchPacket = new DatagramPacket(searchBytes, searchBytes.length, InetAddress.getByName(r), SRouter.router_port_number);
                            searchSocket.send(searchPacket);

                            //wait for response
                            searchSocket.receive(in_packet);
                            String searchReceived = new String(in_packet.getData(), 0, in_packet.getLength());
                            if(searchReceived.contains("FOUND"))
                            {
                                searchFound = true;
                                response = searchReceived.getBytes();
                                break;
                            }
                        }
                        searchSocket.close();
                        if(!searchFound) {
                            //searched all known routers and can't find anywhere
                            response = ("Failed to find " + received + "...").getBytes();
                        }
                    }
                }
                //if the string is in the format "SEARCH Jill456", "Jill456" is the name we are trying to find
                else if(received.toUpperCase().contains("SEARCH")) {
                    received = received.replace("SEARCH", "").trim();
                    String destination = SRouter.FindClient(received);
                    //we found the destination
                    if(destination != null) {
                        response = ("FOUND" + destination).getBytes();
                    }
                    else {
                        response = ("FAILED").getBytes();
                    }
                }
                else if (received.equals("STOP")) {
                    running = false;
                    continue;
                }
                //send the response packet back to the person sending
                out_packet = new DatagramPacket(response, response.length, address, port);
                //System.out.println("Response: " + new String(response, 0, response.length));
                socket.send(out_packet);
                //System.out.println("----------------------------------------");
            }
            catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        socket.close();
    }
}