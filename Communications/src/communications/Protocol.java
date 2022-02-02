/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package communications;

import java.util.ArrayList;

/**
 *
 * @author masa
 */
/*

1- server health check (envia missatge per saber si tot obe)
2- server health ack (respon que to be)
3- server ask Type (diferencia entre mvl i pc, client retorna el tipo de modul qu es)
4- validateConnection (retorna true si ok, el servidor no te totes les conexions ocupades, false si les te ocupades)
5- CloseConnection (una conexio que es tanca envia aixo perque l'altre part sapiga que es va a tancar i per tant tambe la tanqui)

mes endevant:
6- demana a veinat directe mac ??? alomillo no necesari
7- demena mac qualsevol veinat
8- ask aveilable, s'intenten conectar, si no tens puesto demenaras a tots es veinats amem si colcu en te (retorna null o innetAddress)
9- ChangePositions
10- askLookup, a ne veinat

*/
public class Protocol {
    
    private ArrayList<Integer> nonUsableIDs;
    private ArrayList<ProtocolDescription> protocolList;
    
    public Protocol(){
        this.nonUsableIDs = new ArrayList<>();
        this.protocolList = new ArrayList <>();
        this.protocolList.add(new ProtocolDescription(1, "Socket test", "Integer"));
        this.protocolList.add(new ProtocolDescription(2, "Socket test ACK", "Integer"));
    }
    
    public boolean addCmd(int id, ProtocolDescription desc){
        if(nonUsableIDs.add(id)){
            this.protocolList.add(desc);
            return true;
        }else{
            return false;
        }
    }
    
    public ProtocolDescription getProtocol(int id){
        for(ProtocolDescription protocol : protocolList){
            if(protocol.getId() == id){
                return protocol;
            }
        }
        return null;
    }
    
    public ProtocolDataPacket constructPacket(int id, String source, String target, Object object){
        return new ProtocolDataPacket(source, target, id, object);
    }
    
    public boolean processMessage(Connection conn,ProtocolDataPacket packet){
        if (packet!=null){
            switch (packet.getId()){
                case 1:
                    conn.answerTestRequest(packet);
                    break;
                    
                case 2: 
                    conn.getServerHealth().checkTestAnswer(packet);
                    break;
                
                default:
                    return false;
            }
        }
        return true;
    }
}
