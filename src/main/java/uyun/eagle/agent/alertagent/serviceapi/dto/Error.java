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
 * Error
 */
@javax.annotation.Generated(value = "class io.swagger.codegen.languages.SpringCodegen", date = "2019-06-14T15:08:34.020+08:00")

@DefaultSerializer(CompatibleFieldSerializer.class)
public class Error  implements Serializable {
  @JsonProperty("code")
  private String code = null;

  @JsonProperty("message")
  private String message = null;

  @JsonProperty("path")
  private String path = null;

  @JsonProperty("time")
  private String time = null;

  public Error code(String code) {
    this.code = code;
    return this;
  }

   /**
   * 错误编码
   * @return code
  **/
  @ApiModelProperty(example = "ERR-500", required = true, value = "错误编码")
  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public Error message(String message) {
    this.message = message;
    return this;
  }

   /**
   * 错误消息
   * @return message
  **/
  @ApiModelProperty(example = "用户名称不能为空", required = true, value = "错误消息")
  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public Error path(String path) {
    this.path = path;
    return this;
  }

   /**
   * 请求路径
   * @return path
  **/
  @ApiModelProperty(example = "/v2/cloud/save", value = "请求路径")
  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public Error time(String time) {
    this.time = time;
    return this;
  }

   /**
   * 出错时间
   * @return time
  **/
  @ApiModelProperty(example = "2019-03-23 09:11:20", value = "出错时间")
  public String getTime() {
    return time;
  }

  public void setTime(String time) {
    this.time = time;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Error error = (Error) o;
    return Objects.equals(this.code, error.code) &&
        Objects.equals(this.message, error.message) &&
        Objects.equals(this.path, error.path) &&
        Objects.equals(this.time, error.time);
  }

  @Override
  public int hashCode() {
    return Objects.hash(code, message, path, time);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Error {\n");
    
    sb.append("    code: ").append(toIndentedString(code)).append("\n");
    sb.append("    message: ").append(toIndentedString(message)).append("\n");
    sb.append("    path: ").append(toIndentedString(path)).append("\n");
    sb.append("    time: ").append(toIndentedString(time)).append("\n");
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

