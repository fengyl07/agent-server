package uyun.eagle.agent.alertagent.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uyun.eagle.agent.alertagent.dao.po.TodoPO;

import java.util.List;

/**
 * @author: yangfei
 * @Date: 2019/4/08 Time: 15:52
 * @desc dao接口实现
 */
@Repository
public interface TodoDao extends JpaRepository<TodoPO, Integer>, JpaSpecificationExecutor<TodoPO> {

    List<TodoPO> findByContentLike(String content);
}
