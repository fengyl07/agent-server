package uyun.eagle.agent.alertagent.business.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uyun.eagle.agent.alertagent.business.TodoBusiness;
import uyun.eagle.agent.alertagent.common.i18n.I18nMessages;
import uyun.eagle.agent.alertagent.dao.TodoDao;
import uyun.eagle.agent.alertagent.dao.po.TodoPO;
import uyun.eagle.agent.alertagent.exception.DemoException;
import uyun.whale.consumer.common.WebContext;
import uyun.whale.security.acl.AclServices;
import uyun.whale.security.acl.api.entity.AclEntry;
import uyun.whale.security.acl.api.entity.Subject.SubjectTypes;
import uyun.whale.security.acl.core.DefaultAclAction;

import java.util.Arrays;
import java.util.List;

/**
 * @author: yangfei
 * @desc: todo服务
 * @date: created in 2019/2/16 11:09
 * @modifed by:
 */
@Slf4j
@Service
public class TodoBusinessImpl implements TodoBusiness {

    @Autowired
    private TodoDao todoDao;


    @Override
    public TodoPO insertTodo(TodoPO todoPO) {
        TodoPO po = todoDao.save(todoPO);

        // 创建者拥有删除权限
        log.debug("add 'DELETE' acl entry for object '{}' of type: {}", todoPO.getId(), todoPO.getClass().getName());
        String subjectId = WebContext.getUser().getUserId();
        AclServices.getAclEntryService().addEntries(Arrays.asList(
                new AclEntry(String.valueOf(todoPO.getId()), TodoPO.class.getName(), DefaultAclAction.DELETE, subjectId, SubjectTypes.USER.name())
        ));

        return po;
    }

    @Override
    public TodoPO updateTodoById(TodoPO todoPO) {
        TodoPO td = todoDao.findById(todoPO.getId()).orElse(null);
        if (td != null) {
            todoDao.save(todoPO);
        }
        return todoPO;
    }

    @Override
    public void deleteTodoById(TodoPO todoPO) {
        // 判断是否有删除权限
        String userId = WebContext.getUser().getUserId();
        String action = DefaultAclAction.DELETE.name();
        if (!AclServices.hasPermission(userId, TodoPO.class, String.valueOf(todoPO.getId()), action)) {
            throw DemoException.createClientException(I18nMessages.ERROR_NOT_AUTHORIZED, todoPO.getId(), action);
        }

        todoDao.deleteById(todoPO.getId());

        // 对象删除，关联的ACL记录也要同时删除
        log.debug("delete acl entries for object '{}' of type: {}", todoPO.getId(), todoPO.getClass().getName());
        AclServices.getAclEntryService().deleteEntriesByObject(todoPO.getClass().getName(), String.valueOf(todoPO.getId()));
    }

    @Override
    public TodoPO selectTodoFindById(TodoPO todoPO) {
        return todoDao.findById(todoPO.getId()).orElse(null);
    }

    @Override
    public List<TodoPO> selectTodoFindAll() {
        return todoDao.findAll();
    }

    @Override
    public List<TodoPO> selectTodoFindByContent(String content) {
        return todoDao.findByContentLike(content);
    }
}
