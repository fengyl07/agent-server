package uyun.eagle.agent.alertagent.frontapi.impl;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.RestController;
import uyun.eagle.agent.alertagent.business.TodoBusiness;
import uyun.eagle.agent.alertagent.dao.po.TodoPO;
import uyun.eagle.agent.alertagent.exception.DemoException;
import uyun.eagle.agent.alertagent.frontapi.TodosFrontApi;
import uyun.eagle.agent.alertagent.frontapi.vo.TodoVO;
import uyun.whale.consumer.common.WebContext;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author: yangfei
 * @Date: 2019/4/08 Time: 15:52
 * @desc 任务信息
 */

@Slf4j
@RestController
public class TodosFrontImpl implements TodosFrontApi {

    @Autowired
    private TodoBusiness todoBusiness;

    @Override
    public TodoVO deleteTodoById(Integer id) {
        log.debug("TodosFrontImpl==>deleteTodoById {}", id);
        TodoPO todo = new TodoPO();
        todo.setId(id);
        try {
            todoBusiness.deleteTodoById(todo);
            TodoVO TodoVO = new TodoVO();
            TodoVO.setId(id);
            return TodoVO;
        } catch (Exception e) {
            if (e instanceof DemoException) {
                throw e;
            }
            throw DemoException.createServerException("删除数据失败，参数为空,原因：" + e.getMessage(), null);
        }

    }

    @Override
    public TodoVO insertTodo(TodoVO createTodo) {
        log.debug("TodosFrontImpl==>insertTodo {}", JSON.toJSONString(createTodo));
        try {
            TodoPO todo = new TodoPO();
            todo.setContent(createTodo.getContent());
            todo.setCreateUser(WebContext.getUser().getRealname());
            todo.setDealsUser(WebContext.getUser().getRealname());
            todo.setHasCompleted(false);
            todo.setCreateTime(new Date(createTodo.getCreateTime()));
            todo.setTodoTime(new Date());
            todo.setDealsTime(new Date());
            todo.setHasCompleted(false);

            todo = todoBusiness.insertTodo(todo);

            createTodo.setId(todo.getId());
        } catch (Exception e) {
            if (e instanceof DemoException) {
                throw e;
            }
            throw DemoException.createServerException("保存数据失败，参数为空,原因：" + e.getMessage(), null);
        }
        return createTodo;
    }

    @Override
    public List<TodoVO> queryByContent(Long dutyDate, String content) {
        log.debug("TodosFrontImpl==>search Long: {} currentUser: {} content {}", dutyDate, content);
        List<TodoVO> todoList = new ArrayList<TodoVO>();

        String currentUser = WebContext.getUser().getRealname();
        log.debug("TodosFrontImpl==>currentUser {}", currentUser);
        try {
            List<TodoPO> list = todoBusiness.selectTodoFindByContent("%" + content + "%");
            if (list != null && !list.isEmpty()) {
                for (TodoPO todo : list) {
                    if (DateUtils.isSameDay(new Date(dutyDate), todo.getCreateTime())) {
                        if (todo.getDealsUser().toLowerCase().equalsIgnoreCase(currentUser.toLowerCase())) {
                            TodoVO TodoVO = new TodoVO();
                            TodoVO.setContent(todo.getContent());
                            TodoVO.setDealsTime(todo.getDealsTime().getTime());
                            TodoVO.setCreateTime(todo.getCreateTime().getTime());
                            TodoVO.setTodoTime(todo.getTodoTime().getTime());
                            TodoVO.setHasCompleted(todo.isHasCompleted());
                            TodoVO.setId(todo.getId());
                            TodoVO.setCreateUser(todo.getCreateUser());
                            TodoVO.setDealsUser(todo.getDealsUser());
                            todoList.add(TodoVO);
                        }
                    }
                }
            }
        } catch (Exception e) {
            if (e instanceof DemoException) {
                throw e;
            }
            throw DemoException.createServerException("获取数据失败，参数为空,原因：" + e.getMessage(), null);
        }

        return todoList;
    }

    @Override
    public List<TodoVO> selectTodoFindAll(Long dutyDate) {
        log.debug("TodosFrontImpl==>selectTodoFindAll Long: {} currentUser: {}", dutyDate);
        List<TodoVO> todoList = new ArrayList<TodoVO>();
        String currentUser = WebContext.getUser().getRealname();
        log.debug("TodosFrontImpl==>currentUser {}", currentUser);

        try {
            List<TodoPO> list = todoBusiness.selectTodoFindAll();
            if (list != null && !list.isEmpty()) {
                for (TodoPO todo : list) {
                    if (DateUtils.isSameDay(new Date(dutyDate), todo.getCreateTime())) {
                        if (todo.getDealsUser().toLowerCase().equalsIgnoreCase(currentUser.toLowerCase())) {
                            TodoVO TodoVO = new TodoVO();
                            TodoVO.setContent(todo.getContent());
                            TodoVO.setDealsTime(todo.getDealsTime().getTime());
                            TodoVO.setCreateTime(todo.getCreateTime().getTime());
                            TodoVO.setTodoTime(todo.getTodoTime().getTime());
                            TodoVO.setHasCompleted(todo.isHasCompleted());
                            TodoVO.setId(todo.getId());
                            TodoVO.setCreateUser(todo.getCreateUser());
                            TodoVO.setDealsUser(todo.getDealsUser());
                            todoList.add(TodoVO);
                        }
                    }
                }
            }
        } catch (Exception e) {
            if (e instanceof DemoException) {
                throw e;
            }
            throw DemoException.createServerException("获取数据失败，参数为空,原因：" + e.getMessage(), null);
        }
        return todoList;
    }

    @Secured({"task_assign"})
    @Override
    public TodoVO updateTodoById(TodoVO updateTodo) {
        log.debug("TodosFrontImpl==>updateTodoById {}", JSON.toJSONString(updateTodo));
        try {
            TodoPO todo = new TodoPO();
            todo.setId(updateTodo.getId());
            todo.setContent(updateTodo.getContent());
            todo.setCreateUser(updateTodo.getCreateUser());
            todo.setCreateTime(new Date(updateTodo.getCreateTime()));
            todo.setDealsUser(updateTodo.getDealsUser());
            todo.setHasCompleted(updateTodo.getHasCompleted());
            todo.setTodoTime(new Date(updateTodo.getTodoTime()));
            if (updateTodo.getDealsTime() != null) {
                todo.setDealsTime(new Date(updateTodo.getDealsTime()));
            } else {
                todo.setDealsTime(new Date());
            }
            todoBusiness.updateTodoById(todo);
        } catch (Exception e) {
            if (e instanceof DemoException) {
                throw e;
            }
            throw DemoException.createServerException("修改数据失败，参数为空,原因：" + e.getMessage(), null);
        }

        return updateTodo;
    }
}