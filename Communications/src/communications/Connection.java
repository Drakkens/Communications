/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package communications;

import static communications.CommunicationController.*;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 *
 * @author PC
 */
public class Connection implements Runnable{
    
    private CommunicationController controller;
    private Protocol protocol;
    private Socket socket;
    private ServerHealth serverHealth;
    private boolean statusOk;
    private long lastMessageReceived;
    private InetAddress ip;
    private String connectedMAC;
    private String localMAC;
    private int connectionType;
    private boolean running;
    
    private ObjectInputStream input;
    private ObjectOutputStream output;

    public Connection(CommunicationController controller, Socket socket) throws IOException {
        this.controller = controller;
        this.socket = socket;
        this.ip=this.socket.getInetAddress();
        this.protocol=new Protocol();
        this.output = new ObjectOutputStream(this.socket.getOutputStream());
        this.input = new ObjectInputStream(this.socket.getInputStream());
        this.statusOk=true;
        this.lastMessageReceived = System.currentTimeMillis();
        this.serverHealth = new ServerHealth(controller, this);
        //comença amb 0 ja que sino li feim es setter per cambiar valor a tipus
        //de conexio, instanciat pes seervidor o pes client, farem que simplement
        //sigui tractat com una conexio de ses "antigues"
        this.connectionType=CLIENT;
    }
    
    //TO DO: Do we really need all of this getters and setters?
    public void setServerHealth(ServerHealth serverHealth) {
        this.serverHealth = serverHealth;
    }

    public ServerHealth getServerHealth() {
        return serverHealth;
    }

    public String getConnectedMAC() {
        return connectedMAC;
    }

    public void setConnectedMAC(String connectedMAC) {
        this.connectedMAC = connectedMAC;
    }

    public void setConnectionType(int connectionType) {
        this.connectionType = connectionType;
    }

    public String getLocalMAC() {
        return localMAC;
    }

    public void setLocalMAC(String localMAC) {
        this.localMAC = localMAC;
    }

    public long getLastMessageReceived() {
        return lastMessageReceived;
    }

    public InetAddress getIp() {
        return ip;
    }

    public boolean isStatusOk() {
        return statusOk;
    }
    
    public void setStatusOk(boolean statusOk) {
        this.statusOk = statusOk;
    }

    public void setSocket(Socket socket) throws IOException {
        System.out.println("Openning socket...");
        this.socket = socket;
        this.output = new ObjectOutputStream(this.socket.getOutputStream());
        this.input = new ObjectInputStream(this.socket.getInputStream());
    }

    public Socket getSocket() {
        return socket;
    }
    
    @Override
    public void run() {
        running=true;
        System.out.println("Connection succesfull");
        lastMessageReceived=System.currentTimeMillis();
        if(connectionType==SERVER){
            this.askDeviceType();
        }
        while (running){
            try{
                if (this.statusOk){
                    ProtocolDataPacket recibido=recive();
                    this.protocol.processMessage(this, recibido);
                    lastMessageReceived=System.currentTimeMillis();
                }
                Thread.sleep(50);
            } catch (Exception ex) {
                System.out.println("run connection: "+ex.getMessage());
            }
        }
    }
    
    public synchronized void send(ProtocolDataPacket packet){
        try {
            this.output.writeObject(packet);
        } catch (IOException ex) {
            System.out.println("Couldn't send message: "+ex.getMessage());
        }
    }
    
    public ProtocolDataPacket recive(){
        ProtocolDataPacket object=null;
        try {
            object = (ProtocolDataPacket)this.input.readObject();
        } catch (Exception ex) {
            System.out.println("Error receiving message: "+ex.getMessage());
            this.statusOk=false;
            this.serverHealth.setTestRequestWaiting(false);
        }
        return object;
    }
    
    public void answerTestRequest(ProtocolDataPacket packetReceived){
        ProtocolDataPacket packet = new ProtocolDataPacket(this.localMAC,this.connectedMAC,2,packetReceived.getObject());
        send(packet);
    }
    
    public void askDeviceType(){
        ProtocolDataPacket packet=new ProtocolDataPacket(this.localMAC,null,3,null);
        send(packet);
    }
    
    public void sendDeviceType(ProtocolDataPacket packetReceived){
        this.connectedMAC = (String) packetReceived.getSourceID();
        ProtocolDataPacket packet = new ProtocolDataPacket(this.localMAC,this.connectedMAC,4,PC);
        send(packet);
    }
    
    public void processDeviceType(ProtocolDataPacket packetReceived){
        this.connectedMAC = (String) packetReceived.getSourceID();
        boolean validated=false;
        int deviceType=(int)packetReceived.getObject(); 
        if (deviceType == MVL){
            //add a sa llista de connections de mvl
        } 
        else if (deviceType == PC){
            validated=this.controller.availableConnections();
            if (validated){
            //add a sa llista de connections de pc
            }
        }
        
        ProtocolDataPacket packet = new ProtocolDataPacket(this.localMAC,this.connectedMAC,5,validated);
        send(packet);
        
        if (!validated){
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                System.out.println("sleep processDeviceType: "+ex.getMessage());
            }
            this.cerrarSocket();
            this.running=false;
        }
    }
    
    public void processValidation(ProtocolDataPacket packetReceived){
        if ((boolean)packetReceived.getObject()){
            //ficar conexio a llista pcs, ya que sempre sira pc s'altre banda(servidor)
        } else {
            this.cerrarSocket();
            this.running=false;
        }
    }
    
    public void cerrarSocket(){
        try {
            System.out.println("Closing sockets");
            this.socket.close();
            this.input.close();
            this.output.close();
            this.socket = null;
        } catch (IOException ex) {
            System.out.println("Error closing sockets: "+ex.getMessage());
        }
    }
    
}