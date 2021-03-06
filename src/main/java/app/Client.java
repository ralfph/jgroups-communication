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
    private JChannel channel;
    private String groupName;
    private String multicastAddress;
    private View currentView = null;


    public Client(String groupName, String multicastAddress, DistributedMap distributedMap){
        this.groupName = groupName;
        this.multicastAddress = multicastAddress;
        this.distributedMap = distributedMap;
        this.channel = new JChannel(false);
    }

    @Override
    public void viewAccepted(View newView){
        if(currentView == null){
            System.out.println("New member: " + newView.toString());
        }
        if(newView instanceof MergeView) {
            ViewHandler handler=new ViewHandler(channel, (MergeView)newView);
            handler.start();
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
        }
    }

    public void sendStateMessage(){
        Message m = new Message(null, distributedMap);
        try {
            channel.send(m);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //"230.100.200.1"
    public void prepareClientToRun() throws UnknownHostException {
        ProtocolStack stack=new ProtocolStack();
        channel.setProtocolStack(stack);
        stack.addProtocol(new UDP().setValue("mcast_group_addr",InetAddress.getByName(multicastAddress)))
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
            Thread.sleep( 60000);
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
            sendStateMessage();
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

    public void handleShowingElements(){
        synchronized(distributedMap){
            System.out.println("|--------------------------------------------|");
            for(Map.Entry<String, Integer> entry: distributedMap.stringMap.entrySet())
                System.out.print("entry: " + entry.toString() + ", ");
            System.out.println("\n|--------------------------------------------|");
        }
    }

    public void handlePuttingElementIntoDistributedMap() throws Exception{
        BufferedReader bfr = new BufferedReader(new InputStreamReader(System.in));
        System.out.print("|--------------------------------------------|");
        System.out.println("\nEnter key to add to distributedMap: ");
        System.out.print("|--------------------------------------------|\n>>");
        String key = bfr.readLine();
        System.out.print("|--------------------------------------------|");
        System.out.println("\nEnter value corresponding key to add to distributedMap: ");
        System.out.print("|--------------------------------------------|\n>>");
        String value = bfr.readLine();
        putElementIntoDistributedMap(key, Integer.parseInt(value));
    }

    public void handleRemovingElementFromDistributedMap() throws Exception{
        BufferedReader bfr = new BufferedReader(new InputStreamReader(System.in));
        System.out.print("|--------------------------------------------|");
        System.out.println("\nEnter key to remove from distributedMap: ");
        System.out.print("|--------------------------------------------|\n>>");
        String key = bfr.readLine();
        removeElementFromDistributedMap(key);
    }

    public void handleGettingValueFromDistributedMap() throws Exception{
        BufferedReader bfr = new BufferedReader(new InputStreamReader(System.in));
        System.out.print("|--------------------------------------------|");
        System.out.println("\nEnter key to get value from distributedMap: ");
        System.out.print("|--------------------------------------------|\n>>");
        String key = bfr.readLine();
        Integer value =  getValueFromDistributedMap(key);
        String msg;
        msg = (value != null) ? ("Key: " + key + ", value: " + value) : "null";
        System.out.println("|--------------------------------------------|");
        System.out.println(msg);
        System.out.println("|--------------------------------------------|");
    }

    public void handleDistributedMapContainsKey() throws Exception{
        BufferedReader bfr = new BufferedReader(new InputStreamReader(System.in));
        System.out.print("|--------------------------------------------|");
        System.out.println("\nEnter key to get value from distributedMap: ");
        System.out.print("|--------------------------------------------|\n>>");
        String key = bfr.readLine();
        boolean result = containsKey(key);
        System.out.print("|--------------------------------------------|");
        System.out.println("\nIs key present in distributedMap? : " + result);
        System.out.println("|--------------------------------------------|");
    }

    public void handleQuiting(){
        System.out.println("\n----------------[QUITING]----------------");
    }

    public void runClient() throws Exception{
        boolean isRunning = true;
        BufferedReader bfr = new BufferedReader(new InputStreamReader(System.in));
        prepareClientToRun();
        System.out.println("|--------------------------------------------|");
        System.out.println("|To put new entry into map enter \"put\"\n" +
                "To remove entry from map enter \"remove\"\n" +
                "To get <key, value> from map enter \"get\"\n" +
                "To check key exists in map enter \"contains\"\n" +
                "To print all elements in map enter \"print\"\n" +
                "To exit from app enter q: |");
        System.out.println("|--------------------------------------------|");

        while(isRunning) {
            System.out.print("<put> | <remove> | <get> | <contains> | <print> | <q> \n>>");
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
                case "print":
                    handleShowingElements();
                    break;
                case "q":
                    handleQuiting();
                    isRunning = false;
                    break;
            }
        }
        disconnectFromGroup();
    }

    // ViewHandler stuff from jgroups.org tutorial
    private static class ViewHandler extends Thread {
        JChannel ch;
        MergeView view;

        private ViewHandler(JChannel ch, MergeView view) {
            this.ch = ch;
            this.view = view;
        }

        public void run() {
            List<View> subgroups = view.getSubgroups();
            View tmp_view = subgroups.get(0); // picks the first
            Address local_addr = ch.getAddress();
            if (!tmp_view.getMembers().contains(local_addr)) {
                System.out.println("Not member of the new primary partition ("
                        + tmp_view + "), will re-acquire the state");
                try {
                    ch.getState(null, 30000);
                } catch (Exception ex) {
                }
            } else {
                System.out.println("Not member of the new primary partition ("
                        + tmp_view + "), will do nothing");
            }
        }

    }
}
