/*
 *******************************************
 * File: _WXPay.java
 * Author: Lee
 * Date: 2016年6月23日
 ********************************************/
package org.lee.cloud.pay.wxpay;

import java.io.StringReader;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.lee.cloud.pay.ExecuteCode;
import org.lee.cloud.pay.IPayConfig;
import org.lee.cloud.pay.PayResultListener;
import org.lee.cloud.pay.ResultCode;
import org.xmlpull.v1.XmlPullParser;

import android.app.Activity;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;
import android.util.Xml;

import com.tencent.mm.sdk.constants.ConstantsAPI;
import com.tencent.mm.sdk.modelbase.BaseReq;
import com.tencent.mm.sdk.modelbase.BaseResp;
import com.tencent.mm.sdk.modelpay.PayReq;
import com.tencent.mm.sdk.openapi.IWXAPI;
import com.tencent.mm.sdk.openapi.WXAPIFactory;

/**
 * 
 * @ClassName: _WXPay
 * @Description: TODO(描述: 微信支付)
 * @author Lee
 * @date 2016年6月23日 下午6:59:49
 * @version V1.0
 */
public class _WXPay {
	private final String TAG = "微信支付";
	/**
	 * 
	 */
	private Activity mAty;
	private static PayResultListener resultListener;
	private IPayConfig payConfig;

	/**
	 * 生成签名
	 */
	protected IWXAPI msgApi;
	private StringBuffer apiSb;
	protected String payEntry;
	String outTradNo;

	public _WXPay addPayResultListener(PayResultListener ll) {
		resultListener = ll;
		return this;
	}

	public _WXPay(Activity aty, IPayConfig config) {
		this.payConfig = config;
		this.mAty = aty;
	}

	public void Pay(String outTradNo, String body, String price) {
		this.outTradNo = outTradNo;
		// 检测必要参数
		checkConfig();
		// 创建WXAPI
		msgApi = WXAPIFactory.createWXAPI(mAty, null);
		// 检测是否安装客户端
		if (!isInstail(msgApi)) {
			if (null != resultListener)
				resultListener.onPayResult(ResultCode.PAY_UNINSTAIL, "微信客户端未安装");
			return;
		}
		String money = "";
		try {
			money = String.valueOf((int) (Double.valueOf(price) * 100));
		} catch (Exception e) {
			if (null != resultListener)
				resultListener.onPayResult(ResultCode.PAY_KEY_ERR, "参数错误[支付金额有误]");
			return;

		}
		payEntry = genProductArgs(money, body);
		req = new PayReq();
		apiSb = new StringBuffer();
		// 第二步：注册Appid
		msgApi.registerApp(payConfig.getWXAppId());
		// 第三步：生成预支付订单
		GetPrepayIdTask getPrepayId = new GetPrepayIdTask();
		getPrepayId.execute();

	}

	/**
	 * 
	 * @param ctx
	 * @param outTradNo
	 * @param total_fee
	 * @param body
	 */
	public void Pay(String prepay_id) {

		// 检测必要参数
		checkConfig();
		// 创建WXAPI
		msgApi = WXAPIFactory.createWXAPI(mAty, null);
		// 检测是否安装客户端
		if (!isInstail(msgApi)) {
			if (null != resultListener)
				resultListener.onPayResult(ResultCode.PAY_UNINSTAIL, "微信客户端未安装");
			return;
		}
		req = new PayReq();
		apiSb = new StringBuffer();
		// 注册Appid
		msgApi.registerApp(payConfig.getWXAppId());
		// 生成预支付订单
		sendPayReq(prepay_id);

	}

	/**
	 * 服务端生成预支付订单
	 * 
	 */
	private void sendPayReq(String prepayId) {
		// 生成微信支付参数
		req.appId = payConfig.getWXAppId();
		req.partnerId = payConfig.getWXMchId();
		req.prepayId = prepayId;
		req.packageValue = "prepay_id=" + prepayId;
		req.nonceStr = genNonceStr();
		req.timeStamp = String.valueOf(genTimeStamp());

		List<NameValuePair> signParams = new LinkedList<NameValuePair>();
		signParams.add(new BasicNameValuePair("appid", req.appId));
		signParams.add(new BasicNameValuePair("noncestr", req.nonceStr));
		signParams.add(new BasicNameValuePair("package", req.packageValue));
		signParams.add(new BasicNameValuePair("partnerid", req.partnerId));
		signParams.add(new BasicNameValuePair("prepayid", req.prepayId));
		signParams.add(new BasicNameValuePair("timestamp", req.timeStamp));

		req.sign = genAppSign(signParams);

		apiSb.append("sign\n" + req.sign + "\n\n");
		Log.e("orion", signParams.toString());
		// 调启微信
		msgApi.sendReq(req);
		if (null != resultListener)
			resultListener.onPayExecute(ExecuteCode.EXECUTE_START);
	}

