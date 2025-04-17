package com.alibaba.nls.client;

import com.alibaba.nls.client.AccessToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 阿里云访问令牌管理类
 * 用于在线获取和管理阿里云访问令牌
 * 单例模式实现，确保全局只有一个实例
 */


 
public class TokenManager {
    private static final Logger logger = LoggerFactory.getLogger(TokenManager.class);
    
    // 单例实例
    private static TokenManager instance;
    
    private String accessKeyId;
    private String accessKeySecret;
    private AccessToken accessToken;
    private long expireTime;
    
    /**
     * 私有构造函数，防止外部直接创建实例
     */
    private TokenManager(String accessKeyId, String accessKeySecret) {
        this.accessKeyId = accessKeyId;
        this.accessKeySecret = accessKeySecret;
        this.accessToken = null;
        this.expireTime = 0;
    }
    
    /**
     * 获取TokenManager的单例实例
     * accessKeyId 阿里云访问密钥ID
     * accessKeySecret 阿里云访问密钥Secret
     * 获取TokenManager的单例实例(使用已配置的密钥)
     * @return TokenManager实例
     */
    public static synchronized TokenManager getInstance() {
        if (instance == null) {
            String accessKeyId = ConfigLoader.getAccessKeyId();
            String accessKeySecret = ConfigLoader.getAccessKeySecret();
            instance = new TokenManager(accessKeyId, accessKeySecret);
        }
        return instance;
    }
    
    /**
     * 获取有效的访问令牌
     * 如果当前令牌已过期或未初始化，会自动申请新令牌
     * @return 有效的访问令牌
     */
    public String getToken() {
        if (accessToken == null || System.currentTimeMillis() >= expireTime) {
            refreshToken();
        }
        return accessToken.getToken();
    }
    
    /**
     * 刷新访问令牌
     */
    public void refreshToken() {
        try {
            logger.info("【Token管理】开始刷新访问令牌");
            accessToken = new AccessToken(accessKeyId, accessKeySecret);
            accessToken.apply();
            // 提前5分钟过期，确保安全边界
            expireTime = accessToken.getExpireTime() - (5 * 60 * 1000);
            logger.info("【Token管理】访问令牌刷新成功，有效期至：" + new java.util.Date(accessToken.getExpireTime()));
        } catch (Exception e) {
            logger.error("【Token管理】刷新访问令牌失败", e);
            throw new RuntimeException("刷新访问令牌失败", e);
        }
    }
    
    /**
     * 获取令牌过期时间
     * @return 过期时间的时间戳（毫秒）
     */
    public long getExpireTime() {
        return expireTime;
    }

    public static void main(String[] args) throws Exception {
        String accessKeyId = ConfigLoader.getAccessKeyId();
        String accessKeySecret = ConfigLoader.getAccessKeySecret();
        logger.info("AccessKey信息: ID={}, Secret={}", accessKeyId, accessKeySecret);
        
        // 使用单例获取Token
        TokenManager tokenManager = TokenManager.getInstance();
        String token = tokenManager.getToken();
        logger.info("获取到的Token: {}", token);
    }
}