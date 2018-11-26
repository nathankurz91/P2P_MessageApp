import java.util.ArrayList;
import java.util.Scanner;
import java.io.*;
import java.net.*;

//struct used to hold info about the client connections (friendly name, ip, port)
class ClientInfo {
    public String name, ip, port;
    public ClientInfo(String _name, String _ip, String _port) {
        name = _name;
        ip = _ip;
        port = _port;
    }
}

public class SRouter {

    public static String my_ip;
    static ArrayList<ClientInfo> my_clients;	//store the IP addresses of my clients
    static ArrayList<String> my_routers;

    public static final int router_port_number = 5555;	//the port number to be used on all of the routers

    public static void main(String[] args) throws IOException {
        my_clients = new ArrayList<ClientInfo>();
        my_routers = new ArrayList<String>();

        //output my IP address
        InetAddress addr = InetAddress.getLocalHost();
        String host = addr.getHostAddress(); // Client machine's IP
        my_ip = host;
        System.out.println("My IP is: " + my_ip + "\n");



        //keep arrays of other known routers and their IP addresses
        Scanner scanner = new Scanner(System.in);
        System.out.println("Type in the next router IP address or \"STOP\" to stop");
        String input_line = scanner.nextLine();

        while(input_line.length() > 0 && !input_line.equals("STOP")) {

            //check to see if address is valid and if so, add
            if(isValidInetAddress(input_line.trim()))
            {
                boolean unique = true;
                for(String r : my_routers)
                {
                    if(r.equals(input_line.trim()))
                    {
                        System.out.println("ERROR: Server Router already added... please add unique server routers\n");
                        unique = false;
                        break;
                    }
                }
                if(unique) {
                    if(input_line.trim().equals(my_ip))
                    {
                        System.out.print("ERROR: Don't add the current server router to the list...\n");
                    }
                    else
                    {
                        my_routers.add(input_line.trim());
                        System.out.println("Server Router added!\n");
                    }
                }
            }
            else
                System.out.println("ERROR: IP address of " + input_line + " is not valid!\n");

            System.out.println("Type in the next router IP address or \"STOP\" to stop");
            input_line = scanner.nextLine();
        }

        //print the routers on screen
        for(int i = 0; i < my_routers.size(); i++) {
            System.out.println("Router " + i + ": " + my_routers.get(i));
        }

        //start the server thread that is listening for connections/messages
        SThread server_thread = new SThread(router_port_number, my_routers);
        server_thread.start();

    }

    //used to determine if IP address is valid
    public static boolean isValidInetAddress(String ip)
    {
        try {
            return Inet4Address.getByName(ip).getHostAddress().equals(ip);
        }
        catch(UnknownHostException ex)
        {
            return false;
        }
    }

    public static boolean AddClient(String name, String ip, String port) {
        //check to make sure that client doesn't already exist
        for(ClientInfo c : my_clients) {
            if(c.name.equals(name))
            {
                System.out.println("ERROR: Name already exists... " + name);
                return false;
            }
            if(c.ip.equals(ip) && c.port.equals(port))
            {
                System.out.println("ERROR: IP/port combo already exists... " + ip + ":" + port);
                return false;
            }
        }

        my_clients.add(new ClientInfo(name, ip, port));
        return true;
    }

    public static String FindClient(String destination) {
        for(ClientInfo c : my_clients) {
            if(c.name.equals(destination))
            {
                //found the destination in my own list, return the ip
                return c.ip + ":" + c.port;
            }
        }

        return null;
    }
}