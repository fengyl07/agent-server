package uyun.eagle.agent.alertagent.dao.po;

import lombok.Data;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.util.Date;

/**
 * @author: yangfei
 * @Date: 2019/4/08 Time: 15:52
 * @desc 任务对象
 */
@Entity
@Table(name = "todo")
@Data
public class TodoPO implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String content;

    @Column(name = "has_completed")
    private boolean hasCompleted;

    @Column(name = "create_user")
    private String createUser;

    @Column(name = "deals_user")
    private String dealsUser;

    @Column(name = "create_time")
    private Date createTime;

    @Column(name = "todo_time")
    private Date todoTime;

    @Column(name = "deals_time")
    private Date dealsTime;
}
