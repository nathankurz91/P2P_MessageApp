import java.util.ArrayList;
import java.util.Scanner;
import java.io.*;
import java.net.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class ClientCluster {

    public static final int start_client_number = 25000;	//number to start the clients at and will iterate upwards from there

    public static boolean running = false;

    static File output_file;
    static FileWriter output_writer;

    public static void main(String[] args) throws Exception {
        //create the file to export the data about this run
        output_file = new File("./_OutputFiles/_export.csv");
        output_writer = new FileWriter(output_file, false);
        //print column headers
        output_writer.write("source_name, source_ip, source_port, router_address, destination_name, destination_ip, destination_port, file_name, file_extension, file_size, time_create, time_router_request_connect, time_router_response_connect, time_start_demo, time_return_lookup, time_start_transfer, time_end_transfer");
        output_writer.write("\n");
        output_writer.close();

        String my_ip = InetAddress.getLocalHost().getHostAddress();
        System.out.println("Starting client cluster script on IP " + my_ip + "...");
        Scanner scanner = new Scanner(System.in);

        //determine which router we are connecting to
        System.out.println("Type in the ip address of the router this cluster will connect to...");
        String address = scanner.nextLine();
        while(!isValidInetAddress(address))
        {
            System.out.println("Address is not valid...");
            System.out.println("Type in the ip address of the router this cluster will connect to...");
            address = scanner.nextLine();
        }

        //get the name prefix of the clients of this cluster
        System.out.println("Type the name prefix you would like to add to the clients on this machine...");
        String source_name = scanner.nextLine();
        while(source_name.length() < 1)
        {
            System.out.println("Name length must be larger than 0");
            source_name = scanner.nextLine();
        }

        //determine how many clients we will host in this cluster
        System.out.println("Type how many clients you would like to host on this machine ([1,2500] ... default 1000)...");
        int num_client = scanner.nextInt();
        if(num_client < 1)
            num_client = 1000;
        else if(num_client > 2500)
            num_client = 2500;

        //get the name prefix of the destination cluster
        System.out.println("Type the name prefix of the destination clients...");
        String destination_name = scanner.nextLine();
        while(destination_name.length() < 1)
        {
            System.out.println("Name length must be larger than 0");
            destination_name = scanner.nextLine();
        }

        System.out.println("Creating " + num_client + " clients...");

        InetAddress other_address = InetAddress.getByName(address);
        for(int i = 0; i < num_client; i++) {
            ClientThread new_client = new ClientThread(my_ip, source_name, start_client_number + i, address, other_address, destination_name);
            new_client.start();
        }

        //pause program until the simulation is ready to start
        System.out.println("Press \"Enter\" when ready to start simulation...");
        try {
            System.in.read();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        running = true;
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

    public static void WriteToOutputCSV(String line) throws IOException {
        output_file = new File("./_OutputFiles/_export.csv");
        output_writer = new FileWriter(output_file, true);
        output_writer.write(line + "\n");
        output_writer.close();
//		ArrayList<String> temp = new ArrayList<String>();
//		temp.add(line);
//		Files.write(Paths.get("./_OutputFiles/_export.csv"), temp, Charset.forName("UTF-8"), StandardOpenOption.APPEND);
    }
}