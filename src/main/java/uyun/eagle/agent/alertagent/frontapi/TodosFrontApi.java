package uyun.eagle.agent.alertagent.frontapi;

import uyun.eagle.agent.alertagent.frontapi.vo.TodoVO;
import uyun.eagle.agent.alertagent.frontapi.vo.ErrorResponse;

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


@Api(value = "todos", description = "the todos API")
@RequestMapping(value = "/{appcode}/frontapi/v1")
public interface TodosFrontApi {

    @ApiOperation(value = "删除待办事项", notes = "删除待办事项", response = TodoVO.class, tags={ "rotatodo", })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "成功", response = TodoVO.class),
        @ApiResponse(code = 500, message = "失败", response = ErrorResponse.class) })
    @RequestMapping(value = "/todos/delete",
        produces = { "application/json" }, 
        method = RequestMethod.POST)
    TodoVO deleteTodoById(@ApiParam(value = "待办事项ID", required = true) @RequestParam(value = "id", required = true) Integer id



);


    @ApiOperation(value = "创建待办事项", notes = "创建待办事项", response = TodoVO.class, tags={ "rotatodo", })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "成功", response = TodoVO.class),
        @ApiResponse(code = 500, message = "失败", response = ErrorResponse.class) })
    @RequestMapping(value = "/todos/create",
        produces = { "application/json" }, 
        method = RequestMethod.POST)
    TodoVO insertTodo(

@ApiParam(value = "待办事项"  ) @RequestBody TodoVO createTodo

);


    @ApiOperation(value = "根据条件查询待办事项", notes = "根据条件查询待办事项", response = TodoVO.class, responseContainer = "List", tags={ "rotatodo", })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "成功", response = TodoVO.class, responseContainer = "List"),
        @ApiResponse(code = 400, message = "失败", response = ErrorResponse.class) })
    @RequestMapping(value = "/todos/search",
        produces = { "application/json" }, 
        method = RequestMethod.GET)
    List<TodoVO> queryByContent(@ApiParam(value = "排班日期", required = true) @RequestParam(value = "dutyDate", required = true) Long dutyDate



,@ApiParam(value = "任务内容", required = true) @RequestParam(value = "content", required = true) String content



);


    @ApiOperation(value = "查询待办事项", notes = "查询待办事项", response = TodoVO.class, responseContainer = "List", tags={ "rotatodo", })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "成功", response = TodoVO.class, responseContainer = "List"),
        @ApiResponse(code = 400, message = "失败", response = ErrorResponse.class) })
    @RequestMapping(value = "/todos/query",
        produces = { "application/json" }, 
        method = RequestMethod.GET)
    List<TodoVO> selectTodoFindAll(@ApiParam(value = "排班日期", required = true) @RequestParam(value = "dutyDate", required = true) Long dutyDate



);


    @ApiOperation(value = "更新待办事项", notes = "更新待办事项", response = TodoVO.class, tags={ "rotatodo", })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "成功", response = TodoVO.class),
        @ApiResponse(code = 500, message = "失败", response = ErrorResponse.class) })
    @RequestMapping(value = "/todos/update",
        produces = { "application/json" }, 
        method = RequestMethod.POST)
    TodoVO updateTodoById(

@ApiParam(value = "待办事项"  ) @RequestBody TodoVO updateTodo

);

}
