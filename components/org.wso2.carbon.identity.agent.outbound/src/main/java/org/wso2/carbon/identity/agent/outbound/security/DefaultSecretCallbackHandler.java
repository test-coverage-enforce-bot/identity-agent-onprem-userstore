/*
 * Copyright (c) 2016, WSO2 Inc. (http://wso2.com) All Rights Reserved.
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

package org.wso2.carbon.identity.agent.outbound.security;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.agent.outbound.constant.CommonConstants;
import org.wso2.securevault.secret.AbstractSecretCallbackHandler;
import org.wso2.securevault.secret.SingleSecretCallback;

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
 * This is the default secret call back handler class, shipped with carbon distribution which extends the
 * AbstractSecretCallbackHandler class in synapse secure vault
 *
 * This class retrieves the password of key store and private key as command line input when carbon
 * server is start-up.
 *
 * When server is started as a daemon, this class searches for a simple test file called "password" and read
 * the password from there and delete it.
 *
 * By default, this class assumes that private and key store passwords are same. By passing the
 * key.password=true as a system property in server start-up, default behaviour can be changed
 */

public class DefaultSecretCallbackHandler extends AbstractSecretCallbackHandler {

    private static Log log = LogFactory.getLog(DefaultSecretCallbackHandler.class);
    private static String keyStorePassWord;
    private static String privateKeyPassWord;
    private static File keyDataFile;

    /**
     * {@inheritDoc}
     */
    public void handleSingleSecretCallback(SingleSecretCallback singleSecretCallback) {

        if (keyStorePassWord == null && privateKeyPassWord == null) {

            String textFileName;
            String passwords[];

            String productHome = System.getProperty(CommonConstants.CARBON_HOME);

            String osName = System.getProperty("os.name");
            if (!osName.toLowerCase().contains("win")) {
                textFileName = "password";
            } else {
                textFileName = "password.txt";
            }
            keyDataFile = new File(productHome + File.separator + textFileName);
            if (keyDataFile.exists()) {
                passwords = readPassword(keyDataFile);
                privateKeyPassWord = keyStorePassWord = passwords[0];
                if (!deleteConfigFile()) {
                    handleException("Error deleting Password org.wso2.carbon.identity.agent.outbound.config File");
                }
            } else {
                Console console;
                char[] password;
                if ((console = System.console()) != null && (password = console.readPassword("[%s]",
                                "Enter KeyStore and Private Key Password :")) != null) {
                    keyStorePassWord = String.valueOf(password);
                    privateKeyPassWord = keyStorePassWord;
                }
            }
        }
        if (singleSecretCallback.getId().equals("identity.key.password")) {
            singleSecretCallback.setSecret(privateKeyPassWord);
        } else {
            singleSecretCallback.setSecret(keyStorePassWord);
        }
    }


    /**
     * reads the file which contains the keystore password and retrieve the password.
     * @param file The file which contains the keystore password.
     * @return The lines of password file as an array.
     */
    private String[] readPassword(File file) {

        String stringLines[] = new String[2];
        FileInputStream inputStream = null;
        BufferedReader bufferedReader = null;
        try {
            inputStream = new FileInputStream(file);
            bufferedReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
            stringLines[0] = bufferedReader.readLine();

        } catch (Exception e) {
            handleException("Error reading password from text file ", e);
        } finally {
            try {
                if (bufferedReader != null) {
                    bufferedReader.close();
                }                
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
               log.warn("Error closing output stream of text file");
            }
        }
        return stringLines;
    }

    /**
     * @return true if the file containing the keystore password is successfully deleted, false otherwise.
     */
    private boolean deleteConfigFile() {
        FileOutputStream outputStream = null;
        BufferedWriter bufferedWriter = null;
        try {
            outputStream = new FileOutputStream(keyDataFile);
            bufferedWriter = new BufferedWriter(new OutputStreamWriter(outputStream, "UTF-8"));
            bufferedWriter.write("!@#$%^&*()SDFGHJZXCVBNM!@#$%^&*");
        } catch (Exception e) {
            handleException("Error writing values to text file ", e);
        } finally {
            try {
               if (bufferedWriter != null) {
                    bufferedWriter.close();
                }                
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (IOException e) {
               log.warn("Error closing output stream of text file");
            }
        }
        
        return keyDataFile.exists() && keyDataFile.delete();
    }

    /**
     * @param msg Error message of the org.wso2.carbon.identity.agent.outbound.exception.
     * @param e Thrown org.wso2.carbon.identity.agent.outbound.exception.
     */
    private static void handleException(String msg, Exception e) {
        log.error(msg, e);
        throw new SecretCallbackHandlerException(msg, e);
    }

    /**
     * @param msg Error message of the org.wso2.carbon.identity.agent.outbound.exception.
     */
    private static void handleException(String msg) {
        log.error(msg);
        throw new SecretCallbackHandlerException(msg);
    }
}

