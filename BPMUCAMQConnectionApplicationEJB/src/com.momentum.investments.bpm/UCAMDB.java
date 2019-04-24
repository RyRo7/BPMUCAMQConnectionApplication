package com.momentum.investments.bpm;

/**
 * @Who: Ryan Roberts
 * @When: 2019-04-23 11:43 AM
 */

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.jms.*;
import javax.naming.InitialContext;
import javax.resource.spi.security.PasswordCredential;
import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginContext;
import java.util.HashMap;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Message-Driven Bean implementation class for: UCAMDB
 */
@MessageDriven(
        activationConfig = {
                @ActivationConfigProperty( propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
                @ActivationConfigProperty( propertyName = "destination", propertyValue ="jms/bpmUCAEventQ")
        }
)
public class UCAMDB implements MessageListener {

    private Logger logger = Logger.getLogger("com.momentum.investments.bpm.UCAMDB");

    /**
     * Default constructor.
     */
    public UCAMDB() {
        // TODO Auto-generated constructor stub
    }

    /**
     * @see MessageListener#onMessage(Message)
     */
    public void onMessage(Message message) {
        try {
            TextMessage textMessage = (TextMessage) message;
            String mqMessageText = textMessage.getText();

            logger.info("*** Received MQ Message for UCA Event ["+ mqMessageText +"]");
            InitialContext ctx = new InitialContext();

            QueueConnectionFactory ucaqcf = (QueueConnectionFactory) ctx.lookup("javax.jms.QueueConnectionFactory");
            Queue ucaqueue = (Queue) ctx.lookup("jms/eventqueue");


            //Change this alias to whatever security auth alias allows connectivity to the internal jms/eventqueue
            PasswordCredential passwordCredential = getJ2CData("DeAdminAlias");
            String userName = passwordCredential.getUserName();
            char[] password = passwordCredential.getPassword();

            //TODO Possible password masking?

            logger.fine("*** using creds to connect to internal event queue");
            logger.finest("*** char[] password="+password);
            logger.finest("*** String.copyValueOf(password)="+String.copyValueOf(password));

            QueueConnection connection = ucaqcf.createQueueConnection(userName, String.copyValueOf(password));
            logger.fine("*** connected to jms/eventqueue");

            Session session = connection.createSession(false,Session.AUTO_ACKNOWLEDGE);
            MessageProducer producer = session.createProducer(ucaqueue);
            TextMessage sendUCAMessage = session.createTextMessage();
            logger.fine("*** sending message to event queue");

            sendUCAMessage.setText(mqMessageText);
            producer.send(sendUCAMessage);
            logger.fine("UCAMessage Sent to internal jms/eventqueue.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private PasswordCredential getJ2CData(String j2cAlias) throws Exception {
        logger.fine("*********** in getJ2CData :: getting creds for "+j2cAlias);

        PasswordCredential passwordCredential;
        try {
            HashMap map = new HashMap();
            map.put(com.ibm.wsspi.security.auth.callback.Constants.MAPPING_ALIAS, j2cAlias); // Replace value with your alias.
            CallbackHandler callbackHandler = new com.ibm.wsspi.security.auth.callback.WSMappingCallbackHandler(map, null);
            LoginContext loginContext = new LoginContext("DefaultPrincipalMapping", callbackHandler);
            loginContext.login();
            Subject subject = loginContext.getSubject();
            Set<PasswordCredential> creds = subject.getPrivateCredentials(javax.resource.spi.security.PasswordCredential.class);

            passwordCredential = creds.iterator().next();
        } catch(Exception e) {
            e.printStackTrace();
            logger.severe("APPLICATION ERROR: cannot load credentials for j2calias = " + j2cAlias);
            throw new Exception("Unable to get credentials");
        }

        return passwordCredential;
    }

}
