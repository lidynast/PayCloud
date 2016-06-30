/*
 *******************************************
 * File: ResultCode.java
 * Author: Lee
 * Date: 2016年6月23日
 ********************************************/
package org.lee.cloud.pay;

/**
 * 
 * @ClassName: ResultCode
 * @Description: TODO(描述: 支付结果)
 * @author Lee
 * @date 2016年6月23日 下午5:15:56
 * @version V1.0
 */
public enum ResultCode {
	/** 支付成功 */
	PAY_SUCCESS,
	/** 支付失败 */
	PAY_FAIL,
	/** 用户取消 */
	PAY_CANCEL,
	/** 支付结果确认中 */
	PAY_CONFIRMING,
	/** 网络错误 */
	PAY_NET_ERR,
	/** 客户端未安装 */
	PAY_UNINSTAIL,
	/** 密钥,商户号...等参数为空或错误*/
	PAY_KEY_ERR
}
