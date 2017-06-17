package go.smail.android.smail;

/**
 * Created by Patrik on 01/12/2016.
 */
public class Message {
    private String text;
    private boolean user; // true :- user , false : chatbot
    private String id;

    public Message(String text, boolean user, String id) {
        this.text = text;
        this.user = user;
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public boolean isUser() {
        return user;
    }

    public void setUser(boolean user) {
        this.user = user;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
