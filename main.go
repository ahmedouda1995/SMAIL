
package main

import (
  "encoding/base64"
  "encoding/json"
  "fmt"
  "io/ioutil"
  "log"
  "net/http"
  // "net/url"
  "os"
  // "os/user"
  "math/rand"
  // "path/filepath"
  //"sort"
  "strings"
  "strconv"
  //"bufio"
  "github.com/ramin0/chatbot"
  "golang.org/x/net/context"
  "golang.org/x/oauth2"
  "golang.org/x/oauth2/google"
  "google.golang.org/api/gmail/v1"
)
import _ "github.com/joho/godotenv/autoload"

// getClient uses a Context and Config to retrieve a Token
// then generate a Client. It returns the generated Client.
type message struct {
  From string
  MsgID string
  Date    string // retrieved from message header
  Body string
  Subject string
}
type messagesList struct{
  Messages []message
}
var(
clients =map[string]*http.Client{}
)
func sendMessage(client *http.Client,to string, subject string,body string ) string{

  svc, err := gmail.New(client)
  if err != nil {
    log.Fatalf("Unable to create Gmail service: %v", err)
  }


  var message gmail.Message

  temp := []byte("From: 'me'\r\n" +
    "To:  "+to+"\r\n" +
    "Subject: "+subject+" \r\n" +
    "\r\n "+body)

  message.Raw = base64.StdEncoding.EncodeToString(temp)
  message.Raw = strings.Replace(message.Raw, "/", "_", -1)
  message.Raw = strings.Replace(message.Raw, "+", "-", -1)
  message.Raw = strings.Replace(message.Raw, "=", "", -1)
    _,er := svc.Users.Messages.Send("me",&message).Do();
  if er != nil {
    log.Fatalf("Unable to send. %v", er)
    return "Sorry couldn't send Message"
  }
  return "Message Sent"
}
func listMessages(client *http.Client) string {
 svc, err := gmail.New(client)
  if err != nil {
    log.Fatalf("Unable to create Gmail service: %v", err)
  }


  msgs := []message{}
 req := svc.Users.Messages.List("me").Q("is:unread").MaxResults(5)

r, err := req.Do()
if(err!=nil){
  fmt.Println(err)
  return ""
}
for _, m := range r.Messages {
 date :=""
 body :=""
 from :=""
 subject :=""

  msg, _ := svc.Users.Messages.Get("me", m.Id).Format("full").Do()
 for _, h := range msg.Payload.Headers {
        if h.Name == "Date" {
          date = h.Value
        }
        if h.Name == "From"{
          from = h.Value
        }
        if h.Name == "Subject" {
          subject = h.Value
        }
      }
  for _, part := range msg.Payload.Parts {

    if part.MimeType == "text/plain" {
      part.Body.Data = strings.Replace(part.Body.Data, "/", "_", -1)
      part.Body.Data = strings.Replace(part.Body.Data, "+", "-", -1)
      part.Body.Data = strings.Replace(part.Body.Data, "=", "*", -1)
      data, _ := base64.StdEncoding.DecodeString(part.Body.Data)
      body = string(data)

    }
  }
  msgs = append(msgs, message{
        From:    from,
        MsgID: msg.Id,
        Date:    date,
        Subject: subject,
        Body : body,
      })
}
msgsObj :=&messagesList{Messages:msgs}
messagesJSON, _ := json.Marshal(msgsObj)
return string(messagesJSON)
}
func trashMessage(client *http.Client,id string) bool{
   svc, err := gmail.New(client)
   if err != nil {
     log.Fatalf("Unable to create Gmail service: %v", err)
   }
   req ,er:=svc.Users.Messages.Trash("me",id).Do();
   if er != nil {
    log.Fatalf("Error in trashing message")
    return false
   }
   fmt.Println(req)
   return true
}
func deleteMessage(client *http.Client,id string)bool{
   deleted:=true
   svc, err := gmail.New(client)
   if err != nil {
     log.Fatalf("Unable to create Gmail service: %v", err)
   }
   er:=svc.Users.Messages.Delete("me",id).Do();
   if er != nil {
    log.Fatalf("Error in deleting message")
    deleted= false
   }
   return deleted

}
func summarizeEmail(sentences int,text string) string{
  url := "https://textanalysis-text-summarization.p.mashape.com/text-summarizer-text"

  payload := strings.NewReader("sentnum="+strconv.Itoa(sentences)+"&text="+text)

  req, _ := http.NewRequest("POST", url, payload)

  req.Header.Add("x-mashape-key", "rDCh6CQU2bmshv2zF5gkYzw3sG3jp16Uw7QjsndcbJD6lp0Lgi")
  req.Header.Add("content-type", "application/x-www-form-urlencoded")
  req.Header.Add("accept", "application/json")



  res, _ := http.DefaultClient.Do(req)

  defer res.Body.Close()
  body, _ := ioutil.ReadAll(res.Body)


  return string(body)
}

