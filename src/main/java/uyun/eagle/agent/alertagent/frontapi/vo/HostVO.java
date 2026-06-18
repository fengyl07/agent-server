package uyun.eagle.agent.alertagent.frontapi.vo;

import com.esotericsoftware.kryo.DefaultSerializer;
import com.esotericsoftware.kryo.serializers.CompatibleFieldSerializer;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;




/**
 * HostVO
 */
@javax.annotation.Generated(value = "class io.swagger.codegen.languages.SpringCodegen", date = "2019-04-19T09:38:59.295+08:00")


public class HostVO   {
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

  public HostVO id(String id) {
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

  public HostVO name(String name) {
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

  public HostVO ipAddr(String ipAddr) {
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

  public HostVO osType(String osType) {
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

  public HostVO cpuCores(Integer cpuCores) {
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

  public HostVO memorySize(Double memorySize) {
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

  public HostVO diskCapacity(Double diskCapacity) {
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
    HostVO hostVO = (HostVO) o;
    return Objects.equals(this.id, hostVO.id) &&
        Objects.equals(this.name, hostVO.name) &&
        Objects.equals(this.ipAddr, hostVO.ipAddr) &&
        Objects.equals(this.osType, hostVO.osType) &&
        Objects.equals(this.cpuCores, hostVO.cpuCores) &&
        Objects.equals(this.memorySize, hostVO.memorySize) &&
        Objects.equals(this.diskCapacity, hostVO.diskCapacity);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, ipAddr, osType, cpuCores, memorySize, diskCapacity);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class HostVO {\n");
    
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

