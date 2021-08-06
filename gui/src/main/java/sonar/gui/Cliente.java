/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package codigo;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Alejandro
 */
public class Cliente implements Runnable{
    
    private int puerto;
    private byte[] arrbyt ;
    //mensaje va a ser un arreglo de bytes 
    public Cliente(int puerto, byte[] arrbyt){
        this.puerto=puerto;
        this.arrbyt = arrbyt;
    }
    
    
    @Override
    public void run() {
        final String HOST="192.168.8.111";
       // final int PUERTO = 4500;
        
        DataOutputStream out;
        try {
            Socket sc = new Socket(HOST, puerto);
            
            
            out =  new DataOutputStream(sc.getOutputStream());
            // aqui colocare out.write(bytesimg);
            out.write(arrbyt);
            
            sc.close();
            
        } catch (IOException ex) {
            Logger.getLogger(Cliente.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        
        
        
        
        
        
        
        
        
        
       // throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        
        
        
        
        
        
        
        
        
        
        
       // throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
