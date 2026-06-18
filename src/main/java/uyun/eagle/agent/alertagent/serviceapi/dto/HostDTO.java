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
 * HostDTO
 */
@javax.annotation.Generated(value = "class io.swagger.codegen.languages.SpringCodegen", date = "2019-04-19T09:38:59.960+08:00")

@DefaultSerializer(CompatibleFieldSerializer.class)
public class HostDTO  implements Serializable {
  @JsonProperty("id")
  private String id = null;

  @JsonProperty("name")
  private String name = null;

  @JsonProperty("ipAddr")
  private String ipAddr = null;

  @JsonProperty("osType")
  private String osType = null;

  @JsonProperty("cpuCores")
  private Integer cpuCores = null;

  @JsonProperty("memorySize")
  private Double memorySize = null;

  @JsonProperty("diskCapacity")
  private Double diskCapacity = null;

  public HostDTO id(String id) {
    this.id = id;
    return this;
  }

   /**
   * ID
   * @return id
  **/
  @ApiModelProperty(value = "ID")
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public HostDTO name(String name) {
    this.name = name;
    return this;
  }

   /**
   * 名称
   * @return name
  **/
  @ApiModelProperty(value = "名称")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public HostDTO ipAddr(String ipAddr) {
    this.ipAddr = ipAddr;
    return this;
  }

   /**
   * IP地址
   * @return ipAddr
  **/
  @ApiModelProperty(value = "IP地址")
  public String getIpAddr() {
    return ipAddr;
  }

  public void setIpAddr(String ipAddr) {
    this.ipAddr = ipAddr;
  }

  public HostDTO osType(String osType) {
    this.osType = osType;
    return this;
  }

   /**
   * 操作系统
   * @return osType
  **/
  @ApiModelProperty(value = "操作系统")
  public String getOsType() {
    return osType;
  }

  public void setOsType(String osType) {
    this.osType = osType;
  }

  public HostDTO cpuCores(Integer cpuCores) {
    this.cpuCores = cpuCores;
    return this;
  }

   /**
   * CPU核数
   * @return cpuCores
  **/
  @ApiModelProperty(value = "CPU核数")
  public Integer getCpuCores() {
    return cpuCores;
  }

  public void setCpuCores(Integer cpuCores) {
    this.cpuCores = cpuCores;
  }

  public HostDTO memorySize(Double memorySize) {
    this.memorySize = memorySize;
    return this;
  }

   /**
   * 内存大小
   * @return memorySize
  **/
  @ApiModelProperty(value = "内存大小")
  public Double getMemorySize() {
    return memorySize;
  }

  public void setMemorySize(Double memorySize) {
    this.memorySize = memorySize;
  }

  public HostDTO diskCapacity(Double diskCapacity) {
    this.diskCapacity = diskCapacity;
    return this;
  }

   /**
   * 磁盘容量
   * @return diskCapacity
  **/
  @ApiModelProperty(value = "磁盘容量")
  public Double getDiskCapacity() {
    return diskCapacity;
  }

  public void setDiskCapacity(Double diskCapacity) {
    this.diskCapacity = diskCapacity;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    HostDTO hostDTO = (HostDTO) o;
    return Objects.equals(this.id, hostDTO.id) &&
        Objects.equals(this.name, hostDTO.name) &&
        Objects.equals(this.ipAddr, hostDTO.ipAddr) &&
        Objects.equals(this.osType, hostDTO.osType) &&
        Objects.equals(this.cpuCores, hostDTO.cpuCores) &&
        Objects.equals(this.memorySize, hostDTO.memorySize) &&
        Objects.equals(this.diskCapacity, hostDTO.diskCapacity);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, ipAddr, osType, cpuCores, memorySize, diskCapacity);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class HostDTO {\n");
    
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    ipAddr: ").append(toIndentedString(ipAddr)).append("\n");
    sb.append("    osType: ").append(toIndentedString(osType)).append("\n");
    sb.append("    cpuCores: ").append(toIndentedString(cpuCores)).append("\n");
    sb.append("    memorySize: ").append(toIndentedString(memorySize)).append("\n");
    sb.append("    diskCapacity: ").append(toIndentedString(diskCapacity)).append("\n");
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

