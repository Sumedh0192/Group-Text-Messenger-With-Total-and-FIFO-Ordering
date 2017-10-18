################# Advanced Group Messenger ################

### Description: 
A Group Text Messenger which maintains a TOTAL and FIFO ordering of all the messages sent.

### Implementation:
* The application is launched on 5 different AVDs each with a specified port.
* Each application has a server port which is constantly active to receive messages and a client which sends messages to the other AVDs.
* Messages are sent over the devices using TCP protocol.
* The Client and Server ports are implemented using Java Sockets and Java AsycTasks.
* The Client AVD sends accross message to the other 4 AVDs.
* Once a message is received by a Server AVD it saves the message in its local database as a key value pair using SQLiteDatabase.
* A FIFO and TOTAL ordering is maintained for all the messages at every AVD to provide a sequencial storage of messages and avoid any inconsistency.
* The messenger is also capable of handling device failures and restoring all the lost messages along with previously stored data in the appropriate sequence, when the device is reactivated.
* The messages are diplayed on the screen of every AVD in the order expected.

### Link to the Code files:
https://github.com/Sumedh0192/Group-Text-Messenger-With-Total-and-FIFO-Ordering/blob/master/app/src/main/java/edu/buffalo/cse/cse486586/groupmessenger2/

### Link to official project description:
https://github.com/Sumedh0192/Group-Text-Messenger-With-Total-and-FIFO-Ordering/blob/master/PA%202%20Part%20B%20Specification.pdf


