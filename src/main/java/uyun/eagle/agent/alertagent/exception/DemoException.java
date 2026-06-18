package uyun.eagle.agent.alertagent.exception;

import java.util.StringJoiner;

import uyun.whale.i18n.impls.I18nService;

public class DemoException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	/**
	 * 异常原因
	 */
	public enum CauseType {
		CLIENT, SERVER;
	}

	/**
	 * 消息编码
	 */
	private String messageCode;

	/**
	 * 导致异常的原因
	 */
	private CauseType causeType;

	/**
	 * 参数列表
	 */
	private Object[] params;

	protected DemoException(String message, Throwable cause) {
		super(message, cause);
	}

	public static DemoException createClientException(String messageCode, Object...params) {
		String message = I18nService.getMessage("en_US", messageCode, params);
		DemoException demoEx = new DemoException(message == null ? messageCode : message, null);
		demoEx.causeType = CauseType.CLIENT;
		demoEx.messageCode = messageCode;
		demoEx.params = params;
		return demoEx;
	}

	public static DemoException createServerException(String message, Throwable cause) {
		DemoException demoEx = new DemoException(message, cause);
		demoEx.causeType = CauseType.SERVER;
		demoEx.messageCode = "500";
		return demoEx;
	}

	public CauseType getCauseType() {
		return causeType;
	}

	public String getMessageCode() {
		return messageCode;
	}

	public Object[] getParams() {
		return params;
	}

	@Override
	public String toString() {
		String message = super.toString();
		StringJoiner sj = new StringJoiner(",");
		sj.add("messageCode: " + messageCode);
		sj.add("causeType: " + causeType.name());
		sj.add("message: " + message);
		return sj.toString();
	}

}
