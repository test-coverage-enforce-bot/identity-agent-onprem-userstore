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
package org.wso2.carbon.identity.agent.userstore.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.identity.agent.userstore.model.AgentConfiguration;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.introspector.BeanAccess;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Agent config utility
 */
public class AgentConfigUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgentConfigUtil.class);
    private static final String FILE_NAME = "agent-config.yml";
    private static AgentConfiguration config = null;

    public static AgentConfiguration build() {

        if (config == null) {
            Path path = Paths.get("conf" + File.separator + FILE_NAME);
            if (Files.exists(path)) {
                try {
                    Reader in = new InputStreamReader(Files.newInputStream(path), StandardCharsets.UTF_8);
                    Yaml yaml = new Yaml();
                    yaml.setBeanAccess(BeanAccess.FIELD);
                    config = yaml.loadAs(in, AgentConfiguration.class);
                } catch (IOException e) {
                    String errorMessage = "Error occurred while loading the " + FILE_NAME + " yaml file, ";
                    LOGGER.error(errorMessage, e);
                }
            }
        }
        return config;
    }
}
