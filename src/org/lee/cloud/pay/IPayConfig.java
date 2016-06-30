/*
 *******************************************
 * File: PayConfig.java
 * Author: Lee
 * Date: 2016年6月23日
 ********************************************/
package org.lee.cloud.pay;
/**
 * 
 * @ClassName: PayConfig
 * @Description: TODO(描述: )
 * @author Lee
 * @date 2016年6月23日 下午5:28:12
 * @version V1.0
 */
public interface IPayConfig {
	//支付宝
	/**支付宝-商户PID*/
	public String getAliPartner();
	/**支付宝-商户支付宝账号*/
	public String getAliSeller();
	/**支付宝-商户私钥，pkcs8格式*/
	public String getAliPrivateRas();
	/**支付宝-回调地址*/
	public String getAliNotifyUrl();
	//微信
	/**微信-APP_ID*/
	public String getWXAppId();
	/**微信-商户号MCH_ID*/
	public String getWXMchId();
	/**微信-API_KEY*/
	public String getWXApiKey();
	/**微信-回调地址*/
	public String getWXNotifyUrl();
	//银联支付
	/**银联-是否为测试环境*/
	public boolean isTestEnvironment();
}
