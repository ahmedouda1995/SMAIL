package go.smail.android.smail;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Patrik on 01/12/2016.
 */
public class ListAdapter extends BaseAdapter {
    List<Message> conversation;
    Context mContext;

    public ListAdapter(List<Message> conversation, Context mContext) {
        this.conversation = conversation;
        this.mContext = mContext;
    }

    @Override
    public int getCount() {
        return conversation.size();
    }

    @Override
    public Object getItem(int position) {
        return conversation.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v;
        if(conversation.get(position).isUser()){
            v = View.inflate(mContext,R.layout.user_message,null);
            TextView chatElement = (TextView)v.findViewById(R.id.chatElementUser);
            chatElement.setText(conversation.get(position).getText());
            v.setTag(conversation.get(position).getId());
        }
        else{
            v = View.inflate(mContext,R.layout.chatbot_message,null);
            TextView chatElement = (TextView)v.findViewById(R.id.chatElementChatbot);
            chatElement.setText(conversation.get(position).getText());
            v.setTag(conversation.get(position).getId());
        }
        return v;
    }
}
