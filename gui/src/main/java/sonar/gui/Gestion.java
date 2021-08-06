/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package codigo;
import java.io.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
//import org.apache.commons.io.FileUtils;
/**
 *
 * @author Alejandro
 */
public class Gestion {
    FileInputStream entrada;
    FileOutputStream salida;
    File archivo;
    
    public Gestion(){
        
    } 
    //abrir archivo de texto 
    public String AbrirATexto(File archivo){
            String contenido="";
        try{
            entrada = new FileInputStream(archivo);
            int ascii;
            while((ascii = entrada.read())!=-1){
                char caracter =(char)ascii;
                contenido += caracter;
            }
        
        }catch(Exception e){

        }
        return contenido;
    }
    
    
    
    //guardar archivo e texto
    public String GuardarATexto(File archivo, String contenido){
        String respuesta=null;
        try{
            salida = new FileOutputStream(archivo);
            byte[] bytesTxt = contenido.getBytes();
            salida.write(bytesTxt);
            respuesta = "Se guardo con exito el archivo";
        }catch(Exception e){
            
        }
        return respuesta;
    }
    
    //abrir imagen 
    /*la clase se llama fileUtils le pondre el metodo readFiletoByteArray*/
    public byte[] AbrirAImagen(File archivo){
        byte[] bytesImg = new byte[1024*100];
        try{
            entrada = new FileInputStream(archivo);
            entrada.read(bytesImg);
        }catch(Exception e){
            
        }
        return bytesImg;
    }
    //abrir archivo 
    public byte[] AbrirArchivo(File archivo){
        byte[] bytesArch = new byte[1024*100];
        try{
            entrada = new FileInputStream(archivo);
            //entrada=Files.readAllBytes(bytesArch);
           bytesArch =Files.readAllBytes(archivo.toPath());
           //bytesArch= FileUtils.readFileToByteArray(archivo);
           
        }catch(Exception e){
            
        }
        return bytesArch;
    }
    
    
    
    
    
    //guardar imagen 
    public String GuardarAImagen(File archivo, byte[] bytesImg){
        String respuesta = null;
        try{
            salida = new FileOutputStream(archivo);
            salida.write(bytesImg);
            respuesta = "La imagen se guardo con exito";
        }catch(Exception e){
            
        }
        return respuesta;
    }
    
     public String GuardarArchivo(File archivo, byte[] bytesImg){
        String respuesta = null;
        try{
            salida = new FileOutputStream(archivo);
            salida.write(bytesImg);
            respuesta = "La imagen se guardo con exito";
        }catch(Exception e){
            
        }
        return respuesta;
    }
    
    
}
