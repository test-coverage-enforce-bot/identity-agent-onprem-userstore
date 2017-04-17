/*
 *   Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.wso2.carbon.identity.agent.outbound.server.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.identity.agent.outbound.server.model.MessageBrokerConfig;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Message broker config utility
 */
public class MessageBrokerConfigUtil {

    private static final Logger logger = LoggerFactory.getLogger(MessageBrokerConfigUtil.class);
    private static final String FILE_NAME = "message-broker.yml";

    public static MessageBrokerConfig build() {
        Path path = Paths.get("conf" + File.separator + FILE_NAME);
        MessageBrokerConfig config = null;
        //TODO
        //        if (Files.exists(path)) {
        //            try {
        //                Reader in = new InputStreamReader(Files.newInputStream(path), StandardCharsets.UTF_8);
        //                Yaml yaml = new Yaml();
        //                yaml.setBeanAccess(BeanAccess.FIELD);
        //                config = yaml.loadAs(in, MessageBrokerConfig.class);
        //            } catch (IOException e) {
        //                String errorMessage = "Error occurred while loading the " + FILE_NAME + " yaml file, ";
        //                logger.error(errorMessage, e);
        //            }
        //        }
        config = new MessageBrokerConfig();
        config.setUrl("tcp://localhost:61616");
        return config;
    }
}
