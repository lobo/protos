package ar.edu.itba.pdc.GLoboH;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class GLoboHData {

    private static final GLoboHData INSTANCE = new GLoboHData();
    private static final String CONFIG_PATH = "/globoh.proxy.properties";

    private static final Logger logger = LoggerFactory.getLogger(GLoboHData.class);

    private static final StringBuilder builder = new StringBuilder();

    /**
     * Class method to get the Singleton instance
     * @return the unique instance
     */
    public static GLoboHData getInstance() {return INSTANCE;}

    private String defaultServerHost;
    private int defaultServerPort;
    private String xmppServerName;

    private final Map<String, InetSocketAddress> usersToServerMultiplexedMap = new HashMap<>();
    private final Map<String, AtomicLong> usersAccess = new HashMap<>();
    private final Map<String, AtomicLong> usersMessageSent = new HashMap<>();
    private final Map<String, AtomicLong> usersMessageReceived = new HashMap<>();
    private final Set<String> mutedJIDs = new HashSet<>();
    private boolean l33tActivated = false;
    private String globohUsername;
    private String globohPassword;


    private final AtomicLong messagesSent = new AtomicLong();
    private final AtomicLong messagesReceived = new AtomicLong();
    private final AtomicLong messagesMutedOutgoing = new AtomicLong();
    private final AtomicLong messagesMutedIngoing = new AtomicLong();

    private GLoboHData() {
        Properties properties = new Properties();

        InputStream is = getClass().getResourceAsStream(CONFIG_PATH);
        try {
            properties.load(is);
        } catch (IOException e) {
            logger.error("Could not load properties");
            System.exit(1);
        }
        load(properties);
    }

    /**
     * Loads the properties file values to be set in the configuration data
     * @param properties Properties instance to be loaded
     */
    private void load(Properties properties) {
        globohUsername = properties.getProperty("globoh.username", "glh");
        globohPassword = properties.getProperty("globoh.password", "0000");

        l33tActivated = properties.getProperty("l33t.default", "false").equals("true");
        defaultServerHost = properties.getProperty("server.default.host", "localhost");
        defaultServerPort = Integer.parseInt(properties.getProperty("server.default.port", "5222"));
        xmppServerName = properties.getProperty("xmpp.server.name", "localhost");

        String[] users = properties.getProperty("users.muted").split(",");
        for (String user : users) {
            user = user.trim();
            if (!user.equals("")) {
                mutedJIDs.add(user);
            }
        }

        users = properties.getProperty("users.multiplexed").split(",");
        for (String user : users) {
            user = user.trim();
            if (!user.equals("")) {
                final String[] parts = user.split("->");
                final String[] addressParts = parts[1].split(":");
                if (parts.length != 2 || addressParts.length != 2) {
                    logger.warn("Can't multiplex, invalid user value passed: {}", user);
                }else {
                    final String userPart = parts[0];
                    final String addresHost = addressParts[0];
                    Integer addresPort;
                    try {
                        addresPort = Integer.parseInt(addressParts[1]);
                    }catch (ClassCastException e){
                        addresPort = defaultServerPort;
                    }
                    usersToServerMultiplexedMap.put(userPart, new InetSocketAddress(addresHost, addresPort));
                }
            }
        }
    }

    public InetSocketAddress getServerAddress(String localUsername) {
        return usersToServerMultiplexedMap.getOrDefault(localUsername,new InetSocketAddress(defaultServerHost, defaultServerPort));
    }

    public void addServerMapping(String username, String server, short port) {
        usersToServerMultiplexedMap.put(username, new InetSocketAddress(server,port));
    }

    public void addServerMapping(String username, String server) {
        usersToServerMultiplexedMap.put(username, new InetSocketAddress(server,defaultServerPort));
    }

    public String getXMPPServerName() {
        return xmppServerName;
    }

    public boolean isL33tActivated() {
        return l33tActivated;
    }

    public void setL33t(boolean l33tActivated) {
        this.l33tActivated = l33tActivated;
    }

    /**
     * Sets mute to the assigned user
     * @param user to be muted/un-muted
     * @param mute true if has to be muted, false if not
     */
    public void userIsMuted(String user, boolean mute) {
        if(mute) {
            mutedJIDs.add(user.trim().toLowerCase());
        }else{
            mutedJIDs.remove(user.trim().toLowerCase());
        }
    }

    public boolean isJIDMuted(String jid) {
        String[] parts = jid.split("/");
        String jidProper = parts[0]; // that is, without a resource attached
        return mutedJIDs.contains(jidProper.trim().toLowerCase());
    }

    public void newSentMessage(String username) {
        if(!usersMessageSent.containsKey(username)) {
            usersMessageSent.put(username, new AtomicLong(1));
        }else {
            usersMessageSent.get(username).incrementAndGet();
        }
        messagesSent.incrementAndGet();
    }

    public long getSentMessagesCount(String username) {
        AtomicLong atomicLong;
        if ((atomicLong = usersMessageSent.get(username))==null)
            return 0;
        else
             return atomicLong.get();
    }

    public long getSentMessagesCount() {
        return messagesSent.get();
    }

    public void newReceivedMessage(String username) {
        if(!usersMessageReceived.containsKey(username)){
            usersMessageReceived.put(username,new AtomicLong(1));
        }else {
            usersMessageReceived.get(username).incrementAndGet();
        }
        messagesReceived.incrementAndGet();
    }

    public long getReceivedMessagesCount() {
        return messagesReceived.get();
    }

    public long getReceivedMessagesCount(String username) {
        AtomicLong atomicLong;
        if ((atomicLong = usersMessageReceived.get(username))==null)
            return 0;
        else
            return atomicLong.get();
    }

    public long getMutedMessagesIn() {
        return messagesMutedIngoing.get();
    }
    public long getMutedMessagesOut() {
        return messagesMutedOutgoing.get();
    }

    public void newMutedMessagesIn() {
        messagesMutedIngoing.incrementAndGet();
    }

    public void newMutedMessagesOut() {
        messagesMutedOutgoing.incrementAndGet();
    }

    public String getJID(String localUser) {
        builder.setLength(0);
        return builder.append(localUser).append("@").append(xmppServerName).toString();
    }

    public String getGlobohUsername() {
        return globohUsername;
    }

    public String getGlobohPassword() {
        return globohPassword;
    }

    public String getMutedUsersString(){
        builder.setLength(0);
        for (String jid : mutedJIDs){
            builder.append("\n\t\t");
            builder.append(jid);
        }
        builder.append("\n\t");
        return builder.toString();
    }

    public String getMultiplexedUsersString(){
        builder.setLength(0);
        for (String jid : usersToServerMultiplexedMap.keySet()){
            builder.append("\n\t\t");
            builder.append(jid);
            builder.append("->");
            builder.append(usersToServerMultiplexedMap.get(jid));
        }
        builder.append("\n\t");
        return builder.toString();
    }

    public void newUser(String username){
        if(usersAccess.keySet().contains(username)){
            usersAccess.get(username).incrementAndGet();
        }else{
            usersAccess.put(username,new AtomicLong(0));
        }
    }

    public double getAverageMessageSentByUser(){
        return messagesSent.get()/(usersAccess.keySet().size()==0?1:usersAccess.keySet().size());
    }
    public double getAverageMessageReceivedByUser(){
        return messagesReceived.get()/(usersAccess.keySet().size()==0?1:usersAccess.keySet().size());
    }
    public double getMedianMessageSentByUser(){

        return getMedian(usersMessageSent.values());
    }
    public double getMedianMessageReceivedByUser(){
        return getMedian(usersMessageReceived.values());
    }

    private double getMedian(Collection<AtomicLong> collection){
        Iterator<AtomicLong> longIterator = collection.iterator();
        long array[] = new long[collection.size()];
        int pos = 0;
        while(longIterator.hasNext()){
            array[pos++] = longIterator.next().get();
        }
        Arrays.sort(array);

        if (array.length % 2 == 0) {
            if (array.length == 0) return 0;
            return ((double) array[array.length / 2] + (double) array[array.length / 2 - 1]) / 2;
        }
        else
            return array[array.length/2];
    }

}
