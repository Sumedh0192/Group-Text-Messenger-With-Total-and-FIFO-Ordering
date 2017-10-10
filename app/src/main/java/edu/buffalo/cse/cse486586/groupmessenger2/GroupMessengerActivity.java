package edu.buffalo.cse.cse486586.groupmessenger2;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Timer;
import java.util.TimerTask;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 *
 * @author stevko
 *
 */
public class GroupMessengerActivity extends Activity {

    static final String TAG = GroupMessengerActivity.class.getSimpleName();
    static final String[] PORT_NUMBERS = {"11108", "11112","11116","11120","11124"};
    static final int SERVER_PORT = 10000;
    static Integer messageInsertionNumber = 0;
    private final Uri pa2Uri = buildUri("content", "edu.buffalo.cse.cse486586.groupmessenger2.provider");
    static PriorityQueue<MessageClass> messagePriorityQueue = new PriorityQueue<MessageClass>(10, new Comparator<MessageClass>() {
        @Override
        public int compare(MessageClass lhs, MessageClass rhs) {
            if(lhs.prioritySequence.compareTo(rhs.prioritySequence) == 0){
                return Integer.parseInt(lhs.portNumber) - Integer.parseInt(rhs.portNumber);
            } else {
                return lhs.prioritySequence - rhs.prioritySequence;
            }
        }
    });
    static Integer counter, sequenceNumber;
    static Map<String, MessageClass> backUpQueueMap = new HashMap<String, MessageClass>();
    static Map<String, Integer> proposalCountMap = new HashMap<String, Integer>();
    static Map<String, Integer> maximumProposedPriorityMap = new HashMap<String, Integer>();
    static Map<String, String> maximumProposedPriorityPortMap = new HashMap<String, String>();
    static Map<String, Timer> proposalTimerMap = new HashMap<String, Timer>();
    static Map<String, Timer> agreementTimerMap = new HashMap<String, Timer>();
    static Map<String, Boolean> disableProposalTimeOut = new HashMap<String, Boolean>();
    static Map<String, Boolean> disableAgreementTimeOut = new HashMap<String, Boolean>();
    static final int PROPOSAL_TIMEOUT = 7000;
    static final int AGREEMENT_TIMEOUT = 10000;

