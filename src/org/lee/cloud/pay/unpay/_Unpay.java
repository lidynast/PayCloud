/*
 *******************************************
 * File: _Unpay.java
 * Author: Lee
 * Date: 2016年6月25日
 ********************************************/
package org.lee.cloud.pay.unpay;

import org.lee.cloud.pay.IPayConfig;
import org.lee.cloud.pay.PayResultListener;
import org.lee.cloud.pay.alipay._AliPay;

import com.unionpay.UPPayAssistEx;
import com.unionpay.uppay.PayActivity;

import android.app.Activity;

public class _Unpay {
	/**
	 * 
	 */
	private Activity mAty;
	private PayResultListener resultListener;
	private IPayConfig payConfig;

	public _Unpay addPayResultListener(PayResultListener ll) {
		this.resultListener = ll;
		return this;
	}

	public _Unpay(Activity aty, IPayConfig config) {
		this.payConfig = config;
		this.mAty = aty;
	}

	private String getMode() {
		/*****************************************************************
		 * mMode参数解释： "00" - 启动银联正式环境 "01" - 连接银联测试环境
		 *****************************************************************/
		if (payConfig.isTestEnvironment())
			return "01";
		return "00";
	}
	public void Pay(String tn){
		UPPayAssistEx.startPayByJAR(mAty, PayActivity.class, null, null,
                tn, getMode());
	}
	
}
