package org.wso2.carbon.identity.agent.outbound.server.messaging;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.identity.agent.outbound.server.ServerHandler;
import org.wso2.carbon.identity.agent.outbound.server.util.ServerConfigUtil;
import org.wso2.carbon.identity.user.store.common.MessageRequestUtil;
import org.wso2.carbon.identity.user.store.common.UserStoreConstants;
import org.wso2.carbon.identity.user.store.common.model.ServerOperation;
import org.wso2.carbon.identity.user.store.common.model.UserOperation;

import java.io.IOException;
import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import javax.jms.Topic;
import javax.websocket.Session;

/**
 * JMS Message receiver
 */
public class JMSMessageReceiver implements MessageListener {

    private static final Logger log = LoggerFactory.getLogger(JMSMessageReceiver.class);
    private ServerHandler serverHandler;
    private boolean transacted = false;
    private MessageConsumer requestConsumer;
    private String serverNode;

    public JMSMessageReceiver(ServerHandler serverHandler, String serverNode) {
        this.serverHandler = serverHandler;
        this.serverNode = serverNode;
    }

    public void start() {
        Thread loop = new Thread(this::startReceive);
        loop.start();
    }

    public boolean startReceive() {
        boolean started = true;
        String messageBrokerURL = ServerConfigUtil.build().getMessagebroker().getUrl();
        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(messageBrokerURL);
        Connection connection;
        try {
            connection = connectionFactory.createConnection();
            connection.start();
            javax.jms.Session session = connection.createSession(transacted, javax.jms.Session.AUTO_ACKNOWLEDGE);
            Topic adminQueue = session.createTopic(UserStoreConstants.QUEUE_NAME_REQUEST);
            requestConsumer = session.createConsumer(adminQueue);
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
            processOperation(message);
        } catch (JMSException e) {
            log.error("Error occurred while receiving message", e);
        }
    }

    /**
     * Process message
     * @param message
     * @throws JMSException
     */
    public void processOperation(Message message) throws JMSException {
        if (((ObjectMessage) message).getObject() instanceof UserOperation) {
            UserOperation userOperation = (UserOperation) ((ObjectMessage) message).getObject();
            log.info("Message received for user operation : " + userOperation.getRequestType() + " corerelation Id : "
                    + userOperation.getCorrelationId());
            processUserOperation(userOperation);
        } else if (((ObjectMessage) message).getObject() instanceof ServerOperation) {
            ServerOperation serverOperation = (ServerOperation) ((ObjectMessage) message).getObject();
            log.info("Message received for server operation : " + serverOperation.getOperationType());
            processServerOperation(serverOperation);
        }
    }

    /**
     * Process user operation
     * @param serverOperation
     */
    public void processServerOperation(ServerOperation serverOperation) {
        Thread loop = new Thread(() -> {
            if (serverOperation.getOperationType().equals(UserStoreConstants.SERVER_OPERATION_TYPE_KILL_AGENTS)) {
                try {
                    serverHandler.removeSessions(serverOperation.getTenantDomain(), serverOperation.getDomain());
                } catch (IOException e) {
                    log.error("Error occurred while closing agent connection", e);
                }
            }
        });
        loop.start();
    }

    /**
     * Process User operation
     * @param userOperation
     */
    public void processUserOperation(UserOperation userOperation) {
        Thread loop = new Thread(() -> {
            try {
                Session session = serverHandler.getSession(userOperation.getTenant(), userOperation.getDomain());
                if (session != null) {
                    session.getBasicRemote().sendText(MessageRequestUtil.getUserOperationJSONMessage(userOperation));
                }
            } catch (IOException ex) {
                log.error("Error occurred while sending messaging to client", ex);
            }
        });
        loop.start();
    }
}
