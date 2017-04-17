package org.wso2.carbon.identity.agent.outbound.server.messaging;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.identity.agent.outbound.server.ServerHandler;
import org.wso2.carbon.identity.user.store.outbound.model.UserOperation;

import java.io.IOException;
import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;

/**
 * JMS Message receiver
 */
public class JMSMessageReceiver implements MessageListener {

    private static final Logger log = LoggerFactory.getLogger(JMSMessageReceiver.class);
    private ServerHandler serverHandler;
    private static final String QUEUE_NAME_REQUEST = "requestQueue";
    private boolean transacted = false;
    private MessageConsumer requestConsumer;
    private String serverNode;

    public JMSMessageReceiver(ServerHandler serverHandler, String serverNode) {
        this.serverHandler = serverHandler;
        this.serverNode = serverNode;
        Thread loop = new Thread(() -> startReceive());
        loop.start();
    }

    public boolean startReceive() {
        boolean started = true;
        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory("tcp://localhost:61616");
        Connection connection;
        try {
            connection = connectionFactory.createConnection();
            connection.start();
            javax.jms.Session session = connection.createSession(transacted, javax.jms.Session.AUTO_ACKNOWLEDGE);
            Destination adminQueue = session.createQueue(QUEUE_NAME_REQUEST);

            String filter = String.format("serverNode='%s'", serverNode);
            requestConsumer = session.createConsumer(adminQueue, filter);
            requestConsumer.setMessageListener(this);
            JMSExceptionListener exceptionListener = new JMSExceptionListener(this);
            connection.setExceptionListener(exceptionListener);
            log.info("Message listener successfully started.");
        } catch (JMSException e) {
            log.error("Error occurred while listening message.", e);
            started = false;
        }
        return started;
    }

    @Override
    public void onMessage(Message message) {
        try {
            log.info("Message received : " + message.getJMSCorrelationID());
            UserOperation userOperation = (UserOperation) ((ObjectMessage) message).getObject();
            processOperation(userOperation);
        } catch (JMSException e) {
            log.error("Error occurred while receiving message", e);
        }
    }

    private String convertToJson(UserOperation userOperation) {

        return String
                .format("{correlationId : '%s', requestType : '%s', requestData : %s}",
                        userOperation.getCorrelationId(),
                        userOperation.getRequestType(), userOperation.getRequestData());
    }

    public void processOperation(UserOperation userOperation) {
        Thread loop = new Thread(() -> {
            try {
                serverHandler.getSession(userOperation.getTenant()).getBasicRemote()
                        .sendText(convertToJson(userOperation));
            } catch (IOException ex) {
                log.error("Error occurred while sending messaging to client", ex);
            }
        });
        loop.start();
    }
}
