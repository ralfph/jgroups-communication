package app;

import implementations.DistributedMap;
import org.jgroups.*;
import org.jgroups.protocols.*;
import org.jgroups.protocols.pbcast.*;
import org.jgroups.stack.ProtocolStack;
import org.jgroups.util.Util;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;

public class Client extends ReceiverAdapter {
    private DistributedMap distributedMap;
   // String msg;
    //String msgToSend;
    private JChannel channel;
    private String groupName;
    public View currentView = null;


    public Client(String groupName, DistributedMap distributedMap){
        //this.msgToSend = msgToSend;
        this.groupName = groupName;
        this.distributedMap = distributedMap;
        channel = new JChannel(false);
    }

    @Override
    public void viewAccepted(View newView){
        if(currentView == null){
            System.out.println("New member: " + newView.toString());
        }
        else{
            /*List<Address> addedMembers = View.newMembers(currentView, newView);
            for(Address adr: addedMembers){
                System.out.println(adr.toString());
            }*/
        }
        currentView = newView;
    }

    @Override
    public void getState(OutputStream out){
        synchronized(distributedMap) {
            try {
                Util.objectToStream(distributedMap, new DataOutputStream(out));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void setState(InputStream in){
        synchronized(distributedMap) {
            try {
                distributedMap = (DistributedMap) Util.objectFromStream(new DataInputStream(in));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void receive(Message message){
        synchronized(distributedMap) {
            distributedMap = (DistributedMap) message.getObject();
            for(Map.Entry<String, Integer> entry: distributedMap.stringMap.entrySet())
                System.out.print("Distributed Map: " + entry.toString() + ", ");
            System.out.println();
        }
    }

    public void sendStateMessage(){
        //System.out.println("Msg to send: " +sMsg);
        Message m = new Message(null, distributedMap);
        try {
            channel.send(m);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void prepareClientToRun() throws UnknownHostException {
        ProtocolStack stack=new ProtocolStack();
        channel.setProtocolStack(stack);
        stack.addProtocol(new UDP().setValue("mcast_group_addr",InetAddress.getByName("230.100.200.1")))
                .addProtocol(new PING())
                .addProtocol(new MERGE3())
                .addProtocol(new FD_SOCK())
                .addProtocol(new FD_ALL().setValue("timeout", 12000).setValue("interval", 3000))
                .addProtocol(new VERIFY_SUSPECT())
                .addProtocol(new BARRIER())
                .addProtocol(new NAKACK2())
                .addProtocol(new UNICAST3())
                .addProtocol(new STABLE())
                .addProtocol(new GMS())
                .addProtocol(new UFC())
                .addProtocol(new MFC())
                .addProtocol(new FRAG2())
                .addProtocol(new SEQUENCER())
                .addProtocol(new FLUSH())
                .addProtocol(new STATE_TRANSFER());

        try {
            stack.init();
            channel.setReceiver(this);
            //channel.setDiscardOwnMessages(true);
            channel.connect(groupName);
            channel.getState(null, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void disconnectFromGroup(){
        try {
            Thread.sleep(2 * 60000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        channel.close();
    }

    public void putElementIntoDistributedMap(String key, Integer value){
        synchronized(distributedMap){
            if(!distributedMap.containsKey(key))
                distributedMap.put(key, value);
            sendStateMessage();
        }
    }

    public void removeElementFromDistributedMap(String key){
        synchronized(distributedMap){
            if(distributedMap.containsKey(key))
                distributedMap.remove(key);
        }
    }

    public Integer getValueFromDistributedMap(String key){
        synchronized(distributedMap){
            if(distributedMap.containsKey(key)){
               return distributedMap.get(key);
            }
            return null;
        }
    }

    public boolean containsKey(String key){
        return distributedMap.containsKey(key);
    }

    public void handlePuttingElementIntoDistributedMap() throws Exception{
        BufferedReader bfr = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Enter key to add to distributedMap: ");
        String key = bfr.readLine();
        System.out.println("Enter value corresponding key to add to distributedMap: ");
        String value = bfr.readLine();
        putElementIntoDistributedMap(key, Integer.parseInt(value));
    }

    public void handleRemovingElementFromDistributedMap() throws Exception{
        BufferedReader bfr = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Enter key to remove from distributedMap: ");
        String key = bfr.readLine();
        removeElementFromDistributedMap(key);
    }

    public void handleGettingValueFromDistributedMap() throws Exception{
        BufferedReader bfr = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Enter key to get value from distributedMap: ");
        String key = bfr.readLine();
        Integer value =  getValueFromDistributedMap(key);
        String msg;
        msg = (value != null) ? ("Key: " + key + ", value: " + value) : "null";
        System.out.println(msg);
    }

    public void handleDistributedMapContainsKey() throws Exception{
        BufferedReader bfr = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Enter key to get value from distributedMap: ");
        String key = bfr.readLine();
        boolean result = containsKey(key);
        System.out.println("Is key present in distributedMap? : " + result);
    }

    public void handleQuiting(){
        System.out.println("----------------[QUITING]----------------");
    }

    public void runClient() throws Exception{
        boolean isRunning = true;
        BufferedReader bfr = new BufferedReader(new InputStreamReader(System.in));
        prepareClientToRun();
        System.out.println("To put new entry into map enter \"put\"\n" +
                "To remove entry from map enter \"remove\"\n" +
                "To get <key, value> from map enter \"get\"\n" +
                "To check key exists in map enter \"contains\"\n" +
                "To exit from app enter q: ");

        while(isRunning) {
            String choice = bfr.readLine();
            switch(choice.toLowerCase()){
                case "put":
                    handlePuttingElementIntoDistributedMap();
                    break;
                case "remove":
                    handleRemovingElementFromDistributedMap();
                    break;
                case "get":
                    handleGettingValueFromDistributedMap();
                    break;
                case "contains":
                    handleDistributedMapContainsKey();
                    break;
                case "q":
                    handleQuiting();
                    isRunning = false;
                    break;
            }
        }
        disconnectFromGroup();
    }

}
