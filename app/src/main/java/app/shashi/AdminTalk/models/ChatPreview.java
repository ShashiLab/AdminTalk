package app.shashi.AdminTalk.models;

public class ChatPreview {
    private User user;
    private Message lastMessage;
    private long timestamp;

    public ChatPreview() {
        
    }

    public ChatPreview(User user, Message lastMessage, long timestamp) {
        this.user = user;
        this.lastMessage = lastMessage;
        this.timestamp = timestamp;
    }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Message getLastMessage() { return lastMessage; }
    public void setLastMessage(Message lastMessage) { this.lastMessage = lastMessage; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}