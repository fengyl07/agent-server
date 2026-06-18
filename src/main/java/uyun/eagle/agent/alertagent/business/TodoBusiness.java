package uyun.eagle.agent.alertagent.business;

import uyun.eagle.agent.alertagent.dao.po.TodoPO;

import java.util.List;

/**
 * @author: yangfei
 * @Date: 2019/4/08 Time: 15:52
 * @desc 任务信息接口
 */
public interface TodoBusiness {

    /**
     * param todoPO
     * return
     * desc 保存任务信息
     */
    public TodoPO insertTodo(TodoPO todoPO);


    /**
     * param todoPO
     * return
     * desc 根据id修改信息
     */
    public TodoPO updateTodoById(TodoPO todoPO);


    /**
     * param todoPO
     * return
     * desc 根据id删除信息
     */
    public void deleteTodoById(TodoPO todoPO);

    /**
     * param todoPO
     * return
     * desc 根据id查下任务信息
     */
    public TodoPO selectTodoFindById(TodoPO todoPO);


    /**
     * param todoPO
     * return
     * desc 查询所有任务信息
     */
    public List<TodoPO> selectTodoFindAll();


    /**
     * param todoPO
     * return
     * desc 根据条件查询所有任务信息
     */
    public List<TodoPO> selectTodoFindByContent(String content);

}
