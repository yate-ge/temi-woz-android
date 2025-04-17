package com.alibaba.nls.client;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigLoader {
    private static final Logger logger = LoggerFactory.getLogger(ConfigLoader.class);
    private static Properties properties = new Properties();
    
    static {
        try (InputStream input = ConfigLoader.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input != null) {
                properties.load(input);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    
    public static String getAccessKeyId() {
        String keyi = properties.getProperty("aliyun.accessKeyId", "");
        logger.info("获取AccessKeyId: {}", keyi);
        return keyi;
    }
    
    public static String getAccessKeySecret() {
        return properties.getProperty("aliyun.accessKeySecret", "");
    }    

    public static String getAppKey() {
        return properties.getProperty("temiASR.appKey", "");
    }
}
