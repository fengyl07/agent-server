package uyun.eagle.agent.alertagent.openapi.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
import uyun.eagle.agent.alertagent.business.TodoBusiness;
import uyun.eagle.agent.alertagent.dao.po.TodoPO;
import uyun.eagle.agent.alertagent.openapi.TodosOpenApi;
import uyun.eagle.agent.alertagent.openapi.dto.TodoDTO;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author: yangfei
 * @desc: open服务
 * @date: created in 2019/3/16 11:09
 * @modifed by:
 */
@Slf4j
@RestController
public class TodosOpenImpl implements TodosOpenApi {

    @Autowired
    private TodoBusiness todoBusiness;

    @Override
    public List<TodoDTO> queryTodos(String apikey, Long dutyDate, String currentUser) {
        List<TodoDTO> todoList = new ArrayList<TodoDTO>();

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
}
