/*
 *******************************************
 * File: WXResultActivity.java
 * Author: Lee
 * Date: 2016年6月23日
 ********************************************/
package org.lee.cloud.pay.wxpay;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.tencent.mm.sdk.modelbase.BaseReq;
import com.tencent.mm.sdk.modelbase.BaseResp;
import com.tencent.mm.sdk.openapi.IWXAPI;
import com.tencent.mm.sdk.openapi.IWXAPIEventHandler;

/**
 * 
 * @ClassName: WXResultActivity
 * @Description: TODO(描述: )
 * @author Lee
 * @date 2016年6月23日 下午6:54:38
 * @version V1.0
 */
public abstract class WXPayResultActivity extends Activity implements IWXAPIEventHandler {
	protected IWXAPI api;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		regWXApi();
	}
	protected abstract void regWXApi() ;

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		setIntent(intent);
        api.handleIntent(intent, this);
	}
	@Override
	public void onReq(BaseReq req) {
		_WXPay.onReq(req);
	}
	@Override
	public void onResp(BaseResp resp) {
		_WXPay.onResp(resp);
		finish();
	}

}
