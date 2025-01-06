package app.shashi.AdminTalk.models;

public class Message {
    private String text;
    private long timestamp;
    private String senderId;
    private String senderName;

    public Message() {
        
    }

    public Message(String text, long timestamp, String senderId, String senderName) {
        this.text = text;
        this.timestamp = timestamp;
        this.senderId = senderId;
        this.senderName = senderName;
    }

    
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }
}