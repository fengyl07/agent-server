package uyun.eagle.agent.alertagent.serviceapi.impl;


import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
import uyun.eagle.agent.alertagent.business.TodoBusiness;
import uyun.eagle.agent.alertagent.dao.po.TodoPO;
import uyun.eagle.agent.alertagent.serviceapi.TodosServiceApi;
import uyun.eagle.agent.alertagent.serviceapi.dto.TodoDTO;
import uyun.whale.consumer.common.WebContext;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author: yangfei
 * @desc: dubbo business 服务
 * @date: created in 2019/4/16 11:09
 * @modifed by:
 */
@Slf4j
@RestController
public class TodosServiceImpl implements TodosServiceApi {

    @Autowired
    private TodoBusiness todoBusiness;

    @Override
    public TodoDTO deleteTodoById(Integer id) {
        TodoPO todo = new TodoPO();
        todo.setId(id);
        todoBusiness.deleteTodoById(todo);
        TodoDTO todoDTO = new TodoDTO();
        todoDTO.setId(id);
        return todoDTO;
    }

    @Override
    public TodoDTO insertTodo(TodoDTO createTodo) {
        TodoPO todo = new TodoPO();
        todo.setContent(createTodo.getContent());
        todo.setCreateUser(WebContext.getUser().getRealname());
        todo.setDealsUser(WebContext.getUser().getRealname());
        todo.setCreateTime(new Date());
        todo.setTodoTime(new Date());
        todo.setDealsTime(new Date());
        todo.setHasCompleted(false);

        todo = todoBusiness.insertTodo(todo);

        createTodo.setId(todo.getId());
        return createTodo;
    }

    @Override
    public List<TodoDTO> selectTodoFindAll(Long dutyDate, String currentUser) {
        List<TodoDTO> todoList = new ArrayList<>();

        List<TodoPO> list = todoBusiness.selectTodoFindAll();
        if (list != null && !list.isEmpty()) {
            for (TodoPO todo : list) {
                if (DateUtils.isSameDay(new Date(dutyDate), todo.getCreateTime())) {
                    if (todo.getDealsUser().toLowerCase().equalsIgnoreCase(currentUser.toLowerCase())) {
                        TodoDTO todoDTO = new TodoDTO();
                        todoDTO.setContent(todo.getContent());
                        todoDTO.setDealsTime(todo.getDealsTime().getTime());
                        todoDTO.setCreateTime(todo.getCreateTime().getTime());
                        todoDTO.setTodoTime(todo.getTodoTime().getTime());
                        todoDTO.setHasCompleted(todo.isHasCompleted());
                        todoDTO.setId(todo.getId());
                        todoDTO.setCreateUser(todo.getCreateUser());
                        todoDTO.setDealsUser(todo.getDealsUser());
                        todoList.add(todoDTO);
                    }
                }
            }
        }

        return todoList;
    }

    @Override
    public TodoDTO updateTodoById(TodoDTO updateTodo) {
        TodoPO todo = new TodoPO();
        todo.setId(updateTodo.getId());
        todo.setContent(updateTodo.getContent());
        todo.setCreateUser(updateTodo.getCreateUser());
        todo.setDealsUser(updateTodo.getDealsUser());
        todo.setHasCompleted(updateTodo.getHasCompleted());
        todo.setTodoTime(new Date());
        todo.setDealsTime(new Date());
        todoBusiness.updateTodoById(todo);
        return updateTodo;
    }
}
