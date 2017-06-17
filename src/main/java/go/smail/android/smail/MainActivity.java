package go.smail.android.smail;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Handler;
import android.support.annotation.IntegerRes;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;

import go.smail.android.smail.volley.AppController;

import static android.R.attr.id;
import static android.icu.lang.UCharacter.GraphemeClusterBreak.L;
import static android.icu.lang.UCharacter.GraphemeClusterBreak.V;
import static android.os.Build.VERSION_CODES.M;
import static go.smail.android.smail.R.drawable.send;


public class MainActivity extends AppCompatActivity {
    String hostname ;
    String uuid ="";
    EditText sendMessage;
    String message="";
    Map<String, String> params;
    ListView LvConversation;
    ListAdapter LaConversation;
    List<Message> conversation;
    String lastMessage = "";
    String [][] emails= new String[5][2];
    boolean [] valid = new boolean[5];
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sendMessage = (EditText) findViewById(R.id.sendMessage);
        params = new HashMap<String,String>();
        hostname=getString(R.string.hostname);
        sendMessage("welcome");
        //Bundle bundle = getIntent().getExtras();
        LvConversation =(ListView)findViewById(R.id.message_list);
        conversation = new ArrayList<Message>();

        LaConversation = new ListAdapter(conversation,getApplicationContext());
        LvConversation.setAdapter(LaConversation);
    }

    private void scrollMyListViewToBottom() {
        LvConversation.post(new Runnable() {
            @Override
            public void run() {
                // Select the last row so it will scroll into view...
                LvConversation.setSelection(LaConversation.getCount() - 1);
            }
        });
    }

    public String sendMessage(String route){
        String url = hostname+route;

        JsonObjectRequest welcomeRequest = new JsonObjectRequest(url,new JSONObject(params),new Response.Listener<JSONObject>(){

            @Override
            public void onResponse(JSONObject response) {
                try{
                    String id = "welcome";
                    if(response.has("uuid"))
                        uuid = response.getString("uuid");
                    message = response.getString("message");
                    Log.i("Message Respone",message);
                    if(message.equals("Invalid message please enter a valid one")){
                        String tmp = "I am sorry didn't catch that one\n" +
                                "You can tell me things like\n\n" +
                                "- Get My Emails\n" +
                                "- Send an Email\n" +
                                "- Delete an Email\n" +
                                "- Trash an Email\n" +
                                "- Summarize an Email";
                        conversation.add(new Message(tmp, false, id));
                        LaConversation = new ListAdapter(conversation,getApplicationContext());
                        LvConversation.setAdapter(LaConversation);
                        scrollMyListViewToBottom();
                        return;
                    }
                    if(lastMessage.equals("signin")){
                        Toast.makeText(getApplicationContext(),"Redirecting for authentication...",Toast.LENGTH_LONG).show();
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(message));
                        startActivity(intent);
                        return;
                    }
                    if(message.startsWith("{\n  \"sentences\"")){
                        JSONObject messageArray = new JSONObject(message);
                        JSONArray sentences = messageArray.getJSONArray("sentences");
                        message ="";
                        if(sentences.length() == 0)
                            conversation.add(new Message("You have no unread mails", false,"chatbot"));

                        for(int i =0;i<sentences.length();i++){
                            message += sentences.get(i).toString();
                            message += (i != sentences.length()-1)?"\n":"";
                        }
                        conversation.add(new Message(message, false,"chatbot"));
                    }
                    else if(message.startsWith("{\"Messages\"")){
                        JSONObject messageArray = new JSONObject(message);
                        JSONArray mails = messageArray.getJSONArray("Messages");
                        message ="";
                        if(mails.length() == 0)
                            conversation.add(new Message("You have no unread mails", false,"chatbot"));

                        for(int i =0;i<mails.length();i++){

                            JSONObject mail = mails.getJSONObject(i);
                            message+=i+1+".\n";
                            message +="  __________From___________\n\n"+mail.getString("From");
                            message +="\n\n  __________Subject___________\n\n"+mail.getString("Subject");
                            message +="\n\n  ____________Body__________\n\n"+mail.getString("Body");
                            conversation.add(new Message(message, false, mail.getString("MsgID")));
                            message ="";
                            emails[i][0]=mail.getString("MsgID");
                            emails[i][1]=mail.getString("Body");
                            valid[i]=true;

                        }


                    }else{
                        conversation.add(new Message(message, false,id));
                    }

                    //displayedMessage.setText(message);
                    //Log.i("Welcome Message",message);

                    LaConversation = new ListAdapter(conversation,getApplicationContext());
                    LvConversation.setAdapter(LaConversation);
                    scrollMyListViewToBottom();
                }catch(JSONException e){
                    Log.e("JSONFORMAT",e.getMessage());
                }
            }
        },new Response.ErrorListener(){

            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("json", "failed volley");
            }
        }){
            @Override
            protected Map<String, String> getParams()  {
                return params;
            }
            public Map<String, String> getHeaders() {
                Map<String,String> mHeaders= new HashMap<String,String>();
                mHeaders.put("Authorization",uuid);
                return mHeaders;

            }
        };

        Log.i("APP Instance", String.valueOf(AppController.getInstance()));
        AppController.getInstance().addToRequestQueue(welcomeRequest);
        return message;
    }

    public int whichMail(String command){
        int number = -1;
        for (int i=0;i<command.length();i++){
            if(Character.isDigit(command.charAt(i))) {
                number = Integer.parseInt(command.charAt(i) + "");
                break;
            }
        }
        if(number == -1){
            if(command.contains("first"))
                number = 1;
            else if(command.contains("second"))
                number = 2;
            else if(command.contains("third"))
                number = 3;
            else if(command.contains("fourth"))
                number = 4;
            else if(command.contains("fifth"))
                number = 5;
            else if(command.contains("last"))
                for(int i =0;i<valid.length;i++){
                    if(valid[i])
                        number = i+1;
                }
        }
        return number;
    }

    public void send(View view){

        String id = "user";
        params = new HashMap<String,String>();
        if(!lastMessage.equals("signin")){
            conversation.add(new Message(sendMessage.getText().toString(), true, id));
        }
        String command = sendMessage.getText().toString().toLowerCase();
        Log.i("STRinf ", command);
        if(lastMessage.equals("signin")){
            params.put("message",sendMessage.getText().toString());
            lastMessage = "end";
            sendMessage("chat");
        }
        else if(command.contains("sign")){
            params.put("message","signin");
            lastMessage = "signin";
            sendMessage("chat");
        }
        else if(lastMessage.equals("end")&&command.contains("send")){
            params.put("message","Send an Email");
            lastMessage = "Send an Email";
            sendMessage("chat");
        }
        else if(lastMessage.equals("end")&&command.contains("emails") && (command.contains("get") || command.contains("list") || command.contains("show") || command.contains("see"))){
            Log.i("LIST0","Get my emails");
            params.put("message","Get My Emails");
            lastMessage = "end";
            sendMessage("chat");
        }
        else if((lastMessage.equals("Trash an Email")||lastMessage.equals("end"))&&command.contains("trash")){
            if(whichMail(command) != -1){
                int mailIndex = whichMail(command);
                mailIndex--;
                Log.i("index", mailIndex+"");
                if(mailIndex>=5||!valid[mailIndex] || mailIndex == -1){
                    lastMessage="end";
                    conversation.add(new Message("This mail is not here!",false,"chatbot"));
                }else{
                    lastMessage="end";
                    params.put("message",emails[mailIndex][0]);
                    sendMessage("chat");
                }
            }else{
                params.put("message","Trash an Email");
                lastMessage = "Trash an Email";
                sendMessage("chat");
            }
        }
        else if((lastMessage.equals("Delete an Email")||lastMessage.equals("end"))&&command.contains("delete") || command.contains("remove")){
            if(whichMail(command) != -1){
                int mailIndex = whichMail(command);
                mailIndex--;
                if(mailIndex>=5||!valid[mailIndex] || mailIndex == -1){
                    lastMessage="end";
                    conversation.add(new Message("This mail is not here!",false,"chatbot"));
                }else{
                    lastMessage="end";
                    params.put("message",emails[mailIndex][0]);
                    sendMessage("chat");
                }
            }else{
                params.put("message","Delete an Email");
                lastMessage = "Delete an Email";
                sendMessage("chat");
            }
        }
        else if((lastMessage.equals("Summarize an Email")||lastMessage.equals("end"))&&command.contains("summar")){
            int number = whichMail(command);
            if(number != -1){
                lastMessage="end";
                int mailIndex = whichMail(command);
                mailIndex--;
                if(mailIndex>=5||!valid[mailIndex] || mailIndex == -1){
                    conversation.add(new Message("This mail is not here!",false,"chatbot"));
                }else{
                    params.put("message",emails[mailIndex][1]);
                    sendMessage("chat");
                }
            }else{
                params.put("message","Summarize an Email");
                lastMessage = "Summarize an Email";
                sendMessage("chat");
            }
        }
        else{
           switch (lastMessage){
               case "Send an Email":
                   lastMessage="Email";
                   break;
               case "Email":
                   lastMessage="Subject";
               break;
               default:lastMessage="end";
           }
            params.put("message",sendMessage.getText().toString());
            sendMessage("chat");
        }
        //------------------------------------------------------------------------------------------------------
//
//        String id = "user";
//        params = new HashMap<String,String>();
//        if(!lastMessage.equals("signin")){
//            conversation.add(new Message(sendMessage.getText().toString(), true, id));
//        }
//        if(!sendMessage.getText().toString().equals("Trash an Email") && sendMessage.getText().toString().startsWith("Trash")){
//            int mailIndex =Integer.parseInt(sendMessage.getText().toString().charAt(6)+"");
//            mailIndex--;
//            Log.i("index", mailIndex+"");
//            if(mailIndex>=5||!valid[mailIndex]){
//                conversation.add(new Message("This mail is not here!",false,"chatbot"));
//            }else{
//                params.put("message",emails[mailIndex][0]);
//                sendMessage("chat");
//            }
//        }else{
//            if(!sendMessage.getText().toString().equals("Delete an Email") && sendMessage.getText().toString().startsWith("Delete")){
//                int mailIndex =Integer.parseInt(sendMessage.getText().toString().charAt(7)+"");
//                mailIndex--;
//                if(mailIndex>=5||!valid[mailIndex]){
//                    conversation.add(new Message("This mail is not here!",false,"chatbot"));
//                }else{
//                    params.put("message",emails[mailIndex][0]);
//                    sendMessage("chat");
//                }
//
//            }else{
//                params.put("message",sendMessage.getText().toString());
//                sendMessage("chat");
//            }
//
//        }

        LaConversation = new ListAdapter(conversation,getApplicationContext());
        LvConversation.setAdapter(LaConversation);
        scrollMyListViewToBottom();
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {

            }
        }, 500);
        //lastMessage=sendMessage.getText().toString();
        sendMessage.setText("");
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);

    }
}
