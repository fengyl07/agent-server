package uyun.eagle.agent.alertagent.openapi;

import uyun.eagle.agent.alertagent.openapi.dto.HostQueryResultDTO;
import uyun.eagle.agent.alertagent.openapi.dto.ErrorResponse;

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

@javax.annotation.Generated(value = "class io.swagger.codegen.languages.SpringCodegen", date = "2019-04-19T09:39:00.067+08:00")

@Api(value = "hosts", description = "the hosts API")
@RequestMapping(value = "/{appcode}/openapi/v1")
public interface HostsOpenApi {

    @ApiOperation(value = "查询满足条件的主机", notes = "根据条件查询主机", response = HostQueryResultDTO.class, tags={ "hostmgr", })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "成功", response = HostQueryResultDTO.class),
        @ApiResponse(code = 400, message = "失败", response = ErrorResponse.class) })
    @RequestMapping(value = "/hosts/query",
        produces = { "application/json" }, 
        method = RequestMethod.GET)
    HostQueryResultDTO queryHosts(@ApiParam(value = "访问APIKEY", required = true) @RequestParam(value = "apikey", required = true) String apikey



,@ApiParam(value = "名称或IP地址，模糊匹配") @RequestParam(value = "query_key", required = false) String queryKey



,@ApiParam(value = "查询页号，值为 0 时表示第1页", defaultValue = "0") @RequestParam(value = "page_index", required = false, defaultValue="0") Integer pageIndex



,@ApiParam(value = "每页行数，值为 -1 时表示不分页", defaultValue = "50") @RequestParam(value = "page_size", required = false, defaultValue="50") Integer pageSize



);

}
