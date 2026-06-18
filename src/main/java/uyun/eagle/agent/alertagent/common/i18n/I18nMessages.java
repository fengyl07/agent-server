package uyun.eagle.agent.alertagent.common.i18n;

import uyun.whale.i18n.api.I18nMessage;
import uyun.whale.i18n.api.I18nMessageSource;

@I18nMessageSource("zh_CN")
public interface I18nMessages {

	@I18nMessage("参数【{0}】的值不能为空")
	String ERROR_ARGS_REQUIRED = "alertagent.common.args.required";
	
	@I18nMessage("参数【{0}】的值【{1}】无效")
	String ERROR_ARGS_INVALID = "alertagent.common.args.invalid";
	
	@I18nMessage("没有在对象上【{0}】执行【{1}】权限")
	String ERROR_NOT_AUTHORIZED = "alertagent.todo.acl.not_authorized_acl";
	
}
