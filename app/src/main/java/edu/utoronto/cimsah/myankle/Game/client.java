package edu.utoronto.cimsah.myankle.Game;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * Created by mshzhb on 2016-03-02.
 */
public class client extends Thread {

    Socket sock;
    public client(Socket s)
    {
        sock =s ;
    }
    public void run()
    {
        while (true) {
            try {
                InputStream in = sock.getInputStream();
                DataInputStream din = new DataInputStream(in);
                String st = din.readUTF();
                MainActivity.BN = Float.valueOf(st);

            } catch (IOException e) {
                System.out.println(e);
            }
        }
    }
}