	/**
	 * 
	 * @return
	 */
	private String genProductArgs(String total_fee, String body) {
		StringBuffer xml = new StringBuffer();

		try {
			String nonceStr = genNonceStr();

			xml.append("</xml>");
			List<NameValuePair> packageParams = new LinkedList<NameValuePair>();
			packageParams.add(new BasicNameValuePair("appid", payConfig.getWXAppId()));
			packageParams.add(new BasicNameValuePair("body", body));
			packageParams.add(new BasicNameValuePair("mch_id", payConfig.getWXMchId()));
			packageParams.add(new BasicNameValuePair("nonce_str", nonceStr));
			packageParams.add(new BasicNameValuePair("notify_url", payConfig.getWXNotifyUrl()));
			packageParams.add(new BasicNameValuePair("out_trade_no", outTradNo));
			packageParams.add(new BasicNameValuePair("spbill_create_ip", "127.0.0.1"));
			packageParams.add(new BasicNameValuePair("total_fee", total_fee));
			packageParams.add(new BasicNameValuePair("trade_type", "APP"));

			String sign = genPackageSign(packageParams);
			packageParams.add(new BasicNameValuePair("sign", sign));

			String xmlstring = toXml(packageParams);

			return xmlstring;

		} catch (Exception e) {
			Log.e(TAG, "genProductArgs fail, ex = " + e.getMessage());
			return null;
		}

	}

	private class GetPrepayIdTask extends AsyncTask<Void, Void, Map<String, String>> {

		@Override
		protected void onPostExecute(Map<String, String> result) {
			apiSb.append("prepay_id\n" + result.get("prepay_id") + "\n\n");
			if ("SUCCESS".equals(result.get("result_code"))) {
				resultunifiedorder = result;
				sendPayReq();
			} else {
				if (null != resultListener)
					resultListener.onPayResult(ResultCode.PAY_FAIL, "支付错误[" + result.get("result_code") + "]");
			}

		}

		@Override
		protected void onCancelled() {
			super.onCancelled();
			if (null != resultListener)
				resultListener.onPayResult(ResultCode.PAY_CANCEL, "用户取消支付");
		}

		@Override
		protected Map<String, String> doInBackground(Void... params) {
			if (null != resultListener)
				resultListener.onPayExecute(ExecuteCode.EXECUTE_CREATE);
			String url = String.format("https://api.mch.weixin.qq.com/pay/unifiedorder");
			Log.e("orion", payEntry);
			byte[] buf = WXUtil.httpPost(url, payEntry);
			String content = new String(buf);
			Log.e("orion", content);
			Map<String, String> xml = decodeXml(content);
			return xml;
		}
	}

	/**
	 * 
	 */
	Map<String, String> resultunifiedorder;
	/**
	 * 微信支付参数
	 */
	protected PayReq req;

	/**
	 * APP端生成预支付订单
	 */
	private void sendPayReq() {
		// 生成微信支付参数
		req.appId = payConfig.getWXAppId();
		req.partnerId = payConfig.getWXMchId();
		req.prepayId = resultunifiedorder.get("prepay_id");
		req.packageValue = "prepay_id=" + resultunifiedorder.get("prepay_id");
		req.nonceStr = genNonceStr();
		req.timeStamp = String.valueOf(genTimeStamp());

		List<NameValuePair> signParams = new LinkedList<NameValuePair>();
		signParams.add(new BasicNameValuePair("appid", req.appId));
		signParams.add(new BasicNameValuePair("noncestr", req.nonceStr));
		signParams.add(new BasicNameValuePair("package", req.packageValue));
		signParams.add(new BasicNameValuePair("partnerid", req.partnerId));
		signParams.add(new BasicNameValuePair("prepayid", req.prepayId));
		signParams.add(new BasicNameValuePair("timestamp", req.timeStamp));

		req.sign = genAppSign(signParams);

		apiSb.append("sign\n" + req.sign + "\n\n");
		// 调启微信
		msgApi.sendReq(req);
		if (null != resultListener)
			resultListener.onPayExecute(ExecuteCode.EXECUTE_START);
	}

	/**
	 * 获取必要参数 nonceStr
	 * 
	 * @return
	 */
	private String genNonceStr() {
		Random random = new Random();
		return WXMD5.getMessageDigest(String.valueOf(random.nextInt(10000)).getBytes());
	}

	/**
	 * 
	 * @return
	 */
	private long genTimeStamp() {
		return System.currentTimeMillis() / 1000;
	}

