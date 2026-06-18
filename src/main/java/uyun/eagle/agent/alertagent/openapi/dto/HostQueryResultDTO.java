package uyun.eagle.agent.alertagent.openapi.dto;

import com.esotericsoftware.kryo.DefaultSerializer;
import com.esotericsoftware.kryo.serializers.CompatibleFieldSerializer;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import uyun.eagle.agent.alertagent.openapi.dto.HostDTO;




/**
 * HostQueryResultDTO
 */
@javax.annotation.Generated(value = "class io.swagger.codegen.languages.SpringCodegen", date = "2019-04-19T09:39:00.067+08:00")


public class HostQueryResultDTO   {
  @JsonProperty("totalCount")
  private Long totalCount = null;

  @JsonProperty("pageIndex")
  private Integer pageIndex = null;

  @JsonProperty("pageSize")
  private Integer pageSize = null;

  @JsonProperty("data")
  private List<HostDTO> data = new ArrayList<HostDTO>();

  public HostQueryResultDTO totalCount(Long totalCount) {
    this.totalCount = totalCount;
    return this;
  }

   /**
   * 记录总数
   * @return totalCount
  **/
  @ApiModelProperty(value = "记录总数")
  public Long getTotalCount() {
    return totalCount;
  }

  public void setTotalCount(Long totalCount) {
    this.totalCount = totalCount;
  }

  public HostQueryResultDTO pageIndex(Integer pageIndex) {
    this.pageIndex = pageIndex;
    return this;
  }

   /**
   * 当前页码
   * @return pageIndex
  **/
  @ApiModelProperty(value = "当前页码")
  public Integer getPageIndex() {
    return pageIndex;
  }

  public void setPageIndex(Integer pageIndex) {
    this.pageIndex = pageIndex;
  }

  public HostQueryResultDTO pageSize(Integer pageSize) {
    this.pageSize = pageSize;
    return this;
  }

   /**
   * 每页记录数
   * @return pageSize
  **/
  @ApiModelProperty(value = "每页记录数")
  public Integer getPageSize() {
    return pageSize;
  }

  public void setPageSize(Integer pageSize) {
    this.pageSize = pageSize;
  }

  public HostQueryResultDTO data(List<HostDTO> data) {
    this.data = data;
    return this;
  }

  public HostQueryResultDTO addDataItem(HostDTO dataItem) {
    this.data.add(dataItem);
    return this;
  }

   /**
   * 主机列表
   * @return data
  **/
  @ApiModelProperty(value = "主机列表")
  public List<HostDTO> getData() {
    return data;
  }

  public void setData(List<HostDTO> data) {
    this.data = data;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    HostQueryResultDTO hostQueryResultDTO = (HostQueryResultDTO) o;
    return Objects.equals(this.totalCount, hostQueryResultDTO.totalCount) &&
        Objects.equals(this.pageIndex, hostQueryResultDTO.pageIndex) &&
        Objects.equals(this.pageSize, hostQueryResultDTO.pageSize) &&
        Objects.equals(this.data, hostQueryResultDTO.data);
  }

  @Override
  public int hashCode() {
    return Objects.hash(totalCount, pageIndex, pageSize, data);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class HostQueryResultDTO {\n");
    
    sb.append("    totalCount: ").append(toIndentedString(totalCount)).append("\n");
    sb.append("    pageIndex: ").append(toIndentedString(pageIndex)).append("\n");
    sb.append("    pageSize: ").append(toIndentedString(pageSize)).append("\n");
    sb.append("    data: ").append(toIndentedString(data)).append("\n");
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

