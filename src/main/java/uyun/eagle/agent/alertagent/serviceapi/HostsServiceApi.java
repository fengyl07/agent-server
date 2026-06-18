package uyun.eagle.agent.alertagent.serviceapi;

import uyun.eagle.agent.alertagent.serviceapi.dto.HostDTO;
import uyun.eagle.agent.alertagent.serviceapi.dto.ErrorResponse;

import io.swagger.annotations.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@javax.annotation.Generated(value = "class io.swagger.codegen.languages.SpringCodegen", date = "2019-04-19T09:38:59.960+08:00")

@Api(value = "hosts", description = "the hosts API")
@RequestMapping(value = "/{appcode}/serviceapi/v1")
public interface HostsServiceApi {

    @ApiOperation(value = "新建主机", notes = "新建一个主机", response = HostDTO.class, tags={ "hostmgr", })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "成功，返回新增的主机信息", response = HostDTO.class),
        @ApiResponse(code = 500, message = "失败", response = ErrorResponse.class) })
    @RequestMapping(value = "/hosts/create",
        produces = { "application/json" }, 
        method = RequestMethod.POST)
    HostDTO createHost(

@ApiParam(value = "主机信息"  ) @RequestBody HostDTO hostDTO

);


    @ApiOperation(value = "根据ID删除主机", notes = "删除ID指定的主机", response = HostDTO.class, tags={ "hostmgr", })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "成功，返回被删除的主机信息", response = HostDTO.class),
        @ApiResponse(code = 500, message = "失败", response = ErrorResponse.class) })
    @RequestMapping(value = "/hosts/delete",
        produces = { "application/json" }, 
        method = RequestMethod.POST)
    HostDTO deleteHostById(@ApiParam(value = "待删除主机的ID", required = true) @RequestParam(value = "id", required = true) String id



);


    @ApiOperation(value = "根据ID加载主机", notes = "返回ID指定的主机信息", response = HostDTO.class, tags={ "hostmgr", })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "成功，返回对应的主机信息", response = HostDTO.class),
        @ApiResponse(code = 500, message = "失败", response = ErrorResponse.class) })
    @RequestMapping(value = "/hosts/get",
        produces = { "application/json" }, 
        method = RequestMethod.GET)
    HostDTO getHostById(@ApiParam(value = "主机的ID", required = true) @RequestParam(value = "id", required = true) String id



);


    @ApiOperation(value = "根据ID更新主机", notes = "更新IP对应的主机", response = HostDTO.class, tags={ "hostmgr", })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "成功，返回更新后主机信息", response = HostDTO.class),
        @ApiResponse(code = 500, message = "失败", response = ErrorResponse.class) })
    @RequestMapping(value = "/hosts/update",
        produces = { "application/json" }, 
        method = RequestMethod.POST)
    HostDTO updateHostById(

@ApiParam(value = "待更新的主机信息"  ) @RequestBody HostDTO hostDTO

);

}
