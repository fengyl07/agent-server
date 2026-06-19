package uyun.eagle.agent.alertagent.frontapi;

import uyun.eagle.agent.alertagent.frontapi.vo.DutyVO;
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


@Api(value = "dutys", description = "the dutys API")
@RequestMapping(value = "/{appcode}/frontapi/v1")
public interface DutysFrontApi {

    @ApiOperation(value = "获取某天的排班信息", notes = "获取某天的排班信息", response = DutyVO.class, responseContainer = "List", tags={ "rotatodo", })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "成功", response = DutyVO.class, responseContainer = "List"),
        @ApiResponse(code = 400, message = "失败", response = ErrorResponse.class) })
    @RequestMapping(value = "/dutys/query",
        produces = { "application/json" }, 
        method = RequestMethod.GET)
    List<DutyVO> queryDutys(@ApiParam(value = "排班日期", required = true) @RequestParam(value = "dutyDate", required = true) Long dutyDate



);

}
