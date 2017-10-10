package edu.buffalo.cse.cse486586.groupmessenger2;

import java.io.Serializable;

/**
 * Created by sumedh on 3/26/17.
 */

public class MessageClass implements Serializable{

    public String senderPortNumber;
    public String portNumber;
    public String messageId;
    public String message;
    public Integer prioritySequence;
    public Boolean isAgreement;
    public Boolean isProposal;
    public Boolean deliver;

    public MessageClass(String senderPortNumber, String portNumber, String messageId, String message, Integer prioritySequence, Boolean isProposal, Boolean isAgreement, Boolean deliver){
        this.senderPortNumber = senderPortNumber;
        this.portNumber = portNumber;
        this.messageId = messageId;
        this.message = message;
        this.prioritySequence = prioritySequence;
        this.isAgreement = isAgreement;
        this.isProposal = isProposal;
        this.deliver = deliver;
    }

}