// func getClient(ctx context.Context, config *oauth2.Config) *http.Client {
//   cacheFile, err := tokenCacheFile()
//   if err != nil {
//     log.Fatalf("Unable to get path to cached credential file. %v", err)
//   }
//   tok, err := tokenFromFile(cacheFile)
//   if err != nil {
//     tok = getTokenFromWeb(config)
//     saveToken(cacheFile, tok)
//   }
//   return config.Client(ctx, tok)
// }

// getTokenFromWeb uses Config to request a Token.
// It returns the retrieved Token.
func getTokenFromWeb(config *oauth2.Config) string {

  authURL := config.AuthCodeURL("state-token", oauth2.AccessTypeOnline)
  // fmt.Printf("Go to the following link in your browser then type the "+
  //   "authorization code: \n%v\n", authURL)
  //
  // var code string
  // if _, err := fmt.Scan(&code); err != nil {
  //   log.Fatalf("Unable to read authorization code %v", err)
  // }
  //
  // tok, err := config.Exchange(oauth2.NoContext, code)
  // if err != nil {
  //   log.Fatalf("Unable to retrieve token from web %v", err)
  // }
  return authURL
}

// tokenCacheFile generates credential file path/filename.
// It returns the generated credential path/filename.
// func tokenCacheFile() (string, error) {
//   usr, err := user.Current()
//   if err != nil {
//     return "", err
//   }
//   tokenCacheDir := filepath.Join(usr.HomeDir, ".credentials")
//   os.MkdirAll(tokenCacheDir, 0700)
//   return filepath.Join(tokenCacheDir,
//     url.QueryEscape("gmail-go-quickstart.json")), err
// }

// tokenFromFile retrieves a Token from a given file path.
// It returns the retrieved Token and any read error encountered.
// func tokenFromFile(file string) (*oauth2.Token, error) {
//   f, err := os.Open(file)
//   if err != nil {
//     return nil, err
//   }
//   t := &oauth2.Token{}
//   err = json.NewDecoder(f).Decode(t)
//   defer f.Close()
//   return t, err
// }
//
// // saveToken uses a file path to create a file and store the
// // token in it.
// func saveToken(file string, token *oauth2.Token) {
//   fmt.Printf("Saving credential file to: %s\n", file)
//   f, err := os.Create(file)
//   if err != nil {
//     log.Fatalf("Unable to cache oauth token: %v", err)
//   }
//   defer f.Close()
//   json.NewEncoder(f).Encode(token)
// }

