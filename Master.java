
import java.io.*;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;


public class Master {


    ServerSocket serverSocket;

    int SERVER_PORT;


    public Master(int SERVER_PORT){

        this.SERVER_PORT = SERVER_PORT;
    }


    public void initialize_connection(){
        try {

            serverSocket = new ServerSocket(SERVER_PORT);

        } catch (IOException e) {

            System.out.println("Master : Creation error.");

        }

    }


    public void sync_with_follower(int timeout) {

        int timeout_ms = timeout * 1_000;

        ArrayList<Thread> threads = new ArrayList<>();
        ArrayList<Command_Listener> followers = new ArrayList<>();

        int follower_count = 0;
        Command_Listener lastFollower;

        // create threads for new followers

//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                while(true){
//
//
//
//
//                    try {
//                        Thread.sleep(timeout_ms);
//                    } catch (InterruptedException e) {
//                        System.out.println("Master : Main Listener failed.");
//                    }
//
//                    if (lastFollower.socket.isConnected()){
//
//                }
//            }
//        }).start();




        do {

            Command_Listener follower_listener = new Command_Listener(serverSocket);
            Thread this_thread = new Thread(follower_listener);

            this_thread.start();
            threads.add(this_thread);
            followers.add(follower_listener);

            try {
                Thread.sleep(timeout_ms);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            lastFollower = followers.get(followers.size()-1);

            if (lastFollower.socket != null
                    && lastFollower.socket.isConnected()) follower_count +=1;

        } while(lastFollower.socket != null
                && lastFollower.socket.isConnected());

        // handle existing threads

        for (int i = 0; i < follower_count; i++){
            Thread this_thread = threads.get(i);
            try {
                this_thread.join();
            } catch (InterruptedException e) {
                System.out.println("Master : Thread join error.");
            }
        }

    }


    public void terminate_connection() {

        try {

            if (serverSocket != null){serverSocket.close();}

        } catch (IOException e) {

            System.out.println("Master : Termination error");

        }

    }


}





class Command_Listener implements Runnable{

    ServerSocket serverSocket;

    BufferedReader reader;
    PrintWriter writer;
    Socket socket;


    public Command_Listener(ServerSocket serverSocket){ this.serverSocket = serverSocket; }

    @Override
    public void run() {

        try {

            socket = serverSocket.accept();
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream());

            String command = reader.readLine();

            while(!command.equals("ClientDone")) {

                switch (command) {

                    case "ClientSend":

                        System.out.println("Master : is receiving..");

                        String filesToReceive = reader.readLine();
                        String size_filesToReceive = reader.readLine();

                        Resources.receive_files(socket, filesToReceive, size_filesToReceive);

                        break;

                    case "ClientReceive":

                        System.out.println("Master : is sending..");

                        writer.println(Resources.get_changes_names());
                        writer.println(Resources.get_changes_sizes());
                        writer.flush();

                        Resources.send_files(socket, Resources.get_changes_files());

                        break;

                    default:

                        System.out.println("Master : Obtained unknown Command." + command);

                        break;
                }

                command = reader.readLine();

            }

            System.out.println("Master : Finished interaction with client.");

        } catch (IOException e) {

            System.out.println("Master : Transaction failed");

        }

    }

}
