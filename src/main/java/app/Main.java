package app;

import implementations.DistributedMap;

public class Main {

    public static String groupName;
    public static String multicastAddress;

    public static void processCommandLineMsg(String[] args){
        groupName = args[0];
        multicastAddress = args[1];
    }

    public static void main(String[] args){
        System.setProperty("java.net.preferIPv4Stack","true");
        processCommandLineMsg(args);
        DistributedMap distributedMap = new DistributedMap();
        Client client = new Client(groupName, multicastAddress, distributedMap);
        try {
            client.runClient();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