    private Uri buildUri(String scheme, String authority) {

        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(authority);
        uriBuilder.scheme(scheme);
        return uriBuilder.build();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));
        counter = sequenceNumber = 0;

        /*
         * TODO: Use the TextView to display your messages. Though there is no grading component
         * on how you display the messages, if you implement it, it'll make your debugging easier.
         */
        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());
        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new MessageReceiver().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {
            Log.e(TAG, "Can't create a ServerSocket");
            Log.e(TAG,"Exception-->  " + e.getMessage());
            Log.e(TAG,"Exception--->  " + e.getStackTrace());
            return;
        }
        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */

        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));

        final Button sendButton = (Button)findViewById(R.id.button4);

        sendButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                EditText messageText = (EditText) findViewById(R.id.editText1);
                String messageToSend = messageText.getText().toString();
                messageText.setText("");
                try {
                    new MessageMultiCast().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, new MessageClass(myPort,"", myPort + "_" + (++counter), messageToSend,
                            counter, false, false, false));
                }catch (Exception e){
                    Log.d(TAG,e.getMessage() + "  " + e.getStackTrace());
                }
            }
        });

        /*
         * TODO: You need to register and implement an OnClickListener for the "Send" button.
         * In your implementation you need to get the message from the input box (EditText)
         * and send it to other AVDs.
         */
    }

    private class MessageReceiver extends AsyncTask<ServerSocket, MessageClass, Void> {

        @Override
        protected Void doInBackground(ServerSocket... sockets) {

            ServerSocket serverSocket = sockets[0];
            Socket listner = null;
            while(!serverSocket.isClosed()) {
                try {

                    listner = serverSocket.accept();
                    ObjectInputStream receivedObject = new ObjectInputStream(listner.getInputStream());
                    MessageClass receivedMessage;
                    if(receivedObject != null) {
                        receivedMessage = (MessageClass) receivedObject.readObject();
                        if(!receivedMessage.message.equals("")) {

                            Timer agreementTimer = new Timer();
                            agreementTimer.schedule(new FailureHandler(receivedMessage.messageId, false), AGREEMENT_TIMEOUT);
                            agreementTimerMap.put(receivedMessage.messageId, agreementTimer);
                            disableAgreementTimeOut.put(receivedMessage.messageId, false);

                            backUpQueueMap.put(receivedMessage.messageId, receivedMessage);
                            messagePriorityQueue.add(receivedMessage);
                            publishProgress(new MessageClass(receivedMessage.senderPortNumber,receivedMessage.portNumber,
                                    receivedMessage.messageId,"", ++sequenceNumber, true, false, false));

                        } else if(receivedMessage.isProposal == true) {

                            if(!proposalTimerMap.containsKey(receivedMessage.messageId)){
                                Timer proposalTimer = new Timer();
                                proposalTimer.schedule(new FailureHandler(receivedMessage.messageId, true), PROPOSAL_TIMEOUT);
                                proposalTimerMap.put(receivedMessage.messageId, proposalTimer);
                                disableProposalTimeOut.put(receivedMessage.messageId, false);
                            }

                            if(!proposalCountMap.containsKey(receivedMessage.messageId)){
                                proposalCountMap.put(receivedMessage.messageId,1);
                            } else {
                                proposalCountMap.put(receivedMessage.messageId, proposalCountMap.get(receivedMessage.messageId) + 1);
                            }

                            if(!maximumProposedPriorityMap.containsKey(receivedMessage.messageId)){
                                maximumProposedPriorityMap.put(receivedMessage.messageId,receivedMessage.prioritySequence);
                                maximumProposedPriorityPortMap.put(receivedMessage.messageId,receivedMessage.portNumber);
                            } else {
                                if(receivedMessage.prioritySequence > maximumProposedPriorityMap.get(receivedMessage.messageId)){
                                    maximumProposedPriorityMap.put(receivedMessage.messageId,receivedMessage.prioritySequence);
                                    maximumProposedPriorityPortMap.put(receivedMessage.messageId,receivedMessage.portNumber);
                                } else if (receivedMessage.prioritySequence.compareTo(maximumProposedPriorityMap.get(receivedMessage.messageId)) == 0) {
                                    if (Integer.parseInt(receivedMessage.portNumber) > Integer.parseInt(maximumProposedPriorityPortMap.get(receivedMessage.messageId))) {
                                        maximumProposedPriorityPortMap.put(receivedMessage.messageId, receivedMessage.portNumber);
                                    }
                                }
                            }

                            if(proposalCountMap.get(receivedMessage.messageId) == PORT_NUMBERS.length){

                                if(!disableProposalTimeOut.get(receivedMessage.messageId)) {
                                    Timer proposalTimer = proposalTimerMap.get(receivedMessage.messageId);
                                    proposalTimer.cancel();
                                    proposalTimer.purge();
                                    disableProposalTimeOut.put(receivedMessage.messageId, true);
                                    proposalTimerMap.remove(receivedMessage.messageId);

                                    publishProgress(new MessageClass(receivedMessage.senderPortNumber, maximumProposedPriorityPortMap.get(receivedMessage.messageId),
                                            receivedMessage.messageId, "", maximumProposedPriorityMap.get(receivedMessage.messageId), false, true, false));
                                }
                            }

                        } else if(receivedMessage.isAgreement == true) {

                            if(!disableAgreementTimeOut.get(receivedMessage.messageId)) {
                                Timer agreementTimer = agreementTimerMap.get(receivedMessage.messageId);
                                agreementTimer.cancel();
                                agreementTimer.purge();
                                agreementTimerMap.remove(receivedMessage.messageId);
                                disableAgreementTimeOut.put(receivedMessage.messageId, true);

                                messagePriorityQueue.remove(backUpQueueMap.get(receivedMessage.messageId));
                                receivedMessage.deliver = true;
                                receivedMessage.isAgreement = false;
                                receivedMessage.message = backUpQueueMap.get(receivedMessage.messageId).message;
                                backUpQueueMap.remove(receivedMessage.messageId);
                                messagePriorityQueue.add(receivedMessage);
                                publishProgress(receivedMessage);
                            }

                        }
                    }
                } catch (Exception ex) {
                    Log.e(TAG,"Error in Receiving the message");
                    Log.e(TAG,"Exception-->  " + ex.getMessage());
                    Log.e(TAG,"Exception--->  " + ex.getStackTrace());
                    return null;
                } finally {
                    try {
                        listner.close();
                    } catch (IOException ex) {
                        Log.e(TAG,"Exception-->  " + ex.getMessage());
                        Log.e(TAG,"Exception--->  " + ex.getStackTrace());
                        return null;
                    }
                }
            }
            try {
                serverSocket.close();
            }catch(IOException ex){
                Log.e(TAG,"Exception-->  " + ex.getMessage());
                Log.e(TAG,"Exception--->  " + ex.getStackTrace());
            }
            return null;
        }

        protected void onProgressUpdate(MessageClass...msgs) {

            MessageClass messageToProcess = msgs[0];
            if(messageToProcess.isProposal == true){
                new SingleCast().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, messageToProcess);
            } else if(messageToProcess.isAgreement == true) {
                new MessageMultiCast().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, messageToProcess);
            } else if(messageToProcess.deliver == true) {
                storeDeliveredMessages();
            }
            return;
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }

    private void storeDeliveredMessages(){

        TextView remoteTextView = (TextView) findViewById(R.id.textView1);
        Iterator<MessageClass> it = messagePriorityQueue.iterator();

        if(messagePriorityQueue.size() > 0) {
            MessageClass head = messagePriorityQueue.peek();
            while (head.deliver == true) {

                remoteTextView.append(head.message + "\n");
                ContentValues conVal = new ContentValues();
                conVal.put("key", messageInsertionNumber++);
                conVal.put("value", head.message);
                getContentResolver().insert(pa2Uri, conVal);
                messagePriorityQueue.remove();
                if (messagePriorityQueue.size() > 0) {
                    head = messagePriorityQueue.peek();
                } else {
                    break;
                }
            }
        }
    }


    private class MessageMultiCast extends AsyncTask<MessageClass, Void, Void> {

        @Override
        protected Void doInBackground(MessageClass... msgs) {

            Socket socket = null;
            ObjectOutputStream out = null;
            String remotePort = "";
            MessageClass message = msgs[0];

            for(Integer i = 0; i < PORT_NUMBERS.length; i++) {
                try {
                    remotePort = PORT_NUMBERS[i];
                    if(!message.isAgreement)
                        message.portNumber = remotePort;
                    socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(remotePort));
                    out = new ObjectOutputStream(socket.getOutputStream());
                    out.writeObject(message);
                    out.flush();

                } catch (UnknownHostException e) {
                    Log.e(TAG, "SendTask UnknownHostException");
                    Log.d(TAG,"Exception: " + e.getStackTrace() + "  " + e.getMessage());

                } catch (IOException e) {
                    Log.e(TAG, "SendTask socket IOException");
                    Log.d(TAG,"Exception: " + e.getStackTrace() + "  " + e.getMessage());

                } finally {
                    try {
                        out.close();
                        socket.close();
                    } catch (IOException ex) {
                        Log.d(TAG,"Exception-->  " + ex.getMessage());
                        Log.d(TAG,"Exception--->  " + ex.getStackTrace());
                    }
                }
            }
            return null;
        }
    }

    private class SingleCast extends AsyncTask<MessageClass, Void, Void> {

        @Override
        protected Void doInBackground(MessageClass... msgs) {

            Socket socket = null;
            ObjectOutputStream out = null;
            String remotePort = "";

            try {
                remotePort = msgs[0].senderPortNumber;
                socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                        Integer.parseInt(remotePort));
                out = new ObjectOutputStream(socket.getOutputStream());
                out.writeObject(msgs[0]);
                out.flush();

            } catch (UnknownHostException e) {
                Log.e(TAG, "SendTask UnknownHostException");
                Log.d(TAG,"Exception: " + e.getStackTrace() + "  " + e.getMessage());

            } catch (IOException e) {
                Log.e(TAG, "SendTask socket IOException");
                Log.d(TAG,"Exception: " + e.getStackTrace() + "  " + e.getMessage());

            } finally {
                try {
                    out.close();
                    socket.close();
                } catch (IOException ex) {
                    Log.d(TAG,"Exception-->  " + ex.getMessage());
                    Log.d(TAG,"Exception--->  " + ex.getStackTrace());
                }
            }
            return null;
        }
    }

    private class FailureHandler extends TimerTask{

        String messageId;
        Boolean isProposalFailure;

        FailureHandler(String messageId, Boolean isProposalFailure){

            this.messageId = messageId;
            this.isProposalFailure = isProposalFailure;
        }

        @Override
        public void run(){


            if(isProposalFailure){
                try {
                    if(!disableProposalTimeOut.get(messageId)) {
                        disableProposalTimeOut.put(messageId,true);
                        new MessageMultiCast().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, new MessageClass(messageId.substring(0, messageId.indexOf("_")),
                                maximumProposedPriorityPortMap.get(messageId), messageId, "", maximumProposedPriorityMap.get(messageId), false, true, false));
                    }
                }catch (NullPointerException ex){
                    Log.e(TAG, "Exception--->  " + ex.getMessage());
                }

            } else {
                if(!disableAgreementTimeOut.get(messageId)) {

                    disableAgreementTimeOut.put(messageId,true);
                    Iterator<MessageClass> messageIterator = messagePriorityQueue.iterator();
                    while(messageIterator.hasNext()){
                        MessageClass message = messageIterator.next();
                        if(message.senderPortNumber.equals(messageId.substring(0, messageId.indexOf("_"))) && !message.deliver){
                            messagePriorityQueue.remove(message);
                            backUpQueueMap.remove(message.messageId);
                        }
                    }
                    storeDeliveredMessages();
                }
            }
        }
    }
}