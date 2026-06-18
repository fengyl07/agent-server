package uyun.eagle.agent.alertagent.exception;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.mysql.jdbc.exceptions.MySQLIntegrityConstraintViolationException;

import lombok.extern.slf4j.Slf4j;
import uyun.eagle.agent.alertagent.common.LocaleMessage;
import uyun.eagle.agent.alertagent.exception.DemoException.CauseType;
import uyun.whale.consumer.dto.Response;

/**
 * @author: yangfei
 * @desc: 全局异常
 * @date: created in 2019/1/16 11:09
 * @modifed by:
 */
@Slf4j
@ControllerAdvice
public class AdminExceptionHandler {

    @Autowired
    private LocaleMessage localeMessage;

    @ResponseBody
    @ExceptionHandler(value = DemoException.class)
    public ResponseEntity<Response> ExceptionHandler(HttpServletRequest request, DemoException ex) {
        int code = Integer.valueOf(ex.getMessageCode());
        String message = ex.getMessage();
        String path = request.getRequestURI();

        if (ex.getCauseType() == CauseType.CLIENT) {
            message = localeMessage.getMessage(ex.getMessageCode(), ex.getParams());
            log.info("ClientExcepton: {}", ex.getMessage());
            return new ResponseEntity<Response>(createResponse(code, message+path), HttpStatus.BAD_REQUEST);
        } else {
            log.error("ServerException", ex);
            return new ResponseEntity<Response>(createResponse(code, message+path), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private Response createResponse(int code, String message) {
        Response response = new Response(code, message);
        return response;
    }

    /**
     * @Description: 系统异常捕获处理
     * @Date: 16:07 2018/5/30
     */
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    @ExceptionHandler(value = Exception.class)
    public Response javaExceptionHandler(Exception ex) {//APIResponse是项目中对外统一的出口封装，可以根据自身项目的需求做相应更改
        log.error("捕获到Exception异常", ex);
        //异常日志入库
        return  new Response(HttpStatus.INTERNAL_SERVER_ERROR.value(), localeMessage.getMessage(String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value())));
    }

    /**
     * @Description: 自定义异常捕获处理
     * @Date: 16:08 2018/5/30
     */
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    @ExceptionHandler(value = RuntimeException.class)//MessageCenterException是自定义的一个异常
    public Response messageCenterExceptionHandler(RuntimeException ex) {
        log.error("捕获到RuntimeException异常 {}", ex);
        return new Response(HttpStatus.INTERNAL_SERVER_ERROR.value(), localeMessage.getMessage(String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value())));
    }

    /**
     * @Description: 自定义异常捕获处理
     * @Date: 16:08 2018/5/30
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    @ExceptionHandler(value = IllegalArgumentException.class)
    public Response messageCenterExceptionHandler(IllegalArgumentException ex) {
        log.error("捕获到IllegalArgumentException异常 {}", ex);
        return new Response(HttpStatus.BAD_REQUEST.value(), localeMessage.getMessage(String.valueOf(HttpStatus.BAD_REQUEST.value())));
    }

    /**
     * @Description: 自定义异常捕获处理
     * @Date: 16:08 2018/5/30
     */
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    @ExceptionHandler(value = NoSuchMethodException.class)//MessageCenterException是自定义的一个异常
    public Response noSuchMethodExceptionHandler(NoSuchMethodException ex) {
        log.error("捕获到NoSuchMethodException异常 {}", ex);
        return new Response(HttpStatus.INTERNAL_SERVER_ERROR.value(), localeMessage.getMessage(String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value())));
    }

    /**
     * @param ex
     * @return
     * @desc request参考错误
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    @ExceptionHandler(value = MissingServletRequestParameterException.class)//MessageCenterException是自定义的一个异常
    public Response MissingServletRequestParameterExceptionHandler(MissingServletRequestParameterException ex) {
        log.error("捕获到MissingServletRequestParameterException异常 {}", ex);
        return new Response(HttpStatus.BAD_REQUEST.value(), localeMessage.getMessage(String.valueOf(HttpStatus.BAD_REQUEST.value())));
    }

    /**
     * @param ex
     * @return
     * @desc request参考错误
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    @ExceptionHandler(value = NullPointerException.class)//MessageCenterException是自定义的一个异常
    public Response NullPointerExceptionExceptionHandler(NullPointerException ex) {
        log.error("捕获到NullPointerException异常 {}", ex);
        return new Response(HttpStatus.BAD_REQUEST.value(), localeMessage.getMessage(String.valueOf(HttpStatus.BAD_REQUEST.value())));
    }

    /**
     * @param ex
     * @return
     * @desc mysql 参考错误
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    @ExceptionHandler(value = MySQLIntegrityConstraintViolationException.class)//MessageCenterException是自定义的一个异常
    public Response MySQLIntegrityConstraintViolationExceptionnHandler(MySQLIntegrityConstraintViolationException ex) {
        log.error("捕获到MySQLIntegrityConstraintViolationException {}", ex);
        return new Response(HttpStatus.BAD_REQUEST.value(), localeMessage.getMessage(String.valueOf(HttpStatus.BAD_REQUEST.value())));
    }

}