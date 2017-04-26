/*
 * Copyright (c) 2017, WSO2 Inc. (http://wso2.com) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.identity.agent.userstore.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.identity.agent.userstore.util.ApplicationUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Console;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

/**
 * Access token handling class
 */
public class AccessTokenHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccessTokenHandler.class);

    public String getAccessToken() {
        File keyDataFile;
        String textFileName;
        String filedata[];
        String accesstoken = null;
        String productHome = ApplicationUtils.getProductHomePath();

        String osName = System.getProperty("os.name");
        if (!osName.toLowerCase().contains("win")) {
            textFileName = "accesstoken";
        } else {
            textFileName = "accesstoken.txt";
        }
        keyDataFile = new File(productHome + File.separator + textFileName);
        if (keyDataFile.exists()) {
            filedata = readPassword(keyDataFile);
            accesstoken = filedata[0];
            if (!deleteConfigFile(keyDataFile)) {
                LOGGER.info("Error deleting access token file");
            }
        } else {
            Console console;
            char[] password;
            if ((console = System.console()) != null && (password = console.readPassword("[%s]",
                    "Enter access token :")) != null) {
                accesstoken = String.valueOf(password);
            }
        }
        return accesstoken;
    }

    private boolean deleteConfigFile(File keyDataFile) {
        FileOutputStream outputStream = null;
        BufferedWriter bufferedWriter = null;
        try {
            outputStream = new FileOutputStream(keyDataFile);
            bufferedWriter = new BufferedWriter(new OutputStreamWriter(outputStream, "UTF-8"));
            bufferedWriter.write("!@#$%^&*()SDFGHJZXCVBNM!@#$%^&*");
        } catch (Exception e) {
            LOGGER.error("Error writing values to text file ", e);
        } finally {
            try {
                if (bufferedWriter != null) {
                    bufferedWriter.close();
                }
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (IOException e) {
                LOGGER.warn("Error closing output stream of text file");
            }
        }

        return keyDataFile.exists() && keyDataFile.delete();
    }

    private String[] readPassword(File file) {

        String stringLines[] = new String[2];
        FileInputStream inputStream = null;
        BufferedReader bufferedReader = null;
        try {
            inputStream = new FileInputStream(file);
            bufferedReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
            stringLines[0] = bufferedReader.readLine();

        } catch (IOException e) {
            String msg = "Error reading password from text file";
            LOGGER.error(msg, e);
        } finally {
            try {
                if (bufferedReader != null) {
                    bufferedReader.close();
                }
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                LOGGER.warn("Error closing output stream of text file");
            }
        }
        return stringLines;
    }
}