	/**
	 * 
	 * @param params
	 * @return
	 */
	private String genPackageSign(List<NameValuePair> params) {
		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < params.size(); i++) {
			sb.append(params.get(i).getName());
			sb.append('=');
			sb.append(params.get(i).getValue());
			sb.append('&');
		}
		sb.append("key=");
		sb.append(payConfig.getWXApiKey());

		String packageSign = WXMD5.getMessageDigest(sb.toString().getBytes()).toUpperCase();
		Log.e("orion", packageSign);
		return packageSign;
	}

	/**
	 * 获取sign
	 * 
	 * @param params
	 * @return
	 */
	private String genAppSign(List<NameValuePair> params) {
		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < params.size(); i++) {
			sb.append(params.get(i).getName());
			sb.append('=');
			sb.append(params.get(i).getValue());
			sb.append('&');
		}
		sb.append("key=");
		sb.append(payConfig.getWXApiKey());

		apiSb.append("sign str\n" + sb.toString() + "\n\n");
		String appSign = WXMD5.getMessageDigest(sb.toString().getBytes());
		Log.e("orion", appSign);
		return appSign;
	}

	/**
	 * 生成统一支付接口参数（XML）
	 * 
	 * @param content
	 * @return
	 */
	public Map<String, String> decodeXml(String content) {

		try {
			Map<String, String> xml = new HashMap<String, String>();
			XmlPullParser parser = Xml.newPullParser();
			parser.setInput(new StringReader(content));
			int event = parser.getEventType();
			while (event != XmlPullParser.END_DOCUMENT) {

				String nodeName = parser.getName();
				switch (event) {
				case XmlPullParser.START_DOCUMENT:

					break;
				case XmlPullParser.START_TAG:

					if ("xml".equals(nodeName) == false) {
						// 实例化student对象
						xml.put(nodeName, parser.nextText());
					}
					break;
				case XmlPullParser.END_TAG:
					break;
				}
				event = parser.next();
			}

			return xml;
		} catch (Exception e) {
			Log.e("orion", e.toString());
		}
		return null;

	}

	private String toXml(List<NameValuePair> params) {
		StringBuilder sb = new StringBuilder();
		sb.append("<xml>");
		for (int i = 0; i < params.size(); i++) {
			sb.append("<" + params.get(i).getName() + ">");

			sb.append(params.get(i).getValue());
			sb.append("</" + params.get(i).getName() + ">");
		}
		sb.append("</xml>");
		Log.e("orion", sb.toString());
		return sb.toString();
	}

	/***
	 * 是否安装微信客户端
	 * 
	 * @param api
	 * @return
	 */
	private boolean isInstail(IWXAPI api) {
		// TODO Auto-generated method stub
		boolean sIsWXAppInstalledAndSupported = api.isWXAppInstalled() && api.isWXAppSupportAPI();
		return sIsWXAppInstalledAndSupported;
	}

	private void checkConfig() {
		// TODO Auto-generated method stub
		if (null == payConfig) {
			if (null != resultListener)
				resultListener.onPayResult(ResultCode.PAY_KEY_ERR, "参数有误[实现PayConfig配置必要参数]");
			return;
		}
		if (TextUtils.isEmpty(payConfig.getWXMchId())) {
			if (null != resultListener)
				resultListener.onPayResult(ResultCode.PAY_KEY_ERR, "参数有误[商户号Mch_Id未配置]");
			return;
		}
		if (TextUtils.isEmpty(payConfig.getWXAppId())) {
			if (null != resultListener)
				resultListener.onPayResult(ResultCode.PAY_KEY_ERR, "参数有误[AppId未配置]");
			return;
		}
		if (TextUtils.isEmpty(payConfig.getWXApiKey())) {
			if (null != resultListener)
				resultListener.onPayResult(ResultCode.PAY_KEY_ERR, "参数有误[AppKey未配置]");
			return;
		}
		if (TextUtils.isEmpty(payConfig.getWXNotifyUrl())) {
			if (null != resultListener)
				resultListener.onPayResult(ResultCode.PAY_KEY_ERR, "参数有误[回调地址未配置]");
			return;
		}
	}
	public static void onReq(BaseReq arg0) {
		// TODO Auto-generated method stub

	}

	public static void onResp(BaseResp resp) {
		// TODO Auto-generated method stub
		if (null !=  resultListener&& resp.getType() == ConstantsAPI.COMMAND_PAY_BY_WX) {
			switch (resp.errCode) {
			case 0:
				resultListener.onPayResult(ResultCode.PAY_SUCCESS, "支付成功");
				break;
			case -1:
				resultListener.onPayResult(ResultCode.PAY_FAIL, "支付失败[]签名错误、未注册APPID、项目设置APPID不正确、注册的APPID与设置的不匹配、其他异常等");
				break;
			case -2:
				resultListener.onPayResult(ResultCode.PAY_SUCCESS, "用户取消支付");
				break;
			}
		}
	}
}
