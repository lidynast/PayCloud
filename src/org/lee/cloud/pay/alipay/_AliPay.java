/*
 *******************************************
 * File: _Alipay.java
 * Author: Lee
 * Date: 2016年6月23日
 ********************************************/
package org.lee.cloud.pay.alipay;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.lee.cloud.pay.IPayConfig;
import org.lee.cloud.pay.PayResultListener;
import org.lee.cloud.pay.ResultCode;

import com.alipay.sdk.app.PayTask;

import android.app.Activity;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;

/**
 * 
 * @ClassName: _Alipay
 * @Description: TODO(描述: 支付宝支付)
 * @author Lee
 * @date 2016年6月23日 下午4:55:42
 * @version V1.0
 */
public class _AliPay {
	/**RSA-支付宝公钥(这是文档里面的可以不做修改)*/ 
	public final String RSA_PUBLIC = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCnxj/9qwVfgoUh/y2W89L6BkRAFljhNhgPdyPuBV64bfQNN1PjbCzkIM6qRdKBoLPXmKKMiFYnkd6rAoprih3/PrQEB/VsW8OoM8fxn67UDYuyBTqA23MML9q1+ilIZwBC2AQ2UBVOrFXfFl75p6/B5KsiNG9zpgmLCUYuLkxpLQIDAQAB";
	/** 支付 */
	public static final int SDK_PAY_FLAG = 1;
	/** 检查查询终端设备是否存在支付宝认证账户 */
	public static final int SDK_CHECK_FLAG = 2;
	/** 支付成功 */
	public static final String PAY_STATE_OK = "9000";
	/** 支付结果确认中 */
	public static final String PAY_STATE_CONFIRMING = "8000";
	/** 用户取消 */
	public static final String PAY_STATE_CANCEL = "6001";
	/** 网络连接错误 */
	public static final String PAY_STATE_NET_ERR = "6002";

	/**
	 * 
	 */
	private Activity mAty;
	private PayResultListener resultListener;
	private IPayConfig payConfig;

	public _AliPay addPayResultListener(PayResultListener ll) {
		this.resultListener = ll;
		return this;
	}

