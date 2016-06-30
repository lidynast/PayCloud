/*
 *******************************************
 * File: AliPayResultListener.java
 * Author: Lee
 * Date: 2016年6月23日
 ********************************************/
package org.lee.cloud.pay;


/**
 * 
 * @ClassName: AliPayResultListener
 * @Description: TODO(描述: 支付宝支付结果回调接口)
 * @author Lee
 * @date 2016年6月23日 下午5:03:48
 * @version V1.0
 */
public interface PayResultListener {
	/**
	 * @TODO 支付结果回调
	 * @param resultCode 
	 * @param msg 
	 */
	public void onPayResult(ResultCode resultCode,String msg);
	/**
	 * @TODO 支付执行过程
	 * @param executeCode
	 */
	public void onPayExecute(ExecuteCode executeCode);
}
