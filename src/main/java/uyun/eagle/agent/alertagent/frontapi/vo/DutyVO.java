package uyun.eagle.agent.alertagent.frontapi.vo;

import com.esotericsoftware.kryo.DefaultSerializer;
import com.esotericsoftware.kryo.serializers.CompatibleFieldSerializer;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;




/**
 * DutyVO
 */
@javax.annotation.Generated(value = "class io.swagger.codegen.languages.SpringCodegen", date = "2019-06-14T15:08:33.768+08:00")


public class DutyVO   {
  @JsonProperty("shiftName")
  private String shiftName = null;

  @JsonProperty("users")
  private List<String> users = new ArrayList<String>();

  public DutyVO shiftName(String shiftName) {
    this.shiftName = shiftName;
    return this;
  }

   /**
   * 排班名称
   * @return shiftName
  **/
  @ApiModelProperty(value = "排班名称")
  public String getShiftName() {
    return shiftName;
  }

  public void setShiftName(String shiftName) {
    this.shiftName = shiftName;
  }

  public DutyVO users(List<String> users) {
    this.users = users;
    return this;
  }

  public DutyVO addUsersItem(String usersItem) {
    this.users.add(usersItem);
    return this;
  }

   /**
   * 排班人员
   * @return users
  **/
  @ApiModelProperty(value = "排班人员")
  public List<String> getUsers() {
    return users;
  }

  public void setUsers(List<String> users) {
    this.users = users;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DutyVO dutyVO = (DutyVO) o;
    return Objects.equals(this.shiftName, dutyVO.shiftName) &&
        Objects.equals(this.users, dutyVO.users);
  }

  @Override
  public int hashCode() {
    return Objects.hash(shiftName, users);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class DutyVO {\n");
    
    sb.append("    shiftName: ").append(toIndentedString(shiftName)).append("\n");
    sb.append("    users: ").append(toIndentedString(users)).append("\n");
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

