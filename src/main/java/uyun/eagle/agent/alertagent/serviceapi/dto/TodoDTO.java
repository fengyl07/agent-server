package uyun.eagle.agent.alertagent.serviceapi.dto;

import com.esotericsoftware.kryo.DefaultSerializer;
import com.esotericsoftware.kryo.serializers.CompatibleFieldSerializer;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;


/**
 * TodoDTO
 */
@javax.annotation.Generated(value = "class io.swagger.codegen.languages.SpringCodegen", date = "2019-06-14T15:08:34.020+08:00")

@DefaultSerializer(CompatibleFieldSerializer.class)
public class TodoDTO  implements Serializable {
  @JsonProperty("id")
  private Integer id = null;

  @JsonProperty("createUser")
  private String createUser = null;

  @JsonProperty("dealsUser")
  private String dealsUser = null;

  @JsonProperty("hasCompleted")
  private Boolean hasCompleted = null;

  @JsonProperty("createTime")
  private Long createTime = null;

  @JsonProperty("content")
  private String content = null;

  @JsonProperty("todoTime")
  private Long todoTime = null;

  @JsonProperty("dealsTime")
  private Long dealsTime = null;

  public TodoDTO id(Integer id) {
    this.id = id;
    return this;
  }

   /**
   * id
   * @return id
  **/
  @ApiModelProperty(value = "id")
  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public TodoDTO createUser(String createUser) {
    this.createUser = createUser;
    return this;
  }

   /**
   * 创建用户
   * @return createUser
  **/
  @ApiModelProperty(value = "创建用户")
  public String getCreateUser() {
    return createUser;
  }

  public void setCreateUser(String createUser) {
    this.createUser = createUser;
  }

  public TodoDTO dealsUser(String dealsUser) {
    this.dealsUser = dealsUser;
    return this;
  }

   /**
   * 处理用户
   * @return dealsUser
  **/
  @ApiModelProperty(value = "处理用户")
  public String getDealsUser() {
    return dealsUser;
  }

  public void setDealsUser(String dealsUser) {
    this.dealsUser = dealsUser;
  }

  public TodoDTO hasCompleted(Boolean hasCompleted) {
    this.hasCompleted = hasCompleted;
    return this;
  }

   /**
   * 是否已经完成
   * @return hasCompleted
  **/
  @ApiModelProperty(value = "是否已经完成")
  public Boolean getHasCompleted() {
    return hasCompleted;
  }

  public void setHasCompleted(Boolean hasCompleted) {
    this.hasCompleted = hasCompleted;
  }

  public TodoDTO createTime(Long createTime) {
    this.createTime = createTime;
    return this;
  }

   /**
   * 创建时间
   * @return createTime
  **/
  @ApiModelProperty(value = "创建时间")
  public Long getCreateTime() {
    return createTime;
  }

  public void setCreateTime(Long createTime) {
    this.createTime = createTime;
  }

  public TodoDTO content(String content) {
    this.content = content;
    return this;
  }

   /**
   * 待办内容
   * @return content
  **/
  @ApiModelProperty(value = "待办内容")
  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public TodoDTO todoTime(Long todoTime) {
    this.todoTime = todoTime;
    return this;
  }

   /**
   * 待办时间
   * @return todoTime
  **/
  @ApiModelProperty(value = "待办时间")
  public Long getTodoTime() {
    return todoTime;
  }

  public void setTodoTime(Long todoTime) {
    this.todoTime = todoTime;
  }

  public TodoDTO dealsTime(Long dealsTime) {
    this.dealsTime = dealsTime;
    return this;
  }

   /**
   * 处理时间
   * @return dealsTime
  **/
  @ApiModelProperty(value = "处理时间")
  public Long getDealsTime() {
    return dealsTime;
  }

  public void setDealsTime(Long dealsTime) {
    this.dealsTime = dealsTime;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TodoDTO todoDTO = (TodoDTO) o;
    return Objects.equals(this.id, todoDTO.id) &&
        Objects.equals(this.createUser, todoDTO.createUser) &&
        Objects.equals(this.dealsUser, todoDTO.dealsUser) &&
        Objects.equals(this.hasCompleted, todoDTO.hasCompleted) &&
        Objects.equals(this.createTime, todoDTO.createTime) &&
        Objects.equals(this.content, todoDTO.content) &&
        Objects.equals(this.todoTime, todoDTO.todoTime) &&
        Objects.equals(this.dealsTime, todoDTO.dealsTime);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, createUser, dealsUser, hasCompleted, createTime, content, todoTime, dealsTime);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class TodoDTO {\n");
    
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    createUser: ").append(toIndentedString(createUser)).append("\n");
    sb.append("    dealsUser: ").append(toIndentedString(dealsUser)).append("\n");
    sb.append("    hasCompleted: ").append(toIndentedString(hasCompleted)).append("\n");
    sb.append("    createTime: ").append(toIndentedString(createTime)).append("\n");
    sb.append("    content: ").append(toIndentedString(content)).append("\n");
    sb.append("    todoTime: ").append(toIndentedString(todoTime)).append("\n");
    sb.append("    dealsTime: ").append(toIndentedString(dealsTime)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(java.lang.Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}

