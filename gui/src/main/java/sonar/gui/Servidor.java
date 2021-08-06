/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package codigo;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Observable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Alejandro
 */
public class Servidor extends Observable implements Runnable{

    private int puerto;
    public Servidor(int puerto){
        this.puerto=puerto;
    }
    
    @Override
    public void run() {
        
         ServerSocket servidor = null;
            Socket sc = null;
            DataInputStream in;
            DataOutputStream out;
          
            
        try {    
            servidor = new ServerSocket(puerto);
            System.out.println("Servidor iniciado");
            
            while(true){
                sc = servidor.accept();//espera en la linea. Esto es el socket del cliente
                System.out.println("Cliente conectado");
                in = new DataInputStream(sc.getInputStream());
               
                //cabiar readUTF por read
                byte[] arrbytes= in.readAllBytes(); //A LA ESPERA DE UN MENSAJE
                //System.out.println(mensaje);
                String varloc = new String(arrbytes, java.nio.charset.StandardCharsets.UTF_8);
                this.setChanged();
                this.notifyObservers(varloc);
                this.clearChanged();
                
                
                
                
                
                sc.close();//cierra el cliente
                
                
                
            }
        } catch (IOException ex) {
            Logger.getLogger(Servidor.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        
        
        
        
        
        
        
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        
        
        
        
        
        
        
        
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
