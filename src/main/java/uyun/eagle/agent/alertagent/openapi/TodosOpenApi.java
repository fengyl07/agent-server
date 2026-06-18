package uyun.eagle.agent.alertagent.openapi;

import uyun.eagle.agent.alertagent.openapi.dto.TodoDTO;
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

@javax.annotation.Generated(value = "class io.swagger.codegen.languages.SpringCodegen", date = "2019-06-14T15:08:34.096+08:00")

@Api(value = "todos", description = "the todos API")
@RequestMapping(value = "/{appcode}/openapi/v1")
public interface TodosOpenApi {

    @ApiOperation(value = "查询待办事项", notes = "查询待办事项", response = TodoDTO.class, responseContainer = "List", tags={ "rotatodo", })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "成功", response = TodoDTO.class, responseContainer = "List"),
        @ApiResponse(code = 400, message = "失败", response = ErrorResponse.class) })
    @RequestMapping(value = "/todos/query",
        produces = { "application/json" }, 
        method = RequestMethod.GET)
    List<TodoDTO> queryTodos(@ApiParam(value = "访问APIKEY", required = true) @RequestParam(value = "apikey", required = true) String apikey



,@ApiParam(value = "排班日期", required = true) @RequestParam(value = "dutyDate", required = true) Long dutyDate



,@ApiParam(value = "当前用户", required = true) @RequestParam(value = "currentUser", required = true) String currentUser



);

}