func chatbotProcess(session chatbot.Session, message string) (string, error) {
  ctx := context.Background()
  b, err1 := ioutil.ReadFile("client_secret.json")
  if err1 != nil {
    log.Fatalf("Unable to read client secret file: %v", err1)
  }

  config, err2 := google.ConfigFromJSON(b, "https://mail.google.com/")
  if err2 != nil {
    log.Fatalf("Unable to parse client secret file to config: %v", err2)
  }


 if strings.EqualFold(message, "signin") {
   session["history"] = append(session["history"], message)
   url:=getTokenFromWeb(config)
   return url, nil
 }
 lastMessage:=session["history"][len(session["history"])-1]
 if strings.EqualFold(lastMessage, "signin"){
   session["token"] = []string{}
   session["token"]=append(session["token"], message)
   session["history"] = append(session["history"], message)
   return "Signed in using your google Account",nil
 }
 validMap,action:= readAvailableMessages(message)


 if strings.EqualFold(lastMessage,"send"){
   session["history"] = append(session["history"], "to/,/"+message)
   return validMap["to"]["replies"][rand.Intn(len(validMap["to"]["replies"]))] , nil
 }

 if strings.HasPrefix(lastMessage,"to"){
   session["history"] = append(session["history"], "subject/,/"+message)
   fmt.Println(message);
   return validMap["subject"]["replies"][rand.Intn(len(validMap["subject"]["replies"]))] , nil
 }
 code:=session["token"][len(session["token"])-1]
 client, sessionFound := clients[code]
 if !sessionFound {
   tok, _ := config.Exchange(oauth2.NoContext, code)
   client =config.Client(ctx, tok)
   clients[code]=client
 }


fmt.Println(code)
 if strings.EqualFold(action,"list"){
   session["history"] = append(session["history"], action)

   return listMessages(client) , nil
 }

 if !strings.EqualFold(action,"Invalid message please enter a valid one"){
   session["history"] = append(session["history"], action)
   return validMap[action]["replies"][rand.Intn(len(validMap[action]["replies"]))] , nil
 }
 //ready to excute the apis in google
 if strings.EqualFold(lastMessage,"delete"){
   session["history"] = append(session["history"], "END")

    if deleteMessage(client,message) {
      return "Email deleted", nil
    }else{
      return "Sorry couldn't delete message",nil
    }

 }
 if strings.EqualFold(lastMessage,"trash"){
   session["history"] = append(session["history"], "END")

    if trashMessage(client,message) {
      return "Email trashed", nil
    }else{
      return "Sorry couldn't trash message",nil
    }

 }

 if strings.EqualFold(lastMessage,"summarize"){
   session["history"] = append(session["history"], "END")

    return summarizeEmail(3,message),nil
    }
 if strings.HasPrefix(lastMessage,"subject"){
   subject:= strings.Split(lastMessage,"/,/")[1]
   fmt.Println(subject)
   to:=strings.Split(session["history"][len(session["history"])-2],"/,/")[1]
   session["history"] = append(session["history"], "END")

   returnMessage:=sendMessage(client,to,subject,message)
   return returnMessage,nil

 }
 return action, nil
}
func readAvailableMessages(userMessage string) (map[string]map[string][]string,string){
    b, _ := ioutil.ReadFile("messages.json")
  m := map[string]map[string][]string{}
	err := json.Unmarshal(b, &m)
	if err != nil {
		panic(err)
	}
  // for actions, options := range m {
  //        for _,message := range options["messages"] {
  //          if strings.EqualFold(userMessage,message){
  //            return m,actions
  //          }
  //        }
  //    }
  action, actionFound :=m["actions"][userMessage];
  if !actionFound{
     return m,"Invalid message please enter a valid one"
  }
    return m , action[0]

}

func main() {
  chatbot.WelcomeMessage = "Welcome to Smail chatbot, How may I help You?"
  chatbot.ProcessFunc(chatbotProcess)
  // Use the PORT environment variable
  port := os.Getenv("PORT")
  // Default to 3000 if no PORT environment variable was defined
  if port == "" {
   port = "3000"
  }

  // Start the server
  fmt.Printf("Listening on port %s...\n", port)
  log.Fatalln(chatbot.Engage(":" + port))

// fmt.Println(strings.Trim("tosomeone@email.com","to"));
//  fmt.Println(listMessages(client))
 // sendMessage(client,"patrickmounir@gmail.com","Test Send Method","THis is for testing do not reply");
  //  deleteMessage(client,"13b60439dae931be")
 // fmt.Println(summerizeEmail(4,"Automatic summarization is the process of reducing a text document with a computer program in order to create a summary that retains the most important points of the original document. As the problem of information overload has grown, and as the quantity of data has increased, so has interest in automatic summarization. Technologies that can make a coherent summary take into account variables such as length, writing style and syntax. An example of the use of summarization technology is search engines such as Google. Document summarization is another."))
}