	public _AliPay(Activity aty,IPayConfig config) {
		this.payConfig = config;
		this.mAty = aty;
	}
	/**
	 * 支付
	 */
	public void Pay(String outTradeNo, String subject, String body, String price){
		//检测必要参数
		checkConfig();
		//生成订单信息
		String orderInfo = createOrderInfo(outTradeNo, subject, body, price);
		//签名
		String sign = sign(orderInfo);
		try {
			/**
			 * 仅需对sign 做URL编码
			 */
			sign = URLEncoder.encode(sign, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		/**
		 * 完整的符合支付宝参数规范的订单信息
		 */
		final String payInfo = orderInfo + "&sign=\"" + sign + "\"&" + getSignType();
		Runnable payRunnable = new Runnable() {

			@Override
			public void run() {
				// 构造PayTask 对象
				PayTask alipay = new PayTask(mAty);
				// 调用支付接口，获取支付结果
				String result = alipay.pay(payInfo, true);

				Message msg = new Message();
				msg.what = SDK_PAY_FLAG;
				msg.obj = result;
				mHandler.sendMessage(msg);
			}
		};

		// 必须异步调用
		Thread payThread = new Thread(payRunnable);
		payThread.start();
	}
	
	private Handler mHandler = new Handler() {
		@SuppressWarnings("unused")
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case SDK_PAY_FLAG: {
				AliPayResult payResult = new AliPayResult((String) msg.obj);
				/**
				 * 同步返回的结果必须放置到服务端进行验证（验证的规则请看https://doc.open.alipay.com/doc2/
				 * detail.htm?spm=0.0.0.0.xdvAU6&treeId=59&articleId=103665&
				 * docType=1) 建议商户依赖异步通知
				 */
				String resultInfo = payResult.getResult();// 同步返回需要验证的信息

				String resultStatus = payResult.getResultStatus();
				// 判断resultStatus 为“9000”则代表支付成功，具体状态码代表含义可参考接口文档
				if (TextUtils.equals(resultStatus, PAY_STATE_OK)) {
					if(null!=resultListener)
						resultListener.onPayResult(ResultCode.PAY_SUCCESS, "支付成功");
				} else if(TextUtils.equals(resultStatus, PAY_STATE_CONFIRMING)){
					if(null!=resultListener)
						resultListener.onPayResult(ResultCode.PAY_CONFIRMING, "支付结果确认中");
				}else if(TextUtils.equals(resultStatus, PAY_STATE_CANCEL)){
					if(null!=resultListener)
						resultListener.onPayResult(ResultCode.PAY_CANCEL, "取消支付");
				}else if(TextUtils.equals(resultStatus, PAY_STATE_NET_ERR)){
					if(null!=resultListener)
						resultListener.onPayResult(ResultCode.PAY_NET_ERR, "网络错误");
				}else{
					if(null!=resultListener)
						resultListener.onPayResult(ResultCode.PAY_FAIL, "支付失败");
				}
				
				break;
			}
			}
		};
	};
	
	
	/**
	 * create the order info. 创建订单信息
	 * 
	 */
	private String createOrderInfo(String outTradeNo, String subject, String body, String price) {
		// 签约合作者身份ID
		String orderInfo = "partner=" + "\"" + payConfig.getAliPartner() + "\"";

		// 签约卖家支付宝账号
		orderInfo += "&seller_id=" + "\"" + payConfig.getAliSeller() + "\"";

		// 商户网站唯一订单号
		orderInfo += "&out_trade_no=" + "\"" + outTradeNo + "\"";

		// 商品名称
		orderInfo += "&subject=" + "\"" + subject + "\"";

		// 商品详情
		orderInfo += "&body=" + "\"" + body + "\"";

		// 商品金额
		orderInfo += "&total_fee=" + "\"" + price + "\"";

		// 服务器异步通知页面路径
		orderInfo += "&notify_url=" + "\"" + payConfig.getAliNotifyUrl() + "\"";

		// 服务接口名称， 固定值
		orderInfo += "&service=\"mobile.securitypay.pay\"";

		// 支付类型， 固定值
		orderInfo += "&payment_type=\"1\"";

		// 参数编码， 固定值
		orderInfo += "&_input_charset=\"utf-8\"";

		// 设置未付款交易的超时时间
		// 默认30分钟，一旦超时，该笔交易就会自动被关闭。
		// 取值范围：1m～15d。
		// m-分钟，h-小时，d-天，1c-当天（无论交易何时创建，都在0点关闭）。
		// 该参数数值不接受小数点，如1.5h，可转换为90m。
		orderInfo += "&it_b_pay=\"30m\"";

		// extern_token为经过快登授权获取到的alipay_open_id,带上此参数用户将使用授权的账户进行支付
		// orderInfo += "&extern_token=" + "\"" + extern_token + "\"";

		// 支付宝处理完请求后，当前页面跳转到商户指定页面的路径，可空
		orderInfo += "&return_url=\"m.alipay.com\"";

		// 调用银行卡支付，需配置此参数，参与签名， 固定值 （需要签约《无线银行卡快捷支付》才能使用）
		// orderInfo += "&paymethod=\"expressGateway\"";

		return orderInfo;
	}

	private void checkConfig() {
		// TODO Auto-generated method stub
		if(null==payConfig){
			if(null!=resultListener)
				resultListener.onPayResult(ResultCode.PAY_KEY_ERR, "参数有误[实现PayConfig配置必要参数]");
			return;
		}
		if(TextUtils.isEmpty(payConfig.getAliPartner())){
			if(null!=resultListener)
				resultListener.onPayResult(ResultCode.PAY_KEY_ERR, "参数有误[商户PID未配置]");
			return;
		}
		if(TextUtils.isEmpty(payConfig.getAliSeller())){
			if(null!=resultListener)
				resultListener.onPayResult(ResultCode.PAY_KEY_ERR, "参数有误[商户支付宝账号未配置]");
			return;
		}
		if(TextUtils.isEmpty(payConfig.getAliPrivateRas())){
			if(null!=resultListener)
				resultListener.onPayResult(ResultCode.PAY_KEY_ERR, "参数有误[商户私钥未配置]");
			return;
		}
		if(TextUtils.isEmpty(payConfig.getAliNotifyUrl())){
			if(null!=resultListener)
				resultListener.onPayResult(ResultCode.PAY_KEY_ERR, "参数有误[回调地址未配置]");
			return;
		}
	}

	/**
	 * sign the order info. 对订单信息进行签名
	 * 
	 * @param content
	 *            待签名订单信息
	 */
	private String sign(String content) {
		return AliSignUtils.sign(content, payConfig.getAliPrivateRas());
	}

	/**
	 * get the sign type we use. 获取签名方式
	 * 
	 */
	private String getSignType() {
		return "sign_type=\"RSA\"";
	}
}
